package com.garagelog.ui;

import com.garagelog.model.*;
import com.garagelog.service.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;

public class ReportsView extends ScrollPane {
    private final VehicleService vehicleService;
    private final ReportService reportService;
    private final SettingsService settingsService;

    public ReportsView(VehicleService vehicleService, ReportService reportService, SettingsService settingsService) {
        this.vehicleService = vehicleService;
        this.reportService = reportService;
        this.settingsService = settingsService;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        refreshView();
    }

    public void refreshView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 32, 32, 32));
        root.getStyleClass().add("main-content");

        String currency = settingsService.getSettings().getCurrency();
        String distUnit = settingsService.getSettings().getDistanceUnit();

        // Header
        VBox titleBox = new VBox(4);
        Label title = new Label("Reports & Analytics");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Spending trends, total cost of ownership and fuel consumption.");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        root.getChildren().add(titleBox);

        // 1. Spending Trends Chart Card
        VBox chartCard = new VBox(12);
        chartCard.getStyleClass().add("card");

        Label chartTitle = new Label("Monthly Spending by Category (Past 6 Months)");
        chartTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, currency + " ", ""));

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setPrefHeight(260);
        barChart.setAnimated(false);

        List<ReportService.MonthlyCategorySpending> monthlyData = reportService.getMonthlySpendingReport(null, 6);

        // Build series for Fuel, Maintenance, Other Expenses
        XYChart.Series<String, Number> maintSeries = new XYChart.Series<>();
        maintSeries.setName("Maintenance");

        XYChart.Series<String, Number> fuelSeries = new XYChart.Series<>();
        fuelSeries.setName("Fuel");

        XYChart.Series<String, Number> expSeries = new XYChart.Series<>();
        expSeries.setName("Other Expenses");

        for (ReportService.MonthlyCategorySpending m : monthlyData) {
            String label = m.yearMonth().getMonth().name().substring(0, 3) + " " + (m.yearMonth().getYear() % 100);
            BigDecimal maint = m.spendingByCategory().getOrDefault("Maintenance", BigDecimal.ZERO);
            BigDecimal fuel = m.spendingByCategory().getOrDefault("Fuel", BigDecimal.ZERO);

            BigDecimal other = BigDecimal.ZERO;
            for (var entry : m.spendingByCategory().entrySet()) {
                if (!entry.getKey().equals("Maintenance") && !entry.getKey().equals("Fuel")) {
                    other = other.add(entry.getValue());
                }
            }

            maintSeries.getData().add(new XYChart.Data<>(label, maint.doubleValue()));
            fuelSeries.getData().add(new XYChart.Data<>(label, fuel.doubleValue()));
            expSeries.getData().add(new XYChart.Data<>(label, other.doubleValue()));
        }

        barChart.getData().addAll(maintSeries, fuelSeries, expSeries);
        chartCard.getChildren().addAll(chartTitle, barChart);
        root.getChildren().add(chartCard);

        // 2. Total Cost of Ownership Table Card
        VBox tcoCard = new VBox(12);
        tcoCard.getStyleClass().add("card");

        Label tcoTitle = new Label("Total Cost of Ownership Breakdown");
        tcoTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        TableView<ReportService.VehicleTcoSummary> tcoTable = new TableView<>();
        tcoTable.setPlaceholder(new Label("No vehicle data available for report."));
        tcoTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tcoTable.setPrefHeight(220);

        TableColumn<ReportService.VehicleTcoSummary, String> plateCol = new TableColumn<>("Vehicle");
        plateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().plateNumber() + " (" + d.getValue().vehicleDisplayName() + ")"));
        plateCol.setPrefWidth(180);

        TableColumn<ReportService.VehicleTcoSummary, String> purchaseCol = new TableColumn<>("Purchase");
        purchaseCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().purchasePrice().setScale(2)));

        TableColumn<ReportService.VehicleTcoSummary, String> maintCol = new TableColumn<>("Maintenance");
        maintCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().totalMaintenance().setScale(2)));

        TableColumn<ReportService.VehicleTcoSummary, String> expCol = new TableColumn<>("Expenses");
        expCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().totalExpenses().setScale(2)));

        TableColumn<ReportService.VehicleTcoSummary, String> fuelCol = new TableColumn<>("Fuel");
        fuelCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().totalFuel().setScale(2)));

        TableColumn<ReportService.VehicleTcoSummary, String> totalCol = new TableColumn<>("Total TCO");
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().totalCostOfOwnership().setScale(2)));
        totalCol.setStyle("-fx-font-weight: bold;");

        TableColumn<ReportService.VehicleTcoSummary, String> kmCol = new TableColumn<>("Cost / " + distUnit);
        kmCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().costPerKm().setScale(2)));

        TableColumn<ReportService.VehicleTcoSummary, String> monthCol = new TableColumn<>("Cost / Month");
        monthCol.setCellValueFactory(d -> new SimpleStringProperty(currency + " " + d.getValue().costPerMonth().setScale(2)));

        tcoTable.getColumns().addAll(plateCol, purchaseCol, maintCol, expCol, fuelCol, totalCol, kmCol, monthCol);

        List<ReportService.VehicleTcoSummary> tcoList = new ArrayList<>();
        for (Vehicle v : vehicleService.getAllVehicles(true)) {
            tcoList.add(reportService.calculateTco(v.getId()));
        }
        tcoTable.getItems().setAll(tcoList);

        tcoCard.getChildren().addAll(tcoTitle, tcoTable);
        root.getChildren().add(tcoCard);

        setContent(root);
    }
}
