package com.garagelog.model;

public enum ExpenseCategory {
    FUEL("Fuel"),
    PARKING("Parking"),
    TOLL("Toll"),
    INSURANCE("Insurance"),
    ROAD_TAX("Road tax"),
    REPAIRS("Repairs"),
    OTHER("Other");

    private final String displayName;

    ExpenseCategory(String displayName) {
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
