package com.garagelog.util;

import com.garagelog.exception.ValidationException;
import com.garagelog.model.Vehicle;
import com.garagelog.model.VehicleStatus;
import com.garagelog.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationUtilTest {

    @Test
    void testValidVehiclePasses() {
        Vehicle v = new Vehicle();
        v.setPlateNumber("BN21 LXR");
        v.setBrand("Toyota");
        v.setModel("Corolla");
        v.setYear(2021);
        v.setType(VehicleType.CAR);
        v.setStatus(VehicleStatus.ACTIVE);
        v.setCurrentOdometer(48260);

        assertDoesNotThrow(() -> ValidationUtil.validateVehicle(v));
    }

    @Test
    void testMissingPlateThrows() {
        Vehicle v = new Vehicle();
        v.setPlateNumber("   ");
        v.setBrand("Toyota");
        v.setModel("Corolla");
        v.setYear(2021);

        assertThrows(ValidationException.class, () -> ValidationUtil.validateVehicle(v));
    }

    @Test
    void testInvalidYearThrows() {
        Vehicle v = new Vehicle();
        v.setPlateNumber("ABC 123");
        v.setBrand("Toyota");
        v.setModel("Corolla");
        v.setYear(1899);

        assertThrows(ValidationException.class, () -> ValidationUtil.validateVehicle(v));

        v.setYear(LocalDate.now().getYear() + 2);
        assertThrows(ValidationException.class, () -> ValidationUtil.validateVehicle(v));
    }

    @Test
    void testNegativeOdometerThrows() {
        Vehicle v = new Vehicle();
        v.setPlateNumber("ABC 123");
        v.setBrand("Toyota");
        v.setModel("Corolla");
        v.setYear(2021);
        v.setCurrentOdometer(-10);

        assertThrows(ValidationException.class, () -> ValidationUtil.validateVehicle(v));
    }
}
