package com.jht.chimera.lib.widget.keypad;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.IInputConfiguration;
import com.jht.androiduiwidgets.keypad.Keypad;

import org.jetbrains.annotations.NonNls;

@SuppressLint("MissingPermission")
public class PasscodeKeypadDlg {
    @NonNls private static final String TAG = "PasscodeKeypadDlg";

    public interface IPasscodeValue {
        void onKeypadValue(String value);
    }
    static Dialog sDialog;

    public static void showDlg(Context context, @NonNull IPasscodeValue iKeypadPanelCallback) {
        if (sDialog != null && sDialog.isShowing()) return;

        try {
            Keypad keypad = new Keypad(context);
            IInputConfiguration configuration = keypad.getSettings();
            configuration.isPassword(true);
            configuration.resolution(1);
            configuration.max(999999999);
            configuration.min(0);
            configuration.value("");
            keypad.setKeypadSettings(configuration);

            keypad.setOnConfirm( view -> iKeypadPanelCallback.onKeypadValue(keypad.getKeypadValue()) );


            sDialog = new Dialog(context);
            sDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            sDialog.setContentView(keypad);
            sDialog.setCanceledOnTouchOutside(true);
            sDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.argb(100, 255, 255, 255)));

            // make dialog some percentage of screen
            WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = window.getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            int widthPixels = size.x;
            int heightPixels = size.y;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(sDialog.getWindow().getAttributes());
            lp.width = (int) (widthPixels * .40);
            lp.height = (int) (heightPixels * .90);
            sDialog.getWindow().setAttributes(lp);

            sDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
            sDialog.show();

        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void closeDialog(){
        sDialog.dismiss();
    }


}
