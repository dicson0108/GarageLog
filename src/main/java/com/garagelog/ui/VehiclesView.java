package com.garagelog.ui;

import com.garagelog.model.*;
import com.garagelog.service.VehicleService;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

public class VehiclesView extends VBox {
    private final VehicleService vehicleService;
    private final NavigationListener navListener;

    private TextField searchField;
    private ComboBox<String> statusFilter;
    private ComboBox<String> typeFilter;
    private ComboBox<String> fuelFilter;
    private CheckBox showSoldCheckBox;
    private ComboBox<String> sortByBox;

    private TableView<Vehicle> vehicleTable;
    private HBox selectionBar;
    private Label selectionLabel;
    private Button viewBtn;
    private Button editBtn;
    private Button markSoldBtn;
    private Button deleteBtn;

    private Label subtitleLabel;
    private StackPane tableContainer;
    private VBox emptyStateBox;
    private Label emptyTitle;
    private Label emptySubtext;
    private Button emptyActionBtn;

    public VehiclesView(VehicleService vehicleService, NavigationListener navListener) {
        this.vehicleService = vehicleService;
        this.navListener = navListener;

        setSpacing(16);
        setPadding(new Insets(24, 32, 32, 32));
        getStyleClass().add("main-content");

        buildUI();
        refreshData();
    }

