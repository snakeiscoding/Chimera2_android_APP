package com.jht.chimera.lib.workout;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.dapi.DapiUser;

public class FavoriteIcon {
    private final static String TAG = "FavoriteIcon";

    public static void populateIcon(View view, WorkoutStartupParams workout) {
        try {
            // guest user cannot save favorites
            if (DapiUser.isGuestUser()) return;

            View icon = (view == null ? null : view.findViewById(R.id.favorite));
            if (icon == null) return;

            CheckBox checkBox = (CheckBox) icon;

            checkBox.setVisibility(View.VISIBLE);
            ((ViewGroup)checkBox.getParent()).setVisibility(View.VISIBLE);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setButtonDrawable(R.drawable.favorite_drawable);
            // if used on fragment, onViewStateRestored may trigger unwanted check changed based on last state, so queue this with UI
            checkBox.post(()->{
                if (FavoriteWorkoutFile.getInstance().containsWorkout(workout)) {
                    checkBox.setChecked(true);
                } else {
                    checkBox.setChecked(false);
                }

                checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                    if (isChecked) FavoriteWorkoutFile.getInstance().addToFavorite(workout);
                    else FavoriteWorkoutFile.getInstance().removeFromFavorite(workout);
                });
            });

        } catch (Exception e) {
            Log.d(TAG, "populateIcon threw", e);
        }
    }

}
