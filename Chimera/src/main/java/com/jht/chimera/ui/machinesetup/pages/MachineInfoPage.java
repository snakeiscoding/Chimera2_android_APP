package com.jht.chimera.ui.machinesetup.pages;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import com.jht.androidcommonalgorithms.util.SerialNumberUtil;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;

import java.io.File;

public class MachineInfoPage extends MachineSetupPageBase {
    /**
     * Required file name to be able to import serial number from text file on a USB.
     */
    private final String BRIDGE_SERIAL_NUMBER_FILE_NAME = "BSerialNumber.txt";

    private UserTextInput consoleSerialNumber;
    private UserTextInput frameSerialNumber;
    private UserTextInput bridgeSerialNumber;
    private UserTextInput clubId;
    private boolean shown = false;

    private final TextWatcher consoleSerialNumberWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            pageData().consoleSerialNumberEntered = SerialNumberUtil.fixSNToBeAlphanumeric(s.toString());
            checkNextButton();
        }
    };

    private final TextWatcher frameSerialNumberWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            pageData().frameSerialNumberEntered = SerialNumberUtil.fixSNToBeAlphanumeric(s.toString());
            checkNextButton();
        }
    };

    private final TextWatcher bridgeSerialNumberWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            pageData().bridgeSerialNumberEntered = SerialNumberUtil.fixSNToBeAlphanumeric(s.toString());
            checkNextButton();
        }
    };

    private final TextWatcher facilityIdWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            int clubId = 0;
            try {
                clubId = Integer.parseInt(s.toString());
            } catch (Exception ignored) {
                pageData().nextButton.setVisibility(View.GONE);
            }
            pageData().clubIdEntered = clubId;
            checkNextButton();
        }
    };

    @SuppressLint("InflateParams")
    public MachineInfoPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.machine_information, null);
        container.addView(view);

        consoleSerialNumber = view.findViewById(R.id.machine_setup_console_serial_number);
        frameSerialNumber = view.findViewById(R.id.machine_setup_frame_serial_number);
        consoleSerialNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        frameSerialNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        clubId = view.findViewById(R.id.machine_setup_club_id);
        clubId.setOnIMEAction(action -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                if (pageData().nextButton.getVisibility() == View.VISIBLE) {
                    pageData().nextButton.callOnClick();
                }
            }
            return false;
        });

        bridgeSerialNumber = view.findViewById(R.id.machine_setup_bridge_serial_number);

        if (BuildConfig.HAS_BRIDGE_SN) {
            if (bridgeSerialNumber != null) {
                bridgeSerialNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            }
        } else {
            setVisible(bridgeSerialNumber, View.GONE);
        }

        view.findViewById(R.id.machine_setup_import_serial_numbers).setOnClickListener(v -> {
            String consoleSerialNumberImport = SerialNumberUtil.readSerialNumberFromFile(new File(StorageUtils.getUSBPath(), "CSerialNumber.txt")); //NON-NLS
            consoleSerialNumberImport = consoleSerialNumberImport.toUpperCase();
            if (!consoleSerialNumberImport.equals("")) {
                consoleSerialNumber.setText(consoleSerialNumberImport);
                pageData().consoleSerialNumberEntered = consoleSerialNumberImport;
                pageData().consoleSerialNumberConfirm = consoleSerialNumberImport;
            }

            String frameSerialNumberImport = SerialNumberUtil.readSerialNumberFromFile(new File(StorageUtils.getUSBPath(), "FSerialNumber.txt")); //NON-NLS
            frameSerialNumberImport = frameSerialNumberImport.toUpperCase();
            if (!frameSerialNumberImport.equals("")) {
                frameSerialNumber.setText(frameSerialNumberImport);
                pageData().frameSerialNumberEntered = frameSerialNumberImport;
                pageData().frameSerialNumberConfirm = frameSerialNumberImport;
            }

            if (BuildConfig.HAS_BRIDGE_SN) {
                String bridgeSerialNumberImport = SerialNumberUtil.readSerialNumberFromFile(new File(StorageUtils.getUSBPath(), BRIDGE_SERIAL_NUMBER_FILE_NAME));
                bridgeSerialNumberImport = bridgeSerialNumberImport.toUpperCase();
                if (!bridgeSerialNumberImport.isEmpty()) {
                    if (bridgeSerialNumber != null) {
                        bridgeSerialNumber.setText(bridgeSerialNumberImport);
                    }
                    pageData().bridgeSerialNumberEntered = bridgeSerialNumberImport;
                    pageData().bridgeSerialNumberConfirm = bridgeSerialNumberImport;
                }
            } else {
                setVisible(bridgeSerialNumber, View.GONE);
            }

            checkNextButton();
        });

        consoleSerialNumber.getFocus();

        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        pageData().loadSerialNumbers();
        pageData().title.setText(R.string.machine_information_tc);
        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);

        if (ProductConfig.isCommercial()) {
            consoleSerialNumber.setInputProperties(consoleSerialNumber.getInputProperties().inputValue(pageData().consoleSerialNumberEntered));
        } else {
            consoleSerialNumber.setInputProperties(consoleSerialNumber.getInputProperties().inputValue(pageData().consoleSerialNumberEntered).minChars(ProductConfig.getConsoleType(pageData().machineTypeSelection).getConsolePrefix().length()));
        }
        frameSerialNumber.setInputProperties(frameSerialNumber.getInputProperties().inputValue(pageData().frameSerialNumberEntered).minChars(FrameConfigManager.getInstance().getFramePrefixLength()));
        clubId.setText(pageData().clubIdEntered + "");

        clubId.setOnIMEAction( action -> {
            if(action == EditorInfo.IME_ACTION_DONE) {
                if(pageData().nextButton.getVisibility() == View.VISIBLE) {
                    pageData().nextButton.callOnClick();
                }
            }
            return false;
        });

        if (BuildConfig.HAS_BRIDGE_SN) {
            bridgeSerialNumber.setInputProperties(bridgeSerialNumber.getInputProperties().inputValue(pageData().bridgeSerialNumberEntered).minChars(1).maxChars(40));
            bridgeSerialNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            bridgeSerialNumber.addTextWatcher(bridgeSerialNumberWatcher);
        } else {
            bridgeSerialNumber.setVisibility(View.GONE);
        }

        shown = true;
        checkNextButton();

        consoleSerialNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        frameSerialNumber.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        consoleSerialNumber.addTextWatcher(consoleSerialNumberWatcher);
        frameSerialNumber.addTextWatcher(frameSerialNumberWatcher);
        clubId.addTextWatcher(facilityIdWatcher);
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        shown = false;

        consoleSerialNumber.removeTextWatcher(consoleSerialNumberWatcher);
        frameSerialNumber.removeTextWatcher(frameSerialNumberWatcher);
        clubId.removeTextWatcher(facilityIdWatcher);

        if (BuildConfig.HAS_BRIDGE_SN) {
            if (bridgeSerialNumber != null) {
                bridgeSerialNumber.removeTextWatcher(bridgeSerialNumberWatcher);
            }
        } else {
            setVisible(bridgeSerialNumber, View.GONE);
        }
    }

    @Override
    public boolean onNext() {
        return true;
    }

    private boolean suffixAscending(String suffix) {
        for(int i = 1; i < suffix.length(); i++) {
            if((int)suffix.charAt(i) != suffix.charAt(i - 1) + 1) {
                return false;
            }
        }
        return true;
    }

    private boolean suffixDescending(String suffix) {
        for(int i = 1; i < suffix.length(); i++) {
            if((int)suffix.charAt(i) != suffix.charAt(i - 1) - 1) {
                return false;
            }
        }
        return true;
    }
    private boolean suffixValid(String suffix) {
        if (suffix != null && !suffix.equals("") && !suffixAscending(suffix) && !suffixDescending(suffix)) {
            // Make sure suffix isn't the same.
            for (int i = 1; i < suffix.length(); i++)
                if (suffix.charAt(0) != suffix.charAt(i)) {
                    return true;
                }
        }
        return false;
    }

    private void checkNextButton() {
        if (shown) {
            if (areFSNandCSNValid()) {
                // Bridge SN only requires the letter B to be entered to be valid at this time
                if (BuildConfig.HAS_BRIDGE_SN) {
                    isBridgeSNValidForNextButton();
                }
                // Doesn't require Bridge SN, continue
                else {
                    setVisible(bridgeSerialNumber, View.GONE);
                    pageData().nextButton.setVisibility(View.VISIBLE);
                }
            } else {
                fsnOrCsnInvalid();
            }
        }
    }

    private void fsnOrCsnInvalid() {
        // CSN and FSN aren't valid, don't want valid BSN to allow to continue
        if (BuildConfig.HAS_BRIDGE_SN) {
            pageData().nextButton.setVisibility(View.GONE);
        }
        // CSN and FSN aren't valid, BSN doesn't matter
        else {
            setVisible(bridgeSerialNumber, View.GONE);
            pageData().nextButton.setVisibility(View.GONE);
        }
    }

    private void isBridgeSNValidForNextButton() {
        // Bridge SN not entered, don't show Next button
        if (pageData().bridgeSerialNumberEntered.isEmpty()) {
            pageData().nextButton.setVisibility(View.GONE);
        }
        // Bridge SN entered, show Next button
        else {
            pageData().nextButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Returns true if CSN length correct, CSN valid, FSN length correct, and FSN valid.
     * 
     * @return      true if all four values are correct, false if not.
     */
    private boolean areFSNandCSNValid() {
        return pageData().consoleSerialNumberEntered.length() > ProductConfig.getConsoleType(
                pageData().machineTypeSelection).getConsolePrefix().length()
                && suffixValid(pageData().consoleSerialNumberEntered.substring(ProductConfig.getConsoleType(
                        pageData().machineTypeSelection).getConsolePrefix().length()))
                && pageData().frameSerialNumberEntered.length() > FrameConfig.MINIMUM_FRAME_SERIAL_LENGTH
                && suffixValid(FrameConfigManager.getInstance().getFrameSerialNumberSuffix(
                        pageData().frameSerialNumberEntered));
    }
}