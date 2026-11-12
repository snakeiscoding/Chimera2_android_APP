package com.jht.chimera.ui.machinesettings.dialogs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.androiduiwidgets.dialog.DialogFactory;
import com.jht.androiduiwidgets.dialog.SimpleMessageDialog;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.tv.ChannelInfo;

import java.util.Collections;
import java.util.List;

/**
 * This dialog pops up when the user wants to log a service event.  It asks for a description and
 * the user can either create the entry or close it.
 */
public class IPTVChannelDialog extends DialogBase {

    private final UserTextInput name;
    private final UserTextInput number;
    private final OptionSelectorItem protocol;
    private final UserTextInput port;
    private final UserTextInput address;
    private final UserTextInput path;
    private final CheckBox deinterlace;
    private List<ChannelInfo> existingChannels = Collections.emptyList();


    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     */
    public IPTVChannelDialog(Context context) {
        this(context, null, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context Current context for the view.
     * @param attrs   Attributes that are set from the view's XML.
     */
    public IPTVChannelDialog(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Initialize the dialog.
     *
     * @param context      Current context for the view.
     * @param attrs        Attributes that are set from the view's XML.
     * @param defStyleAttr Default style for the view.
     */
    public IPTVChannelDialog(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        name = findViewById(R.id.channel_name);
        number = findViewById(R.id.channel_number);
        protocol = findViewById(R.id.channel_protocol);
        port = findViewById(R.id.channel_data2);
        address = findViewById(R.id.channel_data1);
        path = findViewById(R.id.channel_path);
        deinterlace = findViewById(R.id.channel_deinterlace);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.enseo_channel_dialog;
    }


    /**
     * Initialize the dialog.
     *
     */
    public void init(ChannelInfo channelInfo, List<ChannelInfo> existingChannels, IOnConfirm onConfirm) {
        this.existingChannels = existingChannels;
        name.setText(channelInfo.name());
        number.setText(channelInfo.number());
        int protocolArrayId;
        switch(channelInfo.channelType()) {

            case ENSEO_PRO_IPTV:
                protocolArrayId = R.array.enseo_pro_iptv_protocols;
                deinterlace.setVisibility(View.GONE);
                break;
            case UNENCRYPTED_IPTV:
                //todo -- flesh the different channeltypes this default business is highly suspect  :|
            default:
                protocolArrayId = R.array.iptv_protocols;
                deinterlace.setVisibility(View.GONE);
                break;
        }

        ChannelInfo.URLDecode decode = new ChannelInfo.URLDecode(channelInfo.url());
        String[] protocols = getResources().getStringArray(protocolArrayId);
        boolean found = false;
        for(int i = 0; i < protocols.length; i++) {
            if(protocols[i].equalsIgnoreCase(decode.protocol())) {
                protocol.setOptions(protocolArrayId, i);
                found = true;
                break;
            }
        }
        if(!found) {
            protocol.setOptions(protocolArrayId, 0);
        }
        if(protocols.length == 1) {
            protocol.setReadOnly(true);
        }

        if(protocols[protocol.getSelectedPosition()].equalsIgnoreCase("udp")) { //NON-NLS
            path.setVisibility(View.INVISIBLE);
        } else {
            path.setVisibility(View.VISIBLE);
        }
        protocol.setOnOptionSelected( (pos, option) -> {
            if(option.equalsIgnoreCase("udp")) { //NON-NLS
                path.setVisibility(View.INVISIBLE);
            } else {
                path.setVisibility(View.VISIBLE);
            }
        });
        port.setText(decode.port());
        address.setText(decode.address());
        path.setText(decode.path());
        deinterlace.setChecked(channelInfo.deinterlaced());

        //todo  -- potential condidate for removal
        findViewById(R.id.dialog_confirm_btn).setOnClickListener(v -> {
            // CGC-22432: Prevent duplicate channels, block if same address and port already exists.
            // TODO: CGC-23503 Update to UUID comparison once Swift & MST controllers are migrated to IChannel
            for (ChannelInfo ch : this.existingChannels) {
                if (ch != channelInfo) {
                    ChannelInfo.URLDecode existingDecode = new ChannelInfo.URLDecode(ch.url());
                    if (address.getText().equals(existingDecode.address()) && port.getText().equals(existingDecode.port())) {
                        DialogFactory.message(this, getContext().getString(R.string.channel_already_exists_pg), null, SimpleMessageDialog.SIMPLE_BUTTON_STATE.OK);
                        return;
                    }
                }
            }
            channelInfo.name(name.getText());
            channelInfo.number(number.getText());
            channelInfo.deinterlaced(false);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(protocols[protocol.getSelectedPosition()].toLowerCase()).append("://");
            /*
             This is now covered in the IPTV Player.
            if(protocols[protocol.getSelectedPosition()].equalsIgnoreCase("udp") && !address.getText().startsWith("@")) { //NON-NLS
                stringBuilder.append("@");
            }
            */
            stringBuilder.append(address.getText());
            if(!port.getText().isEmpty()) {
                stringBuilder.append(":").append(port.getText());
            }
            if(!path.getText().isEmpty()) {
                if(!path.getText().startsWith("/")) {
                    stringBuilder.append("/");
                }
                stringBuilder.append(path.getText());
            }
            channelInfo.url(stringBuilder.toString());

            if(onConfirm != null) {
                onConfirm.onConfirm();
            }
            hide();
        });
        findViewById(R.id.dialog_cancel_btn).setOnClickListener( v -> hide() );
    }


}
