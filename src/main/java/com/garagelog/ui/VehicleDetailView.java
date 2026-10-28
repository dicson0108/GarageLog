package com.garagelog.ui;

import com.garagelog.model.*;
import com.garagelog.service.*;
import com.garagelog.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;

public class VehicleDetailView extends VBox {
    private final Vehicle vehicle;
    private final VehicleService vehicleService;
    private final MaintenanceService maintenanceService;
    private final ExpenseService expenseService;
    private final FuelService fuelService;
    private final ReportService reportService;
    private final SettingsService settingsService;
    private final NavigationListener navListener;

    private Label titleLabel;
    private Label subtitleLabel;
    private Label statusBadge;
    private TabPane tabPane;
    private TableView<MaintenanceRecord> maintenanceTable;
    private TableView<Expense> expenseTable;
    private TableView<FuelFillUp> fuelTable;
    private Tab costTab;
    private HBox fuelStatRow;

    public VehicleDetailView(Vehicle vehicle, VehicleService vehicleService,
                             MaintenanceService maintenanceService, ExpenseService expenseService,
                             FuelService fuelService, ReportService reportService,
                             SettingsService settingsService, NavigationListener navListener) {
        this.vehicle = vehicle;
        this.vehicleService = vehicleService;
        this.maintenanceService = maintenanceService;
        this.expenseService = expenseService;
        this.fuelService = fuelService;
        this.reportService = reportService;
        this.settingsService = settingsService;
        this.navListener = navListener;

        setSpacing(16);
        setPadding(new Insets(20, 32, 32, 32));
        getStyleClass().add("main-content");

        buildUI();
    }

    private void buildUI() {
        // Back navigation
        Hyperlink backLink = new Hyperlink("← Back to vehicles");
        backLink.setStyle("-fx-text-fill: #FF8000; -fx-font-weight: bold;");
        backLink.setOnAction(e -> navListener.navigateToVehicles());
        getChildren().add(backLink);

        // Header
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        titleLabel = new Label((vehicle.getNickname() != null && !vehicle.getNickname().isBlank() ? vehicle.getNickname() + " · " : "") +
                vehicle.getBrand() + " " + vehicle.getModel());
        titleLabel.getStyleClass().add("page-title");

        HBox subBox = new HBox(10);
        subBox.setAlignment(Pos.CENTER_LEFT);

        subtitleLabel = new Label(vehicle.getPlateNumber() + " · " + vehicle.getYear() + " · " +
                String.format("%,d", (int) vehicle.getCurrentOdometer()) + " " + settingsService.getSettings().getDistanceUnit());
        subtitleLabel.getStyleClass().add("page-subtitle");

        statusBadge = new Label(vehicle.getStatus().getDisplayName());
        statusBadge.getStyleClass().add(vehicle.getStatus() == VehicleStatus.SOLD ? "badge-sold" : "badge-active");

        subBox.getChildren().addAll(subtitleLabel, statusBadge);
        titleBox.getChildren().addAll(titleLabel, subBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit Vehicle");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            VehicleDialog dlg = new VehicleDialog(vehicle, vehicleService);
            dlg.showAndWait().ifPresent(v -> {
                refreshHeader();
                navListener.refreshAll();
            });
        });

