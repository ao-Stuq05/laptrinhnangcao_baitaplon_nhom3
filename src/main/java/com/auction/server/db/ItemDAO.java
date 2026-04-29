package com.auction.server.db;
import com.auction.shared.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {

    private final Connection conn;
    private final UserDAO userDAO;

    public ItemDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
        this.userDAO = new UserDAO();
    }

    // ── CREATE ────────────────────────────────────────────────

    public void save(Item item) throws SQLException {
        String sql = """
            INSERT INTO items (id, name, description, base_price, category, seller_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setDouble(4, item.getBasePrice());
            ps.setString(5, item.getCategory());
            ps.setString(6, item.getSeller().getId());
            ps.setString(7, item.getCreatedAt().toString());
            ps.setString(8, item.getUpdatedAt().toString());
            ps.executeUpdate();
            System.out.println("[ItemDAO] Đã lưu item: " + item.getName());
        }
    }

    // ── READ ──────────────────────────────────────────────────

    public Optional<Item> findById(String id) throws SQLException {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRowToItem(rs));
        }
        return Optional.empty();
    }

    public List<Item> findBySeller(String sellerId) throws SQLException {
        String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRowToItem(rs));
        }
        return result;
    }

    public List<Item> findAll() throws SQLException {
        String sql = "SELECT * FROM items ORDER BY created_at DESC";
        List<Item> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) result.add(mapRowToItem(rs));
        }
        return result;
    }

    // ── UPDATE ────────────────────────────────────────────────

    public void update(Item item) throws SQLException {
        String sql = """
            UPDATE items 
            SET name = ?, description = ?, base_price = ?, updated_at = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setDouble(3, item.getBasePrice());
            ps.setString(4, LocalDateTime.now().toString());
            ps.setString(5, item.getId());
            ps.executeUpdate();
            System.out.println("[ItemDAO] Đã cập nhật item: " + item.getId());
        }
    }

    // ── DELETE ────────────────────────────────────────────────

    public void delete(String id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
            System.out.println("[ItemDAO] Đã xóa item: " + id);
        }
    }

    // ── MAP ROW TO ITEM ────────────────────────────────────────

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        double basePrice = rs.getDouble("base_price");
        String category = rs.getString("category");
        String sellerId = rs.getString("seller_id");

        // Load seller từ UserDAO
        Seller seller = (Seller) userDAO.findById(sellerId)
                .orElseThrow(() -> new SQLException("Không tìm thấy seller: " + sellerId));

        // Tạo đúng subclass dựa trên category
        return switch (category) {
            case "ELECTRONICS" -> new Electronics(id, name, description, basePrice, seller, 12); // default warranty
            case "ART" -> new Art(id, name, description, basePrice, seller, "Unknown", 2024);
            case "VEHICLE" -> new Vehicle(id, name, description, basePrice, seller, "Unknown", 0);
            default -> throw new SQLException("Category không hợp lệ: " + category);
        };
    }

}
    

