package com.jht.chimera.lib.widget.dialog;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.widget.DataMatrixCheckBox;

public class LoggingOptions extends DialogBase {

    public LoggingOptions(Context context) {
        this(context, null, 0);
    }

    public LoggingOptions(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoggingOptions(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ((DataMatrixCheckBox)findViewById(R.id.log_mcu_messages)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_MCU_PACKETS_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_csafe_messages)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_CSAFE_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_urls)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_URLS_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_lcb_messages)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_LCB_PACKETS_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_gymkit_messages)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_GYMKIT_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_ptp)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_PTP_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_dapi_messages)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_DAPI_PACKETS_BOOL);
        ((DataMatrixCheckBox)findViewById(R.id.log_rfid_messages)).linkCheckBox(SettingKeys.Experimental.DATA_LOG_RFID_BOOL);
        findViewById(R.id.cornerCloseButton).setOnClickListener(view -> hide());
        setCloseOnOutsideTouch(true);

    }

    @Override
    protected int getLayoutResId() { return R.layout.logging_options; }


}