        Button markSoldBtn = new Button("Mark as Sold");
        markSoldBtn.getStyleClass().add("btn-secondary");
        markSoldBtn.setDisable(vehicle.getStatus() == VehicleStatus.SOLD);
        markSoldBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Mark as Sold");
            confirm.setHeaderText("Archive " + vehicle.getPlateNumber() + "?");
            confirm.setContentText("The vehicle will be marked as sold. History will be kept.");
            if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                vehicleService.markAsSold(vehicle.getId());
                refreshHeader();
                navListener.refreshAll();
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            int linked = vehicleService.countLinkedRecords(vehicle.getId());
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Vehicle");
            confirm.setHeaderText("Permanently delete " + vehicle.getPlateNumber() + "?");
            confirm.setContentText("Will delete vehicle and all " + linked + " linked records. Proceed?");
            if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isPresent()) {
                vehicleService.deleteVehicle(vehicle.getId());
                navListener.navigateToVehicles();
            }
        });

        header.getChildren().addAll(titleBox, spacer, editBtn, markSoldBtn, deleteBtn);
        getChildren().add(header);

        // TabPane
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Tab overviewTab = new Tab("Overview", createOverviewTab());
        Tab maintTab = new Tab("Maintenance", createMaintenanceTab());
        Tab expTab = new Tab("Expenses", createExpensesTab());
        Tab fuelTab = new Tab("Fuel", createFuelTab());
        costTab = new Tab("Cost Summary", createCostSummaryTab());

        tabPane.getTabs().addAll(overviewTab, maintTab, expTab, fuelTab, costTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == expTab && expenseTable != null) {
                expenseTable.requestLayout();
            } else if (newTab == maintTab && maintenanceTable != null) {
                maintenanceTable.requestLayout();
            } else if (newTab == fuelTab && fuelTable != null) {
                fuelTable.requestLayout();
            }
        });

        getChildren().add(tabPane);
    }

    private void refreshHeader() {
        titleLabel.setText((vehicle.getNickname() != null && !vehicle.getNickname().isBlank() ? vehicle.getNickname() + " · " : "") +
                vehicle.getBrand() + " " + vehicle.getModel());
        subtitleLabel.setText(vehicle.getPlateNumber() + " · " + vehicle.getYear() + " · " +
                String.format("%,d", (int) vehicle.getCurrentOdometer()) + " " + settingsService.getSettings().getDistanceUnit());
        statusBadge.setText(vehicle.getStatus().getDisplayName());
        statusBadge.getStyleClass().setAll(vehicle.getStatus() == VehicleStatus.SOLD ? "badge-sold" : "badge-active");
    }

    private Node createOverviewTab() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(16, 0, 0, 0));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.getStyleClass().add("card");

        int r = 0;
        addDetailRow(grid, r++, "License Plate:", vehicle.getPlateNumber());
        addDetailRow(grid, r++, "Nickname:", vehicle.getNickname() != null ? vehicle.getNickname() : "-");
        addDetailRow(grid, r++, "Brand & Model:", vehicle.getBrand() + " " + vehicle.getModel());
        addDetailRow(grid, r++, "Year:", String.valueOf(vehicle.getYear()));
        addDetailRow(grid, r++, "Vehicle Type:", vehicle.getType().getDisplayName());
        addDetailRow(grid, r++, "Fuel Type:", vehicle.getFuelType().getDisplayName());
        addDetailRow(grid, r++, "Colour:", vehicle.getColour() != null ? vehicle.getColour() : "-");
        addDetailRow(grid, r++, "VIN:", vehicle.getVin() != null ? vehicle.getVin() : "-");
        addDetailRow(grid, r++, "Current Odometer:", String.format("%,d %s", (int) vehicle.getCurrentOdometer(), settingsService.getSettings().getDistanceUnit()));
        addDetailRow(grid, r++, "Purchase Date:", vehicle.getPurchaseDate() != null ? DateUtil.format(vehicle.getPurchaseDate()) : "-");

        String currency = settingsService.getSettings().getCurrency();
        addDetailRow(grid, r++, "Purchase Price:", vehicle.getPurchasePrice() != null ? currency + " " + vehicle.getPurchasePrice().setScale(2) : "-");
        addDetailRow(grid, r++, "Insurance Expiry:", vehicle.getInsuranceExpiryDate() != null ? DateUtil.format(vehicle.getInsuranceExpiryDate()) : "-");
        addDetailRow(grid, r++, "Road Tax Expiry:", vehicle.getRoadTaxExpiryDate() != null ? DateUtil.format(vehicle.getRoadTaxExpiryDate()) : "-");
        addDetailRow(grid, r++, "Notes:", vehicle.getNotes() != null && !vehicle.getNotes().isBlank() ? vehicle.getNotes() : "No notes recorded.");

        root.getChildren().add(grid);
        return root;
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #0F172A;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private void refreshMaintenance() {
        if (maintenanceTable != null) {
            maintenanceTable.getItems().setAll(maintenanceService.getRecordsForVehicle(vehicle.getId()));
        }
    }

    private void refreshExpenses() {
        if (expenseTable != null) {
            expenseTable.getItems().setAll(expenseService.getExpensesForVehicle(vehicle.getId()));
        }
    }

    private void refreshFuel() {
        if (fuelTable != null) {
            fuelTable.getItems().setAll(fuelService.getFillUpsForVehicle(vehicle.getId()));
        }
        updateFuelStats();
    }

    private void updateFuelStats() {
        if (fuelStatRow == null) return;
        List<FuelService.ConsumptionInterval> intervals = fuelService.calculateConsumptionHistory(vehicle.getId());
        double avgL100 = 0;
        double avgKmL = 0;
        if (!intervals.isEmpty()) {
            double totalDist = intervals.stream().mapToDouble(FuelService.ConsumptionInterval::distanceKm).sum();
            double totalLitres = intervals.stream().mapToDouble(FuelService.ConsumptionInterval::litresConsumed).sum();
            if (totalDist > 0 && totalLitres > 0) {
                avgL100 = (totalLitres / totalDist) * 100.0;
                avgKmL = totalDist / totalLitres;
            }
        }
        fuelStatRow.getChildren().setAll(
                createMiniStat("Average Consumption", avgL100 > 0 ? String.format("%.2f L/100 km", avgL100) : "N/A"),
                createMiniStat("Fuel Efficiency", avgKmL > 0 ? String.format("%.2f km/L", avgKmL) : "N/A"),
                createMiniStat("Total Fuel Spend", settingsService.getSettings().getCurrency() + " " + fuelService.calculateTotalFuelCost(vehicle.getId()).setScale(2))
        );
    }

    private void refreshCostSummary() {
        if (costTab != null) {
            costTab.setContent(createCostSummaryTab());
        }
    }

    private Node createMaintenanceTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 0, 0, 0));

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Maintenance Records");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Maintenance Record");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            MaintenanceDialog dlg = new MaintenanceDialog(vehicle.getId(), null, maintenanceService);
            dlg.showAndWait().ifPresent(m -> {
                refreshHeader();
                refreshMaintenance();
                refreshCostSummary();
            });
        });

        top.getChildren().addAll(title, sp, addBtn);
        root.getChildren().add(top);

        maintenanceTable = new TableView<>();
        maintenanceTable.setPlaceholder(new Label("No maintenance records recorded yet."));
        maintenanceTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        maintenanceTable.setMinHeight(250);
        VBox.setVgrow(maintenanceTable, Priority.ALWAYS);

        TableColumn<MaintenanceRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.format(d.getValue().getDate())));
        dateCol.setMinWidth(110);
        dateCol.setPrefWidth(120);

        TableColumn<MaintenanceRecord, String> odoCol = new TableColumn<>("Odometer");
        odoCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,d", (int) d.getValue().getOdometer())));
        odoCol.setMinWidth(110);
        odoCol.setPrefWidth(120);

        TableColumn<MaintenanceRecord, String> workCol = new TableColumn<>("Work Done");
        workCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWorkDone()));
        workCol.setMinWidth(350);
        workCol.setPrefWidth(450);

        String currency = settingsService.getSettings().getCurrency();
        TableColumn<MaintenanceRecord, String> costCol = new TableColumn<>("Cost");
        costCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().getCost().setScale(2)));
        costCol.setMinWidth(110);
        costCol.setPrefWidth(120);

        TableColumn<MaintenanceRecord, String> shopCol = new TableColumn<>("Workshop");
        shopCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getWorkshop() != null ? d.getValue().getWorkshop() : "-"));
        shopCol.setMinWidth(180);
        shopCol.setPrefWidth(220);

        TableColumn<MaintenanceRecord, String> dueCol = new TableColumn<>("Next Due");
        dueCol.setCellValueFactory(d -> {
            MaintenanceRecord m = d.getValue();
            String s = "";
            if (m.getNextServiceDueDate() != null) s += DateUtil.format(m.getNextServiceDueDate());
            if (m.getNextServiceDueOdometer() != null) s += (s.isEmpty() ? "" : " / ") + String.format("%,d km", m.getNextServiceDueOdometer().intValue());
            return new SimpleStringProperty(s.isEmpty() ? "-" : s);
        });
        dueCol.setMinWidth(180);
        dueCol.setPrefWidth(220);

        TableColumn<MaintenanceRecord, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setMinWidth(160);
        actionCol.setPrefWidth(160);
        actionCol.setResizable(false);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editB = new Button("Edit");
            private final Button delB = new Button("Delete");
            private final HBox pane = new HBox(8, editB, delB);

            {
                editB.setMinWidth(Region.USE_PREF_SIZE);
                delB.setMinWidth(Region.USE_PREF_SIZE);
                pane.setAlignment(Pos.CENTER_LEFT);
                editB.getStyleClass().add("btn-secondary");
                delB.getStyleClass().add("btn-danger");
                editB.setOnAction(e -> {
                    MaintenanceRecord rec = getTableRow().getItem();
                    if (rec != null) {
                        MaintenanceDialog dlg = new MaintenanceDialog(vehicle.getId(), rec, maintenanceService);
                        dlg.showAndWait().ifPresent(res -> {
                            refreshHeader();
                            refreshMaintenance();
                            refreshCostSummary();
                        });
                    }
                });
                delB.setOnAction(e -> {
                    MaintenanceRecord rec = getTableRow().getItem();
                    if (rec != null) {
                        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Delete this maintenance record?", ButtonType.YES, ButtonType.NO);
                        if (c.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                            maintenanceService.deleteRecord(rec.getId());
                            refreshHeader();
                            refreshMaintenance();
                            refreshCostSummary();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        maintenanceTable.getColumns().addAll(dateCol, odoCol, workCol, costCol, shopCol, dueCol, actionCol);
        maintenanceTable.getItems().setAll(maintenanceService.getRecordsForVehicle(vehicle.getId()));

        root.getChildren().add(maintenanceTable);
        return root;
    }

    private Node createExpensesTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 0, 0, 0));

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Expenses");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Expense");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            ExpenseDialog dlg = new ExpenseDialog(vehicle.getId(), null, expenseService);
            dlg.showAndWait().ifPresent(res -> {
                refreshExpenses();
                refreshCostSummary();
            });
        });

        top.getChildren().addAll(title, sp, addBtn);
        root.getChildren().add(top);

        expenseTable = new TableView<>();
        expenseTable.setPlaceholder(new Label("No expenses recorded yet."));
        expenseTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        expenseTable.setMinHeight(250);
        VBox.setVgrow(expenseTable, Priority.ALWAYS);

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.format(d.getValue().getDate())));
        dateCol.setMinWidth(110);
        dateCol.setPrefWidth(120);

        TableColumn<Expense, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory().getDisplayName()));
        catCol.setMinWidth(160);
        catCol.setPrefWidth(180);

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        descCol.setMinWidth(300);
        descCol.setPrefWidth(420);

        String currency = settingsService.getSettings().getCurrency();
        TableColumn<Expense, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().getAmount().setScale(2)));
        amtCol.setMinWidth(120);
        amtCol.setPrefWidth(140);

        TableColumn<Expense, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setMinWidth(160);
        actionCol.setPrefWidth(160);
        actionCol.setResizable(false);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editB = new Button("Edit");
            private final Button delB = new Button("Delete");
            private final HBox pane = new HBox(8, editB, delB);

            {
                editB.setMinWidth(Region.USE_PREF_SIZE);
                delB.setMinWidth(Region.USE_PREF_SIZE);
                pane.setAlignment(Pos.CENTER_LEFT);
                editB.getStyleClass().add("btn-secondary");
                delB.getStyleClass().add("btn-danger");
                editB.setOnAction(e -> {
                    Expense rec = getTableRow().getItem();
                    if (rec != null) {
                        ExpenseDialog dlg = new ExpenseDialog(vehicle.getId(), rec, expenseService);
                        dlg.showAndWait().ifPresent(res -> {
                            refreshExpenses();
                            refreshCostSummary();
                        });
                    }
                });
                delB.setOnAction(e -> {
                    Expense rec = getTableRow().getItem();
                    if (rec != null) {
                        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Delete this expense?", ButtonType.YES, ButtonType.NO);
                        if (c.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                            expenseService.deleteExpense(rec.getId());
                            refreshExpenses();
                            refreshCostSummary();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        expenseTable.getColumns().addAll(dateCol, catCol, descCol, amtCol, actionCol);
        expenseTable.getItems().setAll(expenseService.getExpensesForVehicle(vehicle.getId()));

        root.getChildren().add(expenseTable);
        return root;
    }

    private Node createFuelTab() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16, 0, 0, 0));

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Fuel Log");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Fuel Fill-up");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> {
            FuelDialog dlg = new FuelDialog(vehicle.getId(), null, fuelService, maintenanceService);
            dlg.showAndWait().ifPresent(res -> {
                refreshHeader();
                refreshFuel();
                refreshCostSummary();
            });
        });

        top.getChildren().addAll(title, sp, addBtn);
        root.getChildren().add(top);

        // Fuel Consumption Summary Card
        fuelStatRow = new HBox(16);
        updateFuelStats();
        root.getChildren().add(fuelStatRow);

        fuelTable = new TableView<>();
        fuelTable.setPlaceholder(new Label("No fuel fill-ups recorded yet."));
        fuelTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        fuelTable.setMinHeight(250);
        VBox.setVgrow(fuelTable, Priority.ALWAYS);

        TableColumn<FuelFillUp, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(DateUtil.format(d.getValue().getDate())));
        dateCol.setMinWidth(110);
        dateCol.setPrefWidth(120);

        TableColumn<FuelFillUp, String> odoCol = new TableColumn<>("Odometer");
        odoCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%,d", (int) d.getValue().getOdometer())));
        odoCol.setMinWidth(110);
        odoCol.setPrefWidth(130);

        TableColumn<FuelFillUp, String> litresCol = new TableColumn<>("Litres");
        litresCol.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f L", d.getValue().getLitres())));
        litresCol.setMinWidth(110);
        litresCol.setPrefWidth(130);

        String currency = settingsService.getSettings().getCurrency();
        TableColumn<FuelFillUp, String> costCol = new TableColumn<>("Total Cost");
        costCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().getTotalCost().setScale(2)));
        costCol.setMinWidth(120);
        costCol.setPrefWidth(140);

        TableColumn<FuelFillUp, String> fullCol = new TableColumn<>("Full Tank");
        fullCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isFullTank() ? "Yes" : "Partial"));
        fullCol.setMinWidth(110);
        fullCol.setPrefWidth(120);

        TableColumn<FuelFillUp, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setMinWidth(160);
        actionCol.setPrefWidth(160);
        actionCol.setResizable(false);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editB = new Button("Edit");
            private final Button delB = new Button("Delete");
            private final HBox pane = new HBox(8, editB, delB);

            {
                editB.setMinWidth(Region.USE_PREF_SIZE);
                delB.setMinWidth(Region.USE_PREF_SIZE);
                pane.setAlignment(Pos.CENTER_LEFT);
                editB.getStyleClass().add("btn-secondary");
                delB.getStyleClass().add("btn-danger");
                editB.setOnAction(e -> {
                    FuelFillUp rec = getTableRow().getItem();
                    if (rec != null) {
                        FuelDialog dlg = new FuelDialog(vehicle.getId(), rec, fuelService, maintenanceService);
                        dlg.showAndWait().ifPresent(res -> {
                            refreshHeader();
                            refreshFuel();
                            refreshCostSummary();
                        });
                    }
                });
                delB.setOnAction(e -> {
                    FuelFillUp rec = getTableRow().getItem();
                    if (rec != null) {
                        Alert c = new Alert(Alert.AlertType.CONFIRMATION, "Delete this fuel record?", ButtonType.YES, ButtonType.NO);
                        if (c.showAndWait().filter(b -> b == ButtonType.YES).isPresent()) {
                            fuelService.deleteFillUp(rec.getId());
                            refreshHeader();
                            refreshFuel();
                            refreshCostSummary();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        fuelTable.getColumns().addAll(dateCol, odoCol, litresCol, costCol, fullCol, actionCol);
        fuelTable.getItems().setAll(fuelService.getFillUpsForVehicle(vehicle.getId()));

        root.getChildren().add(fuelTable);
        return root;
    }

    private Node createCostSummaryTab() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(16, 0, 0, 0));

        ReportService.VehicleTcoSummary tco = reportService.calculateTco(vehicle.getId());
        String currency = settingsService.getSettings().getCurrency();
        String distUnit = settingsService.getSettings().getDistanceUnit();

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
                createMiniStat("Total Cost of Ownership", currency + " " + tco.totalCostOfOwnership().setScale(2)),
                createMiniStat("Cost per " + distUnit, currency + " " + tco.costPerKm().setScale(2) + " / " + distUnit),
                createMiniStat("Cost per Month", currency + " " + tco.costPerMonth().setScale(2) + " / mo")
        );
        root.getChildren().add(cards);

        GridPane breakdown = new GridPane();
        breakdown.setHgap(20);
        breakdown.setVgap(12);
        breakdown.getStyleClass().add("card");

        int r = 0;
        addDetailRow(breakdown, r++, "Purchase Price:", currency + " " + tco.purchasePrice().setScale(2));
        addDetailRow(breakdown, r++, "Total Maintenance:", currency + " " + tco.totalMaintenance().setScale(2));
        addDetailRow(breakdown, r++, "Total Non-Maintenance Expenses:", currency + " " + tco.totalExpenses().setScale(2));
        addDetailRow(breakdown, r++, "Total Fuel Costs:", currency + " " + tco.totalFuel().setScale(2));
        addDetailRow(breakdown, r++, "Months in Ownership:", tco.monthsOwned() + " months");
        addDetailRow(breakdown, r++, "Odometer Distance Recorded:", String.format("%,d %s", (int) vehicle.getCurrentOdometer(), distUnit));

        root.getChildren().add(breakdown);
        return root;
    }

    private VBox createMiniStat(String label, String value) {
        VBox box = new VBox(4);
        box.getStyleClass().add("card");
        box.setPrefWidth(240);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        box.getChildren().addAll(l, v);
        return box;
    }
}
