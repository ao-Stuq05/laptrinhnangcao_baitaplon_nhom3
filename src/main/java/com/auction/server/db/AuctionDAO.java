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
 */
public class AuctionDAO {

    private final UserDAO userDAO;
    private final ItemDAO itemDAO;

    public AuctionDAO() {
        this.userDAO = new UserDAO();
        this.itemDAO = new ItemDAO();
    }

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────

    public void save(Auction auction) throws SQLException {
        String sql = """
            INSERT INTO auctions
              (id, item_id, seller_id, status, current_price,
               start_time, end_time, winner_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        """;
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId());
            ps.setString(2, auction.getItem().getId());
            ps.setString(3, auction.getSeller().getId());
            ps.setString(4, auction.getStatus().name());
            ps.setDouble(5, auction.getCurrentPrice());
            ps.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(8, auction.getWinner() != null ? auction.getWinner().getId() : null);
            ps.executeUpdate();
            System.out.println("[AuctionDAO] Đã lưu phiên mới: " + auction.getId());
        }
    }

    // ── READ ──────────────────────────────────────────────────

    public Optional<Auction> findById(String id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAuction(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Auction> findAll() throws SQLException {
        return queryList("SELECT * FROM auctions ORDER BY created_at DESC");
    }

    /**
     * [BUG FIX] Trước đây dùng status = 'ACTIVE' nhưng enum Java không có giá trị ACTIVE.
     * Các phiên đang hoạt động có status là OPEN hoặc RUNNING.
     * Sửa lại đúng để server preload được phiên khi khởi động.
     */
    public List<Auction> findActive() throws SQLException {
        return queryList(
            "SELECT * FROM auctions WHERE status IN ('OPEN','RUNNING') ORDER BY end_time ASC"
        );
    }

    // ── UPDATE ────────────────────────────────────────────────

    public void updatePriceAndWinner(String auctionId, double nextPrice, String bidderId) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, winner_id = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nextPrice);
            ps.setString(2, bidderId);
            ps.setString(3, auctionId);
            ps.executeUpdate();
        }
    }

    public void updateStatus(String auctionId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auctions SET status = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, auctionId);
            ps.executeUpdate();
            System.out.println("[AuctionDAO] Đã cập nhật trạng thái phiên " + auctionId + " thành " + status);
        }
    }

    public void update(Auction auction) throws SQLException {
        String sql = """
            UPDATE auctions
            SET item_id = ?, seller_id = ?, status = ?, current_price = ?,
                start_time = ?, end_time = ?, winner_id = ?, updated_at = NOW()
            WHERE id = ?
        """;
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getItem().getId());
            ps.setString(2, auction.getSeller().getId());
            ps.setString(3, auction.getStatus().name());
            ps.setDouble(4, auction.getCurrentPrice());
            ps.setTimestamp(5, Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(6, Timestamp.valueOf(auction.getEndTime()));
            ps.setString(7, auction.getWinner() != null ? auction.getWinner().getId() : null);
            ps.setString(8, auction.getId());
            ps.executeUpdate();
            System.out.println("[AuctionDAO] Đã cập nhật phiên: " + auction.getId());
        }
    }

    public void updateEndTime(String auctionId, LocalDateTime endTime) throws SQLException {
        String sql = "UPDATE auctions SET end_time = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(endTime));
            ps.setString(2, auctionId);
            ps.executeUpdate();
            System.out.println("[AuctionDAO] Đã cập nhật thời gian kết thúc của phiên " + auctionId);
        }
    }

    // ── Helper & Refactored Methods ───────────────────────────

    /**
     * Cấu trúc dữ liệu thô (Raw Data) tạm thời để giải phóng ResultSet sớm,
     * tránh lỗi xung đột kết nối lồng nhau (Nested Queries).
     */
    private static class RawAuctionData {
        String id;
        String itemId;
        String sellerId;
        String statusStr;
        double currentPrice;
        LocalDateTime startTime;
        LocalDateTime endTime;
        String winnerId;
    }

    private List<Auction> queryList(String sql) throws SQLException {
        List<RawAuctionData> rawList = new ArrayList<>();

        // BƯỚC 1: Đọc hết ResultSet vào list thô, đóng Connection ngay
        try (Connection conn = getConn();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rawList.add(mapResultSetToRaw(rs));
            }
        }

        // BƯỚC 2: ResultSet và Connection cũ đã đóng. Tiến hành enrich từng record
        List<Auction> result = new ArrayList<>();
        for (RawAuctionData raw : rawList) {
            try {
                result.add(enrichRawToAuction(raw));
            } catch (Exception e) {
                // [FIX] Bỏ qua auction lỗi dữ liệu thay vì throw toàn bộ list
                System.err.println("[AuctionDAO] Bỏ qua phiên lỗi " + raw.id + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<Auction> findBySeller(String sellerId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE seller_id = ? ORDER BY created_at DESC";
        List<RawAuctionData> rawList = new ArrayList<>();

        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rawList.add(mapResultSetToRaw(rs));
                }
            }
        }

        List<Auction> result = new ArrayList<>();
        for (RawAuctionData raw : rawList) {
            try {
                result.add(enrichRawToAuction(raw));
            } catch (Exception e) {
                System.err.println("[AuctionDAO] Bỏ qua phiên lỗi " + raw.id + ": " + e.getMessage());
            }
        }
        return result;
    }

    private RawAuctionData mapResultSetToRaw(ResultSet rs) throws SQLException {
        RawAuctionData raw = new RawAuctionData();
        raw.id           = rs.getString("id");
        raw.itemId       = rs.getString("item_id");
        raw.sellerId     = rs.getString("seller_id");
        raw.statusStr    = rs.getString("status");
        raw.currentPrice = rs.getDouble("current_price");
        raw.startTime    = rs.getTimestamp("start_time").toLocalDateTime();
        raw.endTime      = rs.getTimestamp("end_time").toLocalDateTime();
        raw.winnerId     = rs.getString("winner_id");
        return raw;
    }

    private Auction enrichRawToAuction(RawAuctionData raw) throws SQLException {
        // [FIX] Bảo vệ khỏi status string không hợp lệ trong DB (ví dụ giá trị cũ 'ACTIVE')
        AuctionStatus status;
        try {
            status = AuctionStatus.valueOf(raw.statusStr);
        } catch (IllegalArgumentException e) {
            System.err.println("[AuctionDAO] Status không hợp lệ '" + raw.statusStr
                    + "' cho phiên " + raw.id + " — mặc định OPEN");
            status = AuctionStatus.OPEN;
        }

        Item item = itemDAO.findById(raw.itemId).orElseThrow(() ->
            new RuntimeException("Không tìm thấy Item ID: " + raw.itemId));
        Seller seller = (Seller) userDAO.findById(raw.sellerId).orElseThrow(() ->
            new RuntimeException("Không tìm thấy Seller ID: " + raw.sellerId));

        Bidder winner = raw.winnerId != null
                ? (Bidder) userDAO.findById(raw.winnerId).orElse(null)
                : null;

        return new Auction(raw.id, item, seller, status, raw.currentPrice,
                raw.startTime, raw.endTime, null, winner);
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        RawAuctionData raw = mapResultSetToRaw(rs);
        return enrichRawToAuction(raw);
    }
}
