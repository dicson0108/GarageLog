package com.garagelog.service;

import com.garagelog.model.AppData;
import com.garagelog.model.Settings;
import com.garagelog.repository.GarageRepository;

public class SettingsService {
    private final GarageRepository repository;
    private final VehicleService vehicleService;

    public SettingsService(GarageRepository repository, VehicleService vehicleService) {
        this.repository = repository;
        this.vehicleService = vehicleService;
    }

    public synchronized Settings getSettings() {
        return vehicleService.getAppData().getSettings();
    }

    public synchronized void updateSettings(Settings updated) {
        AppData appData = vehicleService.getAppData();
        appData.setSettings(updated);
        repository.save(appData);
    }
}
