package com.jht.mastboard.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.jht.mastboard.R;
import com.jht.mastboard.lib.PanelSide;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

public class LCDDebugView {

    Context context;
    ViewGroup floatingView;
    View rootView;

    public LCDDebugView(Context context, View rootView) {
        this.context = context;
        this.rootView = rootView;
    }

    public void show(PanelSide panelSide) {
        // inflate view
        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = (ViewGroup) inflater.inflate(R.layout.lcd_debug, null);


        // add float view to Window Manager
        WindowManager.LayoutParams floatWindowLayoutParam = new WindowManager.LayoutParams(
                240,
                360,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        floatWindowLayoutParam.gravity = Gravity.CENTER;

        if (panelSide == PanelSide.LEFT) {
            ((TextView) floatingView.findViewById(R.id.text_view)).setText("LEFT");
            floatWindowLayoutParam.x = -300;
            floatWindowLayoutParam.y = 300;
        } else {
            ((TextView) floatingView.findViewById(R.id.text_view)).setText("RIGHT");
            floatWindowLayoutParam.x = 300;
            floatWindowLayoutParam.y = 300;
        }
        windowManager.addView(floatingView, floatWindowLayoutParam);

        // handle dragging view
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            final WindowManager.LayoutParams layoutParam = floatWindowLayoutParam;
            double x, y, px, py;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = layoutParam.x;
                        y = layoutParam.y;
                        px = event.getRawX();
                        py = event.getRawY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        layoutParam.x = (int) ((x + event.getRawX()) - px);
                        layoutParam.y = (int) ((y + event.getRawY()) - py);
                        windowManager.updateViewLayout(floatingView, layoutParam);
                        break;
                }
                return false;
            }
        });

        // pass touch event
        floatingView.findViewById(R.id.image_view).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (rootView != null) rootView.dispatchTouchEvent(event);
                return false;
            }
        });

    }

    public void hide() {
        if (floatingView != null) {
            WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    public DebugView getDebugView() {
        if (floatingView != null) {
            return floatingView.findViewById(R.id.image_view);
        }
        return null;
    }
}
