package com.jht.chimera.ui.mainapp.carousel;

import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.ui.mainapp.goscreen.BitmapAsyncLoadTask;
import com.jht.datamatrixlib.DataMatrix;

public class CarouselItemFragment extends Fragment {

    private static final String POSITON = "position";
    private static final String SCALE = "scale";
    private static final String URI = "uri";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String START_BUTTON_TEXT = "startButtonText";
    private static final String WORKOUT_ID = "workout id";

    private int screenWidth;
    private int screenHeight;
    private OnClickListener onClickListener;
    private DataMatrix dmInstance = new DataMatrix();

    public interface OnClickListener {
        void onClick(int position);
    }

    public static Fragment newInstance(FragmentManager fragmentManager, int pos, float scale, Uri uri, String title, String description, String startButtonText, int workoutId) {
        Bundle b = new Bundle();
        b.putInt(POSITON, pos);
        b.putFloat(SCALE, scale);
        b.putParcelable(URI, uri);
        b.putString(TITLE, title);
        b.putString(DESCRIPTION, description);
        b.putString(START_BUTTON_TEXT, startButtonText);
        b.putInt(WORKOUT_ID, workoutId);

        FragmentFactory factory = fragmentManager.getFragmentFactory();
        Fragment fragment = factory.instantiate(ClassLoader.getSystemClassLoader(), CarouselItemFragment.class.getName());
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWidthAndHeight();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }

        return inflater.inflate(R.layout.featured_carousel_item, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dmInstance.finish();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final int position = this.getArguments().getInt(POSITON);
        float scale = this.getArguments().getFloat(SCALE);
        final Uri uri = this.getArguments().getParcelable(URI);
        final String title = this.getArguments().getString(TITLE);
        final String description = this.getArguments().getString(DESCRIPTION);
        final String startButtonText = this.getArguments().getString(START_BUTTON_TEXT);
        final int workoutId = this.getArguments().getInt(WORKOUT_ID);

        int itemWidth = (int) (screenWidth * 0.7);
        int itemHeight = (int) (screenHeight * 0.6);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(itemWidth, itemHeight);

        ImageView background = view.findViewById(R.id.featured_background);

        TextView titleTextView = view.findViewById(R.id.carousel_title);
        TextView descriptionTextView = view.findViewById(R.id.carousel_description);
        TextView setupButton = view.findViewById(R.id.carousel_start_button);

        CarouselLinearLayout root = view.findViewById(R.id.root_container);
        ConstraintLayout rootItem = view.findViewById(R.id.main_feature_layout);
        rootItem.setLayoutParams(layoutParams);

        titleTextView.setText(title);
        descriptionTextView.setText(description);

        if (startButtonText != null && !startButtonText.isEmpty()) setupButton.setText(startButtonText);

        BitmapAsyncLoadTask bitmapAsyncLoadTask = new BitmapAsyncLoadTask(background, itemWidth, itemHeight);
        bitmapAsyncLoadTask.execute(uri);

        setupButton.setOnClickListener(v -> {
            if (onClickListener != null) onClickListener.onClick(position);
        });

        dmInstance.createIntAdapter(UiServiceKeys.EVENT_FEATURED_WORKOUT_PRESS_INT, value -> {
            if (isVisible()) {
                if (workoutId == value) {
                    setupButton.performClick();
                }
            }
        }, false);

        root.setScaleBoth(scale);
    }

    private void getWidthAndHeight() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screenHeight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }
}
