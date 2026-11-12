package com.jht.chimera.ui.bluetooth;

import android.content.Context;

import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Gets the required scanners for bluetooth scanning
 */
public class BluetoothScanners {
    private static final boolean USES_HEADPHONE_SCANNER = BuildConfig.HEADPHONES_SCANNER;
    private static final boolean USES_HEART_RATE_SCANNER = BuildConfig.HEART_RATE_SCANNER;
    private static final boolean USES_MEDIA_PLAYER_SCANNER = BuildConfig.HAS_BT_SINK && BuildConfig.MEDIA_PLAYER_SCANNER;
    private static final boolean USES_SALUTRON_SCANNER = ProductConfig.hasSalutron();

    private BluetoothScanners() {}

    private static class SingletonHolder {
        private static final BluetoothScanners INSTANCE = new BluetoothScanners();
    }

    public static BluetoothScanners getInstance() {
        return BluetoothScanners.SingletonHolder.INSTANCE;
    }

    /**
     * Gets the correct scanners based on the current console configuration
     *
     * @param context              The context
     * @param addTestScanner       Whether to add the test scanner
     * @param showMediaDeviceFirst Whether to display the media device first in the list
     * @return The list of scanners in display order
     */
    public List<IBluetoothScanner> getDisplayedScanners(Context context, boolean addTestScanner, boolean showMediaDeviceFirst) {
        List<IBluetoothScanner> availableScannerList = new ArrayList<>();

        if (addTestScanner) {
            availableScannerList.add(new TestScanner());
        }

        SalutronHeartRateScanner salutronDeviceScanner = new SalutronHeartRateScanner(context);

        if (USES_HEADPHONE_SCANNER) availableScannerList.add(new HeadphoneScanner(context));

        if (USES_SALUTRON_SCANNER) {
            availableScannerList.add(salutronDeviceScanner);
        } else if (USES_HEART_RATE_SCANNER) {
            availableScannerList.add(new HeartRateScanner(context));
        }

        MediaDeviceScanner mediaDeviceScanner = null;
        if (USES_MEDIA_PLAYER_SCANNER) mediaDeviceScanner = new MediaDeviceScanner();

        if (mediaDeviceScanner != null) {
            if (showMediaDeviceFirst) {
                availableScannerList.add(0, mediaDeviceScanner);
            } else {
                availableScannerList.add(mediaDeviceScanner);
            }
        }

        if (DataMatrix.getBoolean(SettingKeys.Hardware.DATA_FTMS_ADVERTISING_ENABLED_BOOL)) {
            availableScannerList.add(new FTMSDeviceScanner());
        }

        return availableScannerList;
    }

    /**
     * Gets the correct scanners based on the current console configuration
     *
     * @param context The context
     * @return The list of scanners for background scanning
     */
    public List<IBluetoothScanner> getBackgroundScanners(Context context) {
        List<IBluetoothScanner> availableScannerList = new ArrayList<>();

        if (USES_SALUTRON_SCANNER) {
            availableScannerList.add(new SalutronHeartRateScanner(context));
        } else if (USES_HEART_RATE_SCANNER) {
            availableScannerList.add(new HeartRateScanner(context));
        }

        if (USES_HEADPHONE_SCANNER) availableScannerList.add(new HeadphoneScanner(context));

        return availableScannerList;
    }
}
