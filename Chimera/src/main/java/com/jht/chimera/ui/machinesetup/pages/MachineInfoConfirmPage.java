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
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.datamatrixlib.DataMatrix;

public class MachineInfoConfirmPage extends MachineSetupPageBase {

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
            pageData().consoleSerialNumberConfirm = SerialNumberUtil.fixSNToBeAlphanumeric(s.toString());
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
            pageData().frameSerialNumberConfirm = SerialNumberUtil.fixSNToBeAlphanumeric(s.toString());
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
            pageData().bridgeSerialNumberConfirm = SerialNumberUtil.fixSNToBeAlphanumeric(s.toString());
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
                pageData().confirmButton.setVisibility(View.GONE);
            }
            pageData().clubIdConfirm = clubId;
            checkNextButton();
        }
    };

    @SuppressLint("InflateParams")
    public MachineInfoConfirmPage(final MachineSetupPageData pageData) {
        super(pageData);
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.machine_information, null);
        container.addView(view);
        consoleSerialNumber = view.findViewById(R.id.machine_setup_console_serial_number);
        frameSerialNumber = view.findViewById(R.id.machine_setup_frame_serial_number);
        clubId = view.findViewById(R.id.machine_setup_club_id);

        view.findViewById(R.id.machine_setup_import_serial_numbers).setVisibility(View.GONE);

        clubId.setOnIMEAction(action -> {
            if (action == EditorInfo.IME_ACTION_DONE) {
                if (pageData().nextButton.getVisibility() == View.VISIBLE) {
                    pageData().nextButton.callOnClick();
                }
            }
            return false;
        });

        UserTextInput ftsBridgeSN = view.findViewById(R.id.machine_setup_bridge_serial_number);
        if (BuildConfig.HAS_BRIDGE_SN) {
            if (ftsBridgeSN != null) {
                bridgeSerialNumber = ftsBridgeSN;
            }
        } else {
            setVisible(ftsBridgeSN, View.GONE);
        }

        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        pageData().title.setText(R.string.machine_information_confirm_tc);
        pageData().previousButton.setVisibility(View.VISIBLE);
        pageData().nextButton.setVisibility(View.VISIBLE);

        if(!pageData().consoleSerialNumberEntered.equals(pageData().consoleSerialNumberConfirm)) {
            if (!ProductConfig.isCommercial()) pageData().consoleSerialNumberConfirm = pageData().consolePrefix;
        }
        if(!pageData().frameSerialNumberEntered.equals(pageData().frameSerialNumberConfirm)) {
            pageData().frameSerialNumberConfirm = pageData().framePrefix;
            if (!pageData().frameSerialNumberEntered.startsWith(pageData().framePrefix)) {
                pageData().frameSerialNumberConfirm = (pageData().frameSerialNumberEntered.substring(0, 3));
                FrameConfigEnum.findSupportedFrameConfig((frame, frameConfig) ->{
                    if (frameConfig.framePrefixValid(pageData().frameSerialNumberEntered)) {
                        for (String framePrefix : frameConfig.getFramePrefixes()) {
                            if (pageData().frameSerialNumberEntered.startsWith(framePrefix)) {
                                pageData().frameSerialNumberConfirm = framePrefix;
                                return true;
                            }
                        }
                        return true;
                    }
                    return false;
                });
            }
        }

        if (ProductConfig.isCommercial()) {
            consoleSerialNumber.setInputProperties(consoleSerialNumber.getInputProperties().inputValue(pageData().consoleSerialNumberConfirm));
        } else {
            consoleSerialNumber.setInputProperties(consoleSerialNumber.getInputProperties().inputValue(pageData().consoleSerialNumberConfirm).minChars(ProductConfig.getConsoleType(pageData().machineTypeSelection).getConsolePrefix().length()));
        }
        frameSerialNumber.setInputProperties(frameSerialNumber.getInputProperties().inputValue(pageData().frameSerialNumberConfirm).minChars(FrameConfigManager.getInstance().getFramePrefixLength()));

        if (BuildConfig.HAS_BRIDGE_SN) {
            if (bridgeSerialNumber != null) {
                bridgeSerialNumber.setInputProperties(bridgeSerialNumber.getInputProperties().inputValue(pageData().bridgeSerialNumberConfirm).minChars(1).maxChars(40));
                bridgeSerialNumber.addTextWatcher(bridgeSerialNumberWatcher);
            }
        } else {
            setVisible(bridgeSerialNumber, View.GONE);
        }

        clubId.setText(pageData().clubIdConfirm + "");

        shown = true;
        checkNextButton();

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
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(pageData().consoleSerialNumberEntered));
        DataMatrix.putString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(pageData().frameSerialNumberEntered));
        DataMatrix.putInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT, pageData().clubIdEntered);

        if (BuildConfig.HAS_BRIDGE_SN) {
            DataMatrix.putString(SettingKeys.AssetManagement.DATA_BRIDGE_SERIAL_STRING, SerialNumberUtil.fixSNToBeAlphanumeric(pageData().bridgeSerialNumberEntered));
        } else {
            setVisible(bridgeSerialNumber, View.GONE);
        }

        return true;
    }

    private void checkNextButton() {
        if (shown) {
            if (matchesPreviousSNs()) {
                if (BuildConfig.HAS_BRIDGE_SN) {
                    verifyBridgeSNMoveForward();
                }
                // if no Bridge SN and they do match
                else {
                    setVisible(bridgeSerialNumber, View.GONE);
                    pageData().confirmButton.setVisibility(View.VISIBLE);
                    pageData().nextButton.setVisibility(View.GONE);
                }
            } else {
                if (BuildConfig.HAS_BRIDGE_SN) {
                    verifyBridgeSNKeepBlocked();
                }
                // if no Bridge SN and they don't match
                else {
                    setVisible(bridgeSerialNumber, View.GONE);
                    pageData().confirmButton.setVisibility(View.GONE);
                    pageData().nextButton.setVisibility(View.GONE);
                }
            }

            boolean validCSN = (SerialNumberUtil.fixSNToBeAlphanumeric(pageData().consoleSerialNumberEntered)
                    .compareTo(SerialNumberUtil.fixSNToBeAlphanumeric(pageData().consoleSerialNumberConfirm))) == 0;

            consoleSerialNumber.setIsValid(validCSN, "");

            boolean validFSN = (SerialNumberUtil.fixSNToBeAlphanumeric(pageData().frameSerialNumberEntered)
                    .compareTo(SerialNumberUtil.fixSNToBeAlphanumeric(pageData().frameSerialNumberConfirm))) == 0;

            frameSerialNumber.setIsValid(validFSN, "");

            if (BuildConfig.HAS_BRIDGE_SN) {
                if (bridgeSerialNumber != null) {
                    // valid if Bridge SN match
                    bridgeSerialNumber.setIsValid(pageData().bridgeSerialNumberEntered.compareTo(pageData().bridgeSerialNumberConfirm) == 0, "");
                }
            } else {
                setVisible(bridgeSerialNumber, View.GONE);
            }

            clubId.setIsValid(pageData().clubIdEntered == pageData().clubIdConfirm, "");
        }
    }

    private void verifyBridgeSNMoveForward() {
        // if required Bridge SN and they don't match
        if (pageData().bridgeSerialNumberEntered.compareTo(pageData().bridgeSerialNumberConfirm) != 0) {
            pageData().confirmButton.setVisibility(View.GONE);
            pageData().nextButton.setVisibility(View.GONE);
        }
        // if required Bridge SN and they do match
        else {
            pageData().confirmButton.setVisibility(View.VISIBLE);
            pageData().nextButton.setVisibility(View.GONE);
        }
    }

    private void verifyBridgeSNKeepBlocked() {
        // if required Bridge SN and they do match, but CSN or FSN don't
        if (pageData().bridgeSerialNumberEntered.compareTo(pageData().bridgeSerialNumberConfirm) == 0) {
            pageData().confirmButton.setVisibility(View.GONE);
            pageData().nextButton.setVisibility(View.GONE);
        }
        // if required Bridge SN and they don't match, nor does CSN or FSN
        else {
            pageData().confirmButton.setVisibility(View.GONE);
            pageData().nextButton.setVisibility(View.GONE);
        }
    }

    /**
     * Checks whether SNs entered on Machine Information page of FTS match values on Confirm Machine Information page.
     *
     * @return      true if values match, false if they do not
     */
    private boolean matchesPreviousSNs() {
        return pageData().consoleSerialNumberEntered.compareTo(pageData().consoleSerialNumberConfirm) == 0
                && pageData().frameSerialNumberEntered.compareTo(pageData().frameSerialNumberConfirm) == 0
                && pageData().clubIdEntered == (pageData().clubIdConfirm);
    }
}