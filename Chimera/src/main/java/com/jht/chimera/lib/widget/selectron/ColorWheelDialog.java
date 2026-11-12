package com.jht.chimera.lib.widget.selectron;


import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

/**
 * Created by alex.nguyen on 3/28/2017.
 */

public class ColorWheelDialog extends Dialog
{
    ColorWheel colorWheel;
    OnColorChangListener onColorChangListener;


    public interface OnColorChangListener {
        void colorWheelChanged(int color);
    }

    public void setOnColorChangListener(OnColorChangListener listener){
        onColorChangListener = listener;
    }

    public ColorWheelDialog(Context context)
    {
        super(context);
        colorWheel = new ColorWheel(getContext());
        setContentView(colorWheel);

        int titleLayoutId = getContext().getResources().getIdentifier("title", "id", "android");
        final TextView title = findViewById(titleLayoutId);


        colorWheel.setOnColorChangedListener(color -> {
            setTitle(Integer.toHexString(color));
            title.setTextColor(0xFF000000 + (0xFFFFFF ^ (color&0x00FFFFFF)));
            title.setBackgroundColor(color);
            onColorChangListener.colorWheelChanged(color);
        });

    }



}
