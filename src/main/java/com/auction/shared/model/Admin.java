package com.auction.shared.model;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;

public class Admin extends User {

    public Admin(String username, String email, String passwordHash) {
        super(username, email, passwordHash);
    }

    @Override
    public String getRole() { return "ADMIN"; }

    public void cancelAuction(Auction auction) {
        auction.cancel();
        System.out.println("[ADMIN] Cancelled auction: " + auction.getId());
    }

    public void deactivateUser(User user) {
        user.setActive(false);
        System.out.println("[ADMIN] Deactivated user: " + user.getUsername());
    }

    @Override
    public void printInfo() {
        super.printInfo();
        System.out.println("  [ADMIN privileges]");
    }
}
