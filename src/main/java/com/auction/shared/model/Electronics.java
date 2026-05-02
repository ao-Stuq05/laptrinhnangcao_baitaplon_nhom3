package com.auction.shared.model;

import java.io.Serializable;

public class Electronics extends Item implements Serializable {

    private static final long serialVersionUID = 1L;

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