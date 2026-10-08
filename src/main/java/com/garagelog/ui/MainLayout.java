package com.garagelog.ui;

import com.garagelog.model.Settings;
import com.garagelog.model.Vehicle;
import com.garagelog.repository.GarageRepository;
import com.garagelog.service.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainLayout extends BorderPane implements NavigationListener {
    private final Stage stage;
    private final GarageRepository repository;
    private final VehicleService vehicleService;
    private final MaintenanceService maintenanceService;
    private final ExpenseService expenseService;
    private final FuelService fuelService;
    private final ReportService reportService;
    private final ReminderService reminderService;
    private final SettingsService settingsService;

    private Button btnNavDashboard;
    private Button btnNavVehicles;
    private Button btnNavReports;
    private Button btnNavData;
    private Button btnNavSettings;

    private Label workspaceMetaLabel;
    private Label backupStatusLabel;
    private StackPane contentArea;

    private enum ActiveSection {
        DASHBOARD, VEHICLES, VEHICLE_DETAIL, REPORTS, DATA, SETTINGS
    }

    private ActiveSection currentSection = ActiveSection.DASHBOARD;
    private Vehicle currentDetailVehicle = null;

    public MainLayout(Stage stage, GarageRepository repository, VehicleService vehicleService,
                      MaintenanceService maintenanceService, ExpenseService expenseService,
                      FuelService fuelService, ReportService reportService,
                      ReminderService reminderService, SettingsService settingsService) {
        this.stage = stage;
        this.repository = repository;
        this.vehicleService = vehicleService;
        this.maintenanceService = maintenanceService;
        this.expenseService = expenseService;
        this.fuelService = fuelService;
        this.reportService = reportService;
        this.reminderService = reminderService;
        this.settingsService = settingsService;

        setLeft(createSidebar());
        contentArea = new StackPane();
        setCenter(contentArea);
        setBottom(createStatusBar());

        navigateToDashboard();
    }

    private Node createSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(220);
        sidebar.getStyleClass().add("sidebar");

        // Brand logo
        HBox brand = new HBox(4);
        brand.setAlignment(Pos.BASELINE_LEFT);
        Label slash = new Label("//");
        slash.getStyleClass().add("brand-slash");
        Label brandName = new Label(" GarageLog");
        brandName.getStyleClass().add("brand-label");
        brand.getChildren().addAll(slash, brandName);

        Label sub = new Label("Vehicle ledger");
        sub.getStyleClass().add("brand-subtitle");
        VBox brandBox = new VBox(2, brand, sub);
        brandBox.setPadding(new Insets(0, 0, 16, 0));

        // Nav Buttons
        btnNavDashboard = createNavButton("Dashboard");
        btnNavDashboard.setOnAction(e -> navigateToDashboard());

        btnNavVehicles = createNavButton("Vehicles");
        btnNavVehicles.setOnAction(e -> navigateToVehicles());

        btnNavReports = createNavButton("Reports");
        btnNavReports.setOnAction(e -> navigateToReports());

        btnNavData = createNavButton("Data");
        btnNavData.setOnAction(e -> navigateToData());

        btnNavSettings = createNavButton("Settings");
        btnNavSettings.setOnAction(e -> navigateToSettings());

        VBox navBox = new VBox(6, btnNavDashboard, btnNavVehicles, btnNavReports, btnNavData, btnNavSettings);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Local workspace info
        VBox metaBox = new VBox(4);
        metaBox.setPadding(new Insets(16, 0, 8, 0));
        metaBox.setStyle("-fx-border-color: #E2E8F0 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        Label wsHeader = new Label("LOCAL WORKSPACE");
        wsHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        Label wsName = new Label("Personal garage");
        wsName.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Settings settings = settingsService.getSettings();
        workspaceMetaLabel = new Label(settings.getCurrency() + " · " + settings.getDistanceUnit());
        workspaceMetaLabel.getStyleClass().add("sidebar-meta");

        Label storageLabel = new Label("Stored on this device");
        storageLabel.getStyleClass().add("sidebar-meta");

        Button exitBtn = new Button("Exit");
        exitBtn.getStyleClass().add("sidebar-exit-button");
        exitBtn.setOnAction(e -> {
            Platform.exit();
        });

        metaBox.getChildren().addAll(wsHeader, wsName, workspaceMetaLabel, storageLabel, exitBtn);

        sidebar.getChildren().addAll(brandBox, navBox, spacer, metaBox);
        return sidebar;
    }

    private Button createNavButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("nav-button");
        return b;
    }

    private Node createStatusBar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");

        Label savedLabel = new Label("✓ All changes saved");
        savedLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        backupStatusLabel = new Label();
        updateBackupStatus();

        bar.getChildren().addAll(savedLabel, spacer, backupStatusLabel);
        return bar;
    }

    private void updateBackupStatus() {
        List<Path> backups = repository.listBackups();
        if (backups.isEmpty()) {
            backupStatusLabel.setText("No backups yet · Local data");
        } else {
            try {
                Instant time = Files.getLastModifiedTime(backups.get(0)).toInstant();
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());
                backupStatusLabel.setText("Last backup: " + dtf.format(time) + " · Local data");
            } catch (Exception ex) {
                backupStatusLabel.setText("Local data");
            }
        }
    }

    private void setActiveNavButton(Button active) {
        btnNavDashboard.getStyleClass().remove("nav-button-selected");
        btnNavVehicles.getStyleClass().remove("nav-button-selected");
        btnNavReports.getStyleClass().remove("nav-button-selected");
        btnNavData.getStyleClass().remove("nav-button-selected");
        btnNavSettings.getStyleClass().remove("nav-button-selected");

        if (active != null && !active.getStyleClass().contains("nav-button-selected")) {
            active.getStyleClass().add("nav-button-selected");
        }
    }

    @Override
    public void navigateToDashboard() {
        currentSection = ActiveSection.DASHBOARD;
        setActiveNavButton(btnNavDashboard);
        contentArea.getChildren().setAll(new DashboardView(
                vehicleService, maintenanceService, expenseService, fuelService,
                reminderService, settingsService, this
        ));
    }

    @Override
    public void navigateToVehicles() {
        currentSection = ActiveSection.VEHICLES;
        setActiveNavButton(btnNavVehicles);
        contentArea.getChildren().setAll(new VehiclesView(vehicleService, this));
    }

    @Override
    public void navigateToVehicleDetail(Vehicle vehicle) {
        currentSection = ActiveSection.VEHICLE_DETAIL;
        currentDetailVehicle = vehicle;
        setActiveNavButton(btnNavVehicles);
        contentArea.getChildren().setAll(new VehicleDetailView(
                vehicle, vehicleService, maintenanceService, expenseService,
                fuelService, reportService, settingsService, this
        ));
    }

    @Override
    public void navigateToReports() {
        currentSection = ActiveSection.REPORTS;
        setActiveNavButton(btnNavReports);
        contentArea.getChildren().setAll(new ReportsView(
                vehicleService, maintenanceService, expenseService, fuelService,
                reportService, settingsService, this
        ));
    }

    @Override
    public void navigateToData() {
        currentSection = ActiveSection.DATA;
        setActiveNavButton(btnNavData);
        contentArea.getChildren().setAll(new DataView(repository, vehicleService, this));
    }

    @Override
    public void navigateToSettings() {
        currentSection = ActiveSection.SETTINGS;
        setActiveNavButton(btnNavSettings);
        contentArea.getChildren().setAll(new SettingsView(settingsService, this));
    }

    @Override
    public void refreshAll() {
        Settings settings = settingsService.getSettings();
        workspaceMetaLabel.setText(settings.getCurrency() + " · " + settings.getDistanceUnit());
        updateBackupStatus();

        switch (currentSection) {
            case DASHBOARD -> navigateToDashboard();
            case VEHICLES -> navigateToVehicles();
            case VEHICLE_DETAIL -> {
                if (currentDetailVehicle != null) {
                    vehicleService.findById(currentDetailVehicle.getId()).ifPresentOrElse(
                            this::navigateToVehicleDetail,
                            this::navigateToVehicles
                    );
                } else {
                    navigateToVehicles();
                }
            }
            case REPORTS -> navigateToReports();
            case DATA -> navigateToData();
            case SETTINGS -> navigateToSettings();
        }
    }
}
