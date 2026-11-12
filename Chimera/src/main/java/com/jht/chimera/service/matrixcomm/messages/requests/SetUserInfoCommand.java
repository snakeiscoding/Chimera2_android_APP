package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.datamatrixkeys.DAPIKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to set the user information.  Currently this is just setting the weight.
 */
public class SetUserInfoCommand extends RequestBase {
    private final double kg;

    /**
     * Create the basic request.
     * @param kg    Usaer weight.
     */
    @VisibleForTesting
    public SetUserInfoCommand(double kg) {
        super("setuserinfo", 1);
        this.kg = kg;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public SetUserInfoCommand(JSONObject object) throws JSONException {
        super(object);
        kg = object.optDouble("weight", DataMatrix.getFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT));
    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("weight", kg);
        } catch (JSONException ignored) {
        }
        return object;
    }

    /**
     * Set the user weight in the system if it is valid.  If the weight is out of range, then
     * reply with an error.
     *
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {

        if (kg < FrameConfigManager.getInstance().getFrameConfig().weightKg.getMin() || kg > FrameConfigManager.getInstance().getFrameConfig().weightKg.getMax()) {
            sendErrorResponse("invalid weight", socket);
        }
        else {
            DataMatrix.putFloat(DAPIKeys.DATA_USER_WEIGHT_KG_FLOAT, (float) kg);
            sendGeneralResponse("ok", socket);
        }
    }

    @Override
    public String toString() {
        return "SetUserInfoCommand: kg " + kg + ", " + super.toString();
    }
}
