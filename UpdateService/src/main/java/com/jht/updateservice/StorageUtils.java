package com.jht.updateservice;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class StorageUtils {
    static final String TAG = "StorageUtils";
    static final boolean DEBUG = false;

    public interface DeviceListener {
        void onDeviceFound(String path);
    }

    public static class StorageInfo {

        public final String path;
        public final boolean readonly;
        public final boolean removable;
        public final int number;

        StorageInfo(String path, boolean readonly, boolean removable, int number) {
            this.path = path;
            this.readonly = readonly;
            this.removable = removable;
            this.number = number;
        }

        public String getDisplayName() {
            StringBuilder res = new StringBuilder();
            if (!removable) {
                res.append("Internal SD card"); // NON-NLS
            } else if (number > 1) {
                res.append("SD card " + number); // NON-NLS
            } else {
                res.append("SD card"); // NON-NLS
            }
            if (readonly) {
                res.append(" (Read only)"); // NON-NLS
            }
            return res.toString();
        }
    }

    public static List<StorageInfo> getStorageList() {

        List<StorageInfo> list = new ArrayList<StorageInfo>();
        /*
        String def_path = Environment.getExternalStorageDirectory().getPath();
        boolean def_path_removable = Environment.isExternalStorageRemovable();
        String def_path_state = Environment.getExternalStorageState();
        boolean def_path_available = def_path_state.equals(Environment.MEDIA_MOUNTED)
                || def_path_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean def_path_readonly = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
        */

        Set<String> paths = new LinkedHashSet<String>();
        int cur_removable_number = 1;

        /* Don't add emulated drive
        if (def_path_available) {
            paths.add(def_path);
            list.add(0, new StorageInfo(def_path, def_path_readonly, def_path_removable, def_path_removable ? cur_removable_number++ : -1));
        }
        */

        BufferedReader buf_reader = null;
        try {
            buf_reader = new BufferedReader(new FileReader("/proc/mounts"));
            String line;
            if (DEBUG) Log.d(TAG, "/proc/mounts");
            while ((line = buf_reader.readLine()) != null) {
                if (DEBUG) Log.d(TAG, line);
                if (line.contains("vfat") || line.contains("/mnt")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    String unused = tokens.nextToken(); //device
                    String mount_point = tokens.nextToken(); //mount point
                    if (paths.contains(mount_point)) {
                        continue;
                    }
                    unused = tokens.nextToken(); //file system
                    List<String> flags = Arrays.asList(tokens.nextToken().split(",")); //flags
                    boolean readonly = flags.contains("ro"); // NON-NLS

                    if (line.contains("/dev/block/vold")) {
                        if (!line.contains("/mnt/secure")
                                && !line.contains("/mnt/asec")
                                && !line.contains("/mnt/obb")
                                && !line.contains("/dev/mapper")
                                && !line.contains("tmpfs")) {
                            paths.add(mount_point);
                            list.add(new StorageInfo(mount_point, readonly, true, cur_removable_number++));
                        }
                    }
                }
            }

        } catch (FileNotFoundException ex) {
            Log.w(TAG, ex);
        } catch (IOException ex) {
            Log.w(TAG, ex);
        } finally {
            if (buf_reader != null) {
                try {
                    buf_reader.close();
                } catch (IOException ex) {
                }
            }
        }
        return list;
    }

    public static void checkDevice(final DeviceListener deviceListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int numRetries = 5;
                for (int i = 0; i < numRetries; i++) {
                    List<StorageInfo> storageInfoList = StorageUtils.getStorageList();
                    if (storageInfoList.size() > 0) {
                        String path = storageInfoList.get(0).path;
                        if (deviceListener != null) deviceListener.onDeviceFound(path);
                        return;
                    }
                    SystemClock.sleep(1000);
                }
            }
        }).start();
    }
}
