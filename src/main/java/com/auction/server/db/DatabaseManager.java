
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton quản lý kết nối đến SQLite database.
 * Chỉ có 1 instance duy nhất trong toàn bộ ứng dụng.
 */
public class DatabaseManager {

    // Đường dẫn đến file database — tạo trong thư mục data/
    private static final String DB_URL = "jdbc:sqlite:data/auction.db";

    // Instance duy nhất — static để dùng chung toàn app
    private static DatabaseManager instance;

    // Connection duy nhất đến SQLite
    private Connection connection;

    // Constructor private — bên ngoài không thể new DatabaseManager()
    private DatabaseManager() {
        try {
            // Tạo thư mục data/ nếu chưa có
            new java.io.File("data").mkdirs();

            // Mở kết nối đến file SQLite (tự tạo file nếu chưa tồn tại)
            connection = DriverManager.getConnection(DB_URL);

            // Bật foreign key support (SQLite tắt mặc định!)
            connection.createStatement()
                      .execute("PRAGMA foreign_keys = ON");

            System.out.println("[DB] Kết nối SQLite thành công: " + DB_URL);

            // Tạo tất cả bảng ngay khi khởi động
            createTables();

        } catch (SQLException e) {
            throw new RuntimeException("Không thể kết nối database: " + e.getMessage(), e);
        }
    }

    // Singleton getInstance() — thread-safe với synchronized
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Trả về Connection để DAO dùng
    public Connection getConnection() {
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