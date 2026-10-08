package com.garagelog.exception;

public class GarageLogException extends RuntimeException {
    public GarageLogException(String message) {
        super(message);
    }

    public GarageLogException(String message, Throwable cause) {
        super(message, cause);
    }
}
