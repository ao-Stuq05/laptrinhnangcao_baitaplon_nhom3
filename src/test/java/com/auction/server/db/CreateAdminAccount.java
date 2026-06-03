package com.auction.server.db;

import com.auction.shared.model.Admin;
import com.auction.shared.model.PasswordUtil;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility để tạo tài khoản Admin.
 * Chạy file này để tạo admin account và lưu vào database.
 */
public class CreateAdminAccount {
    public static void main(String[] args) {

        // Thông tin admin
        String username = "Admindat";
        String email = "admindat123@system.com";
        String rawPassword = "12052007";
        String adminLevel = "SUPER";

        UserDAO userDAO = new UserDAO();

        try {
            // 1. Kiểm tra xem admin đã tồn tại chưa
            System.out.println("[1] Kiểm tra xem admin đã tồn tại...");
            if (userDAO.existsByUsername(username)) {
                System.out.println("⚠ Admin '" + username + "' đã tồn tại trong hệ thống!");
                System.out.println("\n📝 Thông tin đăng nhập hiện tại:");
                System.out.println("   Username: " + username);
                System.out.println("   Password: " + rawPassword);
                return;
            }

            // 2. Hash password
            System.out.println("[2] Mã hóa mật khẩu...");
            String hashedPassword = PasswordUtil.hash(rawPassword);
            System.out.println("    ✓ Hash thành công");

            // 3. Tạo Admin object
            System.out.println("[3] Tạo tài khoản Admin...");
            String adminId = UUID.randomUUID().toString();
            Admin admin = new Admin(adminId, username, email, hashedPassword, adminLevel);
            admin.setActive(true);
            System.out.println("    ✓ Admin object tạo thành công");

            // 4. Lưu vào database
            System.out.println("[4] Lưu vào database...");
            userDAO.save(admin);
            System.out.println("    ✓ Lưu thành công!");

            // 5. Kiểm tra lại
            System.out.println("[5] Xác nhận...");
            Optional<com.auction.shared.model.User> saved = userDAO.findByUsername(username);
            if (saved.isPresent()) {
                com.auction.shared.model.User user = saved.get();
                System.out.println("    ✓ Tìm thấy trong DB:");
                System.out.println("      • ID: " + user.getId());
                System.out.println("      • Username: " + user.getUsername());
                System.out.println("      • Email: " + user.getEmail());
                System.out.println("      • Role: " + user.getRole());
                System.out.println("      • Active: " + user.isActive());
            }

            // Hiển thị thông tin đăng nhập
            System.out.println("\n" + "=".repeat(45));
            System.out.println("✅ ADMIN ACCOUNT CREATED SUCCESSFULLY!");
            System.out.println("=".repeat(45));
            System.out.println("\n📝 Thông tin đăng nhập:");
            System.out.println("┌─────────────────────────────────────────┐");
            System.out.println("│ Username: " + String.format("%-28s │", username));
            System.out.println("│ Password: " + String.format("%-28s │", rawPassword));
            System.out.println("│ Email:    " + String.format("%-28s │", email));
            System.out.println("│ Role:     " + String.format("%-28s │", "ADMIN"));
            System.out.println("│ Level:    " + String.format("%-28s │", adminLevel));
            System.out.println("└─────────────────────────────────────────┘");
            System.out.println("\n💡 Sử dụng thông tin này để đăng nhập vào Admin Dashboard");

        } catch (SQLException e) {
            System.err.println("❌ Lỗi SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
