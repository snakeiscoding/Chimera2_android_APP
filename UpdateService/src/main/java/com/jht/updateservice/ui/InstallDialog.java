package com.jht.updateservice.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.jht.updateservice.R;

import org.jetbrains.annotations.NonNls;

import java.util.Timer;
import java.util.TimerTask;

@SuppressLint({"SetTextI18n", "ViewConstructor"})
public class InstallDialog extends ConstraintLayout {
    @NonNls private static final String TAG = "InstallDialog";
    protected Context context;

    protected Handler mHandler = new Handler();

    private View mTopLeftView;
    private View mTopRightView;
    private View mBottomLeftView;
    private View mBottomRightView;

    private AppCompatTextView mInstallLabel;
    private AppCompatTextView mInstallSubLabel;
    private AppCompatTextView mInstallDescription;
    private AppCompatImageView mSpinnerView;

    private AppCompatTextView mVersionTextView;

    private LinearLayout mErrorLayout;
    private TextView mErrorText;
    private Button mRestartButton;

    private TextView mTitleTextView;
    private int mStopUpdateTouches = 0;
    private Timer mStopUpdateTimer;

    private ConstraintLayout mManualResetLayout;


    private UiEvents mUiEvents;

    /**
     * Used to get methods that interact with the window manager
     */
    public interface UiEvents {
        void hideInstallScreenEvent();
    }

    public InstallDialog(Context context, UiEvents uiEvents, UpdateUi.IUpdateUi updateUiInterface) {
        super(context);
        this.context = context;
        mUiEvents = uiEvents;
        LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater.inflate(R.layout.update_layout, this, true);

        mTopLeftView = findViewById(R.id.top_left_view);
        mTopRightView = findViewById(R.id.top_right_view);
        mBottomLeftView = findViewById(R.id.bottom_left_view);
        mBottomRightView = findViewById(R.id.bottom_right_view);

        mInstallLabel = findViewById(R.id.installing_text_view);
        mInstallSubLabel = findViewById(R.id.install_subtitle);
        mInstallDescription = findViewById(R.id.install_message);
        mSpinnerView = findViewById(R.id.progress_spinner_image_view);

        mErrorLayout = findViewById(R.id.error_message_layout);
        mErrorText = findViewById(R.id.error_description_text_view);
        mRestartButton = findViewById(R.id.reboot_button);

        mVersionTextView = findViewById(R.id.version_text_view);

        mManualResetLayout = findViewById(R.id.system_reset_layout);

        mTitleTextView = findViewById(R.id.title_text_view);
        mTitleTextView.setOnClickListener(v -> {
            if (mStopUpdateTimer != null) {
                mStopUpdateTimer.cancel();
                mStopUpdateTimer.purge();
            }

            mStopUpdateTimer = new Timer();
            mStopUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mStopUpdateTouches = 0;
                }
            }, 2000);

