package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Used to set the speed, incline, or resistance.
 */
public class SetWorkoutParameterCommand extends RequestBase {
    public enum WORKOUT_PARAMETER {
        SPEED,
        INCLINE,
        RESISTANCE,
        INVALID;

        public static WORKOUT_PARAMETER fromString(String value) {
            try {
                return WORKOUT_PARAMETER.valueOf(value.toUpperCase(Locale.ENGLISH));
            }
            catch (IllegalArgumentException ignored) {
                return INVALID;
            }
        }
    }
    private final WORKOUT_PARAMETER parameter;
    private final double value;
    private final String parameterName;

    /**
     * Create the basic command.
     * @param parameter Speed, incline, or resistance.
     * @param value     Value to set.
     */
    @VisibleForTesting
    public SetWorkoutParameterCommand(WORKOUT_PARAMETER parameter, double value) {
        super("setworkoutparameter", 1);
        this.parameter = parameter;
        parameterName = parameter.toString().toLowerCase(Locale.ENGLISH);
        this.value = value;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public SetWorkoutParameterCommand(JSONObject object) throws JSONException {
        super(object);
        parameterName = object.optString("type", "invalid");
        parameter = WORKOUT_PARAMETER.fromString(parameterName);
        value = parameter == WORKOUT_PARAMETER.RESISTANCE ?
                object.optInt("value", 1) :
                object.optDouble("value", 0.0);
    }

    /**
     * Set the speed, incline, or resistance.  The valid types depend on the current machine type.
     * If the command is invalid or out of range then send the error message.
     *
     * TODO: For some reason we say ok if we are not in a workout.  We should respond with an error.
     *       Issue tracked at https://jhtnaee.atlassian.net/browse/CHIM-9225.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

        if(DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_RUNNING) {
            // TODO: Need to update this message, but can't because that will break IFIT.  Tracked in https://jhtnaee.atlassian.net/browse/CHIM-9225.
            sendGeneralResponse("invalid value", socket);
            return;
        }
        switch (parameter) {
            case SPEED:
                if (!frameConfig.hasSpeed()) {
                    sendGeneralResponse("invalid value", socket);
                    return;
                }
                if (value != 0 && (
                        value < frameConfig.speedKph.getMin() || value > frameConfig.getEffectiveMaxSpeedKph()
                )) {
                    sendGeneralResponse("out of range", socket);
                    return;
                }
                DataMatrix.putFloat(WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT, (float)value);
                break;
            case INCLINE:
                if (!frameConfig.hasIncline()) {
                    sendGeneralResponse("invalid value", socket);
                    return;
                }
                if (value < frameConfig.incline.getMin() || value > frameConfig.incline.getMax()) {
                    sendGeneralResponse("out of range", socket);
                    return;
                }
                DataMatrix.putFloat(WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT, (float)value);
                break;
            case RESISTANCE:
                if (!frameConfig.hasResistance()) {
                    sendGeneralResponse("invalid value", socket);
                    return;
                }
                if (value != 0 && (
                        value < frameConfig.resistance.getMin() || value > frameConfig.resistance.getMax()
                 )) { // allow for climb mill stop
                    sendGeneralResponse("out of range", socket);
                    return;
                }
                DataMatrix.putInt(WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT, (int)value);
                break;
            default:
                sendErrorResponse("Unsupported workout parameter: '" + parameterName + "'", socket);
                return;
        }
        sendGeneralResponse("ok", socket);
    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("type", parameter.toString().toLowerCase(Locale.ENGLISH));
            object.put("value", parameter == WORKOUT_PARAMETER.RESISTANCE ? (int)value : value);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "SetWorkoutParameterCommand: type " + parameter.toString() + ", value " + value + ", " + super.toString();
    }
}
