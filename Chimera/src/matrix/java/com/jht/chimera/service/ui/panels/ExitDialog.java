package com.jht.chimera.service.ui.panels;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.RealScreen;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.datamatrixlib.DataMatrix;

// Based on AnalyticsDialog and SimpleYesNoMessageDialog
// Need exit confirmation dialog that can sit on top of third party app
public class ExitDialog extends Dialog {

    private final DataMatrix dataMatrix = new DataMatrix();

    public interface IExitCallback {
        void onYesNo(boolean yes);
    }
    private IExitCallback onYesNo;

    public ExitDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float screenMultiplier = 1.0f;
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        if(ProductConfig.isCommercial()) {
            setContentView(R.layout.message_dialog_yes_no);
            ((TextView)(findViewById(R.id.message_text_content))).setText(R.string.exit_confirm_pg);
        }
        else {
            setContentView(R.layout.dialog_confirm);
            findViewById(R.id.messageBoxCloseImageButton).setOnClickListener(v -> dismiss());
            screenMultiplier = 0.55f;
            setCanceledOnTouchOutside(true);
        }

        findViewById(com.jht.androiduiwidgets.R.id.message_dialog_cancel).setOnClickListener(v -> {
            dismiss();
            if(onYesNo != null) {
                onYesNo.onYesNo(false);
            }
        });
        findViewById(com.jht.androiduiwidgets.R.id.message_dialog_ok).setOnClickListener(v -> {
            dismiss();
            if(onYesNo != null) {
                onYesNo.onYesNo(true);
            }
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(getWindow().getAttributes());
        lp.width = (int) (RealScreen.widthPixels * screenMultiplier);
        lp.height = (int) (RealScreen.heightPixels * screenMultiplier);
        getWindow().setAttributes(lp);

        dataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, state -> {
            if (state == UiServiceKeys.WorkoutPanelState.IDLE) {
                dismiss();
            }
        }, false);

    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        dataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, state -> {
            if (state == UiServiceKeys.WorkoutPanelState.IDLE) {
                dismiss();
            }
        }, false);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dataMatrix.finish();
    }

    public void init(IExitCallback onYesNo) {
        this.onYesNo = onYesNo;
    }

}
