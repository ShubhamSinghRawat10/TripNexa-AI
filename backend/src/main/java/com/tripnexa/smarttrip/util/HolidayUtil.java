package com.tripnexa.smarttrip.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;
import java.util.Optional;

public final class HolidayUtil {
    private static final Map<MonthDay, String> FIXED_HOLIDAYS = Map.of(
        MonthDay.of(1, 26), "Republic Day",
        MonthDay.of(8, 15), "Independence Day",
        MonthDay.of(10, 2), "Gandhi Jayanti",
        MonthDay.of(12, 25), "Christmas Day"
    );

    // Festival dates move each year. Keep these explicit and update them annually from the
    // Government of India holiday calendar instead of attempting unreliable lunar calculations.
    private static final Map<LocalDate, String> FESTIVALS_2026 = Map.ofEntries(
        Map.entry(LocalDate.of(2026, 3, 4), "Holi"),
        Map.entry(LocalDate.of(2026, 3, 21), "Id-ul-Fitr"),
        Map.entry(LocalDate.of(2026, 3, 26), "Ram Navami"),
        Map.entry(LocalDate.of(2026, 3, 31), "Mahavir Jayanti"),
        Map.entry(LocalDate.of(2026, 4, 3), "Good Friday"),
        Map.entry(LocalDate.of(2026, 5, 1), "Buddha Purnima"),
        Map.entry(LocalDate.of(2026, 5, 27), "Id-ul-Zuha"),
        Map.entry(LocalDate.of(2026, 6, 26), "Muharram"),
        Map.entry(LocalDate.of(2026, 8, 26), "Id-e-Milad"),
        Map.entry(LocalDate.of(2026, 9, 4), "Janmashtami"),
        Map.entry(LocalDate.of(2026, 10, 20), "Dussehra"),
        Map.entry(LocalDate.of(2026, 11, 8), "Diwali"),
        Map.entry(LocalDate.of(2026, 11, 24), "Guru Nanak Jayanti")
    );

    private HolidayUtil() {}

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
            || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static boolean isPublicHoliday(LocalDate date) {
        return holidayName(date).isPresent();
    }

    public static boolean isPeakDate(LocalDate date) {
        return isWeekend(date) || isPublicHoliday(date);
    }

    public static Optional<String> holidayName(LocalDate date) {
        String festival = FESTIVALS_2026.get(date);
        if (festival != null) {
            return Optional.of(festival);
        }
        return Optional.ofNullable(FIXED_HOLIDAYS.get(MonthDay.from(date)));
    }

    public static String peakReason(LocalDate date) {
        return holidayName(date).orElseGet(() -> isWeekend(date) ? "Weekend" : "Regular weekday");
    }
}
