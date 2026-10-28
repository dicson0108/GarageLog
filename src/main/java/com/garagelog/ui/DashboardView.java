package com.garagelog.ui;

import com.garagelog.model.*;
import com.garagelog.service.*;
import com.garagelog.util.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DashboardView extends ScrollPane {
    private final VehicleService vehicleService;
    private final MaintenanceService maintenanceService;
    private final ExpenseService expenseService;
    private final ReportService reportService;
    private final FuelService fuelService;
    private final ReminderService reminderService;
    private final SettingsService settingsService;
    private final NavigationListener navListener;

    public DashboardView(VehicleService vehicleService, MaintenanceService maintenanceService,
                         ExpenseService expenseService, FuelService fuelService, ReportService reportService,
                         ReminderService reminderService, SettingsService settingsService,
                         NavigationListener navListener) {
        this.vehicleService = vehicleService;
        this.maintenanceService = maintenanceService;
        this.expenseService = expenseService;
        this.reportService = reportService;
        this.fuelService = fuelService;
        this.reminderService = reminderService;
        this.settingsService = settingsService;
        this.navListener = navListener;

        setFitToWidth(true);
        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        refreshView();
    }

    public void refreshView() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 32, 32, 32));
        root.getStyleClass().add("main-content");

        Settings settings = settingsService.getSettings();
        String currency = settings.getCurrency();

        // 1. Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);

        VBox titleBox = new VBox(4);
        Label title = new Label("Your garage, in view.");
        title.getStyleClass().add("page-title");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
        Label subtitle = new Label(LocalDate.now().format(dtf) + " · Personal garage");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addVehicleBtn = new Button("+ Add Vehicle");
        addVehicleBtn.getStyleClass().add("btn-primary");
        addVehicleBtn.setOnAction(e -> {
            VehicleDialog dlg = new VehicleDialog(null, vehicleService);
            dlg.showAndWait().ifPresent(v -> navListener.refreshAll());
        });

        header.getChildren().addAll(titleBox, spacer, addVehicleBtn);
        root.getChildren().add(header);

        // 2. Metric Cards
        List<Vehicle> activeVehicles = vehicleService.getAllVehicles(false);
        List<Vehicle> allVehicles = vehicleService.getAllVehicles(true);
        int soldCount = (int) allVehicles.stream().filter(v -> v.getStatus() == VehicleStatus.SOLD).count();

        // Monthly spending calculation
        YearMonth thisMonth = YearMonth.now();
        List<ReportService.MonthlyCategorySpending> monthlySpending = reportService.getMonthlySpendingReport(null, 3);
        BigDecimal monthSpending = monthlySpending.getLast().total();

        List<ReminderService.ReminderItem> reminders = reminderService.getActiveReminders();
        long overdueCount = reminders.stream().filter(r -> r.severity() == ReminderService.ReminderSeverity.OVERDUE).count();
        long upcomingCount = reminders.stream().filter(r -> r.severity() == ReminderService.ReminderSeverity.UPCOMING).count();

        HBox metricsRow = new HBox(16);
        metricsRow.setFillHeight(true);

        VBox cardVehicles = createMetricCard("Active vehicles", String.valueOf(activeVehicles.size()),
                soldCount > 0 ? soldCount + " sold vehicle in your archive" : "All vehicles active");

        String budgetSubtext = "Within monthly budget";
        if (settings.getMonthlyBudget() != null && settings.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
            if (monthSpending.compareTo(settings.getMonthlyBudget()) > 0) {
                BigDecimal over = monthSpending.subtract(settings.getMonthlyBudget());
                budgetSubtext = currency + " " + over.setScale(2) + " over " + currency + " " + settings.getMonthlyBudget() + " budget";
            } else {
                BigDecimal remaining = settings.getMonthlyBudget().subtract(monthSpending);
                budgetSubtext = currency + " " + remaining.setScale(2) + " remaining of " + currency + " " + settings.getMonthlyBudget();
            }
        }
        VBox cardSpending = createMetricCard("This month's spending", currency + " " + monthSpending.setScale(2), budgetSubtext);

        VBox cardReminders = createMetricCard("Reminders to review", String.valueOf(reminders.size()),
                overdueCount + " overdue · " + upcomingCount + " upcoming");

        HBox.setHgrow(cardVehicles, Priority.ALWAYS);
        HBox.setHgrow(cardSpending, Priority.ALWAYS);
        HBox.setHgrow(cardReminders, Priority.ALWAYS);
        metricsRow.getChildren().addAll(cardVehicles, cardSpending, cardReminders);
        root.getChildren().add(metricsRow);

        // Budget Exceeded Banner
        if (settings.getMonthlyBudget() != null && settings.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0
                && monthSpending.compareTo(settings.getMonthlyBudget()) > 0) {
            BigDecimal over = monthSpending.subtract(settings.getMonthlyBudget());
            int pct = monthSpending.multiply(BigDecimal.valueOf(100)).divide(settings.getMonthlyBudget(), 0, java.math.RoundingMode.HALF_UP).intValue();

            VBox banner = new VBox(4);
            banner.getStyleClass().add("alert-banner-warning");
            Label bTitle = new Label("Monthly budget exceeded by " + currency + " " + over.setScale(2));
            bTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #9A3412;");
            Label bDesc = new Label(thisMonth.getMonth().name() + " spending is " + pct + "% of your " + currency + " " + settings.getMonthlyBudget() + " budget. Review expenses or adjust your budget in Settings.");
            bDesc.setStyle("-fx-text-fill: #C2410C; -fx-font-size: 12px;");
            banner.getChildren().addAll(bTitle, bDesc);
            root.getChildren().add(banner);
        }

        // 3. Middle Columns: Left = Reminders, Right = Quick Entry & Spending Chart
        HBox middleSection = new HBox(20);
        middleSection.setFillHeight(true);

        // Left Column: Needs Attention
        VBox remindersBox = new VBox(12);
        remindersBox.getStyleClass().add("card");
        HBox.setHgrow(remindersBox, Priority.ALWAYS);

        Label remindersHeader = new Label("Needs your attention (" + reminders.size() + ")");
        remindersHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label remindersSub;
        if (reminders.size() > 3) {
            remindersSub = new Label(reminders.size() + " items needing action · Showing 3 at once (scroll to view all)");
        } else {
            remindersSub = new Label("Reminders use your " + settings.getReminderLeadDays() + "-day / " + settings.getReminderLeadKm() + "-" + settings.getDistanceUnit() + " lead time.");
        }
        remindersSub.getStyleClass().add("card-metric-subtext");
        remindersBox.getChildren().addAll(remindersHeader, remindersSub);

        if (reminders.isEmpty()) {
            Label allClear = new Label("✓ No upcoming reminders. All maintenance and renewals are up to date.");
            allClear.setStyle("-fx-text-fill: #059669; -fx-padding: 20 0;");
            remindersBox.getChildren().add(allClear);
        } else {
            VBox reminderList = new VBox(10);
            reminderList.setPadding(new Insets(2, 6, 2, 2));
            for (ReminderService.ReminderItem item : reminders) {
                reminderList.getChildren().add(createReminderCard(item));
            }

            ScrollPane reminderScrollPane = new ScrollPane(reminderList);
            reminderScrollPane.setFitToWidth(true);
            reminderScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            reminderScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            reminderScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
            double targetHeight = Math.min(348, Math.max(108, reminders.size() * 118));
            reminderScrollPane.setPrefHeight(targetHeight);
            reminderScrollPane.setMaxHeight(348);

            remindersBox.getChildren().add(reminderScrollPane);
        }

        // Right Column: Quick entry & Recent spending
        VBox rightColumn = new VBox(16);
        rightColumn.setPrefWidth(380);

        // Quick Entry Card
        VBox quickEntryCard = new VBox(10);
        quickEntryCard.getStyleClass().add("card");
        Label qeTitle = new Label("Quick entry");
        qeTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label qeSub = new Label("Keep your ledger up to date.");
        qeSub.getStyleClass().add("card-metric-subtext");

        VBox qeButtons = new VBox(8);
        Button btnAddVeh = createQuickButton("🚗  Add Vehicle");
        btnAddVeh.setOnAction(e -> {
            VehicleDialog dlg = new VehicleDialog(null, vehicleService);
            dlg.showAndWait().ifPresent(v -> navListener.refreshAll());
        });

        Button btnAddFuel = createQuickButton("⛽  Add Fuel");
        btnAddFuel.setOnAction(e -> {
            promptSelectVehicleAndOpenFuel();
        });

        Button btnAddExpense = createQuickButton("💵  Add Expense");
        btnAddExpense.setOnAction(e -> {
            promptSelectVehicleAndOpenExpense();
        });

        qeButtons.getChildren().addAll(btnAddVeh, btnAddFuel, btnAddExpense);
        quickEntryCard.getChildren().addAll(qeTitle, qeSub, qeButtons);

        // Recent Spending Mini Chart
        VBox chartCard = new VBox(8);
        chartCard.getStyleClass().add("card");
        Label chartTitle = new Label("Recent spending");
        chartTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label chartSub = new Label("All vehicles · Past 3 months");
        chartSub.getStyleClass().add("card-metric-subtext");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, currency + " ", ""));
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setPrefHeight(180);
        barChart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (ReportService.MonthlyCategorySpending month : monthlySpending) {
            series.getData().add(new XYChart.Data<>(month.yearMonth().getMonth().name().substring(0, 3), month.total().doubleValue()));
        }
        barChart.getData().add(series);

        chartCard.getChildren().addAll(chartTitle, chartSub, barChart);

        rightColumn.getChildren().addAll(quickEntryCard, chartCard);
        middleSection.getChildren().addAll(remindersBox, rightColumn);
        root.getChildren().add(middleSection);

        // 4. Latest Activity Card
        VBox activityCard = new VBox(12);
        activityCard.getStyleClass().add("card");

        HBox actHeader = new HBox();
        actHeader.setAlignment(Pos.CENTER_LEFT);
        Label actTitle = new Label("Latest activity");
        actTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, Priority.ALWAYS);
        Hyperlink viewReportsLink = new Hyperlink("View reports →");
        viewReportsLink.setStyle("-fx-text-fill: #FF8000; -fx-font-weight: bold;");
        viewReportsLink.setOnAction(e -> navListener.navigateToReports());
        actHeader.getChildren().addAll(actTitle, actSpacer, viewReportsLink);

        TableView<ActivityRow> activityTable = createActivityTable();
        activityCard.getChildren().addAll(actHeader, activityTable);
        root.getChildren().add(activityCard);

        setContent(root);
    }

    private VBox createMetricCard(String title, String value, String subtext) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        Label lTitle = new Label(title);
        lTitle.getStyleClass().add("card-title");
        Label lVal = new Label(value);
        lVal.getStyleClass().add("card-metric-value");
        Label lSub = new Label(subtext);
        lSub.getStyleClass().add("card-metric-subtext");
        card.getChildren().addAll(lTitle, lVal, lSub);
        return card;
    }

    private Button createQuickButton(String text) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.getStyleClass().add("btn-secondary");
        b.setAlignment(Pos.BASELINE_LEFT);
        return b;
    }

    private Node createReminderCard(ReminderService.ReminderItem item) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E2E8F0; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10 12;");
        box.setMinHeight(108);
        box.setPrefHeight(108);
        box.setMaxHeight(108);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(item.title());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label badge = new Label(item.severity() == ReminderService.ReminderSeverity.OVERDUE ? "Overdue" : "Upcoming");
        badge.getStyleClass().add(item.severity() == ReminderService.ReminderSeverity.OVERDUE ? "badge-overdue" : "badge-upcoming");
        top.getChildren().addAll(title, sp, badge);

        Hyperlink vehicleLink = new Hyperlink(item.vehiclePlate() + " · " + item.vehicleName() + " ↗");
        vehicleLink.setStyle("-fx-text-fill: #FF8000; -fx-font-weight: bold; -fx-padding: 0;");
        vehicleLink.setOnAction(e -> {
            vehicleService.findById(item.vehicleId()).ifPresent(navListener::navigateToVehicleDetail);
        });

        Label desc = new Label(item.description());
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-padding: 4 0 0 0;");

        Button snoozeBtn = new Button("⏱ Snooze 7 days");
        snoozeBtn.getStyleClass().add("btn-secondary");
        snoozeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
        snoozeBtn.setOnAction(e -> {
            reminderService.snoozeReminder(item.id(), 7);
            refreshView();
        });

        Button dismissBtn = new Button("Dismiss");
        dismissBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748B; -fx-font-size: 11px; -fx-cursor: hand;");
        dismissBtn.setOnAction(e -> {
            reminderService.dismissReminder(item.id());
            refreshView();
        });

        actions.getChildren().addAll(snoozeBtn, dismissBtn);

        box.getChildren().addAll(top, vehicleLink, desc, actions);
        return box;
    }

    public record ActivityRow(LocalDate rawDate, String date, String vehiclePlate, String record, String amount) {}

    private TableView<ActivityRow> createActivityTable() {
        TableView<ActivityRow> table = new TableView<>();
        table.setPlaceholder(new Label("No recent activities."));
        table.setPrefHeight(180);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<ActivityRow, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date()));
        dateCol.setPrefWidth(120);

        TableColumn<ActivityRow, String> vehCol = new TableColumn<>("Vehicle");
        vehCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().vehiclePlate()));
        vehCol.setPrefWidth(140);

        TableColumn<ActivityRow, String> recCol = new TableColumn<>("Record");
        recCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().record()));
        recCol.setPrefWidth(300);

        TableColumn<ActivityRow, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().amount()));
        amtCol.setPrefWidth(120);

        table.getColumns().addAll(dateCol, vehCol, recCol, amtCol);

        String currency = settingsService.getSettings().getCurrency();
        List<ActivityRow> rows = new ArrayList<>();

        for (MaintenanceRecord m : vehicleService.getAppData().getMaintenanceRecords()) {
            String plate = vehicleService.findById(m.getVehicleId()).map(Vehicle::getPlateNumber).orElse("Unknown");
            String cost = m.getCost() != null ? currency + " " + m.getCost().setScale(2) : "-";
            rows.add(new ActivityRow(m.getDate(), DateUtil.format(m.getDate()), plate, "Maintenance · " + m.getWorkDone(), cost));
        }
        for (Expense exp : vehicleService.getAppData().getExpenses()) {
            String plate = vehicleService.findById(exp.getVehicleId()).map(Vehicle::getPlateNumber).orElse("Unknown");
            String cost = exp.getAmount() != null ? currency + " " + exp.getAmount().setScale(2) : "-";
            rows.add(new ActivityRow(exp.getDate(), DateUtil.format(exp.getDate()), plate, "Expense · " + exp.getDescription(), cost));
        }
        for (FuelFillUp f : vehicleService.getAppData().getFuelFillUps()) {
            String plate = vehicleService.findById(f.getVehicleId()).map(Vehicle::getPlateNumber).orElse("Unknown");
            String cost = f.getTotalCost() != null ? currency + " " + f.getTotalCost().setScale(2) : "-";
            rows.add(new ActivityRow(f.getDate(), DateUtil.format(f.getDate()), plate, "Fuel · " + (f.isFullTank() ? "Full tank" : "Partial fill"), cost));
        }

        rows.sort(Comparator.comparing(ActivityRow::rawDate, Comparator.nullsLast(Comparator.reverseOrder())));
        table.getItems().addAll(rows.stream().limit(8).toList());

        return table;
    }

    private void promptSelectVehicleAndOpenFuel() {
        List<Vehicle> list = vehicleService.getAllVehicles(false);
        if (list.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Please add a vehicle first.").showAndWait();
            return;
        }
        ChoiceDialog<Vehicle> dlg = new ChoiceDialog<>(list.get(0), list);
        dlg.setTitle("Select Vehicle");
        dlg.setHeaderText("Choose a vehicle to log fuel for:");
        dlg.showAndWait().ifPresent(v -> {
            FuelDialog fuelDlg = new FuelDialog(v.getId(), null, fuelService, maintenanceService);
            fuelDlg.showAndWait().ifPresent(f -> navListener.refreshAll());
        });
    }

    private void promptSelectVehicleAndOpenExpense() {
        List<Vehicle> list = vehicleService.getAllVehicles(false);
        if (list.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Please add a vehicle first.").showAndWait();
            return;
        }
        ChoiceDialog<Vehicle> dlg = new ChoiceDialog<>(list.get(0), list);
        dlg.setTitle("Select Vehicle");
        dlg.setHeaderText("Choose a vehicle to log an expense for:");
        dlg.showAndWait().ifPresent(v -> {
            ExpenseDialog expDlg = new ExpenseDialog(v.getId(), null, expenseService);
            expDlg.showAndWait().ifPresent(e -> navListener.refreshAll());
        });
    }
}
