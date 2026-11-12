package com.jht.chimera.ui.machinesettings.pages.tv;

import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

abstract public class EngineeringPageBase extends SettingPageBase {

    protected void linkCheckbox(Object key, int checkbox, CompoundButton.OnCheckedChangeListener callback) {
        CheckBox view = page().findViewById(checkbox);
        view.setChecked(DataMatrix.getBoolean(key));
        view.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DataMatrix.putBoolean(key, isChecked);
            if (callback != null) {
                callback.onCheckedChanged(buttonView, isChecked);
            }
        });
    }

    protected UserTextInput linkEditInt(Object key, int edit, UserTextInput.IOnTextChanged onTextChanged) {
        UserTextInput input = page().findViewById(edit);

        input.setText(DataMatrix.getInt(key) + "");
        input.setOnTextChanged(s -> {
            DataMatrix.putInt(key, CommonRoutines.parseInt(s));
            if (onTextChanged != null) {
                onTextChanged.onTextChanged(s);
            }
        });
        return input;
    }


    private boolean checkHDCPKey(){
        ExecuteAsRoot checkHDCP = new ExecuteAsRoot();
        checkHDCP.addCommand("i2cfwupdate_leo -c 1 -d -a 1 0x2b");
        StringBuilder checkResult = new StringBuilder();
        checkHDCP.execute(false, true, checkResult);
        return -1 != checkResult.indexOf("Key exist");
    }

    protected <E extends Enum> OptionSelectorItem linkOption(Object key, String[] optionLabels, E[] options, int optionResId, OptionSelectorItem.IOnOptionSelected onOptionSelected) {

        OptionSelectorItem selector = page().findViewById(optionResId);
        selector.setOptions(new OptionManager(optionLabels, CommonRoutines.convertStringToOrdinal(options, DataMatrix.getString(key))));
        selector.setOnOptionSelected((pos, option) -> {
            if (ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Chimera_2)) {
                SettingKeys.TVSource selectedSource = SettingKeys.TVSource.fromName(options[selector.getSelectedPosition()].name());
                if (selectedSource != null && selectedSource.isLG()) {
                    if (!checkHDCPKey()){
                        DialogFactory.message(selector.getRootView(), ContextUtil.getString(R.string.check_hdcp_key_tc), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        selector.setOptions(new OptionManager(optionLabels, CommonRoutines.convertStringToOrdinal(options, DataMatrix.getString(key))));
                        return;
                    }
                }
            }
            DataMatrix.putString(key, options[pos].name());
            if (onOptionSelected != null) {
                onOptionSelected.onOptionSelected(pos, option);
            }
        });
        return selector;
    }

}
