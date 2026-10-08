package com.garagelog.util;

import com.garagelog.model.*;
import com.garagelog.service.VehicleService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CsvUtil {
    private CsvUtil() {}

    public static String exportVehiclesToCsv(List<Vehicle> vehicles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Plate,Nickname,Brand,Model,Year,Type,FuelType,Colour,VIN,Odometer,PurchaseDate,PurchasePrice,Status,InsuranceExpiry,RoadTaxExpiry,Notes\n");
        for (Vehicle v : vehicles) {
            sb.append(escape(v.getPlateNumber())).append(",")
              .append(escape(v.getNickname())).append(",")
              .append(escape(v.getBrand())).append(",")
              .append(escape(v.getModel())).append(",")
              .append(v.getYear()).append(",")
              .append(v.getType() != null ? v.getType().name() : "").append(",")
              .append(v.getFuelType() != null ? v.getFuelType().name() : "").append(",")
              .append(escape(v.getColour())).append(",")
              .append(escape(v.getVin())).append(",")
              .append(v.getCurrentOdometer()).append(",")
              .append(v.getPurchaseDate() != null ? v.getPurchaseDate().toString() : "").append(",")
              .append(v.getPurchasePrice() != null ? v.getPurchasePrice().toPlainString() : "").append(",")
              .append(v.getStatus() != null ? v.getStatus().name() : "").append(",")
              .append(v.getInsuranceExpiryDate() != null ? v.getInsuranceExpiryDate().toString() : "").append(",")
              .append(v.getRoadTaxExpiryDate() != null ? v.getRoadTaxExpiryDate().toString() : "").append(",")
              .append(escape(v.getNotes())).append("\n");
        }
        return sb.toString();
    }

    public record ImportReport(int addedCount, int skippedCount, List<String> errorLines) {}

    public static ImportReport importVehiclesFromCsv(String csvContent, VehicleService vehicleService) throws IOException {
        int added = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String header = reader.readLine();
            if (header == null) {
                return new ImportReport(0, 0, errors);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 5) {
                    errors.add("Line " + lineNum + ": Insufficient columns");
                    continue;
                }

                String plate = parts[0].trim().replace("\"", "");
                if (plate.isEmpty()) {
                    errors.add("Line " + lineNum + ": Plate is empty");
                    continue;
                }

                if (vehicleService.findByPlate(plate).isPresent()) {
                    skipped++;
                    continue;
                }

                try {
                    Vehicle v = new Vehicle();
                    v.setPlateNumber(plate);
                    v.setNickname(parts.length > 1 ? parts[1].trim().replace("\"", "") : "");
                    v.setBrand(parts.length > 2 ? parts[2].trim().replace("\"", "") : "Unknown");
                    v.setModel(parts.length > 3 ? parts[3].trim().replace("\"", "") : "Unknown");
                    v.setYear(parts.length > 4 && !parts[4].isBlank() ? Integer.parseInt(parts[4].trim()) : LocalDate.now().getYear());

                    if (parts.length > 5 && !parts[5].isBlank()) {
                        try { v.setType(VehicleType.valueOf(parts[5].trim())); } catch (Exception ignored) {}
                    }
                    if (parts.length > 6 && !parts[6].isBlank()) {
                        try { v.setFuelType(FuelType.valueOf(parts[6].trim())); } catch (Exception ignored) {}
                    }
                    if (parts.length > 7) v.setColour(parts[7].trim().replace("\"", ""));
                    if (parts.length > 8) v.setVin(parts[8].trim().replace("\"", ""));
                    if (parts.length > 9 && !parts[9].isBlank()) {
                        v.setCurrentOdometer(Double.parseDouble(parts[9].trim()));
                    }
                    if (parts.length > 10 && !parts[10].isBlank()) {
                        v.setPurchaseDate(LocalDate.parse(parts[10].trim()));
                    }
                    if (parts.length > 11 && !parts[11].isBlank()) {
                        v.setPurchasePrice(new BigDecimal(parts[11].trim()));
                    }
                    if (parts.length > 12 && !parts[12].isBlank()) {
                        try { v.setStatus(VehicleStatus.valueOf(parts[12].trim())); } catch (Exception ignored) {}
                    }

                    vehicleService.addVehicle(v);
                    added++;
                } catch (Exception ex) {
                    errors.add("Line " + lineNum + " (" + plate + "): " + ex.getMessage());
                }
            }
        }

        return new ImportReport(added, skipped, errors);
    }

    private static String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
