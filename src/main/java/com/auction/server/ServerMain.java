package com.auction.server;

import com.auction.server.network.AuctionServer;

public class ServerMain {
    public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG ===");

        // Khởi tạo và chạy Server ở cổng 1234
        AuctionServer server = new AuctionServer(1234);
        server.startServer();
    }
}