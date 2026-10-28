package com.garagelog.service;

import com.garagelog.model.MaintenanceRecord;
import com.garagelog.model.Settings;
import com.garagelog.model.Vehicle;
import com.garagelog.util.DateUtil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ReminderService {
    private final VehicleService vehicleService;
    private final MaintenanceService maintenanceService;

    // Track snoozed reminder items by unique key -> snoozed until date
    private final Map<String, LocalDate> snoozedReminders = new HashMap<>();
    private final Set<String> dismissedReminders = new HashSet<>();

    public ReminderService(VehicleService vehicleService, MaintenanceService maintenanceService) {
        this.vehicleService = vehicleService;
        this.maintenanceService = maintenanceService;
    }

    public enum ReminderSeverity {
        OVERDUE,
        UPCOMING
    }

    public enum ReminderType {
        MAINTENANCE_DATE,
        MAINTENANCE_ODOMETER,
        INSURANCE,
        ROAD_TAX
    }

    public record ReminderItem(
            String id,
            UUID vehicleId,
            String vehiclePlate,
            String vehicleName,
            ReminderType type,
            ReminderSeverity severity,
            String title,
            String description,
            LocalDate dueDate,
            Double dueOdometer,
            long daysRemaining,
            double kmRemaining
    ) {}

    public synchronized List<ReminderItem> getActiveReminders() {
        Settings settings = vehicleService.getAppData().getSettings();
        int leadDays = settings.getReminderLeadDays();
        int leadKm = settings.getReminderLeadKm();
        LocalDate today = LocalDate.now();

        List<ReminderItem> items = new ArrayList<>();

        for (Vehicle v : vehicleService.getAllVehicles(false)) {
            // Insurance reminder
            if (v.getInsuranceExpiryDate() != null) {
                String reminderId = "INSURANCE-" + v.getId();
                if (!isIgnored(reminderId, today)) {
                    LocalDate expiry = v.getInsuranceExpiryDate();
                    long daysDiff = ChronoUnit.DAYS.between(today, expiry);
                    if (daysDiff < 0) {
                        items.add(new ReminderItem(
                                reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                ReminderType.INSURANCE, ReminderSeverity.OVERDUE,
                                "Insurance expired",
                                "Expired on " + DateUtil.format(expiry) + " · " + Math.abs(daysDiff) + " days overdue",
                                expiry, null, daysDiff, 0
                        ));
                    } else if (daysDiff <= leadDays) {
                        items.add(new ReminderItem(
                                reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                ReminderType.INSURANCE, ReminderSeverity.UPCOMING,
                                "Insurance expires soon",
                                "Expires " + DateUtil.format(expiry) + " · " + daysDiff + " days remaining",
                                expiry, null, daysDiff, 0
                        ));
                    }
                }
            }

            // Road tax reminder
            if (v.getRoadTaxExpiryDate() != null) {
                String reminderId = "ROADTAX-" + v.getId();
                if (!isIgnored(reminderId, today)) {
                    LocalDate expiry = v.getRoadTaxExpiryDate();
                    long daysDiff = ChronoUnit.DAYS.between(today, expiry);
                    if (daysDiff < 0) {
                        items.add(new ReminderItem(
                                reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                ReminderType.ROAD_TAX, ReminderSeverity.OVERDUE,
                                "Road tax expired",
                                "Expired on " + DateUtil.format(expiry) + " · " + Math.abs(daysDiff) + " days overdue",
                                expiry, null, daysDiff, 0
                        ));
                    } else if (daysDiff <= leadDays) {
                        items.add(new ReminderItem(
                                reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                ReminderType.ROAD_TAX, ReminderSeverity.UPCOMING,
                                "Road tax renewal",
                                "Expires " + DateUtil.format(expiry) + " · " + daysDiff + " days remaining",
                                expiry, null, daysDiff, 0
                        ));
                    }
                }
            }

            // Maintenance reminders from records
            for (MaintenanceRecord m : maintenanceService.getRecordsForVehicle(v.getId())) {
                if (m.getNextServiceDueDate() != null) {
                    String reminderId = "MAINT-DATE-" + m.getId();
                    if (!isIgnored(reminderId, today)) {
                        LocalDate due = m.getNextServiceDueDate();
                        long daysDiff = ChronoUnit.DAYS.between(today, due);
                        if (daysDiff < 0) {
                            items.add(new ReminderItem(
                                     reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                    ReminderType.MAINTENANCE_DATE, ReminderSeverity.OVERDUE,
                                    "Service overdue",
                                    "Due " + DateUtil.format(due) + " · " + Math.abs(daysDiff) + " days overdue",
                                    due, null, daysDiff, 0
                            ));
                        } else if (daysDiff <= leadDays) {
                            items.add(new ReminderItem(
                                    reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                    ReminderType.MAINTENANCE_DATE, ReminderSeverity.UPCOMING,
                                    "Service approaching",
                                    "Due " + DateUtil.format(due) + " · " + daysDiff + " days remaining",
                                    due, null, daysDiff, 0
                            ));
                        }
                    }
                }

                if (m.getNextServiceDueOdometer() != null) {
                    String reminderId = "MAINT-KM-" + m.getId();
                    if (!isIgnored(reminderId, today)) {
                        double diffKm = m.getNextServiceDueOdometer() - v.getCurrentOdometer();
                        if (diffKm < 0) {
                            items.add(new ReminderItem(
                                    reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                    ReminderType.MAINTENANCE_ODOMETER, ReminderSeverity.OVERDUE,
                                    "Service overdue by odometer",
                                    "Due at " + m.getNextServiceDueOdometer().intValue() + " " + settings.getDistanceUnit() +
                                            " · " + Math.abs((int) diffKm) + " " + settings.getDistanceUnit() + " overdue",
                                    null, m.getNextServiceDueOdometer(), 0, diffKm
                            ));
                        } else if (diffKm <= leadKm) {
                            items.add(new ReminderItem(
                                    reminderId, v.getId(), v.getPlateNumber(), v.getDisplayName(),
                                    ReminderType.MAINTENANCE_ODOMETER, ReminderSeverity.UPCOMING,
                                    "Service approaching by odometer",
                                    "Due at " + m.getNextServiceDueOdometer().intValue() + " " + settings.getDistanceUnit() +
                                            " · " + ((int) diffKm) + " " + settings.getDistanceUnit() + " remaining",
                                    null, m.getNextServiceDueOdometer(), 0, diffKm
                            ));
                        }
                    }
                }
            }
        }

        items.sort(Comparator.comparing(ReminderItem::severity)
                .thenComparing(r -> r.dueDate() != null ? r.dueDate() : LocalDate.MAX));
        return items;
    }

    private boolean isIgnored(String id, LocalDate today) {
        if (dismissedReminders.contains(id)) {
            return true;
        }
        LocalDate snoozedUntil = snoozedReminders.get(id);
        return snoozedUntil != null && !today.isAfter(snoozedUntil);
    }

    public synchronized void snoozeReminder(String reminderId, int days) {
        snoozedReminders.put(reminderId, LocalDate.now().plusDays(days));
    }

    public synchronized void dismissReminder(String reminderId) {
        dismissedReminders.add(reminderId);
    }
}
