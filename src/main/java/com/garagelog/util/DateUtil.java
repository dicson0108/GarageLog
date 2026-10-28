package com.garagelog.util;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateUtil {
    public static final String DATE_PATTERN = "dd/MM/yyyy";
    public static final DateTimeFormatter UI_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

    private static final DateTimeFormatter[] PARSE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("ddMMyyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy")
    };

    private DateUtil() {}

    public static String format(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.format(UI_FORMATTER);
    }

    public static String formatOrEmpty(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(UI_FORMATTER);
    }

    public static LocalDate parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String trimmed = text.trim();
        for (DateTimeFormatter formatter : PARSE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("Cannot parse date: " + text);
    }

    public static StringConverter<LocalDate> getDateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return formatOrEmpty(date);
            }

            @Override
            public LocalDate fromString(String string) {
                try {
                    return DateUtil.parse(string);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }
}
