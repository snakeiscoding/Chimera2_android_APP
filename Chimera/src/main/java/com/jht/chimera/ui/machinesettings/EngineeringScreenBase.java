package com.jht.chimera.ui.machinesettings;


import android.Manifest;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.ui.machinesettings.pages.AnalyticsPage;
import com.jht.chimera.ui.machinesettings.pages.Applications;
import com.jht.chimera.ui.machinesettings.pages.AssetManagement;
import com.jht.chimera.ui.machinesettings.pages.Calibration;
import com.jht.chimera.ui.machinesettings.pages.DateTime;
import com.jht.chimera.ui.machinesettings.pages.ErrorLogPage;
import com.jht.chimera.ui.machinesettings.pages.Experimental;
import com.jht.chimera.ui.machinesettings.pages.General;
import com.jht.chimera.ui.machinesettings.pages.Hardware;
import com.jht.chimera.ui.machinesettings.pages.IO;
import com.jht.chimera.ui.machinesettings.pages.KeyTest;
import com.jht.chimera.ui.machinesettings.pages.ManagementPage;
import com.jht.chimera.ui.machinesettings.pages.NetworkSetup;
import com.jht.chimera.ui.machinesettings.pages.RunMachine;
import com.jht.chimera.ui.machinesettings.pages.Service;
import com.jht.chimera.ui.machinesettings.pages.SetupDefault;
import com.jht.chimera.ui.machinesettings.pages.TV;
import com.jht.chimera.ui.machinesettings.pages.Update;
import com.jht.chimera.ui.machinesettings.pages.Weather;
import com.jht.chimera.ui.machinesettings.pages.Workout;
import com.jht.chimera.ui.machinesettings.viewpager.CustomSettingViewPager;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.gattserver2.WatchState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This houses the Engineering screen activity which is made up of several pages that you can navigate
 * using a side menu.
 *
 * Each product flavor supplies a thin {@code EngineeringScreen} subclass, because the activity is
 * registered in main/AndroidManifest.xml by that name and a java source set cannot override a class
 * in main. Flavor differences belong in here behind a ProductConfig or BuildConfig check, not in a
 * forked copy of this file.
 */

public abstract class EngineeringScreenBase extends RestringActivity {

    private final DataMatrix dataMatrix = new DataMatrix();

    // Used to hold the page views.
    private final List<IPageHolder> pages = new ArrayList<>();

    // The view pager.  We customize it so the user cannot swipe and give it our own animations.
    private CustomSettingViewPager pager;

    public EngineeringScreenBase() {
        EngineeringIdleTimerManager.idleTimerManagement = new IdleTimerManagement();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG && false /* set this to true to run */) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.engineering_screen);
        uiReady(0);

        GrantPermission.grant(this, getPackageName(), Manifest.permission.WRITE_EXTERNAL_STORAGE, getApplicationInfo().uid);
        GrantPermission.grant(this, getPackageName(), Manifest.permission.READ_EXTERNAL_STORAGE, getApplicationInfo().uid);
    }

    @Override
    public void onPause() {
        super.onPause();
        dataMatrix.finish();
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_WATCHES_BOOL, DataMatrix.getInt(HALKeys.DATA_SAMSUNG_WATCH_CONNECTION_STATUS_INT, WatchState.Disconnected.ordinal()) == WatchState.Disconnected.ordinal() && DataMatrix.getInt(HALKeys.DATA_GYM_KIT_STATUS_INT) == HALKeys.STATUS_GYM_KIT_DISCONNECTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, true);
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.MES.name());

        DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, false);
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_WATCHES_BOOL, false);
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ((TextView)findViewById(R.id.machine_settings_title)).setText(R.string.machine_settings_tc);
        uiReady(pager.getCurrentItem());

        new Handler().post(() -> {
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt((SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT)));
            LocaleUtil.changeKeyboard(EngineeringScreenBase.this, lp);
        });

    }

    public void uiReady(int currentPage) {

        findViewById(R.id.eng_home).setOnClickListener(v -> {
            pager.setCurrentItem(-1, pages, false);
            // checks to see if changes were made, and if so sends console config to core
            DataMatrix.sendEvent(DAPIKeys.ACTION_UPLOAD_CONSOLE_CONFIG);
            LaunchAppUtil.launchSplashScreen();
        });

        // Initialize the on screen widgets.
        pager = findViewById(R.id.engineering_pages);

        // Build the page objects.
        buildPages();

        // Create the menu
        RecyclerView pageMenu = findViewById(R.id.engineering_page_selector);
        SelectorDefaultAnimations.setDefaultAnimations(this, pageMenu);

        // Initialize the menu adapter.
        ISelectorItem menu = buildMenu();
        pageMenu.setAdapter(new StandardSelectorItemAdapter(this, menu, item -> pager.setCurrentItem(item.getAbsolutePosition(), pages, false), true));

        // Initialize the page adapter.
        int index = 0;
        for(int i = 0; i < menu.numChildren(); i++) {
            if(index >= currentPage) {
                menu.setSelection(i);
                break;
            }
            index += menu.getChild(i).countAllChildren() + 1;
        }
        pager.setCurrentItem(menu.getChild(currentPage).getIndex(), pages, true);

        EngineeringIdleTimerManager.idleTimerManagement.clearTimers();
    }


    private ISelectorItem buildMenu() {

        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        ArrayList<ISelectorItem> calibrationPages = new ArrayList<>();
        if (!frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            calibrationPages.add(new MenuSelectorItem2(StringLookup.get(R.string.key_test_tc), this));
            calibrationPages.add(new MenuSelectorItem2(StringLookup.get(R.string.run_machine_tc), this));
        }

        ArrayList<ISelectorItem> topMenu = new ArrayList<>();
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.date_time_setup_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.general_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.workout_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.setup_default_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.update_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.network_setup_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.asset_management_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.weather_tc), this));
        if (!frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.tv_uc), this));
        }
        // topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.tv_channel_setup_tc), this));

        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.applications_tc), this));
        if (frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.calibration_tc), this));
        }
        else {
            topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.calibration_tc), this, calibrationPages));
        }

        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.hardware_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.management_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.service_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.error_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.analytics_tc), this));

        ArrayList<ISelectorItem> experimentalPages = new ArrayList<>();
        experimentalPages.add(new MenuSelectorItem2("IO",this)); //NON-NLS
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.experimental_features_tc), this,experimentalPages));

        if (ProductConfig.isProjectOne()) {
            topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.engineering_debug_tc), this));
        }

        SelectorItemBase menu = new SelectorItemBase(topMenu);
        menu.setSelection(0);

        return menu;
    }


    private void buildPages() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        pages.add(new DateTime());
        pages.add(new General());
        pages.add(new Workout());
        pages.add(new SetupDefault());
        pages.add(new Update());
        pages.add(new NetworkSetup());
        pages.add(new AssetManagement());
        pages.add(new Weather());
        if (!frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            pages.add(new TV());
        }

        pages.add(new Applications());
        pages.add(new Calibration());
        if (!frameConfig.machineClassType.equals(JHTCommonTypes.MachineClassificationType.SpinBike)) {
            pages.add(new KeyTest());
            pages.add(new RunMachine());
        }
        pages.add(new Hardware());
        pages.add(new ManagementPage());
        pages.add(new Service());
        pages.add(new ErrorLogPage());
        pages.add(new AnalyticsPage());
        pages.add(new Experimental());
        pages.add(new IO());
        if (ProductConfig.isProjectOne()) {
            // Must stay last and must match the buildMenu() condition above: the pager index is the menu index.
            pages.add(new EngineeringDebug());
        }
    }

}



