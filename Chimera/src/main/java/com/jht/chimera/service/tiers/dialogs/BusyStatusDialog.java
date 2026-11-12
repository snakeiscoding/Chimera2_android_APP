package com.jht.chimera.service.tiers.dialogs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;

public class BusyStatusDialog extends DialogBase {

    private TextView upgradeMessage;
    private Button okButton;
    private ProgressBar progressBar;

    public BusyStatusDialog(Context context) {
        this(context, null, 0);
    }

    public BusyStatusDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BusyStatusDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        upgradeMessage = findViewById(R.id.upgrade_message);
        okButton = findViewById(R.id.dialog_button);
        progressBar = findViewById(R.id.loading_spinner);

        okButton.setOnClickListener(v -> hide());
    }

    public void setStatusMessage(String message) {
        upgradeMessage.setText(message);
    }

    public void changeButtonStateToOkay() {
        okButton.setVisibility(VISIBLE);
        progressBar.setVisibility(GONE);
    }


    @Override
    protected int getLayoutResId() {
        return R.layout.busy_status_dialog;
    }
}
