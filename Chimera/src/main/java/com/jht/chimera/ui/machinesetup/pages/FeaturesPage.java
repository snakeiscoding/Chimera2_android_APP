package com.jht.chimera.ui.machinesetup.pages;

import static com.jht.chimera.lib.common.context.ContextUtil.getString;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.updatemanager.IUpdateCallback;
import com.jht.updatemanager.UpdateManagerClient;
import com.jht.updatemanager.UpdateManagerTypes;

import java.util.List;

import updatepackages.UpdatePackageList;

public class FeaturesPage extends MachineSetupPageBase {
    public final String TAG = "FeaturesPage";
    private CheckBox checkBoxAM;
    private CheckBox checkBoxWT;
    private CheckBox checkBoxEGym;

    private UpdateManagerClient mUpdateClientUSB;
    private Handler handler = new Handler();
    SimpleMessageDialog checkUpdateDlg;
    private static final String UPDATE_USB_ID = "update_usb_id"; //NON-NLS
    private Button checkForUSBUpdatesButton;
    private Button installUSBUpdatesButton;
    private TextView usbUpdateInfo;
    UpdatePackageList updatePackageList;

    public FeaturesPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.features_page, null);
        container.addView(view);

        checkBoxAM = view.findViewById(R.id.machine_setup_enable_am);
        checkBoxAM.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL));
        checkBoxAM.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_ASSET_MANAGEMENT_BOOL, isChecked));

        checkBoxWT = view.findViewById(R.id.machine_setup_enable_wt);
        checkBoxWT.setChecked(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL));
        checkBoxWT.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.AssetManagement.DATA_USE_WORKOUT_TRACKING_BOOL, isChecked));

        checkBoxEGym = view.findViewById(R.id.machine_setup_enable_egym);
        checkBoxEGym.setChecked(DataMatrix.getBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL));
        checkBoxEGym.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Experimental.DATA_EGYM_ENABLED_BOOL, isChecked));

        usbUpdateInfo = view.findViewById(R.id.update_details); // text to show usb update versions
        installUSBUpdatesButton = view.findViewById(R.id.install_usb_update);
        checkForUSBUpdatesButton = view.findViewById(R.id.check_for_usb_update);

        checkForUSBUpdatesButton.setOnClickListener(v -> { // checks for an available update from usb
            checkForUSBUpdatesButton.setEnabled(false);
            handler.post(() -> {
                checkUpdateDlg = DialogFactory.message(page(), getContext().getString(R.string.checking_for_updates_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.NONE);
                boolean forceDowngrade = DataMatrix.getBoolean(SettingKeys.Experimental.DATA_FORCE_DOWNGRADE_UPDATES_BOOL);
                mUpdateClientUSB.checkUpdateUsb(UPDATE_USB_ID, forceDowngrade); // this call goes into the updateManager artifact, the callback is in the onCreate
                checkForUSBUpdatesButton.setEnabled(true);
            });
        });
        installUSBUpdatesButton.setOnClickListener(v -> { // installs the usb update if one is found after the check
            mUpdateClientUSB.applyUpdate(UPDATE_USB_ID);
        });

        if (mUpdateClientUSB == null) {
            mUpdateClientUSB = new UpdateManagerClient(getContext(), new IUpdateCallback() {
                @Override
                public void applyUpdatesCallback(int returnCode, String id, Intent intent) {
                }

                // callback for checkUsbForUpdates()
                @Override
                public void checkUpdateCallback(int returnCode, String id, Intent intent) {

                    if (!isShown()) {
                        return;
                    }

                    handler.post(() -> {
                        if (id.equals(UPDATE_USB_ID)) {
                            switch (returnCode) {
                                case UpdateManagerTypes.CHECK_UPDATE_NEW_UPDATE:
                                    installUSBUpdatesButton.setEnabled(true);
                                    checkUpdateDlg.init(StringLookup.get(R.string.update_new_found_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    // populate the update info with the info received from the usb update intent
                                    List<String> newUpdates = intent.getStringArrayListExtra(UpdateManagerTypes.DATA_NEW_UPDATE_STRINGS);
                                    if (newUpdates != null) {
                                        StringBuilder updateString = new StringBuilder(getString(R.string.available_updates_tc));
                                        for (String updatePackageString : newUpdates) {
                                            updateString.append("\n").append(updatePackageString);
                                        }
                                        usbUpdateInfo.setText(updateString.toString());
                                    }
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATE_FOUND:
                                    checkUpdateDlg.init(StringLookup.get(R.string.could_not_find_update_files_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NO_UPDATES:
                                    checkUpdateDlg.init(StringLookup.get(R.string.no_updates_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_NOT_ENOUGH_STORAGE:
                                    checkUpdateDlg.init(StringLookup.get(R.string.not_enough_storage_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                                case UpdateManagerTypes.CHECK_UPDATE_ERROR:
                                default:
                                    Log.e(TAG, "Error when getting update: " + returnCode);
                                    checkUpdateDlg.init(StringLookup.get(R.string.error_occurred_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                                    break;
                            }
                        }
                        checkForUSBUpdatesButton.setEnabled(true);
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

        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().title.setText(R.string.features_tc);
        checkBoxAM.setText(R.string.asset_management_tc);
        checkBoxWT.setText(R.string.workout_tracking_tc);
        installUSBUpdatesButton.setEnabled(false);
        usbUpdateInfo.setText("");
        pageData().nextButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
    }


    @Override
    public boolean onNext() {
        return true;
    }
}
