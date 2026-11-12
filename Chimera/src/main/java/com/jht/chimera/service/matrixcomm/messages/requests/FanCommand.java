package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Controls fan speed
 */
public class FanCommand extends RequestBase {

    private int fanSpeed;

    /**
     * Default constructor used to create the command.
     */
    @VisibleForTesting
    public FanCommand(int fanSpeed) {
        super("fan", 1);
        if (fanSpeed < 0) this.fanSpeed = 0;
        else if (fanSpeed > 3) this.fanSpeed = 3;
        else this.fanSpeed = fanSpeed;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param command           The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public FanCommand(JSONObject command) throws JSONException {
        super(command);
        int fanSpeedOpt = command.optInt("parameter", 0);
        if (fanSpeedOpt < 0) this.fanSpeed = 0;
        else if (fanSpeedOpt > 3) this.fanSpeed = 3;
        else this.fanSpeed = fanSpeedOpt;
    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("parameter", fanSpeed);
        } catch (JSONException ignored) {
        }
        return object;
    }

    /**
     * Start the beep timer and respond with an OK.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        DataMatrix.putInt(HALKeys.DATA_IO_SET_FAN_SPEED_INT, fanSpeed);
        sendGeneralResponse("ok", socket);
    }

    @NonNull
    @Override
    public String toString() {
        return "FanCommand: speed " + fanSpeed + " " + super.toString();
    }
}
