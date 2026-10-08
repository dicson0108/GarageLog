package com.garagelog.util;

import com.garagelog.model.*;
import com.garagelog.service.ExpenseService;
import com.garagelog.service.FuelService;
import com.garagelog.service.MaintenanceService;
import com.garagelog.service.VehicleService;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class SampleDataGenerator {
    private SampleDataGenerator() {}

    public static void populateIfEmpty(VehicleService vehicleService, MaintenanceService maintenanceService,
                                       ExpenseService expenseService, FuelService fuelService) {
        if (!vehicleService.getAllVehicles(true).isEmpty()) {
            return;
        }

        // Vehicle 1: Daily driver (Toyota Corolla)
        Vehicle v1 = new Vehicle();
        v1.setPlateNumber("BN21 LXR");
        v1.setNickname("Daily driver");
        v1.setBrand("Toyota");
        v1.setModel("Corolla");
        v1.setYear(2021);
        v1.setType(VehicleType.CAR);
        v1.setColour("Silver");
        v1.setFuelType(FuelType.PETROL);
        v1.setCurrentOdometer(48260);
        v1.setPurchaseDate(LocalDate.now().minusYears(2));
        v1.setPurchasePrice(new BigDecimal("18500.00"));
        v1.setStatus(VehicleStatus.ACTIVE);
        v1.setInsuranceExpiryDate(LocalDate.now().plusDays(45));
        v1.setRoadTaxExpiryDate(LocalDate.now().plusDays(70));
        vehicleService.addVehicle(v1);

        // Vehicle 2: Workhorse (Ford Transit Custom)
        Vehicle v2 = new Vehicle();
        v2.setPlateNumber("GN19 KPT");
        v2.setNickname("Workhorse");
        v2.setBrand("Ford");
        v2.setModel("Transit Custom");
        v2.setYear(2019);
        v2.setType(VehicleType.TRUCK_VAN);
        v2.setColour("White");
        v2.setFuelType(FuelType.DIESEL);
        v2.setCurrentOdometer(96840);
        v2.setPurchaseDate(LocalDate.now().minusYears(3));
        v2.setPurchasePrice(new BigDecimal("14200.00"));
        v2.setStatus(VehicleStatus.ACTIVE);
        v2.setInsuranceExpiryDate(LocalDate.now().plusDays(90));
        v2.setRoadTaxExpiryDate(LocalDate.now().plusDays(18)); // Upcoming reminder
        vehicleService.addVehicle(v2);

        // Vehicle 3: Weekend (Mazda MX-5)
        Vehicle v3 = new Vehicle();
        v3.setPlateNumber("LK23 VNM");
        v3.setNickname("Weekend");
        v3.setBrand("Mazda");
        v3.setModel("MX-5");
        v3.setYear(2023);
        v3.setType(VehicleType.CAR);
        v3.setColour("Soul Red");
        v3.setFuelType(FuelType.PETROL);
        v3.setCurrentOdometer(18420);
        v3.setPurchaseDate(LocalDate.now().minusMonths(14));
        v3.setPurchasePrice(new BigDecimal("24000.00"));
        v3.setStatus(VehicleStatus.ACTIVE);
        v3.setInsuranceExpiryDate(LocalDate.now().plusDays(11)); // Upcoming reminder
        v3.setRoadTaxExpiryDate(LocalDate.now().plusDays(120));
        vehicleService.addVehicle(v3);

        // Vehicle 4: City electric (Volkswagen ID.3)
        Vehicle v4 = new Vehicle();
        v4.setPlateNumber("RX22 ELC");
        v4.setNickname("City electric");
        v4.setBrand("Volkswagen");
        v4.setModel("ID.3");
        v4.setYear(2022);
        v4.setType(VehicleType.CAR);
        v4.setColour("Glacier White");
        v4.setFuelType(FuelType.ELECTRIC);
        v4.setCurrentOdometer(31200);
        v4.setPurchaseDate(LocalDate.now().minusYears(2));
        v4.setPurchasePrice(new BigDecimal("22500.00"));
        v4.setStatus(VehicleStatus.ACTIVE);
        v4.setInsuranceExpiryDate(LocalDate.now().plusDays(80));
        v4.setRoadTaxExpiryDate(LocalDate.now().plusDays(150));
        vehicleService.addVehicle(v4);

        // Vehicle 5: Sold car (Volkswagen Golf)
        Vehicle v5 = new Vehicle();
        v5.setPlateNumber("WF16 JDS");
        v5.setNickname("First car");
        v5.setBrand("Volkswagen");
        v5.setModel("Golf");
        v5.setYear(2016);
        v5.setType(VehicleType.CAR);
        v5.setColour("Black");
        v5.setFuelType(FuelType.PETROL);
        v5.setCurrentOdometer(112600);
        v5.setPurchaseDate(LocalDate.now().minusYears(5));
        v5.setPurchasePrice(new BigDecimal("9800.00"));
        v5.setStatus(VehicleStatus.SOLD);
        vehicleService.addVehicle(v5);

        // Records for Vehicle 1
        MaintenanceRecord m1 = new MaintenanceRecord();
        m1.setVehicleId(v1.getId());
        m1.setDate(LocalDate.now().minusMonths(6));
        m1.setOdometer(40000);
        m1.setWorkDone("Oil and filters");
        m1.setCost(new BigDecimal("320.00"));
        m1.setWorkshop("Apex Autocare");
        m1.setNextServiceDueDate(LocalDate.now().minusDays(6)); // Overdue reminder
        m1.setNextServiceDueOdometer(48000.0);
        maintenanceService.addRecord(m1);

        Expense e1 = new Expense();
        e1.setVehicleId(v2.getId());
        e1.setDate(LocalDate.now().minusDays(4));
        e1.setCategory(ExpenseCategory.INSURANCE);
        e1.setAmount(new BigDecimal("620.00"));
        e1.setDescription("Annual commercial policy renewal");
        expenseService.addExpense(e1);

        FuelFillUp f1 = new FuelFillUp();
        f1.setVehicleId(v1.getId());
        f1.setDate(LocalDate.now());
        f1.setOdometer(48260);
        f1.setLitres(42.5);
        f1.setPricePerLitre(new BigDecimal("1.98"));
        f1.setTotalCost(new BigDecimal("84.15"));
        f1.setFullTank(true);
        fuelService.addFillUp(f1);

        FuelFillUp f0 = new FuelFillUp();
        f0.setVehicleId(v1.getId());
        f0.setDate(LocalDate.now().minusDays(10));
        f0.setOdometer(47680);
        f0.setLitres(44.0);
        f0.setPricePerLitre(new BigDecimal("1.95"));
        f0.setTotalCost(new BigDecimal("85.80"));
        f0.setFullTank(true);
        fuelService.addFillUp(f0);
    }
}
