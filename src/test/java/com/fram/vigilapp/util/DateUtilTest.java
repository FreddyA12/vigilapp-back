package com.fram.vigilapp.util;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void week_month_year_ranges_are_consistent() {
        DateUtil util = new DateUtil();

        Date sw = util.getStartOfWeek();
        Date ew = util.getEndOfWeek();
        assertNotNull(sw);
        assertNotNull(ew);
        assertTrue(sw.before(ew) || sw.equals(ew));

        Date sm = util.getStartOfMonth();
        Date em = util.getEndOfMonth();
        assertNotNull(sm);
        assertNotNull(em);
        assertTrue(sm.before(em) || sm.equals(em));

        Date sy = util.getStartOfYear();
        Date ey = util.getEndOfYear();
        assertNotNull(sy);
        assertNotNull(ey);
        assertTrue(sy.before(ey) || sy.equals(ey));
    }
}
