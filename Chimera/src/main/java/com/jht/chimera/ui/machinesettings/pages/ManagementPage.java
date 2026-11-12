package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.service.systemsettings.SystemSettings;
import com.jht.chimera.ui.machinesettings.EngineeringIdleTimerManager;
import com.jht.chimera.ui.machinesettings.adapters.management.ConsoleItem;
import com.jht.chimera.ui.machinesettings.adapters.management.ManagementAdapter;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * Implements the management page gluing the page to the model layer.
 */
public class ManagementPage extends SettingPageBase {

    private final ArrayList<ConsoleItem> siteSurveyConsoles = new ArrayList<>();
    private ManagementAdapter mAdapter;
    private final Handler handler;
    private final DataMatrix mDataMatrix = new DataMatrix();


    private boolean consoleEntryExists(String newConsoleName) {

        for (ConsoleItem item : siteSurveyConsoles) {
            if (item.getText().equalsIgnoreCase(newConsoleName)) return true;
        }

        return false;
    }

    private void neutralizeUI() {
        findViewById(R.id.eng_export_settings).setEnabled(true);
        findViewById(R.id.eng_site_survey).setEnabled(true);
        findViewById(R.id.eng_export_settings_usb).setEnabled(true);
        findViewById(R.id.eng_import_settings_usb).setEnabled(true);

        findViewById(R.id.site_survey_spinner).setVisibility(View.INVISIBLE);
        findViewById(R.id.export_settings_spinner).setVisibility(View.INVISIBLE);
        findViewById(R.id.export_usb_spinner).setVisibility(View.INVISIBLE);
        findViewById(R.id.import_usb_spinner).setVisibility(View.INVISIBLE);

    }


    public ManagementPage() {
        handler = new Handler();
    }

