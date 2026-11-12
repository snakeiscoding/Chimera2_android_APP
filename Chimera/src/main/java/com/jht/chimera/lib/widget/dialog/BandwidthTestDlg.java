package com.jht.chimera.lib.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.service.logger.LoggerService;
import com.jht.chimera.ui.mainapp.PopupWebView;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

public class BandwidthTestDlg extends ConstraintLayout {
    @NonNls private static final String TAG = "BandwidthTestDlg";

    static Dialog sDialog;


    public BandwidthTestDlg(Context context) {
        super(context);
        CommonRoutines.initWebWidget(context, this, R.layout.bandwidth_test_dialog, LoggerService.TAG, true);
        findViewById(R.id.web_view_container_popup_container).setOnClickListener(v -> sDialog.dismiss());

        reload();
    }

    private void reload() {
        PopupWebView popupView = findViewById(R.id.web_view_container_popup);
        popupView.getSettings().setDomStorageEnabled(true);
        // TODO Do we want to make a key for change
        popupView.allowExternalLinks(false);
        popupView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.132 Safari/537.36");
        popupView.loadWebsite(DataMatrix.getString(SettingKeys.Experimental.DATA_BANDWIDTH_TEST_URL_STRING));
    }


    public static void showDlg() {
        if (sDialog != null && sDialog.isShowing()) return;

        try {
            Context context = ContextUtil.getApplicationContext();
            sDialog = new Dialog(context);
            final BandwidthTestDlg bandwidthTestDlg = new BandwidthTestDlg(context);
            sDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            sDialog.setContentView(bandwidthTestDlg);
            sDialog.setCanceledOnTouchOutside(true);
            sDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // make dialog 90% of screen
            WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = window.getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            int widthPixels = (int)(size.x * .70);
            int heightPixels = (int)(size.y * .70);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(sDialog.getWindow().getAttributes());
            lp.width = widthPixels;
            lp.height = heightPixels;
            //lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            sDialog.getWindow().setAttributes(lp);

            sDialog.getWindow().setType(ChimeraEnums.TYPE_SYSTEM_ALERT);
            sDialog.show();

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static boolean isShowing() {
        if (sDialog == null) return false;
        return sDialog.isShowing();
    }

}
