package com.jht.chimera.ui.machinesettings.dialogs;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.language.StringHelper;

/**
 * This dialog pops up when the user wants to log a service event.  It asks for a description and
 * the user can either create the entry or close it.
 */
public class ServiceLogEntry extends DialogBase {

    /**
     * Callback for when the user is done with the dialog.
     */
    public interface IOnEntryComplete {
        void onCancel();

        void onEntry(StringHelper.ServiceTypes serviceType, String description);
    }


    // The service type being created.
    private StringHelper.ServiceTypes serviceType;

    // The description entered.
    private UserTextInput description;

    // The callback when complete.
    private IOnEntryComplete onEntryComplete;

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     */
    public ServiceLogEntry(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     * @param attrs   Attributes that are set from the view's XML.
     */
    public ServiceLogEntry(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context      Current context for the view.
     * @param attrs        Attributes that are set from the view's XML.
     * @param defStyleAttr Default style for the view.
     */
    public ServiceLogEntry(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        description = findViewById(R.id.service_log_entry_description);
        description.setOnIMEAction(action -> {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(description.getWindowToken(), 0);
            return true;
        });
        findViewById(R.id.service_log_entry_create).setOnClickListener(v -> {
            findViewById(R.id.service_log_entry_create).setEnabled(false);
            if (onEntryComplete != null) {
                onEntryComplete.onEntry(serviceType, description.getText());
            }
            hide();
        });
        findViewById(R.id.service_log_entry_cancel).setOnClickListener(v -> {
            if (onEntryComplete != null) {
                onEntryComplete.onCancel();
            }
            hide();
        });


    }

    @Override
    protected int getLayoutResId() {
        return R.layout.service_log_entry;
    }

    /**
     * Initialize the dialog.
     *
     * @param serviceType     The service type being created.
     * @param onEntryComplete The callback to call when finished.
     */
    public void init(StringHelper.ServiceTypes serviceType, IOnEntryComplete onEntryComplete) {
        this.serviceType = serviceType;
        ((TextView) findViewById(R.id.service_log_entry_service_type)).setText(StringHelper.getServiceTypeString(getContext(), serviceType));
        findViewById(R.id.service_log_entry_create).setEnabled(true);
        this.onEntryComplete = onEntryComplete;

    }


}
