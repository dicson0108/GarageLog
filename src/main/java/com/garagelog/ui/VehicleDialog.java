package com.garagelog.ui;

import com.garagelog.model.*;
import com.garagelog.service.VehicleService;
import com.garagelog.util.DateUtil;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VehicleDialog extends Dialog<Vehicle> {
    public VehicleDialog(Vehicle existingVehicle, VehicleService vehicleService) {
        setTitle(existingVehicle == null ? "Add Vehicle" : "Edit Vehicle — " + existingVehicle.getPlateNumber());
        setHeaderText(existingVehicle == null ? "Enter vehicle details" : "Update vehicle details");

        ButtonType saveButtonType = new ButtonType(existingVehicle == null ? "Add Vehicle" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        TextField plateField = new TextField(existingVehicle != null ? existingVehicle.getPlateNumber() : "");
        plateField.setPromptText("e.g. BN21 LXR");

        TextField nicknameField = new TextField(existingVehicle != null && existingVehicle.getNickname() != null ? existingVehicle.getNickname() : "");
        nicknameField.setPromptText("e.g. Daily driver");

        TextField brandField = new TextField(existingVehicle != null ? existingVehicle.getBrand() : "");
        brandField.setPromptText("e.g. Toyota");

        TextField modelField = new TextField(existingVehicle != null ? existingVehicle.getModel() : "");
        modelField.setPromptText("e.g. Corolla");

        Spinner<Integer> yearSpinner = new Spinner<>(1900, LocalDate.now().getYear() + 1,
                existingVehicle != null && existingVehicle.getYear() > 0 ? existingVehicle.getYear() : LocalDate.now().getYear());
        yearSpinner.setEditable(true);

        ComboBox<VehicleType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(VehicleType.values());
        typeCombo.setValue(existingVehicle != null && existingVehicle.getType() != null ? existingVehicle.getType() : VehicleType.CAR);

        ComboBox<FuelType> fuelCombo = new ComboBox<>();
        fuelCombo.getItems().addAll(FuelType.values());
        fuelCombo.setValue(existingVehicle != null && existingVehicle.getFuelType() != null ? existingVehicle.getFuelType() : FuelType.PETROL);

        ComboBox<VehicleStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(VehicleStatus.values());
        statusCombo.setValue(existingVehicle != null && existingVehicle.getStatus() != null ? existingVehicle.getStatus() : VehicleStatus.ACTIVE);

        TextField colourField = new TextField(existingVehicle != null && existingVehicle.getColour() != null ? existingVehicle.getColour() : "");
        colourField.setPromptText("e.g. Silver");

        TextField vinField = new TextField(existingVehicle != null && existingVehicle.getVin() != null ? existingVehicle.getVin() : "");
        vinField.setPromptText("Optional VIN");

        TextField odoField = new TextField(existingVehicle != null ? String.valueOf((int) existingVehicle.getCurrentOdometer()) : "0");
        odoField.setPromptText("Current odometer (km)");

        DatePicker purchaseDatePicker = new DatePicker(existingVehicle != null ? existingVehicle.getPurchaseDate() : null);
        purchaseDatePicker.setConverter(DateUtil.getDateConverter());
        purchaseDatePicker.setPromptText("DD/MM/YYYY");
        TextField priceField = new TextField(existingVehicle != null && existingVehicle.getPurchasePrice() != null ? existingVehicle.getPurchasePrice().toPlainString() : "");
        priceField.setPromptText("Purchase price");

        DatePicker insuranceDatePicker = new DatePicker(existingVehicle != null ? existingVehicle.getInsuranceExpiryDate() : null);
        insuranceDatePicker.setConverter(DateUtil.getDateConverter());
        insuranceDatePicker.setPromptText("DD/MM/YYYY");
        DatePicker roadTaxDatePicker = new DatePicker(existingVehicle != null ? existingVehicle.getRoadTaxExpiryDate() : null);
        roadTaxDatePicker.setConverter(DateUtil.getDateConverter());
        roadTaxDatePicker.setPromptText("DD/MM/YYYY");

        TextArea notesArea = new TextArea(existingVehicle != null && existingVehicle.getNotes() != null ? existingVehicle.getNotes() : "");
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Optional notes");

        // Layout rows
        int row = 0;
        grid.add(new Label("License Plate: *"), 0, row);
        grid.add(plateField, 1, row++);

        grid.add(new Label("Nickname:"), 0, row);
        grid.add(nicknameField, 1, row++);

        grid.add(new Label("Brand: *"), 0, row);
        grid.add(brandField, 1, row++);

        grid.add(new Label("Model: *"), 0, row);
        grid.add(modelField, 1, row++);

        grid.add(new Label("Year: *"), 0, row);
        grid.add(yearSpinner, 1, row++);

        grid.add(new Label("Vehicle Type: *"), 0, row);
        grid.add(typeCombo, 1, row++);

        grid.add(new Label("Fuel Type:"), 0, row);
        grid.add(fuelCombo, 1, row++);

        grid.add(new Label("Status: *"), 0, row);
        grid.add(statusCombo, 1, row++);

        grid.add(new Label("Colour:"), 0, row);
        grid.add(colourField, 1, row++);

        grid.add(new Label("VIN:"), 0, row);
        grid.add(vinField, 1, row++);

        grid.add(new Label("Current Odometer (km):"), 0, row);
        grid.add(odoField, 1, row++);

        grid.add(new Label("Purchase Date:"), 0, row);
        grid.add(purchaseDatePicker, 1, row++);

        grid.add(new Label("Purchase Price:"), 0, row);
        grid.add(priceField, 1, row++);

        grid.add(new Label("Insurance Expiry:"), 0, row);
        grid.add(insuranceDatePicker, 1, row++);

        grid.add(new Label("Road Tax Expiry:"), 0, row);
        grid.add(roadTaxDatePicker, 1, row++);

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        VBox content = new VBox(10, grid, errorLabel);
        getDialogPane().setContent(content);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveButtonType);
        saveBtn.getStyleClass().add("btn-primary");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                Vehicle target = existingVehicle != null ? existingVehicle : new Vehicle();
                target.setPlateNumber(plateField.getText().trim());
                target.setNickname(nicknameField.getText().trim());
                target.setBrand(brandField.getText().trim());
                target.setModel(modelField.getText().trim());
                target.setYear(yearSpinner.getValue());
                target.setType(typeCombo.getValue());
                target.setFuelType(fuelCombo.getValue());
                target.setStatus(statusCombo.getValue());
                target.setColour(colourField.getText().trim());
                target.setVin(vinField.getText().trim());

                double odo = 0;
                if (!odoField.getText().isBlank()) {
                    odo = Double.parseDouble(odoField.getText().trim());
                }
                target.setCurrentOdometer(odo);

                target.setPurchaseDate(purchaseDatePicker.getValue());
                if (!priceField.getText().isBlank()) {
                    target.setPurchasePrice(new BigDecimal(priceField.getText().trim()));
                } else {
                    target.setPurchasePrice(null);
                }

                target.setInsuranceExpiryDate(insuranceDatePicker.getValue());
                target.setRoadTaxExpiryDate(roadTaxDatePicker.getValue());
                target.setNotes(notesArea.getText().trim());

                if (existingVehicle == null) {
                    vehicleService.addVehicle(target);
                } else {
                    vehicleService.updateVehicle(target);
                }
            } catch (Exception e) {
                event.consume();
                errorLabel.setText(e.getMessage());
                errorLabel.setVisible(true);
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return existingVehicle;
            }
            return null;
        });
    }
}
