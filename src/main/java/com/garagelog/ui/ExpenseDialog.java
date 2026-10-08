package com.garagelog.ui;

import com.garagelog.model.Expense;
import com.garagelog.model.ExpenseCategory;
import com.garagelog.service.ExpenseService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ExpenseDialog extends Dialog<Expense> {
    public ExpenseDialog(UUID vehicleId, Expense expenseToEdit, ExpenseService expenseService) {
        setTitle(expenseToEdit == null ? "Add Expense" : "Edit Expense");
        setHeaderText(expenseToEdit == null ? "Record a non-maintenance expense" : "Update expense details");

        ButtonType saveButtonType = new ButtonType(expenseToEdit == null ? "Add Expense" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        DatePicker datePicker = new DatePicker(expenseToEdit != null ? expenseToEdit.getDate() : LocalDate.now());

        ComboBox<ExpenseCategory> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(ExpenseCategory.values());
        categoryCombo.setValue(expenseToEdit != null && expenseToEdit.getCategory() != null ? expenseToEdit.getCategory() : ExpenseCategory.PARKING);

        TextField amountField = new TextField(expenseToEdit != null && expenseToEdit.getAmount() != null ? expenseToEdit.getAmount().toPlainString() : "");
        amountField.setPromptText("e.g. 50.00");

        TextField descField = new TextField(expenseToEdit != null ? expenseToEdit.getDescription() : "");
        descField.setPromptText("e.g. Monthly parking pass");

        int row = 0;
        grid.add(new Label("Date: *"), 0, row);
        grid.add(datePicker, 1, row++);

        grid.add(new Label("Category: *"), 0, row);
        grid.add(categoryCombo, 1, row++);

        grid.add(new Label("Amount: *"), 0, row);
        grid.add(amountField, 1, row++);

        grid.add(new Label("Description: *"), 0, row);
        grid.add(descField, 1, row++);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        VBox content = new VBox(10, grid, errorLabel);
        getDialogPane().setContent(content);

        Button saveBtn = (Button) getDialogPane().lookupButton(saveButtonType);
        saveBtn.getStyleClass().add("btn-primary");

        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                Expense target = expenseToEdit != null ? expenseToEdit : new Expense();
                target.setVehicleId(vehicleId);
                target.setDate(datePicker.getValue());
                target.setCategory(categoryCombo.getValue());
                target.setAmount(new BigDecimal(amountField.getText().trim()));
                target.setDescription(descField.getText().trim());

                if (expenseToEdit == null) {
                    expenseService.addExpense(target);
                } else {
                    expenseService.updateExpense(target);
                }
            } catch (Exception e) {
                event.consume();
                errorLabel.setText(e.getMessage());
                errorLabel.setVisible(true);
            }
        });

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return expenseToEdit;
            }
            return null;
        });
    }
}
