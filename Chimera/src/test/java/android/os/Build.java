package android.os;

// TESTING MOCK FOR BUILD PROPERTIES
// Non-final so tests can override values (e.g. to simulate specific hardware) before
// classes that read them (like ProductConfig) are first loaded.

public class Build {
    public static String PRODUCT = "unittest";
    public static String BRAND = "unittest";
    public static String DEVICE = "unittest";
    public static String FINGERPRINT = "unittest";
    public static String HARDWARE = "unittest";
    public static String MODEL = "unittest";
    public static String BOARD = "unittest";
    public static String MANUFACTURER = "unittest";
    public static String SERIAL = "unittest";
    public static String HOST = "unittest";
    public static String TYPE = "unittest";
    public static String TAGS = "unittest";
    public static String USER = "unittest";
    public static String ID = "unittest";
    public static String DISPLAY = "unittest";
    public static String BOOTLOADER = "unittest";
    public static String UNKNOWN = "unknown";
    public static long TIME = 0L;
}
