package com.jht.chimera.lib.widget.tv;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.tv.tuner.IChimeraTuner;
import com.jht.serialport.peripheral.tuner.ITuner;
import com.jht.serialport.peripheral.tuner.ITunerCapabilities;
import com.jht.serialport.peripheral.tuner.channel.IChannel;

public class TVExtendedControls extends ConstraintLayout {
    private static final String TAG = "TVExtendedControls";
    public enum CORNER_BUTTON_OPERATION {
        MENU(R.drawable.ic_menu_black_80dp),
        SETUP(R.drawable.ic_setting_black),
        EXIT(R.drawable.ic_close_black_80dp),
        BACK(R.drawable.ic_tv_icon_back),
        INFO(R.drawable.ic_icon_info_available_black),
        CC(R.drawable.ic_icon_cc),
        GUIDE(R.drawable.ic_tv_icon_guide);

        private final int drawableId;

        public int getDrawableId() { return drawableId; }

        CORNER_BUTTON_OPERATION(int drawableId) {
            this.drawableId = drawableId;
        }
    }

    public interface ITVExtendedControls {
        void cornerButtonOperation(CORNER_BUTTON_OPERATION operation);
        void left();
        void right();
        void up();
        void down();
        void select();
    }

    public enum CORNER_BUTTONS {
        TOP_LEFT(R.id.eng_tv_control_expanded_upper_left),
        TOP_LEFT_2(R.id.eng_tv_control_expanded_upper_left_2),
        TOP_RIGHT(R.id.eng_tv_control_expanded_upper_right),
        BOTTOM_LEFT(R.id.eng_tv_control_expanded_bottom_left),
        BOTTOM_RIGHT(R.id.eng_tv_control_expanded_bottom_right);

        private final int viewId;

        public int viewId() { return viewId; };

        CORNER_BUTTONS(int viewId) {
            this.viewId = viewId;
        }
    }

    private CORNER_BUTTON_OPERATION[] cornerButtonOperation = {
            CORNER_BUTTON_OPERATION.MENU,
            CORNER_BUTTON_OPERATION.GUIDE,
            CORNER_BUTTON_OPERATION.EXIT,
            CORNER_BUTTON_OPERATION.INFO,
            CORNER_BUTTON_OPERATION.CC,
    };


    public TVExtendedControls(Context context) {
        super(context);
        init(context);
    }

    public TVExtendedControls(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TVExtendedControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context) {
        inflate(context, R.layout.tv_extended_controls, this);
    }

    public void setCornerButton(CORNER_BUTTONS corner, CORNER_BUTTON_OPERATION operation ) {
        AppCompatImageButton button = findViewById(corner.viewId);
        if(button != null) {
            cornerButtonOperation[corner.ordinal()] = operation;
            button.setImageResource(operation.getDrawableId());
        }
    }

    public void setOnKeyPress(ITVExtendedControls callback) {
        if(callback != null) {
            for(CORNER_BUTTONS button : CORNER_BUTTONS.values()) {
                findViewById(button.viewId).setOnClickListener(v -> callback.cornerButtonOperation(cornerButtonOperation[button.ordinal()]));
            }
            findViewById(R.id.eng_tv_control_up).setOnClickListener(v -> callback.up());
            findViewById(R.id.eng_tv_control_left).setOnClickListener(v -> callback.left());
            findViewById(R.id.eng_tv_control_confirm).setOnClickListener(v -> callback.select());
            findViewById(R.id.eng_tv_control_right).setOnClickListener(v -> callback.right());
            findViewById(R.id.eng_tv_control_down).setOnClickListener(v -> callback.down());
        }
    }

    private void enableButton(View view, boolean enable) {
        view.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        view.setEnabled(enable);
    }

    private boolean isCornerButtonAvailable(CORNER_BUTTON_OPERATION operation, ITunerCapabilities capabilities) {
        switch (operation) {
            case MENU:
                return capabilities.menu();
            case SETUP:
                return capabilities.setup();
            case EXIT:
                return capabilities.exit();
            case BACK:
                return capabilities.back();
            case INFO:
                return capabilities.info();
            case CC:
                return capabilities.cc();
            case GUIDE:
                return capabilities.guide();
        }
        return false;
    }

    public void showButtons(ITunerCapabilities capabilities) {

        for(CORNER_BUTTONS button : CORNER_BUTTONS.values()) {
            Log.d(TAG, "show corner button " + button + " " + cornerButtonOperation[button.ordinal()] + " " + isCornerButtonAvailable(cornerButtonOperation[button.ordinal()], capabilities));
            enableButton(findViewById(button.viewId), isCornerButtonAvailable(cornerButtonOperation[button.ordinal()], capabilities));
        }
        enableButton(findViewById(R.id.eng_tv_control_confirm), capabilities.enter());
    }

    private void executeTunerOperation(IChimeraTuner<? extends IChannel> tuner, CORNER_BUTTON_OPERATION operation) {
        switch(operation) {

            case MENU:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.MENU);
                break;
            case SETUP:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.SETUP);
                break;
            case EXIT:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.EXIT);
                break;
            case BACK:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.BACK);
                break;
            case INFO:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.INFO);
                break;
            case CC:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.TOGGLE_CC);
                break;
            case GUIDE:
                tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.GUIDE);
                break;
        }
    }

    public void setTuner(IChimeraTuner<? extends IChannel> tuner) {
        if(tuner != null) {
            for(CORNER_BUTTONS button : CORNER_BUTTONS.values()) {
                findViewById(button.viewId).setOnClickListener(v -> executeTunerOperation(tuner, cornerButtonOperation[button.ordinal()]));
            }
            findViewById(R.id.eng_tv_control_up).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.UP));
            findViewById(R.id.eng_tv_control_left).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.LEFT));
            findViewById(R.id.eng_tv_control_confirm).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.SELECT));
            findViewById(R.id.eng_tv_control_right).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.RIGHT));
            findViewById(R.id.eng_tv_control_down).setOnClickListener(v -> tuner.tuner().clickRemoteButton(ITuner.REMOTE_BUTTON.DOWN));
            showButtons(tuner.tuner().capabilities());

        }

    }

}
