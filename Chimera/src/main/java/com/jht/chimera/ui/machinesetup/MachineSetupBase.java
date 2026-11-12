package com.jht.chimera.ui.machinesetup;

import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.viewpager.CustomAnimatedViewPager;
import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.androiduiwidgets.viewpager.StandardPageAdapter;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemBrightness;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.LcbToFramelMapper;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.ui.machinesetup.pages.CalibrationPage;
import com.jht.chimera.ui.machinesetup.pages.FeaturesPage;
import com.jht.chimera.ui.machinesetup.pages.LanguagePage;
import com.jht.chimera.ui.machinesetup.pages.MachineInfoConfirmPage;
import com.jht.chimera.ui.machinesetup.pages.MachineInfoPage;
import com.jht.chimera.ui.machinesetup.pages.MachineSelectionPage;
import com.jht.chimera.ui.machinesetup.pages.MachineSetupPageBase;
import com.jht.chimera.ui.machinesetup.pages.ModelSelectionPage;
import com.jht.chimera.ui.machinesetup.pages.NetworkSetupPage;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.firsttimesetup.BarcodeScannerHelper;
import com.jht.firsttimesetup.ConsoleSerialHandler;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared implementation of the first time machine setup wizard.
 *
 * Each product flavor supplies a thin {@code MachineSetup} subclass, because the activity is
 * registered in main/AndroidManifest.xml by that name and a java source set cannot override a
 * class in main. Flavor differences belong in here behind a ProductConfig or BuildConfig check,
 * not in a forked copy of this file.
 */
public abstract class MachineSetupBase extends RestringActivity {
    @NonNls private static final String TAG = "MachineSetup";
    private enum PAGES {
        LANGUAGE_SELECT,
        FEATURES,
        NETWORK,
        MACHINE_SELECTION,
        MODEL_SELECTION,
        MACHINE_INFO,
        CONFIRM_MACHINE_INFO,
        CALIBRATION,
    }

    private CustomAnimatedViewPager pager;
    private final MachineSetupPageData pageData = new MachineSetupPageData();

    private final List<IPageHolder> pages = new ArrayList<>();

