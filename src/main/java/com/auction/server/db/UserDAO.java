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

    private final Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────

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

            if (user instanceof Seller seller) {
                ps.setString(7, seller.getShopName());
            } else {
                ps.setNull(7, Types.VARCHAR);
            }

            if (user instanceof Bidder bidder) {
                ps.setDouble(8, bidder.getBalance());
            } else {
                ps.setDouble(8, 0.0);
            }

            ps.setTimestamp(9,  Timestamp.valueOf(user.getCreatedAt()));
            ps.setTimestamp(10, Timestamp.valueOf(user.getUpdatedAt()));

            ps.executeUpdate();
            System.out.println("[UserDAO] Đã lưu user: " + user.getUsername());
        }
    }

    // ── READ ──────────────────────────────────────────────────

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRowToUser(rs));
        }
        return Optional.empty();
    }

    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRowToUser(rs));
        }
        return Optional.empty();
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> result = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(mapRowToUser(rs));
        }
        return result;
    }

    public boolean existsByUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // ── UPDATE ────────────────────────────────────────────────

    public void updateActive(String userId, boolean isActive) throws SQLException {
        String sql = "UPDATE users SET is_active = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, isActive ? 1 : 0);
            ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(3, userId);
            ps.executeUpdate();
        }
    }

    public void updateBalance(String bidderId, double newBalance) throws SQLException {
        String sql = "UPDATE users SET balance = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(3, bidderId);
            ps.executeUpdate();
        }
    }

    /** Cập nhật frozen_balance cho bidder */
    public void updateFrozenBalance(String bidderId, double newFrozen) throws SQLException {
        String sql = "UPDATE users SET frozen_balance = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newFrozen);
            ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(3, bidderId);
            ps.executeUpdate();
        }
    }

    /** Cập nhật cả balance và frozen_balance cùng lúc (dùng khi kết thúc phiên) */
    public void updateBalanceAndFrozen(String bidderId, double newBalance, double newFrozen) throws SQLException {
        String sql = "UPDATE users SET balance = ?, frozen_balance = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setDouble(2, newFrozen);
            ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(4, bidderId);
            ps.executeUpdate();
        }
    }

    /** MỚI: Cập nhật email và password của user từ màn hình Hồ sơ */
    public void updateProfile(String userId, String newEmail, String newPassword) throws SQLException {
        if (newPassword != null && !newPassword.isEmpty()) {
            String sql = "UPDATE users SET email = ?, password_hash = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newEmail);
                ps.setString(2, newPassword); // production: hash bằng BCrypt trước
                ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
                ps.setString(4, userId);
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE users SET email = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newEmail);
                ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));
                ps.setString(3, userId);
                ps.executeUpdate();
            }
        }
        System.out.println("[UserDAO] updateProfile OK: " + userId);
    }

    // ── Helper ────────────────────────────────────────────────

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