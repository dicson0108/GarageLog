package com.garagelog.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Vehicle {
    private final UUID id;
    private String plateNumber;
    private String nickname;
    private String brand;
    private String model;
    private int year;
    private VehicleType type;
    private String colour;
    private FuelType fuelType;
    private String vin;
    private double currentOdometer;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private VehicleStatus status;
    private LocalDate insuranceExpiryDate;
    private LocalDate roadTaxExpiryDate;
    private String photoPath;
    private String notes;

    public Vehicle() {
        this.id = UUID.randomUUID();
        this.type = VehicleType.CAR;
        this.status = VehicleStatus.ACTIVE;
        this.fuelType = FuelType.PETROL;
    }

    public Vehicle(UUID id) {
        this.id = id != null ? id : UUID.randomUUID();
        this.type = VehicleType.CAR;
        this.status = VehicleStatus.ACTIVE;
        this.fuelType = FuelType.PETROL;
    }

    public UUID getId() {
        return id;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public VehicleType getType() {
        return type;
    }

    public void setType(VehicleType type) {
        this.type = type;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public double getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(double currentOdometer) {
        this.currentOdometer = currentOdometer;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public LocalDate getInsuranceExpiryDate() {
        return insuranceExpiryDate;
    }

    public void setInsuranceExpiryDate(LocalDate insuranceExpiryDate) {
        this.insuranceExpiryDate = insuranceExpiryDate;
    }

    public LocalDate getRoadTaxExpiryDate() {
        return roadTaxExpiryDate;
    }

    public void setRoadTaxExpiryDate(LocalDate roadTaxExpiryDate) {
        this.roadTaxExpiryDate = roadTaxExpiryDate;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname + (plateNumber != null && !plateNumber.isBlank() ? " (" + plateNumber + ")" : "");
        }
        String makeModel = ((brand != null ? brand : "") + " " + (model != null ? model : "")).trim();
        if (!makeModel.isEmpty()) {
            return makeModel + (plateNumber != null && !plateNumber.isBlank() ? " (" + plateNumber + ")" : "");
        }
        return plateNumber != null ? plateNumber : "Vehicle";
    }

    @Override
    public String toString() {
        String makeModel = ((brand != null ? brand : "") + " " + (model != null ? model : "")).trim();
        String plate = plateNumber != null ? plateNumber : "";
        if (nickname != null && !nickname.isBlank()) {
            if (!makeModel.isEmpty() && !nickname.equalsIgnoreCase(makeModel)) {
                return plate + (plate.isEmpty() ? "" : " · ") + makeModel + " (" + nickname + ")";
            }
            return plate + (plate.isEmpty() ? "" : " · ") + nickname;
        }
        return plate + (plate.isEmpty() ? "" : " · ") + (!makeModel.isEmpty() ? makeModel : "Vehicle");
    }
}
