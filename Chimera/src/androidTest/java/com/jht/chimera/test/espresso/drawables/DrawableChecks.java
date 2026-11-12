package com.jht.chimera.test.espresso.drawables;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;

import org.junit.Assert;

public class DrawableChecks {
    public static ViewAssertion withDrawable(final int resourceId, boolean background) {
        return new DrawableMatcher(resourceId, background);
    }

    public static ViewAssertion noDrawable(boolean background) {
        return new DrawableMatcher(-1, background);
    }


    private static class DrawableMatcher implements ViewAssertion {

        private final int expectedId;
        private boolean background;

        public DrawableMatcher(int resourceId, boolean backgound) {
            this.expectedId = resourceId;
            this.background = backgound;
        }


        private Bitmap getBitmap(Drawable drawable) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            Assert.assertTrue(view instanceof ImageView);

            ImageView imageView = (ImageView) view;
            if (expectedId < 0) {
                Assert.assertNull(imageView.getDrawable());
            }
            Resources resources = view.getContext().getResources();
            Drawable expectedDrawable = resources.getDrawable(expectedId);
            Assert.assertNotNull(expectedDrawable);

            Bitmap bitmap = getBitmap(background ? imageView.getBackground() : imageView.getDrawable());
            Bitmap otherBitmap = getBitmap(expectedDrawable);
            Assert.assertTrue(bitmap.sameAs(otherBitmap));

        }
    }
}
