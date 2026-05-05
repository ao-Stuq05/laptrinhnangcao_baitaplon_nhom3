package com.auction.server.db;

import com.auction.server.db.UserDAO;
import com.auction.shared.model.Bidder;
import java.sql.SQLException;
import java.util.UUID;

public class TestSaveUser {
    public static void main(String[] args) {
        System.out.println("=== THỬ NGHIỆM LƯU ĐỐI TƯỢNG USER VÀO MYSQL ===");

        // 1. Khởi tạo DAO
        UserDAO userDAO = new UserDAO();

        // 2. Tạo một đối tượng Bidder mẫu (Dùng dữ liệu giả)
        String randomId = UUID.randomUUID().toString();
        Bidder testBidder = new Bidder(
            randomId, 
            "test_user_" + System.currentTimeMillis(), // Username không trùng
            "email_" + System.currentTimeMillis() + "@gmail.com", 
            "password123", 
            5000.0 // Số dư ví dụ
        );

        try {
            // 3. Gọi hàm save() từ UserDAO để lưu đối tượng vào DB
            System.out.println("[...] Đang lưu user: " + testBidder.getUsername());
            userDAO.save(testBidder);
            System.out.println("[SUCCESS] Đã lưu đối tượng vào bảng 'users' thành công!");

            // 4. Kiểm tra lại bằng cách tìm theo Username vừa tạo
            userDAO.findByUsername(testBidder.getUsername()).ifPresent(user -> {
                System.out.println("[CHECK] Đã tìm thấy user trong DB: " + user.getUsername());
                System.out.println("[CHECK] ID: " + user.getId());
                System.out.println("[CHECK] Role: " + user.getRole());
            });

        } catch (SQLException e) {
            System.err.println("[FAIL] Lỗi khi lưu dữ liệu: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[FAIL] Lỗi hệ thống: " + e.getMessage());
        }
    }
}