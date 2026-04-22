package com.auction.shared.model;

public class Art extends Item {
    private String artistName;
    private int creationYear;

    public Art(String id, String name, String description, double basePrice,Seller seller, String artistName, int creationYear) {
        super(id, name, description, basePrice,seller);
        this.artistName = artistName;
        this.creationYear = creationYear;
    }

    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + name + " | Tác giả: " + artistName + " (" + creationYear + ") | Giá: $" + basePrice);
    }
    @Override
    public String getCategory(){
        return "ART";
    }
}