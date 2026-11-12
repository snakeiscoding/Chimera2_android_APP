package com.jht.chimera.ui.ifit;

import com.jht.androidcommonalgorithms.resources.ResourceHelper;

import org.json.JSONObject;

import java.io.File;

class IFitTestSettings {
    private static final File settingsFile = new File("/sdcard/test/ifitTestSettings.json");

    private int getStartedDelaySeconds = 40;
    private int guestWorkoutSelectionSeconds = 20;
    private int workoutSelectionDelaySeconds = 20;
    private int startWorkoutDelaySeconds = 10;
    private int acceptDelaySeconds = 10;
    private int endWarmupTimeSeconds = 30;
    private int workoutTimeSeconds = 30;

    IFitTestSettings() {
        loadSettings();
    }

    private void loadSettings() {
        JSONObject settingsObject = ResourceHelper.loadJSONFromFile(settingsFile);
        if(settingsObject != null) {
            workoutTimeSeconds = settingsObject.optInt("workoutTimeSeconds", workoutTimeSeconds);
            endWarmupTimeSeconds = settingsObject.optInt("endWarmupTimeSeconds", endWarmupTimeSeconds);
            workoutSelectionDelaySeconds = settingsObject.optInt("workoutSelectionDelaySeconds", workoutSelectionDelaySeconds);
            startWorkoutDelaySeconds = settingsObject.optInt("startWorkoutDelaySeconds", startWorkoutDelaySeconds);
            acceptDelaySeconds = settingsObject.optInt("acceptDelaySeconds", acceptDelaySeconds);
            guestWorkoutSelectionSeconds = settingsObject.optInt("guestWorkoutSelectionSeconds", guestWorkoutSelectionSeconds);
            getStartedDelaySeconds = settingsObject.optInt("getStartedDelaySeconds", getStartedDelaySeconds);
        }
    }

    int workoutTimeSeconds() { return workoutTimeSeconds; }
    int endWarmupTimeSeconds() { return endWarmupTimeSeconds; }
    int guestWorkoutSelection() { return guestWorkoutSelectionSeconds; }
    int workoutSelectionDelaySeconds() { return workoutSelectionDelaySeconds; }
    int startWorkoutDelaySeconds() { return startWorkoutDelaySeconds; }
    int getStartedDelaySeconds() { return getStartedDelaySeconds; }
    int acceptDelaySeconds() { return acceptDelaySeconds; }

    IFitTestSettings(int getStartedDelaySeconds, int guestWorkoutSelectionSeconds, int workoutSelectionDelaySeconds, int startWorkoutDelaySeconds, int acceptDelaySeconds, int endWarmupTimeSeconds, int workoutTimeSeconds) {
        this.workoutSelectionDelaySeconds = workoutSelectionDelaySeconds;
        this.startWorkoutDelaySeconds = startWorkoutDelaySeconds;
        this.acceptDelaySeconds = acceptDelaySeconds;
        this.endWarmupTimeSeconds = endWarmupTimeSeconds;
        this.workoutTimeSeconds = workoutTimeSeconds;
        this.guestWorkoutSelectionSeconds = guestWorkoutSelectionSeconds;
        this.getStartedDelaySeconds = getStartedDelaySeconds;
    }
}
