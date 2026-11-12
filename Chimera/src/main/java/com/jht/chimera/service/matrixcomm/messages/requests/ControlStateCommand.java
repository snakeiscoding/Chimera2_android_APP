package com.jht.chimera.service.matrixcomm.messages.requests;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.StatIncrementer;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Handles a control state command.  Used to increment or decrement the speed, incline, or resistance.
 */
public class ControlStateCommand extends RequestBase {
    // Action for the command.
    public enum ACTION {
        SPEEDUP,
        SPEEDDOWN,
        INCLINEUP,
        INCLINEDOWN,
        RESISTANCEUP,
        RESISTANCEDOWN,
        INVALID;

        public static ACTION fromString(String action) {
            try {
                return ACTION.valueOf(action.toUpperCase(Locale.ENGLISH));
            }
            catch(IllegalArgumentException ignored) {
                return INVALID;
            }
        }
    }
    private final ACTION action;
    private final String actionString;

    /**
     * Create a control state command.  Useful for testing.
     * @param action    The action to perform.
     */
    @VisibleForTesting
    public ControlStateCommand(ACTION action) {
        super("control", 1);
        this.action = action;
        actionString = action.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public ControlStateCommand(JSONObject object) throws JSONException {
        super(object);
        actionString = object.optString("parameter", "");
        action = ACTION.fromString(actionString);
    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("parameter", action.toString().toLowerCase(Locale.ENGLISH));
        } catch (JSONException ignored) {
        }
        return object;
    }

    /**
     * Increment or decrement the speed, incline, or resistance based on the command.  Reply with
     * ok if we can do that command on the given system.  Otherwise reply with an appropriate error.
     *
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        StatIncrementer statIncrementer = null;
        switch (action) {
            case SPEEDUP:
            case SPEEDDOWN:
                if(FrameConfigManager.getInstance().getFrameConfig().hasSpeed()) {
                    if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) {
                        statIncrementer =
                                new StatIncrementer(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.toString(), WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT.toString(),
                                        action.equals(ACTION.SPEEDUP), FrameConfigManager.getInstance().getFrameConfig().speedKph, StatIncrementer.TYPE_FLOAT, null);
                    } else {
                        statIncrementer =
                                new StatIncrementer(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT.toString(), WorkoutKeys.ACTION_SET_SPEED_KPH_WORKOUT_FLOAT.toString(),
                                        action.equals(ACTION.SPEEDUP), FrameConfigManager.getInstance().getFrameConfig().speedMph, StatIncrementer.TYPE_FLOAT, null);
                        statIncrementer.setConversionFactor(SpeedConvertUtil.MILES_PER_HOUR_FACTOR);
                    }
                }
                else {
                    sendErrorResponse("speed not available", socket);
                }
                break;
            case INCLINEUP:
            case INCLINEDOWN:
                if(FrameConfigManager.getInstance().getFrameConfig().hasIncline()) {
                    statIncrementer =
                            new StatIncrementer(HALKeys.DATA_TARGET_INCLINE_FLOAT.toString(), WorkoutKeys.ACTION_SET_INCLINE_WORKOUT_FLOAT.toString(),
                                    action.equals(ACTION.INCLINEUP), FrameConfigManager.getInstance().getFrameConfig().incline, StatIncrementer.TYPE_FLOAT, null);
                }
                else {
                    sendErrorResponse("incline not available", socket);
                }
                break;
            case RESISTANCEUP:
            case RESISTANCEDOWN:
                Log.d("ControlStateCommand.java", "####CS JSONLocalSocket RESISTANCEUP/RESISTANCEDOWN action");
                if(FrameConfigManager.getInstance().getFrameConfig().hasResistance()) {
                    statIncrementer =
                            new StatIncrementer(HALKeys.DATA_RESISTANCE_INT.toString(), WorkoutKeys.ACTION_SET_RESISTANCE_WORKOUT_INT.toString(),
                                    action.equals(ACTION.RESISTANCEUP), FrameConfigManager.getInstance().getFrameConfig().resistance, StatIncrementer.TYPE_INT, null);
                }
                else {
                    sendErrorResponse("resistance not available", socket);
                }
                break;
            case INVALID:
            default:
                sendErrorResponse("Unrecognized parameter: '" + actionString + "'", socket);
                break;
        }

        if(statIncrementer != null) {
            Float result = statIncrementer.changeValue();
            new ControlStateResponse(this, result != null ? "ok" : "at limit", result == null ? statIncrementer.currentValue() : result).send(socket);
        }
    }

    @Override
    public String toString() {
        return "ControlStateCommand: control " + action.toString() + ", " + super.toString();
    }
}
