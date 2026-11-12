package com.jht.chimera.ui.machinesettings;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.jht.chimera.lib.widget.activity.RestringActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.MainAppKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.permission.GrantPermission;
import com.jht.chimera.lib.common.timer.IdleTimerManagement;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.ui.machinesettings.pages.AnalyticsPage;
import com.jht.chimera.ui.machinesettings.pages.Applications;
import com.jht.chimera.ui.machinesettings.pages.Calibration;
import com.jht.chimera.ui.machinesettings.pages.DateTime;
import com.jht.chimera.ui.machinesettings.pages.ErrorLogPage;
import com.jht.chimera.ui.machinesettings.pages.Experimental;
import com.jht.chimera.ui.machinesettings.pages.FactoryOnly;
import com.jht.chimera.ui.machinesettings.pages.General;
import com.jht.chimera.ui.machinesettings.pages.Hardware;
import com.jht.chimera.ui.machinesettings.pages.IO;
import com.jht.chimera.ui.machinesettings.pages.SafetyKey;
import com.jht.chimera.ui.machinesettings.pages.KeyTest;
import com.jht.chimera.ui.machinesettings.pages.NetworkSetup;
import com.jht.chimera.ui.machinesettings.pages.PortTest;
import com.jht.chimera.ui.machinesettings.pages.RunMachine;
import com.jht.chimera.ui.machinesettings.pages.SetupDefault;
import com.jht.chimera.ui.machinesettings.pages.Update;
import com.jht.chimera.ui.machinesettings.pages.Weather;
import com.jht.chimera.ui.machinesettings.pages.Workout;
import com.jht.chimera.ui.machinesettings.viewpager.CustomSettingViewPager;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This houses the Engineering screen activity which is made up of several pages that you can
 * navigate using a side menu.
 */
public class EngineeringScreen extends RestringActivity {

    private DataMatrix dataMatrix = new DataMatrix();

    // Used to hold the page views.
    private List<IPageHolder> pages = new ArrayList<>();

    // The view pager.  We customize it so the user cannot swipe and give it our own animations.
    private CustomSettingViewPager pager;

    private final String SHOW_ADVANCED = "show_advanced";
    private boolean showAdvanced = false;
    private boolean showService = false;
    private boolean showFactory = false;

