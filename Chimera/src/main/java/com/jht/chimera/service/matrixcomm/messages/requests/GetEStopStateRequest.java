package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.chimera.service.matrixcomm.messages.events.EStopEventMonitor;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Request to see if the EStop is engaged on a treadmill or climbmill.
 */
public class GetEStopStateRequest extends RequestBase {
    /**
     * Basic request.
     */
    @VisibleForTesting
    public GetEStopStateRequest() {
        super("getestopstate", 1);
    }


    /**
     * Parse the request we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public GetEStopStateRequest(JSONObject object) throws JSONException {
        super(object);
    }

    /**
     * Send a general response with set if any safety key is triggered.  Reset otherwise.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        sendGeneralResponse(EStopEventMonitor.isAnySafetyKeyTriggered() ? "set" : "reset", socket);
    }

    @Override
    public String toString() {
        return "GetEStopStateRequest: " + super.toString();
    }
}
