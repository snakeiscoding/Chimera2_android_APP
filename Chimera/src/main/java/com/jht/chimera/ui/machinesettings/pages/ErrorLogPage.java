package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemUtils;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datetime.DateTimeUtil;
import com.jht.chimera.ui.machinesettings.adapters.error.ErrorLogItem;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.errorostringsdatabase.ErrorCodeStrings;
import com.jht.chimera.ui.machinesettings.errorostringsdatabase.ErrorStringsDB;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.ErrorCodeList;
import com.jht.liberror.ErrorCodeManager;
import com.jht.liberror.ErrorLog;
import com.jht.liberror.ErrorLogList;
import com.jhtp.dapiservice.DapiEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Implements the error log page gluing the page to the model layer.
 */
public final class ErrorLogPage extends SettingPageBase {

    private final DataMatrix dataMatrix = new DataMatrix();
    private ErrorLogList errorLogList;
    private SelectorItemBase errorLogData;
    private RecyclerView errorLogTable;
    private ErrorStringsDB errorStringsDB;

    public ErrorLogPage() {}

    /**
     * Creates the Error log tab.
     * @param context
     * @param container
     * @return created View for the Error tab
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected View onCreate(Context context, ViewGroup container) {
        View view = LayoutInflater.from(context).inflate(R.layout.errors, null);
        container.addView(view);
        errorStringsDB = new ErrorStringsDB(context);

        //Persistent Logging checkbox
        CheckBox persistentLogs = view.findViewById(R.id.persistent_logs);
        persistentLogs.setChecked(DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL));
        persistentLogs.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if(isChecked) {
                DataMatrix.putLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG, new Date().getTime());
            }
            DataMatrix.putBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL, isChecked);
            updatePersistentLogNotice();
        }));

        //Save Logs button
        view.findViewById(R.id.save_logs_usb).setOnClickListener(v -> DataMatrix.sendEvent(LoggerKeys.ACTION_SAVE_LOGS));

        //Logging Options button
        view.findViewById(R.id.logging_options).setOnClickListener(ProjectDialogFactory::showLoggingOptions);

        //Each error code's checkbox
        ((CheckBox)view.findViewById(R.id.select_error)).setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener) (buttonView, isChecked) -> {
            for(int i = 0; i < errorLogData.numChildren(); i++) {
                ISelectorItem rowData = errorLogData.getChild(i);
                if(rowData instanceof ErrorLogItem) {
                    ((ErrorLogItem)rowData).setIsChecked(isChecked);
                }
            }
            if(errorLogTable.getAdapter() != null) {
                errorLogTable.getAdapter().notifyDataSetChanged();
            }
        });

        errorLogTable = view.findViewById(R.id.error_code_log_list);
        SelectorDefaultAnimations.setDefaultAnimations(context, errorLogTable);

        //Export Errors to USB button
        view.findViewById(R.id.save_error_logs).setOnClickListener(v -> saveErrorCodes());

        //Delete button
        view.findViewById(R.id.delete_logs).setOnClickListener(v -> DialogFactory.confirm(v, getContext().getString(R.string.delete_error_logs_tc), () -> { // NON-NLS
            // Identify all logs that have been selected for removal.
            List<Integer> codesRemoving = new ArrayList<>();
            for (int i = 0; i < errorLogData.numChildren(); i++) {
                final ISelectorItem item = errorLogData.getChild(i);
                if (item instanceof ErrorLogItem) {
                    if (((ErrorLogItem) item).isChecked()) {
                        codesRemoving.add(errorLogList.errorLogList.get(i).errorCode);
                    }
                }
            }

            // Send the selected items for removal.
            ErrorCodeManager.deleteLogsFromList(new ErrorCodeList(codesRemoving));
        }, null));
        return view;
    }

    /**
     * Update the error log table using the given error log list.
     * <p></p>
     *
     * If new errors are created, the database will need to be updated. If not updated, the
     * descriptions for the new errors will say <code>"Missing Description"</code>. Instructions for
     * updating this database are located in Confluence. Additionally, the database being referenced
     * is located here: <code>src/main/assets/databases/service_strings.db</code>
     *
     * @param list `null` if no errors have been published, else the current state of the error log
     *             database.
     */
    private void updateAdapter(@Nullable final ErrorLogList list) {
        errorLogList = list; // cache the value. for future reference.
        errorLogData = new SelectorItemBase(new ArrayList<>());

        // Process the log list if non-null.
        if (errorLogList != null) {
            final List<ErrorLog> errorLogs = errorLogList.errorLogList;
            errorLogData = new SelectorItemBase(new ArrayList<>());

            for (final ErrorLog errorLog : errorLogs) {
                final ErrorCodeStrings descriptionObj = errorStringsDB.getServiceDescriptions(errorLog.errorCode);
                String descriptionString = "Missing Description"; // NON-NLS
                if (descriptionObj != null && descriptionObj.description != null) {
                    descriptionString = descriptionObj.description;
                }
                errorLogData.addItem(new ErrorLogItem(errorLog, descriptionString));
            }
        }

        errorLogTable.setAdapter(new SelectorAdapterBase(getContext(), errorLogData, null));
    }

    /**
     * This method is called when the Export Errors to USB button is clicked.
     */
    private void saveErrorCodes() {
        final File destination = SystemUtils.getLogUSBPath(Calendar.getInstance());
        if (destination != null) {
            ErrorCodeManager.saveErrorCodes(destination);
        }
    }

    /**
     * Used to show the error list page. Passes an animator listener to listen for animation events
     * while showing the page
     *
     * @param listener Used to pass animation event callbacks.
     * @param start    If true then we animate in from the start of the page.
     */
    @Override
    public void show(@Nullable final Animator.AnimatorListener listener, final boolean start) {
        super.show(listener, start);
        updatePersistentLogNotice();
        dataMatrix.createParcelableAdapter(LoggerKeys.DATA_ERROR_LOG_LIST_PARCELABLE, ErrorLogList.class, this::updateAdapter);
    }

    /**
     * Used to hide the error list page. Passes an animator listener to listenr for animation events
     * while hiding the page.
     *
     * @param listener Used to pass animation event callbacks.
     * @param start    If true then we animate out to the start of the page.
     */
    @Override
    public void hide(@Nullable final Animator.AnimatorListener listener, final boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();
    }

    /**
     * Updates text under the "Persistent Logging" checkbox so that the user knows until what date
     * and time the logs will be collected/saved through.
     */
    @SuppressLint("SetTextI18n")
    private void updatePersistentLogNotice() {
        TextView persistentNotice = findViewById(R.id.persistent_logs_notification);
        long persistentLogStartingTime = DataMatrix.getLong(SettingKeys.Debug.DATA_SETTING_PERSIST_LOGGING_START_TIME_LONG);
        if (DataMatrix.getBoolean(SettingKeys.Debug.DATA_ENABLE_PERSISTENT_LOG_BOOL) && DataMatrix.getInt(SettingKeys.General.DATA_DAPI_ENVIRONMENT_ENUM_INT) == DapiEnvironment.Production.ordinal()) {
            Calendar persistentStopTime = Calendar.getInstance();
            persistentStopTime.setTimeInMillis(persistentLogStartingTime);
            persistentStopTime.add(Calendar.DAY_OF_MONTH, 14);
            persistentNotice.setText(
                    getContext().getString(R.string.persistent_log_end_notification_tc) + "\n" + DateTimeUtil.getDateTime(persistentStopTime)
            );
        } else {
            persistentNotice.setText("");
        }
    }
}