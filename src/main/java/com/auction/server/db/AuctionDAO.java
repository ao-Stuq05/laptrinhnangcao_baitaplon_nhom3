package com.auction.server.db;

import com.auction.shared.model.Auction;
import com.auction.shared.model.AuctionStatus;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Item;
import com.auction.shared.model.Seller;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AuctionDAO — Data Access Object cho bảng auctions.
 *
 * SKELETON — các method đã có đủ SQL và logic,
 * phần mapRowToAuction() cần UserDAO + ItemDAO để load liên kết.
 */
public class AuctionDAO {

    private final Connection conn;
    private final UserDAO userDAO;
    private final ItemDAO itemDAO;

    public AuctionDAO() {
        this.conn    = DatabaseManager.getInstance().getConnection();
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
    }

    // ── CREATE ────────────────────────────────────────────────

    public void save(Auction auction) throws SQLException {
        String sql = """
            INSERT INTO auctions
              (id, item_id, seller_id, status, current_price,
               start_time, end_time, winner_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getItem().getId());
            ps.setString(3, auction.getSeller().getId());
            ps.setString(4, auction.getStatus().name());
            ps.setDouble(5, auction.getCurrentPrice());
            ps.setString(6, auction.getStartTime().toString());
            ps.setString(7, auction.getEndTime().toString());

            // winner_id có thể null (chưa có winner)
            if (auction.getWinner() != null) {
                ps.setString(8, auction.getWinner().getId());
            } 
            else {
                ps.setNull(8, Types.VARCHAR);
            }

            ps.setString(9,  auction.getCreatedAt().toString());
            ps.setString(10, auction.getUpdatedAt().toString());
            ps.executeUpdate();
            System.out.println("[AuctionDAO] Đã lưu phiên: " + auction.getId());
        }
    }

    // ── READ ──────────────────────────────────────────────────

    public Optional<Auction> findById(String id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRowToAuction(rs));
        }
        return Optional.empty();
    }

    /** Lấy tất cả phiên đang OPEN hoặc RUNNING */
    public List<Auction> findActive() throws SQLException {
        String sql = """
            SELECT * FROM auctions
            WHERE status IN ('OPEN', 'RUNNING')
            ORDER BY end_time ASC
        """;
        return queryList(sql);
    }

    /** Lấy tất cả phiên của 1 Seller */
    public List<Auction> findBySeller(String sellerId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE seller_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            ResultSet rs = ps.executeQuery();
            List<Auction> result = new ArrayList<>();
            while (rs.next()) result.add(mapRowToAuction(rs));
            return result;
        }
    }

    /** Lấy tất cả phiên đấu giá */
    public List<Auction> findAll() throws SQLException {
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";
        return queryList(sql);
    }

    // ── UPDATE ────────────────────────────────────────────────

    /** Cập nhật status + current_price + winner sau mỗi bid hoặc khi đóng phiên */
    public void updateStatus(String auctionId, AuctionStatus status,
                              double currentPrice, String winnerId) throws SQLException {
        String sql = """
            UPDATE auctions
            SET status = ?, current_price = ?, winner_id = ?, updated_at = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setDouble(2, currentPrice);
            if (winnerId != null) ps.setString(3, winnerId);
            else ps.setNull(3, Types.VARCHAR);
            ps.setString(4, LocalDateTime.now().toString());
            ps.setString(5, auctionId);
            ps.executeUpdate();
        }
    }

    /** Cập nhật endTime — dùng cho Anti-sniping */
    public void updateEndTime(String auctionId, LocalDateTime newEndTime) throws SQLException {
        String sql = "UPDATE auctions SET end_time = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newEndTime.toString());
            ps.setString(2, LocalDateTime.now().toString());
            ps.setString(3, auctionId);
            ps.executeUpdate();
        }
    }

    /** Cập nhật toàn bộ auction object */
    public void update(Auction auction) throws SQLException {
        String sql = """
            UPDATE auctions
            SET status = ?, current_price = ?, winner_id = ?, end_time = ?, updated_at = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getStatus().name());
            ps.setDouble(2, auction.getCurrentPrice());
            if (auction.getWinner() != null) {
                ps.setString(3, auction.getWinner().getId());
            } else {
                ps.setNull(3, Types.VARCHAR);
            }
            ps.setString(4, auction.getEndTime().toString());
            ps.setString(5, LocalDateTime.now().toString());
            ps.setString(6, auction.getId());
            ps.executeUpdate();
            System.out.println("[AuctionDAO] Đã cập nhật phiên: " + auction.getId());
        }
    }

    // ── Helper ────────────────────────────────────────────────

    private List<Auction> queryList(String sql) throws SQLException {
        List<Auction> result = new ArrayList<>();
        try (Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) result.add(mapRowToAuction(rs));
        }
        return result;
    }


    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        String id           = rs.getString("id");
        String itemId       = rs.getString("item_id");
        String sellerId     = rs.getString("seller_id");
        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));
        double currentPrice = rs.getDouble("current_price");
        LocalDateTime start = LocalDateTime.parse(rs.getString("start_time"));
        LocalDateTime end   = LocalDateTime.parse(rs.getString("end_time"));
        String winnerId     = rs.getString("winner_id");
        Item item     = itemDAO.findById(itemId).orElseThrow();
        Seller seller = (Seller) userDAO.findById(sellerId).orElseThrow();
        Bidder winner = winnerId != null ? (Bidder) userDAO.findById(winnerId).orElse(null) : null;
        return new Auction(id, item, seller, status, currentPrice, start, end, null, winner);
    }
}
