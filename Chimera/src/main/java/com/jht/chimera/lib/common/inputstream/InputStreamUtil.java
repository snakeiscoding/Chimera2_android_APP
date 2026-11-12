package com.jht.chimera.lib.common.inputstream;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * This class contains the InputStream utility functions.
 *
 * @author Andy Rosene
 */
public class InputStreamUtil {
    private static final String TAG = "InputStreamUtil";

    private InputStreamUtil() { /* prevent instantiation attempts */ }

    /**
     * Convert the given {@link InputStream} to a usable temp {@link File} that will be deleted on
     * exit.
     *
     * @param in        the {@link InputStream} to convert.
     * @param fileName  the file name to give the temporary {@link File}.
     * @param extension the file extension to give the temporary {@link File}.
     *
     * @return the temporary {@link File} object or null if something went wrong.
     */
    public static File toTempFile(@NonNull final InputStream in, @NonNull final String fileName, @NonNull final String extension) {
        File tempFile;

        try {
            tempFile = File.createTempFile(fileName, extension);
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                int read;
                byte[] bytes = new byte[1024];

                while ((read = in.read(bytes)) != -1)
                    out.write(bytes, 0, read);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert InputStream to a temp File", e);
            tempFile = null;
        }

        return tempFile;
    }
}
