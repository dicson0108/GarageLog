package com.garagelog.service;

import com.garagelog.exception.DuplicatePlateException;
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

import static org.junit.jupiter.api.Assertions.*;

class VehicleServiceTest {
    private Path tempDir;
    private JsonGarageRepository repository;
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("garagelog-vehicle-test-");
        repository = new JsonGarageRepository(tempDir);
        vehicleService = new VehicleService(repository);
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

    private Vehicle createVehicle(String plate, String brand, String model, int year) {
        Vehicle v = new Vehicle();
        v.setPlateNumber(plate);
        v.setBrand(brand);
        v.setModel(model);
        v.setYear(year);
        v.setType(VehicleType.CAR);
        v.setStatus(VehicleStatus.ACTIVE);
        v.setCurrentOdometer(10000);
        return v;
    }

    @Test
    void testAddAndRetrieveVehicle() {
        Vehicle v = createVehicle("BN21 LXR", "Toyota", "Corolla", 2021);
        vehicleService.addVehicle(v);

        assertTrue(vehicleService.findByPlate("bn-21-lxr").isPresent());
        assertEquals("Toyota", vehicleService.findByPlate("BN21LXR").get().getBrand());
    }

    @Test
    void testDuplicatePlateThrowsException() {
        Vehicle v1 = createVehicle("BN21 LXR", "Toyota", "Corolla", 2021);
        Vehicle v2 = createVehicle("bn21-lxr", "Honda", "Civic", 2022);

        vehicleService.addVehicle(v1);
        assertThrows(DuplicatePlateException.class, () -> vehicleService.addVehicle(v2));
    }

    @Test
    void testMarkAsSold() {
        Vehicle v = createVehicle("BN21 LXR", "Toyota", "Corolla", 2021);
        vehicleService.addVehicle(v);

        vehicleService.markAsSold(v.getId());
        assertEquals(VehicleStatus.SOLD, vehicleService.findById(v.getId()).get().getStatus());
        assertEquals(0, vehicleService.getAllVehicles(false).size());
        assertEquals(1, vehicleService.getAllVehicles(true).size());
    }

    @Test
    void testCascadingDelete() {
        Vehicle v = createVehicle("BN21 LXR", "Toyota", "Corolla", 2021);
        vehicleService.addVehicle(v);

        MaintenanceRecord m = new MaintenanceRecord();
        m.setVehicleId(v.getId());
        m.setDate(LocalDate.now());
        m.setWorkDone("Oil change");
        m.setCost(new BigDecimal("150.00"));
        vehicleService.getAppData().getMaintenanceRecords().add(m);

        assertEquals(1, vehicleService.countLinkedRecords(v.getId()));

        VehicleService.DeleteResult result = vehicleService.deleteVehicle(v.getId());
        assertEquals(1, result.maintenanceDeleted());
        assertEquals(1, result.totalDeleted());
        assertFalse(vehicleService.findById(v.getId()).isPresent());
        assertEquals(0, vehicleService.getAppData().getMaintenanceRecords().size());
    }

    @Test
    void testSearchAndFilter() {
        Vehicle v1 = createVehicle("BN21 LXR", "Toyota", "Corolla", 2021);
        Vehicle v2 = createVehicle("GN19 KPT", "Ford", "Transit Custom", 2019);
        v2.setType(VehicleType.TRUCK_VAN);
        vehicleService.addVehicle(v1);
        vehicleService.addVehicle(v2);

        List<Vehicle> filtered = vehicleService.searchAndFilter("transit", null, null, null, false, "plate", true);
        assertEquals(1, filtered.size());
        assertEquals("GN19 KPT", filtered.get(0).getPlateNumber());

        List<Vehicle> vanOnly = vehicleService.searchAndFilter("", null, VehicleType.TRUCK_VAN, null, false, "plate", true);
        assertEquals(1, vanOnly.size());
    }
}
