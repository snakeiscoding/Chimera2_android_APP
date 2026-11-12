package com.jht.chimera.ui.mainapp.carousel;

import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured.FeaturedItem;
import com.jht.datamatrixlib.DataMatrix;

import java.lang.reflect.Field;
import java.util.List;

// Base idea taken from https://github.com/DevExchanges/Carousel-Layout-by-ViewPager
public class CarouselPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
    private final String TAG = "CarouselPagerAdapter";

    public final static int LOOP_COUNT = 1000;

    public final static float BIG_SCALE = 1.0f;
    public final static float SMALL_SCALE = 0.7f;

    public final static float BIG_SCALE_2 = 0.75f;
    public final static float SMALL_SCALE_2 = 0.525f;

    public final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;
    public final static float DIFF_SCALE_2 = BIG_SCALE_2 - SMALL_SCALE_2;
    private final FragmentManager fragmentManager;
    private float scale;
    private final List<FeaturedItem> items;
    private OnClickListener onClickListener;
    private final boolean clickEnabled = true;

    private final boolean isChangeWorkout;

    public interface OnClickListener {
        void onClick(int position);
    }

    private float getBigScale() {
        return isChangeWorkout ? BIG_SCALE_2 : BIG_SCALE;
    }

    private float getSmallScale() {
        return isChangeWorkout ? SMALL_SCALE_2 : SMALL_SCALE;
    }

    private float getDiffScale() {
        return isChangeWorkout ? DIFF_SCALE_2 : DIFF_SCALE;
    }

    public CarouselPagerAdapter(FragmentManager fm, List<FeaturedItem> items) {
        super(fm);
        this.fragmentManager = fm;
        this.items = items;
        isChangeWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // make the first pager bigger than others
        try {
            if (position == items.size())
                scale = getBigScale();
            else
                scale = getSmallScale();

            position = position % items.size();

        } catch (Exception e) {
            Log.w(TAG, e);
        }

        FeaturedItem item = items.get(position);
        CarouselItemFragment carouselItemFragment = (CarouselItemFragment) CarouselItemFragment.newInstance(fragmentManager, position, scale, item.getSafeBackgroundItem(position), item.title, item.description, item.startButtonText, item.workoutId);
        carouselItemFragment.setOnClickListener(index -> {
            if (onClickListener != null && clickEnabled) onClickListener.onClick(index);
        });
        return carouselItemFragment;
    }

    @Override
    public int getCount() {
        int count = 0;
        try {
            count = items.size() * LOOP_COUNT;
        } catch (Exception e) {
            Log.e(TAG, "Caught exception " + e.getMessage());
        }
        return count;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        try {
            if (positionOffset >= 0f && positionOffset <= 1f) {
                CarouselLinearLayout cur = getRootView(position);
                CarouselLinearLayout next = getRootView(position + 1);
                if (cur != null) cur.setScaleBoth(getBigScale() - getDiffScale() * positionOffset);
                if (next != null) next.setScaleBoth(getSmallScale() + getDiffScale() * positionOffset);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @SuppressWarnings("ConstantConditions")
    @Nullable
    private CarouselLinearLayout getRootView(int position) {
        try {
            Field mFragmentsField = FragmentStatePagerAdapter.class.getDeclaredField("mFragments");
            mFragmentsField.setAccessible(true);
            List<Fragment> fragments = (List<Fragment>) mFragmentsField.get(this);
            Fragment fragment = fragments.get(position);
            return fragment != null ? fragment.getView().findViewById(R.id.root_container) : null;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        int index = this.items.indexOf(object);
        return index == -1 ? POSITION_NONE : index;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    // Add to prevent
    @Override
    public Parcelable saveState() {
        return null;
    }
}
