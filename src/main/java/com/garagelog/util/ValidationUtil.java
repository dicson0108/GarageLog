package com.garagelog.util;

import com.garagelog.exception.ValidationException;
import com.garagelog.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static void validateVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new ValidationException("Vehicle cannot be null.");
        }
        if (vehicle.getPlateNumber() == null || vehicle.getPlateNumber().trim().isEmpty()) {
            throw new ValidationException("License plate is required.");
        }
        if (vehicle.getBrand() == null || vehicle.getBrand().trim().isEmpty()) {
            throw new ValidationException("Vehicle brand is required.");
        }
        if (vehicle.getModel() == null || vehicle.getModel().trim().isEmpty()) {
            throw new ValidationException("Vehicle model is required.");
        }
        int maxYear = LocalDate.now().getYear() + 1;
        if (vehicle.getYear() < 1900 || vehicle.getYear() > maxYear) {
            throw new ValidationException("Year must be between 1900 and " + maxYear + ".");
        }
        if (vehicle.getType() == null) {
            throw new ValidationException("Vehicle type is required.");
        }
        if (vehicle.getStatus() == null) {
            throw new ValidationException("Vehicle status is required.");
        }
        if (vehicle.getCurrentOdometer() < 0) {
            throw new ValidationException("Odometer cannot be negative.");
        }
        if (vehicle.getPurchasePrice() != null && vehicle.getPurchasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Purchase price cannot be negative.");
        }
    }

    public static void validateMaintenance(MaintenanceRecord record) {
        if (record == null) {
            throw new ValidationException("Maintenance record cannot be null.");
        }
        if (record.getVehicleId() == null) {
            throw new ValidationException("Linked vehicle is required.");
        }
        if (record.getDate() == null) {
            throw new ValidationException("Maintenance date is required.");
        }
        if (record.getWorkDone() == null || record.getWorkDone().trim().isEmpty()) {
            throw new ValidationException("Work done description is required.");
        }
        if (record.getCost() == null || record.getCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Maintenance cost must be zero or positive.");
        }
        if (record.getOdometer() < 0) {
            throw new ValidationException("Odometer reading cannot be negative.");
        }
        if (record.getNextServiceDueDate() != null && record.getDate() != null) {
            if (record.getNextServiceDueDate().isBefore(record.getDate())) {
                throw new ValidationException("Next service due date cannot be earlier than service date.");
            }
        }
        if (record.getNextServiceDueOdometer() != null && record.getNextServiceDueOdometer() < record.getOdometer()) {
            throw new ValidationException("Next service due odometer cannot be lower than current service odometer.");
        }
    }

    public static void validateExpense(Expense expense) {
        if (expense == null) {
            throw new ValidationException("Expense record cannot be null.");
        }
        if (expense.getVehicleId() == null) {
            throw new ValidationException("Linked vehicle is required.");
        }
        if (expense.getDate() == null) {
            throw new ValidationException("Expense date is required.");
        }
        if (expense.getCategory() == null) {
            throw new ValidationException("Expense category is required.");
        }
        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Expense amount must be greater than zero.");
        }
        if (expense.getDescription() == null || expense.getDescription().trim().isEmpty()) {
            throw new ValidationException("Expense description is required.");
        }
    }

    public static void validateFuel(FuelFillUp fuel) {
        if (fuel == null) {
            throw new ValidationException("Fuel fill-up record cannot be null.");
        }
        if (fuel.getVehicleId() == null) {
            throw new ValidationException("Linked vehicle is required.");
        }
        if (fuel.getDate() == null) {
            throw new ValidationException("Fill-up date is required.");
        }
        if (fuel.getOdometer() < 0) {
            throw new ValidationException("Odometer reading cannot be negative.");
        }
        if (fuel.getLitres() <= 0) {
            throw new ValidationException("Litres must be greater than zero.");
        }
        if (fuel.getTotalCost() == null || fuel.getTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Total cost must be greater than zero.");
        }
    }
}
