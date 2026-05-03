package com.auction.shared.model;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.Serializable;

public class Seller extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String shopName;
    private List<Item> listedItems;
    private double rating; // 0.0 – 5.0

    public Seller( String id, String username, String email, String passwordHash, String shopName,double rating) {
        super(id, username, email, passwordHash);
        this.shopName = shopName;
        this.listedItems = new ArrayList<>();
        this.rating = rating;
    }

    @Override
    public String getRole() { 
        return "SELLER"; 
    }

    public void listItem(Item item) {
        if (item != null) {
            listedItems.add(item);
        }
    }

    /**
     * Tạo và đăng ký một cuộc đấu giá cho vật phẩm.
     * Lưu ý: Hãy đảm bảo file Auction.java đã có constructor Auction(Item, LocalDateTime)
     */
    public Auction createAuction(Item item, LocalDateTime endTime) {
        listItem(item);
        Auction auction = new Auction(item, endTime);
        // Đảm bảo AuctionManager đã được định nghĩa đúng theo Pattern Singleton
        AuctionManager.getInstance().registerAuction(auction);
        return auction;
    }

    public List<Item> getListedItems() { 
        return Collections.unmodifiableList(listedItems); 
    }

    public String getShopName() { return shopName; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    @Override
    public void printInfo() {
        // Gọi hàm in của lớp cha User
        super.printInfo(); 
        System.out.printf("  shop=%s | rating=%.1f | items=%d%n",
                shopName, rating, listedItems.size());
    }
}