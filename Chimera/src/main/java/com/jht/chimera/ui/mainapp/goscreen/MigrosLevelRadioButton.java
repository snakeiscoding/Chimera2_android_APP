package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RadioGroup;

import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;

public class MigrosLevelRadioButton  extends ConstraintLayout {
    private RadioGroup mGroup;
    private AppCompatRadioButton mBeginnerButton;
    private AppCompatRadioButton mAdvancedButton;

    public MigrosLevelRadioButton(Context context) {
        super(context);
        initialize(context);
    }

    public MigrosLevelRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public MigrosLevelRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.migros_level_buttons_layout, this);
        mGroup = findViewById(R.id.level_radio_group);
        mBeginnerButton = findViewById(R.id.beginner_radio_button);
        mAdvancedButton = findViewById(R.id.advanced_radio_button);
    }

    public void setLevelParameter(WorkoutParameter parameter) {
        mGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId == R.id.beginner_radio_button) {
                parameter.value(1);
            }
            else {
                parameter.value(2);
            }
        });
        if(parameter.valueAsDouble() == 1) {
            mBeginnerButton.setChecked(true);
        }
        else {
            mAdvancedButton.setChecked(true);
        }
    }
}

