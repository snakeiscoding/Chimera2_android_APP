package android.os;

// TESTING MOCK FOR elapsedRealtime

public class SystemClock {
	// base represents the zero on time point, so we can compute an approximated uptime
	private static long base = System.nanoTime();
    public static long elapsedRealtime() {
        return (System.nanoTime() - base) / 1_000_000;
    }

    public static long uptimeMillis() {
        // offset so this never returns 0: UptimeTimer.getUptime() treats 0 as "no uptime clock"
        // and falls back to epoch time, which would poison its scheduling
        return (System.nanoTime() - base) / 1_000_000 + 60_000;
    }
}
