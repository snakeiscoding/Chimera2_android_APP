package com.jht.chimera.lib.workoutapps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import java.util.Calendar;

public class CalendarAppIcon extends AppIcon {
    CalendarAppIcon(String iconId, String appName, int iconResource, AppInfo info) {
        super(iconId, appName, iconResource, info);
    }

    @Override
    public Drawable getAppIcon() {
        Drawable drawable = super.getAppIcon();

        final int Y_POS_TEXT_OFFSET = 18;
        final int DAY_NUMBER_TEXT_SIZE = 62;
        final int CANVAS_SCALE = 1;

        int width = drawable.getIntrinsicWidth() * CANVAS_SCALE;
        int height = drawable.getIntrinsicHeight() * CANVAS_SCALE;

        //Bitmap bm = drawableToBitmap(drawable);//background is blurry...
        //Bitmap bm = BitmapFactory.decodeFile(filepath.getAbsolutePath()); null bitmap...
        Bitmap bm = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(DAY_NUMBER_TEXT_SIZE);
        Canvas canvas = new Canvas(bm);
        Calendar calendar = Calendar.getInstance();
        int dom = calendar.get(Calendar.DAY_OF_MONTH);
        int xPos = width / 2;
        int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2)) + Y_POS_TEXT_OFFSET;
        canvas.drawText("" + dom, xPos, yPos, paint);

        //return new BitmapDrawable(bm);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{drawable, new BitmapDrawable(bm)});
        return layerDrawable;
    }

}