    private BarcodeScannerHelper scanner;

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (scanner != null) scanner.dispatchKeyEvent(event);
        return super.dispatchKeyEvent(event);
    }

    public void setScannerEnabled(boolean enabled) {
        if (scanner != null) scanner.setEnabled(enabled);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.machine_setup);
        loadPage();

        scanner = new BarcodeScannerHelper(
                this,
                new ConsoleSerialHandler() {
                    @Override
                    public void updateSerial(@NotNull String serial) {
                        DataMatrix.updateString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, serial);
                    }

                    @Override
                    public @NotNull String getSerial() {
                        return DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideKeyboard(MachineSetupBase.this);
    }
    @Override
    public void onResume() {
        super.onResume();
        DataMatrix.putBoolean(DAPIKeys.DATA_LOCK_CARDIO_INFO_BOOL, true);
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.FIRST_TIME_SETUP.name());
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard(MachineSetupBase.this);
    }

    @Override
    public void onStop() {
        super.onStop();
        hideKeyboard(MachineSetupBase.this);
    }

    public void loadPage() {
        pageData.nextButton = findViewById(R.id.machine_setup_next_button);
        pageData.previousButton = findViewById(R.id.machine_setup_back_button);
        pageData.confirmButton = findViewById(R.id.machine_setup_confirm_button);
        pageData.title = findViewById(R.id.machine_setup_page_title);

        pageData.nextButton.setOnClickListener(v -> {
            hideKeyboard(MachineSetupBase.this);
            onNextPage();
        });
        pageData.previousButton.setOnClickListener(v -> {
            hideKeyboard(MachineSetupBase.this);
            goToPreviousPage();
        });
        pageData.confirmButton.setOnClickListener(v -> validateSerialNumbers());

        // Initialize the on screen widgets.
        pager = findViewById(R.id.machine_setup_pages);

        initPages();

        pager.setCurrentItem(0, pages);

        // start the wifi scan right away so that the wifi setup can show the list faster
        try {
            ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).startScan();
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        // reset the brightness to the max
        try {
            boolean isMachineSetup = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL);
            if (!isMachineSetup) {
                SystemBrightness.setSystemBrightness(getContentResolver(), 100);
                DataMatrix.putInt(SettingKeys.Hardware.DATA_BACKLIGHT_PROGRESS_INT, 100);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }


    private void initPages() {
        pages.clear();
        pages.add(new LanguagePage(pageData));
        pages.add(new FeaturesPage(pageData));
        pages.add(new NetworkSetupPage(pageData));
        pages.add(new MachineSelectionPage(pageData));
        pages.add(new ModelSelectionPage(pageData));
        pages.add(new MachineInfoPage(pageData));
        pages.add(new MachineInfoConfirmPage(pageData));
        pages.add(new CalibrationPage(pageData));
        pager.setAdapter(new StandardPageAdapter(pages));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MachineSetupPageBase page = (MachineSetupPageBase) pages.get(pager.getCurrentItem());
        page.updateText(getApplicationContext());
    }

    private void onNextPage() {
        MachineSetupPageBase page = (MachineSetupPageBase) pages.get(pager.getCurrentItem());
        if (!page.onNext()) {
            pageData.confirmButton.setEnabled(false);
            pageData.nextButton.setEnabled(false);
            pageData.previousButton.setEnabled(false);
        } else {
            goToNextPage();
        }
    }

    private void goToNextPage() {
        pageData.confirmButton.setVisibility(View.GONE);
        pageData.nextButton.setVisibility(View.GONE);
        pageData.previousButton.setVisibility(View.GONE);
        pageData.confirmButton.setEnabled(true);
        pageData.nextButton.setEnabled(true);
        pageData.previousButton.setEnabled(true);

        if (pager.getCurrentItem() < PAGES.values().length) {
            switch (PAGES.values()[pager.getCurrentItem()]) {
                case LANGUAGE_SELECT:
                    pager.setCurrentItem(PAGES.FEATURES.ordinal(), pages);
                    setScannerEnabled(false); // disable the barcode scanner after language screen
                    break;
                case FEATURES:
                    showNetwork(true);
                    break;
                case NETWORK:
                    showMachine(true);
                    break;
                case MACHINE_SELECTION:
                    showModel(true);
                    break;
                case MODEL_SELECTION:
                    gotoCalibrationOrInfo();
                    break;
                case CALIBRATION:
                    confirmSkipCalibration(()-> pager.setCurrentItem(PAGES.MACHINE_INFO.ordinal(), pages));
                    break;
                case MACHINE_INFO:
                    confirmClub(()-> {
                        if (pageData.consoleSerialNumberEntered.compareTo(pageData.consoleSerialNumberConfirm) == 0 &&
                                pageData.frameSerialNumberEntered.compareTo(pageData.frameSerialNumberConfirm) == 0 &&
                                pageData.clubIdEntered == (pageData.clubIdConfirm)) {
                            ((MachineSetupPageBase) pages.get(PAGES.CONFIRM_MACHINE_INFO.ordinal())).onNext();
                            validateSerialNumbers();
                        } else {
                            pager.setCurrentItem(PAGES.CONFIRM_MACHINE_INFO.ordinal(), pages);
                        }
                    });
                    break;
                case CONFIRM_MACHINE_INFO:
                    validateSerialNumbers();
                    break;
            }

        }
    }

    private void finishMachineSetup() {
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(pageData.consoleSerialNumberEntered));
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(pageData.frameSerialNumberEntered));
        if (BuildConfig.HAS_BRIDGE_SN) {
            // Not run through SerialNumberUtil: bridge serials were never sanitized on ProjectOne
            // and nothing else in the codebase sanitizes them either.
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING, pageData.bridgeSerialNumberEntered);
        }
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, pageData.clubIdEntered);
        DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_FLOORS_FLOAT, 0);
        DataMatrix.putFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT, 0);
        DataMatrix.putInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT, 0);

        hideKeyboard(MachineSetupBase.this);

        // give some time for above key update callback to trigger before the machine setup flag is set
        new Handler().postDelayed(() -> {
            DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true);
            DataMatrix.sendEvent(HALKeys.EVENT_MACHINE_SETUP_COMPLETE);
            LaunchAppUtil.launchSplashScreen();
        }, 250);
    }

    private void hideOverlay(View view) {
        view.clearAnimation();
        view.setVisibility(View.GONE);
        findViewById(R.id.machine_setup_overlay).setVisibility(View.GONE);
    }

    private boolean gotoCalibrationOrInfo() {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        if (frameConfig.hasIncline()) {
            pageData.wasCalibrationDone = false; // calibration is available but not done
            pager.setCurrentItem(PAGES.CALIBRATION.ordinal(), pages);
            return true;
        } else {
            pageData.wasCalibrationDone = null; // calibration is not available
            pager.setCurrentItem(PAGES.MACHINE_INFO.ordinal(), pages);
            return false;
        }
    }

    private void validateSerialNumbers() {

        if(!AddressHelper.isNetworkConnected(getApplicationContext())) {
            finishMachineSetup();
            return;
        }
        Handler handler = new Handler();
        DataMatrix dataMatrix = new DataMatrix();
        View view = findViewById(R.id.machine_settings_busy);
        JHTTimer timer = new JHTTimer(30000, () -> {
            handler.post(() -> hideOverlay(view));
            dataMatrix.finish();
            finishMachineSetup();
        });

        view.setVisibility(View.VISIBLE);
        findViewById(R.id.machine_setup_overlay).setVisibility(View.VISIBLE);
        view.startAnimation(AnimationUtils.loadAnimation(this, R.anim.spinning_clockwise));
        dataMatrix.createEventAdapter(DAPIKeys.EVENT_SETTINGS_VALID, s -> handler.post( () -> {
            timer.stop();
            handler.post(() -> hideOverlay(view));
            dataMatrix.finish();
            finishMachineSetup();
        }));
        dataMatrix.createEventAdapter(DAPIKeys.EVENT_SETTINGS_INVALID, s -> handler.post( () -> {
            handler.post(() -> hideOverlay(view));
            timer.stop();
            if (BuildConfig.HAS_BRIDGE_SN) {
                DialogFactory.confirm(pageData.nextButton, getString(R.string.invalid_identification_3_pg), this::finishMachineSetup, this::showPreviousNextButtons);
            } else {
                DialogFactory.confirm(pageData.nextButton, getString(R.string.invalid_identification_pg), this::finishMachineSetup, this::showPreviousNextButtons);
            }
            dataMatrix.finish();
        }));
        DataMatrix.putString(DAPIKeys.ACTION_VALIDATE_SETTINGS_STRING, pageData.frameSerialNumberEntered + "," + pageData.consoleSerialNumberEntered + "," + pageData.clubIdEntered);

        timer.start();

    }

    private void goToPreviousPage() {
        pageData.confirmButton.setVisibility(View.GONE);
        pageData.nextButton.setVisibility(View.GONE);
        pageData.previousButton.setVisibility(View.GONE);

        if (pager.getCurrentItem() < PAGES.values().length) {
            switch (PAGES.values()[pager.getCurrentItem()]) {
                case LANGUAGE_SELECT:
                    break;
                case FEATURES:
                    pager.setCurrentItem(PAGES.LANGUAGE_SELECT.ordinal(), pages);
                    setScannerEnabled(true); // enable the barcode scanner on language screen
                    break;
                case NETWORK:
                    pager.setCurrentItem(PAGES.FEATURES.ordinal(), pages);
                    break;
                case MACHINE_SELECTION:
                    showNetwork(false);
                    break;
                case MODEL_SELECTION:
                    showMachine(false);
                    break;
                case CALIBRATION: // technically backing out of info goes back to model then you can move forward to calibration
                case MACHINE_INFO:
                    showModel(false);
                    break;
                case CONFIRM_MACHINE_INFO:
                    pager.setCurrentItem(PAGES.MACHINE_INFO.ordinal(), pages);
                    break;
            }

        }

    }

    private void showNetwork(boolean forward) {
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL)) {
            pager.setCurrentItem(PAGES.NETWORK.ordinal(), pages);
        } else if (forward) {
            showMachine(true);
        } else {
            pager.setCurrentItem(PAGES.FEATURES.ordinal(), pages);
        }
    }

    private void showMachine(boolean forward) {
        int lcb = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) ? 0 : DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        List<JHTCommonTypes.MachineType> availableMachines = LcbToFramelMapper.getAvailableMachines(lcb);
        if (availableMachines.size() > 1) {
            pager.setCurrentItem(PAGES.MACHINE_SELECTION.ordinal(), pages);
        } else if (forward) {
            pageData.machineTypeSelection = availableMachines.get(0);
            showModel(true);
        } else {
            showNetwork(false);
        }

    }

    private void showModel(boolean forward) {
        int lcb = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) ? 0 : DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT);
        List<FrameConfigEnum> availableMachines = LcbToFramelMapper.getAvailableModels(pageData.machineTypeSelection, lcb);
        if (availableMachines.size() > 1) {
            pager.setCurrentItem(PAGES.MODEL_SELECTION.ordinal(), pages);
        } else if (forward) {
            FrameConfigManager.getInstance().setFrame(availableMachines.get(0));

            gotoCalibrationOrInfo(); //pager.setCurrentItem(PAGES.MACHINE_INFO.ordinal(), pages);
        } else {
            showMachine(false);
        }
    }

    private void confirmClub(Runnable nextAction) {
        if (pageData.clubIdEntered == 60) {
            DialogFactory.confirm(pageData.nextButton, getString(R.string.confirm_club_60_pg), nextAction::run, this::showPreviousNextButtons);
        } else {
            nextAction.run();
        }
    }

    private void confirmSkipCalibration(Runnable nextAction) {
        if (pageData.wasCalibrationDone != null && !pageData.wasCalibrationDone) {
            DialogFactory.confirm(pageData.nextButton, getString(R.string.skip_setup_uc), nextAction::run, this::showPreviousNextButtons);
        } else {
            nextAction.run();
        }
    }

    private void showPreviousNextButtons() {
        pageData.nextButton.setVisibility(View.VISIBLE);
        pageData.previousButton.setVisibility(View.VISIBLE);
    }
}



