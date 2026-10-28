package com.garagelog.service;

import com.garagelog.model.*;
import com.garagelog.repository.JsonGarageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTest {
    @Test
    void monthlyTotalsIncludeAllRecordTypesAndRespectMonthAndVehicleFilters(@TempDir Path directory) {
        var repository = new JsonGarageRepository(directory);
        var vehicles = new VehicleService(repository);
        var reports = new ReportService(vehicles, new MaintenanceService(repository, vehicles),
                new ExpenseService(repository, vehicles), new FuelService(repository, vehicles));
        var data = vehicles.getAppData();
        var month = YearMonth.now();
        var vehicleId = UUID.randomUUID();

        var maintenance = new MaintenanceRecord();
        maintenance.setVehicleId(vehicleId);
        maintenance.setDate(month.atDay(1));
        maintenance.setCost(new BigDecimal("100.25"));
        data.getMaintenanceRecords().add(maintenance);

        var expense = new Expense();
        expense.setVehicleId(vehicleId);
        expense.setDate(month.atEndOfMonth());
        expense.setCategory(ExpenseCategory.PARKING);
        expense.setAmount(new BigDecimal("20.50"));
        data.getExpenses().add(expense);

        var fuel = new FuelFillUp();
        fuel.setVehicleId(vehicleId);
        fuel.setDate(month.atDay(1));
        fuel.setTotalCost(new BigDecimal("30.75"));
        data.getFuelFillUps().add(fuel);

        var otherVehicle = new Expense();
        otherVehicle.setVehicleId(UUID.randomUUID());
        otherVehicle.setDate(month.minusMonths(1).atEndOfMonth());
        otherVehicle.setAmount(new BigDecimal("9.00"));
        data.getExpenses().add(otherVehicle);

        // Missing dates/amounts and records outside the reporting window are ignored.
        data.getMaintenanceRecords().add(new MaintenanceRecord());
        data.getExpenses().add(new Expense());
        data.getFuelFillUps().add(new FuelFillUp());
        var outsideWindow = new Expense();
        outsideWindow.setDate(month.plusMonths(1).atDay(1));
        outsideWindow.setAmount(new BigDecimal("999.00"));
        data.getExpenses().add(outsideWindow);

        var report = reports.getMonthlySpendingReport(null, 3);
        assertEquals(List.of(month.minusMonths(2), month.minusMonths(1), month),
                report.stream().map(ReportService.MonthlyCategorySpending::yearMonth).toList());
        assertEquals(List.of(BigDecimal.ZERO, new BigDecimal("9.00"), new BigDecimal("151.50")),
                report.stream().map(ReportService.MonthlyCategorySpending::total).toList());
        assertEquals(new BigDecimal("30.75"), report.getLast().spendingByCategory().get("Fuel"));
        var filtered = reports.getMonthlySpendingReport(vehicleId, 3);
        assertEquals(BigDecimal.ZERO, filtered.get(1).total());
        assertEquals(new BigDecimal("151.50"), filtered.getLast().total());
    }
}
