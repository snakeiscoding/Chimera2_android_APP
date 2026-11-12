package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.ChannelInfo;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

/**
 * This dialog pops up when the user wants to log a service event.  It asks for a description and
 * the user can either create the entry or close it.
 */
public class TVChannelDialog2 extends DialogBase {

    public interface IOnChannelEdit {
        void onChannelEdit(String name, String number);
    }

    private final UserTextInput name;
    private final UserTextInput number;


    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     */
    public TVChannelDialog2(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     * @param attrs   Attributes that are set from the view's XML.
     */
    public TVChannelDialog2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context      Current context for the view.
     * @param attrs        Attributes that are set from the view's XML.
     * @param defStyleAttr Default style for the view.
     */
    public TVChannelDialog2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        name = findViewById(R.id.tv_channel_name);
        number = findViewById(R.id.tv_channel_number);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.tv_channel_edit_2;
    }

    /**
     * Initialize the dialog.
     *
     */
    public void init(ChannelInfo channelInfo, IOnConfirm onConfirm) {
        name.setText(channelInfo.name());
        number.setText(channelInfo.number());
        findViewById(R.id.message_dialog_ok).setOnClickListener(v -> {
            channelInfo.name(name.getText());
            channelInfo.number(number.getText());
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
    public void init(IChannel channel, IOnChannelEdit onConfirm) {
        name.setText(channel.name());
        number.setText(channel.number());
        findViewById(R.id.message_dialog_ok).setOnClickListener(v -> {
            if(onConfirm != null) {
                onConfirm.onChannelEdit(name.getText(), number.getText());
            }
            hide();
        });
        findViewById(R.id.message_dialog_cancel).setOnClickListener( v -> hide() );

    }


}