    private void buildUI() {
        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Vehicles");
        title.getStyleClass().add("page-title");

        subtitleLabel = new Label("One ledger for every journey.");
        subtitleLabel.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addVehicleBtn = new Button("+ Add Vehicle");
        addVehicleBtn.getStyleClass().add("btn-primary");
        addVehicleBtn.setOnAction(e -> {
            VehicleDialog dlg = new VehicleDialog(null, vehicleService);
            dlg.showAndWait().ifPresent(v -> {
                refreshData();
                navListener.refreshAll();
            });
        });

        header.getChildren().addAll(titleBox, spacer, addVehicleBtn);
        getChildren().add(header);

        // Filter / Controls Card
        VBox controlsCard = new VBox(12);
        controlsCard.getStyleClass().add("card");

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍 Search plate, nickname, brand or model");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, o, n) -> refreshData());

        statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("Status: All", "Active", "In storage", "Under repair", "Sold");
        statusFilter.setValue("Status: All");
        statusFilter.setOnAction(e -> refreshData());

        typeFilter = new ComboBox<>();
        typeFilter.getItems().addAll("Type: All", "Car", "Motorcycle", "Truck/Van", "Other");
        typeFilter.setValue("Type: All");
        typeFilter.setOnAction(e -> refreshData());

        fuelFilter = new ComboBox<>();
        fuelFilter.getItems().addAll("Fuel: All", "Petrol", "Diesel", "Hybrid", "Electric", "Other");
        fuelFilter.setValue("Fuel: All");
        fuelFilter.setOnAction(e -> refreshData());

        filterRow.getChildren().addAll(searchField, statusFilter, typeFilter, fuelFilter);

        HBox subRow = new HBox(16);
        subRow.setAlignment(Pos.CENTER_LEFT);

        showSoldCheckBox = new CheckBox("Show sold vehicles");
        showSoldCheckBox.setSelected(true);
        showSoldCheckBox.setOnAction(e -> refreshData());

        Region subSpacer = new Region();
        HBox.setHgrow(subSpacer, Priority.ALWAYS);

        Label sortLabel = new Label("Sort by");
        sortLabel.setStyle("-fx-text-fill: #64748B;");

        sortByBox = new ComboBox<>();
        sortByBox.getItems().addAll("Plate", "Brand", "Year", "Odometer");
        sortByBox.setValue("Plate");
        sortByBox.setOnAction(e -> refreshData());

        subRow.getChildren().addAll(showSoldCheckBox, subSpacer, sortLabel, sortByBox);
        controlsCard.getChildren().addAll(filterRow, subRow);
        getChildren().add(controlsCard);

        // Table
        vehicleTable = new TableView<>();
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(vehicleTable, Priority.ALWAYS);

        TableColumn<Vehicle, String> plateCol = new TableColumn<>("Plate ↑");
        plateCol.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getPlateNumber()));
        plateCol.setPrefWidth(120);

        TableColumn<Vehicle, String> nickCol = new TableColumn<>("Nickname");
        nickCol.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getNickname() != null ? v.getValue().getNickname() : "-"));
        nickCol.setPrefWidth(150);

        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand / model");
        brandCol.setCellValueFactory(v -> new SimpleStringProperty((v.getValue().getBrand() != null ? v.getValue().getBrand() : "") + " " + (v.getValue().getModel() != null ? v.getValue().getModel() : "")));
        brandCol.setPrefWidth(180);

        TableColumn<Vehicle, String> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(v -> new SimpleStringProperty(String.valueOf(v.getValue().getYear())));
        yearCol.setPrefWidth(80);

        TableColumn<Vehicle, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(v -> new SimpleStringProperty(v.getValue().getStatus() != null ? v.getValue().getStatus().getDisplayName() : "-"));
        statusCol.setPrefWidth(110);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("Sold".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-sold");
                    } else {
                        badge.getStyleClass().add("badge-active");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Vehicle, String> odoCol = new TableColumn<>("Odometer (km)");
        odoCol.setCellValueFactory(v -> new SimpleStringProperty(String.format("%,d", (int) v.getValue().getCurrentOdometer())));
        odoCol.setPrefWidth(120);

        vehicleTable.getColumns().addAll(plateCol, nickCol, brandCol, yearCol, statusCol, odoCol);

        vehicleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            updateSelectionBar(selected);
        });

        // Double click to view vehicle details
        vehicleTable.setRowFactory(tv -> {
            TableRow<Vehicle> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    navListener.navigateToVehicleDetail(row.getItem());
                }
            });
            return row;
        });

        vehicleTable.setPlaceholder(new Label(""));

        // Empty state overlay
        emptyStateBox = new VBox(14);
        emptyStateBox.setAlignment(Pos.CENTER);
        emptyStateBox.setPadding(new Insets(40));
        emptyStateBox.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E8EAED; -fx-border-radius: 10; -fx-border-width: 1;");
        emptyTitle = new Label("No vehicles yet");
        emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        emptySubtext = new Label("Add your first vehicle to start tracking maintenance and fuel.");
        emptySubtext.setStyle("-fx-text-fill: #64748B;");
        emptyActionBtn = new Button("Add Vehicle");
        emptyActionBtn.getStyleClass().add("btn-primary");
        emptyActionBtn.setOnAction(e -> {
            VehicleDialog dlg = new VehicleDialog(null, vehicleService);
            dlg.showAndWait().ifPresent(v -> {
                refreshData();
                navListener.refreshAll();
            });
        });
        emptyStateBox.getChildren().addAll(emptyTitle, emptySubtext, emptyActionBtn);

        tableContainer = new StackPane(vehicleTable, emptyStateBox);
        VBox.setVgrow(tableContainer, Priority.ALWAYS);
        getChildren().add(tableContainer);

        // Selection Action Bar
        selectionBar = new HBox(12);
        selectionBar.setAlignment(Pos.CENTER_LEFT);
        selectionBar.getStyleClass().add("card");
        selectionBar.setStyle("-fx-padding: 10 16; -fx-background-color: #FFF7ED; -fx-border-color: #FED7AA;");
        selectionBar.setVisible(false);
        selectionBar.setManaged(false);

        Label badgeSelected = new Label("1 selected");
        badgeSelected.setStyle("-fx-background-color: #FF8000; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px;");

        selectionLabel = new Label();
        selectionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Region barSpacer = new Region();
        HBox.setHgrow(barSpacer, Priority.ALWAYS);

        viewBtn = new Button("View");
        viewBtn.getStyleClass().add("btn-primary");
        viewBtn.setOnAction(e -> {
            Vehicle sel = vehicleTable.getSelectionModel().getSelectedItem();
            if (sel != null) navListener.navigateToVehicleDetail(sel);
        });

        editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Vehicle sel = vehicleTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                VehicleDialog dlg = new VehicleDialog(sel, vehicleService);
                dlg.showAndWait().ifPresent(v -> {
                    refreshData();
                    navListener.refreshAll();
                });
            }
        });

        markSoldBtn = new Button("Mark as Sold");
        markSoldBtn.getStyleClass().add("btn-secondary");
        markSoldBtn.setOnAction(e -> {
            Vehicle sel = vehicleTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Mark as Sold");
                confirm.setHeaderText("Archive " + sel.getPlateNumber() + "?");
                confirm.setContentText("The vehicle will be marked as sold and hidden from active views. Its full service history will be retained.");
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    vehicleService.markAsSold(sel.getId());
                    refreshData();
                    navListener.refreshAll();
                }
            }
        });

        deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Vehicle sel = vehicleTable.getSelectionModel().getSelectedItem();
            if (sel != null) {
                int linked = vehicleService.countLinkedRecords(sel.getId());
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Delete Vehicle");
                confirm.setHeaderText("Permanently delete " + sel.getPlateNumber() + "?");
                confirm.setContentText("This will permanently remove the vehicle and its " + linked + " linked records (maintenance, expenses, fuel). This action cannot be undone.");
                Optional<ButtonType> res = confirm.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    vehicleService.deleteVehicle(sel.getId());
                    refreshData();
                    navListener.refreshAll();
                }
            }
        });

        selectionBar.getChildren().addAll(badgeSelected, selectionLabel, barSpacer, viewBtn, editBtn, markSoldBtn, deleteBtn);
        getChildren().add(selectionBar);
    }

    private void updateSelectionBar(Vehicle vehicle) {
        if (vehicle == null) {
            selectionBar.setVisible(false);
            selectionBar.setManaged(false);
        } else {
            selectionBar.setVisible(true);
            selectionBar.setManaged(true);
            selectionLabel.setText(vehicle.getPlateNumber() + " · " + (vehicle.getNickname() != null ? vehicle.getNickname() : vehicle.getBrand()));
            markSoldBtn.setDisable(vehicle.getStatus() == VehicleStatus.SOLD);
        }
    }

    public void refreshData() {
        String q = searchField != null ? searchField.getText() : "";
        boolean includeSold = showSoldCheckBox != null && showSoldCheckBox.isSelected();

        VehicleStatus status = null;
        if (statusFilter != null && !statusFilter.getValue().equals("Status: All")) {
            for (VehicleStatus s : VehicleStatus.values()) {
                if (s.getDisplayName().equalsIgnoreCase(statusFilter.getValue())) {
                    status = s;
                    break;
                }
            }
        }

        VehicleType type = null;
        if (typeFilter != null && !typeFilter.getValue().equals("Type: All")) {
            for (VehicleType t : VehicleType.values()) {
                if (t.getDisplayName().equalsIgnoreCase(typeFilter.getValue())) {
                    type = t;
                    break;
                }
            }
        }

        FuelType fuel = null;
        if (fuelFilter != null && !fuelFilter.getValue().equals("Fuel: All")) {
            for (FuelType f : FuelType.values()) {
                if (f.getDisplayName().equalsIgnoreCase(fuelFilter.getValue())) {
                    fuel = f;
                    break;
                }
            }
        }

        String sort = sortByBox != null ? sortByBox.getValue() : "Plate";

        List<Vehicle> list = vehicleService.searchAndFilter(q, status, type, fuel, includeSold, sort, true);
        vehicleTable.getItems().setAll(list);

        List<Vehicle> all = vehicleService.getAllVehicles(true);
        long activeCount = all.stream().filter(v -> v.getStatus() != VehicleStatus.SOLD).count();
        long soldCount = all.size() - activeCount;
        subtitleLabel.setText("One ledger for every journey. " + activeCount + " active vehicles · " + soldCount + " sold.");

        if (all.isEmpty()) {
            vehicleTable.setVisible(false);
            emptyStateBox.setVisible(true);
            emptyTitle.setText("No vehicles yet");
            emptySubtext.setText("Add your first vehicle to start tracking maintenance and fuel.");
            emptyActionBtn.setVisible(true);
            emptyActionBtn.setText("Add Vehicle");
            emptyActionBtn.setOnAction(e -> {
                VehicleDialog dlg = new VehicleDialog(null, vehicleService);
                dlg.showAndWait().ifPresent(v -> {
                    refreshData();
                    navListener.refreshAll();
                });
            });
        } else if (list.isEmpty()) {
            vehicleTable.setVisible(false);
            emptyStateBox.setVisible(true);
            emptyTitle.setText("No matching vehicles");
            emptySubtext.setText("No vehicles match your current search and filter settings.");
            emptyActionBtn.setVisible(true);
            emptyActionBtn.setText("Clear Filters");
            emptyActionBtn.setOnAction(e -> {
                searchField.clear();
                statusFilter.setValue("Status: All");
                typeFilter.setValue("Type: All");
                fuelFilter.setValue("Fuel: All");
                showSoldCheckBox.setSelected(true);
                refreshData();
            });
        } else {
            vehicleTable.setVisible(true);
            emptyStateBox.setVisible(false);
        }
    }
}
