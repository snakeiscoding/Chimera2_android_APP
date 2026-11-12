package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.chimera.service.workout.SafeToStartFlags;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Used to stop, pause, resume workouts.  Also has commands to do a passive start or stop
 * which only applies to retail where part or all of the workout is done off of the machine.
 */
public class WorkoutControlCommand extends RequestBase {
    public enum ACTION {
        PAUSE,
        STOP,
        RESUME,
        PASSIVESTART,
        PASSIVESTOP,
        INVALID;

        public static ACTION fromString(String value) {
            try {
                return ACTION.valueOf(value.toUpperCase(Locale.ENGLISH));
            }
            catch (IllegalArgumentException ignored) {
                return INVALID;
            }
        }
    }
    private final ACTION action;
    private final String actionName;

    /**
     * Create a workotu control command for testing.
     * @param action    The command.
     */
    @VisibleForTesting
    public WorkoutControlCommand(ACTION action) {
        super("workoutcontrol", 1);
        this.action = action;
        actionName = action.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public WorkoutControlCommand(JSONObject object) throws JSONException {
        super(object);

        actionName = object.optString("action", "invalid");
        action = ACTION.fromString(actionName);
    }

    /**
     * Change the workout state.
     *
     * TODO: The resume does not check to see if it is safe.  We might be passing back a false positive.
     *       Also we aren't verifying that we are in a workout.  Maybe that is no big deal.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        switch (action) {
            case PAUSE:
                DataMatrix.sendEvent(WorkoutKeys.ACTION_PAUSE_WORKOUT);
                break;
            case STOP:
                DataMatrix.sendEvent(WorkoutKeys.ACTION_STOP_WORKOUT);
                break;
            case RESUME:
                if (
                    (
                        FrameConfigManager.getInstance().getFrameConfig().machineClassType == JHTCommonTypes.MachineClassificationType.Climbmill &&
                        DataMatrix.getBoolean(HALKeys.DATA_CLIMB_START_CONTROL_STATUS_BOOL)
                    ) ||
                        !DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)
                ) {
                    sendGeneralResponse("cannot resume workout", socket);

                    //Display a message due to the SafeToStart restriction.
                    int isSafeToStartShow = DataMatrix.getInt(HALKeys.DATA_TM_IS_SAFE_TO_START_BITMASK_INT);
                    if(!DataMatrix.getBoolean(HALKeys.DATA_IS_SAFE_TO_START_WORKOUT_BOOL)) SafeToStartFlags.checkSafeToStartWithNotification(isSafeToStartShow);
                    return;
                }
                else {
                    DataMatrix.sendEvent(WorkoutKeys.ACTION_RESUME_WORKOUT);
                }
                break;
            case PASSIVESTART:
                DataMatrix.putBoolean(WorkoutKeys.ACTION_PASSIVE_WORKOUT_BOOL, true);
                break;
            case PASSIVESTOP:
                DataMatrix.putBoolean(WorkoutKeys.ACTION_PASSIVE_WORKOUT_BOOL, false);
                break;
            case INVALID:
                sendGeneralResponse("invalid action", socket);
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
            object.put("action", action.toString().toLowerCase(Locale.ENGLISH));
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "WorkoutControlCommand: action " + action.toString() + ", " + super.toString();
    }
}
