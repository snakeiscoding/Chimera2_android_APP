package com.jht.chimera.service.tiers.dialogs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TextView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;

public class DefaultUpgradeDialog extends DialogBase {

    private Button okayButton;
    private TextView message;
    private TextView title;

    public DefaultUpgradeDialog(Context context) {
        this(context, null, 0);
    }

    public DefaultUpgradeDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefaultUpgradeDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        okayButton = findViewById(R.id.dialog_button);
        message = findViewById(R.id.upgrade_message);
        title = findViewById(R.id.dialog_title);
        findViewById(R.id.dialog_button).setOnClickListener(v -> hide());
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.default_upgrade_dialog;
    }

    public void init(String message, String title) {
        this.message.setText(message);
        this.title.setText(title);
    }
}
