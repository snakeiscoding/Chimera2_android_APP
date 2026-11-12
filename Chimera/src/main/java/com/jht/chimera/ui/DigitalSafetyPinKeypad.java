package com.jht.chimera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;

public class DigitalSafetyPinKeypad  extends ConstraintLayout {

    public Button button_1;
    public Button button_2;
    public Button button_3;
    public Button button_4;
    public Button button_5;
    public Button button_6;
    public Button button_7;
    public Button button_8;
    public Button button_9;
    public Button button_0;
    public androidx.appcompat.widget.AppCompatImageButton button_back;
    public androidx.appcompat.widget.AppCompatImageButton button_confirm;

    public DigitalSafetyPinKeypad(Context context) {
        super(context);
        init(context);
    }

    public DigitalSafetyPinKeypad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DigitalSafetyPinKeypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        inflate(context, R.layout.dialog_digital_safety_pin_keypad, this);
        button_1 = findViewById(R.id.safety_pin_button_1);
        button_2 = findViewById(R.id.safety_pin_button_2);
        button_3 = findViewById(R.id.safety_pin_button_3);
        button_4 = findViewById(R.id.safety_pin_button_4);
        button_5 = findViewById(R.id.safety_pin_button_5);
        button_6 = findViewById(R.id.safety_pin_button_6);
        button_7 = findViewById(R.id.safety_pin_button_7);
        button_8 = findViewById(R.id.safety_pin_button_8);
        button_9 = findViewById(R.id.safety_pin_button_9);
        button_0 = findViewById(R.id.safety_pin_button_0);
        button_back = findViewById(R.id.safety_pin_button_back);
        button_confirm = findViewById(R.id.safety_pin_button_enter);
    }
}
