package com.auction.server.db;

import com.auction.server.network.AuctionServer;
import com.auction.server.service.AuctionManager;
import com.auction.shared.model.*;
import com.auction.shared.network.Message;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TwoClientTest {

    // Dùng port khác để không xung đột với server đang chạy (port 9090)
    private static final int TEST_PORT = 9099;

    public static void main(String[] args) throws Exception {

        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║   TEST: 2 CLIENT KẾT NỐI VÀ ĐẶT GIÁ ĐỒNG THỜI  ║");
        System.out.println("╚═══════════════════════════════════════════════════╝\n");

        // ── 1. Khởi động Server ngầm trong 1 luồng riêng ──────────────────────
        AuctionServer server = new AuctionServer(TEST_PORT);
        Thread serverThread = new Thread(server::startServer);
        serverThread.setDaemon(true);
        serverThread.start();

        // Chờ 1 giây để đảm bảo Server kịp Socket bind thành công
        Thread.sleep(1000);

        // ── 2. Tạo dữ liệu giả lập cho phiên đấu giá thông qua Manager ────────
        AuctionManager manager = AuctionManager.getInstance();
        Seller seller = new Seller("id_seller", "shop_seller", "seller@test.com", "hash_pass", "Shop_VIP", 5.0);
        Electronics laptop = new Electronics("id_item_1", "MacBook M3 Pro", "Laptop Apple", 20000000, seller, 12);

        Auction auction = new Auction("auc_123", laptop, seller,
                LocalDateTime.now().plusSeconds(1), LocalDateTime.now().plusSeconds(10));
        manager.registerAuction(auction);

        // ── 3. Chuẩn bị môi trường Concurrent Testing cho 2 Client ────────────
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicReference<Double> lastPriceRef = new AtomicReference<>((double) 20000000);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Client 1: bid 21,000,000 VNĐ
        executor.submit(() -> runClientBidTask("user_alpha", 21000000, startLatch, doneLatch, successCount, failCount, lastPriceRef));
        // Client 2: cũng đồng thời tranh chấp giá 21,000,000 VNĐ
        executor.submit(() -> runClientBidTask("user_beta", 21000000, startLatch, doneLatch, successCount, failCount, lastPriceRef));

        // Kích hoạt đồng thời cả 2 luồng đặt giá bắn phá vào hệ thống
        System.out.println("[Test] >>> BẮT ĐẦU ĐẶT GIÁ ĐỒNG THỜI...");
        startLatch.countDown();

        // Chờ tối đa 5 giây cho cuộc đua tranh chấp luồng kết thúc
        boolean finishedCleanly = doneLatch.await(5, TimeUnit.SECONDS);

        // ── 4. Thu thập thông tin và Assert kết quả ─────────────────────────
        executor.shutdown();
        System.out.println("\n╔═══════════════════════════════════════════════════╗");
        System.out.println("║                KẾT QUẢ KIỂM TRA                   ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        check("Hai client hoàn thành luồng xử lý không treo", finishedCleanly);
        check("Chỉ có DUY NHẤT 1 lượt đặt giá thành công (Giao dịch Nguyên tố)", successCount.get() == 1);
        check("Lượt đặt giá còn lại phải bị Server từ chối (Mức giá cũ)", failCount.get() == 1);
        check("Giá hiện tại của phiên đấu giá tăng lên 21,000,000 VNĐ", lastPriceRef.get() == 21000000 || auction.getCurrentPrice() == 21000000);
    }

    private static void runClientBidTask(String username, double amount,
                                         CountDownLatch startLatch, CountDownLatch doneLatch,
                                         AtomicInteger successCount, AtomicInteger failCount,
                                         AtomicReference<Double> lastPriceRef) {
        try {
            // Kết nối mạng đến Test Server
            Socket socket = new Socket("localhost", TEST_PORT);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // Thực hiện chuỗi hành động: Gửi gói đăng nhập giả lập
            Message loginMsg = new Message();
            loginMsg.setAction("LOGIN");
            loginMsg.setData(new Bidder("id_" + username, username, username + "@gmail.com", "password_hash", 100000000));
            out.writeObject(loginMsg);
            out.flush();

            // Chờ hiệu lệnh xuất phát đồng bộ
            startLatch.await();

            // Gửi gói đặt giá
            Message bidMsg = new Message();
            bidMsg.setAction("PLACE_BID");
            bidMsg.setDataMap(new java.util.HashMap<>());
            bidMsg.setData("auc_123"); // ID phiên đấu giá
            bidMsg.setData("amount", amount); // Số tiền đặt

            out.writeObject(bidMsg);
            out.flush();

            // Thiết lập Timeout nhận phản hồi tránh treo Client
            socket.setSoTimeout(3000);
            try {
                Message resp = (Message) in.readObject();

                // Đồng bộ hóa trường action/status theo file Message.java và AuctionServer.java hiện tại
                if ("BID_UPDATE".equals(resp.getAction()) || "SUCCESS".equals(resp.getStatus())) {
                    successCount.incrementAndGet();
                    if (resp.getData() instanceof BidTransaction tx) {
                        lastPriceRef.set(tx.getBidAmount());
                    } else {
                        lastPriceRef.set(amount);
                    }
                    System.out.printf("[%s] ✅ ĐẶT GIÁ THÀNH CÔNG: %s%n", username, resp.getMessage());
                } else {
                    failCount.incrementAndGet();
                    System.out.printf("[%s] ❌ Server từ chối lượt bid: %s%n", username, resp.getMessage());
                }

            } catch (java.net.SocketTimeoutException e) {
                System.out.println("[" + username + "] ⚠️ Hết hạn chờ (Timeout) phản hồi mạng.");
                failCount.incrementAndGet();
            }

        } catch (Exception e) {
            System.err.println("[" + username + "] Xảy ra ngoại lệ hệ thống: " + e.getMessage());
            failCount.incrementAndGet();
        } finally {
            doneLatch.countDown();
        }
    }

    private static void check(String description, boolean condition) {
        System.out.println((condition ? "✅ ĐẠT (PASS)" : "❌ THẤT BẠI (FAIL)") + ": " + description);
    }
}