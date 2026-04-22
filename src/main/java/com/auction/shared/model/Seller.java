package com.auction.shared.model;

import com.auction.model.item.Item;
import com.auction.model.Auction;
import com.auction.pattern.AuctionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Seller extends User {

    private String shopName;
    private List<Item> listedItems;
    private double rating; // 0.0 – 5.0

    public Seller(String username, String email, String passwordHash, String shopName) {
        super(username, email, passwordHash);
        this.shopName = shopName;
        this.listedItems = new ArrayList<>();
        this.rating = 0.0;
    }

    @Override
    public String getRole() { return "SELLER"; }

    public void listItem(Item item) {
        listedItems.add(item);
    }

    /**
     * Convenience: create and register an auction for an item.
     */
    public Auction createAuction(Item item, LocalDateTime endTime) {
        listItem(item);
        Auction auction = new Auction(item, endTime);
        AuctionManager.getInstance().registerAuction(auction);
        return auction;
    }

    public List<Item> getListedItems() { return Collections.unmodifiableList(listedItems); }
    public String getShopName() { return shopName; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.printf("  shop=%s | rating=%.1f | items=%d%n",
                shopName, rating, listedItems.size());
    }
}
