package com.garagelog.service;

import com.garagelog.exception.VehicleNotFoundException;
import com.garagelog.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReportService {
    private final VehicleService vehicleService;
    private final MaintenanceService maintenanceService;
    private final ExpenseService expenseService;
    private final FuelService fuelService;

    public ReportService(VehicleService vehicleService, MaintenanceService maintenanceService,
                         ExpenseService expenseService, FuelService fuelService) {
        this.vehicleService = vehicleService;
        this.maintenanceService = maintenanceService;
        this.expenseService = expenseService;
        this.fuelService = fuelService;
    }

    public record VehicleTcoSummary(
            UUID vehicleId,
            String plateNumber,
            String vehicleDisplayName,
            BigDecimal purchasePrice,
            BigDecimal totalMaintenance,
            BigDecimal totalExpenses,
            BigDecimal totalFuel,
            BigDecimal totalCostOfOwnership,
            BigDecimal costPerKm,
            BigDecimal costPerMonth,
            long monthsOwned
    ) {}

    public synchronized VehicleTcoSummary calculateTco(UUID vehicleId) {
        Vehicle vehicle = vehicleService.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId.toString()));

        BigDecimal purchase = vehicle.getPurchasePrice() != null ? vehicle.getPurchasePrice() : BigDecimal.ZERO;

        BigDecimal maintenanceSum = maintenanceService.getRecordsForVehicle(vehicleId).stream()
                .map(MaintenanceRecord::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenseSum = expenseService.calculateTotalExpenses(vehicleId);

        BigDecimal fuelSum = fuelService.calculateTotalFuelCost(vehicleId);

        BigDecimal grandTotal = purchase.add(maintenanceSum).add(expenseSum).add(fuelSum);

        double odo = vehicle.getCurrentOdometer();
        BigDecimal costPerKm = (odo > 0)
                ? grandTotal.divide(BigDecimal.valueOf(odo), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDate startDate = vehicle.getPurchaseDate() != null ? vehicle.getPurchaseDate() : LocalDate.now().minusMonths(1);
        long months = Math.max(1, ChronoUnit.MONTHS.between(startDate, LocalDate.now()));
        BigDecimal costPerMonth = grandTotal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);

        return new VehicleTcoSummary(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getDisplayName(),
                purchase,
                maintenanceSum,
                expenseSum,
                fuelSum,
                grandTotal,
                costPerKm,
                costPerMonth,
                months
        );
    }

    public record MonthlyCategorySpending(
            YearMonth yearMonth,
            Map<String, BigDecimal> spendingByCategory,
            BigDecimal total
    ) {}

    public synchronized List<MonthlyCategorySpending> getMonthlySpendingReport(UUID vehicleIdFilter, int recentMonths) {
        AppData data = vehicleService.getAppData();
        YearMonth start = YearMonth.now().minusMonths(recentMonths - 1);

        Map<YearMonth, Map<String, BigDecimal>> monthMap = new TreeMap<>();
        for (int i = 0; i < recentMonths; i++) {
            monthMap.put(start.plusMonths(i), new HashMap<>());
        }

        // Maintenance
        for (MaintenanceRecord m : data.getMaintenanceRecords()) {
            if (vehicleIdFilter != null && !vehicleIdFilter.equals(m.getVehicleId())) continue;
            if (m.getDate() == null || m.getCost() == null) continue;
            YearMonth ym = YearMonth.from(m.getDate());
            if (monthMap.containsKey(ym)) {
                monthMap.get(ym).merge("Maintenance", m.getCost(), BigDecimal::add);
            }
        }

        // Expenses
        for (Expense e : data.getExpenses()) {
            if (vehicleIdFilter != null && !vehicleIdFilter.equals(e.getVehicleId())) continue;
            if (e.getDate() == null || e.getAmount() == null) continue;
            YearMonth ym = YearMonth.from(e.getDate());
            if (monthMap.containsKey(ym)) {
                String cat = e.getCategory() != null ? e.getCategory().getDisplayName() : "Other";
                monthMap.get(ym).merge(cat, e.getAmount(), BigDecimal::add);
            }
        }

        // Fuel
        for (FuelFillUp f : data.getFuelFillUps()) {
            if (vehicleIdFilter != null && !vehicleIdFilter.equals(f.getVehicleId())) continue;
            if (f.getDate() == null || f.getTotalCost() == null) continue;
            YearMonth ym = YearMonth.from(f.getDate());
            if (monthMap.containsKey(ym)) {
                monthMap.get(ym).merge("Fuel", f.getTotalCost(), BigDecimal::add);
            }
        }

        List<MonthlyCategorySpending> result = new ArrayList<>();
        for (var entry : monthMap.entrySet()) {
            BigDecimal total = entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            result.add(new MonthlyCategorySpending(entry.getKey(), entry.getValue(), total));
        }

        return result;
    }
}