    public EngineeringScreen() {
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

        int currentPage = 0;
        Intent intent = getIntent();
        if (intent.getBooleanExtra(SHOW_ADVANCED, false) == true) {
            showAdvanced = true;
        } else if (intent.getStringExtra("mode") != null) {
            String mode = intent.getStringExtra("mode");
            if (mode.equalsIgnoreCase("wifi")) {
                currentPage = 5; // wifi index
            } else if (mode.equalsIgnoreCase("service")) {
                showService = true;

            } else if (mode.equalsIgnoreCase("engineering")) {
                showAdvanced = true;
            }
        }

        showFactory = (new File(StorageUtils.getUSBPath(), "jht_factory_only.txt").exists());

        setContentView(R.layout.engineering_screen);
        uiReady(currentPage);

        GrantPermission.grant(this, getPackageName(), Manifest.permission.WRITE_EXTERNAL_STORAGE, getApplicationInfo().uid);
        GrantPermission.grant(this, getPackageName(), Manifest.permission.READ_EXTERNAL_STORAGE, getApplicationInfo().uid);

        JHTTimer viewPressTimer = new JHTTimer(5000, () -> {
            if (showAdvanced) return;
            Intent launchIntent = getIntent().putExtra(SHOW_ADVANCED, true);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // seems to prevent crashing to welcome screen
            startActivity(launchIntent);
            finish();
        });
        findViewById(R.id.machine_settings_title).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                viewPressTimer.start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                viewPressTimer.stop();
            }
            return false;
        });

        updateTitle();
    }

    @Override
    public void onPause() {
        super.onPause();
        DataMatrix.putBoolean(MainAppKeys.DATA_SPLASH_ANALYTICS_BOOL, true);
        dataMatrix.finish();
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
    }

    @Override
    public void onResume() {
        super.onResume();
        DataMatrix.putBoolean(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, true);
        EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTitle();
        uiReady(pager.getCurrentItem());

        new Handler().post(() -> {
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt((SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT)));
            LocaleUtil.changeKeyboard(EngineeringScreen.this, lp);
        });

    }

    void updateTitle() {
        if (showAdvanced) {
            ((TextView) findViewById(R.id.machine_settings_title)).setText(R.string.engineering_mode_tc);
        } else if (showService) {
            ((TextView) findViewById(R.id.machine_settings_title)).setText(R.string.service_mode_tc);
        } else {
            ((TextView) findViewById(R.id.machine_settings_title)).setText(R.string.machine_settings_tc);
        }
    }

    public void uiReady(int currentPage) {

        findViewById(R.id.eng_home).setOnClickListener(v -> {
            pager.setCurrentItem(-1, pages, false);
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
        pageMenu.setAdapter(new StandardSelectorItemAdapter(this, menu, item -> {
            pager.setCurrentItem(item.getAbsolutePosition(), pages, false);
        }, true));

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

        dataMatrix.createIntAdapter(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT, this::initScreenTimeout);
        dataMatrix.createBoolAdapter(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL, value -> initScreenTimeout(DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT)));
        initScreenTimeout(DataMatrix.getInt(SettingKeys.General.DATA_SCREEN_TIMEOUT_SEC_INT));
    }


    private void initScreenTimeout(int timeout) {
        EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
        EngineeringIdleTimerManager.idleTimerManagement.clearTimers();
        if (timeout > 0 && !DataMatrix.getBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL)) {
            EngineeringIdleTimerManager.idleTimerManagement.addTimer(new JHTTimer(timeout * 1000, LaunchAppUtil::launchSplashScreen));
            EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
        }
    }

    private ISelectorItem buildMenu() {

        ArrayList<ISelectorItem> calibrationPages = new ArrayList<>();
        calibrationPages.add(new MenuSelectorItem2(StringLookup.get(R.string.key_test_tc), this));
        if (showAdvanced || showService) calibrationPages.add(new MenuSelectorItem2(StringLookup.get(R.string.run_machine_tc), this));

        // Do not port this in the future this was specific to Vendor tests needed for chimera 3 CGC-23262
        ArrayList<ISelectorItem> hardwarePages = new ArrayList<>();
        if (showAdvanced){
            hardwarePages.add(new MenuSelectorItem2(StringLookup.get(R.string.port_test_tc), this));
            hardwarePages.add(new MenuSelectorItem2("IO", this));
            hardwarePages.add(new MenuSelectorItem2("SafetyKey", this));
        }

        ArrayList<ISelectorItem> topMenu = new ArrayList<>();
        if (showFactory) topMenu.add(new MenuSelectorItem2("FACTORY ONLY", this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.date_time_setup_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.general_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.workout_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.setup_default_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.update_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.network_setup_tc), this));
        //topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.asset_management_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.weather_tc), this));
        //topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.tv_uc), this));

        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.applications_tc), this));
        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.calibration_tc), this, calibrationPages));

        topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.hardware_tc), this, hardwarePages));
        //topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.management_tc), this));
        //if (showAdvanced || showService) topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.service_tc), this));
        if (showAdvanced || showService) topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.error_tc), this));
        if (showAdvanced || showService) topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.analytics_tc), this));
        if (showAdvanced) topMenu.add(new MenuSelectorItem2(StringLookup.get(R.string.advanced_tc), this));

        SelectorItemBase menu = new SelectorItemBase(topMenu);
        menu.setSelection(0);

        return menu;
    }


    private void buildPages() {
        boolean showSimple = !(showAdvanced || showService);
        if (showFactory) pages.add(new FactoryOnly());
        pages.add(new DateTime());
        pages.add(new General(showSimple));
        pages.add(new Workout(showSimple));
        pages.add(new SetupDefault(showSimple));
        pages.add(new Update());
        pages.add(new NetworkSetup());
        //pages.add(new AssetManagement());
        pages.add(new Weather());
        //pages.add(new TV());

        pages.add(new Applications());
        pages.add(new Calibration(showSimple));
        pages.add(new KeyTest());
        if (showAdvanced || showService) pages.add(new RunMachine());
        //pages.add(new IO());
        pages.add(new Hardware(showSimple));
        if (showAdvanced || showService){
            pages.add(new PortTest());
            pages.add(new IO());
            pages.add(new SafetyKey());
        }
        //pages.add(new ManagementPage());
        //if (showAdvanced || showService) pages.add(new Service());
        if (showAdvanced || showService) pages.add(new ErrorLogPage());
        if (showAdvanced || showService) pages.add(new AnalyticsPage());
        if (showAdvanced) pages.add(new Experimental());


    }

}



