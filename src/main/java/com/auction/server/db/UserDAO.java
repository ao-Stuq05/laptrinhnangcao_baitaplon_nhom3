package com.auction.server.db;

import com.auction.shared.model.Admin;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Seller;
import com.auction.shared.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class UserDAO {

    // Lấy connection từ DatabaseManager Singleton
    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────

    /**
     * Lưu user mới vào database.
     *
     * @param user Bidder, Seller hoặc Admin cần lưu
     * @throws SQLException nếu username/email đã tồn tại (UNIQUE constraint)
     */
    public void save(User user) throws SQLException {
        String sql = """
            INSERT INTO users (id, username, email, password_hash, role,
                               is_active, shop_name, balance, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole());
            ps.setInt   (6, user.isActive() ? 1 : 0);

            // shop_name chỉ có ở Seller, các role khác để NULL
            if (user instanceof Seller seller) {
                ps.setString(7, seller.getShopName());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            // balance chỉ có ở Bidder
            if (user instanceof Bidder bidder) {
                ps.setDouble(8, bidder.getBalance());
            } else {
                ps.setDouble(8, 0.0);
            }

            ps.setString(9,  user.getCreatedAt().toString());
            ps.setString(10, user.getUpdatedAt().toString());

            ps.executeUpdate();
            System.out.println("[UserDAO] Đã lưu user: " + user.getUsername());
        }
    }

    // ── READ ──────────────────────────────────────────────────

    /**
     * Tìm user theo username — dùng khi đăng nhập.
     *
     * @param username Tên đăng nhập
     * @return Optional.empty() nếu không tìm thấy
     */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Tìm user theo ID.
     *
     * @param id UUID của user
     */
    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToUser(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy tất cả user (Admin dùng).
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> result = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRowToUser(rs));
            }
        }
        return result;
    }

    /**
     * Kiểm tra username đã tồn tại chưa — dùng khi đăng ký.
     */
    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ── UPDATE ────────────────────────────────────────────────

    /**
     * Cập nhật trạng thái active của user (Admin ban/unban).
     */
    public void updateActive(String userId, boolean isActive) throws SQLException {
        String sql = "UPDATE users SET is_active = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, isActive ? 1 : 0);
            ps.setString(2, java.time.LocalDateTime.now().toString());
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Cập nhật số dư của Bidder sau khi thắng đấu giá.
     */
    public void updateBalance(String bidderId, double newBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, java.time.LocalDateTime.now().toString());
            ps.setString(3, bidderId);
            ps.executeUpdate();
        }
    }
    // ── Helper: map ResultSet → User object ──────────────────

    /**
     * Chuyển 1 dòng ResultSet thành đúng subclass (Bidder/Seller/Admin).
     * Dùng field "role" trong DB để biết tạo class nào.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        String id           = rs.getString("id");
        String username     = rs.getString("username");
        String email        = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        String role         = rs.getString("role");
        boolean isActive    = rs.getInt("is_active") == 1;

        return switch (role) {
            case "BIDDER" -> {
                double balance = rs.getDouble("balance");
                Bidder b = new Bidder(id, username, email, passwordHash, balance);
                b.setActive(isActive);
                yield b;
            }
            case "SELLER" -> {
                String shopName = rs.getString("shop_name");
                Seller s = new Seller(id, username, email, passwordHash, shopName, 5.0);
                s.setActive(isActive);
                yield s;
            }
            case "ADMIN" -> {
                Admin a = new Admin(id, username, email, passwordHash, "NORMAL");
                a.setActive(isActive);
                yield a;
            }
            default -> throw new SQLException("Role không hợp lệ trong DB: " + role);
        };
    }
}
