package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.androiduiwidgets.selector.StandardSelectorItemAdapter;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.dapi.DapiGetSoftware;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.update.UpdateValues;
import com.jht.chimera.service.doore.DooreApi;
import com.jht.chimera.service.doore.NightlyBuildsWorker;
import com.jht.chimera.ui.machinesettings.adapters.update.UpdatePackageSelectorItem;
import com.jht.chimera.ui.machinesettings.adapters.update.UpdateSelectorItem;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.updatemanager.IUpdateCallback;
import com.jht.updatemanager.UpdateManagerClient;
import com.jht.updatemanager.UpdateManagerTypes;
import com.jhtp.dapiservice.DapiResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import dapi.IOtaUpdateInterface;
import kotlin.Unit;
import updatepackages.UpdatePackage;
import updatepackages.UpdatePackageList;
import updatepackages.UpdateXmlGenerator;
import updatepackages.Version;

/**
 * Implements the update page gluing the page to the model layer.
 */
public class Update extends SettingPageBase {

    public final String TAG = "Update";
    private static final String UPDATE_USB_ID = "update_usb_id"; //NON-NLS
    private static final String UPDATE_OTA_ID = "update_rscu_id"; //NON-NLS
    public static final String SYSTEM_OTA_PATH = "/sdcard/Download"; //NON-NLS
    private static final String APPS_UPDATE_ID = "63a74e6c-8380-4416-963a-2ee9d2357cc6";
    //private static final String CHIMERA1_OS_PACKAGE_NAME = "OS K2_2"; //NON-NLS
    //private static final String CHIMERA2_OS_PACKAGE_NAME = "OS2"; //NON-NLS

    private final DataMatrix dataMatrix = new DataMatrix();
    private UpdateManagerClient mUpdateClientUSB;
    private UpdateManagerClient mUpdateClientOTA;

    private ProgressBar checkUpdatesSpinnerOta;
    private ProgressBar checkUpdatesSpinnerUsb;
    private ProgressBar nightlyUpdatesSpinner;
    UpdatePackageList updatePackageList;

    private RecyclerView updatePackages;
    private RecyclerView availableUpdates;
    private Handler handler = new Handler();
    private Button checkForUsbUpdatesButton;
    private Button checkForOtaUpdatesButton;
    private View view;

    // ---------------------------------------------------------------------------------------------
    // Nightly Builds Dropdown
    private OptionSelectorItem nightlyBuildsDropdown;
    private TextView nightlyUpdatesLabel;
    private String nightlyUrl;
    private String nightlyVersion;
    private LinearLayout coverNightlyBox;
    Thread nightlyApiThread;

    /**
     * Track whether nightly was clicked or if we're checking for updates by another means.
     *
     * If true, we load manifest from DOORE based on URL and version.
     * If false, we let the normal update process happen, whether from USB or CORE.
     */
    private boolean nightlyWasClicked = false;
    // ---------------------------------------------------------------------------------------------

    private boolean isOtaUpdate = false;
    /** Last latest-software DAPI result. Null until DAPI replies. */
    private volatile DapiResult lastSoftwareDapi;
    /** True when the updater app is one of the packages that will be installed. */
    private boolean installsUpdater;
    SimpleMessageDialog checkUpdateDlg;

    public Update() {
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.update, null);
        container.addView(view);

