package com.auction.server.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton quản lý kết nối đến SQLite database.
 * Chỉ có 1 instance duy nhất trong toàn bộ ứng dụng.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:data/auction.db";// Đường dẫn đến file SQLite (tự tạo nếu chưa tồn tại)
    private static DatabaseManager instance;// Singleton instance
    private Connection connection;  // Connection duy nhất đến SQLite

    // private constructor để ngăn tạo instance từ bên ngoài
    private DatabaseManager() {
        try {
            new java.io.File("data").mkdirs();// tao folder data nếu chưa tồn tại
            connection = DriverManager.getConnection(DB_URL);// Kết nối đến SQLite database (tự tạo file nếu chưa tồn tại)
            connection.createStatement()// bật foreign key support (SQLite mặc định tắt)
                      .execute("PRAGMA foreign_keys = ON");
            System.out.println("[DB] Kết nối SQLite thành công: " + DB_URL);
            createTables();// Tạo bảng nếu chưa tồn tại
        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối database: " + e.getMessage(), e);
        }
    }

    // Singleton getInstance() — thread-safe với synchronized( nếu như có 2 luồng vào cùng lúc không khởi tạo 2 instance)
    public static DatabaseManager getInstance() {
    if (instance == null) {
        synchronized (DatabaseManager.class) {
            if (instance == null) {
                instance = new DatabaseManager();
            }
        }
    }
    return instance;
}
    public Connection getConnection() {
        // Trả về Connection để DAO dùng
        return connection;
    }

    // Tạo tất cả bảng nếu chưa tồn tại
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();
        // Bảng users
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id            TEXT PRIMARY KEY,
                username      TEXT NOT NULL UNIQUE,
                email         TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                role          TEXT NOT NULL CHECK(role IN ('BIDDER','SELLER','ADMIN')),
                is_active     INTEGER NOT NULL DEFAULT 1,
                shop_name     TEXT,
                balance       REAL NOT NULL DEFAULT 0.0,
                created_at    TEXT NOT NULL,
                updated_at    TEXT NOT NULL
            )
        """);
        // Bảng items
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS items (
                id          TEXT PRIMARY KEY,
                name        TEXT NOT NULL,
                description TEXT,
                base_price  REAL NOT NULL CHECK(base_price > 0),
                category    TEXT NOT NULL CHECK(category IN ('ELECTRONICS','ART','VEHICLE')),
                seller_id   TEXT NOT NULL,
                created_at  TEXT NOT NULL,
                updated_at  TEXT NOT NULL,
                FOREIGN KEY (seller_id) REFERENCES users(id)
            )
        """);
        // Bảng auctions
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS auctions (
                id            TEXT PRIMARY KEY,
                item_id       TEXT NOT NULL,
                seller_id     TEXT NOT NULL,
                status        TEXT NOT NULL DEFAULT 'OPEN',
                current_price REAL NOT NULL,
                start_time    TEXT NOT NULL,
                end_time      TEXT NOT NULL,
                winner_id     TEXT,
                created_at    TEXT NOT NULL,
                updated_at    TEXT NOT NULL,
                FOREIGN KEY (item_id)    REFERENCES items(id),
                FOREIGN KEY (seller_id)  REFERENCES users(id),
                FOREIGN KEY (winner_id)  REFERENCES users(id)
            )
        """);
        // Bảng bid_transactions
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS bid_transactions (
                id         TEXT PRIMARY KEY,
                auction_id TEXT NOT NULL,
                bidder_id  TEXT NOT NULL,
                bid_amount REAL NOT NULL CHECK(bid_amount > 0),
                is_winning INTEGER NOT NULL DEFAULT 0,
                timestamp  TEXT NOT NULL,
                FOREIGN KEY (auction_id) REFERENCES auctions(id),
                FOREIGN KEY (bidder_id)  REFERENCES users(id)
            )
        """);

        System.out.println("[DB] Tất cả bảng đã sẵn sàng.");
        stmt.close();
    }
    // Đóng kết nối khi server tắt
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Đã đóng kết nối database.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Lỗi khi đóng: " + e.getMessage());
        }
    }
}