package com.garagelog.ui;

import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.service.*;
import com.garagelog.util.SampleDataGenerator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GarageLogApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Storage & Startup Backup
        JsonGarageRepository repository = new JsonGarageRepository();
        try {
            repository.createBackup();
        } catch (Exception ex) {
            System.err.println("Startup backup warning: " + ex.getMessage());
        }

        // 2. Services
        VehicleService vehicleService = new VehicleService(repository);
        MaintenanceService maintenanceService = new MaintenanceService(repository, vehicleService);
        ExpenseService expenseService = new ExpenseService(repository, vehicleService);
        FuelService fuelService = new FuelService(repository, vehicleService);
        ReminderService reminderService = new ReminderService(vehicleService, maintenanceService);
        SettingsService settingsService = new SettingsService(repository, vehicleService);
        ReportService reportService = new ReportService(vehicleService, maintenanceService, expenseService, fuelService);

        // 3. Seed sample data if empty
        SampleDataGenerator.populateIfEmpty(vehicleService, maintenanceService, expenseService, fuelService);

        // 4. UI Layout & Scene
        MainLayout mainLayout = new MainLayout(
                repository, vehicleService, maintenanceService,
                expenseService, fuelService, reportService, reminderService, settingsService
        );

        Scene scene = new Scene(mainLayout, 1200, 780);
        String css = getClass().getResource("/css/mclaren-theme.css") != null
                ? getClass().getResource("/css/mclaren-theme.css").toExternalForm()
                : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        primaryStage.setTitle("GarageLog — Enhanced Vehicle Management System");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
