package com.garagelog.model;

public enum FuelType {
    PETROL("Petrol"),
    DIESEL("Diesel"),
    HYBRID("Hybrid"),
    ELECTRIC("Electric"),
    OTHER("Other");

    private final String displayName;

    FuelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
