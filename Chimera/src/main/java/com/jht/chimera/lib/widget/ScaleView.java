package com.jht.chimera.lib.widget;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.TimePicker;

import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.widget.selectron.SelectronButton;

/**
 * Created by alex.nguyen on 7/24/2017.
 * Based on http://www.vanteon.com/downloads/Scaling_Android_Apps_White_Paper.pdf
 */

public class ScaleView {
    private static final String TAG = "ScaleView";

    // The width and height to default target in dp
    public static final int DEFAULT_TARGET_WIDTH = 1366;
    public static final int DEFAULT_TARGET_HEIGHT = 768;

    // The actual width and height of the screen in pixels
    private static int sWidth = 0;
    private static int sHeight = 0;


    // Width and height of screen target in pixels (this value is converted based on current dpi)
    private static int sTargetWidth;
    private static int sTargetHeight;
    static float mDensity = 1;

    private static void updateWidthHeight() {
        if (sWidth == 0 || sHeight == 0) {
            Context context = ContextUtil.getApplicationContext();
            WindowManager window = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = window.getDefaultDisplay();
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            Point size = new Point();
            display.getRealSize(size);
            int widthPixels = size.x;
            int heightPixels = size.y;

            sWidth = widthPixels;
            sHeight = heightPixels;
            mDensity = metrics.density;
        }
    }

    public static void scaleContents(View rootView, int targetWidth, int targetHeight) {
        updateWidthHeight();
        sTargetWidth = (int) (targetWidth * mDensity);
        sTargetHeight = (int) (targetHeight * mDensity);

        // Compute the scaling ratio
        float xScale = (float) sWidth / (float) sTargetWidth;
        float yScale = (float) sHeight / (float) sTargetHeight;

        // Scale our contents
        scaleViewAndChildren(rootView, xScale, yScale);
    }

    // Scales the contents of the given view so that it completely fills the given
    // container on one axis (that is, we're scaling isotropically).
    public static void scaleContents(View rootView) {
        // default to 1366x768
        scaleContents(rootView, DEFAULT_TARGET_WIDTH, DEFAULT_TARGET_HEIGHT);
    }

    /**
     * Scales a dp height value for dynamic changing of views based on the current size of the screen.
     *
     * @param heightValue The sHeight value to scale
     * @return The scaled sHeight value
     */
    public static int scaleHeightValue(int heightValue) {
        updateWidthHeight();

        float yScale = (float) sHeight / sTargetHeight * mDensity;
        return Math.round(heightValue * yScale);
    }

    /**
     * Scales a dp width value for dynamic changing of views based on the current size of the screen.
     *
     * @param widthValue The sWidth value to scale
     * @return The scaled sWidth value
     */
    public static int scaleWidthValue(int widthValue) {
        updateWidthHeight();
        int widthScaled = (int) (widthValue * mDensity);
        float xScale = (float) sWidth / sTargetWidth;
        return Math.round(widthScaled * xScale);
    }

    // Scale the given view, its contents, and all of its children by the given factor.

    /**
     * Scales the given view, its contents, and all of its children by the given factor.
     * @param root The view to scale
     * @param xScale The value the x scale should change by to be correct in pixels.
     * @param yScale The value the y scale should change by to be correct in pixels
     */
    private static void scaleViewAndChildren(View root, float xScale, float yScale) {
        // Retrieve the view's layout information
        ViewGroup.LayoutParams layoutParams = root.getLayoutParams();

        if (layoutParams == null) return;
        // Scale the view itself
        if (root.getRotation() == 270 || root.getRotation() == 90) {
            float tempscale = xScale;
            xScale = yScale;
            yScale = tempscale;
        }

        int tmpWidth = 0;
        if (layoutParams.width != ViewGroup.LayoutParams.MATCH_PARENT &&
                layoutParams.width != ViewGroup.LayoutParams.WRAP_CONTENT &&
                layoutParams.width > 0) {
            tmpWidth = layoutParams.width;
            layoutParams.width = Math.round(layoutParams.width * xScale);
        }
        if (layoutParams.height != ViewGroup.LayoutParams.MATCH_PARENT &&
                layoutParams.height != ViewGroup.LayoutParams.WRAP_CONTENT &&
                layoutParams.height > 0) {
            if (tmpWidth == layoutParams.height) {
                // if the width and height are the same, keep them the same
                layoutParams.height = layoutParams.width;
            } else {
                layoutParams.height = Math.round(layoutParams.height * yScale);
            }
        }

        // If this view has margins, scale those too
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginParams.setMarginStart(Math.round(marginParams.getMarginStart() * xScale));
            marginParams.setMarginEnd(Math.round(marginParams.getMarginEnd() * xScale));
            marginParams.rightMargin = Math.round(marginParams.rightMargin * (xScale));
            marginParams.leftMargin = Math.round(marginParams.leftMargin * (xScale));
            marginParams.topMargin = Math.round(marginParams.topMargin * (yScale));
            marginParams.bottomMargin = Math.round(marginParams.bottomMargin * (yScale));
        }

        // Set the layout information back into the view
        root.setLayoutParams(layoutParams);

        // Scale the view's padding
        root.setPadding(
                (int) (root.getPaddingLeft() * xScale + 0.5),
                (int) (root.getPaddingTop() * yScale + 0.5),
                (int) (root.getPaddingRight() * xScale + 0.5),
                (int) (root.getPaddingBottom() * yScale + 0.5));

        // If the root view is a subclass TextView, scale the size of its text
        if (TextView.class.isAssignableFrom(root.getClass())) {
            TextView textView = (TextView) root;
            int autoSizeType = 0;
            autoSizeType = textView.getAutoSizeTextType();
            if (autoSizeType == TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM) {
                int maxSize = Math.round(textView.getAutoSizeMaxTextSize() * yScale);
                int minSize = Math.round(textView.getAutoSizeMinTextSize() * yScale);
                textView.setAutoSizeTextTypeUniformWithConfiguration(minSize, maxSize, 1, TypedValue.COMPLEX_UNIT_PX);
            } else {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() * yScale);
            }
        }

        // If the root view is a ViewGroup, scale all of its children recursively
        if (root instanceof ViewGroup) {
            if (root.getRotation() == 270 || root.getRotation() == 90) {
                float tempscale = xScale;
                xScale = yScale;
                yScale = tempscale;
            }
            ViewGroup groupView = (ViewGroup) root;
            for (int cnt = 0; cnt < groupView.getChildCount(); ++cnt)
                scaleViewAndChildren(groupView.getChildAt(cnt), xScale, yScale);
        }

        if (root instanceof SelectronButton) {
            SelectronButton selectronButton = (SelectronButton) root;
            if (selectronButton.isIconSquare())
                selectronButton.scaleIcon(xScale, xScale);
            else
                selectronButton.scaleIcon(xScale, yScale); // TODO only scale xScale?
        }

        if (root instanceof TimePicker ) {
            View picker = (View) root;
            picker.setScaleX(picker.getScaleX() * xScale);
            picker.setScaleY(picker.getScaleY() * yScale);
        }
    }
}
