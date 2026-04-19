package com.auction.shared.model;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double basePrice, int warrantyMonths) {
        super(id, name, description, basePrice);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Điện tử] " + name + " | Giá gốc: $" + basePrice + " | Bảo hành: " + warrantyMonths + " tháng");
    }
}