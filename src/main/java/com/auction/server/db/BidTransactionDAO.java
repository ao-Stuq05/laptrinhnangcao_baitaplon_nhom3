package com.auction.server.db;

import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Bidder;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    private final Connection conn;
    private final UserDAO userDAO;

    public BidTransactionDAO() {
        this.conn    = DatabaseManager.getInstance().getConnection();
        this.userDAO = new UserDAO();
    }

    /** Lưu 1 BidTransaction mới vào DB */
    public void save(BidTransaction bid) throws SQLException {
        String sql = """
            INSERT INTO bid_transactions (id, auction_id, bidder_id, bid_amount, is_winning, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bid.getId());
            ps.setString(2, bid.getAuctionId());
            ps.setString(3, bid.getBidder().getId());
            ps.setDouble(4, bid.getBidAmount());
            ps.setInt(5, bid.isWinning() ? 1 : 0);
            ps.setTimestamp(6, Timestamp.valueOf(bid.getTimestamp()));
            ps.executeUpdate();
        }
    }

    /** Lấy tất cả bid của 1 phiên, sắp xếp theo giá giảm dần */
    public List<BidTransaction> findByAuction(String auctionId) throws SQLException {
        String sql = """
            SELECT * FROM bid_transactions
            WHERE auction_id = ?
            ORDER BY bid_amount DESC, timestamp ASC
        """;
        List<BidTransaction> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    /** MỚI: Lấy tất cả bid của 1 Bidder, sắp xếp mới nhất trước — dùng cho tab "Lịch sử mua" */
    public List<BidTransaction> findByBidder(String bidderId) throws SQLException {
        String sql = """
            SELECT * FROM bid_transactions
            WHERE bidder_id = ?
            ORDER BY timestamp DESC
        """;
        List<BidTransaction> results = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
}