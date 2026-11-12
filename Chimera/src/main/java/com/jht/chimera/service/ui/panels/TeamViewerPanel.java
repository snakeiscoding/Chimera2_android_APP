package com.jht.chimera.service.ui.panels;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.workoutapps.AppManager;
import com.jht.common.CmdProcessHelper;
import com.jht.datamatrixlib.DataMatrix;

public class TeamViewerPanel extends ConstraintLayout implements IPanel {
    private static final String TAG = "TeamViewerPanel";

    private AppCompatImageButton mToggleTeamViewerButton;
    private AppCompatImageButton mCloseButton;
    private AppCompatImageView mLeftArrow;
    private final DataMatrix mDataMatrix = new DataMatrix(); // this is set/cleared on panel add/remove
    private Context mContext;

    private long mPressTime = 0;
    private boolean mLargeSize = true;
    private boolean showChimera = true;

    public TeamViewerPanel(Context context) {
        super(context);
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.teamviewer_ui_panel, this, true);
        mToggleTeamViewerButton = findViewById(R.id.toggle_teamviewer_button);
        mCloseButton = findViewById(R.id.close_teamviewer_button);
        AppCompatTextView mControlsLabel = findViewById(R.id.teamviewer_controls_label);
        mLeftArrow = findViewById(R.id.left_arrow_image_view);

        mToggleTeamViewerButton.setOnClickListener(v -> {
            long currentTime = SystemClock.uptimeMillis();
            if ((mPressTime + 1000) <= currentTime) {
                mPressTime = currentTime;
                if (showChimera) {
                    LaunchAppUtil.launchSplashScreen();
                    showChimera = false;
                } else {
                    LaunchAppUtil.launchTeamviewer();
                    showChimera = true;
                }
            }
        });

        mCloseButton.setOnClickListener(v -> {
            long currentTime = SystemClock.elapsedRealtime();
            if ((mPressTime + 1000) <= currentTime) {
                mPressTime = currentTime;
                DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
                WorkoutPanelManager.getInstance().showPanel(PanelIds.TEAM_VIEWER_PANEL, false);
                clearAppData();
            }
        });

        mControlsLabel.setOnClickListener(v -> {
            mLargeSize = !mLargeSize;
            if (!mLargeSize) {
                mToggleTeamViewerButton.setVisibility(GONE);
                mCloseButton.setVisibility(GONE);
                mLeftArrow.setRotation(180);
            } else {
                mToggleTeamViewerButton.setVisibility(VISIBLE);
                mCloseButton.setVisibility(VISIBLE);
                mLeftArrow.setRotation(0);
            }

            WorkoutPanelManager.getInstance().updatePanel(PanelIds.TEAM_VIEWER_PANEL);
        });
    }

    private boolean mStartingDemoModeValue = false;

    @Override
    public void panelAdded() {
        // While we are in team viewer mode we must stay in demo mode
        mStartingDemoModeValue = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL);
        mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, inDemoMode -> {
            if (!inDemoMode) {
                DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, true);
            }
        }, true);
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
        mDataMatrix.finish();
        DataMatrix.putBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, mStartingDemoModeValue);
        mLargeSize = true;
        clearAppData();
    }

    @Override
    public void dispose() {}

    private void clearAppData() {
        CmdProcessHelper cmdProcessHelper = new CmdProcessHelper();
        //noinspection HardCodedStringLiteral
        cmdProcessHelper.addCommand("pm clear " + AppManager.TEAM_VIEWER_APP_NAME);
        try {
            cmdProcessHelper.execute();
        } catch (InterruptedException e) {
            Log.e(TAG, "command interrupted ", e);
        }
    }

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        WindowManager.LayoutParams viewParams;
        if (mLargeSize) {
            viewParams = new WindowManager.LayoutParams(300, 150,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.END | Gravity.TOP;
            viewParams.x = 300;
            viewParams.y = 0;
        } else {
            viewParams = new WindowManager.LayoutParams(300, 50,
                    ChimeraEnums.TYPE_SYSTEM_ERROR, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
            viewParams.gravity = Gravity.END | Gravity.TOP;
            viewParams.x = 300;
            viewParams.y = 0;
        }

        return viewParams;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public String getDebugName() {
        return getClass().getName();
    }
}
