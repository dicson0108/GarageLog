package com.garagelog.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void testFormat() {
        assertEquals("08/10/2026", DateUtil.format(LocalDate.of(2026, 10, 8)));
        assertEquals("-", DateUtil.format(null));
        assertEquals("08/10/2026", DateUtil.formatOrEmpty(LocalDate.of(2026, 10, 8)));
        assertEquals("", DateUtil.formatOrEmpty(null));
    }

    @Test
    void testParse() {
        assertEquals(LocalDate.of(2026, 10, 8), DateUtil.parse("08/10/2026"));
        assertEquals(LocalDate.of(2026, 10, 8), DateUtil.parse("2026-10-08"));
        assertEquals(LocalDate.of(2026, 10, 8), DateUtil.parse("08-10-2026"));
        assertEquals(LocalDate.of(2026, 10, 8), DateUtil.parse("08102026"));
        assertEquals(LocalDate.of(2026, 10, 8), DateUtil.parse("8/10/2026"));
        assertNull(DateUtil.parse(""));
        assertNull(DateUtil.parse(null));
    }

    @Test
    void testStringConverter() {
        var converter = DateUtil.getDateConverter();
        assertEquals("08/10/2026", converter.toString(LocalDate.of(2026, 10, 8)));
        assertEquals(LocalDate.of(2026, 10, 8), converter.fromString("08/10/2026"));
    }
}
