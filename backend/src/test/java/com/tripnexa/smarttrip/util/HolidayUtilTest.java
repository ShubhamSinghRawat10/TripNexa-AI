package com.tripnexa.smarttrip.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HolidayUtilTest {
    @Test
    void flagsWeekendAsPeak() {
        LocalDate saturday = LocalDate.of(2026, 7, 4);
        assertThat(HolidayUtil.isPeakDate(saturday)).isTrue();
        assertThat(HolidayUtil.peakReason(saturday)).isEqualTo("Weekend");
    }

    @Test
    void flagsMovingAndFixedPublicHolidays() {
        assertThat(HolidayUtil.holidayName(LocalDate.of(2026, 11, 8))).contains("Diwali");
        assertThat(HolidayUtil.holidayName(LocalDate.of(2027, 1, 26))).contains("Republic Day");
    }

    @Test
    void leavesRegularWeekdayUnflagged() {
        LocalDate weekday = LocalDate.of(2026, 7, 2);
        assertThat(HolidayUtil.isPeakDate(weekday)).isFalse();
        assertThat(HolidayUtil.peakReason(weekday)).isEqualTo("Regular weekday");
    }
}
