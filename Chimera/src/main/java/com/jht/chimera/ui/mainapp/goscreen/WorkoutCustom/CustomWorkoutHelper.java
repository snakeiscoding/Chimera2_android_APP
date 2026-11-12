package com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Contains logic to convert a custom workout to a usable format, either XML or a script.
 * <p>
 * This will only work with certain parameter names and may require refactoring of the artifact
 * to work in a general fashion.
 */
public class CustomWorkoutHelper {
    private static final String TAG = "CustomWorkoutHelper";

    private static final double MI_TO_KM_FACTOR = 1.609344;

    /**
     * Gets a workout script string for the custom workout.
     *
     * @param customWorkout     The custom workout data
     * @param showMetricUnits Whether the parameter values stored in CustomWorkoutItem should be interpreted as metric units
     * @return The workout script for the custom workout data
     */
    public static String getWorkoutScript(CustomWorkoutItem customWorkout, boolean showMetricUnits, boolean includeAutomaticCooldown) {
        StringBuilder scriptString = new StringBuilder();
        scriptString.append(getSettings());
        scriptString.append(getInputsAndOutputs());

        List<Float> intervalEndTimes = new ArrayList<>();
        List<Float> speedValues = new ArrayList<>();
        List<Float> inclineValues = new ArrayList<>();
        List<Float> resistanceValues = new ArrayList<>();
        getArrayValues(customWorkout, showMetricUnits, intervalEndTimes, speedValues, inclineValues, resistanceValues);

        scriptString.append(getArrayLines(intervalEndTimes, speedValues, inclineValues, resistanceValues));
        scriptString.append(getStage(customWorkout, intervalEndTimes, speedValues, inclineValues, resistanceValues, includeAutomaticCooldown));
        scriptString.append(getCooldown());

        return scriptString.toString();
    }

    public static String getWorkoutScript(CustomWorkoutItem customWorkout, boolean showMetricUnits) {
        return getWorkoutScript(customWorkout, showMetricUnits, customWorkout.hasCooldown);
    }

    /**
     * Gets the settings block for the custom workout
     *
     * @return the settings block of the workout
     */
    @SuppressWarnings("HardCodedStringLiteral")
    private static String getSettings() {
        // NOTE: WorkoutIds.CUSTOM_WORKOUT = 90;
        return "settings\n" +
                "  workout_id = 90\n" +
                "  allow_skip_warmup = 1\n" +
                "  graph_future = 1\n" +
                "  allow_skip_workout = 1\n" +
                "  allow_user_goal_changes = 0\n";
    }

    /**
     * Gets the inputs and exports for the custom workout script
     *
     * @return The string for inputs and exports
     */
    @SuppressWarnings("HardCodedStringLiteral")
    private static String getInputsAndOutputs() {
        return "input GOAL\n" +
                "input COOLDOWN_TIME_START\n" +
                "input SPEED_IN\n" +
                "export SPEED\n" +
                "export INCLINE\n" +
                "export RESISTANCE\n" +
                "export GOAL\n" +
                "export GOAL_TYPE\n" +
                "export INTERVAL_GOAL_REMAINING\n" +
                "export INTERVAL_NUMBER\n" +
                "export PREVENT_USER_GOAL_CHANGES\n";
    }

    protected static void addAllMultiplied(List<Float> dest, List<Float> source, float multiplier) {
        for (Float f : source) dest.add(f * multiplier);
    }

    /**
     * Gets the arrays of data from the custom workout item
     *
     * @param customWorkoutItem The item to get data from
     * @param showMetricUnits Whether this is displaying metric units or needs to be converted
     * @param endTimes          End times for each interval, relative to beginning of workout,
     *                          in seconds, to be populated by this method
     * @param speedValues       The values for speed, to be populated by this method
     * @param inclineValues     The values for incline, to be populated by this method
     * @param resistanceValues  The values for resistance, to be populated by this method
     */
    private static void getArrayValues(CustomWorkoutItem customWorkoutItem,
                                       boolean showMetricUnits,
                                       List<Float> endTimes, List<Float> speedValues,
                                       List<Float> inclineValues, List<Float> resistanceValues) {
        for (Map.Entry<String,List<Float>> entry : customWorkoutItem.intervalData.entrySet()) {
            if (entry.getKey().equals("TIME_GOAL_START")) {
                float second = 0;
                for (Float intervalLengthMinutes : entry.getValue()) {
                    second += intervalLengthMinutes.floatValue() * 60;
                    endTimes.add(Float.valueOf(Math.round(second)));
                }
            } else if (entry.getKey().equals("START_SPEED")) {
                addAllMultiplied(speedValues, entry.getValue(), showMetricUnits ? 1 : (float) MI_TO_KM_FACTOR);
            } else if (entry.getKey().equals("START_INCLINE")) {
                inclineValues.addAll(entry.getValue());
            } else if (entry.getKey().equals("START_RESISTANCE")) {
                resistanceValues.addAll(entry.getValue());
            } else {
                Log.e(TAG, "Invalid workout parameter in custom script workout: " + entry.getKey());
            }
        }
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static String getArrayLines(List<Float> endTimes, List<Float> speedValues,
                                        List<Float> inclineValues, List<Float> resistanceValues) {
        StringBuilder arrayLines = new StringBuilder();
        if (endTimes.size() > 0)
            arrayLines.append(getArrayLine("intervalEndTimes", endTimes)).append("\n");
        if (speedValues.size() > 0)
            arrayLines.append(getArrayLine("SPEED_ARRAY", speedValues)).append("\n");
        if (inclineValues.size() > 0)
            arrayLines.append(getArrayLine("INCLINE_ARRAY", inclineValues)).append("\n");
        if (resistanceValues.size() > 0)
            arrayLines.append(getArrayLine("RESISTANCE_ARRAY", resistanceValues)).append("\n");

        return arrayLines.toString();
    }

    private static String getArrayLine(String variable, List<Float> values) {
        StringBuilder arrayLine = new StringBuilder();
        arrayLine.append(variable).append(" = [");
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            arrayLine.append(value);

            if (i + 1 < values.size()) arrayLine.append(", ");
        }

        arrayLine.append("]");
        return arrayLine.toString();
    }

