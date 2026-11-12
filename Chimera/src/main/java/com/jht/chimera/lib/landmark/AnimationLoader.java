package com.jht.chimera.lib.landmark;

import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

public class AnimationLoader {
    public static void loadKenburnAnimation(ImageView view) {
        Animation translateAnim = new TranslateAnimation(0f, 200f, 0f, 0f);
        translateAnim.setDuration(20000);
        Animation scaleAnim = new ScaleAnimation(1.0f, 0.75f, 1.0f, 0.7f, 50, 50);
        scaleAnim.setDuration(30000);

        AnimationSet set = new AnimationSet(false);
        set.addAnimation(translateAnim);
        set.addAnimation(scaleAnim);

        set.setFillBefore(true);
        set.setFillAfter(false);
        set.setFillEnabled(false);
        view.startAnimation(set);
    }

    public static void fadeOut(Context context, View view) {
        fadeOut(context, view, null);
    }

    public static void fadeOut(Context context, View view, AnimationListener listener) {
        Animation fadeOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);

        if (null != listener) {
            fadeOut.setAnimationListener(listener);
        }

        view.startAnimation(fadeOut);
    }

    public static void fadeIn(Context context, View view) {
        fadeIn(context, view, null);
    }

    public static void fadeIn(Context context, View view, AnimationListener listener) {
        Animation fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);

        if (null != listener) {
            fadeIn.setAnimationListener(listener);
        }

        view.startAnimation(fadeIn);
    }

    public static void croassfade(final Context context, final ImageView snapshot, final Bitmap nextImage) {
        croassfade(context, snapshot, nextImage, null);
    }

    public static void croassfade(final Context context,
                                  final ImageView snapshot, final Bitmap nextImage, final AnimatorListener listener) {

        snapshot.animate().alpha(0.3f).setDuration(600).start();

        new Handler().postDelayed(() -> {
            snapshot.setImageBitmap(nextImage);
            snapshot.animate().alpha(1f).setDuration(600).start();
            AnimationLoader.loadKenburnAnimation(snapshot);
        }, 500);
    }
}