        if (mUpdateClientUSB == null) {
            mUpdateClientUSB = new UpdateManagerClient(getContext(), new IUpdateCallback() {
                @Override
                public void applyUpdatesCallback(int returnCode, String id, Intent intent) {
                }

                @Override
                public void checkUpdateCallback(int returnCode, String id, Intent intent) {
                    if (!isShown()) {
                        return;
                    }
                    final List<String> newUpdates = intent == null ? null : intent.getStringArrayListExtra(UpdateManagerTypes.DATA_NEW_UPDATE_STRINGS);
                    handler.post(() -> {
                        checkUpdatesSpinnerUsb.setVisibility(View.GONE);
                        if (id.equals(UPDATE_USB_ID)) {
                            switch (returnCode) {
                                case UpdateManagerTypes.CHECK_UPDATE_NEW_UPDATE:
                                case UpdateManagerTypes.CHECK_UPDATE_DOWNLOADING_UPDATE:
                                case UpdateManagerTypes.CHECK_UPDATE_READY_TO_INSTALL:
                                case UpdateManagerTypes.CHECK_UPDATE_ALREADY_UPDATING:
                                    if (!isOtaUpdate) {
                                        File manifest = getUsbManifest();
                                        if (manifest.exists()) {
                                            isOtaUpdate = true;

                                            // Reads update manifest, load packages' information, and re-enables the Update buttons
                                            loadManifest(manifest, newUpdates);
                                        } else {
                                            DialogFactory.message(view, StringLookup.get(R.string.no_updates_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                        }
                                    }
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATE_FOUND:
                                case UpdateManagerTypes.CHECK_UPDATE_NO_OTA_FILES:
                                    checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.could_not_find_update_files_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATES:
                                    checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.no_updates_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NOT_ENOUGH_STORAGE:
                                    checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.not_enough_storage_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_ERROR:
                                default:
                                    showError(UpdateCheckErrors.usbUpdateErrorChecks(returnCode, intent != null,
                                            manifestEquipment(getUsbManifest()), BuildConfig.UPDATE_EQUIPMENT));
                                    break;
                            }
                        }
                        setButtonsEnabled(true);
                    });
                }

                @Override
                public void downloadedUpdateFilesCallback(int returnCode, String id, Intent intent) {
                }

                @Override
                public void checkStatusCallback(int i, String s) {
                }
            });
        }

        if (mUpdateClientOTA == null) {
            mUpdateClientOTA = new UpdateManagerClient(getContext(), new IUpdateCallback() {

                @Override
                public void applyUpdatesCallback(int returnCode, String id, Intent intent) {
                }

                @Override
                public void checkUpdateCallback(int returnCode, String id, Intent intent) {
                    if (!isShown()) {
                        return;
                    }
                    final List<String> newUpdates = intent == null ? null : intent.getStringArrayListExtra(UpdateManagerTypes.DATA_NEW_UPDATE_STRINGS);
                    handler.post(() -> {
                        checkUpdatesSpinnerOta.setVisibility(View.GONE);
                        nightlyUpdatesSpinner.setVisibility(View.GONE);
                        if (id.equals(UPDATE_OTA_ID)) {
                            switch (returnCode) {
                                case UpdateManagerTypes.CHECK_UPDATE_NEW_UPDATE:
                                case UpdateManagerTypes.CHECK_UPDATE_DOWNLOADING_UPDATE:
                                case UpdateManagerTypes.CHECK_UPDATE_READY_TO_INSTALL:
                                case UpdateManagerTypes.CHECK_UPDATE_ALREADY_UPDATING:
                                    File manifest = new File(SYSTEM_OTA_PATH + "/update.xml");
                                    if (manifest.exists()) {
                                        isOtaUpdate = true;

                                        // Reads update manifest, load packages' information, and re-enables the Update buttons
                                        loadManifest(manifest, newUpdates);
                                    } else {
                                        DialogFactory.message(view, StringLookup.get(R.string.no_updates_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    }
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATE_FOUND:
                                case UpdateManagerTypes.CHECK_UPDATE_NO_OTA_FILES:
                                    UpdateCheckResult serialError = UpdateCheckErrors.serialNumberCheck(DapiResult.fromInt(DataMatrix.getInt(DAPIKeys.DATA_CARDIO_INFO_DAPI_RESULT_INT)));
                                    if (serialError != null) {
                                        showError(serialError);
                                    } else {
                                        checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.could_not_find_update_files_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    }
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATES:
                                    checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.no_updates_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NOT_ENOUGH_STORAGE:
                                    checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.not_enough_storage_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_ERROR:
                                default:
                                    showError(UpdateCheckErrors.otaUpdateErrorChecks(returnCode, intent != null, lastSoftwareDapi,
                                            manifestEquipment(new File(SYSTEM_OTA_PATH + "/update.xml")), BuildConfig.UPDATE_EQUIPMENT));
                                    break;
                            }
                        }
                        setButtonsEnabled(true);
                    });
                }

                @Override
                public void downloadedUpdateFilesCallback(int returnCode, String id, Intent intent) {
                }

                @Override
                public void checkStatusCallback(int i, String s) {
                }
            });
            mUpdateClientOTA.initOtaUpdates(mOtaUpdateInterface);
        }

        return view;
    }

    /**
     * Reads update manifest, load packages' information, and re-enables the Update buttons (Update From USB and Check for Updates).
     *
     * @param manifest  file being loaded
     * @param newUpdates    list of updates
     */
    private void loadManifest(final File manifest, final List<String> newUpdates) {
        updatePackageList = null;
        new Thread(() -> {
            UpdatePackageList updatePackages = UpdateXmlGenerator.readUpdateFile(manifest);
            if (updatePackages != null && updatePackages.getEquipmentType().equals(BuildConfig.UPDATE_EQUIPMENT)) {
                updatePackageList = updatePackages;
            } else {
                // Not applicable; no update!
                return;
            }

            handler.post(() -> {
                SelectorDefaultAnimations.setDefaultAnimations(getContext(), availableUpdates);
                SelectorItemBase updates = new SelectorItemBase(new ArrayList<>());
                Version manifestVersion = updatePackageList.getVersion();
                String version = manifestVersion.getVersionString();
                if(nightlyWasClicked) {
                    version = nightlyVersion;
                }
                updates.addItem(new UpdateSelectorItem(getContext(), version, onApplyUpdate));
                StandardSelectorItemAdapter adapter = new StandardSelectorItemAdapter(getContext(), updates, iSelectorItem -> {
                    // do nothing
                });
                updates.setSelection(0);
                selectUpdate(newUpdates);
                availableUpdates.setAdapter(adapter);
                setButtonsEnabled(true);
            });
        }).start();
    }

    private void selectUpdate(List<String> newUpdates) {
        updatePackages.setVisibility(View.VISIBLE);
        SelectorDefaultAnimations.setDefaultAnimations(getContext(), updatePackages);
        SelectorItemBase updatePackageBase = new SelectorItemBase(new ArrayList<>());
        List<UpdatePackage> packageList = updatePackageList.getUpdatePackages();
        installsUpdater = false;
        for (UpdatePackage updatePackage : packageList) {
            /* is this needed? breaks parker which uses same names, is listing both an issue? if so maybe we can pass the list through update service to filter? or add a "hw" attribute in the manifest?
            if (updatePackage.name.contains(CHIMERA1_OS_PACKAGE_NAME)) {
                if (!ProductConfig.hardware.equals(ProductConfig.Hardware.Chimera)) {
                    continue;
                }
            } else if (updatePackage.name.contains(CHIMERA2_OS_PACKAGE_NAME)) {
                if (!ProductConfig.hardware.equals(ProductConfig.Hardware.Chimera_2)) {
                    continue;
                }
            }*/
            // check if we need to skip update since it is not new
            boolean isOldPackage = true;
            if (newUpdates != null) {
                for (String newUpdate : newUpdates) {
                    if (newUpdate.contains(updatePackage.name)) {
                        isOldPackage = false;
                        break;
                    }
                }
            }

            if (!isOldPackage && updatePackage.isSoftwareUpdaterPackage()) {
                installsUpdater = true;
            }

            // could do asterisk for to-be-installed, or maybe show the installSize in parathesis?
            updatePackageBase.addItem(new UpdatePackageSelectorItem(updatePackage.name + " - " + updatePackage.packageVersion + (isOldPackage ? "" : " ***"))); //NON-NLS
        }
        StandardSelectorItemAdapter adapter = new StandardSelectorItemAdapter(getContext(), updatePackageBase, null);
        updatePackages.setAdapter(adapter);
    }

    private final UpdateSelectorItem.IOnApplyUpdate onApplyUpdate = updateIndex -> {
        Runnable apply = isOtaUpdate
                ? () -> mUpdateClientOTA.applyUpdate(UPDATE_OTA_ID)
                : () -> mUpdateClientUSB.applyUpdate(UPDATE_USB_ID);
        boolean isPersistent = Settings.Secure.getInt(getContext().getContentResolver(), "jht_disable_persistent", 0) == 0; //NON-NLS
        if (!isPersistent && installsUpdater) {
            // Warn user that persistent is off and update will fail.
            // Note that only devs should be turning off persistent process. This is a developer warning so no need for translation.
            DialogFactory.confirm(page(), "Persistent process is off. Updating the updater will fail because this will kill the update service process. Continue?", //NON-NLS
                    apply::run, () -> { });
        } else {
            apply.run();
        }

    };

    private final IOtaUpdateInterface mOtaUpdateInterface = new IOtaUpdateInterface() {

        @Override
        public String getSystemSoftwareType() {
            Log.d("NightlyUpdate", "getSystemSoftwareType, value: " + UpdateValues.getOtaSwType());
            return UpdateValues.getOtaSwType();
        }

        @Override
        public void getLatestSoftware(String softwareType) {
            if (nightlyWasClicked) {
                Log.d("NightlyUpdate", "getLatestSoftware called with nightly enabled. nightlyVersion: " + nightlyVersion + ", nightlyUrl: " + nightlyUrl);
                lastSoftwareDapi = DapiResult.Success; // nightly skips DAPI
                mUpdateClientOTA.nightlySoftwareResponseVersion(softwareType, nightlyVersion, nightlyUrl);
            } else {
                Log.d("NightlyUpdate", "getLatestSoftware called with nightly disabled");
                regularUpdateProcess(softwareType);
            }
        }

        @Override
        public File getOtaUpdateXmlFileDir() {
            return new File(SYSTEM_OTA_PATH);
        }
    };

    /** Latest software based on software type
     *
     * @param softwareType  type of software an update is being checked for
     */
    private void regularUpdateProcess(@NonNull final String softwareType) {
        DapiGetSoftware.getLatestSoftware(softwareType, response -> {
            int artifactResult;
            DapiResult result = DapiResult.fromInt(response.dapiResponse);
            lastSoftwareDapi = result;
            switch (result) {
                case Success:
                    artifactResult = UpdateManagerClient.DAPI_RESPONSE_SUCCESS;
                    break;
                case NoSoftwareFound:
                    artifactResult = UpdateManagerClient.DAPI_RESPONSE_NO_SOFTWARE_FOUND;
                    break;
                case GeneralError:
                default:
                    artifactResult = UpdateManagerClient.DAPI_RESPONSE_FAILURE;
                    break;
            }
            mUpdateClientOTA.getLatestSoftwareVersionResponse(artifactResult, response.softwareType,
                    response.version, response.url, response.md5);
        });
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        checkUpdatesSpinnerOta = view.findViewById(R.id.check_updates_spinner_ota);
        checkUpdatesSpinnerUsb = view.findViewById(R.id.check_updates_spinner_usb);
        updatePackages = view.findViewById(R.id.eng_update_packages);
        availableUpdates = view.findViewById(R.id.eng_available_updates);

        CheckBox autoUpdateCheckBox = view.findViewById(R.id.eng_auto_update_time_enabled);
        autoUpdateCheckBox.setChecked(DataMatrix.getBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL));
        autoUpdateCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            nightlyWasClicked = false;
            DataMatrix.putBoolean(SettingKeys.Update.DATA_AUTO_UPDATE_BOOL, isChecked);
        });

        UserTextInput autoUpdateTimeInput = view.findViewById(R.id.eng_auto_update_time);
        int time = DataMatrix.getInt(SettingKeys.Update.DATA_AUTO_UPDATE_HOUR_INT);
        String timeString = time + ":00 - " + time + ":59";
        Calendar updateTime = Calendar.getInstance();
        updateTime.set(Calendar.HOUR_OF_DAY, time);
        updateTime.set(Calendar.MINUTE, 0);
        updateTime.set(Calendar.SECOND, 0);
        autoUpdateTimeInput.setText(updateTime);
        autoUpdateTimeInput.setText(timeString);
        autoUpdateTimeInput.setOnTimePicked((view, hourOfDay, minute) -> {
            nightlyWasClicked = false;
            DataMatrix.putInt(SettingKeys.Update.DATA_AUTO_UPDATE_HOUR_INT, hourOfDay);
            String timeString2 = hourOfDay + ":00 - " + hourOfDay + ":59";
            autoUpdateTimeInput.setText(timeString2);
        });

        checkForUsbUpdatesButton = view.findViewById(R.id.eng_update_from_usb);
        checkForUsbUpdatesButton.setOnClickListener(v -> {
            // Set to false so that we look for latest
            nightlyWasClicked = false;

            UpdateCheckResult updaterError = UpdateCheckErrors.updaterInstalledCheck(getContext().getPackageManager());
            if (updaterError != null) {
                showError(updaterError);
                return;
            }
            UpdateCheckResult usbError = UpdateCheckErrors.usbDriveCheck(Arrays.asList(
                    new File(StorageUtils.getUdiskPath()), new File(StorageUtils.getUdisk2Path())));
            if (usbError != null) {
                showError(usbError);
                return;
            }
            checkUpdatesSpinnerUsb.setVisibility(View.VISIBLE);
            setButtonsEnabled(false);
            isOtaUpdate = false;
            boolean forceDowngrade = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL);
            mUpdateClientUSB.checkUpdateUsb(UPDATE_USB_ID, forceDowngrade);
        });

