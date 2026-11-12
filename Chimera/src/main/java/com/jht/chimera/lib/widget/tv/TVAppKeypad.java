package com.jht.chimera.lib.widget.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;

public class TVAppKeypad  extends ConstraintLayout {

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


    public TVAppKeypad(Context context) {
        super(context);
        init(context);
    }

    public TVAppKeypad(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TVAppKeypad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        // Inflate the layout.
        inflate(context, R.layout.tv_app_keypad, this);
    }

    public void enableDash(boolean enable) {
        findViewById(R.id.tv_app_player_remote_button_dash).setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.tv_app_player_remote_button_dash).setEnabled(enable);
    }

    public void setOnKeyPress( ITVKeypad callback) {
        if(callback != null) {
            findViewById(R.id.tv_app_player_remote_button_0).setOnClickListener(v -> callback.zero());
            findViewById(R.id.tv_app_player_remote_button_1).setOnClickListener(v -> callback.one());
            findViewById(R.id.tv_app_player_remote_button_2).setOnClickListener(v -> callback.two());
            findViewById(R.id.tv_app_player_remote_button_3).setOnClickListener(v -> callback.three());
            findViewById(R.id.tv_app_player_remote_button_4).setOnClickListener(v -> callback.four());
            findViewById(R.id.tv_app_player_remote_button_5).setOnClickListener(v -> callback.five());
            findViewById(R.id.tv_app_player_remote_button_6).setOnClickListener(v -> callback.six());
            findViewById(R.id.tv_app_player_remote_button_7).setOnClickListener(v -> callback.seven());
            findViewById(R.id.tv_app_player_remote_button_8).setOnClickListener(v -> callback.eight());
            findViewById(R.id.tv_app_player_remote_button_9).setOnClickListener(v -> callback.nine());
            findViewById(R.id.tv_app_player_remote_button_dash).setOnClickListener(v -> callback.dash());
            findViewById(R.id.tv_app_player_remote_button_enter).setOnClickListener(v -> callback.enter());
        }
    }


}
