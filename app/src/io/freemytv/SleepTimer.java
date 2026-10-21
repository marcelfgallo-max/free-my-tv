package io.freemytv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * The drawer's sleep timer: in 30, 60 or 90 minutes the TV goes to standby, whatever is on screen. An alarm wakes this
 * receiver (main process), which sends KEYCODE_SLEEP through the TV's own ADB: it only turns the TV off, so a TV that
 * is already off stays off. Not exported.
 */
public class SleepTimer extends BroadcastReceiver {
    static final int[] STEPS = {30, 60, 90};

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("sleep", Context.MODE_PRIVATE);
    }

    /** When the TV goes to standby, or 0 with the timer off. */
    static long endsAt(Context c) {
        long at = prefs(c).getLong("at", 0);
        return at > System.currentTimeMillis() ? at : 0;
    }

    /** Off → 30 → 60 → 90 min → off. Returns the minutes now set (0: off). */
    static int next(Context c) {
        int step = endsAt(c) == 0 ? -1 : prefs(c).getInt("step", -1);
        step = step + 1 < STEPS.length ? step + 1 : -1;
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, 0, new Intent(c, SleepTimer.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (step < 0) {
            alarms.cancel(pi);
            prefs(c).edit().putLong("at", 0).putInt("step", -1).apply();
            return 0;
        }
        long at = System.currentTimeMillis() + STEPS[step] * 60_000L;
        // Android 12+ may not allow exact alarms: then the system's own window (a few minutes at most while in use).
        if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        }
        prefs(c).edit().putLong("at", at).putInt("step", step).apply();
        return STEPS[step];
    }

    /** The drawer tile's name: "Sleep Timer", or how long until the TV turns off. */
    static String label(Context c) {
        long at = endsAt(c);
        if (at == 0) return Ui.tr("Timer", "Sleep Timer");
        long minutes = Math.max(1, (at - System.currentTimeMillis() + 59_999) / 60_000);
        return Ui.tr("Desliga em ", "Off in ", "Apaga en ") + minutes + " min";
    }

    @Override public void onReceive(Context context, Intent intent) {
        PendingResult done = goAsync();
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try (Adb adb = Adb.connectKnown(app)) {
                adb.shell("input keyevent KEYCODE_SLEEP");
                AppLog.w("sleep timer: TV to standby");
            } catch (Exception e) {
                AppLog.w("sleep timer: " + e.getMessage());
            } finally {
                done.finish();
            }
        }, "sleep-timer").start();
    }
}
