package com.auction.shared.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// AuctionManager là Singleton — quản lý toàn bộ danh sách phiên đấu giá.

public final class AuctionManager {

    // ── Singleton: double-checked locking ────────────────────────────────────

    //"volatile" là bắt buộc khi dùng double-checked locking, ngăn reorder và đảm bảo đúng thứ tự
    private static volatile AuctionManager instance;

    // Private constructor: ngăn new AuctionManager() từ bên ngoài
    private AuctionManager() {
        this.auctions     = new ArrayList<>();
        this.registryLock = new ReentrantLock(true); // fair=true: FIFO order
        this.scheduler    = Executors.newScheduledThreadPool(4);
        // newScheduledThreadPool(4): tạo pool 4 thread để schedule đóng phiên
        // → 4 phiên có thể tự đóng đồng thời
    }

    /**
     Double-checked locking pattern:
     Check 1 (ngoài synchronized): nếu đã có instance → return ngay để tránh lock khi không cần thiết 
     Check 2 (trong synchronized): check lại vì có thể 2 thread cùng vượt qua check 1 
     */ 
    public static AuctionManager getInstance() {
        if (instance == null) {                          // Check 1: không lock
            synchronized (AuctionManager.class) {        // Chỉ lock khi cần
                if (instance == null) {                  // Check 2: trong lock
                    instance = new AuctionManager();
                }
            }
        }
        return instance;
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final List<Auction> auctions;

    // ReentrantLock bảo vệ danh sách auctions.
    private final ReentrantLock registryLock;

    // ScheduledExecutorService: lên lịch tự động đóng phiên khi hết giờ.
    private final ScheduledExecutorService scheduler;

    // ── Đăng ký phiên đấu giá ────────────────────────────────────────────────

    // Đăng ký và bắt đầu một phiên đấu giá.
    public void registerAuction(Auction auction) {
        registryLock.lock();         // Lấy lock — thread khác phải chờ
        try {
            auctions.add(auction);   // Thêm vào danh sách an toàn
            auction.startAuction();  // OPEN → RUNNING
            scheduleClose(auction);  // Lên lịch tự đóng khi hết giờ
            System.out.println("[AuctionManager] Đã đăng ký phiên: " + auction.getId());
        } finally {
            registryLock.unlock();   // LUÔN giải phóng lock dù có lỗi hay không
        }
    }

    // Lên lịch tự động đóng phiên khi hết giờ 
    public void scheduleClose(Auction auction) {
        // Tính khoảng cách thời gian từ bây giờ đến lúc kết thúc (milliseconds)
        long delayMs = java.time.Duration
                .between(LocalDateTimeNow(), auction.getEndTime())
                .toMillis();

        if (delayMs <= 0) {
            // Thời gian đã qua → đóng ngay
            auction.endAuction();
            return;
        }

        // Lên lịch: sau delayMs milliseconds, gọi endAuction() trên background thread
        scheduler.schedule(() -> {
            // Lambda này chạy trên ScheduledExecutor thread, không phải main thread
            if (auction.getStatus() == AuctionStatus.RUNNING) {
                auction.endAuction(); // RUNNING → FINISHED
            }
            // Nếu phiên đã bị hủy (CANCELLED) thì không làm gì
        }, delayMs, TimeUnit.MILLISECONDS);

        System.out.println("[AuctionManager] Đã lên lịch đóng phiên "
                + auction.getId() + " sau " + delayMs + "ms");
    } 

    // Helper nhỏ để dễ mock trong test
    private java.time.LocalDateTime LocalDateTimeNow() {
        return java.time.LocalDateTime.now();
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách phiên đang RUNNING.
     * Dùng unmodifiableList để bên ngoài không thể xóa/thêm vào list gốc.
     */
    public List<Auction> getActiveAuctions() {
        registryLock.lock();
        try {
            List<Auction> active = new ArrayList<>();
            for (Auction a : auctions) {
                if (a.getStatus() == AuctionStatus.RUNNING) {
                    active.add(a);
                }
            }
            return Collections.unmodifiableList(active);
        } finally {
            registryLock.unlock();
        }
    }

    // Lấy toàn bộ phiên (mọi trạng thái)
    public List<Auction> getAllAuctions() {
        registryLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(auctions));
        } finally {
            registryLock.unlock();
        }
    }

    /**
     * Tắt server: dừng scheduler.
     * Gọi khi tắt ứng dụng để không bị thread leak.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            // Chờ tối đa 5 giây cho các task đang chạy hoàn thành
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow(); // Quá 5 giây → ép tắt
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt(); // Khôi phục interrupted flag
        }
        System.out.println("[AuctionManager] Đã tắt.");
    }
}
