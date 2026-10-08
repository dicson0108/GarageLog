package com.garagelog.ui;

import com.garagelog.repository.GarageRepository;
import com.garagelog.service.VehicleService;
import com.garagelog.util.CsvUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DataView extends ScrollPane {
    private final GarageRepository repository;
    private final VehicleService vehicleService;
    private final NavigationListener navListener;

    private TableView<Path> backupTable;

    public DataView(GarageRepository repository, VehicleService vehicleService, NavigationListener navListener) {
        this.repository = repository;
        this.vehicleService = vehicleService;
        this.navListener = navListener;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        buildUI();
    }

    private void buildUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 32, 32, 32));
        root.getStyleClass().add("main-content");

        // Header
        VBox titleBox = new VBox(4);
        Label title = new Label("Data Management");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Export, import, and backup your garage ledger.");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        root.getChildren().add(titleBox);

        // Card 1: Import & Export
        VBox csvCard = new VBox(12);
        csvCard.getStyleClass().add("card");

        Label csvTitle = new Label("CSV Export & Import");
        csvTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label csvDesc = new Label("Export vehicles to CSV for spreadsheet analysis, or import vehicles from another file.");
        csvDesc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        HBox csvButtons = new HBox(12);
        Button exportBtn = new Button("Export Vehicles (CSV)");
        exportBtn.getStyleClass().add("btn-secondary");
        exportBtn.setOnAction(e -> handleExportCsv());

        Button importBtn = new Button("Import Vehicles (CSV)");
        importBtn.getStyleClass().add("btn-primary");
        importBtn.setOnAction(e -> handleImportCsv());

        csvButtons.getChildren().addAll(exportBtn, importBtn);
        csvCard.getChildren().addAll(csvTitle, csvDesc, csvButtons);
        root.getChildren().add(csvCard);

        // Card 2: Backups & Restores
        VBox backupCard = new VBox(12);
        backupCard.getStyleClass().add("card");

        HBox bkHeader = new HBox();
        bkHeader.setAlignment(Pos.CENTER_LEFT);
        Label bkTitle = new Label("Automatic Backups & Restore");
        bkTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button createBackupBtn = new Button("Create Backup Now");
        createBackupBtn.getStyleClass().add("btn-secondary");
        createBackupBtn.setOnAction(e -> {
            Path created = repository.createBackup();
            if (created != null) {
                new Alert(Alert.AlertType.INFORMATION, "Backup created: " + created.getFileName()).showAndWait();
                refreshBackups();
            }
        });

        bkHeader.getChildren().addAll(bkTitle, sp, createBackupBtn);

        Label bkDesc = new Label("The app retains the last 10 backups in your local application directory. You can restore from any point.");
        bkDesc.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        backupTable = new TableView<>();
        backupTable.setPrefHeight(200);
        backupTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        TableColumn<Path, String> fileCol = new TableColumn<>("Backup File");
        fileCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFileName().toString()));
        fileCol.setPrefWidth(280);

        TableColumn<Path, String> dateCol = new TableColumn<>("Timestamp");
        dateCol.setCellValueFactory(d -> {
            try {
                return new SimpleStringProperty(dtf.format(Files.getLastModifiedTime(d.getValue()).toInstant()));
            } catch (Exception ex) {
                return new SimpleStringProperty("-");
            }
        });
        dateCol.setPrefWidth(160);

        TableColumn<Path, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(d -> {
            try {
                return new SimpleStringProperty(Files.size(d.getValue()) / 1024 + " KB");
            } catch (Exception ex) {
                return new SimpleStringProperty("-");
            }
        });
        sizeCol.setPrefWidth(90);

        TableColumn<Path, Void> restoreCol = new TableColumn<>("Action");
        restoreCol.setCellFactory(col -> new TableCell<>() {
            private final Button restoreBtn = new Button("Restore");
            {
                restoreBtn.getStyleClass().add("btn-primary");
                restoreBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 10;");
                restoreBtn.setOnAction(e -> {
                    Path backup = getTableRow().getItem();
                    if (backup != null) {
                        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
                        c.setTitle("Restore Backup");
                        c.setHeaderText("Restore from " + backup.getFileName() + "?");
                        c.setContentText("Current ledger data will be replaced by this backup snapshot. Continue?");
                        if (c.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                            repository.restoreBackup(backup);
                            vehicleService.reload();
                            navListener.refreshAll();
                            new Alert(Alert.AlertType.INFORMATION, "Backup restored successfully.").showAndWait();
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : restoreBtn);
            }
        });

        backupTable.getColumns().addAll(fileCol, dateCol, sizeCol, restoreCol);
        backupCard.getChildren().addAll(bkHeader, bkDesc, backupTable);
        root.getChildren().add(backupCard);

        refreshBackups();
        setContent(root);
    }

    private void refreshBackups() {
        List<Path> backups = repository.listBackups();
        backupTable.getItems().setAll(backups);
    }

    private void handleExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Vehicles CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("vehicles-export.csv");

        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                String csv = CsvUtil.exportVehiclesToCsv(vehicleService.getAllVehicles(true));
                Files.writeString(file.toPath(), csv);
                new Alert(Alert.AlertType.INFORMATION, "Exported " + vehicleService.getAllVehicles(true).size() + " vehicles to " + file.getName()).showAndWait();
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to export: " + ex.getMessage()).showAndWait();
            }
        }
    }

    private void handleImportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Vehicles CSV to Import");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                CsvUtil.ImportReport report = CsvUtil.importVehiclesFromCsv(content, vehicleService);
                navListener.refreshAll();

                StringBuilder msg = new StringBuilder();
                msg.append("Import completed:\n")
                   .append("• Added: ").append(report.addedCount()).append(" vehicles\n")
                   .append("• Skipped duplicates: ").append(report.skippedCount()).append(" vehicles\n");
                if (!report.errorLines().isEmpty()) {
                    msg.append("• Errors:\n");
                    for (String err : report.errorLines()) {
                        msg.append("  - ").append(err).append("\n");
                    }
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("CSV Import Results");
                alert.setHeaderText("Import summary");
                alert.setContentText(msg.toString());
                alert.showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Import failed: " + ex.getMessage()).showAndWait();
            }
        }
    }
}
