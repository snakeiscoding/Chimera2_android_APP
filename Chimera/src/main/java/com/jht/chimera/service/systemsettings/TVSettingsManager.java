package com.jht.chimera.service.systemsettings;

import static com.jht.chimera.lib.tv.tuner.IChimeraTuner.TunerOperation.EXPORT_CHANNEL_LIST_TO_TUNER;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.TVControllerFactory;
import com.jht.chimera.lib.tv.switft.jhttvservice.atsc.ATSC_STATUS;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.chimera.lib.tv.tuner.TunerFactory;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.TunerProperties;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.io.File;

/**
 * This class helps us manage the TV settings as we may be importing from a console that has
 * a different TV setting.
 */
class TVSettingsManager {

    private SettingKeys.TVSource originalSource;
    private final File settingsCache;
    private final File settingsDir;
    private final Context context;

    TVSettingsManager(Context context) {
        this.context = context;
        settingsCache = new File(context.getFilesDir(), "settings_cache");
        settingsDir = new File(context.getFilesDir(), "settings");
    }

    void storeSettings() {
        FileUtils.delete(settingsCache);
        //noinspection ResultOfMethodCallIgnored
        settingsCache.mkdirs();
        ExecuteAsRoot.copyFilePreserve(settingsDir.getAbsolutePath() + "/swift", settingsCache.getAbsolutePath());
        ExecuteAsRoot.copyFilePreserve(settingsDir.getAbsolutePath() + "/us7", settingsCache.getAbsolutePath());
        ExecuteAsRoot.copyFilePreserve(settingsDir.getAbsolutePath() + "/tv_settings", settingsCache.getAbsolutePath());
        ExecuteAsRoot.copyFilePreserve(settingsDir.getAbsolutePath() + "/us4", settingsCache.getAbsolutePath());
        originalSource = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));
    }

    void updateTVSettings(@NonNull SystemSettings.IImportSettingsComplete importComplete) {
        SettingKeys.TVSource source = SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING));
        if(source.followsSwiftProtocol() && !sourceMatchesDetected(originalSource, source)) {
            // Need to reset the TV setting to the original.
            DataMatrix.putString(SettingKeys.TV.DATA_TV_SOURCE_STRING, originalSource.toString());
            ExecuteAsRoot.copyFilePreserve(settingsCache.getAbsolutePath() + "/us7", settingsDir.getAbsolutePath());
            ExecuteAsRoot.copyFilePreserve(settingsCache.getAbsolutePath() + "/tv_settings", settingsDir.getAbsolutePath());
            ExecuteAsRoot.copyFilePreserve(settingsCache.getAbsolutePath() + "/swift", settingsDir.getAbsolutePath());
            ExecuteAsRoot.copyFilePreserve(settingsCache.getAbsolutePath() + "/us4", settingsDir.getAbsolutePath());
            importComplete.onImportSettingsComplete();
        }
        else {
            exportSettingsToTuner(source, importComplete);
        }
        FileUtils.delete(settingsCache);
    }

    private boolean sourceMatchesDetected(SettingKeys.TVSource oldSource, SettingKeys.TVSource source) {
        if(source.followsSwiftProtocol()) {
            if (oldSource.equals(SettingKeys.TVSource.US4)) {
                // This means we just powered on the tuner.
                ThreadHelper.sleepIgnoreInterruption(10000);
            }
            JHTDataNotification<TunerProperties> tunerPropertiesNotification = new JHTDataNotification<>();
            IChimeraTuner<? extends IChannel> tuner = TunerFactory.INSTANCE.getTuner(context, SettingKeys.TVSource.US6);
            if(tuner != null) {
                for(int i = 0; i < 5; i++) {
                    tuner.tuner().loadProperties(tunerPropertiesNotification::setData);
                    TunerProperties tunerProperties = tunerPropertiesNotification.waitForData(5100);
                    if (tunerProperties != null && tunerProperties.tunerType() != null) {
                        if (tunerProperties.tunerType() != TunerProperties.TUNER_TYPE.UNKNOWN && source.followsSwiftProtocol()) {
                            SettingKeys.TVSource detectedSource = SettingKeys.TVSource.getSource(tunerProperties.tunerType());
                            DataMatrix.putInt(TVKeys.DATA_TV_TUNER_TYPE_DETECTED_INT, tunerProperties.tunerType().ordinal());
                            SystemPropertyUtil.set("persist.jht.tuner.detect", tunerProperties.tunerType().name());
                            SystemPropertyUtil.set("persist.jht.tuner.version", tunerProperties.version());
                            tuner.destroy();
                            return detectedSource.equals(source);
                        }
                    }
                    ThreadHelper.sleepIgnoreInterruption(4000);
                    tuner.resetSerialPort(false);
                }
                tuner.destroy();
            }
        }
        return true;
    }

    private void exportSettingsToTuner(SettingKeys.TVSource source, @NonNull SystemSettings.IImportSettingsComplete importComplete) {
        if(source.useNewLib()){
            IChimeraTuner<? extends IChannel> tuner = TunerFactory.INSTANCE.getTuner(context, source);
            if(tuner != null) {
                tuner.executeOperation(null, EXPORT_CHANNEL_LIST_TO_TUNER, null, (i, done, o) -> {
                    if (done) {
                        tuner.destroy();
                        importComplete.onImportSettingsComplete();
                    }
                },null);
            }

        } else{
            TVControllerBase controller = TVControllerFactory.getTVController(context, SettingKeys.TVSource.fromName(DataMatrix.getString(SettingKeys.TV.DATA_TV_SOURCE_STRING)));
            controller.open();
            ExecuteAsRoot.runSyncCommand();
            ExecuteAsRoot.fixFilesInPackage("com.jht.chimera");
            controller.importChannels(new TVControllerBase.IOnProgress() {
                @Override
                public void onProgress(int progressPercentage) {
                }

                @Override
                public void onComplete(boolean success, ATSC_STATUS status) {
                    importComplete.onImportSettingsComplete();
                    controller.destroy();
                }
            }, TVControllerBase.IMPORT_CHANNEL_MODE.FLASH_TO_TUNER);
        }
    }
}
