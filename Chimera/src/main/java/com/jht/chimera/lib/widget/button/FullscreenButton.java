package com.jht.chimera.lib.widget.button;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.datamatrixlib.DataMatrix;


public class FullscreenButton extends SelectronButton {

    DataMatrix mDataMatrix = new DataMatrix();
    boolean isFullscreen = false;
    int mIconExandId = R.drawable.ic_expand_white;
    int mIconShrinkId = R.drawable.ic_shrink_white;

    private Handler handler = new Handler();

    public FullscreenButton(Context context) {
        super(context);
        init();
    }

    public FullscreenButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FullscreenButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setIconId(int iconExpandId, int iconShrinkId) {
        mIconShrinkId = iconShrinkId;
        mIconExandId = iconExpandId;
    }

    void init() {
        setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isFullscreen) {
                    DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
                } else {
                    DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.FULLSCREEN);
                }
            }
            return false;
        });
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            mDataMatrix.finish();
            mDataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, value -> {
                isFullscreen = (value == UiServiceKeys.WorkoutPanelState.FULLSCREEN);
                if (isFullscreen) {
                    setIcon(mIconShrinkId);
                } else {
                    setIcon(mIconExandId);
                }
            });

        } else {
            mDataMatrix.finish();
        }
    }


}
