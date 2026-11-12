package com.jht.chimera.lib.widget.indicator;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TopBarKeys;
import com.jht.datamatrixlib.DataMatrix;

public class TopBarBusyIndicator extends ImageView {

    Animation mSpinClockwise;
    boolean mAnimationStarted = false;
    DataMatrix mDataMatrix = new DataMatrix();

    public TopBarBusyIndicator(Context context) {
        super(context);
        init();
    }

    public TopBarBusyIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.ic_spinner));

        mSpinClockwise = AnimationUtils.loadAnimation(getContext(),
                R.anim.spinning_clockwise);

        stopProgress();

    }

    void startProgress() {
        if (!mAnimationStarted) {
            this.startAnimation(mSpinClockwise);
            mAnimationStarted = true;
        }

        this.setVisibility(View.VISIBLE);
    }

    void stopProgress() {
        if (mAnimationStarted) {
            mSpinClockwise.cancel();
            mSpinClockwise.reset();
            this.setAnimation(null);
            mAnimationStarted = false;
        }

        this.setVisibility(View.GONE);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        //Check for editing, otherwise will not render preview in Android Studio
        if(!isInEditMode()) {
            if (visibility == VISIBLE) {
                mDataMatrix.finish();
                mDataMatrix.createEventAdapter(TopBarKeys.ACTION_SHOW_BUSY_INDICATOR, keyReturnedBack -> startProgress());

                mDataMatrix.createEventAdapter(TopBarKeys.ACTION_HIDE_BUSY_INDICATOR, keyReturnedBack -> stopProgress());
            } else {
                stopProgress();
                mDataMatrix.finish();
            }
        }
    }

}
