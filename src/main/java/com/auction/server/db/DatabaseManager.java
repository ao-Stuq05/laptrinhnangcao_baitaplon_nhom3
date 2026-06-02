package com.auction.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager quản lý kết nối MySQL sử dụng Singleton Pattern.
 * Đảm bảo database và các bảng được khởi tạo tự động.
 */
public class DatabaseManager {
    // Cấu hình kết nối - Lưu ý: dbname được tách ra để hỗ trợ tự động tạo DB nếu chưa có
    private static final String DB_HOST = "jdbc:mysql://192.168.2.244";
    private static final String DB_NAME = "auction_db";
    private static final String DB_OPTIONS = "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "auction_db";
    private static final String PASS = "12052007";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            // 1. Nạp Driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Kết nối tới MySQL (không chỉ định DB trước để tránh lỗi nếu DB chưa tồn tại)
            connection = DriverManager.getConnection(DB_HOST + DB_OPTIONS, USER, PASS);

            // 3. Khởi tạo Database và Bảng
            initDatabase();

            System.out.println("[DB] Hệ thống cơ sở dữ liệu đã sẵn sàng.");

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Không tìm thấy MySQL Driver: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kết nối MySQL: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            // Kiểm tra nếu kết nối bị đóng thì khởi tạo lại (tránh lỗi kết nối treo)
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_HOST + DB_NAME + DB_OPTIONS, USER, PASS);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    private void initDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Tạo database nếu chưa có
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            // Sử dụng database
            stmt.execute("USE " + DB_NAME);

            // Bật kiểm tra khóa ngoại
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");

            // Khởi tạo các bảng
            createTables(stmt);
        }
    }

    private void createTables(Statement stmt) throws SQLException {
        // 1. Bảng users
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(50) PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                email VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role ENUM('BIDDER','SELLER','ADMIN') NOT NULL,
                is_active TINYINT(1) NOT NULL DEFAULT 1,
                shop_name VARCHAR(100),
                balance DOUBLE NOT NULL DEFAULT 0.0,
                frozen_balance DOUBLE NOT NULL DEFAULT 0.0,
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL
            ) ENGINE=InnoDB;
        """);
        // Migration: thêm frozen_balance nếu bảng đã tồn tại từ phiên bản cũ
        try {
            stmt.execute("ALTER TABLE users ADD COLUMN frozen_balance DOUBLE NOT NULL DEFAULT 0.0");
        } catch (java.sql.SQLException ignored) {
            // Cột đã tồn tại → bỏ qua
        }

        // 2. Bảng items
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS items (
                id VARCHAR(50) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                base_price DOUBLE NOT NULL CHECK(base_price > 0),
                category ENUM('ELECTRONICS','ART','VEHICLE') NOT NULL,
                seller_id VARCHAR(50) NOT NULL,
                image_data MEDIUMTEXT,
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL,
                FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB;
        """);
        // Thêm cột image_data nếu bảng đã tồn tại từ phiên bản cũ
        try {
            stmt.execute("ALTER TABLE items ADD COLUMN image_data MEDIUMTEXT");
        } catch (java.sql.SQLException ignored) {
            // Cột đã tồn tại → bỏ qua
        }

        // 3. Bảng auctions
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS auctions (
                id VARCHAR(50) PRIMARY KEY,
                item_id VARCHAR(50) NOT NULL,
                seller_id VARCHAR(50) NOT NULL,
                status ENUM('OPEN', 'CLOSED', 'CANCELLED', 'RUNNING', 'FINISHED', 'PAID') DEFAULT 'OPEN',
                current_price DOUBLE NOT NULL,
                start_time DATETIME NOT NULL,
                end_time DATETIME NOT NULL,
                winner_id VARCHAR(50),
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL,
                FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL
            ) ENGINE=InnoDB;
        """);

        // 4. Bảng bid_transactions
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS bid_transactions (
                id VARCHAR(50) PRIMARY KEY,
                auction_id VARCHAR(50) NOT NULL,
                bidder_id VARCHAR(50) NOT NULL,
                bid_amount DOUBLE NOT NULL CHECK(bid_amount > 0),
                is_winning TINYINT(1) NOT NULL DEFAULT 0,
                timestamp DATETIME NOT NULL,
                FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB;
        """);

        System.out.println("[DB] Đã khởi tạo cấu trúc bảng thành công.");
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Đã đóng kết nối MySQL.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Lỗi khi đóng: " + e.getMessage());
        }
    }
}