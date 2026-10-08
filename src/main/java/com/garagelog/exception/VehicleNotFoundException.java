package com.garagelog.exception;

public class VehicleNotFoundException extends GarageLogException {
    public VehicleNotFoundException(String identifier) {
        super("Vehicle not found: " + identifier);
    }
}
