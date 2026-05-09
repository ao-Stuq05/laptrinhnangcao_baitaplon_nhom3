package com.auction.shared.model;

// FIX: import từng class riêng — không dùng Item.Electronics (sai cú pháp)
import com.auction.shared.model.Auction;
import com.auction.shared.model.AuctionStatus;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Seller;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   TEST ĐA LUỒNG — 2 THREAD ĐẶT GIÁ       ║");
        System.out.println("╚═══════════════════════════════════════════╝");

        // ── 1. Chuẩn bị dữ liệu test ─────────────────────────────────────────

        // Tạo Seller
        Seller seller = new Seller(
                "S001", "seller1", "s@test.com",
                "hashed_pass", "TestShop", 5.0
        );

       
        // Tham số cuối là boolean hasWarranty, không phải int warrantyMonths
        Electronics iphone = new Electronics(
                "ITM-001",
                "iPhone 16 Pro",
                "Điện thoại cao cấp",
                10_000_000.0,
                seller,
                12

        );
        seller.listItem(iphone);

        // FIX: Auction constructor cần seller
        // Dùng constructor: Auction(String id, Item item, Seller seller,
        //                          AuctionStatus status, double currentPrice,
        //                          LocalDateTime start, LocalDateTime end,
        //                          Bidder leadingBidder, Bidder winner)
        Auction auction = new Auction(
                "AUC-TEST-001",
                iphone,
                seller,
                AuctionStatus.OPEN,
                10_000_000.0,
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(5),
                null,   // leadingBidder
                null    // winner
        );

        // Chuyển sang RUNNING để nhận bid
        auction.startAuction(); // OPEN → RUNNING

        // Tạo 2 Bidder
        Bidder alice = new Bidder(
                "B001", "alice", "alice@test.com",
                "hashed_pass", 100_000_000.0
        );
        Bidder bob = new Bidder(
                "B002", "bob", "bob@test.com",
                "hashed_pass", 100_000_000.0
        );

        System.out.printf("Giá khởi điểm: %,.0f VNĐ%n", auction.getCurrentPrice());
        System.out.println("Alice và Bob sẽ đặt 11.000.000 cùng lúc...\n");

        // ── 2. CountDownLatch — đồng bộ hóa 2 thread ─────────────────────────

        // startGate: chặn 2 thread cho đến khi mở cổng
        CountDownLatch startGate = new CountDownLatch(1);
        // doneLatch: chờ cả 2 thread hoàn thành
        CountDownLatch doneLatch = new CountDownLatch(2);

        // AtomicInteger: thread-safe counter (không dùng int++ vì không an toàn)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        // ── 3. Tạo và chạy 2 thread ───────────────────────────────────────────

        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Thread 1: Alice đặt 11 triệu
        pool.submit(() -> {
            try {
                System.out.println("[Alice] Sẵn sàng, đang chờ lệnh bắt đầu...");
                startGate.await(); // Chờ tại cổng

                System.out.println("[Alice] BẮT ĐẦU đặt giá 11.000.000");
                auction.placeBid(alice, 11_000_000.0);
                successCount.incrementAndGet();
                System.out.println("[Alice] Đặt giá THÀNH CÔNG");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[Alice] Bid bị từ chối: " + e.getMessage());
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown(); // Báo đã xong
            }
        });

        // Thread 2: Bob đặt 11 triệu (cùng số tiền)
        pool.submit(() -> {
            try {
                System.out.println("[Bob]   Sẵn sàng, đang chờ lệnh bắt đầu...");
                startGate.await(); // Chờ tại cổng

                System.out.println("[Bob]   BẮT ĐẦU đặt giá 11.000.000");
                auction.placeBid(bob, 11_000_000.0);
                successCount.incrementAndGet();
                System.out.println("[Bob]   Đặt giá THÀNH CÔNG");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[Bob]   Bid bị từ chối: " + e.getMessage());
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown(); // Báo đã xong
            }
        });

        // ── 4. Mở cổng — cả 2 thread chạy cùng lúc ──────────────────────────

        Thread.sleep(200); // Đảm bảo cả 2 đã await() trước khi mở cổng
        System.out.println("\n>> Mở cổng! Cả hai thread bắt đầu...\n");
        startGate.countDown(); // Giảm 1→0 → mở cổng

        // ── 5. Chờ cả 2 thread hoàn thành ────────────────────────────────────

        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        if (!finished) {
            System.out.println("⚠️  TIMEOUT — test mất quá 5 giây!");
            return;
        }

        // ── 6. In kết quả ─────────────────────────────────────────────────────

        System.out.println("\n╔═══════════════════════════════╗");
        System.out.println("║           KẾT QUẢ             ║");
        System.out.println("╚═══════════════════════════════╝");
        System.out.println("Bid thành công : " + successCount.get());
        System.out.println("Bid thất bại   : " + failCount.get());
        System.out.printf ("Giá cuối cùng  : %,.0f VNĐ%n", auction.getCurrentPrice());
        System.out.println("Người dẫn đầu  : " +
                (auction.getLeadingBidder() != null
                        ? auction.getLeadingBidder().getUsername()
                        : "chưa có"));
        System.out.println("Số bid đã ghi  : " + auction.getBids().size());

        // ── 7. Kiểm tra tính đúng đắn ─────────────────────────────────────────

        System.out.println("\n── Kiểm tra logic ───────────────────────────");

        // Kỳ vọng: CHỈ 1 trong 2 bid thành công
        // Vì cả 2 đặt 11tr → bid thứ 2 không > currentPrice (đã là 11tr) → bị từ chối
        if (successCount.get() == 1 && failCount.get() == 1) {
            System.out.println("✅ PASS: Đúng! Chỉ 1 bid thành công, 1 bị từ chối.");
            System.out.println("      → synchronized ngăn lost update thành công.");
        } else if (successCount.get() == 2) {
            System.out.println("❌ FAIL: Cả 2 bid thành công — BỊ LOST UPDATE!");
            System.out.println("      → Kiểm tra lại synchronized trong placeBid().");
        } else {
            System.out.println("⚠️  Kết quả không mong đợi: "
                    + successCount.get() + " thành công, "
                    + failCount.get() + " thất bại");
        }

        // Chỉ 1 BidTransaction được ghi vào lịch sử
        if (auction.getBids().size() == 1) {
            System.out.println("✅ PASS: Chỉ 1 BidTransaction được ghi.");
        } else {
            System.out.println("❌ FAIL: " + auction.getBids().size()
                    + " BidTransaction — dữ liệu bị nhân đôi!");
        }

        System.out.println("\n── Test kết thúc ─────────────────────────────");
    }
}
