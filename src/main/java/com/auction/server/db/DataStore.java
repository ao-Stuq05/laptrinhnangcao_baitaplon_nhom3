package com.auction.server.db;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
import com.auction.shared.model.Seller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DataStore {

    private DataStore() {
    }


    public static void saveAuctions(List<Auction> auctions, String filePath) throws SQLException {
        saveAuctions(auctions);
    }

    public static void saveAuctions(List<Auction> auctions) throws SQLException {
        if (auctions == null) {
            throw new IllegalArgumentException("Auction list không được null");
        }

        // Lấy kết nối duy nhất từ DatabaseManager để đồng bộ trạng thái với các DAO
        Connection conn = DatabaseManager.getInstance().getConnection();
        boolean originalAutoCommit = conn.getAutoCommit();

        try {
            // Tắt Auto-Commit để bắt đầu một Transaction (Giao dịch)
            conn.setAutoCommit(false);

            AuctionDAO auctionDAO = new AuctionDAO();
            ItemDAO itemDAO = new ItemDAO();
            UserDAO userDAO = new UserDAO();

            // Bộ đệm trên RAM để lưu các ID đã xử lý trong lô dữ liệu hiện tại
            Set<String> processedUserIds = new HashSet<>();
            Set<String> processedItemIds = new HashSet<>();
            Set<String> processedAuctionIds = new HashSet<>();

            for (Auction auction : auctions) {
                if (auction == null) {
                    continue;
                }

                // Kiểm tra và lưu Seller của phiên đấu giá (Sử dụng UPSERT ngầm bên trong DAO)
                Seller seller = auction.getSeller();
                if (seller != null && !processedUserIds.contains(seller.getId())) {
                    // ĐÃ TỐI ƯU: Loại bỏ userDAO.findById(). Khó bị trùng lặp nhờ lệnh UPSERT trong MySQL
                    userDAO.save(seller); 
                    processedUserIds.add(seller.getId());
                }
                Item item = auction.getItem();
                if (item != null) {
                    Seller itemSeller = item.getSeller();
                    if (itemSeller != null && !processedUserIds.contains(itemSeller.getId())) {

                        userDAO.save(itemSeller);
                        processedUserIds.add(itemSeller.getId());
                    }

                    if (!processedItemIds.contains(item.getId())) {
                        // ĐÃ TỐI ƯU: Gọi thẳng hàm save dạng UPSERT
                        itemDAO.save(item);
                        processedItemIds.add(item.getId());
                    }
                }

                // Kiểm tra và lưu Người chiến thắng (Winner) nếu có
                if (auction.getWinner() != null && !processedUserIds.contains(auction.getWinner().getId())) {
                    // ĐÃ TỐI ƯU: Gọi thẳng hàm save dạng UPSERT
                    userDAO.save(auction.getWinner());
                    processedUserIds.add(auction.getWinner().getId());
                }

                // Lưu mới hoặc cập nhật thông tin phiên đấu giá (Auction)
                if (!processedAuctionIds.contains(auction.getId())) {
                    // ĐÃ TỐI ƯU: Gộp cả save và update làm một thông qua hàm upsert (hoặc hàm save mới)
                    auctionDAO.save(auction);
                    processedAuctionIds.add(auction.getId());
                }
            }
            // Xác nhận lưu toàn bộ dữ liệu vào DB nếu vòng lặp không xảy ra lỗi
            conn.commit();
        } catch (SQLException e) {
            // Hủy bỏ toàn bộ các thay đổi nếu có bất kỳ lỗi nào xảy ra giữa chừng
            conn.rollback();
            throw e;
        } finally {
            // Khôi phục lại trạng thái ban đầu của kết nối để không ảnh hưởng luồng khác
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    @Deprecated
    public static List<Auction> loadAuctions(String filePath) throws SQLException {
        return loadAuctions();
    }

    public static List<Auction> loadAuctions() throws SQLException {
        return new AuctionDAO().findAll();
    }
}