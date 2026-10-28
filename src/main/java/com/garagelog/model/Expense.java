package com.garagelog.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Expense {
    private final UUID id;
    private UUID vehicleId;
    private LocalDate date;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String description;
    private List<String> documentPaths = new ArrayList<>();

    public Expense() {
        this.id = UUID.randomUUID();
        this.category = ExpenseCategory.OTHER;
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

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDocumentPaths() {
        if (documentPaths == null) {
            documentPaths = new ArrayList<>();
        }
        return documentPaths;
    }

    public void setDocumentPaths(List<String> documentPaths) {
        this.documentPaths = documentPaths != null ? documentPaths : new ArrayList<>();
    }
}
