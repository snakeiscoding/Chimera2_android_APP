package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

/**
 * The response message to the get equipment info.
 */
public class GetEquipmentInfoResponse extends RequestBase {
    private final JHTCommonTypes.MachineType machineType;
    private final int minResistance;
    private final int maxResistance;
    private final double minSpeed;
    private final double maxSpeed;
    private final double maxIncline;
    private final int maxTime;
    private final int maxCalories;
    private final double maxDistance;
    private final String frameSerialNumber;
    private final String consoleSerialNumber;
    private final int clubId;
    private final float accumulatedDistance;
    private final int accumulatedTime;
    private final String market;

    private final HashMap<JHTCommonTypes.MachineType, String> machineTypeMap = new HashMap<>();


    /**
     * Create the response gathering all system information.
     * @param request   The request that generated the response.
     */
    public GetEquipmentInfoResponse(RequestBase request) {
        super(request);
        initializeMachineMap();
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        machineType = frameConfig.machineType.equals(JHTCommonTypes.MachineType.CardioSpinBike) ? JHTCommonTypes.MachineType.UprightBike : frameConfig.machineType;
        minResistance = (int)frameConfig.resistance.getMin();
        maxResistance = (int)frameConfig.resistance.getMax();
        minSpeed = frameConfig.getEffectiveMinSpeedKph();
        maxSpeed = frameConfig.getEffectiveMaxSpeedKph();
        maxIncline = frameConfig.incline.getMax();
        maxTime = FrameConfig.THIRD_PARTY_MAX_WORKOUT_TIME_SECONDS;
        maxCalories = (int)frameConfig.workoutCalories.getMax();
        maxDistance = FrameConfig.THIRD_PARTY_MAX_WORKOUT_DISTANCE;
        frameSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_FRAME_SERIAL_STRING);
        consoleSerialNumber = DataMatrix.getString(SettingKeys.AssetManagement.DATA_CONSOLE_SERIAL_STRING);
        clubId = DataMatrix.getInt(SettingKeys.AssetManagement.DATA_CLUB_ID_INT);
        accumulatedDistance = DataMatrix.getFloat(SettingKeys.General.DATA_ACCUMULATED_DISTANCE_KM_FLOAT);
        accumulatedTime = DataMatrix.getInt(SettingKeys.General.DATA_ACCUMULATED_TIME_SECONDS_INT);
        market = getMarketString();
    }

    /**
     * Parse the response we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public GetEquipmentInfoResponse(JSONObject object) throws JSONException {
        super(object);
        initializeMachineMap();
        machineType = getMachineTypeFromString(object.getString("type").toLowerCase(Locale.ENGLISH));

        minResistance = object.getInt("minResistance");
        maxResistance = object.getInt("maxResistance");
        minSpeed = object.getDouble("minSpeed");
        maxSpeed = object.getDouble("maxSpeed");
        maxIncline = object.getDouble("maxIncline");
        maxTime = object.getInt("maxTime");
        maxCalories = object.getInt("maxCalories");
        maxDistance = object.getDouble("maxDistance");
        frameSerialNumber = object.getString("snFrame");
        consoleSerialNumber = object.getString("snConsole");
        clubId = object.getInt("clubId");
        accumulatedDistance = object.getInt("accumulatedDistance");
        accumulatedTime = object.getInt("accumulatedTime");
        market = object.getString("market");
    }

    @Override
    @VisibleForTesting
    public int expectedFields() { return 14 + super.expectedFields(); }

    /**
     * Used to check that we didn't get extra fields.  Useful for testing.
     * @return  The number of fields we would expect.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("type", getMachineTypeString(machineType));
            object.put("minResistance", minResistance);
            object.put("maxResistance", maxResistance);
            object.put("minSpeed", minSpeed);
            object.put("maxSpeed", maxSpeed);
            object.put("maxIncline", maxIncline);
            object.put("maxTime", maxTime);
            object.put("maxCalories", maxCalories);
            object.put("maxDistance", maxDistance);
            object.put("snFrame", frameSerialNumber);
            object.put("snConsole", consoleSerialNumber);
            object.put("clubId", clubId);
            object.put("accumulatedDistance", accumulatedDistance);
            object.put("accumulatedTime", accumulatedTime);
            object.put("market", market);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof GetEquipmentInfoResponse) {
            GetEquipmentInfoResponse otherResponse = (GetEquipmentInfoResponse)other;

            return super.equals(other) &&
                    otherResponse.machineType.equals(machineType) &&
                    otherResponse.minResistance == minResistance &&
                    otherResponse.maxResistance == maxResistance &&
                    Math.abs(otherResponse.minSpeed - minSpeed) < 0.1 &&
                    Math.abs(otherResponse.maxSpeed - maxSpeed) < 0.1 &&
                    Math.abs(otherResponse.maxIncline - maxIncline) < 0.1 &&
                    otherResponse.maxTime == maxTime &&
                    otherResponse.maxCalories == maxCalories &&
                    Math.abs(otherResponse.maxDistance - maxDistance) < 0.1 &&
                    otherResponse.frameSerialNumber.equals(frameSerialNumber) &&
                    otherResponse.consoleSerialNumber.equals(consoleSerialNumber) &&
                    otherResponse.clubId == clubId &&
                    otherResponse.accumulatedDistance == accumulatedDistance &&
                    otherResponse.accumulatedTime == accumulatedTime &&
                    otherResponse.market.equals(market);
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return "GetEquipmentInfoResponse: machineType " + machineType +
                ", minResistance" +  minResistance +
                ", maxResistance" +  maxResistance +
                ", minSpeed" +  minSpeed +
                ", maxSpeed" +  maxSpeed +
                ", maxIncline" +  maxIncline +
                ", maxTime" +  maxTime +
                ", maxCalories" +  maxCalories +
                ", maxDistance" +  maxDistance +
                ", frameSerialNumber" +  frameSerialNumber +
                ", consoleSerialNumber" +  consoleSerialNumber +
                ", accumulatedDistance" +  accumulatedDistance +
                ", accumulatedTime" +  accumulatedTime +
                ", market" + market +
                ", " + super.toString();
    }

    /**
     * Create a mapping from machine type to a string.
     */
    private void initializeMachineMap() {
        machineTypeMap.put(JHTCommonTypes.MachineType.Treadmill, "treadmill");
        machineTypeMap.put(JHTCommonTypes.MachineType.UprightBike, "upright bike");
        machineTypeMap.put(JHTCommonTypes.MachineType.HybridBike, "hybrid bike");
        machineTypeMap.put(JHTCommonTypes.MachineType.RecumbentBike, "recumbent bike");
        machineTypeMap.put(JHTCommonTypes.MachineType.RecumbentElliptical, "recumbent elliptical");
        machineTypeMap.put(JHTCommonTypes.MachineType.Climbmill, "climbmill");
        machineTypeMap.put(JHTCommonTypes.MachineType.Stepper, "stepper");
        machineTypeMap.put(JHTCommonTypes.MachineType.Ascent, "ascent trainer");
        machineTypeMap.put(JHTCommonTypes.MachineType.Elliptical, "elliptical");
    }

    /**
     *
     * @param value The machine type string from the request.
     * @return      The corresponding frame type.
     */
    private JHTCommonTypes.MachineType getMachineTypeFromString(String value) {
        for(HashMap.Entry<JHTCommonTypes.MachineType, String> values : machineTypeMap.entrySet()) {
            if(values.getValue().equals(value)) {
                return values.getKey();
            }
        }
        return null;
    }

    /**
     *
     * @param machineType   The current machine type.
     * @return              The machine type as string per the API documentation.
     */
    private String getMachineTypeString(JHTCommonTypes.MachineType machineType) {
        if(machineType.equals(JHTCommonTypes.MachineType.CardioSpinBike)) {
            // We treat Athena like an upright bike.  Annoying, but that is the current documentation.
            return getMachineTypeString(JHTCommonTypes.MachineType.UprightBike);
        } else if (machineType.equals(JHTCommonTypes.MachineType.RecumbentElliptical)) {
            // We treat Recumbent Elliptical like a recumbent bike.  Annoying, but that is the current documentation.
            return getMachineTypeString(JHTCommonTypes.MachineType.RecumbentBike);
        }
        String value = machineTypeMap.get(machineType);
        return value == null ? "" : value;
    }

    /**
     * Gets the market that this console is for.
     * @return The string for the market this console uses.
     */
    private String getMarketString() {
        // Find which market this console is part of
        if (ProductConfig.isCommercial() || ProductConfig.isLightCommercial()) {
            return "commercial";
        } else if (ProductConfig.isRetail()) {
            return "retail";
        } else {
            return "unknown";
        }
    }
}
