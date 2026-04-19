package com.auction.shared.model;

public class Art extends Item {
    private String artistName;
    private int creationYear;

    public Art(String id, String name, String description, double basePrice, String artistName, int creationYear) {
        super(id, name, description, basePrice);
        this.artistName = artistName;
        this.creationYear = creationYear;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Nghệ thuật] " + name + " | Tác giả: " + artistName + " (" + creationYear + ") | Giá: $" + basePrice);
    }
}