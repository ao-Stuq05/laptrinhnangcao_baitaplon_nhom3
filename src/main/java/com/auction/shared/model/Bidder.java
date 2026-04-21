package com.auction.shared.model;

import java.util.ArrayList;
import java.util.List;

class Bidder extends User {
    private double balance;
    private List<BidTransaction> bidHistory;

    public Bidder(String id, String username, String email, String pw, double balance) {
        super(id, username, email, pw);
        this.balance = balance;
        this.bidHistory = new ArrayList<>();
    }

    @Override
    public String getRole() {
        return "Bidder";
    }

    public void bid(Auction auction, double amount) {
        auction.placeBid(this, amount);
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }
}
