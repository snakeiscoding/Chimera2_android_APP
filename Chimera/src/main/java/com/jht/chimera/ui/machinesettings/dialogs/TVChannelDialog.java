package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.chimera.lib.tv.SKNETChannelInfo;
import com.jht.serialport.peripheral.tuner.swift.SwiftChannel;

/**
 * This dialog pops up when the user wants to log a service event.  It asks for a description and
 * the user can either create the entry or close it.
 */
public class TVChannelDialog extends DialogBase {

    private UserTextInput name;


    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     */
    public TVChannelDialog(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     * @param attrs   Attributes that are set from the view's XML.
     */
    public TVChannelDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context      Current context for the view.
     * @param attrs        Attributes that are set from the view's XML.
     * @param defStyleAttr Default style for the view.
     */
    public TVChannelDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        name = findViewById(R.id.tv_channel_name);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.tv_channel_edit;
    }

    /**
     * Initialize the dialog.
     *
     */
    public void init(ChannelInfo channelInfo, IOnConfirm onConfirm) {
        name.setText(channelInfo.name());
        findViewById(R.id.message_dialog_ok).setOnClickListener(v -> {
            channelInfo.name(name.getText());
            if(onConfirm != null) {
                onConfirm.onConfirm();
            }
            hide();
        });
        findViewById(R.id.message_dialog_cancel).setOnClickListener( v -> hide() );
    }

    public void init(SKNETChannelInfo channelInfo, IOnConfirm onConfirm) {
        name.setText(channelInfo.getServiceName());
        findViewById(R.id.message_dialog_ok).setOnClickListener(v -> {
            channelInfo.setServiceName(name.getText());
            if(onConfirm != null) {
                onConfirm.onConfirm();
            }
            hide();
        });
        findViewById(R.id.message_dialog_cancel).setOnClickListener( v -> hide() );
    }

    /**
     * Initialize the dialog.
     *
     */
    public void init(SwiftChannel channel, IOnConfirm onConfirm) {
        name.setText(channel.name());
        findViewById(R.id.message_dialog_ok).setOnClickListener(v -> {
            channel.name(name.getText());
            if(onConfirm != null) {
                onConfirm.onConfirm();
            }
            hide();
        });
        findViewById(R.id.message_dialog_cancel).setOnClickListener( v -> hide() );
    }
}
