package com.jht.chimera.service.hal.lib.hardware.heartrate;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.util.SystemDetection;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.service.hal.lib.hardware.IDriver;
import com.jht.chimera.service.hal.lib.hardware.ParkerDriver;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.enums.HeartRateSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeartRateManager implements IHeartRateReader.IHeartRateUpdated {

    public static final String TAG = "HeartRateManager";

    private final List<IHeartRateReader> readers = new ArrayList<>();

    public static HeartRateManager getInstance() {
        return HeartRateManager.SingletonHolder.INSTANCE;
    }

    @Override
    public void heartRateUpdated() { // also need to call this when source changes, maybe need to split them or rework these managers a little?
        if(!DataMatrix.getBoolean(HALKeys.DATA_LOCK_MACHINE_DATA_FOR_TESTING_BOOL)) {
            IHeartRateReader reader = getActiveReader();
            if (reader == null) {
                DataMatrix.putInt(HALKeys.DATA_HEART_RATE_INT, 0);
                DataMatrix.putInt(HALKeys.DATA_HEART_RATE_SOURCE_INT, HeartRateSource.None.ordinal());
            } else {
                boolean blankState = reader.blankState();
                int heartRate = reader.heartRate();
                if (blankState) {
                    // blinking is current achieve by using -1 as blank and dashes for zero
                    DataMatrix.putInt(HALKeys.DATA_HEART_RATE_INT, -1);
                } else {
                    DataMatrix.putInt(HALKeys.DATA_HEART_RATE_INT, heartRate);
                }
                HeartRateSource source = reader.getSource();
                DataMatrix.putInt(HALKeys.DATA_HEART_RATE_SOURCE_INT, source == null ? HeartRateSource.None.ordinal() : reader.getSource().ordinal());
            }
        }
    }

    private static class SingletonHolder {
        private static final HeartRateManager INSTANCE = new HeartRateManager();
    }

    private HeartRateManager() {
    }

    private final DataMatrix mDataMatrix = new DataMatrix();



    public void start(Context context) {
        //noinspection ConstantConditions
        start(context, new ParkerDriver(), ProductConfig.hasSalutron());
    }

    public void start(Context context, IDriver driver, boolean useSalutron) {
        readers.add(new WatchHeartRateReader(this));
        readers.add(new BLEHeartRateReader2(context, this));

        // Ignore salutron and devices
        if (!SystemDetection.isEmulator()) {
            if (!useSalutron) {
                if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker)
                        || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)
                ) {
                    readers.add(new ParkerPolarHeartRate(driver, this));
                    readers.add(new ParkerHandGripHeartRate(driver, this));
                }
                else if (ProductConfig.hardware.isVortexBase()) {
                    readers.add(new VortexSalutron8600HeartRate(this));
                }

                if(ProductConfig.useCANHeartRate()) {
                    readers.add(new CanHeartRateReader(this));
                }

                DataMatrix.putString(HALKeys.DATA_VERSION_SALUTRON_STRING, "N/A"); //NON-NLS
                DataMatrix.putBoolean(HALKeys.DATA_SALUTRON_CONNECTED_BOOL, false);
            } else {
                readers.add(new SalutronHeartRateReader(context, this));
            }
        }
        readers.add(new DemoHeartRateReader(this));
        // refresh HR reading when stats are reset
        mDataMatrix.createEventAdapter(WorkoutKeys.ACTION_RESET_STATS, v -> heartRateUpdated(), false);
    }


    public void close() {
        mDataMatrix.finish();
        for(IHeartRateReader reader : readers) {
            try {
                reader.close();
            } catch (IOException ignored) {}
        }

    }

    public IHeartRateReader getActiveReader() {
        // give priority to first listed reader reporting a connection
        for (IHeartRateReader reader : readers) {
            if (reader.hasConnection()) {
                return reader;
            }
        }
        // if no active connections reported, just grab first reader with a reading
        for (IHeartRateReader reader : readers) {
            if (reader.heartRate() > 0) {
                return reader;
            }
        }
        return null;
    }

    public int getHeartRate() {
        IHeartRateReader reader = getActiveReader();
        if (reader != null) return reader.heartRate();
        return 0;
    }

    @VisibleForTesting
    public void simulateSalutronHeartRate(int heartRate) {
        for(IHeartRateReader reader : readers) {
            if(reader instanceof SalutronHeartRateReader) {
                ((SalutronHeartRateReader)reader).processSalutronHeartRate(heartRate);
            }
        }
    }

}

