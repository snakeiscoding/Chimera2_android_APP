package com.jht.chimera.lib.widget.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.util.List;

public class TVDefaultChannel extends RelativeLayout {
    private final UserTextInput defaultChannelInput;
    private final OptionSelectorItem defaultChannelSelector;
    private IChimeraTuner<?> tuner;

    public TVDefaultChannel(Context context) {
        this(context, null);
    }

    public TVDefaultChannel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TVDefaultChannel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflate(context, R.layout.tv_default_channel, this);

        defaultChannelInput = findViewById(R.id.tv_channel_setup_default_entry);
        defaultChannelSelector = findViewById(R.id.tv_channel_setup_default_selector);
    }

    public void reset() {
        try {
            switch (tuner.getDefaultChannelType()) {

                case CHANNEL_INDEX:
                    defaultChannelInput.setVisibility(View.GONE);
                    defaultChannelSelector.setVisibility(View.VISIBLE);
                    final List<? extends IChannel> channelList = tuner.tuner().getChannelList(false);
                    if (channelList.size() > 0) {
                        String[] channelNames = new String[channelList.size()];
                        for (int i = 0; i < channelNames.length; i++) {
                            channelNames[i] = channelList.get(i).displayName(null);
                        }

                        // Resolve the default channel by UUID so hidden-channel list shifts do
                        // not corrupt the selection. Fall back to the stored integer index for
                        // devices that have not migrated yet (UUID key empty)
                        int defaultIndex = -1;
                        String storedUuid = "";
                        try {
                            storedUuid = tuner.getDefaultChannel();
                        } catch (UnsupportedOperationException ignored) {}
                        if (!storedUuid.isEmpty()) {
                            for (int i = 0; i < channelList.size(); i++) {
                                IChannel ch = channelList.get(i);
                                if (ch.uuid().equals(storedUuid)) {
                                    defaultIndex = i;
                                    break;
                                }
                            }
                        }
                        if (defaultIndex < 0) {
                            // UUID not found, migration path or channel was removed
                            defaultIndex = tuner.getDefaultChannelInt();
                        }
                        if (defaultIndex < 0 || defaultIndex >= channelNames.length) {
                            defaultIndex = 0;
                        }

                        // Write UUID back to migrate old int-only devices on first open
                        final IChannel resolvedDefault = channelList.get(defaultIndex);
                        try {
                            tuner.setDefaultChannel(resolvedDefault.uuid());
                        } catch (UnsupportedOperationException ignored) {}

                        // Null out old listener before setOptions to prevent index 0 callbacks.
                        defaultChannelSelector.setOnOptionSelected(null);
                        defaultChannelSelector.setOptions(new OptionManager(channelNames, defaultIndex));
                        defaultChannelSelector.setOnOptionSelected((index, string) -> {
                            IChannel selected = channelList.get(index);
                            try {
                                tuner.setDefaultChannel(selected.uuid());
                            } catch (UnsupportedOperationException ignored) {}
                            try {
                                tuner.setDefaultChannel(index);
                            } catch (UnsupportedOperationException ignored) {}
                            DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                        });
                    } else {
                        // No channels, hide the selector so it doesn't show a stale value
                        defaultChannelSelector.setVisibility(View.GONE);
                    }
                    break;
                case CHANNEL_STRING:
                    defaultChannelInput.setVisibility(View.VISIBLE);
                    defaultChannelSelector.setVisibility(View.GONE);
                    defaultChannelInput.setText(tuner.getDefaultChannel());
                    defaultChannelInput.setOnTextChanged(value -> {
                        tuner.setDefaultChannel(value);
                        DataMatrix.sendEvent(TVKeys.EVENT_UPDATE_DEFAULT_CHANNEL_OPTION);
                    });
                    break;
            }
        }
        catch(UnsupportedOperationException ex) {
            // Should never get here.
            defaultChannelSelector.setVisibility(View.GONE);
            defaultChannelInput.setVisibility(View.GONE);
        }

    }

    /**
     * Update the default channel widget based on the tuner input.
     * @param tuner  The tuner that houses the default channel.
     */
    public void setTuner(IChimeraTuner<?> tuner) {
        this.tuner = tuner;
        reset();
    }

}
