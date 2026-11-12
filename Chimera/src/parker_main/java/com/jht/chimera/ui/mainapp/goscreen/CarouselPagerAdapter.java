package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.jht.chimera.R;

import java.lang.reflect.Field;
import java.util.List;

// Base idea taken from https://github.com/DevExchanges/Carousel-Layout-by-ViewPager
public class CarouselPagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {
    private final String TAG = "CarouselPagerAdapter";
    public static class CarouselItemData {
        /** URI of the main image */
        public Uri uri;
        /** Text that will be shown over the image, if any */
        public @Nullable String caption;
        /** Unique identifier for the item; will be put into contentDescription for automated testing purposes. */
        public String slug;
        public CarouselItemData(Uri uri, String caption, String slug) {
            this.uri = uri;
            this.caption = caption;
            this.slug = slug;
        }
    }

    public final static int LOOP_COUNT = 1000;
    public final static float BIG_SCALE = 1.0f;
    public final static float SMALL_SCALE = 0.7f;
    public final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;
    private Context context;
    private FragmentManager fragmentManager;
    private float scale;
    private String viewPagerId;
    private List<CarouselItemData> items;
    private OnClickListener onClickListener;
    private boolean clickEnabled = true;

    public interface OnClickListener {
        void onClick(int position);
    }

    public CarouselPagerAdapter(Context context, FragmentManager fm, int viewPagerId, List<CarouselItemData> items) {
        super(fm);
        this.fragmentManager = fm;
        this.context = context;
        this.viewPagerId = String.valueOf(viewPagerId);
        this.items = items;
    }

    public void setClickEnabled(boolean enabled) { this.clickEnabled = enabled; }

    @Override
    public Fragment getItem(int position) {
        // make the first pager bigger than others
        try {
            if (position == items.size())
                scale = BIG_SCALE;
            else
                scale = SMALL_SCALE;

            position = position % items.size();

        } catch (Exception e) {
            Log.w(TAG, e);
        }

        CarouselItemData item = items.get(position);
        CarouselItemFragment carouselItemFragment = (CarouselItemFragment) CarouselItemFragment.newInstance(context, position, scale, item.uri, item.caption, item.slug);
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
        }
        return count;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        try {
            if (positionOffset >= 0f && positionOffset <= 1f) {
                CarouselLinearLayout cur = getRootView(position);
                CarouselLinearLayout next = getRootView(position + 1);
                if (cur != null) cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
                if (next != null) next.setScaleBoth(SMALL_SCALE + DIFF_SCALE * positionOffset);
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
            List<Fragment> fragments = (List<Fragment>)mFragmentsField.get(this);
            Fragment fragment = fragments.get(position);
            return fragment != null ? fragment.getView().findViewById(R.id.root_container) : null;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
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
}
