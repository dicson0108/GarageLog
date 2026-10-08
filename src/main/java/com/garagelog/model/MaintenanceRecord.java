package com.garagelog.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MaintenanceRecord {
    private final UUID id;
    private UUID vehicleId;
    private LocalDate date;
    private double odometer;
    private String workDone;
    private BigDecimal cost;
    private String workshop;
    private LocalDate nextServiceDueDate;
    private Double nextServiceDueOdometer;
    private List<String> documentPaths = new ArrayList<>();

    public MaintenanceRecord() {
        this.id = UUID.randomUUID();
    }

    public MaintenanceRecord(UUID id) {
        this.id = id != null ? id : UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getOdometer() {
        return odometer;
    }

    public void setOdometer(double odometer) {
        this.odometer = odometer;
    }

    public String getWorkDone() {
        return workDone;
    }

    public void setWorkDone(String workDone) {
        this.workDone = workDone;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getWorkshop() {
        return workshop;
    }

    public void setWorkshop(String workshop) {
        this.workshop = workshop;
    }

    public LocalDate getNextServiceDueDate() {
        return nextServiceDueDate;
    }

    public void setNextServiceDueDate(LocalDate nextServiceDueDate) {
        this.nextServiceDueDate = nextServiceDueDate;
    }

    public Double getNextServiceDueOdometer() {
        return nextServiceDueOdometer;
    }

    public void setNextServiceDueOdometer(Double nextServiceDueOdometer) {
        this.nextServiceDueOdometer = nextServiceDueOdometer;
    }

    public List<String> getDocumentPaths() {
        if (documentPaths == null) {
            documentPaths = new ArrayList<>();
        }
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        this.documentPaths = documentPaths != null ? documentPaths : new ArrayList<>();
    }
}
