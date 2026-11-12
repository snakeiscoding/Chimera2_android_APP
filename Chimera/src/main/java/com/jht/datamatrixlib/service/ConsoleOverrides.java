package com.jht.datamatrixlib.service;

import android.os.Handler;
import android.util.Log;

import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.BTSinkKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ClubComKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.FeaturedWorkoutKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.PTPKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SSOKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.Sprint8Keys;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TiersKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UsbMediaDeviceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.VaKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.setting.DMSettings;
import com.jht.datamatrixservice.IGlobalSettings;

import java.util.LinkedList;
import java.util.List;


/**
 * This class contains all of the custom console overrides used by Chimera. This is included using
 * reflection.
 *
 * @author Andy Rosene
 */
@SuppressWarnings("unused")
public class ConsoleOverrides {
    private static final String TAG = "ConsoleOverrides";

    private static List<DataDefault> sAllDefaultValuesList = new LinkedList<>();
    private static Handler mHandler = new Handler();

    /**
     * Load all of the console settings and the initial console values.
     */
    public static void loadConsoleSettings(DMSettings mDMSettings, IGlobalSettings.Stub mInterface) {
        Log.e(TAG, "This is actually running.");

        PersistedDataConverter.convertPersistedDataInSharedPreferences(ContextUtil.getApplicationContext());

        mDMSettings.loadSettingsFromList(SettingKeys.sDataDefaultList);
        mDMSettings.loadSettingsFromList(BTSinkKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(ClubComKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(ConsoleConfigKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(DAPIKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(FeaturedWorkoutKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(HALKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(ImportExportKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(LoggerKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(MainAppKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(PTPKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(Sprint8Keys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(SSOKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(TiersKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(TopBarKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(TVKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(UiServiceKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(UsbMediaDeviceKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(VaKeys.sDefaultValuesList);
        mDMSettings.loadSettingsFromList(WorkoutKeys.sDefaultValuesList);
    }
}
