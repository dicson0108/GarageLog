package com.garagelog.model;

import java.util.ArrayList;
import java.util.List;

public class AppData {
    private int schemaVersion = 1;
    private Settings settings = new Settings();
    private List<Vehicle> vehicles = new ArrayList<>();
    private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();
    private List<Expense> expenses = new ArrayList<>();
    private List<FuelFillUp> fuelFillUps = new ArrayList<>();

    public AppData() {
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public Settings getSettings() {
        if (settings == null) {
            settings = new Settings();
        }
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public List<Vehicle> getVehicles() {
        if (vehicles == null) {
            vehicles = new ArrayList<>();
        }
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles != null ? vehicles : new ArrayList<>();
    }

    public List<MaintenanceRecord> getMaintenanceRecords() {
        if (maintenanceRecords == null) {
            maintenanceRecords = new ArrayList<>();
        }
        return maintenanceRecords;
    }

    public void setMaintenanceRecords(List<MaintenanceRecord> maintenanceRecords) {
        this.maintenanceRecords = maintenanceRecords != null ? maintenanceRecords : new ArrayList<>();
    }

    public List<Expense> getExpenses() {
        if (expenses == null) {
            expenses = new ArrayList<>();
        }
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses != null ? expenses : new ArrayList<>();
    }

    public List<FuelFillUp> getFuelFillUps() {
        if (fuelFillUps == null) {
            fuelFillUps = new ArrayList<>();
        }
        return fuelFillUps;
    }

    public void setFuelFillUps(List<FuelFillUp> fuelFillUps) {
        this.fuelFillUps = fuelFillUps != null ? fuelFillUps : new ArrayList<>();
    }
}
