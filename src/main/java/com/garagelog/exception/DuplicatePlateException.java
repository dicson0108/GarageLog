package com.garagelog.exception;

public class DuplicatePlateException extends GarageLogException {
    public DuplicatePlateException(String plateNumber) {
        super("A vehicle with license plate '" + plateNumber + "' already exists.");
    }
}
