package com.jht.chimera.ui.mainapp.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/*
 *This class is for a slideshow of background images on the home screens
 */
public class BackgroundManager {
    private static final String TAG = "BackgroundManager";
    private ImageView mCurrentView;
    private ImageView mNextView;

    // Used to synchronize the timer with the GUI thread.
    private final Handler mHandler;

    // This is the list of backgrounds to use.
    private ArrayList<File> mBackgrounds = new ArrayList<>();
    private final ArrayList<Integer> mDefaultBackgrounds = new ArrayList<>();
    private int mBackgroundIndex = 0;
    private int mDefaultImagesIndex = 0;

    // Used to change backgrounds.
    private PeriodicTimer cycleBackgroundsTimer = null;

    /*
     * Handles the background cycling on a periodic timer.
     */
    private final JHTTimer.IOnTimer mOnTimer = this::cycleBackgroundImage;

    private final Context context;

    /*
     * This loads the images and starts the background manager.
     */
    public BackgroundManager(ImageView b1, ImageView b2)
    {
        mCurrentView = b1;
        mNextView = b2;
        mHandler = new Handler();
        loadDefaultBackgrounds();
        loadCustomImages();
        context = mCurrentView.getContext();
    }

    /*
     * This loads the images and starts the background manager.  This can get called when there is a system configuration update.
     */
    public void loadCustomImages()
    {
        new Thread(() -> {
            mBackgrounds.clear();
            stopTimer();
            if(DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_USE_CUSTOM_WELCOME_SCREEN_BOOL))
            {
                //display AM images if found, else display default wallpapers
                loadBackgrounds();
                if(mBackgrounds.size() > 1) {
                    startTimer();
                }
                else if (mBackgrounds.size() == 1) {
                    mHandler.post(() -> mCurrentView.setImageBitmap(decodeBackgroundImage(mBackgrounds.get(mBackgroundIndex))));
                }
            }
            if(mBackgrounds.isEmpty()){
                mHandler.post(() -> mCurrentView.setImageResource(R.drawable.welcome_background));
            }
        }).start();
    }

    public void enableDefaultWallpapers(boolean isEnabled) {
        if (mBackgrounds.isEmpty()) {
            stopTimer();
            if (isEnabled) {
                startTimer();
            } else {
                mDefaultImagesIndex = 0;
                mCurrentView.setImageResource(R.drawable.welcome_background);
            }
        }
    }

    /*
     * Used to start the timer when we want to cross-fade multiple images.
     */
    public void startTimer()
    {
        stopTimer();
        cycleBackgroundsTimer = new PeriodicTimer(0, DataMatrix.getInt(SettingKeys.AssetManagement.DATA_WELCOME_SCREEN_SLIDESHOW_SEC_INT) * 1000, mOnTimer, null);
        cycleBackgroundsTimer.start();
    }

    /*
     * Used to stop the timer when we leave the activity.
     */
    public void stopTimer()
    {
        if(cycleBackgroundsTimer != null) {
            cycleBackgroundsTimer.stop();
            cycleBackgroundsTimer = null;
        }
    }

    /*
     * This loads the backgrounds from /data/jht/settings/skin/theme/welcome.
     */
    private void loadBackgrounds()
    {
        @NonNls String[] imageExtensions =  new String[] {"jpg", "png", "gif","jpeg", "webp"};
        mBackgrounds = new ArrayList<>();

        File path = new File(context.getFilesDir(), "/settings/theme/welcome");
        ExecuteAsRoot executeAsRoot = new ExecuteAsRoot();
        executeAsRoot.addCommand("mkdir -p /data/data/com.jht.chimera/files/temp");
        executeAsRoot.addCommand("rm -R /data/data/com.jht.chimera/files/temp/*");
        executeAsRoot.addCommand("cp -rf " + path.getAbsolutePath() + "/* /data/data/com.jht.chimera/files/temp");
        executeAsRoot.execute();

        File tempPath = new File(context.getFilesDir(), "/temp");

        if(tempPath.exists()) {
            ExecuteAsRoot.chmodRecursive("777", tempPath.getAbsolutePath());
            File[] files = tempPath.listFiles();

            if(files != null && files.length != 0) {
                for(File file : files){
                    try {
                        int j = file.getName().lastIndexOf(".");
                        if (j > 0) {
                            String extension = file.getName().substring(j + 1);
                            for (String imageExtension : imageExtensions) {
                                if (imageExtension.equals(extension)) {
                                    mBackgrounds.add(file);
                                    break;
                                }
                            }
                        }
                    }catch (Exception e) {
                        Log.e(TAG, "EXCEPTION", e);
                    }
                }
            }
            if(mBackgrounds.size() > 1) {
                Collections.sort(mBackgrounds, (f1, f2) -> f1.getName().compareTo(f2.getName()));
            }
        }
    }

    private void cycleBackgroundImage(){
        mHandler.post(() -> {
            //Cycle through custom backgrounds if available
            if(!mBackgrounds.isEmpty()) {
                if (mBackgroundIndex >= mBackgrounds.size()) {
                    mBackgroundIndex = 0;
                }
                mNextView.setImageBitmap(decodeBackgroundImage(mBackgrounds.get(mBackgroundIndex)));
                mBackgroundIndex++;
            }
            //Cycle through default backgrounds if there are no custom backgrounds
            else{
                if(mDefaultImagesIndex >= mDefaultBackgrounds.size()){
                    mDefaultImagesIndex = 0;
                }
                mNextView.setImageResource(mDefaultBackgrounds.get(mDefaultImagesIndex));
                mDefaultImagesIndex++;
            }
            crossFadeImages();
        });
    }

    /*
     * Update the background while doing a cross fade.
     */
    private void crossFadeImages(){
        mCurrentView.animate().alpha(0.0f).setDuration(1000);
        mNextView.animate().alpha(1.0f).setDuration(1000);

        //Swap current and next views
        ImageView tempView = mNextView;
        mNextView = mCurrentView;
        mCurrentView = tempView;
    }

    private void loadDefaultBackgrounds(){
        mDefaultBackgrounds.add(R.drawable.screen_saver_commercial_v1);
        mDefaultBackgrounds.add(R.drawable.screen_saver_commercial_v2);
        mDefaultBackgrounds.add(R.drawable.screen_saver_commercial_v3);
        mDefaultBackgrounds.add(R.drawable.screen_saver_commercial_v4);
        mDefaultBackgrounds.add(R.drawable.screen_saver_commercial_v5);
        mDefaultBackgrounds.add(R.drawable.screen_saver_commercial_v6);
    }

    public static Bitmap decodeBackgroundImage(File file) {
        // Make sure the file exists.
        if(!file.exists()) {
            return null;
        }

        // Get the image dimensions.
        BitmapFactory.Options options;

        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;

        // Find the appropriate scale.
        options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.outWidth = width;
        options.outHeight = height;

        // Decode the file.
        return BitmapFactory.decodeFile(file.getPath(), options);
    }
}