            mStopUpdateTouches++;
            if (mStopUpdateTouches == 10) {
                Log.e(TAG, "Attempting to cancel update!!");
                updateUiInterface.cancelUpdate();
            }
        });

        mRestartButton.setOnClickListener(v -> reboot());

        setOnCornerTouchListeners();
    }

    private void setInstallViewsVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        mInstallLabel.setVisibility(visibility);
        mInstallSubLabel.setVisibility(visibility);
        mInstallDescription.setVisibility(visibility);

        if (visibility == View.VISIBLE) {
            mSpinnerView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.rotate_progress));
        }
        else {
            mSpinnerView.clearAnimation();
        }
        mSpinnerView.setVisibility(visibility);
    }

    private void setFirstTimeInstallVisibility() {
        mInstallLabel.setVisibility(View.VISIBLE);
        mInstallSubLabel.setVisibility(View.INVISIBLE);
        mInstallDescription.setVisibility(View.INVISIBLE);
        mSpinnerView.setVisibility(View.INVISIBLE);
        mInstallLabel.setText("Please insert a USB to begin installation"); //NON-NLS
    }

    public void setVersionText(final String version) {
        mHandler.post(() -> mVersionTextView.setText(version));
    }

    public void showInitialUpdateScreen() {
        mHandler.post(this::setFirstTimeInstallVisibility);
    }

    public void showInstalling(final String packageName) {
        mHandler.post(() -> {
            setInstallViewsVisibility(true);

            mInstallLabel.setText("Installing: " + packageName); //NON-NLS
            mInstallSubLabel.setText("");
            mInstallDescription.setText("");
        });
    }

    public void showDownloading(final boolean isStorageUpdate) {
        mHandler.post(() -> {
            setInstallViewsVisibility(true);

            mInstallLabel.setText("Downloading Updates"); //NON-NLS
            if (!isStorageUpdate) {
                mInstallSubLabel.setText("0%");
            } else {
                mInstallSubLabel.setText("");
            }
        });
    }

    public void updateDownloadProgress(final int progress) {
        mHandler.post(() -> mInstallSubLabel.setText(progress + "%"));
    }

    public void updateInstallProgress(final String message) {
        mHandler.post(() -> mInstallSubLabel.setText(message));
    }

    public void showError(final String msg, final boolean canReboot) {
        mHandler.post(() -> {
            setInstallViewsVisibility(false);
            mErrorText.setText("There was an error installing the software.\n\n" + msg); //NON-NLS
            Log.d(TAG, "There was an error installing the software: " + msg);
            mRestartButton.setVisibility(canReboot ? View.VISIBLE : View.GONE);
            mRestartButton.requestFocus();
            mErrorLayout.setVisibility(View.VISIBLE);
        });
    }

    public void showSuccess() {
        mHandler.post(() -> {
            setInstallViewsVisibility(true);
            mInstallLabel.setText("Install Complete"); //NON-NLS
            mInstallDescription.setText("Console will restart in a moment, please wait"); //NON-NLS
        });
    }

    public void showSystemResetScreen() {
        mHandler.post(() -> mManualResetLayout.setVisibility(View.VISIBLE));
    }

    public void reboot() {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        powerManager.reboot("");
    }

    private void setOnCornerTouchListeners() {
        mTopLeftView.setOnClickListener(v -> {
            Log.d(TAG, "touched top left");
            trackCornerTouches(TOP_LEFT);
        });

        mTopRightView.setOnClickListener(v -> {
            Log.d(TAG, "touched top right");
            trackCornerTouches(TOP_RIGHT);
        });

        mBottomLeftView.setOnClickListener(v -> {
            Log.d(TAG, "touched bottom left");
            trackCornerTouches(BOTTOM_LEFT);
        });

        mBottomRightView.setOnClickListener(v -> {
            Log.d(TAG, "touched bottom right");
            trackCornerTouches(BOTTOM_RIGHT);
        });
    }

    private static final int TOP_LEFT = 0;
    private static final int TOP_RIGHT = 1;
    private static final int BOTTOM_LEFT = 2;
    private static final int BOTTOM_RIGHT = 3;

    private TouchHelper topLeft = new TouchHelper();
    private TouchHelper topRight = new TouchHelper();
    private TouchHelper bottomLeft = new TouchHelper();
    private TouchHelper bottomRight = new TouchHelper();

    private void trackCornerTouches(int corner) {
        switch (corner) {
            case TOP_LEFT:
                topLeft.touched();
                break;
            case TOP_RIGHT:
                topRight.touched();
                break;
            case BOTTOM_LEFT:
                bottomLeft.touched();
                break;
            case BOTTOM_RIGHT:
                bottomRight.touched();
                break;
            default:
                break;
        }

        if (topLeft.isTouched() && topRight.isTouched() && bottomLeft.isTouched() && bottomRight.isTouched()) {
            Log.d(TAG, "Touch events triggered, hiding the software installer!");
            if (mUiEvents != null) mUiEvents.hideInstallScreenEvent();
        }
    }

    private static final int TOUCH_TIMEOUT = 10000;

    private class TouchHelper {
        private Timer touchTimer = new Timer();
        boolean trackedBoolean = false;

        boolean isTouched() {
            return trackedBoolean;
        }

        void touched() {
            trackedBoolean = true;
            touchTimer.cancel();
            touchTimer = new Timer();
            touchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    trackedBoolean = false;
                }
            }, TOUCH_TIMEOUT);
        }
    }
}
