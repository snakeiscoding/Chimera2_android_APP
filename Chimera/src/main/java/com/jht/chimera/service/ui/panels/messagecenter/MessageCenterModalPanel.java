package com.jht.chimera.service.ui.panels.messagecenter;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.chimera.service.ui.PanelManager;
import com.jht.chimera.service.ui.panels.ManagedPanel;

import java.util.UUID;

/**
 * A UI panel that displays further info about a message that was ellipsized. Contains a scrolling
 * panel for display.
 * <p>
 * Created by chris.foyer on 10/17/2017.
 */
public class MessageCenterModalPanel extends ManagedPanel {
    private final RelativeLayout mHeaderBackground;
    private final ScrollView mScrollView;
    private final SelectronButton mCloseButton;
    private final TextView mMessageTextView;
    private final TextView mTitleTextView;


    public MessageCenterModalPanel(Context context, UUID id) {
        this(context, id, null);
    }
    public MessageCenterModalPanel(Context context, UUID id, PanelManager panelManager) {
        super(context, id, panelManager);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.message_center_dialog, this, true);
        mCloseButton = findViewById(R.id.btnClose);
        mMessageTextView = findViewById(R.id.text_message);
        mTitleTextView = findViewById(R.id.text_title);
        mHeaderBackground = findViewById(R.id.titleBar);
        mScrollView = findViewById(R.id.message_scroll_view);
    }

    public void showMessage(MessageCenterMessage message, DialogBase.IOnCloseDialog onClose) {
        mCloseButton.setOnClickListener((v) -> onClose.onCloseDialog());
        mTitleTextView.setText(message.getMessageHeader());
        mMessageTextView.setText(message.getMessageText());
        mMessageTextView.setTextColor(message.getTextColor());
        mHeaderBackground.setBackgroundColor(message.getHeaderColor());
        mScrollView.setBackgroundColor(message.getBackgroundColor());
        mCloseButton.setBackgroundColor(message.getBackgroundColor());
        mCloseButton.setTint(message.getCloseButtonColor());
        show();
    }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        viewParams.x = 0;
        viewParams.y = 0;
        return viewParams;
    }

    @Override
    public String getDebugName() {
        return getClass().getName();
    }
}
