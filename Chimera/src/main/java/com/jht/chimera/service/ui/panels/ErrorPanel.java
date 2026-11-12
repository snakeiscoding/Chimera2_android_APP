package com.jht.chimera.service.ui.panels;

import static com.jht.chimera.lib.common.ExternalAssetsUtilKt.findSdCardErrorPanelAsset;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_CZ;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_HANDRAIL;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_IR;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_CLIMBMILL_UCB;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TM_HYBRID_IR_ESTOP;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TM_ROLLER_GUARD;
import static com.jht.chimera.lib.common.config.ChimeraEnums.SAFETY_KEY_TREADMILL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.SystemUtils;
import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.mastboard.data.UiEventKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.service.ui.UiConfiguration;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.liberror.CodeLevel;
import com.jht.liberror.EC;
import com.jht.liberror.ErrorCodes;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Shows the E-stop screen. Displays when the E-stop is pulled and hides when the E-stop is
 * replaced.
 * <p>
 * Created by chris.foyer on 11/8/2017.
 */
public class ErrorPanel extends ConstraintLayout implements IPanel {
    final String TAG = "ErrorPanel";
    private static final int DELAY_TIME = 1000;

    private ImageView mEstopImageView;
    private TextView mEstopTextView;
    private Drawable mImage1;
    private Drawable mImage2;
    private TextView mErrorCodeText;
    private TextView mErrorCodeMessage;
    private Group mErrorCodeGroup;
    private TextView outOfOrderMessage;

    private ConstraintLayout hybridDialogConfirm;
    private Button hybridDialogResetButton;

    private boolean isInflated = false;
    private boolean mIsSafetyKeyShowing = false;
    private boolean mIsErrorCodeShowing = false;
    private boolean mIsOutOfOrderShowing = false;
    private boolean mDisableSafetyScreens = false;

    private String errorCodeString;
    private String errorMessage;

    private final DataMatrix mDataMatrix = new DataMatrix();
    private Context mContext;
    private final Handler mHandler = new Handler();
    private boolean isOutOfOrder = DataMatrix.getBoolean(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL, false);

    private boolean hybridTreadmillNeedsConfirmation = false;
    private boolean launchSplashOnDismiss = false;

    public ErrorPanel(Context context) {
        super(context);
        initialize(context);
    }

    public ErrorPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ErrorPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public void initialize(Context context) {
        mContext = context;
        inflate();

        mDataMatrix.createIntAdapter(HALKeys.ACTION_ERROR_CODE_INT, this::handleErrorCode, false);

        mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL, aBoolean -> {
            mIsErrorCodeShowing = false;
            runPanelLogic();
        }, false);

        mDataMatrix.createBoolAdapter(UiServiceKeys.ACTION_DISABLE_SAFETY_SCREENS_BOOL, value -> {
            mDisableSafetyScreens = value;
            runPanelLogic();
        }, false);

        mDataMatrix.createBoolAdapter(SettingKeys.General.DATA_SHOW_OUT_OF_ORDER_BOOL, outOfOrder -> {
            isOutOfOrder = outOfOrder;
            runPanelLogic();
        }, false);

