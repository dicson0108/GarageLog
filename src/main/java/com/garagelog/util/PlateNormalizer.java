package com.garagelog.util;

public final class PlateNormalizer {
    private PlateNormalizer() {
    }

    public static String normalize(String plate) {
        if (plate == null) {
            return "";
        }
        return plate.trim()
                .toUpperCase()
                .replaceAll("[\\s\\-]+", "");
    }
}
