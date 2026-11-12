package com.jht.chimera.lib.widget.button;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;

/**
 * The custom home button widget.
 *
 * @author Andy Rosene
 * @since 2019.11.6
 */
public class HomeButton extends ConstraintLayout {
    private final ImageView homeIcon;

    public HomeButton(Context context) {
        this(context, null);
    }

    public HomeButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HomeButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Inflate the home button and attach to the layout.
        inflate(context, R.layout.btn_ic_home, this);

        // cache the home icon
        homeIcon = findViewById(R.id.home_icon);

        // apply the custom attributes
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HomeButton, defStyleAttr, 0);
        try {
            // Apply the custom tint if there is one (Need to do the following because using vector drawable).
            homeIcon.setColorFilter(typedArray.getColor(R.styleable.HomeButton_tint,  0));
        } finally {
            typedArray.recycle();
        }
    }

    /**
     * Set the icon drawable to the given drawable.
     *
     * @param drawable The given drawable.
     *
     * @deprecated The caller must instead implement their own custom button implementation. This will no longer be
     * supporting this use case.
     */
    public void setIconDrawable(@NonNull final Drawable drawable) {
        homeIcon.setImageDrawable(drawable);
    }
}