    @Override
    protected View onCreate(Context context, ViewGroup viewGroup) {

        final View view = LayoutInflater.from(context).inflate(R.layout.management, null);
        viewGroup.addView(view);

        view.findViewById(R.id.eng_site_survey).setOnClickListener(v -> {
            view.findViewById(R.id.eng_export_settings).setEnabled(false);
            view.findViewById(R.id.eng_site_survey).setEnabled(false);
            view.findViewById(R.id.site_survey_spinner).setVisibility(View.VISIBLE);
            int size = siteSurveyConsoles.size();
            siteSurveyConsoles.clear();
            mAdapter.notifyItemRangeRemoved(0, size);
            DataMatrix.sendEvent(ImportExportKeys.ACTION_BROADCAST_SETTINGS);
        });



        mAdapter = new ManagementAdapter(siteSurveyConsoles);

        Button exportSettingsButton = view.findViewById(R.id.eng_export_settings);
        exportSettingsButton.setOnClickListener(v -> {

            if(siteSurveyConsoles.size() > 0)
            {

                if(areAnyItemsSelected()) {
                    view.findViewById(R.id.export_settings_spinner).setVisibility(View.VISIBLE);
                    findViewById(R.id.eng_export_settings).setEnabled(false);
                    findViewById(R.id.eng_site_survey).setEnabled(false);
                    for (ConsoleItem item : siteSurveyConsoles) {
                        if(item.isSelected()) {
                            DataMatrix.putString(ImportExportKeys.DATA_EXPORT_SETTINGS_STRING, item.getText());
                            //systemSettingsWrapper.exportSettings(item.getText());
                        }
                    }
                } else {
                    DialogFactory.message(v, getContext().getString(R.string.no_devices_selected_pg),
                            null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                }



            } else {
                DialogFactory.message(v, getContext().getString(R.string.no_devices_connected_pg),
                        null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
        });

        view.findViewById(R.id.eng_export_settings_usb).setOnClickListener(v -> {

            File exportDir = StorageUtils.getUSBPath();
            if(exportDir.exists()) {
                findViewById(R.id.eng_import_settings_usb).setEnabled(false);
                findViewById(R.id.eng_export_settings_usb).setEnabled(false);
                findViewById(R.id.export_usb_spinner).setVisibility(View.VISIBLE);
                DataMatrix.sendEvent(ImportExportKeys.ACTION_EXPORT_SETTINGS_USB);
            }else {
                DialogFactory.message(v, getContext().getString(R.string.please_insert_udisk_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }
        });
//
        view.findViewById(R.id.eng_import_settings_usb).setOnClickListener(v -> {

            File importDir = new File(StorageUtils.getUSBPath(), "settings");
            if(importDir.exists()) {
                findViewById(R.id.eng_import_settings_usb).setEnabled(false);
                findViewById(R.id.eng_export_settings_usb).setEnabled(false);
                findViewById(R.id.import_usb_spinner).setVisibility(View.VISIBLE);
                EngineeringIdleTimerManager.idleTimerManagement.stopMonitoring();
                DataMatrix.sendEvent(ImportExportKeys.ACTION_IMPORT_SETTINGS_USB);
            }else {
                DialogFactory.message(v, getContext().getString(R.string.please_insert_udisk_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
            }

        });

        view.findViewById(R.id.select_all_button).setOnClickListener(v -> {
            for(ConsoleItem item : siteSurveyConsoles) {
                  item.setSelected(true);
            }
            mAdapter.notifyDataSetChanged();
        });

        RecyclerView mRecyclerView = view.findViewById(R.id.eng_site_survey_table);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    private boolean areAnyItemsSelected() {
        for(ConsoleItem item : siteSurveyConsoles) {
            if(item.isSelected())
                return true;
        }

        return false;
    }


    @Override
    public void show(Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        mDataMatrix.createEventAdapter(ImportExportKeys.EVENT_REFRESH_UI, s -> neutralizeUI());

        mDataMatrix.createEventAdapter(ImportExportKeys.EVENT_FINISHED_EXPORT_USB, s -> {
            neutralizeUI();
            DialogFactory.message(findViewById(R.id.eng_export_settings_usb), getContext().getString(R.string.export_settings_disk_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
        });

        mDataMatrix.createEventAdapter(ImportExportKeys.EVENT_FINISHED_IMPORT_USB, s -> {
            neutralizeUI();
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(DataMatrix.getInt((SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT)));

            LocaleUtil.changeLanguage(ContextUtil.getApplicationContext(), lp, false);

            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL));
            handler.post(() -> {
                EngineeringIdleTimerManager.idleTimerManagement.monitorTimers();
                DialogFactory.message(findViewById(R.id.eng_import_settings_usb), getContext().getString(R.string.import_settings_success_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);

            });
        });

        mDataMatrix.createStringListAdapter(ImportExportKeys.DATA_CONSOLE_IDENTIFICATION_AND_STATUS_STRING_LIST, this::handleSettingsClientResponse);
    }


    @Override
    public void hide(Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        mDataMatrix.finish();
    }

    private void handleSettingsClientResponse(List<String> list) {
        if (list.size() == 2) {
            String status = list.get(1);
            if (status.equalsIgnoreCase(SystemSettings.CONNECTED)) {
                String newConsoleName = list.get(0);
                if (newConsoleName != null) {
                    if (!consoleEntryExists(newConsoleName) && !newConsoleName.isEmpty()) {
                        siteSurveyConsoles.add(new ConsoleItem(newConsoleName, status));
                        mAdapter.notifyItemInserted(siteSurveyConsoles.size() - 1);
                    }
                }
            } else if (status.equalsIgnoreCase(SystemSettings.TRANSFERRING)) {
                for (int i = 0; i < siteSurveyConsoles.size(); i++) {
                    if (siteSurveyConsoles.get(i).getText().equalsIgnoreCase(list.get(0))) {
                        siteSurveyConsoles.get(i).setStatus(getContext().getString(R.string.transferring_tc));
                        mAdapter.notifyItemChanged(i);
                        break;
                    }
                }

            } else if (status.equalsIgnoreCase(SystemSettings.DONE)) {
                neutralizeUI();
                for (int i = 0; i < siteSurveyConsoles.size(); i++) {
                    if (siteSurveyConsoles.get(i).getText().equalsIgnoreCase(list.get(0))) {
                        siteSurveyConsoles.get(i).setStatus(getContext().getString(R.string.finished_tc));
                        siteSurveyConsoles.get(i).setSelected(false);
                        mAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            } else if(status.equalsIgnoreCase(SystemSettings.FAILED_EXPORT)) {
                neutralizeUI();
                for (int i = 0; i < siteSurveyConsoles.size(); i++) {
                    if (siteSurveyConsoles.get(i).getText().equalsIgnoreCase(list.get(0))) {
                        siteSurveyConsoles.get(i).setStatus(getContext().getString(R.string.export_settings_network_fail_pg));
                        siteSurveyConsoles.get(i).setSelected(false);
                        mAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            } else if(status.equalsIgnoreCase(SystemSettings.RECONNECT)) {
                neutralizeUI();
                for (int i = 0; i < siteSurveyConsoles.size(); i++) {
                    if (siteSurveyConsoles.get(i).getText().equalsIgnoreCase(list.get(0))) {
                        siteSurveyConsoles.get(i).setStatus(getContext().getString(R.string.last_export_failed_site_survey_pg));
                        siteSurveyConsoles.get(i).setSelected(false);
                        mAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
        }
    }

}
