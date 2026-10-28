package com.garagelog.ui;

import com.garagelog.model.Settings;
import com.garagelog.service.SettingsService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;

public class SettingsView extends ScrollPane {
    private final SettingsService settingsService;
    private final NavigationListener navListener;

    public SettingsView(SettingsService settingsService, NavigationListener navListener) {
        this.settingsService = settingsService;
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
        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Preferences for currency, units, reminder intervals, and monthly budget.");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);
        root.getChildren().add(titleBox);

        Settings current = settingsService.getSettings();

        // Form Card
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        TextField currencyField = new TextField(current.getCurrency());
        currencyField.setPromptText("e.g. MYR, GBP, USD");

        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll("km", "miles");
        unitCombo.setValue(current.getDistanceUnit() != null ? current.getDistanceUnit() : "km");

        Spinner<Integer> daysSpinner = new Spinner<>(1, 365, current.getReminderLeadDays());
        daysSpinner.setEditable(true);

        Spinner<Integer> kmSpinner = new Spinner<>(100, 10000, current.getReminderLeadKm(), 100);
        kmSpinner.setEditable(true);

        TextField budgetField = new TextField(current.getMonthlyBudget() != null ? current.getMonthlyBudget().toPlainString() : "");
        budgetField.setPromptText("Optional monthly budget limit");

        int r = 0;
        grid.add(new Label("Currency Symbol:"), 0, r);
        grid.add(currencyField, 1, r++);

        grid.add(new Label("Distance Unit:"), 0, r);
        grid.add(unitCombo, 1, r++);

        grid.add(new Label("Reminder Lead Time (days):"), 0, r);
        grid.add(daysSpinner, 1, r++);

        grid.add(new Label("Reminder Lead Distance:"), 0, r);
        grid.add(kmSpinner, 1, r++);

        grid.add(new Label("Monthly Spending Budget:"), 0, r);
        grid.add(budgetField, 1, r++);

        Button saveBtn = new Button("Save Settings");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            try {
                current.setCurrency(currencyField.getText().trim());
                current.setDistanceUnit(unitCombo.getValue());
                current.setReminderLeadDays(daysSpinner.getValue());
                current.setReminderLeadKm(kmSpinner.getValue());

                if (!budgetField.getText().isBlank()) {
                    current.setMonthlyBudget(new BigDecimal(budgetField.getText().trim()));
                } else {
                    current.setMonthlyBudget(null);
                }

                settingsService.updateSettings(current);
                navListener.refreshAll();
                new Alert(Alert.AlertType.INFORMATION, "Settings saved successfully.").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Invalid input: " + ex.getMessage()).showAndWait();
            }
        });

        card.getChildren().addAll(grid, saveBtn);
        root.getChildren().add(card);

        setContent(root);
    }
}
