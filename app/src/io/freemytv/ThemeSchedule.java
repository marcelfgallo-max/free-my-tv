package io.freemytv;

import java.util.Calendar;
import java.util.TimeZone;

/** Local wall-clock schedule; no alarms, network or background service. */
final class ThemeSchedule {
    static final int DAY_START = 7 * 60, NIGHT_START = 19 * 60, STEP = 30;
    private ThemeSchedule() {}

    static boolean valid(int day, int night) {
        return day >= 0 && day < 1440 && night >= 0 && night < 1440 && day != night;
    }

    static boolean isNight(String mode, int day, int night, int minute) {
        if (!Theme.MODE_AUTO.equals(Theme.normalizeMode(mode)))
            return !Theme.MODE_DAY.equals(Theme.normalizeMode(mode));
        if (!valid(day, night)) { day = DAY_START; night = NIGHT_START; }
        minute = Math.floorMod(minute, 1440);
        boolean inDay = day < night ? minute >= day && minute < night : minute >= day || minute < night;
        return !inDay;
    }

    static boolean at(String mode, int day, int night, long now, TimeZone zone) {
        if (!Theme.MODE_AUTO.equals(mode)) return isNight(mode, day, night, 0);
        Calendar clock = Calendar.getInstance(zone);
        clock.setTimeInMillis(now);
        return isNight(mode, day, night, clock.get(Calendar.HOUR_OF_DAY) * 60 + clock.get(Calendar.MINUTE));
    }
}
