package com.auction.util;

import com.auction.shared.model.*;
import com.auction.shared.model.Item.Electronics;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyTest {

    public static void main(String[] args) throws InterruptedException {

        System.out.println("║   TEST ĐA LUỒNG — 2 THREAD ĐẶT GIÁ     ║");

        // ── 1. Chuẩn bị dữ liệu test ─────────────────────────────────────────

        // Tạo Seller giả
        Seller seller = new Seller("S001", "seller1", "s@test.com",
                "hashed_pass", "TestShop", 5.0);

        // Tạo sản phẩm: iPhone giá khởi điểm 10 triệu
        Electronics iphone = new Electronics(
                "ITM-001", "iPhone 16 Pro", "Điện thoại cao cấp",
                10_000_000.0, seller, 12);
        seller.listItem(iphone);

        // Tạo phiên đấu giá kéo dài 5 phút
        Auction auction = new Auction(
                "AUC-TEST-001", iphone, seller,
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        auction.startAuction(); // OPEN → RUNNING (bỏ qua AuctionManager cho test này)

        // Tạo 2 Bidder tham gia
        Bidder alice = new Bidder("B001", "alice", "alice@test.com",
                "hashed_pass", 100_000_000.0);
        Bidder bob   = new Bidder("B002", "bob",   "bob@test.com",
                "hashed_pass", 100_000_000.0);

        System.out.println("Giá khởi điểm: " + auction.getCurrentPrice());
        System.out.println("Alice và Bob sẽ đặt giá cùng lúc...\n");

        // ── 2. CountDownLatch — đồng bộ hóa 2 thread ─────────────────────────

        CountDownLatch startGate = new CountDownLatch(1);
        // doneLatch: chờ cho đến khi CẢ HAI thread hoàn thành
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Đếm số bid thành công
        // Dùng AtomicInteger thay int vì nhiều thread cùng tăng giá trị
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);

        // ── 3. Tạo và chạy 2 thread ───────────────────────────────────────────

        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Thread 1: Alice đặt 11 triệu
        pool.submit(() -> {
            try {
                System.out.println("[Alice] Sẵn sàng, đang chờ lệnh bắt đầu...");
                startGate.await(); // Alice đứng chờ tại cổng

                // ─── Đây là lúc 2 thread chạy đồng thời ───
                System.out.println("[Alice] BẮT ĐẦU đặt giá 11.000.000");
                auction.placeBid(alice, 11_000_000.0);
                successCount.incrementAndGet();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[Alice] Lỗi: " + e.getMessage());
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown(); // Alice báo đã xong
            }
        });

        // Thread 2: Bob đặt 11 triệu (cùng số tiền với Alice)
        pool.submit(() -> {
            try {
                System.out.println("[Bob]   Sẵn sàng, đang chờ lệnh bắt đầu...");
                startGate.await(); // Bob đứng chờ tại cổng

                // ─── Cùng thời điểm với Alice ───
                System.out.println("[Bob]   BẮT ĐẦU đặt giá 11.000.000");
                auction.placeBid(bob, 11_000_000.0);
                successCount.incrementAndGet();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.out.println("[Bob]   Lỗi: " + e.getMessage());
                failCount.incrementAndGet();
            } finally {
                doneLatch.countDown(); // Bob báo đã xong
            }
        });

        // ── 4. Mở cổng — cả 2 thread chạy cùng lúc ──────────────────────────

        Thread.sleep(100); // Đảm bảo cả 2 thread đã await() trước khi mở cổng
        System.out.println("\n>> Mở cổng! Cả hai thread bắt đầu...\n");
        startGate.countDown(); // Giảm từ 1 về 0 → mở cổng → 2 thread chạy

        // ── 5. Chờ cả 2 thread hoàn thành ────────────────────────────────────

        // Chờ tối đa 5 giây
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        pool.shutdown();

        // ── 6. In kết quả và kiểm tra ─────────────────────────────────────────

        System.out.println("║              KẾT QUẢ                    ║");
        System.out.println("Bid thành công : " + successCount.get());
        System.out.println("Bid thất bại   : " + failCount.get());
        System.out.printf ("Giá cuối cùng  : %,.0f%n", auction.getCurrentPrice());
        System.out.println("Người dẫn đầu  : "
                + (auction.getLeadingBidder() != null
                   ? auction.getLeadingBidder().getUsername() : "chưa có"));
        System.out.println("Số bid đã ghi  : " + auction.getBids().size());

        // ── 7. Kiểm tra tính đúng đắn ─────────────────────────────────────────

        System.out.println("\n── Kiểm tra logic ─────────────────────────");

        // Kỳ vọng: CHỈ 1 bid thành công (không phải 2)
        // Vì 2 bid cùng giá 11tr → cái thứ 2 phải bị từ chối (11tr không > 11tr)
        if (successCount.get() == 1 && failCount.get() == 1) {
            System.out.println("PASS: Đúng! Chỉ 1 bid thành công, 1 bid bị từ chối.");
            System.out.println("   → synchronized đã ngăn lost update thành công.");
        } else if (successCount.get() == 2) {
            System.out.println("FAIL: Cả 2 bid đều thành công — BỊ LOST UPDATE!");
            System.out.println("   → Hãy kiểm tra lại synchronized trong placeBid().");
        } else {
            System.out.println("⚠️  Kết quả không như mong đợi: " + successCount.get()
                    + " thành công, " + failCount.get() + " thất bại");
        }

        // Kỳ vọng: chỉ có đúng 1 BidTransaction được ghi
        if (auction.getBids().size() == 1) {
            System.out.println("PASS: Chỉ 1 BidTransaction được ghi vào lịch sử.");
        } else {
            System.out.println("FAIL: " + auction.getBids().size()
                    + " BidTransaction — dữ liệu bị nhân đôi!");
        }

        System.out.println("\n── Test kết thúc ───────────────────────────");
    }
}
