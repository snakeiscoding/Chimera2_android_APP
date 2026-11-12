package com.jht.chimera.lib.widget.indicator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageButton;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.ConsoleConfigKeys;
import com.jht.chimera.lib.common.datamatrixkeys.LoggerKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.widget.dialog.ProductDashboardHost;
import com.jht.datamatrixlib.DataMatrix;

import java.io.File;


public class TopBarLogo extends AppCompatImageButton {

    GestureDetector mGestureDetector;
    private JHTTimer logoPressTimer;
    private final Handler mHandler = new Handler();
    DataMatrix mDataMatrix = new DataMatrix();

    public TopBarLogo(Context context) {
        super(context);
        init();
    }

    public TopBarLogo(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public TopBarLogo(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressLint("ClickableViewAccessibility")
    void init() {
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            mDataMatrix.createEventAdapter(ConsoleConfigKeys.EVENT_CONSOLE_THEME_READY, s -> {
                // Delay so that we can make sure the logo is ready to be read.
                mHandler.postDelayed(this::updateLogo, 2000);
            });

            updateLogo();

            logoPressTimer = new JHTTimer(2000, () -> mHandler.post(ProductDashboardHost::show));

            mGestureDetector = new GestureDetector(getContext(), new MyGestureListener());
            setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    new Thread(() -> logoPressTimer.start()).start();
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    logoPressTimer.stop();
                }
                return mGestureDetector.onTouchEvent(event);
            });
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (visibility == VISIBLE) {
                updateLogo();
            } else {
                logoPressTimer.stop();
            }
        }
    }

    private void updateLogo() {
        //TODO Offload bitmap decoding from main ui thread
        boolean customLogoSet = false;
        if(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_LOGO_BOOL)) {
            File logoDir = new File(getContext().getFilesDir(), "settings/theme/logo");
            if (logoDir.exists()) {

                File[] files = logoDir.listFiles();
                if (files != null && files.length >= 1) {
                    File logoFile = files[0];
                    Bitmap bitmap = BitmapFactory.decodeFile(logoFile.getPath(), new BitmapFactory.Options());
                    if (bitmap != null) {
                        setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                        customLogoSet = true;
                    }
                }
            }
        }

        if (!customLogoSet) {
            setImageResource(R.drawable.ic_logo_matrix);
        }
    }


    private static class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            DataMatrix.sendEvent(LoggerKeys.ACTION_SHOW_HAL_DLG);
            return true;
        }
    }
}
