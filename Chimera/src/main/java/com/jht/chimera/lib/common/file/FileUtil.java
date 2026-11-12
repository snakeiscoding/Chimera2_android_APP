package com.jht.chimera.lib.common.file;

import android.util.Log;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.string.StringUtil;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Contains the functions needed to calculate different hash algorithms on a given {@link File}.
 *
 * @author Andy Rosene
 */
public class FileUtil {
    private static final String TAG = "FileUtil";

    private FileUtil() { /* prevent instantiation of utility class. */ }

    /**
     * Calculate the MD5 of the given {@link File}.
     *
     * @param file the {@link File} hashing.
     *
     * @return the hash of the given {@link File} or null on error.
     */
    public static String calculateMD5(@Nullable final File file) {
        if (file == null) return null;

        InputStream inputStream = null;
        DigestInputStream digestInputStream = null;

        try {
            @NonNls MessageDigest md = MessageDigest.getInstance("MD5");
            inputStream = new FileInputStream(file);
            digestInputStream = new DigestInputStream(inputStream, md);

            final byte[] buffer = new byte[4098];
            //noinspection StatementWithEmptyBody
            while (digestInputStream.read(buffer) != -1);

            return StringUtil.byteArray2Hex(digestInputStream.getMessageDigest().digest());
        } catch (Exception e) {
            Log.e(TAG, "Failed to calculate the MD5 for: " + file.getAbsolutePath(), e);
            return null;
        } finally {
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (digestInputStream != null) digestInputStream.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Calculate the SHA-1 of the given {@link File}.
     *
     * @param file the {@link File} hashing.
     *
     * @return the hash of the given {@link File} or null on error.
     */
    public static String calculateSHA1(@Nullable final File file) {
        if (file == null) return null;

        InputStream inputStream = null;
        DigestInputStream digestInputStream = null;

        try {
            @NonNls MessageDigest md = MessageDigest.getInstance("SHA-1");
            inputStream = new FileInputStream(file);
            digestInputStream = new DigestInputStream(inputStream, md);

            final byte[] buffer = new byte[4098];
            //noinspection StatementWithEmptyBody
            while (digestInputStream.read(buffer) != -1);

            return StringUtil.byteArray2Hex(digestInputStream.getMessageDigest().digest());
        } catch (Exception e) {
            Log.e(TAG, "Failed to calculate the SHA-1 for: " + file.getAbsolutePath(), e);
            return null;
        } finally {
            try { if (inputStream != null) inputStream.close(); } catch (Exception ignored) {}
            try { if (digestInputStream != null) digestInputStream.close(); } catch (Exception ignored) {}
        }
    }
}