        mDataMatrix.createBoolAdapter(HALKeys.DATA_SIMULATED_ESTOP_BOOL, value -> runPanelLogic(), false);
        mDataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, value -> runPanelLogic(), false);
        mDataMatrix.createBoolAdapter(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL, value -> runPanelLogic(), false);
        mDataMatrix.createBoolAdapter(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL, value -> runPanelLogic(), false);
        mDataMatrix.createIntAdapter(SettingKeys.General.DATA_SELECTED_LANGUAGE_ENUM_INT, (lang) -> {
            if (outOfOrderMessage != null) {
                outOfOrderMessage.post(() -> outOfOrderMessage.setText(R.string.out_of_order_message_pg));
            }
        }, false);
    }
    @Override
    public void dispose() {
        mDataMatrix.finish();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void inflate() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.estop_layout, this, true);
        mErrorCodeText = findViewById(R.id.error_code_text);
        mErrorCodeMessage = findViewById(R.id.error_code_message_text);
        mErrorCodeGroup = findViewById(R.id.error_code_group);
        mEstopImageView = findViewById(R.id.estop_picture_image_view);
        mEstopTextView = findViewById(R.id.estop_title_text);
        outOfOrderMessage = findViewById(R.id.out_of_order_message);
        hybridDialogConfirm = findViewById(R.id.hybrid_reset_layout);
        hybridDialogResetButton = findViewById(R.id.hybrid_reset_button);
        isInflated = true;
    }

    public void runPanelLogic() {
        boolean demoMode = DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL);
        boolean isMachineSetup = DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IS_MACHINE_SETUP_BOOL);
        boolean simulateEStop = DataMatrix.getBoolean(HALKeys.DATA_SIMULATED_ESTOP_BOOL);

        UiConfiguration uiConfig = UiConfiguration.getCurrent();
        if (!uiConfig.getActivityOutOfOrder().isEmpty() && isOutOfOrder) {
            // if the uiConfig is not empty that means we have a screen to show for ooo screen.
            // new ui has a screen in its own module.
            // old ui does not have a screen, it has this panel and the logic below.
            // TODO rework this when we tackle error and e-stop panels
            return;
        }

        // if show out of order is enabled, show it
        if (isMachineSetup && !mDisableSafetyScreens && !isOutOfOrder) {
            mIsOutOfOrderShowing = false;
            if (isInflated) {
                setErrorPanelConfiguration();
            }
        } if (isMachineSetup && !mDisableSafetyScreens && isOutOfOrder) {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
            DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.OUT_OF_ORDER_SCREEN.name());
            mIsOutOfOrderShowing = true;
            mIsSafetyKeyShowing = false;
            mIsErrorCodeShowing = false;
            if (isInflated) {
                setErrorPanelConfiguration();
            }
            showPanel(true, true);
        }
        // don't show error panel if demo mode, or machine is not setup, or ignore error code is enabled
        else if ((demoMode && !simulateEStop) || !isMachineSetup || mDisableSafetyScreens) {
            showPanel(false, !mDisableSafetyScreens);
        }
        // if there is an error code, show it, except for the condition the safety key is showing
        else if (mIsErrorCodeShowing && !mIsSafetyKeyShowing) {
            DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
            showPanel(true, false);
        } else {
            checkSafetyKey(simulateEStop);
        }
    }

    private void checkSafetyKey(boolean simulateEStop) {
        // logic that check if the safety key image needs to be shown
        int statusBitmap = DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT);
        boolean needToShow = false;
        boolean bailToSplash = true;

        if (!DataMatrix.getBoolean(SettingKeys.Hardware.DATA_IGNORE_ALL_LEVEL_ERRORS_BOOL)) {
            boolean isSafetyKeyPulled = false;
            boolean climbmillEStopTriggered = false;
            boolean handrailStopTriggered = false;
            boolean controlZoneTriggered = false;
            boolean irSensorTriggeredClimbmill = false;
            boolean irSensorTriggeredTreadmill = false;
            boolean isRollerGuardPulled = false;
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Treadmill) {
                isSafetyKeyPulled = (statusBitmap & SAFETY_KEY_TREADMILL) > 0;
                isRollerGuardPulled = (statusBitmap & SAFETY_KEY_TM_ROLLER_GUARD) > 0;
                irSensorTriggeredTreadmill = (statusBitmap & SAFETY_KEY_TM_HYBRID_IR_ESTOP) > 0;

                if(frameConfig.TM_SUPPORTS_REVERSE_BELT && (irSensorTriggeredTreadmill || hybridTreadmillNeedsConfirmation)) {
                    showHybridResetDialog(irSensorTriggeredTreadmill);
                }
            }

            if (frameConfig.machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
                climbmillEStopTriggered = (statusBitmap & SAFETY_KEY_CLIMBMILL_UCB) > 0;
                handrailStopTriggered = (statusBitmap & SAFETY_KEY_CLIMBMILL_HANDRAIL) > 0;
                controlZoneTriggered = (statusBitmap & SAFETY_KEY_CLIMBMILL_CZ) > 0;
                irSensorTriggeredClimbmill = (statusBitmap & SAFETY_KEY_CLIMBMILL_IR) > 0;
            }

            if (isRollerGuardPulled) { // Roller Guard > EStop for priority
                setupImagesForFrame(frameConfig, "roller_guard");
                needToShow = true;
            } else if (irSensorTriggeredTreadmill) {
                setupImagesForFrame(frameConfig, "hybrid_ir_sensor"); // could do "ir_frame_sensor" under a "hybrid" category
                needToShow = true;
                //bailToSplash = false; // TODO
                /*
                boolean isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
                // only show if not in workout since it's supposed to pause the workout otherwise
                if (frameConfig.HAS_IR_SENSOR && !isInWorkout) {
                    setupImagesForFrame(frameConfig, "hybrid_ir_sensor");
                    needToShow = true;
                    bailToSplash = false;
                }
                */
            } else if ((isSafetyKeyPulled || simulateEStop) && !hybridTreadmillNeedsConfirmation) {
                setupImagesForFrame(frameConfig, "safety_key");
                needToShow = true;
            } else if (climbmillEStopTriggered) {
                setupImagesForFrame(frameConfig, "climbmill_mast_estop");
                needToShow = true;
            } else if (handrailStopTriggered) {
                setupImagesForFrame(frameConfig, "climbmill_hndl_estop");
                needToShow = true;
            } else if (controlZoneTriggered) {
                boolean isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
                // only show the IR and control zone on flavor that needs it
                // only show if not in workout since it's supposed to pause the workout otherwise
                if (BuildConfig.SHOW_CLIMBMILL_IR_CZ_SAFETY_SCREEN && !isInWorkout) {
                    setupImagesForFrame(frameConfig, "control_zone");
                    needToShow = true;
                    bailToSplash = false;
                }
            } else if (irSensorTriggeredClimbmill) {
                boolean isInWorkout = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP;
                // only show the IR and control zone on flavor that needs it
                // only show if not in workout since it's supposed to pause the workout otherwise
                if (BuildConfig.SHOW_CLIMBMILL_IR_CZ_SAFETY_SCREEN && !isInWorkout) {
                    setupImagesForFrame(frameConfig, "ir_frame_sensor");
                    needToShow = true;
                    bailToSplash = false;
                }
            }
            //Block if waiting on hybrid treadmill manual dialog confirmation
            if(hybridTreadmillNeedsConfirmation){
                Log.w(TAG, "Waiting on Hybrid Treadmill Reset Confirmation");
                needToShow = true;
                bailToSplash = false;
            }
        }
        mIsSafetyKeyShowing = needToShow;

        if (isInflated) {
            setErrorPanelConfiguration();
        }
        showPanel(needToShow, bailToSplash);
    }

    /** Sets the background images per frame for given display item */
    private void setupImagesForFrame(FrameConfig frameConfig, String item) {
        // TODO - we should probably add resource ID or image URI to a frame config assets map field instead of growing special conditions
        String frameCategory;
        if (ProductConfig.isProjectOne()) {
            frameCategory = "p1";
        } else if (frameConfig.isLifestyle) {
            frameCategory = "lifestyle";
        } else if (!ProductConfig.isRetail()) {
            frameCategory = "performance";
        } else {
            frameCategory = "default";
        }
        // check expected category, fallback to default if missing
        if (!setupImagesForFrame(frameCategory, item) && !frameCategory.equals("default")) {
            setupImagesForFrame("default", item);
        }
    }
    private boolean setupImagesForFrame(String frameCategory, String item) {
        // if file does not exist, images end up null so only will see text
        mImage1 = mImage2 = null;
        List<File> files = findSdCardErrorPanelAsset(frameCategory, item);
        if (!files.isEmpty()) mImage1 = Drawable.createFromPath(files.get(0).getAbsolutePath());
        if (files.size() > 1) mImage2 = Drawable.createFromPath(files.get(1).getAbsolutePath());
        else mImage2 = mImage1;
        return mImage1 != null;
    }

    private void showPanel(boolean show, boolean showSplashScreen) {
        launchSplashOnDismiss = !show && showSplashScreen; // only arm on an actual hide request
        DataMatrix.putBoolean(UiServiceKeys.DATA_SHOW_ERROR_PANEL_BOOL, show);
        IWorkoutPanelManager panelManager = WorkoutPanelManager.getInstance();
        if (panelManager != null) panelManager.showPanel(PanelIds.E_STOP_PANEL, show);
    }

    /**
     * This is responsible for displaying the error panel if the given error code meets a specific
     * set of requirements. Due to time constraints, the specific requirements are "self-documented"
     * through the code... Sorry.
     *
     * @param value The given error code.
     */
    private void handleErrorCode(final int value) {
        final EC errorCode = ErrorCodes.findErrorCode(value);
        Log.i(TAG, "logErrorCode: " + errorCode + " " + errorCode.getLevel());

        // parker doesn't show error code except for climbmill which only show safety ones
        if (ProductConfig.isRetail()) {
            // for overspeed, these errors are not to be ignored, apparently estop reset might not clear them and they may need power cycle
            // https://jhtnaee.atlassian.net/browse/PRKS-1479
            if (!ErrorCodes.OverAcPowerInputVoltage.equals(errorCode) && !ErrorCodes.OverDcBusVoltage.equals(errorCode)) {
                // Return if it's not a Climbmill or Treadmill
                if (!SystemUtils.isClimbmill() && !SystemUtils.isTreadmill()) return;

                // Return if it's not a safety error.
                if (!errorCode.isSafetyError()) return;
            }
        }

        // check if we need to ignore all error codes
        if (SystemUtils.ignoreAllErrors()) {
            Log.i(TAG, "IGNORE ALL -- Ignoring error code: " + errorCode);
            return;
        }

        // Ignore all non-A-level error codes.
        if (errorCode.getLevel() != CodeLevel.A) {
            Log.i(TAG, "Ignore non-A-level error: " + errorCode);
            return;
        }

        mIsErrorCodeShowing = true;
        errorCodeString = String.format(Locale.ENGLISH, "%04X", errorCode.getCode());
        errorMessage = errorCode.getName();
        Log.w(TAG, "mErrorCodeText?: " + errorCodeString);
        runPanelLogic();
    }

    private final Runnable mSetImage1 = new Runnable() {
        @Override
        public void run() {
            mEstopImageView.setImageDrawable(mImage1);
            mHandler.postDelayed(mSetImage2, DELAY_TIME);
        }
    };

    private final Runnable mSetImage2 = new Runnable() {
        @Override
        public void run() {
            mEstopImageView.setImageDrawable(mImage2);
            mHandler.postDelayed(mSetImage1, DELAY_TIME);
        }
    };

    private void setErrorPanelConfiguration() {
        outOfOrderMessage.setVisibility(GONE);
        mErrorCodeGroup.setVisibility(GONE);

        // Show E-Stop title
        mEstopTextView.setVisibility(View.VISIBLE);
        if (FrameConfigManager.getInstance().getFrameConfig().machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill) {
            mEstopTextView.setText(getResources().getString(R.string.stop_engaged_tc));
        } else {
            mEstopTextView.setText(getResources().getString(R.string.emergency_stop_engaged_tc));
        }
        if (mIsOutOfOrderShowing) {
            mEstopTextView.setVisibility(View.GONE);
            outOfOrderMessage.setVisibility(VISIBLE);
            mHandler.removeCallbacksAndMessages(null);
            if (mEstopImageView != null) {
                mEstopImageView.setImageDrawable(null);
            }
        } else if (mIsSafetyKeyShowing) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.post(mSetImage1);
        } else if (mIsErrorCodeShowing) {
            mEstopTextView.setVisibility(INVISIBLE);
            mErrorCodeText.setText(errorCodeString);
            mErrorCodeMessage.setText(errorMessage);
            mErrorCodeGroup.setVisibility(VISIBLE);
        } else {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.post(mSetImage1);
        }
    }

    public void showHybridResetDialog(boolean eStopActive){
        if (hybridDialogConfirm != null) {
            hybridDialogConfirm.setVisibility(VISIBLE);
            hybridTreadmillNeedsConfirmation = true;
        }
        if (hybridDialogResetButton != null) {
            hybridDialogResetButton.setOnClickListener(v -> {
                hybridTreadmillNeedsConfirmation = false;
                if (hybridDialogConfirm != null) {
                    hybridDialogConfirm.setVisibility(GONE);
                }
                checkSafetyKey(false);
                // Reset the incline to 0 (safe to start should always return true but check just in case)
                if (DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)) {
                    DataMatrix.putFloat(HALKeys.ACTION_SET_INCLINE_FLOAT, 0.0f);
                }
            });
            //Reset button is active as long as the IR event itself is cleared (regardless of physical key)
            hybridDialogResetButton.setEnabled(!eStopActive);
        }
    }

    @Override
    public void panelAdded() {
        setErrorPanelConfiguration();
    }

    @Override
    public void panelUpdated() {}

    @Override
    public void panelRemoved() {
        mHandler.removeCallbacksAndMessages(null);
        mIsErrorCodeShowing = false;
        mIsOutOfOrderShowing = false;
        mIsSafetyKeyShowing = false;
        errorCodeString = "";
        errorMessage = "";
        if (mEstopImageView != null) {
            mEstopImageView.setImageDrawable(null);
        }
        hybridTreadmillNeedsConfirmation = false;
        hybridDialogConfirm.setVisibility(GONE);
        if (launchSplashOnDismiss) {
            LaunchAppUtil.launchSplashScreen();
        }
    }

    @Override
    public boolean setPanelMode(WorkoutPanelMode mode) { return false; }

    @Override
    public WindowManager.LayoutParams getPanelParams() {
        WindowManager.LayoutParams viewParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH, PixelFormat.TRANSPARENT);
        viewParams.gravity = Gravity.NO_GRAVITY;
        viewParams.x = 0;
        viewParams.y = 0;
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
