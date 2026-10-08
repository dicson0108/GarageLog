package com.garagelog.service;

import com.garagelog.model.*;
import com.garagelog.repository.JsonGarageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuelServiceTest {
    private Path tempDir;
    private JsonGarageRepository repository;
    private VehicleService vehicleService;
    private FuelService fuelService;
    private Vehicle testVehicle;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("garagelog-fuel-test-");
        repository = new JsonGarageRepository(tempDir);
        vehicleService = new VehicleService(repository);
        fuelService = new FuelService(repository, vehicleService);

        testVehicle = new Vehicle();
        testVehicle.setPlateNumber("TEST-CAR");
        testVehicle.setBrand("Toyota");
        testVehicle.setModel("Yaris");
        testVehicle.setYear(2022);
        testVehicle.setCurrentOdometer(10000);
        vehicleService.addVehicle(testVehicle);
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {}
            });
        }
    }

    @Test
    void testConsumptionBetweenConsecutiveFullTanks() {
        // Fill up 1: Full tank at 10,000 km
        FuelFillUp f1 = new FuelFillUp();
        f1.setVehicleId(testVehicle.getId());
        f1.setDate(LocalDate.of(2026, 1, 1));
        f1.setOdometer(10000);
        f1.setLitres(40);
        f1.setTotalCost(new BigDecimal("100.00"));
        f1.setFullTank(true);
        fuelService.addFillUp(f1);

        // Fill up 2: Partial fill up 10 litres at 10,200 km
        FuelFillUp f2 = new FuelFillUp();
        f2.setVehicleId(testVehicle.getId());
        f2.setDate(LocalDate.of(2026, 1, 5));
        f2.setOdometer(10200);
        f2.setLitres(10);
        f2.setTotalCost(new BigDecimal("25.00"));
        f2.setFullTank(false);
        fuelService.addFillUp(f2);

        // Fill up 3: Full tank at 10,600 km, took 20 litres
        // Distance from f1 = 600 km. Total fuel between f1 and f3 full tanks = 10 + 20 = 30 L.
        // Consumption = 30 L / 600 km * 100 = 5.0 L/100km.
        // km/L = 600 / 30 = 20.0 km/L.
        FuelFillUp f3 = new FuelFillUp();
        f3.setVehicleId(testVehicle.getId());
        f3.setDate(LocalDate.of(2026, 1, 10));
        f3.setOdometer(10600);
        f3.setLitres(20);
        f3.setTotalCost(new BigDecimal("50.00"));
        f3.setFullTank(true);
        fuelService.addFillUp(f3);

        List<FuelService.ConsumptionInterval> intervals = fuelService.calculateConsumptionHistory(testVehicle.getId());
        assertEquals(1, intervals.size());

        FuelService.ConsumptionInterval interval = intervals.get(0);
        assertEquals(600.0, interval.distanceKm());
        assertEquals(30.0, interval.litresConsumed());
        assertEquals(5.0, interval.litresPer100Km(), 0.001);
        assertEquals(20.0, interval.kmPerLitre(), 0.001);
    }
}
