package com.auction.shared.model;
public abstract class Item extends Entity {

    protected String name;
    protected String description;
    protected double basePrice;
    protected Seller seller;

    protected Item(String id, String name, String description, double basePrice, Seller seller) {
        super();
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.seller = seller;
    }

    /** Returns the category label, e.g. "ELECTRONICS", "ART", "VEHICLE" */
    public abstract String getCategory();



    @Override
    public void printInfo() {
        System.out.printf("[%s] %s | basePrice=%.2f | seller=%s%n",
                getCategory(), name, basePrice, seller.getUsername());
    }

    // Getters / Setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getBasePrice() { return basePrice; }
    public Seller getSeller() { return seller; }
    public void setDescription(String description) { this.description = description; }
}
