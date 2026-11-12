package com.jht.chimera.ui.machinesetup;

import android.content.Context;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.viewpager.CustomAnimatedViewPager;
import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.androiduiwidgets.viewpager.StandardPageAdapter;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.LcbToFramelMapper;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesetup.pages.LanguagePage;
import com.jht.chimera.ui.machinesetup.pages.MachineSelectionPage;
import com.jht.chimera.ui.machinesetup.pages.MachineSetupPageBase;
import com.jht.chimera.ui.machinesetup.pages.ModelSelectionPage;
import com.jht.chimera.ui.machinesetup.pages.NetworkSetupPage;
import com.jht.datamatrixlib.DataMatrix;

import java.util.ArrayList;
import java.util.List;

import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

// Note: Ignore the duplicate class found since the duplicate class is removed via gradle sourceSets
// Eventually the main equivalent class should be moved to the chimera flavor location
public class MachineSetup extends RestringActivity {
    private static final String TAG = "MachineSetup";
    private enum PAGES {
        LANGUAGE_SELECT,
        NETWORK,
        MACHINE_SELECTION,
        MODEL_SELECTION
    }

    private CustomAnimatedViewPager pager;
    private MachineSetupPageData pageData = new MachineSetupPageData();

    private List<IPageHolder> pages = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadPage(-1);
    }


    public void loadPage(int selectedIndex) {

        setContentView(R.layout.machine_setup);

        pageData.nextButton = findViewById(R.id.machine_setup_next_button);
        pageData.previousButton = findViewById(R.id.machine_setup_back_button);
        pageData.confirmButton = findViewById(R.id.machine_setup_confirm_button);
        pageData.title = findViewById(R.id.machine_setup_page_title);

        pageData.nextButton.setOnClickListener(v -> {
            hideKeyboard(MachineSetup.this);
            onNextPage();
        });
        pageData.previousButton.setOnClickListener(v -> {
            hideKeyboard(MachineSetup.this);
            goToPreviousPage();
        });
        pageData.confirmButton.setOnClickListener(v -> {
            finishMachineSetup();
        });

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
            if (!isMachineSetup)
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 100);
        } catch (Exception e) {
            Log.w(TAG, e);
        }


    }


    @Override
    public void onResume() {
        super.onResume();
    }

    private void initPages() {
        pages.clear();
        pages.add(new LanguagePage(pageData));
        pages.add(new NetworkSetupPage(pageData));
        pages.add(new MachineSelectionPage(pageData));
        pages.add(new ModelSelectionPage(pageData));
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
                    showNetwork(true);
                    break;
                case NETWORK:
                    showMachine(true);
                    break;
                case MACHINE_SELECTION:
                    showModel(true);
                    break;
                case MODEL_SELECTION:
                    finishMachineSetup();
                    break;
            }

        }
    }

    private void finishMachineSetup() {

        pageData.loadSerialNumbers();
        String suffix = AddressHelper.getMACAddressStr("wlan0", false).toUpperCase();
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        String frameSerial = frameConfig.getFramePrefix() + suffix;
        String consoleSerial = ProductConfig.getConsoleType(frameConfig.machineType).getConsolePrefix() + suffix;
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, consoleSerial);
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, frameSerial);
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, pageData.clubIdEntered);

        hideKeyboard(MachineSetup.this);
        // disable Asset Management
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, false);
        // disable Workout Tracking
        DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, false);

        DataMatrix.putBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, true);
        DataMatrix.sendEvent(HALKeys.EVENT_MACHINE_SETUP_COMPLETE);
        LaunchAppUtil.launchSplashScreen();
    }

    private void goToPreviousPage() {
        pageData.confirmButton.setVisibility(View.GONE);
        pageData.nextButton.setVisibility(View.GONE);
        pageData.previousButton.setVisibility(View.GONE);

        if (pager.getCurrentItem() < PAGES.values().length) {
            switch (PAGES.values()[pager.getCurrentItem()]) {
                case LANGUAGE_SELECT:
                    break;
                case NETWORK:
                    pager.setCurrentItem(PAGES.LANGUAGE_SELECT.ordinal(), pages);
                    break;
                case MACHINE_SELECTION:
                    showNetwork(false);
                    break;
                case MODEL_SELECTION:
                    showMachine(false);
                    break;
            }

        }

    }

    private void showNetwork(boolean forward) {
        /*if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL)) {
            pager.setCurrentItem(PAGES.NETWORK.ordinal(), pages);
        } else if (forward) {
            showMachine(true);
        } else {
            pager.setCurrentItem(PAGES.LANGUAGE_SELECT.ordinal(), pages);
        }*/

        pager.setCurrentItem(PAGES.NETWORK.ordinal(), pages);
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
            new Handler().postDelayed(() -> {
                finishMachineSetup();
            }, 250);
        } else {
            showMachine(false);
        }
    }


}



