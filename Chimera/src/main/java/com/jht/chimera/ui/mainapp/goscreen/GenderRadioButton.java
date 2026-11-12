package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.datamatrixlib.DataMatrix;

public class GenderRadioButton extends ConstraintLayout {
    private RadioGroup mGroup;
    private AppCompatRadioButton mMaleButton;
    private AppCompatRadioButton mFemaleButton;
    private AppCompatRadioButton mNonBinaryGenderButton;
    private TextView mGenderTitle;

    public GenderRadioButton(Context context) {
        super(context);
        initialize(context);
    }

    public GenderRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public GenderRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gender_buttons_layout, this);
        mGroup = findViewById(R.id.gender_radio_group);
        mMaleButton = findViewById(R.id.male_radio_button);
        mFemaleButton = findViewById(R.id.female_radio_button);
        mNonBinaryGenderButton = findViewById(R.id.non_binary_radio_button);

        mGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // We want to update the keys so that the gender is maintained if the user selects a different
            // workout.
            int gender = (int)getGenderValue();
            DataMatrix.putInt(DAPIKeys.DATA_USER_GENDER_ENUM_INT, gender);
        });
    }

    public float getGenderValue() {
        if (mGroup.getCheckedRadioButtonId() == mMaleButton.getId()) return 0;
        if (mGroup.getCheckedRadioButtonId() == mFemaleButton.getId()) return 1;
        return 2;
    }

    public void setSelectedGender(int value) {
        if (value == 0) {
            setMaleSelected();
        }
        else if (value == 1)  {
            setFemaleSelected();
        } else  {
            setNonBinaryGenderSelected();
        }
    }

    public void setMaleSelected() {
        mMaleButton.setChecked(true);
    }

    public void setFemaleSelected() {
        mFemaleButton.setChecked(true);
    }

    public void setNonBinaryGenderSelected() {
        mNonBinaryGenderButton.setChecked(true);
    }
}
