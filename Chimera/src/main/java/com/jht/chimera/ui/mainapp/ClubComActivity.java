package com.jht.chimera.ui.mainapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.widget.RelativeLayout;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.RealScreen;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ChimeraEnums;

public class ClubComActivity extends WebAppActivity {
    private static final String TAG = "ClubComActivity";
    private static final String INTERFACE_NAME = "matrix_external"; //THIS CANNOT CHANGE!!!! It is the link between the ClubCom javascript and our android code! NON-NLS

    JHTTimer mTimer = new JHTTimer(20 * 1000);
    WindowManager mWindowManager;
    LockUIPanel mLockUIPanel;
    boolean mIsLockPanelAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLockUIPanel = new LockUIPanel(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCurrentWebView.mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mCurrentWebView.mWebView.setBackgroundColor(Color.BLACK);
        mCurrentWebView.mWebView.addJavascriptInterface(new CCJavascriptInterface(), INTERFACE_NAME);

        if (mCurrentWebView.mUrl.contains("preroll")) {
            mTimer.setOnTimer(() -> {
                LaunchAppUtil.launchWorkoutScreen();
                unlockUI();
            });
            lockUI();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unlockUI();
    }

    void lockUI() {
        try {
            mWindowManager.addView(mLockUIPanel, mLockUIPanel.getPanelParams());
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        mIsLockPanelAdded = true;
        mTimer.start();
    }

    void unlockUI() {
        try {
            if (mIsLockPanelAdded) mWindowManager.removeView(mLockUIPanel);
        } catch (Exception e) {
        }
        mIsLockPanelAdded = false;
        mTimer.stop();
    }

    public class CCJavascriptInterface {

        @JavascriptInterface
        public void hide() {
            Log.d(TAG, "hide() called");
            LaunchAppUtil.launchWorkoutScreen();
            unlockUI();
        }

        @JavascriptInterface
        public void destroy() {
            Log.d(TAG, "destroy() called");
        }

        @JavascriptInterface
        public void unlockButtons() {
            Log.d(TAG, "unlockButtons() called");
            unlockUI();
        }

    }

    public class LockUIPanel extends RelativeLayout {

        public LockUIPanel(Context context) {
            super(context);
            initialize(context);
        }

        public LockUIPanel(Context context, AttributeSet attrs) {
            super(context, attrs);
            initialize(context);
        }

        public LockUIPanel(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            initialize(context);
        }

        private void initialize(Context context) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View grayLayout = inflater.inflate(R.layout.gray_layout, this, true);

            // app width with normal panels is calculated now - there should be easier way to block panels, maybe a command to UI service to toggle a lock state?
            int width = (RealScreen.widthPixels - (int) getResources().getDimension(R.dimen.app_container_width)) / 2;
            View left = grayLayout.findViewById(R.id.left_background_view);
            View right = grayLayout.findViewById(R.id.right_background_view);
            if (left != null) left.getLayoutParams().width = width;
            if (right != null) right.getLayoutParams().width = width;
        }

        public WindowManager.LayoutParams getPanelParams() {
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);

            viewParams.gravity = Gravity.NO_GRAVITY;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        }
    }
}
