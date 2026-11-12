package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

public class MachineSerialNumberPage extends MachineSetupPageBase {

    private UserTextInput consoleSerialNumber;
    private UserTextInput frameSerialNumber;

    private final TextWatcher consoleSerialNumberWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            pageData().consoleSerialNumberEntered = s.toString();
        }
    };

    private final TextWatcher frameSerialNumberWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            pageData().frameSerialNumberEntered = s.toString();
        }
    };

    @SuppressLint("InflateParams")
    public MachineSerialNumberPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.machine_information, null);
        container.addView(view);

        consoleSerialNumber = view.findViewById(R.id.machine_setup_console_serial_number);
        frameSerialNumber = view.findViewById(R.id.machine_setup_frame_serial_number);

        consoleSerialNumber.getFocus();

        ((TextView)view.findViewById(R.id.text_lcb_type)).setText(String.format("LCB: 0x%02x",
                DataMatrix.getInt(HALKeys.DATA_LCB_TYPE_INT)));

        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        loadSerialNumbers();
        pageData().title.setText(R.string.machine_information_tc);
        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.GONE);
        pageData().confirmButton.setVisibility(View.VISIBLE);

        String consoleSerialNoC = pageData().consoleSerialNumberEntered;
        consoleSerialNumber.setInputProperties(consoleSerialNumber.getInputProperties().
                inputValue(consoleSerialNoC)
                .minChars(0)
                .maxChars(32));

        String frameSerialNoF = pageData().frameSerialNumberEntered;
        frameSerialNumber.setInputProperties(frameSerialNumber.getInputProperties()
                .inputValue(frameSerialNoF)
                .minChars(0)
                .maxChars(32));

        consoleSerialNumber.addTextWatcher(consoleSerialNumberWatcher);
        frameSerialNumber.addTextWatcher(frameSerialNumberWatcher);

    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);

        consoleSerialNumber.removeTextWatcher(consoleSerialNumberWatcher);
        frameSerialNumber.removeTextWatcher(frameSerialNumberWatcher);
    }

    @Override
    public boolean onNext() {
        return true;
    }

    void loadSerialNumbers() {

        String consoleSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        String frameSerial = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);

        String consolePrefix = ProductConfig.getConsoleType(pageData().machineTypeSelection).getConsolePrefix();

        if (!consoleSerial.startsWith(consolePrefix)) {
            pageData().consoleSerialNumberEntered = consolePrefix;
        } else {
            pageData().consoleSerialNumberEntered = consoleSerial;
        }

        pageData().frameSerialNumberEntered = frameSerial;

    }
}
