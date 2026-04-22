package com.auction.shared.model;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double basePrice,Seller seller, int warrantyMonths) {
        super(id, name, description, basePrice, seller);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Điện tử] " + name + " | Giá gốc: $" + basePrice + " | Bảo hành: " + warrantyMonths + " tháng");
    }
    @Override
    public String getCategory() {
        return "Electronics";
    }
}