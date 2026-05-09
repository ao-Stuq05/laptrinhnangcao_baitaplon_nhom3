package com.auction.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // URL kết nối MySQL XAMPP
    private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            // Kết nối tới MySQL
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
            
            // Bật kiểm tra khóa ngoại
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
            }

            System.out.println("[DB] Kết nối thành công đến MySQL (XAMPP)");
            createTables();

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
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
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
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL
                ) ENGINE=InnoDB;
            """);

            // 2. Bảng items
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS items (
                    id VARCHAR(50) PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    description TEXT,
                    base_price DOUBLE NOT NULL CHECK(base_price > 0),
                    category ENUM('ELECTRONICS','ART','VEHICLE') NOT NULL,
                    seller_id VARCHAR(50) NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL,
                    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """);

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

            System.out.println("[DB] Đã khởi tạo đầy đủ 4 bảng thành công.");
        }
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