package com.cloudfridge;

public class FoodModel {
    private String name;
    private String expiryDate;

    public String getName() {
        return name;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public FoodModel(String name, String expiryDate) {
        this.name = name;
        this. expiryDate = expiryDate;
    }
}
