package com.auction.server.db;

import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Bidder;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    // conn field removed
    private final UserDAO userDAO;

    public BidTransactionDAO() {
        this.userDAO = new UserDAO();
    }

    private Connection getConn() {
        return DatabaseManager.getInstance().getConnection();
    }

    public void save(BidTransaction bid) throws SQLException {
        String sql = """
            INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, is_winning, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bid.getId());
            ps.setString(2, bid.getAuctionId());
            ps.setString(3, bid.getBidder().getId());
            ps.setDouble(4, bid.getBidAmount());
            ps.setInt(5, bid.isWinning() ? 1 : 0);
            ps.setTimestamp(6, Timestamp.valueOf(bid.getTimestamp()));
            ps.executeUpdate();
        }
    }
    public List<BidTransaction> findByAuction(String auctionId) throws SQLException {
        String sql = """
            SELECT * FROM bid_transactions
            WHERE auction_id = ?
            ORDER BY bid_amount DESC, timestamp ASC
        """;
        List<BidTransaction> results = new ArrayList<>();
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id         = rs.getString("id");
                String bidderId   = rs.getString("bidder_id");
                double bidAmount  = rs.getDouble("bid_amount");
                boolean isWinning = rs.getInt("is_winning") == 1;
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

                Bidder bidder = (Bidder) userDAO.findById(bidderId)
                        .orElseThrow(() -> new SQLException("Không tìm thấy bidder: " + bidderId));

                results.add(new BidTransaction(id, bidder, bidAmount, auctionId, timestamp, isWinning));
            }
        }
        return results;
    }

    /** Lấy tất cả bid của 1 Bidder, sắp xếp mới nhất trước */
    public List<BidTransaction> findByBidder(String bidderId) throws SQLException {
        String sql = """
            SELECT * FROM bid_transactions
            WHERE bidder_id = ?
            ORDER BY timestamp DESC
        """;
        List<BidTransaction> results = new ArrayList<>();
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String id         = rs.getString("id");
                String auctionId  = rs.getString("auction_id");
                double bidAmount  = rs.getDouble("bid_amount");
                boolean isWinning = rs.getInt("is_winning") == 1;
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

                Bidder bidder = (Bidder) userDAO.findById(bidderId)
                        .orElseThrow(() -> new SQLException("Không tìm thấy bidder: " + bidderId));

                results.add(new BidTransaction(id, bidder, bidAmount, auctionId, timestamp, isWinning));
            }
        }
        return results;
    }
    public List<BidTransaction> findByBidderWithItem(String bidderId, AuctionDAO auctionDAO) throws SQLException {
        String sql = """
            SELECT bt.id, bt.auction_id, bt.bid_amount, bt.is_winning, bt.timestamp,
                   i.name AS item_name, a.current_price AS final_price, a.status AS auction_status
            FROM bid_transactions bt
            LEFT JOIN auctions a ON bt.auction_id = a.id
            LEFT JOIN items i ON a.item_id = i.id
            WHERE bt.bidder_id = ?
            ORDER BY bt.timestamp DESC
        """;
        List<BidTransaction> results = new ArrayList<>();
        try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidderId);
            ResultSet rs = ps.executeQuery();

            Bidder bidder = (Bidder) userDAO.findById(bidderId)
                    .orElseThrow(() -> new SQLException("Không tìm thấy bidder: " + bidderId));

            while (rs.next()) {
                String id         = rs.getString("id");
                String auctionId  = rs.getString("auction_id");
                double bidAmount  = rs.getDouble("bid_amount");
                boolean isWinning = rs.getInt("is_winning") == 1;
                LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();

                // Dùng tên sản phẩm làm "label" để hiển thị trong UI
                String itemName = rs.getString("item_name");
                String displayId = (itemName != null && !itemName.isEmpty())
                        ? itemName : auctionId;

                results.add(new BidTransaction(id, bidder, bidAmount, displayId, timestamp, isWinning));
            }
        }
        return results;
    }
}
