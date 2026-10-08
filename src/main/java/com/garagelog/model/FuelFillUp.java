package com.garagelog.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class FuelFillUp {
    private final UUID id;
    private UUID vehicleId;
    private LocalDate date;
    private double odometer;
    private double litres;
    private BigDecimal pricePerLitre;
    private BigDecimal totalCost;
    private boolean fullTank = true;

    public FuelFillUp() {
        this.id = UUID.randomUUID();
    }

    public FuelFillUp(UUID id) {
        this.id = id != null ? id : UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(UUID vehicleId) {
        this.vehicleId = vehicleId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getOdometer() {
        return odometer;
    }

    public void setOdometer(double odometer) {
        this.odometer = odometer;
    }

    public double getLitres() {
        return litres;
    }

    public void setLitres(double litres) {
        this.litres = litres;
    }

    public BigDecimal getPricePerLitre() {
        return pricePerLitre;
    }

    public void setPricePerLitre(BigDecimal pricePerLitre) {
        this.pricePerLitre = pricePerLitre;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public boolean isFullTank() {
        return fullTank;
    }

    public void setFullTank(boolean fullTank) {
        this.fullTank = fullTank;
    }
}
