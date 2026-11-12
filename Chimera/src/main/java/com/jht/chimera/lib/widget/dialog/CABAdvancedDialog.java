package com.jht.chimera.lib.widget.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.tv.tuner.CABTuner;
import com.jht.datamatrixlib.DataMatrix;

/**
 * This class manages an editable field that the requires a keyboard or keypad.  We extend the
 * base Android EditText so that we can get labels and add units in the field.  The floating hint
 * is nice in the support library, but we cannot style the label as we would like.  This class
 * also takes care of validating the input using XML attributes for validation and can link to a field
 * in the model layer.
 *
 *
 */
public class CABAdvancedDialog extends DialogBase {


    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     */
    public CABAdvancedDialog(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     * @param attrs     Attributes that are set from the view's XML.
     */
    public CABAdvancedDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the option selector item.
     *
     * @param context   Current context for the view.
     * @param attrs     Attributes that are set from the view's XML.
     * @param defStyleAttr  Default style for the view.
     */
    public CABAdvancedDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @Override
    protected int getLayoutResId() { return R.layout.cab_advanced; }

    public void init(CABTuner cabTuner) {
        if(cabTuner.tuner() instanceof com.jht.serialport.peripheral.tuner.cab.CABTuner) {
            com.jht.serialport.peripheral.tuner.cab.CABTuner tuner = (com.jht.serialport.peripheral.tuner.cab.CABTuner)cabTuner.tuner();
            findViewById(R.id.message_dialog_ok).setOnClickListener(view -> hide(true));
            ((CheckBox)findViewById(R.id.DATA_CAB_CLOSED_CAPTIONING_BOOL)).setChecked(DataMatrix.getBoolean(SettingKeys.TV.DATA_CAB_CLOSED_CAPTIONING_BOOL));
            ((CheckBox)findViewById(R.id.DATA_CAB_CLOSED_CAPTIONING_BOOL)).setOnCheckedChangeListener((view, checked) -> DataMatrix.putBoolean(SettingKeys.TV.DATA_CAB_CLOSED_CAPTIONING_BOOL, checked));
            findViewById(R.id.cab_volume_set).setOnClickListener(view -> tuner.setTunerVolume(Integer.parseInt(((UserTextInput)findViewById(R.id.cab_volume)).getText())));
            findViewById(R.id.cab_volume_gain_set).setOnClickListener(view -> tuner.setInternalGain(com.jht.serialport.peripheral.tuner.cab.CABTuner.CAB_INTERNAL_GAIN.values()[(((OptionSelectorItem)findViewById(R.id.cab_volume_gain)).getSelectedPosition())]));
            findViewById(R.id.cab_stb_set).setOnClickListener(view -> tuner.setSTBType(Integer.parseInt(((UserTextInput)findViewById(R.id.cab_stb_setting)).getText())));
        }
    }


}
