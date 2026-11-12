package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jht.chimera.R;

public class CarouselItemFragment extends Fragment {

    private static final String POSITON = "position";
    private static final String SCALE = "scale";
    private static final String URI = "uri";
    private static final String CAPTION = "caption";
    private static final String SLUG = "slug";

    private int screenWidth;
    private int screenHeight;
    private OnClickListener onClickListener;

    public interface OnClickListener {
        void onClick(int position);
    }

    public static Fragment newInstance(Context context, int pos, float scale, Uri uri, String caption, String slug) {
        Bundle b = new Bundle();
        b.putInt(POSITON, pos);
        b.putFloat(SCALE, scale);
        b.putParcelable(URI, uri);
        b.putString(CAPTION, caption);
        b.putString(SLUG, slug);

        return Fragment.instantiate(context, CarouselItemFragment.class.getName(), b);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWidthAndHeight();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        return inflater.inflate(R.layout.carousel_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final int position = this.getArguments().getInt(POSITON);
        float scale = this.getArguments().getFloat(SCALE);
        final Uri uri = this.getArguments().getParcelable(URI);
        final String caption = this.getArguments().getString(CAPTION);
        final String slug = this.getArguments().getString(SLUG);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(screenWidth / 2, screenHeight / 2);

        TextView textView = view.findViewById(R.id.text);
        CarouselLinearLayout root = view.findViewById(R.id.root_container);
        ImageView imageView = view.findViewById(R.id.image);
        RelativeLayout rootItem = view.findViewById(R.id.root_item);

        textView.setText(caption);
        rootItem.setLayoutParams(layoutParams);
        imageView.setContentDescription(slug);
        imageView.setOnClickListener(v -> {
            if (onClickListener != null) onClickListener.onClick(position);
        });

        imageView.setImageURI(uri); // try to just load the image since BitmapAsyncLoadTask doesn't support vector drawables
        BitmapAsyncLoadTask bitmapAsyncLoadTask = new BitmapAsyncLoadTask(imageView, screenWidth / 2, screenHeight / 2);
        bitmapAsyncLoadTask.execute(uri);

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
