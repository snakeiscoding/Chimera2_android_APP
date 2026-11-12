package com.jht.chimera.ui.mainapp.goscreen.WorkoutCustom;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.jht.chimera.lib.common.conversionlib.ConversionHelper;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.enums.ConsoleWorkoutVariables;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts a custom workout object to or from JSON to save.
 * <p>
 * Values are saved in the current display units, tagged with "isMetric" so speeds can be
 * converted on load if the unit preference has changed. Untagged (older) JSON is left as-is.
 */
public class CustomWorkoutItemJSONParser {
    private static final String IS_METRIC_KEY = "isMetric"; //NON-NLS

    public static JSONObject toJsonObject(List<CustomWorkoutItem> customWorkouts, boolean isMetric) throws JSONException {
        JSONObject object = new JSONObject();
        object.put(IS_METRIC_KEY, isMetric);
        JSONArray workoutArray = new JSONArray();

        for (CustomWorkoutItem item : customWorkouts) {
            if (item.categoryType != 0) {
                continue;
            }
            JSONObject itemObject = new JSONObject();
            workoutArray.put(itemObject);
            itemObject.put("uuid", item.uuid);
            itemObject.put("title", item.title);
            itemObject.put("subTitle", item.subTitle);
            itemObject.put("description", item.description);
            itemObject.put("subDescription", item.subDescription);
            if (item.backgroundPath == null) {
                itemObject.put("backgroundPath", "");
            } else {
                itemObject.put("backgroundPath", item.backgroundPath.toString());
            }
            if (item.thumbnailPath == null) {
                itemObject.put("iconPath", "");
            } else {
                itemObject.put("iconPath", item.thumbnailPath.toString());
            }
            itemObject.put("identifier", item.identifier);
            itemObject.put("categoryType", item.categoryType);
            JSONObject intervalDataJso = new JSONObject();
            if (item.intervalData != null) {
                for (Map.Entry<String,List<Float>> entry : item.intervalData.entrySet()) {
                    JSONArray itemWorkoutDataItemIntervalData = new JSONArray();
                    for (Float f : entry.getValue()) {
                        itemWorkoutDataItemIntervalData.put(f.doubleValue());
                    }
                    intervalDataJso.put(entry.getKey(), itemWorkoutDataItemIntervalData);
                }
            }
            itemObject.put("intervalData", intervalDataJso);
            itemObject.put("hasCooldown", item.hasCooldown);
        }
        object.put("customWorkouts", workoutArray);

        return object;
    }

    public static String toJson(List<CustomWorkoutItem> customWorkouts, boolean isMetric) {
        try {
            return toJsonObject(customWorkouts, isMetric).toString();
        } catch (JSONException e) {
            // This should never happen unless toJsonObject is buggy.
            throw new RuntimeException(e);
        }
    }

    protected static List<Float> parseIntervalData(JSONArray jsArr) {
        List<Float> intervalData = new ArrayList<>();
        for (int k = 0; k < jsArr.length(); k++) {
            intervalData.add((float) jsArr.optDouble(k));
        }
        return intervalData;
    }

    /** Converts speeds between mph and kph, rounded to the 0.1 shown/stepped in the UI */
    private static List<Float> convertSpeeds(List<Float> speeds, boolean storedMetric) {
        List<Float> converted = new ArrayList<>(speeds.size());
        for (Float speed : speeds) {
            float value = storedMetric ? SpeedConvertUtil.convertKphToMph(speed) : SpeedConvertUtil.convertMphToKph(speed);
            converted.add(ConversionHelper.round(value, 1));
        }
        return converted;
    }

    public static List<CustomWorkoutItem> fromJsonObject(JSONObject object, boolean displayMetric) throws JSONException {
        List<CustomWorkoutItem> customWorkoutItems = new ArrayList<>();
        boolean shouldConvertSpeeds = object.has(IS_METRIC_KEY) && object.getBoolean(IS_METRIC_KEY) != displayMetric;

        JSONArray customItems = object.getJSONArray("customWorkouts");
        for (int i = 0; i < customItems.length(); i++) {
            JSONObject itemObject = customItems.getJSONObject(i);
            if (itemObject.optInt("categoryType", 1) != 0) {
                continue;
            }
            UUID uuid = itemObject.has("uuid") ?
                UUID.fromString(itemObject.getString("uuid")) :
                UUID.randomUUID();
            boolean hasCooldown = itemObject.optBoolean("hasCooldown", true);
            CustomWorkoutItem item = new CustomWorkoutItem(itemObject.optString("title"), uuid, new HashMap<>(), hasCooldown);
            item.subTitle = itemObject.optString("subTitle");
            item.description = itemObject.optString("description");
            item.subDescription = itemObject.optString("subDescription");
            String path = itemObject.optString("backgroundPath");
            if (path == null || path.equals("")) {
                item.backgroundPath = null;
            } else {
                item.backgroundPath = Uri.parse(path);
            }
            path = itemObject.optString("iconPath");
            if (path == null || path.equals("")) {
                item.thumbnailPath = null;
            } else {
                item.thumbnailPath = Uri.parse(path);
            }
            item.identifier = itemObject.optString("identifier");
            item.categoryType = itemObject.optInt("categoryType");

            // New style!
            if (itemObject.has("intervalData")) {
                JSONObject intervalDataJso = itemObject.getJSONObject("intervalData");
                for (Iterator<String> keyIt = intervalDataJso.keys(); keyIt.hasNext(); ) {
                    String key = keyIt.next();
                    item.intervalData.put(key, parseIntervalData(intervalDataJso.getJSONArray(key)));
                }
            }

            // Old style!
            if (itemObject.has("customWorkoutData")) {
                JSONArray itemWorkoutData = itemObject.getJSONArray("customWorkoutData");
                for (int j = 0; j < itemWorkoutData.length(); j++) {
                    JSONObject itemWorkoutDataItem = itemWorkoutData.getJSONObject(j);
                    String parameterId = itemWorkoutDataItem.optString("identifier");
                    item.intervalData.put(parameterId, parseIntervalData(itemWorkoutDataItem.optJSONArray("intervalData")));
                }
            }

            if (shouldConvertSpeeds) {
                List<Float> speeds = item.intervalData.get(ConsoleWorkoutVariables.START_SPEED);
                if (speeds != null) {
                    item.intervalData.put(ConsoleWorkoutVariables.START_SPEED, convertSpeeds(speeds, !displayMetric));
                }
            }

            customWorkoutItems.add(item);
        }

        return customWorkoutItems;
    }

    public static List<CustomWorkoutItem> fromJson(@Nullable String json, boolean displayMetric) throws JSONException {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        return fromJsonObject(new JSONObject(json), displayMetric);
    }
}