    static <T> T last(List<T> things, T defaultValue) {
        return things.size() > 0 ? things.get(things.size() - 1) : defaultValue;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static String getStage(CustomWorkoutItem customWorkout, List<Float> intervalEndTimes, List<Float> speedValues,
                                   List<Float> inclineValues, List<Float> resistanceValues,
                                   boolean includeAutomaticCooldown) {
        StringBuilder stageLines = new StringBuilder();

        stageLines.append("stage workout\n");

        float lengthSeconds = Math.round(last(intervalEndTimes, Float.valueOf(0)));

        stageLines.append("  PREVENT_USER_GOAL_CHANGES = "+(customWorkout.allowGoalChanges?"0":"1")+"\n");
        stageLines.append("  AUTOMATIC_COOLDOWN = "+(includeAutomaticCooldown?"1":"0")+"\n");
        stageLines.append("  GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n");
        stageLines.append("  TIME_GOAL_START = ").append(lengthSeconds).append(" * 1000\n");
        stageLines.append("  GOAL = ").append(lengthSeconds).append(" * 1000\n");
        stageLines.append("  INTERVAL_NUMBER = 0\n");
        stageLines.append("  totalIntervals = ").append(intervalEndTimes.size()).append("\n");
        stageLines.append("  currentIntervalEndTime = 100 // Arbitrary initial value\n");
        stageLines.append("  when STAGE_TIME_MS >= currentIntervalEndTime * 1000 * GOAL / TIME_GOAL_START\n");
        stageLines.append("    INTERVAL_NUMBER = INTERVAL_NUMBER + 1\n");
        stageLines.append("    if ((interval == " + (intervalEndTimes.size() - 1) + ") && (AUTOMATIC_COOLDOWN == 0))\n");
        stageLines.append("      end\n");
        stageLines.append("    else\n");
        stageLines.append("      interval = interval + 1\n");
        stageLines.append("  level 1\n");
        stageLines.append("    setIntervals currentIntervalEndTime = intervalEndTimes\n");
        if (speedValues.size() > 0) stageLines.append("    setIntervals SPEED = SPEED_ARRAY\n");
        if (inclineValues.size() > 0)
            stageLines.append("    setIntervals INCLINE = INCLINE_ARRAY\n");
        if (resistanceValues.size() > 0)
            stageLines.append("    setIntervals RESISTANCE = RESISTANCE_ARRAY\n");

        Log.d (TAG, "Custom Script= \n" + stageLines.toString());

        return stageLines.toString();
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private static String getCooldown() {
        return "stage \"cooldown\"\n" +
                "    interval\n" +
                "        PREVENT_USER_GOAL_CHANGES = 0\n" +
                "        SPEED = min(0.6 * SPEED_IN, 6.43738)\n" +
                "        INCLINE = 0\n" +
                "        RESISTANCE = 1\n" +
                "        GOAL_TYPE = GOAL_TYPE_TIME_STAGE_CONSTANT\n" +
                "        GOAL = COOLDOWN_TIME_START\n" +
                "        when STAGE_TIME_MS >= GOAL\n" +
                "            end";
    }


    /**
     * Gets an XML workout from the custom workout
     *
     * @param customWorkoutItem The custom workout data
     * @param showImperialUnits Whether metric or imperial was displayed
     * @return The XML workout from the workout data
     */
    @SuppressWarnings("HardCodedStringLiteral")
    public static String getWorkoutXml(CustomWorkoutItem customWorkoutItem, boolean showImperialUnits) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<workout name=\"custom\" id=\"0\" ascent_workout=\"true\" bike_workout=\"true\" climbmill_workout=\"true\" ep_workout=\"true\" stepper_workout=\"true\" treadmill_workout=\"true\">");

        // add the workout
        xml.append("<state type=\"workout\" monitor=\"intervals\" >");
        int intervalCount = 0;
        for (Map.Entry<String,List<Float>> entry : customWorkoutItem.intervalData.entrySet()) {
            intervalCount = Math.max(intervalCount, entry.getValue().size());
        }
        for (int i = 0; i < intervalCount; i++) {
            xml.append("<interval ");
            for (Map.Entry<String,List<Float>> entry : customWorkoutItem.intervalData.entrySet()) {
                final String parameterId = entry.getKey();
                final List<Float> intervalData = entry.getValue();
                if (i >= intervalData.size()) continue;
                final Float value = intervalData.get(i);

                //Change time to seconds
                if (parameterId.equals("time_sec")) {
                    xml.append(String.format(Locale.ENGLISH, " time_sec=\"%2f\"", value * 60));
                }
                //Change speed from mph if necessary
                else if (parameterId.equals("speed_kph") && showImperialUnits) {
                    xml.append(String.format(Locale.ENGLISH, " speed_kph=\"%2f\"", value * 1.609344));
                } else {
                    xml.append(String.format(Locale.ENGLISH, " " + parameterId + "=\"%2f\"", value));
                }
            }
            xml.append(" />");
        }
        xml.append("</state>");
        xml.append("</workout>");
        return xml.toString();
    }
}
