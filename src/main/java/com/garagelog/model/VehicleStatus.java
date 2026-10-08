package com.garagelog.model;

public enum VehicleStatus {
    ACTIVE("Active"),
    IN_STORAGE("In storage"),
    UNDER_REPAIR("Under repair"),
    SOLD("Sold");

    private final String displayName;

    VehicleStatus(String displayName) {
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
