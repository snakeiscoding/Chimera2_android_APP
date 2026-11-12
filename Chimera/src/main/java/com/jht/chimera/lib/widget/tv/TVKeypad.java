package com.jht.chimera.lib.widget.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

public class TVKeypad  extends ConstraintLayout {

    public interface ITVKeypad {
        void zero();
        void one();
        void two();
        void three();
        void four();
        void five();
        void six();
        void seven();
        void eight();
        void nine();
        void dash();
        void enter();
    }


    public TVKeypad(Context context) {
        super(context);
        init(context);
    }

    public TVKeypad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TVKeypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        // Inflate the layout.
        inflate(context, R.layout.tv_keypad, this);
    }

    public void enableDash(boolean enable) {
        findViewById(R.id.tv_player_remote_button_dash).setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.tv_player_remote_button_dash).setEnabled(enable);
    }

    public void setOnKeyPress( ITVKeypad callback) {
        if(callback != null) {
            findViewById(R.id.tv_player_remote_button_0).setOnClickListener(v -> callback.zero());
            findViewById(R.id.tv_player_remote_button_1).setOnClickListener(v -> callback.one());
            findViewById(R.id.tv_player_remote_button_2).setOnClickListener(v -> callback.two());
            findViewById(R.id.tv_player_remote_button_3).setOnClickListener(v -> callback.three());
            findViewById(R.id.tv_player_remote_button_4).setOnClickListener(v -> callback.four());
            findViewById(R.id.tv_player_remote_button_5).setOnClickListener(v -> callback.five());
            findViewById(R.id.tv_player_remote_button_6).setOnClickListener(v -> callback.six());
            findViewById(R.id.tv_player_remote_button_7).setOnClickListener(v -> callback.seven());
            findViewById(R.id.tv_player_remote_button_8).setOnClickListener(v -> callback.eight());
            findViewById(R.id.tv_player_remote_button_9).setOnClickListener(v -> callback.nine());
            findViewById(R.id.tv_player_remote_button_dash).setOnClickListener(v -> callback.dash());
            findViewById(R.id.tv_player_remote_button_enter).setOnClickListener(v -> callback.enter());
        }
    }

    public void setTuner(IChimeraTuner<? extends IChannel> tuner) {
        if(tuner != null) {
            findViewById(R.id.tv_player_remote_button_0).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.ZERO));
            findViewById(R.id.tv_player_remote_button_1).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.ONE));
            findViewById(R.id.tv_player_remote_button_2).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.TWO));
            findViewById(R.id.tv_player_remote_button_3).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.THREE));
            findViewById(R.id.tv_player_remote_button_4).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.FOUR));
            findViewById(R.id.tv_player_remote_button_5).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.FIVE));
            findViewById(R.id.tv_player_remote_button_6).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.SIX));
            findViewById(R.id.tv_player_remote_button_7).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.SEVEN));
            findViewById(R.id.tv_player_remote_button_8).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.EIGHT));
            findViewById(R.id.tv_player_remote_button_9).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.NINE));
            findViewById(R.id.tv_player_remote_button_dash).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.DASH));
            findViewById(R.id.tv_player_remote_button_enter).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.ENTER));
        }
    }

}
