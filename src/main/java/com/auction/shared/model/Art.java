package com.auction.shared.model;

import java.io.Serializable;

public class Art extends Item implements Serializable {

    private static final long serialVersionUID = 1L;

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