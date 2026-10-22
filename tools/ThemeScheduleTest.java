package io.freemytv;

import java.time.Instant;
import java.util.TimeZone;

/** Boundary and clock-change tests for the production schedule resolver. */
public final class ThemeScheduleTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static boolean at(String instant, String zone, int day, int night) {
        return ThemeSchedule.at(Theme.MODE_AUTO, day, night, Instant.parse(instant).toEpochMilli(), TimeZone.getTimeZone(zone));
    }
    public static void main(String[] args) {
        check(!ThemeSchedule.valid(420, 420), "equal times rejected");
        check(!ThemeSchedule.valid(-1, 1140), "negative time rejected");
        check(!ThemeSchedule.valid(420, 1440), "end of day excluded");
        check(ThemeSchedule.valid(0, 1), "midnight and adjacent minutes allowed");
        // Every selectable pair, every minute. The Day arc starts inclusive and ends exclusive.
        for (int day = 0; day < 1440; day += 30) for (int night = 0; night < 1440; night += 30) {
            if (day == night) continue;
            int duration = Math.floorMod(night - day, 1440), dayMinutes = 0;
            for (int minute = 0; minute < 1440; minute++) {
                boolean expected = Math.floorMod(minute - day, 1440) >= duration;
                boolean result = ThemeSchedule.isNight("auto", day, night, minute);
                check(result == expected, "schedule interval mismatch");
                if (!result) dayMinutes++;
            }
            check(dayMinutes == duration, "wrong Day duration");
            check(!ThemeSchedule.isNight("auto", day, night, day), "Day boundary");
            check(ThemeSchedule.isNight("auto", day, night, night), "Night boundary");
        }
        for (int minute=0;minute<1440;minute++) {
            check(!ThemeSchedule.isNight("day",420,1140,minute), "manual Day changed");
            check(ThemeSchedule.isNight("night",420,1140,minute), "manual Night changed");
            check(ThemeSchedule.isNight(null,420,1140,minute), "missing mode must preserve Night");
            check(ThemeSchedule.isNight("unknown",420,1140,minute), "unknown mode fallback");
            boolean expected=minute<420||minute>=1140;
            check(ThemeSchedule.isNight("auto",420,420,minute)==expected, "equal persisted values fallback");
            check(ThemeSchedule.isNight("auto",-1,1500,minute)==expected, "corrupt schedule fallback");
        }
        check(at("2026-10-22T09:59:59Z","America/Sao_Paulo",420,1140),"before 07 local");
        check(!at("2026-10-22T10:00:00Z","America/Sao_Paulo",420,1140),"07 local");
        check(!at("2026-10-22T21:59:59Z","America/Sao_Paulo",420,1140),"before 19 local");
        check(at("2026-10-22T22:00:00Z","America/Sao_Paulo",420,1140),"19 local");
        check(!at("2026-10-22T21:00:00Z","America/Sao_Paulo",420,1140),"timezone before");
        check(at("2026-10-22T21:00:00Z","UTC",420,1140),"timezone after");
        // Skipped and repeated local hours resolve from the actual clock, without a stale timer.
        check(at("2026-03-08T06:59:59Z","America/New_York",150,1140),"before spring jump");
        check(!at("2026-03-08T07:00:00Z","America/New_York",150,1140),"after spring jump");
        check(!at("2026-11-01T05:45:00Z","America/New_York",90,1140),"first repeated hour");
        check(at("2026-11-01T06:15:00Z","America/New_York",90,1140),"clock moves backwards");
        check(!at("2026-11-01T06:45:00Z","America/New_York",90,1140),"second repeated hour");
        System.out.println("ThemeSchedule: " + checks + " checks passed (all selectable schedules, manual modes, timezone and DST changes).");
    }
}
