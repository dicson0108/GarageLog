package com.garagelog.model;

public enum VehicleType {
    CAR("Car"),
    MOTORCYCLE("Motorcycle"),
    TRUCK_VAN("Truck/Van"),
    OTHER("Other");

    private final String displayName;

    VehicleType(String displayName) {
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
