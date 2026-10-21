package io.freemytv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which HDMI ports have a device that's on, from HDMI-CEC. Apps can't read CEC (it takes a system permission) and the
 * input states don't tell (on a Sony every port reads "connected"), so this asks `dumpsys hdmi_control` through the
 * TV's own ADB, with the key the TV already accepted: never the "Allow debugging?" prompt. Every 30 s at most, only
 * while the home screen asks, off the UI thread.
 */
final class Cec {
    private Cec() {}

    private static final long EVERY = 30_000;
    /** "logical_address: 0x04 ... power_status: 0 physical_address: 0x1000 port_id: 1" in the list of CEC devices. */
    private static final Pattern DEVICE = Pattern.compile(
            "logical_address: 0x(\\w+) .*?power_status: (-?\\d+) physical_address: 0x\\w+ port_id: (\\d+)");
    /** "[R] ... message=<Report Power Status> 40:90:01": a message a device sent, its source in the first digit. */
    private static final Pattern RECEIVED = Pattern.compile("\\[R\\].*message=<([^>]+)> (\\w)\\w:\\w\\w(?::(\\w\\w))?");
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Bit n: a device on HDMI n is on. */
    private static volatile int ports;
    private static volatile long at;
    private static volatile boolean busy;

    static boolean on(int port) {
        return port > 0 && port < 31 && (ports & (1 << port)) != 0;
    }

    /**
     * Asks again if the last answer is older than 30 s (`now`: right away, as the home screen comes back); `changed`
     * runs on the UI thread when a port turned on or off.
     */
    static void refresh(Context c, boolean now, Runnable changed) {
        long time = System.currentTimeMillis();
        if (busy || (!now && time - at < EVERY && time >= at)) return;
        busy = true;
        at = time;
        Context app = c.getApplicationContext();
        IO.execute(() -> {
            try (Adb adb = Adb.connectKnown(app)) {
                int found = parse(adb.shell("dumpsys hdmi_control"));
                if (found != ports) {
                    ports = found;
                    MAIN.post(changed);
                }
            } catch (Exception e) {
                AppLog.w("cec: " + e.getMessage());
            } finally {
                busy = false;
            }
        });
    }

    /**
     * The devices the TV lists, on (0) or turning on (2), each on its port. The TV asks them only once a minute, so
     * what they said since (the message history, oldest first) wins: standby, no longer the source, or on again.
     * A device the TV doesn't list (a set-top box that answers the same awake or asleep) never counts.
     */
    static int parse(String dump) {
        int devices = dump.indexOf("CEC devices:"), history = dump.indexOf("CEC message history:");
        if (devices < 0) return 0;
        int[] port = new int[16];
        boolean[] on = new boolean[16];
        Matcher m = DEVICE.matcher(dump.substring(devices, history > devices ? history : dump.length()));
        while (m.find()) {
            int address = Integer.parseInt(m.group(1), 16) & 15, power = Integer.parseInt(m.group(2));
            port[address] = Integer.parseInt(m.group(3));
            on[address] = power == 0 || power == 2;
        }
        if (history > 0) {
            Matcher r = RECEIVED.matcher(dump.substring(history));
            while (r.find()) {
                int from = Integer.parseInt(r.group(2), 16);
                switch (r.group(1)) {
                    case "Active Source": on[from] = true; break;
                    case "InActive Source": case "Standby": on[from] = false; break;
                    case "Report Power Status":
                        if (r.group(3) != null) {
                            int power = Integer.parseInt(r.group(3), 16);
                            on[from] = power == 0 || power == 2;
                        }
                        break;
                    default: break;
                }
            }
        }
        int found = 0;
        for (int i = 1; i < 16; i++) if (on[i] && port[i] > 0 && port[i] < 31) found |= 1 << port[i];
        return found;
    }
}
