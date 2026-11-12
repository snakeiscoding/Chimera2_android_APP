package com.jht.chimera.service.ui.panels.messagecenter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androiduiwidgets.dialog.DialogBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.widget.dialog.MessageCenterMessage;
import com.jht.chimera.lib.widget.selectron.SelectronButton;
import com.jht.chimera.lib.widget.textview.EllipsizingTextView;
import com.jht.chimera.service.ui.PanelManager;
import com.jht.chimera.service.ui.panels.ManagedPanel;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.widgets.WebBrowserComposeKt;
import com.jht.widgets.WebBrowserComposeOptions;

import java.util.UUID;

/**
 * A UI panel that shows at the top middle of the screen and displays the contents of a
 * {@link MessageCenterMessage}. If the message is too long it will be ellipsized and clicking
 * on it will bring up a {@link MessageCenterModalPanel} with the full message.
 */
public class MessageCenterTopPanel extends ManagedPanel {
    private final ConstraintLayout mTextLayout;
    private final SelectronButton mActionButton;
    private final SelectronButton mCloseButton;
    private final EllipsizingTextView mMessageTextView;
    private final JHTTimer hideTimer = new JHTTimer();

    public MessageCenterTopPanel(final Context context, UUID id) {
        this(context, id, null);
    }

    public MessageCenterTopPanel(final Context context, UUID id, PanelManager panelManager) {
        super(context, id, panelManager);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.message_center_hr_view, this, true);

        mTextLayout = findViewById(R.id.layout_message_center);
        mActionButton = findViewById(R.id.action_button);
        mCloseButton = findViewById(R.id.message_close_btn);
        mMessageTextView = findViewById(R.id.text_message);
        mMessageTextView.setMaxLines(2);
        mMessageTextView.addEllipsizeListener(ellipsized ->
            mCloseButton.setVisibility(ellipsized ? View.INVISIBLE : View.VISIBLE));
    }


    @Override
    public void panelRemoved() {
        hideTimer.stop(); // if panel is removed, stop hide timer
    }

    public void showMessage(@Nullable MessageCenterMessage message, @NonNull IOpenModal onModal, DialogBase.IOnCloseDialog onClose) {
        if(message == null) {
            hideTimer.stop();
            hide();
            return;
        }

        mActionButton.setVisibility(message.getActionUrl().isEmpty() ? GONE : VISIBLE);
        if (message.getActionText().isEmpty()) {
            mActionButton.setText("");
        } else {
            mActionButton.setText(message.getActionText());
        }
        mActionButton.setOnClickListener(view -> {
            // Considered altering old WebBrowserActivity or using webViewDialogBox, but a composable solution might ultimately be better long run
            int timeoutSeconds = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_AUTO_HIDE_TIMEOUT_SEC_INT); // not sure what timeout to use
            WebBrowserComposeOptions options = new WebBrowserComposeOptions(timeoutSeconds * 1000);
            WebBrowserComposeKt.launchWebBrowserCompose(getContext(), message.getActionUrl(), options);
            // force dismiss the message panel so it does not block the web activity (though if there is another message queued, it may show next)
            hideTimer.stop();
            onClose.onCloseDialog();
        });

        mMessageTextView.setText(message.getMessageText());
        hideTimer.setOnTimer(onClose::onCloseDialog); // note: this timer might not be on UI thread
        mCloseButton.setOnClickListener(view -> {
            hideTimer.stop();
            onClose.onCloseDialog();
        });
        mTextLayout.setBackgroundColor(message.getBackgroundColor());
        mCloseButton.setBackgroundColor(message.getHeaderColor());
        mCloseButton.setTint(message.getCloseButtonColor());
        mActionButton.setBackgroundColor(0xFFFFCB27); // these colors are fixed at the moment, might consider using theme when converted to compose
        mActionButton.setTint(Color.BLACK);
        mMessageTextView.setTextColor(message.getTextColor());

        if (message.isCloseButtonHidden()) {
            mCloseButton.setVisibility(View.GONE);
        }
        mMessageTextView.setOnClickListener((v) -> {
            if(mMessageTextView.isEllipsized()) {
                onModal.openModal(message);
            }
        });
        hideTimer.start(message.getHideTime() * 1000);
        show();
    }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        boolean isFullscreen = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) != UiServiceKeys.WorkoutPanelState.REGULAR; // treat any state outside of regular as full width
        if (isFullscreen) {
            int topBarHeight = (int) getResources().getDimension(R.dimen.top_bar_height);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, topBarHeight,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        } else {
            int topBarHeight = (int) getResources().getDimension(R.dimen.top_bar_height);
            int width = (int) getResources().getDimension(R.dimen.app_container_width);
            WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(width, topBarHeight,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            viewParams.x = 0;
            viewParams.y = 0;
            return viewParams;
        }
    }

    @Override
    public String getDebugName() {
        return getClass().getName();
    }
}
