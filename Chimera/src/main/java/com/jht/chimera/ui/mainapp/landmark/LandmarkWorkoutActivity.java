package com.jht.chimera.ui.mainapp.landmark;

import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.math.MathUtils;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.landmark.AnimationLoader;
import com.jht.chimera.lib.landmark.ImageCacher;
import com.jht.chimera.lib.landmark.LandmarkDataKt;
import com.jht.chimera.lib.widget.ScaleView;
import com.jht.chimera.lib.widget.activity.TimeoutActivity;
import com.jht.data.LandmarkWorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.unit.UnitConverterKt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class LandmarkWorkoutActivity extends TimeoutActivity
        implements Slideshow.Callback, android.view.SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    final static String TAG = "LandmarkWorkoutActivity";

    // widget
    View mContentView;
    LandmarkProgressBar mProgressBar;
    TextView mGoalValueText;
    TextView mGoalValueUnitText;
    TextView mLocationText;
    TextView mBuildingNameText;
    TextView mHeightText;
    ImageView mSnapshot;
    ImageView mSnapshot2;
    ImageView mSilhouette;
    SurfaceView mVideoHolder;
    View mLeftPanelSpacer;
    View mRightPanelSpacer;

    // data
    int totalFloors;
    LandmarkWorkoutItem mLandmarkWorkoutItem;

    // Background
    ImageCacher mSlideshowCacher;
    Slideshow mSlideshow;

    MediaPlayer mMediaPlayer;
    boolean mVideoError = false;
    Handler mHandler = new Handler();
    int mFloorsCompleted = 0;

    DataMatrix mDataMatrix = new DataMatrix();

    private float mFloorsOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landmark_workout_layout);
        ScaleView.scaleContents(getWindow().getDecorView().findViewById(android.R.id.content));

        // will always be landmark title since we started a landmark workout and this is the landmark activity
        mLandmarkWorkoutItem = LandmarkDataKt.getLandmarkDataFromTitle(DataMatrix.getString(WorkoutKeys.DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING));
        if (mLandmarkWorkoutItem.getAppId().equals("EMPTY")) {
            Log.e(TAG, "DATA_SELECTED_VA_OR_LANDMARK_TITLE_STRING is NOT populated");
            return; // dm key is not populated correctly
        }

        mSlideshowCacher = new ImageCacher();
        List<Uri> slideshowImages = new ArrayList<>(mLandmarkWorkoutItem.getBackground());

        try {
            mSlideshowCacher.buildImages(slideshowImages, 0);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "onCreate: issue building slideshow: "+e);
        }

        configureUI();
        setupLandmarkActivity();
    }

    void configureUI() {
        mContentView = findViewById(R.id.content);
        mProgressBar = findViewById(R.id.progress_bar);
        mGoalValueText = findViewById(R.id.goalValueText);
        mGoalValueUnitText = findViewById(R.id.goalValueUnitText);
        mBuildingNameText = findViewById(R.id.buildingNameText);
        mLocationText = findViewById(R.id.locationText);
        mHeightText = findViewById(R.id.heightText);
        mSnapshot = findViewById(R.id.snapshot);
        mSnapshot2 = findViewById(R.id.snapshot2);
        mSilhouette = findViewById(R.id.shadow);
        mLeftPanelSpacer = findViewById(R.id.left_panel_spacer);
        mRightPanelSpacer = findViewById(R.id.right_panel_spacer);
    }

    void setupLandmarkActivity() {
        createSurface();

        //DataMatrix.putInt(UiServiceKeys.DATA_WORKOUT_PANELS_SET_INT, UiServiceKeys.WorkoutSet.REGULAR); // redundant, onResume does this too

        mSlideshow = new Slideshow(this);

        String goalValue = "";
        String goalValueUnit = "";

        totalFloors = LandmarkDataKt.getFloorsFromLandmarkItem(mLandmarkWorkoutItem);
        goalValue = String.valueOf(totalFloors);
        goalValueUnit = " " + getResources().getString(R.string.floors_uc);
        mProgressBar.setMax(totalFloors);
        mProgressBar.setProgress(0);

        mGoalValueText.setText(goalValue);
        mGoalValueUnitText.setText(goalValueUnit);

        mBuildingNameText.setText(mLandmarkWorkoutItem.getTitleId());

        mLocationText.setText(mLandmarkWorkoutItem.getLocationTitle());

        // TODO doug verify this works - it does not work lol - now it does
        mHeightText.setText(String.format("%d " + getResources().getString(R.string.m_ac)
                + " / " + "%d " + getResources().getString(R.string.ft_uc), (int)mLandmarkWorkoutItem.getMeters(), (int)UnitConverterKt.getMeterUnit(mLandmarkWorkoutItem.getMeters()).getToFeet()));

        mSilhouette.setImageBitmap(BitmapFactory.decodeFile(mLandmarkWorkoutItem.getSilhouetteUri().getPath()));
    }

    @Override
    protected void onResume() {
        super.onResume();

        DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
        DataMatrix.putBoolean(UiServiceKeys.DATA_APP_HANDLES_PAUSE_BOOL, true);

        if (mLandmarkWorkoutItem == null) { return; }

        createDataMatrixAdapters();

        resumeSlideShow();

        TimerTask videoTask = new TimerTask() {
            @Override
            public void run() {
                playStartVideo(mSurfaceHolder);
            }
        };

        File videoFile = new File(mLandmarkWorkoutItem.getVideoUri().getPath());
        if(videoFile.exists()) {
            Timer videoTimer = new Timer();
            videoTimer.schedule(videoTask, 2000);
        }else{ // never used, as we have video files for all landmark locations
            mHandler.post(() -> {
                        startLaunchAnimation();
                        mSnapshot.setImageBitmap(mSlideshowCacher.getImage());
                        mSnapshot.setVisibility(View.VISIBLE);
                        mSnapshot2.setVisibility(View.INVISIBLE);
                        zoomSnapshot(mSnapshot, mLandmarkWorkoutItem.getXPivotCoordinates().get(0), mLandmarkWorkoutItem.getYPivotCoordinates().get(0), false);

                        AnimationLoader.loadKenburnAnimation(mSnapshot);
                        mSlideshow.resume();
                    }
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        DataMatrix.putBoolean(UiServiceKeys.DATA_APP_HANDLES_PAUSE_BOOL, false);

        mSlideshow.pause();
        mSnapshot.clearAnimation();
        mSnapshot2.clearAnimation();

        mDataMatrix.finish();

        int workoutState = DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT);
        if (workoutState != WorkoutKeys.Constants.RUN_STATE_STOP) {
            DataMatrix.putInt(UiServiceKeys.ACTION_SET_WORKOUT_PANEL_STATE_INT, UiServiceKeys.WorkoutPanelState.REGULAR);
        }
    }

    private void createDataMatrixAdapters() {
        // We need to calculate the goal offset to correctly handle switching workouts
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_GOAL_REMAINING_FLOAT, goalRemaining -> {
            int goalType = DataMatrix.getInt(WorkoutKeys.DATA_GOAL_TYPE_INT);
            if (goalType == WorkoutKeys.Constants.GOAL_TYPE_FLOORS_STAGE) {
                float goalValue = DataMatrix.getFloat(WorkoutKeys.DATA_GOAL_VALUE_FLOAT);
                float floors = DataMatrix.getFloat(WorkoutKeys.DATA_FLOORS_FLOAT);
                float goalProgress = goalValue - goalRemaining;
                mFloorsOffset = floors - goalProgress;
            }
        });

        // Calculate based on total floors with offset based on goal
        mDataMatrix.createFloatAdapter(WorkoutKeys.DATA_FLOORS_FLOAT, floors -> {
            float floorsInStage = floors - mFloorsOffset;
            int floorsClamped = (int) MathUtils.clamp(floorsInStage, 0f, totalFloors);
            if (mFloorsCompleted != floorsClamped) {
                mFloorsCompleted = floorsClamped;
                mProgressBar.setProgress(mFloorsCompleted);
            }
        });

        mDataMatrix.createIntAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, value -> {
            // manipulating the left and right margins on mContentView and the expansion button are smoother, but the code/layout is less clear
            if (value == UiServiceKeys.WorkoutPanelState.FULLSCREEN) {
                // fullscreen
                mLeftPanelSpacer.setVisibility(View.VISIBLE);
                mRightPanelSpacer.setVisibility(View.VISIBLE);
            } else {
                // regular
                mLeftPanelSpacer.setVisibility(View.GONE);
                mRightPanelSpacer.setVisibility(View.GONE);
            }
        });
    }

    private void zoomSnapshot(ImageView snapshot, Float xPivot, Float yPivot, boolean capPivotY) {

        float pivotX = snapshot.getWidth() * xPivot / 100.0f;
        float pivotY = yPivot;

        if (capPivotY && pivotY > 65.0f) {
            pivotY = 65.0f;
        }
        pivotY = snapshot.getHeight() * pivotY / 100.0f;

        snapshot.setPivotX(pivotX);
        snapshot.setPivotY(pivotY);

        snapshot.setScaleX(1.2f); // always 1.2
        snapshot.setScaleY(1.2f); // always 1.2
    }

    private void createSurface() {
        mVideoHolder = (SurfaceView) findViewById(R.id.startVideoHolder);
        SurfaceHolder surfaceHolder = mVideoHolder.getHolder();
        surfaceHolder.setFixedSize(100, 100);
        surfaceHolder.addCallback(this);
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    SurfaceHolder mSurfaceHolder = null;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp != null) {
            mp.release();
            mMediaPlayer = null;
        }
        startLaunchAnimation();
        setupAndStartSlideshow();
    }

    private void setupAndStartSlideshow() {
        mHandler.post(() -> {
            mSnapshot.setImageBitmap(mSlideshowCacher.getImage());
            mSnapshot.setVisibility(View.VISIBLE);
            mSnapshot2.setVisibility(View.INVISIBLE);
            zoomSnapshot(mSnapshot, mLandmarkWorkoutItem.getXPivotCoordinates().get(0), mLandmarkWorkoutItem.getYPivotCoordinates().get(0), false);
            resumeSlideShow();
        });
    }

    private void resumeSlideShow() {
        if (!mSlideshow.isActive()) {
            mSlideshow.resume();
            if (mSnapshot.getVisibility() == View.VISIBLE) {
                AnimationLoader.loadKenburnAnimation(mSnapshot);
            } else {
                AnimationLoader.loadKenburnAnimation(mSnapshot2);
            }
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @SuppressWarnings("deprecation")
    private void playStartVideo(SurfaceHolder holder) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener((mp, what, extra) -> false);
            mMediaPlayer.setScreenOnWhilePlaying(true); // keep the screen on while playing
            mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build());
            mMediaPlayer.setDataSource(mLandmarkWorkoutItem.getVideoUri().getPath());
            mMediaPlayer.prepare();
        } catch (Exception e) {
            mVideoError = true;
        }
    }

    private void startLaunchAnimation() {
        mBuildingNameText.setVisibility(View.INVISIBLE);
        mLocationText.setVisibility(View.INVISIBLE);
        mHeightText.setVisibility(View.INVISIBLE);
        mGoalValueText.setVisibility(View.INVISIBLE);
        mGoalValueUnitText.setVisibility(View.INVISIBLE);
        mSilhouette.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.INVISIBLE);

        mVideoHolder.animate().alpha(0.0f).setDuration(400);
        Animation topAnimation = AnimationUtils.loadAnimation(LandmarkWorkoutActivity.this, R.anim.fade_in_from_top);
        long duration = topAnimation.getDuration();
        mProgressBar.setVisibility(View.VISIBLE);
        mProgressBar.startAnimation(topAnimation);
        mVideoHolder.setVisibility(View.INVISIBLE);

        mHandler.postDelayed(() -> {
            Animation a = AnimationUtils.loadAnimation(LandmarkWorkoutActivity.this, R.anim.slow_fade_in_from_left);
            mSilhouette.setVisibility(View.VISIBLE);
            mSilhouette.startAnimation(a);
        }, duration);

        mHandler.postDelayed(() -> {
            Animation a = AnimationUtils.loadAnimation(LandmarkWorkoutActivity.this, R.anim.text_fade_in_from_left);
            mBuildingNameText.setVisibility(View.VISIBLE);
            mBuildingNameText.startAnimation(a);
        }, duration);

        mHandler.postDelayed(() -> {
            Animation a = AnimationUtils.loadAnimation(LandmarkWorkoutActivity.this, R.anim.text_fade_in_from_left);
            mLocationText.setVisibility(View.VISIBLE);
            mLocationText.startAnimation(a);
        }, duration + 100);

        mHandler.postDelayed(() -> {
            Animation a = AnimationUtils.loadAnimation(LandmarkWorkoutActivity.this, R.anim.text_fade_in_from_left);
            mHeightText.setVisibility(View.VISIBLE);
            mHeightText.startAnimation(a);
        }, duration + 200);

        mHandler.postDelayed(() -> {
            Animation a = AnimationUtils.loadAnimation(LandmarkWorkoutActivity.this, R.anim.shake_fade_in_from_left);
            mGoalValueText.setVisibility(View.VISIBLE);
            mGoalValueUnitText.setVisibility(View.VISIBLE);
            mGoalValueText.startAnimation(a);
            mGoalValueUnitText.startAnimation(a);
        }, duration + 300);
    }

    @Override
    public void onTimeout(final int index) {
        mHandler.post(new Runnable() {
            ImageView activeImage;
            ImageView nextImage;
            Animation.AnimationListener fadeOutComplete = new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation arg0) {
                    mHandler.post(() -> {
                        activeImage.setVisibility(View.INVISIBLE);
                        nextImage.setVisibility(View.VISIBLE);
                        nextImage.setAlpha(1.0f);
                        AnimationLoader.loadKenburnAnimation(nextImage);
                    });
                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                }

                @Override
                public void onAnimationStart(Animation arg0) {
                }
            };

            @Override
            public void run() {
                activeImage = mSnapshot.getVisibility() == View.VISIBLE ? mSnapshot : mSnapshot2;
                nextImage = mSnapshot.getVisibility() == View.VISIBLE ? mSnapshot2 : mSnapshot;
                nextImage.setImageBitmap(mSlideshowCacher.getNextImage());
                try {
                    zoomSnapshot(nextImage, mLandmarkWorkoutItem.getXPivotCoordinate(mSlideshowCacher.position()), mLandmarkWorkoutItem.getYPivotCoordinate(mSlideshowCacher.position()), true);
                } catch (Exception e) {
                    Log.w(TAG, "Cannot zoom image", e);
                }
                nextImage.setVisibility(View.VISIBLE);
                nextImage.setAlpha(1.0f);
                Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(800);
                fadeIn.setAnimationListener(fadeOutComplete);
                fadeIn.setFillAfter(true);
                nextImage.startAnimation(fadeIn);
                activeImage.animate().alpha(0.0f).setDuration(800).start();
            }
        });
    }

}
