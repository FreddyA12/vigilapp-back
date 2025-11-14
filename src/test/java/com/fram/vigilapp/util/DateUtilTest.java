package com.fram.vigilapp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    private DateUtil dateUtil;

    @BeforeEach
    void setUp() {
        dateUtil = new DateUtil();
    }

    @Test
    void getStartOfWeek_ShouldReturnMonday() {
        // When
        Date startOfWeek = dateUtil.getStartOfWeek();

        // Then
        assertNotNull(startOfWeek);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfWeek);

        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    void getEndOfWeek_ShouldReturnSunday() {
        // When
        Date endOfWeek = dateUtil.getEndOfWeek();

        // Then
        assertNotNull(endOfWeek);
        Calendar cal = Calendar.getInstance();
        cal.setTime(endOfWeek);

        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    void getStartOfMonth_ShouldReturnFirstDayOfMonth() {
        // When
        Date startOfMonth = dateUtil.getStartOfMonth();

        // Then
        assertNotNull(startOfMonth);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfMonth);

        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    void getEndOfMonth_ShouldReturnLastDayOfMonth() {
        // When
        Date endOfMonth = dateUtil.getEndOfMonth();

        // Then
        assertNotNull(endOfMonth);
        Calendar cal = Calendar.getInstance();
        cal.setTime(endOfMonth);

        LocalDate today = LocalDate.now();
        int lastDay = today.lengthOfMonth();

        assertEquals(lastDay, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    void getStartOfYear_ShouldReturnJanuary1st() {
        // When
        Date startOfYear = dateUtil.getStartOfYear();

        // Then
        assertNotNull(startOfYear);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startOfYear);

        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    void getEndOfYear_ShouldReturnDecember31st() {
        // When
        Date endOfYear = dateUtil.getEndOfYear();

        // Then
        assertNotNull(endOfYear);
        Calendar cal = Calendar.getInstance();
        cal.setTime(endOfYear);

        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    void startOfWeek_ShouldBeBeforeEndOfWeek() {
        // When
        Date start = dateUtil.getStartOfWeek();
        Date end = dateUtil.getEndOfWeek();

        // Then
        assertTrue(start.before(end));
    }

    @Test
    void startOfMonth_ShouldBeBeforeEndOfMonth() {
        // When
        Date start = dateUtil.getStartOfMonth();
        Date end = dateUtil.getEndOfMonth();

        // Then
        assertTrue(start.before(end));
    }

    @Test
    void startOfYear_ShouldBeBeforeEndOfYear() {
        // When
        Date start = dateUtil.getStartOfYear();
        Date end = dateUtil.getEndOfYear();

        // Then
        assertTrue(start.before(end));
    }

    @Test
    void allDates_ShouldBeInCurrentYear() {
        // Given
        int currentYear = LocalDate.now().getYear();

        // When
        Date startOfWeek = dateUtil.getStartOfWeek();
        Date endOfWeek = dateUtil.getEndOfWeek();
        Date startOfMonth = dateUtil.getStartOfMonth();
        Date endOfMonth = dateUtil.getEndOfMonth();
        Date startOfYear = dateUtil.getStartOfYear();
        Date endOfYear = dateUtil.getEndOfYear();

        // Then
        Calendar cal = Calendar.getInstance();

        cal.setTime(startOfWeek);
        assertTrue(cal.get(Calendar.YEAR) == currentYear || cal.get(Calendar.YEAR) == currentYear - 1);

        cal.setTime(endOfWeek);
        assertTrue(cal.get(Calendar.YEAR) == currentYear || cal.get(Calendar.YEAR) == currentYear + 1);

        cal.setTime(startOfMonth);
        assertEquals(currentYear, cal.get(Calendar.YEAR));

        cal.setTime(endOfMonth);
        assertEquals(currentYear, cal.get(Calendar.YEAR));

        cal.setTime(startOfYear);
        assertEquals(currentYear, cal.get(Calendar.YEAR));

        cal.setTime(endOfYear);
        assertEquals(currentYear, cal.get(Calendar.YEAR));
    }
}
