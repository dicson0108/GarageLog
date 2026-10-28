package com.garagelog.service;

import com.garagelog.exception.ValidationException;
import com.garagelog.exception.VehicleNotFoundException;
import com.garagelog.model.AppData;
import com.garagelog.model.Expense;
import com.garagelog.repository.JsonGarageRepository;
import com.garagelog.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseService {
    private final JsonGarageRepository repository;
    private final VehicleService vehicleService;

    public ExpenseService(JsonGarageRepository repository, VehicleService vehicleService) {
        this.repository = repository;
        this.vehicleService = vehicleService;
    }

    private AppData getAppData() {
        return vehicleService.getAppData();
    }

    public synchronized List<Expense> getExpensesForVehicle(UUID vehicleId) {
        if (vehicleId == null) {
            return Collections.emptyList();
        }
        return getAppData().getExpenses().stream()
                .filter(e -> vehicleId.equals(e.getVehicleId()))
                .sorted(Comparator.comparing(Expense::getDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    public synchronized Optional<Expense> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return getAppData().getExpenses().stream()
                .filter(e -> id.equals(e.getId()))
                .findFirst();
    }

    public synchronized Expense addExpense(Expense expense) {
        ValidationUtil.validateExpense(expense);
        vehicleService.findById(expense.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(expense.getVehicleId().toString()));

        getAppData().getExpenses().add(expense);
        repository.save(getAppData());
        return expense;
    }

    public synchronized Expense updateExpense(Expense updated) {
        ValidationUtil.validateExpense(updated);
        Expense existing = findById(updated.getId())
                .orElseThrow(() -> new ValidationException("Expense not found: " + updated.getId()));

        vehicleService.findById(updated.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(updated.getVehicleId().toString()));

        existing.setDate(updated.getDate());
        existing.setCategory(updated.getCategory());
        existing.setAmount(updated.getAmount());
        existing.setDescription(updated.getDescription());
        existing.setDocumentPaths(updated.getDocumentPaths());

        repository.save(getAppData());
        return existing;
    }

    public synchronized void deleteExpense(UUID id) {
        getAppData().getExpenses().removeIf(e -> e.getId().equals(id));
        repository.save(getAppData());
    }

    public synchronized BigDecimal calculateTotalExpenses(UUID vehicleId) {
        return getAppData().getExpenses().stream()
                .filter(e -> vehicleId == null || vehicleId.equals(e.getVehicleId()))
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
