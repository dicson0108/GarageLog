package com.garagelog.service;

import com.garagelog.exception.DuplicatePlateException;
import com.garagelog.exception.VehicleNotFoundException;
import com.garagelog.model.*;
import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.util.PlateNormalizer;
import com.garagelog.util.ValidationUtil;

import java.util.*;
import java.util.stream.Collectors;

public class VehicleService {
    private final JsonGarageRepository repository;
    private AppData appData;

    public VehicleService(JsonGarageRepository repository) {
        this.repository = repository;
        this.appData = repository.load();
    }

    public synchronized void reload() {
        this.appData = repository.load();
    }

    public AppData getAppData() {
        return appData;
    }

    public synchronized List<Vehicle> getAllVehicles(boolean includeSold) {
        return appData.getVehicles().stream()
                .filter(v -> includeSold || v.getStatus() != VehicleStatus.SOLD)
                .collect(Collectors.toList());
    }

    public synchronized Optional<Vehicle> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return appData.getVehicles().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst();
    }

    public synchronized Optional<Vehicle> findByPlate(String plate) {
        if (plate == null) {
            return Optional.empty();
        }
        String normalized = PlateNormalizer.normalize(plate);
        return appData.getVehicles().stream()
                .filter(v -> PlateNormalizer.normalize(v.getPlateNumber()).equals(normalized))
                .findFirst();
    }

    public synchronized Vehicle addVehicle(Vehicle vehicle) {
        ValidationUtil.validateVehicle(vehicle);

        String normalizedPlate = PlateNormalizer.normalize(vehicle.getPlateNumber());
        boolean exists = appData.getVehicles().stream()
                .anyMatch(v -> PlateNormalizer.normalize(v.getPlateNumber()).equals(normalizedPlate));

        if (exists) {
            throw new DuplicatePlateException(vehicle.getPlateNumber());
        }

        appData.getVehicles().add(vehicle);
        repository.save(appData);
        return vehicle;
    }

    public synchronized Vehicle updateVehicle(Vehicle updated) {
        ValidationUtil.validateVehicle(updated);

        Vehicle existing = findById(updated.getId())
                .orElseThrow(() -> new VehicleNotFoundException(updated.getId().toString()));

        String normalizedNewPlate = PlateNormalizer.normalize(updated.getPlateNumber());
        boolean duplicatePlate = appData.getVehicles().stream()
                .anyMatch(v -> !v.getId().equals(updated.getId()) &&
                        PlateNormalizer.normalize(v.getPlateNumber()).equals(normalizedNewPlate));

        if (duplicatePlate) {
            throw new DuplicatePlateException(updated.getPlateNumber());
        }

        existing.setPlateNumber(updated.getPlateNumber());
        existing.setNickname(updated.getNickname());
        existing.setBrand(updated.getBrand());
        existing.setModel(updated.getModel());
        existing.setYear(updated.getYear());
        existing.setType(updated.getType());
        existing.setColour(updated.getColour());
        existing.setFuelType(updated.getFuelType());
        existing.setVin(updated.getVin());
        existing.setCurrentOdometer(updated.getCurrentOdometer());
        existing.setPurchaseDate(updated.getPurchaseDate());
        existing.setPurchasePrice(updated.getPurchasePrice());
        existing.setStatus(updated.getStatus());
        existing.setInsuranceExpiryDate(updated.getInsuranceExpiryDate());
        existing.setRoadTaxExpiryDate(updated.getRoadTaxExpiryDate());
        existing.setPhotoPath(updated.getPhotoPath());
        existing.setNotes(updated.getNotes());

        repository.save(appData);
        return existing;
    }

    public synchronized void markAsSold(UUID vehicleId) {
        Vehicle vehicle = findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId != null ? vehicleId.toString() : "null"));
        vehicle.setStatus(VehicleStatus.SOLD);
        repository.save(appData);
    }

    public synchronized int countLinkedRecords(UUID vehicleId) {
        if (vehicleId == null) {
            return 0;
        }
        int maintenanceCount = (int) appData.getMaintenanceRecords().stream()
                .filter(m -> vehicleId.equals(m.getVehicleId()))
                .count();
        int expenseCount = (int) appData.getExpenses().stream()
                .filter(e -> vehicleId.equals(e.getVehicleId()))
                .count();
        int fuelCount = (int) appData.getFuelFillUps().stream()
                .filter(f -> vehicleId.equals(f.getVehicleId()))
                .count();

        return maintenanceCount + expenseCount + fuelCount;
    }

    public synchronized DeleteResult deleteVehicle(UUID vehicleId) {
        Vehicle vehicle = findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId != null ? vehicleId.toString() : "null"));

        int maintenanceCount = (int) appData.getMaintenanceRecords().stream()
                .filter(m -> vehicleId.equals(m.getVehicleId()))
                .count();
        int expenseCount = (int) appData.getExpenses().stream()
                .filter(e -> vehicleId.equals(e.getVehicleId()))
                .count();
        int fuelCount = (int) appData.getFuelFillUps().stream()
                .filter(f -> vehicleId.equals(f.getVehicleId()))
                .count();

        appData.getMaintenanceRecords().removeIf(m -> vehicleId.equals(m.getVehicleId()));
        appData.getExpenses().removeIf(e -> vehicleId.equals(e.getVehicleId()));
        appData.getFuelFillUps().removeIf(f -> vehicleId.equals(f.getVehicleId()));
        appData.getVehicles().remove(vehicle);

        repository.save(appData);
        return new DeleteResult(maintenanceCount, expenseCount, fuelCount);
    }

    public synchronized List<Vehicle> searchAndFilter(String query, VehicleStatus status, VehicleType type,
                                                      FuelType fuelType, boolean includeSold,
                                                      String sortBy, boolean sortAscending) {
        String cleanQuery = query != null ? query.trim().toLowerCase() : "";

        List<Vehicle> filtered = appData.getVehicles().stream()
                .filter(v -> includeSold || v.getStatus() != VehicleStatus.SOLD)
                .filter(v -> {
                    if (cleanQuery.isEmpty()) return true;
                    boolean plateMatch = v.getPlateNumber() != null && v.getPlateNumber().toLowerCase().contains(cleanQuery);
                    boolean nickMatch = v.getNickname() != null && v.getNickname().toLowerCase().contains(cleanQuery);
                    boolean brandMatch = v.getBrand() != null && v.getBrand().toLowerCase().contains(cleanQuery);
                    boolean modelMatch = v.getModel() != null && v.getModel().toLowerCase().contains(cleanQuery);
                    return plateMatch || nickMatch || brandMatch || modelMatch;
                })
                .filter(v -> status == null || v.getStatus() == status)
                .filter(v -> type == null || v.getType() == type)
                .filter(v -> fuelType == null || v.getFuelType() == fuelType)
                .collect(Collectors.toList());

        Comparator<Vehicle> comparator;
        String sortKey = sortBy != null ? sortBy.toLowerCase() : "plate";

        switch (sortKey) {
            case "brand":
                comparator = Comparator.comparing(v -> v.getBrand() != null ? v.getBrand() : "", String.CASE_INSENSITIVE_ORDER);
                break;
            case "year":
                comparator = Comparator.comparingInt(Vehicle::getYear);
                break;
            case "odometer":
                comparator = Comparator.comparingDouble(Vehicle::getCurrentOdometer);
                break;
            case "plate":
            default:
                comparator = Comparator.comparing(v -> v.getPlateNumber() != null ? v.getPlateNumber() : "", String.CASE_INSENSITIVE_ORDER);
                break;
        }

        if (!sortAscending) {
            comparator = comparator.reversed();
        }

        filtered.sort(comparator);
        return filtered;
    }

    public record DeleteResult(int maintenanceDeleted, int expensesDeleted, int fuelDeleted) {
        public int totalDeleted() {
            return maintenanceDeleted + expensesDeleted + fuelDeleted;
        }
    }
}
