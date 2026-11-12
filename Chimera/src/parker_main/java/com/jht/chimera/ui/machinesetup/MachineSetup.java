package com.jht.chimera.ui.machinesetup;

import static com.jht.chimera.lib.widget.CommonRoutines.hideKeyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jht.chimera.lib.widget.activity.RestringActivity;
import com.jht.androidcommonalgorithms.network.AddressHelper;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.viewpager.CustomAnimatedViewPager;
import com.jht.androiduiwidgets.viewpager.IPageHolder;
import com.jht.androiduiwidgets.viewpager.StandardPageAdapter;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.SerialNumberHelper;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.workoutapps.AppIcon;
import com.jht.chimera.lib.workoutapps.AppIconManager;
import com.jht.chimera.ui.machinesetup.pages.LanguagePage;
import com.jht.chimera.ui.machinesetup.pages.MachineSerialNumberPage;
import com.jht.chimera.ui.machinesetup.pages.MachineSetupPageBase;
import com.jht.chimera.ui.machinesetup.pages.NetworkSetupPage;
import com.jht.chimera.ui.machinesetup.pages.SecurityPinPage;
import com.jht.chimera.ui.mainapp.goscreen.SecurityPinSetupScreen;
import com.jht.chimera.service.hal.lib.hardware.PowerLimiter;
import com.jht.datamatrixlib.DataMatrix;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: Ignore the duplicate class found since the duplicate class is removed via gradle
 * sourceSets. Eventually the main equivalent class should be moved to the chimera flavor location
 */
public class MachineSetup extends RestringActivity {
    private static final String TAG = "MachineSetup";

    private enum PAGES {
        LANGUAGE_SELECT,
        NETWORK,
        SAFETY_PIN,
        MACHINE_SERIAL
    }

    private CustomAnimatedViewPager pager;
    private MachineSetupPageData pageData = new MachineSetupPageData();

