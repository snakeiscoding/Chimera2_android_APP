package com.jht.chimera.lib.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;

import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.context.ContextUtil;

import org.jetbrains.annotations.NonNls;

public class RemoveFocusDlg extends LinearLayout {
    @NonNls private static final String TAG = "RemoveFocusDlg";
    static Dialog sDialog;

    public RemoveFocusDlg(Context context) {
        super(context);
    }

    public static void showDlg() {
        if (sDialog != null && sDialog.isShowing()) return;

        try {
            Context context = ContextUtil.getApplicationContext();
            sDialog = new Dialog(context);
            final RemoveFocusDlg debugDlg = new RemoveFocusDlg(context);
            sDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            sDialog.setContentView(debugDlg);
            sDialog.setCanceledOnTouchOutside(true);
            sDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(0, 255, 255, 255)));
            sDialog.setOnShowListener(dialog -> new Handler().postDelayed(() -> sDialog.dismiss(), 100));
            sDialog.getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
            sDialog.show();

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

}
