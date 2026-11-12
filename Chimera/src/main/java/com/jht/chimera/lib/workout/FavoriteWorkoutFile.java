package com.jht.chimera.lib.workout;

import android.util.Log;

import com.google.gson.Gson;
import com.jht.chimera.lib.common.ObjectUtil;
import com.jht.chimera.lib.common.dapi.DapiUser;
import com.jht.chimera.service.dapi.UserProfileManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FavoriteWorkoutFile {

    final int MAX_FAVORITE_WORKOUT = 10;
    final String FILENAME = "favorite_workouts.json"; //NON-NLS
    private final static String TAG = "FavoriteWorkoutFile";

    List<WorkoutStartupParams> favoriteWorkoutList;

    public static FavoriteWorkoutFile getInstance() {
        return FavoriteWorkoutFile.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final FavoriteWorkoutFile INSTANCE = new FavoriteWorkoutFile();
    }

    private FavoriteWorkoutFile() {
    }

    public void addToFavorite(WorkoutStartupParams workout) {
        favoriteWorkoutList = load();

        // remove any derived class properties (do we need a workout.getClass()!=WorkoutStartupParams.class check? should this be in save instead? do some gson magic?)
        workout = new WorkoutStartupParams(workout);

        // don't do anything if it's already in the list
        //if (favoriteWorkoutList.contains(workout)) return;
        if (containsWorkout(workout)) return;

        favoriteWorkoutList.add(0, workout);
        while (favoriteWorkoutList.size() > MAX_FAVORITE_WORKOUT) {
            favoriteWorkoutList.remove(favoriteWorkoutList.size() - 1);
        }
        save(favoriteWorkoutList);
    }

    // normal "contains" will use strict equals, this will try to figure out with effective equality
    public boolean containsWorkout(WorkoutStartupParams workout) {
        if (favoriteWorkoutList != null) {
            for (WorkoutStartupParams p : favoriteWorkoutList) {
                if (sameEnough(p, workout)) return true;
            }
        }
        return false;
    }

    private boolean sameEnough(WorkoutStartupParams p1, WorkoutStartupParams p2) {
        //if (p.equals(workout)) return true;
        //if (p.equalsIgnoreEmptyString(workout)) return true;
        return (ObjectUtil.equals(p1.programTypeId, p2.programTypeId)
                && ObjectUtil.equalsIgnoreEmptyString(p1.appUuid, p2.appUuid)
                && ObjectUtil.equalsIgnoreEmptyString(p1.customWorkoutUuid, p2.customWorkoutUuid));
    }

    public void removeFromFavorite(WorkoutStartupParams workout) {
        if (workout == null) return; // don't bother if null
        favoriteWorkoutList = load();

        Iterator<WorkoutStartupParams> itr = favoriteWorkoutList.iterator();
        while (itr.hasNext())
        {
            WorkoutStartupParams current = itr.next();
            if (sameEnough(current, workout))
                itr.remove();
        }

        save(favoriteWorkoutList);
    }

    public void removeCustomWorkout(String customWorkoutUuid) {
        if (customWorkoutUuid == null) return; // don't bother if null
        favoriteWorkoutList = load();

        Iterator<WorkoutStartupParams> itr = favoriteWorkoutList.iterator();
        while (itr.hasNext())
        {
            WorkoutStartupParams workoutStartupParams = itr.next();
            if (workoutStartupParams.customWorkoutUuid != null && workoutStartupParams.customWorkoutUuid.equals(customWorkoutUuid))
                itr.remove();
        }

        save(favoriteWorkoutList);
    }

    public List<WorkoutStartupParams> getList() {
        // guest does not have a list
        if (DapiUser.isGuestUser()) return new ArrayList<>();

        favoriteWorkoutList = load();
        return favoriteWorkoutList;
    }

    private List<WorkoutStartupParams> load() {

        File file = new File(UserProfileManager.getInstance().getUserFolder(), FILENAME);
        try {
            if (file.exists()) {
                Gson gson = new Gson();
                WorkoutStartupParams[] array = gson.fromJson(new BufferedReader(new InputStreamReader(new FileInputStream(file))), WorkoutStartupParams[].class);
                List<WorkoutStartupParams> list = new ArrayList<>();
                for (WorkoutStartupParams workout : array) {
                    // Remove any duplicates...
                    if (!list.contains(workout)) list.add(workout);
                }
                return list;
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to load favorites", e);
        }
        return new ArrayList<>();
    }

    private void save(List<WorkoutStartupParams> list) {
        File file = new File(UserProfileManager.getInstance().getUserFolder(), FILENAME);

        try {
            Gson gson = new Gson();
            String json = gson.toJson(list.toArray()); // convert list to array since we are reading an array
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(json);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        } catch (Exception e) {
            Log.d(TAG, "Failed to save favorites", e);
        }
    }
}
