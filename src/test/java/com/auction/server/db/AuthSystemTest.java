package com.auction.server.db;

import com.auction.shared.model.Bidder;
import com.auction.shared.model.User;
import com.auction.shared.model.PasswordUtil; // Giả định class hash của bạn ở đây
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class AuthSystemTest {
    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA HỆ THỐNG: REGISTER & LOGIN (WITH BCRYPT) ===");

        UserDAO userDAO = new UserDAO();
        
        // 1. Dữ liệu đầu vào giả lập từ UI
        String rawPassword = "mySecretPassword123";
        String username = "test_user_" + System.currentTimeMillis();
        String email = "auth_test_" + System.currentTimeMillis() + "@gmail.com";

        try {
            // --- BƯỚC 1: TEST REGISTER (ĐĂNG KÝ) ---
            System.out.println("\n[1] Đang thực hiện đăng ký...");
            
            // Thực hiện Hash mật khẩu trước khi lưu vào đối tượng User
            // Giả sử hàm của bạn là PasswordUtil.hash(password)
            String hashedPassword = PasswordUtil.hash(rawPassword); 

            Bidder newUser = new Bidder(
                UUID.randomUUID().toString(),
                username,
                email,
                hashedPassword, // Lưu mật khẩu đã mã hóa vào DB
                0.0
            );
            
            userDAO.save(newUser);
            System.out.println("[SUCCESS] Đã lưu User với mật khẩu BCrypt vào MySQL.");

            // --- BƯỚC 2: TEST LOGIN (ĐĂNG NHẬP) ---
            System.out.println("\n[2] Đang thực hiện đăng nhập...");
            
            // Tìm user trong DB theo username
            Optional<User> foundUser = userDAO.findByUsername(username);

            if (foundUser.isPresent()) {
                User dbUser = foundUser.get();
                String storedHash = dbUser.getPasswordHash();

                // Dùng BCrypt để kiểm tra mật khẩu thô và mật khẩu đã hash trong DB
                // Giả sử hàm của bạn là PasswordUtil.verify(raw, hash)
                boolean isMatch = PasswordUtil.verify(rawPassword, storedHash);

                if (isMatch) {
                    System.out.println("[SUCCESS] LOGIN THÀNH CÔNG!");
                    System.out.println(" -> User ID: " + dbUser.getId());
                    System.out.println(" -> Balance: " + ((Bidder)dbUser).getBalance());
                } else {
                    System.err.println("[FAIL] LOGIN THẤT BẠI: Mật khẩu không khớp với Hash!");
                }
            } else {
                System.err.println("[FAIL] LOGIN THẤT BẠI: Username không tồn tại.");
            }

        } catch (SQLException e) {
            System.err.println("[ERROR] Lỗi SQL: " + e.getMessage());
        }
    }
}