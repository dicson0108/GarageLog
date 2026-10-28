package com.garagelog.ui;

import com.garagelog.model.Vehicle;

public interface NavigationListener {
    void navigateToVehicles();
    void navigateToVehicleDetail(Vehicle vehicle);
    void navigateToReports();
    void refreshAll();
}
