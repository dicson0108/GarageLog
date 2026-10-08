package com.garagelog.ui;

import com.garagelog.model.FuelFillUp;
import com.garagelog.service.FuelService;
import com.garagelog.service.MaintenanceService;
import com.garagelog.util.DateUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class FuelDialog extends Dialog<FuelFillUp> {
    public FuelDialog(UUID vehicleId, FuelFillUp fillUpToEdit, FuelService fuelService, MaintenanceService maintenanceService) {
        setTitle(fillUpToEdit == null ? "Add Fuel Fill-up" : "Edit Fuel Fill-up");
        setHeaderText(fillUpToEdit == null ? "Record fuel fill-up" : "Update fill-up details");

        ButtonType saveButtonType = new ButtonType(fillUpToEdit == null ? "Add Fill-up" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        DatePicker datePicker = new DatePicker(fillUpToEdit != null ? fillUpToEdit.getDate() : LocalDate.now());
        datePicker.setConverter(DateUtil.getDateConverter());
        datePicker.setPromptText("DD/MM/YYYY");

        TextField odoField = new TextField(fillUpToEdit != null ? String.valueOf((int) fillUpToEdit.getOdometer()) : "");
        odoField.setPromptText("Odometer (km)");

        TextField litresField = new TextField(fillUpToEdit != null ? String.valueOf(fillUpToEdit.getLitres()) : "");
        litresField.setPromptText("e.g. 45.0");

        TextField priceField = new TextField(fillUpToEdit != null && fillUpToEdit.getPricePerLitre() != null ? fillUpToEdit.getPricePerLitre().toPlainString() : "");
        priceField.setPromptText("Price per litre (optional)");

        TextField totalCostField = new TextField(fillUpToEdit != null && fillUpToEdit.getTotalCost() != null ? fillUpToEdit.getTotalCost().toPlainString() : "");
        totalCostField.setPromptText("Total cost");

        CheckBox fullTankCheck = new CheckBox("Full tank (used for consumption calculation)");
        fullTankCheck.setSelected(fillUpToEdit == null || fillUpToEdit.isFullTank());

        // Auto-calculate total cost if litres and price are entered
        Runnable autoCalc = () -> {
            try {
                if (!litresField.getText().isBlank() && !priceField.getText().isBlank()) {
                    BigDecimal litres = new BigDecimal(litresField.getText().trim());
                    BigDecimal price = new BigDecimal(priceField.getText().trim());
                    totalCostField.setText(litres.multiply(price).setScale(2, RoundingMode.HALF_UP).toPlainString());
                }
            } catch (Exception ignored) {}
        };
        litresField.textProperty().addListener((obs, oldV, newV) -> autoCalc.run());
        priceField.textProperty().addListener((obs, oldV, newV) -> autoCalc.run());

        int row = 0;
        grid.add(new Label("Date: *"), 0, row);
        grid.add(datePicker, 1, row++);

        grid.add(new Label("Odometer (km): *"), 0, row);
        grid.add(odoField, 1, row++);

        grid.add(new Label("Litres: *"), 0, row);
        grid.add(litresField, 1, row++);

        grid.add(new Label("Price per Litre:"), 0, row);
        grid.add(priceField, 1, row++);

        grid.add(new Label("Total Cost: *"), 0, row);
        grid.add(totalCostField, 1, row++);

        grid.add(new Label("Full Tank:"), 0, row);
        grid.add(fullTankCheck, 1, row++);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        VBox content = new VBox(10, grid, errorLabel);
        getDialogPane().setContent(content);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveButtonType);
        saveBtn.getStyleClass().add("btn-primary");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                double odo = Double.parseDouble(odoField.getText().trim());

                if (fillUpToEdit == null && maintenanceService.isOdometerLowerThanPrevious(vehicleId, odo)) {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Odometer Warning");
                    confirm.setHeaderText("Odometer lower than recorded vehicle reading");
                    confirm.setContentText("The entered reading (" + (int) odo + " km) is lower than the vehicle's highest recorded reading. Do you wish to proceed anyway?");
                    Optional<ButtonType> choice = confirm.showAndWait();
                    if (choice.isEmpty() || choice.get() != ButtonType.OK) {
                        event.consume();
                        return;
                    }
                }

                FuelFillUp target = fillUpToEdit != null ? fillUpToEdit : new FuelFillUp();
                target.setVehicleId(vehicleId);
                target.setDate(datePicker.getValue());
                target.setOdometer(odo);
                target.setLitres(Double.parseDouble(litresField.getText().trim()));
                target.setTotalCost(new BigDecimal(totalCostField.getText().trim()));

                if (!priceField.getText().isBlank()) {
                    target.setPricePerLitre(new BigDecimal(priceField.getText().trim()));
                } else {
                    target.setPricePerLitre(target.getTotalCost().divide(BigDecimal.valueOf(target.getLitres()), 2, RoundingMode.HALF_UP));
                }

                target.setFullTank(fullTankCheck.isSelected());

                if (fillUpToEdit == null) {
                    fuelService.addFillUp(target);
                } else {
                    fuelService.updateFillUp(target);
                }
            } catch (Exception e) {
                event.consume();
                errorLabel.setText(e.getMessage());
                errorLabel.setVisible(true);
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return fillUpToEdit;
            }
            return null;
        });
    }
}