    private List<IPageHolder> pages = new ArrayList<>();
    private AppIconManager appManager = AppIconManager.getInstance();

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
            hideKeyboard(MachineSetup.this);
            validateSerialNumbers();
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
        pages.add(new SecurityPinPage(pageData));
        pages.add(new MachineSerialNumberPage(pageData));
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
                    pager.setCurrentItem(PAGES.SAFETY_PIN.ordinal(), pages);
                    break;
                case SAFETY_PIN:
                    pager.setCurrentItem(PAGES.MACHINE_SERIAL.ordinal(), pages);
                    break;
            }

        }
    }


    private void validateSerialNumbers() {
        FrameConfigEnum frameConfigEnum = SerialNumberHelper.getFrameFromSn(pageData.frameSerialNumberEntered);
        SerialNumberHelper.Error frameStatus = SerialNumberHelper.isFrameSnValid(frameConfigEnum, pageData.frameSerialNumberEntered);
        SerialNumberHelper.Error consoleStatus = SerialNumberHelper.isConsoleSnValid(frameConfigEnum, pageData.consoleSerialNumberEntered);
        Log.d(TAG, "validateSerialNumbers error: console " + consoleStatus + ", frame " + frameStatus);

        // if the serial number doesn't match or serial numbers are too short, show an error
        if (frameStatus.isError() || consoleStatus.isError()) {
            DialogFactory.message(pageData.nextButton, getString(R.string.invalid_serial_number_pg), () -> {
                // do nothing
            }, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            return;
        }

        // update the frame
        FrameConfigManager.getInstance().setFrame(frameConfigEnum);

        // finish the setup
        finishMachineSetup();
    }

    private void finishMachineSetup() {
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, pageData.consoleSerialNumberEntered);
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, pageData.frameSerialNumberEntered);

        if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_POWER_LIMITING_BOOL)) {
            PowerLimiter.getInstance().checkApplyLimits(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_IO_EXTERNAL_POWER_BOOL));
        }
        DataMatrix.putBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL, pageData.useSafetyPin);
        if (pageData.useSafetyPin && pageData.safetyPin != null) {
            DataMatrix.putString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING, pageData.safetyPin);
        }
        SecurityPinSetupScreen.blockPinSetupDialog();

        hideKeyboard(MachineSetup.this);
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
                case MACHINE_SERIAL:
                    pager.setCurrentItem(PAGES.SAFETY_PIN.ordinal(), pages);
                    break;
                case SAFETY_PIN:
                    showNetwork(false);
                    break;
                case NETWORK:
                    pager.setCurrentItem(PAGES.LANGUAGE_SELECT.ordinal(), pages);
                    break;
            }
        }

    }

    private void showNetwork(boolean forward) {
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL) || DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL)) {
            pager.setCurrentItem(PAGES.NETWORK.ordinal(), pages);
        } else if (forward) {
            pager.setCurrentItem(PAGES.MACHINE_SERIAL.ordinal(), pages);
        } else {
            pager.setCurrentItem(PAGES.LANGUAGE_SELECT.ordinal(), pages);
        }
    }

    // TODO test function to determine location
    private void configureDisabledApps() {
        // try to configure if we are in China or not by checking if a list of addresses is reachable
        if (!AddressHelper.isNetworkConnected(ContextUtil.getApplicationContext())) return;

        new Thread(() -> {
            boolean isDapiReachable = isAddressReachable("dms-cbor.jfit.co");
            if (!isDapiReachable) return;

            boolean isYouTubeReachable = isAddressReachable("youtube.com");
            boolean isFacebookReachable = isAddressReachable("facebook.com");
            boolean isXReachable = isAddressReachable("x.com");
            Log.d(TAG, "isYouTubeReachable: " + isYouTubeReachable + " | isFacebookReachable: " +
                    isFacebookReachable + " | isXReachable: " + isXReachable);

            if (!isYouTubeReachable && !isFacebookReachable && !isXReachable) {
                // dapi is reachable but youtube, facebook, and x seems to be blocked
                // so assume this is China

                List<AppIcon> visibleList = appManager.getAppOrder();
                List<AppIcon> hiddenList = appManager.getHiddenApps();

                // TODO we probably should create an APP uuid enum list
                removeAppIconFromVisibleList("0d1043c6-fd2f-11e6-bc64-92361f002671", visibleList, hiddenList); // facebook
                removeAppIconFromVisibleList("0d10456a-fd2f-11e6-bc64-92361f002671", visibleList, hiddenList); // x
                removeAppIconFromVisibleList("0d104a56-fd2f-11e6-bc64-92361f002671", visibleList, hiddenList); // youtube
                removeAppIconFromVisibleList("0d1051fe-fd2f-11e6-bc64-92361f002671", visibleList, hiddenList); // hulu
                removeAppIconFromVisibleList("0d105370-fd2f-11e6-bc64-92361f002671", visibleList, hiddenList); // netflix
                removeAppIconFromVisibleList("f0a84443-3c06-46e4-9a16-40fc2482c296", visibleList, hiddenList); // instagram
                addAppIconToVisibleList("147973a7-d275-4407-a47c-edff2bcfa160", visibleList, hiddenList); // youku
                addAppIconToVisibleList("0b27088f-03ed-4ff2-9b2c-c2cfc9d98e5c", visibleList, hiddenList); // tencent
                addAppIconToVisibleList("a432e2fd-e8aa-451e-a854-a4560f07a553", visibleList, hiddenList); // iqiyi
                addAppIconToVisibleList("8bd233e8-02b1-43cd-ba02-741b8c7d47a5", visibleList, hiddenList); // weibo
                addAppIconToVisibleList("f9390e5e-e5cd-4976-a24b-9f789db69514", visibleList, hiddenList); // netease
                addAppIconToVisibleList("b9779981-0281-43dc-82b6-d097a6cda575", visibleList, hiddenList); // rejoice

                appManager.saveAppOrdering();
            }

        }).start();
    }

    private boolean isAddressReachable(String host) {
        boolean exists = false;

        try {
            InetAddress address = InetAddress.getByName(host);
            Log.d(TAG, "isAddressReachable: " + address);
            SocketAddress sockaddr = new InetSocketAddress(address, 443);
            Socket sock = new Socket();
            int timeoutMs = 2000;
            sock.connect(sockaddr, timeoutMs);
            exists = true;
        } catch (Exception e) {
        }

        return exists;
    }

    private void addAppToList(AppIcon appIcon, List<AppIcon> list) {
        for (AppIcon item : list) {
            if (item.appInfo.uuid.equalsIgnoreCase(appIcon.appInfo.uuid)) return;
        }
        list.add(appIcon);
    }

    private void removeAppFromList(AppIcon appIcon, List<AppIcon> list) {
        for (AppIcon item : list) {
            if (item.appInfo.uuid.equalsIgnoreCase(appIcon.appInfo.uuid)) {
                list.remove(item);
                return;
            }
        }
    }

    private void addAppIconToVisibleList(String uuid, List<AppIcon> visibleList, List<AppIcon> hiddenList) {
        AppIcon appIcon = appManager.getAppIcon(uuid);
        addAppToList(appIcon, visibleList);
        removeAppFromList(appIcon, hiddenList);
    }

    private void removeAppIconFromVisibleList(String uuid, List<AppIcon> visibleList, List<AppIcon> hiddenList) {
        AppIcon appIcon = appManager.getAppIcon(uuid);
        removeAppFromList(appIcon, visibleList);
        addAppToList(appIcon, hiddenList);
    }

}



