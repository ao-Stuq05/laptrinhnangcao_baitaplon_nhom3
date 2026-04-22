package com.auction.shared.model;

public class ItemFactory {

    public static Item createItem(String type, String id, String name, String description, double basePrice, Seller seller, Object... extraParams) {
        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                // Truyền seller vào vị trí thứ 5 theo đúng constructor của Electronics
                return new Electronics(id, name, description, basePrice, seller, (Integer) extraParams[0]);

            case "ART":
                // Truyền seller vào trước artistName và creationYear
                return new Art(id, name, description, basePrice, seller, (String) extraParams[0], (Integer) extraParams[1]);

            case "VEHICLE":
                // Khớp với constructor: String id, String name, String description, double basePrice, Seller seller, String licensePlate, int mileage
                return new Vehicle(id, name, description, basePrice, seller, (String) extraParams[0], (Integer) extraParams[1]);

            default:
                throw new IllegalArgumentException("Lỗi: Loại sản phẩm không được hỗ trợ -> " + type);
        }
    }
}