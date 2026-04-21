package com.auction.shared.model; 
class Admin extends User {

    public Admin(String id, String username, String email, String passwordHash) {
        super(id, username, email, passwordHash);
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    @Override
    public void printInfo() {
        System.out.println("Admin: " + username + " | Email: " + email);
    }

    // Quản lý hệ thống (ví dụ)
    public void viewAllAuctions() {
        for (Auction a : AuctionManager.getInstance().getActive()) {
            a.printInfo();
        }
    }
}