        checkForOtaUpdatesButton = view.findViewById(R.id.eng_update_from_ota);
        checkForOtaUpdatesButton.setOnClickListener(v -> {
            // Set to false so that we look for latest
            nightlyWasClicked = false;

            UpdateCheckResult networkError = UpdateCheckErrors.networkCheck(DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL));
            if (networkError != null) {
                showError(networkError);
                return;
            }
            UpdateCheckResult updaterError = UpdateCheckErrors.updaterInstalledCheck(getContext().getPackageManager());
            if (updaterError != null) {
                showError(updaterError);
                return;
            }
            lastSoftwareDapi = null;
            checkUpdatesSpinnerOta.setVisibility(View.VISIBLE);
            setButtonsEnabled(false);
            isOtaUpdate = true;
            boolean forceDowngrade = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL);

            //Grabs xml, verifies version and MD5
            mUpdateClientOTA.checkUpdateOta(UPDATE_OTA_ID, forceDowngrade);
        });

        // Nightly Updates
        nightlyUpdatesLabel = view.findViewById(R.id.eng_nightly_updates_label);
        nightlyBuildsDropdown = view.findViewById(R.id.eng_nightly_updates_dropdown);
        nightlyUpdatesSpinner = view.findViewById(R.id.eng_nightly_updates_spinner);
        coverNightlyBox = view.findViewById(R.id.eng_disable_nightly_layout);

        Log.d("NightlyUpdate", "DATA_ENABLE_NIGHTLY_UPDATES_BOOL: " + DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL));
        if (DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL)) {
            nightlyUpdatesLabel.setVisibility(View.VISIBLE);
            nightlyUpdatesSpinner.setVisibility(View.VISIBLE);
            nightlyBuildsDropdown.setVisibility(View.VISIBLE);
            // Check for internet connection
            if (!DataMatrix.getBoolean(TopBarKeys.DATA_NETWORK_CONNECTION_BOOL)) {
                nightlyBuildsDropdown.setVisibility(View.GONE);
                Log.d("NightlyUpdate", "hide dropdown");
                checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(R.string.no_network_pg), null,
                        SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                return;
            }
            refreshNightlyUpdates();
            nightlyBuildsDropdown.setOnOptionSelected((pos, version) -> {
                // Set to true so that we use URL and version instead of latest
                nightlyWasClicked = true;
                Log.d("NightlyUpdate", "nightlyWasClicked true");
                nightlyDropdownOptionSelector(pos, version);
            });
        } else {
            nightlyUpdatesLabel.setVisibility(View.GONE);
            nightlyBuildsDropdown.setVisibility(View.GONE);
            coverNightlyBox.setVisibility(View.GONE);
            nightlyUpdatesSpinner.setVisibility(View.GONE);
        }

        setButtonsEnabled(true);
    }

    /** Set "Check for USB Updates", "Check for Updates" (CORE), and the Nightly Builds
     *  {@link #nightlyBuildsDropdown} to enabled or disabled
     * @param enabled   true to set buttons to enabled, false to make them disabled/not clickable
     */
    private void setButtonsEnabled(boolean enabled) {
        if (checkForUsbUpdatesButton != null) {
            checkForUsbUpdatesButton.setEnabled(enabled);
        }
        if (checkForOtaUpdatesButton != null) {
            checkForOtaUpdatesButton.setEnabled(enabled);
        }
        if ((nightlyBuildsDropdown != null) && DataMatrix.getBoolean(SettingKeys.Experimental.DATA_ENABLE_NIGHTLY_UPDATES_BOOL)) {
            nightlyBuildsDropdown.closePopup();
            // make a box to cover it
            if (enabled) {
                coverNightlyBox.setVisibility(View.GONE);
            } else {
                coverNightlyBox.setVisibility(View.VISIBLE);
            }
        }
    }

    /** Null when the manifest cannot be read. */
    private static String manifestEquipment(File manifest) {
        UpdatePackageList list = UpdateXmlGenerator.readUpdateFile(manifest);
        return list == null ? null : list.getEquipmentType();
    }

    /** Shows only the error text. The detail goes to the log. */
    private void showError(UpdateCheckResult result) {
        Log.e(TAG, "Update check failed: " + result.getError() + " " + result.getDetail());
        checkUpdateDlg = DialogFactory.message(page(), StringLookup.get(result.getError().getMessage()), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
    }

    private File getUsbManifest() {
        File manifest = new File(StorageUtils.getUdisk2Path() + "/update.xml");
        File manifest2 = new File(StorageUtils.getUdiskPath() + "/update.xml");
        if (manifest.exists()) {
            return manifest;
        } else if (manifest2.exists()) {
            return manifest2;
        } else {
            return manifest;
        }
    }

    /** Save URL created and version selected from dropdown so that we can download the manifest later on.
     * @param pos       Slot in dropdown options
     * @param version   SW version selected
     */
    private void nightlyDropdownOptionSelector(int pos, String version) {
        Log.d("NightlyUpdate", "nightlyDropdownOptionSelector: " + pos + ", version: " + version);
        if (nightlyBuildsDropdown.getOptions() == null) {
            nightlyUpdatesSpinner.setVisibility(View.VISIBLE);
            nightlyBuildsDropdown.setEnabled(false);
            Log.w("NightlyUpdate", "No nightly options loaded yet, need to wait and try again!");
        } else {
            setButtonsEnabled(true);
            nightlyBuildsDropdown.setEnabled(true);
            if (BuildConfig.FLAVOR_platform.equals("lcgui")) {
                // API Gateway URL:
                nightlyUrl = "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/lowcostgui/manifest/" + nightlyBuildsDropdown.getOptions()[pos]
                        + "/update_ota.xml";
            } else {
                nightlyUrl = "https://7b2r1ruh94.execute-api.us-east-2.amazonaws.com/main/" + mOtaUpdateInterface.getSystemSoftwareType()
                        + "/manifest/" + nightlyBuildsDropdown.getOptions()[pos] + "/update_ota.xml";
            }

            // Saving the version here so that we can use the existing getLatestSoftware call later.
            nightlyVersion = nightlyBuildsDropdown.getOptions()[pos];
            Log.d("NightlyUpdate", "nightlyVersion: " + nightlyVersion);
        }

        checkForUsbUpdatesButton.setEnabled(false);
        checkForOtaUpdatesButton.setEnabled(false);

        boolean forceDowngrade = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL);
        Log.d("NightlyUpdate", "nightlyVersion: force is " + forceDowngrade);
        lastSoftwareDapi = null;
        mUpdateClientOTA.checkUpdateOta(UPDATE_OTA_ID, forceDowngrade);
        nightlyUpdatesSpinner.setVisibility(View.VISIBLE);
    }

    /** Create thread and make {@link DooreApi} call using {@link NightlyBuildsWorker thread} so
     * that we can see existing options for the given softwareType. */
    private void refreshNightlyUpdates() {
        Log.d("NightlyUpdate", "refreshNightlyUpdates");
        DooreApi dooreAPI = new DooreApi();
        String softwareType = mOtaUpdateInterface.getSystemSoftwareType();
        NightlyBuildsWorker nightlyBuildsWorker = new NightlyBuildsWorker(
            dooreAPI,
            softwareType,
            (options) -> {
                handler.post(() -> onNightlyUpdatesRefreshed(options));
                return Unit.INSTANCE;
            }
        );

        nightlyApiThread = new Thread(nightlyBuildsWorker);
        nightlyApiThread.start();
    }

    /** When a response is received from the {@link DooreApi} call, we parse it into a new array
     * so that the dropdown options can be populated.*/
    private void onNightlyUpdatesRefreshed(List<String> options) {
        Log.d("NightlyUpdate", "onNightlyUpdatesRefreshed: " + options);
        if (options != null) {
            nightlyUpdatesSpinner.setVisibility(View.GONE);
            String[] optionsArray = new String[options.size()];
            optionsArray = options.toArray(optionsArray);
            nightlyBuildsDropdown.setOptions(new OptionManager(optionsArray));
        } else {
            nightlyUpdatesSpinner.setVisibility(View.VISIBLE);
        }
    }

    /** Stops {@link #nightlyApiThread} so that we don't run into memory leak issues when we exit
     *  the Update tab or MES as a whole.*/
    private void stopNightlyApiThread() {
        if (nightlyApiThread != null && nightlyApiThread.isAlive()) {
            nightlyApiThread.interrupt();
            nightlyApiThread = null;
        }
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        stopNightlyApiThread();
        dataMatrix.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNightlyApiThread();
        dataMatrix.finish();
    }
}