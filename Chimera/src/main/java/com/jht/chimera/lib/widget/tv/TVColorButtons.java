package com.jht.chimera.lib.widget.tv;

import android.content.Context;
import android.util.AttributeSet;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

public class TVColorButtons extends ConstraintLayout {

    public interface ITVColorControls {
        void red();
        void blue();
        void yellow();
        void green();
    }


    public TVColorButtons(Context context) {
        super(context);
        init(context);
    }

    public TVColorButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TVColorButtons(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        inflate(context, R.layout.tv_color_buttons, this);
    }

    public void setOnKeyPress( ITVColorControls callback) {
        if(callback != null) {
            findViewById(R.id.eng_tv_control_red).setOnClickListener(v -> callback.red());
            findViewById(R.id.eng_tv_control_yellow).setOnClickListener(v -> callback.yellow());
            findViewById(R.id.eng_tv_control_blue).setOnClickListener(v -> callback.blue());
            findViewById(R.id.eng_tv_control_green).setOnClickListener(v -> callback.green());
        }
    }


    public void setTuner(IChimeraTuner<? extends IChannel> tuner) {
        if(tuner != null) {
            findViewById(R.id.eng_tv_control_red).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.RED));
            findViewById(R.id.eng_tv_control_yellow).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.YELLOW));
            findViewById(R.id.eng_tv_control_blue).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.BLUE));
            findViewById(R.id.eng_tv_control_green).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.GREEN));

        }

    }

}
