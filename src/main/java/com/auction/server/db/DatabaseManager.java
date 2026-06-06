package com.auction.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


public class DatabaseManager {

    private static final String DB_HOST    = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME    = "auction_db";
    private static final String DB_OPTIONS = "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    private static final String USER = "root";
    private static final String PASS = "";

    private static DatabaseManager instance;
    private String dbUrl;

    private DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Bước 1: kết nối không có DB để tạo database trước
            Connection initConn = DriverManager.getConnection(DB_HOST + DB_OPTIONS, USER, PASS);
            initDatabase(initConn);
            initConn.close();
            // Bước 2: URL chính thức sau khi DB đã tồn tại
            this.dbUrl = DB_HOST + DB_NAME + DB_OPTIONS;
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
            return DriverManager.getConnection(dbUrl, USER, PASS);
        } catch (SQLException e) {
            throw new RuntimeException("[DB] Không thể tạo connection: " + e.getMessage(), e);
        }
    }

    private void initDatabase(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            stmt.execute("USE " + DB_NAME);
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1;");
            createTables(stmt);
        }
    }

    private void createTables(Statement stmt) throws SQLException {
        // 1. Bảng users — có cột frozen_balance để lưu tiền đang giữ khi bidder đặt cọc
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

        // 2. Bảng items — image_data cho phép NULL để tránh lỗi khi không upload ảnh
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS items (
                id VARCHAR(50) PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                base_price DOUBLE NOT NULL,
                category VARCHAR(50) NOT NULL,
                seller_id VARCHAR(50) NOT NULL,
                image_data LONGTEXT NULL DEFAULT NULL,
                created_at DATETIME NOT NULL,
                updated_at DATETIME NOT NULL,
                FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB;
        """);


        // 3. Bảng auctions — status dùng VARCHAR(30) để lưu các giá trị enum Java
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS auctions (
                id VARCHAR(50) PRIMARY KEY,
                item_id VARCHAR(50) NOT NULL,
                seller_id VARCHAR(50) NOT NULL,
                status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
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
                bid_amount DOUBLE NOT NULL,
                is_winning TINYINT(1) NOT NULL DEFAULT 0,
                timestamp DATETIME NOT NULL,
                FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
                FOREIGN KEY (bidder_id) REFERENCES users(id) ON DELETE CASCADE
            ) ENGINE=InnoDB;
        """);

        System.out.println("[DB] Đã khởi tạo cấu trúc bảng thành công.");
        migrateSchema(stmt);
    }


    private void migrateSchema(Statement stmt) {
        try { stmt.execute("ALTER TABLE users ADD COLUMN frozen_balance DOUBLE NOT NULL DEFAULT 0.0"); }
        catch (java.sql.SQLException ignored) {} // Đã tồn tại thì bỏ qua

        // [FIX 2] Đảm bảo image_data cho phép NULL để Seller tạo phiên không có ảnh không bị lỗi
        try { stmt.execute("ALTER TABLE items MODIFY COLUMN image_data LONGTEXT NULL DEFAULT NULL"); }
        catch (java.sql.SQLException ignored) {}

        // Giữ lại các migration c  ũ
        try { stmt.execute("ALTER TABLE items MODIFY COLUMN category VARCHAR(50) NOT NULL"); }
        catch (java.sql.SQLException ignored) {}

        try { stmt.execute("ALTER TABLE auctions MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'OPEN'"); }
        catch (java.sql.SQLException ignored) {}

        // Xoa bo trang thai PENDING_APPROVAL: chuyen tat ca phien cho duyet sang OPEN
        try {
            int updated = stmt.executeUpdate(
                    "UPDATE auctions SET status = 'OPEN' WHERE status = 'PENDING_APPROVAL'"
            );
            if (updated > 0)
                System.out.println("[DB] Migration: da chuyen " + updated + " phien PENDING_APPROVAL -> OPEN.");
        } catch (java.sql.SQLException e) {
            System.err.println("[DB] Migration PENDING->OPEN loi: " + e.getMessage());
        }

        System.out.println("[DB] Migration schema OK.");
    }

    public void close() {
        System.out.println("[DB] DatabaseManager closed.");
    }
}