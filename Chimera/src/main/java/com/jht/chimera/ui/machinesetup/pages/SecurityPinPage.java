package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.datamatrixlib.DataMatrix;

// used to be SafetyPinPage but they decided to rename "safety pin"" as "security pin"
public class SecurityPinPage extends MachineSetupPageBase {

    CheckBox pinCheckBox;
    UserTextInput pinTextInput;
    TextView warningText;

    @SuppressLint("InflateParams")
    public SecurityPinPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.safety_pin_fts, null);
        container.addView(view);

        pinCheckBox = view.findViewById(R.id.eng_require_pin_checkbox);
        pinTextInput = view.findViewById(R.id.eng_gen_pin_number_input);
        warningText = view.findViewById(R.id.safety_warning);

        pinCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pageData().useSafetyPin = isChecked;
            if (isChecked) {
                pinTextInput.enable();
                warningText.setVisibility(View.GONE);
            }
            else {
                pinTextInput.disable();
                warningText.setVisibility(View.VISIBLE);
            }
        });
        boolean pinEnabled = DataMatrix.getBoolean(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_CHECK_ENABLED_BOOL);
        pinCheckBox.setChecked(pinEnabled);

        pinTextInput.setText("" + DataMatrix.getString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING, ""));
        pinTextInput.setInputProperties(pinTextInput.getInputProperties().inputValue(DataMatrix.getString(SettingKeys.General.DATA_DIGITAL_SAFETY_PIN_STRING, "")));
        pinTextInput.setOnTextChanged(s -> pageData().safetyPin = s);

        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);
        pageData().title.setText(R.string.security_pin_tc);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);

    }

    @Override
    public boolean onNext() {
        return true;
    }

}
