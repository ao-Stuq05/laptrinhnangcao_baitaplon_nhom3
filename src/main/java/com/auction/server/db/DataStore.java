package com.auction.server.db;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
import com.auction.shared.model.Seller;

import java.sql.SQLException;
import java.util.List;

public final class DataStore {

    private DataStore() {
        // Không cho khởi tạo
    }

    /**
     * Lưu danh sách Auction vào cơ sở dữ liệu SQL.
     *
     * @param auctions danh sách auction cần lưu
     * @param filePath không còn sử dụng; lưu ý đây là API giữ ngược tương thích cũ.
     */
    public static void saveAuctions(List<Auction> auctions, String filePath) throws SQLException {
        if (auctions == null) {
            throw new IllegalArgumentException("Auction list không được null");
        }

        AuctionDAO auctionDAO = new AuctionDAO();
        ItemDAO itemDAO = new ItemDAO();
        UserDAO userDAO = new UserDAO();

        for (Auction auction : auctions) {
            if (auction == null) {
                continue;
            }

            Seller seller = auction.getSeller();
            if (seller != null && userDAO.findById(seller.getId()).isEmpty()) {
                userDAO.save(seller);
            }

            Item item = auction.getItem();
            if (item != null) {
                Seller itemSeller = item.getSeller();
                if (itemSeller != null && userDAO.findById(itemSeller.getId()).isEmpty()) {
                    userDAO.save(itemSeller);
                }
                if (itemDAO.findById(item.getId()).isEmpty()) {
                    itemDAO.save(item);
                }
            }

            if (auction.getWinner() != null && userDAO.findById(auction.getWinner().getId()).isEmpty()) {
                userDAO.save(auction.getWinner());
            }

            if (auctionDAO.findById(auction.getId()).isEmpty()) {
                auctionDAO.save(auction);
            } else {
                auctionDAO.update(auction);
            }
        }
    }

    public static void saveAuctions(List<Auction> auctions) throws SQLException {
        saveAuctions(auctions, null);
    }

    /**
     * Đọc danh sách Auction từ cơ sở dữ liệu SQL.
     *
     * @param filePath không còn sử dụng; giữ API cho tương thích.
     */
    public static List<Auction> loadAuctions(String filePath) throws SQLException {
        return new AuctionDAO().findAll();
    }

    public static List<Auction> loadAuctions() throws SQLException {
        return loadAuctions(null);
    }
}
