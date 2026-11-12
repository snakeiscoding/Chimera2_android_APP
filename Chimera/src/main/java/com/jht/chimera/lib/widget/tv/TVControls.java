package com.jht.chimera.lib.widget.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;

public class TVControls extends ConstraintLayout {

    public interface ITVControls {
        void channelUp();
        void channelDown();
        void lastChannel();
        void toggleCC();
        void toggleSAP();
        void info();
        void guide();
        void back();
    }


    public TVControls(Context context) {
        super(context);
        init(context);
    }

    public TVControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TVControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        inflate(context, R.layout.tv_controls, this);
    }

    public void setOnKeyPress( ITVControls callback) {
        if(callback != null) {
            findViewById(R.id.tv_player_channel_up).setOnClickListener(v -> callback.channelUp());
            findViewById(R.id.tv_player_channel_down).setOnClickListener(v -> callback.channelDown());
            findViewById(R.id.tv_player_last_channel).setOnClickListener(v -> callback.lastChannel());
            findViewById(R.id.tv_player_closed_captioning).setOnClickListener(v -> callback.toggleCC());
            findViewById(R.id.tv_player_sap).setOnClickListener(v -> callback.toggleSAP());
            findViewById(R.id.tv_remote_info).setOnClickListener(v -> callback.info());
        }
    }

    public void showButtons(boolean lastChannel, boolean cc, boolean sap, boolean info) {
        if(!lastChannel && !cc && !sap && !info) {
            findViewById(R.id.tv_player_additional_remote_buttons).setVisibility(GONE);
        }
        else {
            findViewById(R.id.tv_player_last_channel).setVisibility(lastChannel ? View.VISIBLE : View.GONE);
            findViewById(R.id.tv_player_closed_captioning).setVisibility(cc ? View.VISIBLE : View.GONE);
            findViewById(R.id.tv_player_sap).setVisibility(sap ? View.VISIBLE : View.GONE);
            findViewById(R.id.tv_remote_info).setVisibility(info ? View.VISIBLE : View.GONE);
        }
    }

    public void setTuner(IChimeraTuner<?> tuner, RecyclerView view, SelectorItemBase channelList) {

        findViewById(R.id.tv_player_channel_down).setOnClickListener(v -> {
            if(channelList != null) {
                tuner.channelUp();
                view.getAdapter().notifyItemChanged(channelList.getSelection());
                int index = channelList.getSelection() + 1;

                if(index >= channelList.numChildren()) {
                    index = 0;
                }
                view.getAdapter().notifyItemChanged(index);
                view.scrollToPosition(index);
                channelList.setSelection(index);
            }
            else {
                tuner.channelDown();
            }
        });
        findViewById(R.id.tv_player_channel_up).setOnClickListener(v -> {
            if(channelList != null) {
                tuner.channelDown();
                view.getAdapter().notifyItemChanged(channelList.getSelection() );
                int index = channelList.getSelection() - 1;

                if(index < 0) {
                    index = channelList.numChildren() - 1;
                }
                channelList.setSelection(index);
                view.getAdapter().notifyItemChanged(index);
                view.scrollToPosition(index);
            }
            else {
                tuner.channelUp();
            }
        });
        findViewById(R.id.tv_player_last_channel).setOnClickListener(v -> {
            tuner.previousChannel();
            if(channelList != null) {
                getHandler().postDelayed(() -> {
                    view.getAdapter().notifyItemChanged(channelList.getSelection());
                    channelList.setSelection(tuner.getCurrentChannelIndex());
                    view.getAdapter().notifyItemChanged(tuner.getCurrentChannelIndex());
                    view.scrollToPosition(tuner.getCurrentChannelIndex());
                }, 100);
            }
        });
        findViewById(R.id.tv_player_closed_captioning).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.TOGGLE_CC));
        findViewById(R.id.tv_player_sap).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.SAP));
        findViewById(R.id.tv_remote_info).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.INFO));

        ITunerCapabilities capabilities = tuner.tuner().capabilities();
        findViewById(R.id.tv_player_last_channel).setVisibility(capabilities.lastChannel() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_closed_captioning).setVisibility(capabilities.cc() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_sap).setVisibility(capabilities.sap() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_remote_info).setVisibility(capabilities.info() ? View.VISIBLE : View.GONE);
        findViewById(R.id.tv_player_additional_remote_buttons).setVisibility(
                capabilities.lastChannel() || capabilities.cc() || capabilities.sap() || capabilities.info() ? View.VISIBLE : View.GONE);

    }

    public void setClosedCaptionIcon(boolean enabled) {
        AppCompatImageButton button = findViewById(R.id.tv_player_closed_captioning);
        if (enabled) {
            button.setBackgroundResource(R.color.black);
            button.setImageResource(R.drawable.ic_icon_cc_on);
        } else {
            button.setBackgroundResource(R.drawable.standard_button_background);
            button.setImageResource(R.drawable.ic_icon_cc);
        }
    }

}
