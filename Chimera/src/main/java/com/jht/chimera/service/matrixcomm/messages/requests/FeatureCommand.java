package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.app.LaunchAppUtil;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * The feature command allows the third part to open our settings app, go to our home page,
 * or go to the Android launcher.
 */
public class FeatureCommand extends RequestBase {
    public enum FEATURE {
        SETUP,
        HOME,
        ANDROID,
        INVALID;

        static public FEATURE fromString(String value) {
            try {
                return FEATURE.valueOf(value.toUpperCase(Locale.ENGLISH));
            }
            catch (IllegalArgumentException ignored) {
                return INVALID;
            }
        }
    }
    private final FEATURE feature;
    private final String featureString;

    /**
     * Create the feature command.
     * @param feature   The feature to execute.
     */
    @VisibleForTesting
    public FeatureCommand(FEATURE feature) {
        super("feature", 1);
        this.feature = feature;
        featureString = feature.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public FeatureCommand(JSONObject object) throws JSONException {
        super(object);
        featureString = object.optString("feature", "");
        feature = FEATURE.fromString(featureString);
    }

    /**
     * Go to the settings, home, or android launcher screen.  This command can only be sent
     * if we are not in a workout.
     *
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        if (DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) != WorkoutKeys.Constants.RUN_STATE_STOP) {
            sendErrorResponse("'feature' command cannot run because workout is still running", socket);
        }

        switch (feature) {
            case SETUP: LaunchAppUtil.launchSystemSetup(); break;
            case HOME: LaunchAppUtil.launchSplashScreen(); break;
            case ANDROID: LaunchAppUtil.launchAndroidLauncher(); break;
            case INVALID:
            default:
                sendGeneralResponse("invalid feature", socket);
                return;
        }

        sendGeneralResponse("ok", socket);
    }

    /**
     * @return  The response in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("feature", feature.toString().toLowerCase(Locale.ENGLISH));
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "FeatureCommand: result " + feature + ", feature " + super.toString();
    }
}
