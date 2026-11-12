package com.jht.chimera.lib.common.string;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Pattern;

public class StringUtil {

    public static String byteArray2Hex(final byte[] hash) {
        if (hash == null || hash.length == 0) return "";
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b); //NON-NLS
        }
        return formatter.toString();
    }

    public static String byteArray2Hex(String hash) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hash.length(); i+=2) {
            String str = hash.substring(i, i+2);
            output.append((char)Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    private static final int MIN_DISPLAYED_HEART_RATE = 30;
    private static final int DETECTED_HEART_RATE = -1;

    public static String formatHeartRate(int value) {
        if (value <= DETECTED_HEART_RATE) return "";
        if (value < MIN_DISPLAYED_HEART_RATE) return "--";
        return String.valueOf(value);
    }

    /** Quote a string in the JSON/Java style.
     * Intended for debugging purposes when you want something that *looks like* a quoted string */
    public static String quote(String value) {
        return value == null ? "null" : "\"" + value // NON-NLS
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    public static String capitalize(Locale locale, String string) {
        if(string.length() == 0) {
            return "";
        }
        else if(string.length() == 1) {
            return string.toUpperCase(locale);
        }
        return string.substring(0, 1).toUpperCase(locale) + string.substring(1);
    }

    public static @NonNull String getString(@Nullable Context context, int stringId, @NonNull String defaultString) {
        return context == null ? defaultString : context.getString(stringId);
    }

    public static boolean isUUID(String value) {
        return Pattern.compile("([A-F0-9]{8}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{4}-[A-F0-9]{12})").matcher(value.toUpperCase(Locale.ENGLISH)).matches();
    }

    public static boolean isHexadecimal(String s) {
        return s.matches("^[0-9A-Fa-f]+$");
    }

    private static final Pattern IPV4_ADDRESS = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");
    public static boolean isValidIPv4(String value) {
        return IPV4_ADDRESS.matcher(value).matches();
    }

    public static String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toTitleCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
