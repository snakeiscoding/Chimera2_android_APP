package com.jht.chimera.lib.common;

import androidx.annotation.Nullable;

public class ObjectUtil {

    public static boolean isNullOrEmpty(@Nullable String s) {
        return (s == null || s.isEmpty());
    }

    // because "Call requires API level 19 (current min is 18): java.util.Objects#equals"
    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        return a == b || (a != null && b != null && a.equals(b));
    }

    // this will treat null and empty strings as equal
    public static boolean equalsIgnoreEmptyString(@Nullable Object a, @Nullable Object b) {
        if (a instanceof String && ((String) a).isEmpty()) a = null;
        if (b instanceof String && ((String) b).isEmpty()) b = null;
        return equals(a, b);
    }

    /** Equivalent to Objects.hashCode from Java 8 / Android API level 19 */
    public static int hashCode(@Nullable Object obj) {
        return obj == null ? 0 : obj.hashCode();
    }

    /**
     * Equivalent to Objects.hash, or hash() called on a List of the given values.
     * Warning: When a single object reference is supplied, the returned value does not equal the hash code of that object reference. This value can be computed by calling hashCode(Object).
     */
    public static int hash(Object... values) {
        int hashCode = 1;
        for (Object o : values) {
            hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
        }
        return hashCode;
    }

    public static String nullableToString(@Nullable Object a) {
        return (a == null ? "" : a.toString());
    }

    public static <T> T safeCast(Object o, Class<T> clazz) {
        return clazz != null && clazz.isInstance(o) ? clazz.cast(o) : null; // https://stackoverflow.com/a/44680634/1329160
    }

    /**
     * Constrains a value to be within a specified range [min, max].
     *
     * If the value is less than the minimum, the minimum is returned.
     * If the value is greater than the maximum, the maximum is returned.
     * Otherwise, the original value is returned.
     *
     */
    public static float coerce(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
