package com.garagelog.ui;

import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.service.*;
import com.garagelog.util.SampleDataGenerator;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UILoadTest {

    @Test
    void testInitializeAllViewsWithoutException() throws Exception {
        Path tempDir = Files.createTempDirectory("garagelog-ui-test-");
        var repository = new JsonGarageRepository(tempDir);
        var vehicleService = new VehicleService(repository);
        var maintenanceService = new MaintenanceService(repository, vehicleService);
        var expenseService = new ExpenseService(repository, vehicleService);
        var fuelService = new FuelService(repository, vehicleService);
        var reminderService = new ReminderService(vehicleService, maintenanceService);
        var settingsService = new SettingsService(repository, vehicleService);
        var reportService = new ReportService(vehicleService, maintenanceService, expenseService, fuelService);

        SampleDataGenerator.populateIfEmpty(vehicleService, maintenanceService, expenseService, fuelService);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        try {
            Platform.startup(() -> {
                try {
                    NavigationListener dummy = new NavigationListener() {
                        @Override public void navigateToDashboard() {}
                        @Override public void navigateToVehicles() {}
                        @Override public void navigateToVehicleDetail(com.garagelog.model.Vehicle vehicle) {}
                        @Override public void navigateToReports() {}
                        @Override public void navigateToData() {}
                        @Override public void navigateToSettings() {}
                        @Override public void refreshAll() {}
                    };

                    new DashboardView(vehicleService, maintenanceService, expenseService, fuelService, reminderService, settingsService, dummy);
                    new VehiclesView(vehicleService, dummy);
                    var v = vehicleService.getAllVehicles(false).get(0);
                    new VehicleDetailView(v, vehicleService, maintenanceService, expenseService, fuelService, reportService, settingsService, dummy);
                    new ReportsView(vehicleService, maintenanceService, expenseService, fuelService, reportService, settingsService, dummy);
                    new DataView(repository, vehicleService, dummy);
                    new SettingsView(settingsService, dummy);
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                }
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> {
                try {
                    NavigationListener dummy = new NavigationListener() {
                        @Override public void navigateToDashboard() {}
                        @Override public void navigateToVehicles() {}
                        @Override public void navigateToVehicleDetail(com.garagelog.model.Vehicle vehicle) {}
                        @Override public void navigateToReports() {}
                        @Override public void navigateToData() {}
                        @Override public void navigateToSettings() {}
                        @Override public void refreshAll() {}
                    };

                    new DashboardView(vehicleService, maintenanceService, expenseService, fuelService, reminderService, settingsService, dummy);
                    new VehiclesView(vehicleService, dummy);
                    var v = vehicleService.getAllVehicles(false).get(0);
                    new VehicleDetailView(v, vehicleService, maintenanceService, expenseService, fuelService, reportService, settingsService, dummy);
                    new ReportsView(vehicleService, maintenanceService, expenseService, fuelService, reportService, settingsService, dummy);
                    new DataView(repository, vehicleService, dummy);
                    new SettingsView(settingsService, dummy);
                } catch (Throwable t) {
                    errorRef.set(t);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "UI initialization timed out");
        if (errorRef.get() != null) {
            errorRef.get().printStackTrace();
        }
        assertNull(errorRef.get(), "Error creating views: " + errorRef.get());
    }
}
