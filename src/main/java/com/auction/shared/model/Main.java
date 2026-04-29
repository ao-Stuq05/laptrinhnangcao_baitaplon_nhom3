package com.auction.shared.model;

public class Main {
    public static void main(String[] args) {
        // Demo: tạo một cuộc đấu giá sử dụng ItemFactory
        Item item = ItemFactory.createItem("ELECTRONICS", "ITM-001", "iPhone 15 Pro", "Điện thoại cao cấp", 20000000, null);
        System.out.println("Created item: " + item.getName());
    }
}