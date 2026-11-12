package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.util.AttributeSet;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.serialport.peripheral.tuner.channel.SimpleIPTVChannel;

import java.util.Collections;
import java.util.List;

/**
 * This dialog pops up when the user wants to log a service event.  It asks for a description and
 * the user can either create the entry or close it.
 */
public class SimpleIPTVChannelDialog extends DialogBase {

    private UserTextInput port;
    private UserTextInput ipAddress;
    private List<SimpleIPTVChannel> existingChannels = Collections.emptyList();


    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     */
    public SimpleIPTVChannelDialog(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     * @param attrs   Attributes that are set from the view's XML.
     */
    public SimpleIPTVChannelDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context      Current context for the view.
     * @param attrs        Attributes that are set from the view's XML.
     * @param defStyleAttr Default style for the view.
     */
    public SimpleIPTVChannelDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        port = findViewById(R.id.iptv_channel_port);
        ipAddress = findViewById(R.id.iptv_channel_address);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.tv_simple_iptv_channel_edit;
    }

    /**
     * Initialize the dialog.
     *
     */
    public void init(SimpleIPTVChannel channel, List<SimpleIPTVChannel> existingChannels, IOnConfirm onConfirm) {
        this.existingChannels = existingChannels;
        port.setText(channel.port());
        ipAddress.setText(channel.ip());

        findViewById(R.id.message_dialog_ok).setOnClickListener(v -> {
            // CGC-22432: Prevent duplicate channels — block if same IP and port already exists.
            for (SimpleIPTVChannel ch : this.existingChannels) {
                if (!ch.uuid().equals(channel.uuid()) && ipAddress.getText().equals(ch.ip()) && port.getText().equals(ch.port())) {
                    DialogFactory.message(this, getContext().getString(R.string.channel_already_exists_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                    return;
                }
            }
            channel.ip(ipAddress.getText());
            channel.port(port.getText());
            if(onConfirm != null) {
                onConfirm.onConfirm();
            }
            hide();
        });
        findViewById(R.id.message_dialog_cancel).setOnClickListener( v -> hide() );
    }


}
