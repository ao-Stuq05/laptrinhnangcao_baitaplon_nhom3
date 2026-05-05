package com.auction.server.db;

import com.auction.server.db.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("=== BẮT ĐẦU KIỂM TRA TOÀN DIỆN MYSQL ===");

        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            Connection conn = dbManager.getConnection();

            if (conn != null && !conn.isClosed()) {
                System.out.println("[SUCCESS] Kết nối MySQL ổn định.");

                // 1. Kiểm tra cấu trúc 4 bảng
                checkTables(conn);

                // 2. Chạy thử nghiệm CRUD (Thêm và Đọc dữ liệu)
                testUserCRUD(conn);

            }
        } catch (Exception e) {
            System.err.println("[FAIL] Lỗi nghiêm trọng: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("=== KẾT THÚC KIỂM TRA ===");
        }
    }

    private static void checkTables(Connection conn) throws SQLException {
        String[] tables = {"users", "items", "auctions", "bid_transactions"};
        DatabaseMetaData meta = conn.getMetaData();
        for (String table : tables) {
            try (ResultSet rs = meta.getTables(null, null, table, null)) {
                if (rs.next()) {
                    System.out.println("[OK] Bảng '" + table + "': Sẵn sàng.");
                } else {
                    System.err.println("[!] Bảng '" + table + "': Thiếu!");
                }
            }
        }
    }

    private static void testUserCRUD(Connection conn) throws SQLException {
        System.out.println("\n--- Đang thử nghiệm thao tác dữ liệu ---");
        
        String testId = UUID.randomUUID().toString();
        String now = LocalDateTime.now().toString();

        // Thử chèn một User mới
        String insertSql = "INSERT INTO users (id, username, email, password_hash, role, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, testId);
            pstmt.setString(2, "testuser_" + System.currentTimeMillis());
            pstmt.setString(3, "test_" + System.currentTimeMillis() + "@gmail.com");
            pstmt.setString(4, "hashed_password");
            pstmt.setString(5, "BIDDER");
            pstmt.setString(6, now);
            pstmt.setString(7, now);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("[OK] Insert dữ liệu mẫu thành công.");
            }
        }

        // Thử đọc lại dữ liệu vừa chèn
        String selectSql = "SELECT username, role FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setString(1, testId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("[OK] Truy vấn thành công. Username tìm thấy: " + rs.getString("username"));
                }
            }
        }
        
        System.out.println("--- Hoàn tất thử nghiệm dữ liệu ---\n");
    }
}