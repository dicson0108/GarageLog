package com.garagelog.service;

import com.garagelog.exception.ValidationException;
import com.garagelog.exception.VehicleNotFoundException;
import com.garagelog.model.AppData;
import com.garagelog.model.FuelFillUp;
import com.garagelog.model.Vehicle;
import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class FuelService {
    private final JsonGarageRepository repository;
    private final VehicleService vehicleService;

    public FuelService(JsonGarageRepository repository, VehicleService vehicleService) {
        this.repository = repository;
        this.vehicleService = vehicleService;
    }

    private AppData getAppData() {
        return vehicleService.getAppData();
    }

    public synchronized List<FuelFillUp> getFillUpsForVehicle(UUID vehicleId) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }
        return getAppData().getFuelFillUps().stream()
                .filter(f -> vehicleId.equals(f.getVehicleId()))
                .sorted(Comparator.comparing(FuelFillUp::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FuelFillUp::getOdometer).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Optional<FuelFillUp> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return getAppData().getFuelFillUps().stream()
                .filter(f -> id.equals(f.getId()))
                .findFirst();
    }

    public synchronized FuelFillUp addFillUp(FuelFillUp fillUp) {
        ValidationUtil.validateFuel(fillUp);
        Vehicle vehicle = vehicleService.findById(fillUp.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(fillUp.getVehicleId().toString()));

        getAppData().getFuelFillUps().add(fillUp);

        if (fillUp.getOdometer() > vehicle.getCurrentOdometer()) {
            vehicle.setCurrentOdometer(fillUp.getOdometer());
        }

        repository.save(getAppData());
        return fillUp;
    }

    public synchronized FuelFillUp updateFillUp(FuelFillUp updated) {
        ValidationUtil.validateFuel(updated);
        FuelFillUp existing = findById(updated.getId())
                .orElseThrow(() -> new ValidationException("Fuel fill-up not found: " + updated.getId()));

        Vehicle vehicle = vehicleService.findById(updated.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(updated.getVehicleId().toString()));

        existing.setDate(updated.getDate());
        existing.setOdometer(updated.getOdometer());
        existing.setLitres(updated.getLitres());
        existing.setPricePerLitre(updated.getPricePerLitre());
        existing.setTotalCost(updated.getTotalCost());
        existing.setFullTank(updated.isFullTank());

        if (updated.getOdometer() > vehicle.getCurrentOdometer()) {
            vehicle.setCurrentOdometer(updated.getOdometer());
        }

        repository.save(getAppData());
        return existing;
    }

    public synchronized void deleteFillUp(UUID id) {
        getAppData().getFuelFillUps().removeIf(f -> f.getId().equals(id));
        repository.save(getAppData());
    }

    public synchronized BigDecimal calculateTotalFuelCost(UUID vehicleId) {
        return getAppData().getFuelFillUps().stream()
                .filter(f -> vehicleId == null || vehicleId.equals(f.getVehicleId()))
                .map(FuelFillUp::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record ConsumptionInterval(
            FuelFillUp startFillUp,
            FuelFillUp endFillUp,
            double distanceKm,
            double litresConsumed,
            double litresPer100Km,
            double kmPerLitre
    ) {}

    public synchronized List<ConsumptionInterval> calculateConsumptionHistory(UUID vehicleId) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }

        List<FuelFillUp> chronological = getAppData().getFuelFillUps().stream()
                .filter(f -> vehicleId.equals(f.getVehicleId()))
                .sorted(Comparator.comparing(FuelFillUp::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FuelFillUp::getOdometer))
                .toList();

        List<ConsumptionInterval> results = new ArrayList<>();
        FuelFillUp previousFull = null;
        double accruedLitres = 0.0;

        for (FuelFillUp current : chronological) {
            if (previousFull == null) {
                if (current.isFullTank()) {
                    previousFull = current;
                }
                continue;
            }

            accruedLitres += current.getLitres();

            if (current.isFullTank()) {
                double distance = current.getOdometer() - previousFull.getOdometer();
                if (distance > 0 && accruedLitres > 0) {
                    double lPer100 = (accruedLitres / distance) * 100.0;
                    double kmPerL = distance / accruedLitres;
                    results.add(new ConsumptionInterval(previousFull, current, distance, accruedLitres, lPer100, kmPerL));
                }
                previousFull = current;
                accruedLitres = 0.0;
            }
        }

        return results;
    }
}
