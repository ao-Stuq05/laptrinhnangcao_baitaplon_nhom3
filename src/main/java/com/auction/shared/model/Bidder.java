package com.auction.shared.model;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {  // ← thêm public

    private double balance;
    private List<BidTransaction> bidHistory;

    // Constructor đầy đủ
    public Bidder(String username, String email, String passwordHash, double balance) {
        super(username, email, passwordHash);
        this.balance = balance;
        this.bidHistory = new ArrayList<>();
    }

    // Constructor không cần balance — dùng cho Register
    public Bidder(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
        this.balance = 0.0;  // mặc định 0
        this.bidHistory = new ArrayList<>();
    }

    @Override
    public String getRole() {
        return "BIDDER";
    }

    public void bid(Auction auction, double amount) {
        auction.placeBid(this, amount);
    }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
}