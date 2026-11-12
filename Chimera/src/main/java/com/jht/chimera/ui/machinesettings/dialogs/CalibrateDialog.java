package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;

public class CalibrateDialog extends DialogBase {

    public interface IOnCalibrateDialog {
//        void onCancel();
        void onOk();
    }

    private Button okCancelButton;
    private TextView message;
    private TextView title;
    private IOnCalibrateDialog onCalibrateDialog;
    private boolean showOk = true;
    private CalibrateDialog.CALIBRATE_BUTTON_STATE buttonState;

    public CalibrateDialog(Context context) {
        this(context, null, 0);
    }

    public CalibrateDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalibrateDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        okCancelButton = findViewById(R.id.calibrate_dialog_button);
        message = findViewById(R.id.calibrate_dialog_message);
        title = findViewById(R.id.calibrate_dialog_title);
        okCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
                if(onCalibrateDialog != null) {
                    onCalibrateDialog.onOk();
//                    if(showOk) {
//                        onCalibrateDialog.onOk();
//                    }
//                    else {
//                        onCalibrateDialog.onCancel();
//                    }
                }
            }
        });
    }

    public void init(String title, String message, boolean showOk, IOnCalibrateDialog onCalibrateDialog, CalibrateDialog.CALIBRATE_BUTTON_STATE buttonState) {
        okCancelButton.setVisibility(showOk ? View.VISIBLE : View.GONE);
        this.showOk = true;
        this.onCalibrateDialog = onCalibrateDialog;
        this.title.setText(title);
        this.message.setText(message);
        this.buttonState = buttonState;
        updateButton();

    }

    public void complete(String message, boolean showOK) {
        this.message.setText(message);
        this.showOk = showOK;
        updateButton();
    }

    public void setTitle(String title) {
        this.title.setText(title);
    }

    public void setMessage(String message) {
        this.message.setText(message);
    }

    private void updateButton() {
        okCancelButton.setVisibility(showOk ? View.VISIBLE : View.INVISIBLE);
        if(buttonState == CALIBRATE_BUTTON_STATE.OK) {
            okCancelButton.setText("");
            okCancelButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.message_dialog_box_button_yes));
        } else if(buttonState == CALIBRATE_BUTTON_STATE.CANCEL) {
            okCancelButton.setText("");
            okCancelButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.message_dialog_box_button_no));
        } else {
            okCancelButton.setText(getResources().getText(R.string.stop_tc));
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.calibrate_dialog;
    }

    public static enum CALIBRATE_BUTTON_STATE {
        OK,
        CANCEL,
        NONE;

        private CALIBRATE_BUTTON_STATE() {
        }
    }
}
