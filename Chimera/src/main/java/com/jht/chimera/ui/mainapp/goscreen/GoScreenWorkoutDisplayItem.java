package com.jht.chimera.ui.mainapp.goscreen;

import android.content.Context;
import android.net.Uri;

import com.jht.data.WorkoutItem;


public class GoScreenWorkoutDisplayItem {

    public enum WorkoutStateTypes {
        ENABLED,
        DISABLED
    }

    public final static int WORKOUT_TYPE = 0;
    public final static int CATEGORY_TITLE_TYPE = 1;
    public final static int CATEGORY_SUBTITLE_TITLE = 2;
    public final static int LOADING_TYPE = 3;

    public String title = "";
    public String subTitle = "";
    public String description = "";
    public String subDescription = "";
    public Uri backgroundPath;
    public Uri thumbnailPath;
    public String identifier = "";
    public int categoryType;
    public int enabledState;

    public GoScreenWorkoutDisplayItem(Context context, WorkoutItem workoutItem){
        this.title = context.getString(workoutItem.getTitleId());
        this.description = context.getString(workoutItem.getDescriptionId());
        this.backgroundPath = workoutItem.getBackground().get(0);
        this.thumbnailPath = workoutItem.getThumbnail();
        this.identifier = String.valueOf(workoutItem.getWorkoutId());
        categoryType = WORKOUT_TYPE;
    }

    public GoScreenWorkoutDisplayItem(String title, String subTitle, String description, String subDescription, Uri backgroundPath, Uri thumbnailPath, String identifier){
        this.title = title;
        this.subTitle = subTitle;
        this.description = description;
        this.subDescription = subDescription;
        this.backgroundPath = backgroundPath;
        this.thumbnailPath = thumbnailPath;
        this.identifier = identifier;
        categoryType = WORKOUT_TYPE;
    }

    public GoScreenWorkoutDisplayItem(String title, int category){
        this.title = title;
        this.categoryType = category;
    }

    @Override
    public String toString() {
        return "GoScreenWorkoutDisplayItem{" +
                "title='" + title + '\'' +
                ", subTitle='" + subTitle + '\'' +
                ", description='" + description + '\'' +
                ", subDescription='" + subDescription + '\'' +
                ", backgroundPath=" + backgroundPath +
                ", iconPath=" + thumbnailPath +
                ", identifier='" + identifier + '\'' +
                ", categoryType=" + categoryType +
                ", enabledState=" + enabledState +
                '}';
    }
}
