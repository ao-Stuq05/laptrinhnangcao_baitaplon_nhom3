package com.auction.server.db;

import com.auction.shared.model.Auction;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Item;
import com.auction.shared.model.Seller;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataStoreTest {

    @Test
    void saveAndLoadAuctionList() throws Exception {
        Path tempFile = Files.createTempFile("auctions", ".bin");
        try {
            Seller seller = new Seller(
                    "seller-1",
                    "seller1",
                    "seller1@example.com",
                    "hash",
                    "ShopA",
                    4.5
            );

            Item item = new Electronics(
                    "item-1",
                    "Laptop",
                    "Laptop bán chạy",
                    1200.0,
                    seller,
                    24
            );

            Auction auction = new Auction(
                    "auc-1",
                    item,
                    seller,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(1)
            );

            List<Auction> auctions = new ArrayList<>();
            auctions.add(auction);

            DataStore.saveAuctions(auctions, tempFile.toString());

            List<Auction> loaded = DataStore.loadAuctions(tempFile.toString());

            assertEquals(1, loaded.size());
            Auction loadedAuction = loaded.get(0);
            assertEquals("auc-1", loadedAuction.getId());
            assertEquals(auction.getCurrentPrice(), loadedAuction.getCurrentPrice());
            assertEquals("Laptop", loadedAuction.getItem().getName());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
