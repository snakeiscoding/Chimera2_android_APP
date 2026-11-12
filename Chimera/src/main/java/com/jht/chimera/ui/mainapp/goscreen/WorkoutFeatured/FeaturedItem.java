package com.jht.chimera.ui.mainapp.goscreen.WorkoutFeatured;

import android.net.Uri;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.jht.chimera.ui.mainapp.goscreen.GoScreenWorkoutDisplayItem;

import java.util.ArrayList;
import java.util.List;

public class FeaturedItem {

    public interface ISetupApp {
        void launchSetupApp(View view);
    }

    public final static int WORKOUT = 0;
    public final static int WORKOUT_VA = 1;
    public final static int WORKOUT_SPRINT8 = 2;
    public final static int WORKOUT_LANDMARKS = 3;
    public final static int WORKOUT_IFIT = 4;
    public final static int WORKOUT_TARGET_TRAINING = 5;
    public final static int WORKOUT_CUSTOM_HIIT = 6;


    public final static int DISPLAY_GRID_TYPE = 0;
    public final static int DISPLAY_MAIN_FEATURE_TYPE = 1;
    public final static int DISPLAY_SUB_TITLE_TYPE = 2;

    public String title;
    public String subtitle;
    public String subtitle2;
    public List<Uri> backgroundPath;
    public Uri iconPath;
    public Uri thumbnailPath;
    public Uri videoPath;
    public String description;
    public int displayType = FeaturedItem.DISPLAY_GRID_TYPE;
    public int workoutType;
    public String identifier;
    public int workoutId;
    public ISetupApp setupApp = null;

    //Optionals
    public Fragment optionalFragment;
    public GoScreenWorkoutDisplayItem workoutItem;
    public boolean enabled = true;

    //Display only
    public boolean startOfRow = false;
    public String startButtonText;

    public FeaturedItem(int workoutId, String title, String subtitle, String subtitle2, Uri backgroundPath, Uri iconPath,
                        Uri thumbnailPath, Uri videoPath, String identifier, String description){
        this.workoutId = workoutId;
        this.title = title;
        this.subtitle = subtitle;
        this.subtitle2 = subtitle2;
        this.backgroundPath = new ArrayList <Uri>();
        this.backgroundPath.add (backgroundPath);
        this.iconPath = iconPath;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.description = description;
        this.identifier = identifier;
        this.workoutType = WORKOUT;
    }

    public FeaturedItem(int workoutId, String title, String subtitle, String subtitle2, List<Uri> backgroundPaths, Uri iconPath,
                        Uri thumbnailPath, Uri videoPath, String identifier, String description){
        this.workoutId = workoutId;
        this.title = title;
        this.subtitle = subtitle;
        this.subtitle2 = subtitle2;
        this.backgroundPath = backgroundPaths;
        this.iconPath = iconPath;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.description = description;
        this.identifier = identifier;
        this.workoutType = WORKOUT;
    }

    public Uri getSafeBackgroundItem (int position) {
        if (position < backgroundPath.size()) {
            return backgroundPath.get(position);
        }
        return backgroundPath.get(0);
    }

    public FeaturedItem(String title, int displayType){
        this.title = title;
        this.displayType = displayType;
    }
}
