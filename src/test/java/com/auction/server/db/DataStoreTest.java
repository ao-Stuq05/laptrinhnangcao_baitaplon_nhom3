package com.auction.server.db;

import com.auction.server.db.DatabaseManager;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Item;
import com.auction.shared.model.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DataStoreTest {

    @BeforeEach
    void clearDatabase() throws Exception {
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.execute("TRUNCATE TABLE bid_transactions");
            stmt.execute("TRUNCATE TABLE auctions");
            stmt.execute("TRUNCATE TABLE items");
            stmt.execute("TRUNCATE TABLE users");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    @Test
    void testSaveAndLoadAuctionWithSQL() throws Exception {
        // 1. CHUẨN BỊ (Gữi nguyên logic tạo Object như cũ)
        Seller seller = new Seller(
                "S001", "manh_tuan", "tuan@example.com",
                "hashed_pw", "Tuan Store", 4.8
        );

        Item item = new Electronics(
                "I001", "iPhone 15 Pro", "New 100%",
                1000.0, seller, 12
        );

        Auction auction = new Auction(
                "A001", item, seller,
                LocalDateTime.now(), LocalDateTime.now().plusDays(3)
        );

        List<Auction> auctionList = new ArrayList<>();
        auctionList.add(auction);

        // 2. THỰC THI (Lưu vào SQL)
        // Lưu ý: DataStore.saveAuctions bây giờ nên xử lý việc Insert vào nhiều bảng
        DataStore.saveAuctions(auctionList);

        // 3. KIỂM TRA (Load từ SQL)
        List<Auction> loadedList = DataStore.loadAuctions();

        // 4. KHẲNG ĐỊNH (Assertions)
        assertEquals(1, loadedList.size(), "DB phải có đúng 1 phiên đấu giá");
        
        Auction loaded = loadedList.get(0);
        assertEquals("A001", loaded.getId());
        assertEquals("iPhone 15 Pro", loaded.getItem().getName());
        assertEquals("Tuan Store", loaded.getSeller().getShopName());
        
        // Kiểm tra tính đa hình (Polymorphism) nếu là Electronics
        assertNotNull(loaded.getItem());
        assertEquals(1000.0, loaded.getItem().getBasePrice());
    }
}