package com.jht.chimera.service.ui.panels;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.RealScreen;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.DigitalSafetyPinKeypad;
import com.jht.datamatrixlib.DataMatrix;

// Based on ExitDialog
// Need prompt for digital safety PIN entry
public class DigitalSafetyPinDialog extends Dialog {
    private static final String TAG = "DigitalSafetyPinDialog";

    // NOTE: use setOnDismissListener and check DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL if you need to wait for feedback

    private final DataMatrix dataMatrix = new DataMatrix();

    private EditText[] otpETs = new EditText[4];
    private TextView incorrectPinTextView;
    private DigitalSafetyPinKeypad keypad;

    private View.OnClickListener verifyButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String code = getCode();
            checkPin(code);
        }
    };

    private View.OnClickListener numberButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button button = (Button) v;
            String text = button.getText().toString();
            if (otpETs[0].getText().toString().equals("")) {
                otpETs[0].setText(text);
            }
            else if (otpETs[1].getText().toString().equals("")) {
                otpETs[1].setText(text);
            }
            else if (otpETs[2].getText().toString().equals("")) {
                otpETs[2].setText(text);
            }
            else if (otpETs[3].getText().toString().equals("")) {
                otpETs[3].setText(text);
            }
        }
    };

    public DigitalSafetyPinDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ERROR);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        setContentView(R.layout.dialog_digital_safety_pin_popup);

        otpETs[0] = findViewById(R.id.otpET1);
        otpETs[1] = findViewById(R.id.otpET2);
        otpETs[2] = findViewById(R.id.otpET3);
        otpETs[3] = findViewById(R.id.otpET4);

        otpETs[0].setInputType(InputType.TYPE_NULL);
        otpETs[1].setInputType(InputType.TYPE_NULL);
        otpETs[2].setInputType(InputType.TYPE_NULL);
        otpETs[3].setInputType(InputType.TYPE_NULL);

        incorrectPinTextView = findViewById(R.id.incorrect_pin_text_view);

        keypad = findViewById(R.id.pin_keypad);

        if (keypad != null) {
            if (keypad.button_confirm != null) { keypad.button_confirm.setOnClickListener(verifyButtonOnClickListener); }

            if (keypad.button_0 != null) { keypad.button_0.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_1 != null) { keypad.button_1.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_2 != null) { keypad.button_2.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_3 != null) { keypad.button_3.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_4 != null) { keypad.button_4.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_5 != null) { keypad.button_5.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_6 != null) { keypad.button_6.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_7 != null) { keypad.button_7.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_8 != null) { keypad.button_8.setOnClickListener(numberButtonOnClickListener); }
            if (keypad.button_9 != null) { keypad.button_9.setOnClickListener(numberButtonOnClickListener); }

            if (keypad.button_back != null) {
                keypad.button_back.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int inputLength = getCode().length();
                        if (inputLength == 4) {
                            otpETs[3].setText("");
                        }
                        else if (inputLength == 3) {
                            otpETs[2].setText("");
                        }
                        else if (inputLength == 2) {
                            otpETs[1].setText("");
                        }
                        else if (inputLength == 1) {
                            otpETs[0].setText("");
                        }
                    }
                });
            }
        }

        setCanceledOnTouchOutside(true);

        float widthMultiplier = 0.35f;
        float heightMultiplier = 0.80f;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = (int) (RealScreen.widthPixels * widthMultiplier);
        lp.height = (int) (RealScreen.heightPixels * heightMultiplier);
        lp.y = 0;
        getWindow().setAttributes(lp);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            return checkPin(getCode());
        }
        return super.dispatchKeyEvent(event);
    }

    private String getEditTextString(EditText editText) {
        return editText.getText().toString().trim();
    }

    private String getCode() {
        StringBuilder code = new StringBuilder();
        for (EditText et : otpETs) {
            code.append(getEditTextString(et));
        }
        return code.toString();
    }

    private void clearInputs() {
        for (EditText et : otpETs) {
            et.setText("");
        }
    }

    private boolean checkPin(String pin) {
        if (pin != null && pin.equals(DataMatrix.getString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING))) {
            incorrectPinTextView.setVisibility(View.INVISIBLE);
            clearInputs();
            DataMatrix.putBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, false);
            return true;
        } else {
            incorrectPinTextView.setVisibility(View.VISIBLE);
            clearInputs();
            return false;
        }
    }

    private void setupMatrixListen() {
        dataMatrix.createBoolAdapter(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL, needed -> {
            if (!needed) {
                dismiss();
            }
        }, false);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setupMatrixListen();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try {
            clearInputs();
            incorrectPinTextView.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "onDetachedFromWindow: "+e);
        }
        dataMatrix.finish();
    }

    public static boolean needsToShow() {
        return DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL) && DataMatrix.getBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL);
    }

    public interface Action {
        void doAction();
    }
    public void checkIfNeeded(Action nextAction) { checkIfNeeded(nextAction, null); }
    public void checkIfNeeded(Action nextAction, Action abortAction) {
        if (this.isShowing()) return; // prompt is already pending
        if (DigitalSafetyPinDialog.needsToShow()) {
            this.setOnDismissListener(dialogInterface -> {
                boolean success = !DataMatrix.getBoolean(HALKeys.DATA_DIGITAL_SAFETY_PIN_PROMPT_NEEDED_BOOL);
                //Log.d("DigitalSafetyPinDialog", "SAFETY_PIN: " + success);
                if (success) nextAction.doAction();
                else if (abortAction != null) abortAction.doAction();
            });
            this.show();
        } else {
            nextAction.doAction();
        }
    }

}
