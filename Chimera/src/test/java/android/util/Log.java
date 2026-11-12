package android.util;

// TESTING MOCK FOR LOG

public class Log {
    public static int v(String tag, String msg) {
        System.out.println("🔍 Log.v> " + tag + ": " + msg);
        return 0;
    }

    public static int d(String tag, String msg) {
        System.out.println("🐛 Log.d> " + tag + ": " + msg);
        return 0;
    }

    public static int d(String tag, String msg, Throwable tr) {
        System.out.println("🐛 Log.d> " + tag + ": " + msg + '\n' + getStackTraceString(tr));
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("ℹ️ Log.i> " + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg, Throwable tr) {
        System.out.println("ℹ️ Log.i> " + tag + ": " + msg + '\n' + getStackTraceString(tr));
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("⚠️ Log.w> " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg, Throwable tr) {
        System.out.println("⚠️ Log.w> " + tag + ": " + msg + '\n' + getStackTraceString(tr));
        return 0;
    }

    public static int e(String tag, String msg) {
        System.out.println("❌ Log.e> " + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg, Throwable tr) {
        System.out.println("❌ Log.e> " + tag + ": " + msg + '\n' + getStackTraceString(tr));
        return 0;
    }

    // Helper method to convert a stack trace to a String
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        // This adds the stack trace to the log message
        StringBuilder sb = new StringBuilder();
        sb.append(tr).append('\n');
        for (StackTraceElement element : tr.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append('\n');
        }
        return sb.toString();
    }

}
