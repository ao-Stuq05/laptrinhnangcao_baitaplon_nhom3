package com.auction.shared.model;

import java.io.Serializable;

public abstract class Item extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String description;
    protected double basePrice;
    protected Seller seller;
    protected String imageBase64;

    protected Item(String id, String name, String description, double basePrice, Seller seller) {
        super();
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.seller = seller;
        this.imageBase64 = null;
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
    public void setName(String name)               { this.name = name; }
    public void setBasePrice(double basePrice)     { this.basePrice = basePrice; }
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}