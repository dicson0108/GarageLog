package com.garagelog.model;

import java.math.BigDecimal;

public class Settings {
    private String currency = "MYR";
    private String distanceUnit = "km";
    private int reminderLeadDays = 30;
    private int reminderLeadKm = 500;
    private BigDecimal monthlyBudget = null;

    public Settings() {
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDistanceUnit() {
        return distanceUnit;
    }

    public void setDistanceUnit(String distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    public int getReminderLeadDays() {
        return reminderLeadDays;
    }

    public void setReminderLeadDays(int reminderLeadDays) {
        this.reminderLeadDays = reminderLeadDays;
    }

    public int getReminderLeadKm() {
        return reminderLeadKm;
    }

    public void setReminderLeadKm(int reminderLeadKm) {
        this.reminderLeadKm = reminderLeadKm;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }
}
