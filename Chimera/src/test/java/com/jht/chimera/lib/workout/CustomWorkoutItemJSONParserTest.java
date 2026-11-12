package com.jht.chimera.lib.workout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItem;
import com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom.CustomWorkoutItemJSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomWorkoutItemJSONParserTest
{
    protected void testEncodeDecode(List<CustomWorkoutItem> customWorkouts) throws JSONException {
        JSONObject jso = CustomWorkoutItemJSONParser.toJsonObject(customWorkouts, true);
        String json = jso.toString();
        assertNotEquals(0, jso.length());

        List<CustomWorkoutItem> decoded = CustomWorkoutItemJSONParser.fromJson(json, true);
        assertEquals(customWorkouts, decoded);
    }

    @Test
    public void testToFromJson() throws JSONException {
        List<CustomWorkoutItem> customWorkouts = new ArrayList<>();
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("foo", Arrays.asList(1f, 2f, 3f, 4f));
        workoutData.put("bar", Arrays.asList(5f, 6f, 7f, 8f));
        customWorkouts.add(new CustomWorkoutItem("workout one", UUID.randomUUID(), workoutData, false));
        customWorkouts.add(new CustomWorkoutItem("workout too", UUID.randomUUID(), workoutData, true));
        testEncodeDecode(customWorkouts);
    }

    private List<CustomWorkoutItem> encodeThenDecode(Map<String,List<Float>> workoutData, boolean savedMetric, boolean displayMetric) throws JSONException {
        List<CustomWorkoutItem> customWorkouts = new ArrayList<>();
        customWorkouts.add(new CustomWorkoutItem("workout", UUID.randomUUID(), workoutData, true));
        String json = CustomWorkoutItemJSONParser.toJson(customWorkouts, savedMetric);
        return CustomWorkoutItemJSONParser.fromJson(json, displayMetric);
    }

    @Test
    public void testSpeedsConvertedFromImperialToMetric() throws JSONException {
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("START_SPEED", Arrays.asList(0.5f, 2.0f, 12.0f));
        workoutData.put("START_INCLINE", Arrays.asList(1f, 2f, 3f));
        List<CustomWorkoutItem> decoded = encodeThenDecode(workoutData, false, true);
        assertEquals(Arrays.asList(0.8f, 3.2f, 19.3f), decoded.get(0).intervalData.get("START_SPEED"));
        assertEquals(Arrays.asList(1f, 2f, 3f), decoded.get(0).intervalData.get("START_INCLINE"));
    }

    @Test
    public void testSpeedsConvertedFromMetricToImperial() throws JSONException {
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("START_SPEED", Arrays.asList(0.8f, 3.2f, 16.0f));
        List<CustomWorkoutItem> decoded = encodeThenDecode(workoutData, true, false);
        assertEquals(Arrays.asList(0.5f, 2.0f, 9.9f), decoded.get(0).intervalData.get("START_SPEED"));
    }

    @Test
    public void testSpeedsUnchangedWhenUnitsMatch() throws JSONException {
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("START_SPEED", Arrays.asList(0.5f, 2.0f));
        List<CustomWorkoutItem> decoded = encodeThenDecode(workoutData, false, false);
        assertEquals(Arrays.asList(0.5f, 2.0f), decoded.get(0).intervalData.get("START_SPEED"));
    }

    @Test
    public void testUnitSystemWrittenToJson() throws JSONException {
        List<CustomWorkoutItem> customWorkouts = new ArrayList<>();
        assertTrue(CustomWorkoutItemJSONParser.toJsonObject(customWorkouts, true).getBoolean("isMetric"));
        assertFalse(CustomWorkoutItemJSONParser.toJsonObject(customWorkouts, false).getBoolean("isMetric"));
    }

    @Test
    public void testOldStyleJsonSpeedsConverted() throws JSONException {
        String json = "{\"isMetric\":false,\"customWorkouts\":[{\"title\":\"legacy\",\"categoryType\":0," +
                "\"customWorkoutData\":[{\"identifier\":\"START_SPEED\",\"intervalData\":[2.0]}," +
                "{\"identifier\":\"START_INCLINE\",\"intervalData\":[1.5]}]}]}";
        List<CustomWorkoutItem> decoded = CustomWorkoutItemJSONParser.fromJson(json, true);
        assertEquals(Arrays.asList(3.2f), decoded.get(0).intervalData.get("START_SPEED"));
        assertEquals(Arrays.asList(1.5f), decoded.get(0).intervalData.get("START_INCLINE"));
    }

    @Test
    public void testConversionRoundTripIsStable() throws JSONException {
        // imperial -> metric -> imperial must return the original values, no rounding drift
        List<Float> imperialSpeeds = Arrays.asList(0.5f, 2.0f, 3.5f, 7.3f, 12.0f);
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("START_SPEED", imperialSpeeds);
        List<CustomWorkoutItem> asMetric = encodeThenDecode(workoutData, false, true);
        String json = CustomWorkoutItemJSONParser.toJson(asMetric, true);
        List<CustomWorkoutItem> backToImperial = CustomWorkoutItemJSONParser.fromJson(json, false);
        assertEquals(imperialSpeeds, backToImperial.get(0).intervalData.get("START_SPEED"));
    }

    @Test
    public void testAllWorkoutsInListConverted() throws JSONException {
        List<CustomWorkoutItem> customWorkouts = new ArrayList<>();
        Map<String,List<Float>> workoutData1 = new HashMap<>();
        workoutData1.put("START_SPEED", Arrays.asList(2.0f));
        customWorkouts.add(new CustomWorkoutItem("one", UUID.randomUUID(), workoutData1, true));
        Map<String,List<Float>> workoutData2 = new HashMap<>();
        workoutData2.put("START_SPEED", Arrays.asList(4.0f));
        customWorkouts.add(new CustomWorkoutItem("two", UUID.randomUUID(), workoutData2, true));
        String json = CustomWorkoutItemJSONParser.toJson(customWorkouts, false);
        List<CustomWorkoutItem> decoded = CustomWorkoutItemJSONParser.fromJson(json, true);
        assertEquals(Arrays.asList(3.2f), decoded.get(0).intervalData.get("START_SPEED"));
        assertEquals(Arrays.asList(6.4f), decoded.get(1).intervalData.get("START_SPEED"));
    }

    @Test
    public void testWorkoutWithoutSpeedUnaffectedByUnitMismatch() throws JSONException {
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("START_RESISTANCE", Arrays.asList(5f, 10f));
        workoutData.put("TIME_GOAL_START", Arrays.asList(5f, 5f));
        List<CustomWorkoutItem> decoded = encodeThenDecode(workoutData, true, false);
        assertEquals(Arrays.asList(5f, 10f), decoded.get(0).intervalData.get("START_RESISTANCE"));
        assertEquals(Arrays.asList(5f, 5f), decoded.get(0).intervalData.get("TIME_GOAL_START"));
    }

    @Test
    public void testSpeedsUnchangedWhenJsonHasNoUnits() throws JSONException {
        // JSON saved before the isMetric field existed is assumed to be in the display units
        Map<String,List<Float>> workoutData = new HashMap<>();
        workoutData.put("START_SPEED", Arrays.asList(2.0f));
        List<CustomWorkoutItem> customWorkouts = new ArrayList<>();
        customWorkouts.add(new CustomWorkoutItem("workout", UUID.randomUUID(), workoutData, true));
        JSONObject jso = CustomWorkoutItemJSONParser.toJsonObject(customWorkouts, false);
        jso.remove("isMetric");
        List<CustomWorkoutItem> decoded = CustomWorkoutItemJSONParser.fromJson(jso.toString(), true);
        assertEquals(Arrays.asList(2.0f), decoded.get(0).intervalData.get("START_SPEED"));
    }

    String getResourceAsString(String name) throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream(name)) {
            if (is == null) throw new NullPointerException("Couldn't find resource: "+name);
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[65536];
            int read = 0;
            while ((read = isr.read(buffer)) > 0) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        }
    }

    @Test
    public void testLoadOldStyleTreadmillCustomWorkoutJso() throws IOException, JSONException {
        JSONObject fileJso = new JSONObject(getResourceAsString("custom-workout.treadmill.style1.json"));
        JSONArray workoutsJso = fileJso.getJSONArray("customWorkouts");
        JSONObject customWorkoutJso = workoutsJso.getJSONObject(0);
        JSONArray cwdJso = customWorkoutJso.getJSONArray("customWorkoutData");
        assertEquals(3, cwdJso.length());
    }

    protected void checkLoadedWorkouts(List<CustomWorkoutItem> customWorkoutItems) {
        assertEquals(1, customWorkoutItems.size());
        CustomWorkoutItem cwi = customWorkoutItems.get(0);
        assertEquals("", cwi.title);
        assertEquals(3, cwi.intervalData.size());
        assertTrue(cwi.intervalData.containsKey("START_INCLINE"));
        assertTrue(cwi.intervalData.containsKey("START_SPEED"));
        assertTrue(cwi.intervalData.containsKey("TIME_GOAL_START"));
        List<Float> inclineData = cwi.intervalData.get("START_INCLINE");
        assertEquals(2, inclineData.size());
        assertEquals(Float.valueOf(2.5f), inclineData.get(0));
        assertEquals(Float.valueOf(3.0f), inclineData.get(1));
    }

    protected void checkWorkoutsInFile(String filename) throws IOException, JSONException {
        checkLoadedWorkouts(CustomWorkoutItemJSONParser.fromJson(getResourceAsString(filename), true));
    }

    @Test
    public void testEmptyJsonMeansNoWorkouts() throws JSONException {
        assertEquals(Collections.emptyList(), CustomWorkoutItemJSONParser.fromJson("", true));
    }

    @Test
    public void testNullJsonMeansNoWorkouts() throws JSONException {
        assertEquals(Collections.emptyList(), CustomWorkoutItemJSONParser.fromJson(null, true));
    }

    @Test
    public void testLoadStyle1TreadmillCustomWorkout() throws IOException, JSONException {
        checkWorkoutsInFile("custom-workout.treadmill.style1.json");
    }

    @Test
    public void testLoadStyle2TreadmillCustomWorkout() throws IOException, JSONException {
        checkWorkoutsInFile("custom-workout.treadmill.style2.json");
    }
}
