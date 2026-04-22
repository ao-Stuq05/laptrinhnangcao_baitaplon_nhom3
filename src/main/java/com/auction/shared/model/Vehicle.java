package com.auction.shared.model;

public class Vehicle extends Item {
    private String licensePlate;
    private int mileage;

    public Vehicle(String id, String name, String description, double basePrice, Seller seller, String licensePlate, int mileage) {
        super(id, name, description, basePrice, seller);
        this.licensePlate = licensePlate;
        this.mileage = mileage;
    }

    @Override
    public void printInfo() {
        System.out.println("[Xe cộ] " + name + " | Biển số: " + licensePlate + " | ODO: " + mileage + "km | Giá: $" + basePrice);
    }
    @Override
    public String getCategory() {
        return "VEHICLE";
    }
}