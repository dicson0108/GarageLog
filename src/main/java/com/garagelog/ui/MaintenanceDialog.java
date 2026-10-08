package com.garagelog.ui;

import com.garagelog.model.MaintenanceRecord;
import com.garagelog.service.MaintenanceService;
import com.garagelog.util.DateUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class MaintenanceDialog extends Dialog<MaintenanceRecord> {
    public MaintenanceDialog(UUID vehicleId, MaintenanceRecord recordToEdit, MaintenanceService maintenanceService) {
        setTitle(recordToEdit == null ? "Add Maintenance Record" : "Edit Maintenance Record");
        setHeaderText(recordToEdit == null ? "Record a new service or repair" : "Update maintenance details");

        ButtonType saveButtonType = new ButtonType(recordToEdit == null ? "Add Record" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        DatePicker datePicker = new DatePicker(recordToEdit != null ? recordToEdit.getDate() : LocalDate.now());
        datePicker.setConverter(DateUtil.getDateConverter());
        datePicker.setPromptText("DD/MM/YYYY");
        TextField odoField = new TextField(recordToEdit != null ? String.valueOf((int) recordToEdit.getOdometer()) : "");
        odoField.setPromptText("Odometer at service (km)");

        TextField workDoneField = new TextField(recordToEdit != null ? recordToEdit.getWorkDone() : "");
        workDoneField.setPromptText("e.g. Oil change and brake inspection");

        TextField costField = new TextField(recordToEdit != null && recordToEdit.getCost() != null ? recordToEdit.getCost().toPlainString() : "");
        costField.setPromptText("e.g. 250.00");

        TextField workshopField = new TextField(recordToEdit != null && recordToEdit.getWorkshop() != null ? recordToEdit.getWorkshop() : "");
        workshopField.setPromptText("e.g. Apex Autocare");

        DatePicker nextDueDatePicker = new DatePicker(recordToEdit != null ? recordToEdit.getNextServiceDueDate() : null);
        nextDueDatePicker.setConverter(DateUtil.getDateConverter());
        nextDueDatePicker.setPromptText("DD/MM/YYYY");
        TextField nextDueOdoField = new TextField(recordToEdit != null && recordToEdit.getNextServiceDueOdometer() != null ? String.valueOf(recordToEdit.getNextServiceDueOdometer().intValue()) : "");
        nextDueOdoField.setPromptText("e.g. 60000");

        int row = 0;
        grid.add(new Label("Service Date: *"), 0, row);
        grid.add(datePicker, 1, row++);

        grid.add(new Label("Odometer (km): *"), 0, row);
        grid.add(odoField, 1, row++);

        grid.add(new Label("Work Done: *"), 0, row);
        grid.add(workDoneField, 1, row++);

        grid.add(new Label("Cost: *"), 0, row);
        grid.add(costField, 1, row++);

        grid.add(new Label("Workshop:"), 0, row);
        grid.add(workshopField, 1, row++);

        grid.add(new Label("Next Service Due Date:"), 0, row);
        grid.add(nextDueDatePicker, 1, row++);

        grid.add(new Label("Next Service Due Odometer (km):"), 0, row);
        grid.add(nextDueOdoField, 1, row++);

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

                // Odometer warning check per PRD
                if (recordToEdit == null && maintenanceService.isOdometerLowerThanPrevious(vehicleId, odo)) {
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

                MaintenanceRecord target = recordToEdit != null ? recordToEdit : new MaintenanceRecord();
                target.setVehicleId(vehicleId);
                target.setDate(datePicker.getValue());
                target.setOdometer(odo);
                target.setWorkDone(workDoneField.getText().trim());
                target.setCost(new BigDecimal(costField.getText().trim()));
                target.setWorkshop(workshopField.getText().trim());
                target.setNextServiceDueDate(nextDueDatePicker.getValue());

                if (!nextDueOdoField.getText().isBlank()) {
                    target.setNextServiceDueOdometer(Double.parseDouble(nextDueOdoField.getText().trim()));
                } else {
                    target.setNextServiceDueOdometer(null);
                }

                if (recordToEdit == null) {
                    maintenanceService.addRecord(target);
                } else {
                    maintenanceService.updateRecord(target);
                }
            } catch (Exception e) {
                event.consume();
                errorLabel.setText(e.getMessage());
                errorLabel.setVisible(true);
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return recordToEdit;
            }
            return null;
        });
    }
}
