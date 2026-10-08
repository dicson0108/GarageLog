package com.garagelog.ui;

import com.garagelog.model.Vehicle;

public interface NavigationListener {
    void navigateToDashboard();
    void navigateToVehicles();
    void navigateToVehicleDetail(Vehicle vehicle);
    void navigateToReports();
    void navigateToData();
    void navigateToSettings();
    void refreshAll();
}
