package com.jht.chimera.lib.widget.radiogroup;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.appcompat.widget.AppCompatTextView;

import com.jht.chimera.R;


/**
 * The selector adapter is used to create a selector list utilizing a recycler view.
 */
public class RadioGroup extends LinearLayout {

    public interface IOnOptionChanged {
        void onOptionChanged(int index);
    }

    private IOnOptionChanged onOptionChanged = null;

    public RadioGroup(Context context) {
        this(context, null, android.R.attr.textViewStyle);
    }

    public RadioGroup(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public RadioGroup(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        addView(LayoutInflater.from(context).inflate(R.layout.radio_group, null, false));

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.JHTGeneralWidget, defStyle, 0);
        ((AppCompatTextView) findViewById(R.id.radio_group_label)).setText(a.getText(R.styleable.JHTGeneralWidget_label));
        a.recycle();

    }


    public void setLabel(String text) {
        AppCompatTextView label = findViewById(R.id.radio_group_label);
        label.setText(text);
    }

    public void linkField(int index, String[] options, int orientation) {
        android.widget.RadioGroup group = findViewById(R.id.general_radio_group);
        if (orientation == VERTICAL){
            group.setOrientation(orientation);
        }
        group.removeAllViews();
        for (int i = 0; i < options.length; i++) {
            RadioButton button = (RadioButton) LayoutInflater.from(getContext()).inflate(R.layout.radio_button, null, false);
            if (button != null) {
                button.setText(options[i]);
                button.setId(i);
                if (orientation == VERTICAL && i != options.length - 1){
                    button.setPadding(0,0,0,10);
                } else {
                    button.setPadding(0,0,10,0);
                }
            }
            if (i == index) button.setChecked(true);
            group.addView(button);
        }
        group.setOnCheckedChangeListener((group1, checkedId) -> {
            if (onOptionChanged != null) {
                onOptionChanged.onOptionChanged(checkedId);
            }
        });
    }

    public void setOnOptionChanged(IOnOptionChanged onOptionChanged) {
        this.onOptionChanged = onOptionChanged;
    }

    public void select(int index) {
        ((android.widget.RadioGroup) findViewById(R.id.general_radio_group)).check(index);
    }


}
