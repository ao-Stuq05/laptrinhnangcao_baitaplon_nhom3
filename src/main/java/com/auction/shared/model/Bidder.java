package com.auction.shared.model;

import java.util.ArrayList;
import java.util.List;

class Bidder extends User {
    private double balance;
    private List<BidTransaction> bidHistory;

    public Bidder( String username, String email, String passwordHash, double balance ) {
        super( username, email, passwordHash);
        this.balance = balance;
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
