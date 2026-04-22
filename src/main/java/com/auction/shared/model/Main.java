package com.auction.shared.model;
import java.util.ArrayList;
import java.util.List;

class Seller extends User {
  private String shopName;
  private List<Item> listedItems;
  private double rating;

  public Seller( String username, String email, String passwordHash, String shopName) {
    super(username, email, passwordHash);
    this.shopName = shopName;
    this.listedItems = new ArrayList<>();
  }

  @Override
  public String getRole() {
    return "Seller";
  }

  public void listItem(Item item) {
    listedItems.add(item);
  }

  public void createAuction(Item item) {
    Auction auction = new Auction("AUC-" + System.currentTimeMillis(), item);
    AuctionManager.getInstance().registerAuction(auction);
  }
}