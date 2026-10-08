package com.garagelog.ui;

import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.service.*;
import com.garagelog.util.SampleDataGenerator;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started
        }
    }

    private void runOnFx(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                errorRef.set(t);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "UI task timed out");
        if (errorRef.get() != null) {
            errorRef.get().printStackTrace();
        }
        assertNull(errorRef.get(), "Error on FX thread: " + errorRef.get());
    }

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

        runOnFx(() -> {
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
        });
    }

    @Test
    void testLoadUserGarageDataAndViews() throws Exception {
        var repository = new JsonGarageRepository();
        var vehicleService = new VehicleService(repository);
        var maintenanceService = new MaintenanceService(repository, vehicleService);
        var expenseService = new ExpenseService(repository, vehicleService);
        var fuelService = new FuelService(repository, vehicleService);
        var reminderService = new ReminderService(vehicleService, maintenanceService);
        var settingsService = new SettingsService(repository, vehicleService);
        var reportService = new ReportService(vehicleService, maintenanceService, expenseService, fuelService);

        var vehicles = vehicleService.getAllVehicles(true);
        assertTrue(vehicles.size() >= 4, "Should have 4 vehicles");

        long activeCount = vehicles.stream().filter(v -> v.getStatus() == com.garagelog.model.VehicleStatus.ACTIVE).count();
        long soldCount = vehicles.stream().filter(v -> v.getStatus() == com.garagelog.model.VehicleStatus.SOLD).count();
        long repairCount = vehicles.stream().filter(v -> v.getStatus() == com.garagelog.model.VehicleStatus.UNDER_REPAIR).count();
        long storageCount = vehicles.stream().filter(v -> v.getStatus() == com.garagelog.model.VehicleStatus.IN_STORAGE).count();

        assertTrue(activeCount >= 1, "Should have ACTIVE vehicle");
        assertTrue(soldCount >= 1, "Should have SOLD vehicle");
        assertTrue(repairCount >= 1, "Should have UNDER_REPAIR vehicle");
        assertTrue(storageCount >= 1, "Should have IN_STORAGE vehicle");

        for (var v : vehicles) {
            assertTrue(!maintenanceService.getRecordsForVehicle(v.getId()).isEmpty(), "Vehicle " + v.getPlateNumber() + " should have maintenance");
            assertTrue(!expenseService.getExpensesForVehicle(v.getId()).isEmpty(), "Vehicle " + v.getPlateNumber() + " should have expenses");
            assertTrue(!fuelService.getFillUpsForVehicle(v.getId()).isEmpty(), "Vehicle " + v.getPlateNumber() + " should have fuel fill-ups");
        }

        var reminders = reminderService.getActiveReminders();
        assertTrue(reminders.size() > 3, "Should have more than 3 reminders to exercise scrolling");

        runOnFx(() -> {
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
        });
    }
}
