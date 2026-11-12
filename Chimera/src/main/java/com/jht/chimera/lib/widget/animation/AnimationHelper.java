package com.jht.chimera.lib.widget.animation;

import android.animation.Animator;
import android.view.View;

public class AnimationHelper {

    public static void toggleVisibility(final View view) {
        toggleVisibility(view, 200);
    }

    public static void toggleVisibility(final View view, int duration) {
        if(view.getVisibility() == View.VISIBLE) {
            hideView(view, duration);
        }
        else {
            showView(view, duration);
        }
    }
    public static void hideView(final View view) {
        hideView(view, 200);
    }
    public static void hideView(final View view, int duration) {
        view.animate().alpha(0.0f).setDuration(duration).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.GONE);
                view.setAlpha(0.0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public static void showView(final View view) {
        showView(view, 200);
    }
    public static void showView(final View view, int duration) {
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1.0f).setDuration(duration).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setAlpha(1.0f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    public static void showView(final View view, int duration, float endAlpha) {
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(0.0f).setDuration(duration).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setAlpha(endAlpha);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}
