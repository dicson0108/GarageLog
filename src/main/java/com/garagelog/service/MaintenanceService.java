package com.garagelog.service;

import com.garagelog.exception.ValidationException;
import com.garagelog.exception.VehicleNotFoundException;
import com.garagelog.model.AppData;
import com.garagelog.model.MaintenanceRecord;
import com.garagelog.model.Vehicle;
import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.util.ValidationUtil;

import java.util.*;
import java.util.stream.Collectors;

public class MaintenanceService {
    private final JsonGarageRepository repository;
    private final VehicleService vehicleService;

    public MaintenanceService(JsonGarageRepository repository, VehicleService vehicleService) {
        this.repository = repository;
        this.vehicleService = vehicleService;
    }

    private AppData getAppData() {
        return vehicleService.getAppData();
    }

    public synchronized List<MaintenanceRecord> getRecordsForVehicle(UUID vehicleId) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }
        return getAppData().getMaintenanceRecords().stream()
                .filter(m -> vehicleId.equals(m.getVehicleId()))
                .sorted(Comparator.comparing(MaintenanceRecord::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Optional<MaintenanceRecord> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return getAppData().getMaintenanceRecords().stream()
                .filter(m -> id.equals(m.getId()))
                .findFirst();
    }

    public synchronized double getHighestOdometerForVehicle(UUID vehicleId) {
        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId.toString()));
        double highest = vehicle.getCurrentOdometer();

        for (MaintenanceRecord r : getRecordsForVehicle(vehicleId)) {
            if (r.getOdometer() > highest) {
                highest = r.getOdometer();
            }
        }
        for (var f : getAppData().getFuelFillUps()) {
            if (vehicleId.equals(f.getVehicleId()) && f.getOdometer() > highest) {
                highest = f.getOdometer();
            }
        }
        return highest;
    }

    public synchronized boolean isOdometerLowerThanPrevious(UUID vehicleId, double newOdometer) {
        double highest = getHighestOdometerForVehicle(vehicleId);
        return newOdometer < highest;
    }

    public synchronized MaintenanceRecord addRecord(MaintenanceRecord record) {
        ValidationUtil.validateMaintenance(record);
        Vehicle vehicle = vehicleService.findById(record.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(record.getVehicleId().toString()));

        getAppData().getMaintenanceRecords().add(record);

        // Update vehicle odometer if new reading is higher
        if (record.getOdometer() > vehicle.getCurrentOdometer()) {
            vehicle.setCurrentOdometer(record.getOdometer());
        }

        repository.save(getAppData());
        return record;
    }

    public synchronized MaintenanceRecord updateRecord(MaintenanceRecord updated) {
        ValidationUtil.validateMaintenance(updated);
        MaintenanceRecord existing = findById(updated.getId())
                .orElseThrow(() -> new ValidationException("Maintenance record not found: " + updated.getId()));

        Vehicle vehicle = vehicleService.findById(updated.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(updated.getVehicleId().toString()));

        existing.setDate(updated.getDate());
        existing.setOdometer(updated.getOdometer());
        existing.setWorkDone(updated.getWorkDone());
        existing.setCost(updated.getCost());
        existing.setWorkshop(updated.getWorkshop());
        existing.setNextServiceDueDate(updated.getNextServiceDueDate());
        existing.setNextServiceDueOdometer(updated.getNextServiceDueOdometer());
        existing.setDocumentPaths(updated.getDocumentPaths());

        if (updated.getOdometer() > vehicle.getCurrentOdometer()) {
            vehicle.setCurrentOdometer(updated.getOdometer());
        }

        repository.save(getAppData());
        return existing;
    }

    public synchronized void deleteRecord(UUID id) {
        getAppData().getMaintenanceRecords().removeIf(m -> m.getId().equals(id));
        repository.save(getAppData());
    }
}
