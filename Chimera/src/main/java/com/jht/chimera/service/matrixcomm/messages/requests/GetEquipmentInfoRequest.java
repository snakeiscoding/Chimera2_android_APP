package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Replies with the current equipment info so the third party knows how to properly control
 * the machine and what workouts it can present to them.
 */
public class GetEquipmentInfoRequest extends RequestBase {

    /**
     * Create a generic request.
     */
    @VisibleForTesting
    public GetEquipmentInfoRequest() {
        super("getequipmentinfo", 1);
    }


    /**
     * Parse the request we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public GetEquipmentInfoRequest(JSONObject object) throws JSONException {
        super(object);
    }

    /**
     * Send a response.  There are no real error conditions here.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        new GetEquipmentInfoResponse(this).send(socket);

    }

    @NonNull
    @Override
    public String toString() {
        return "GetEquipmentInfoRequest: " + super.toString();
    }
}
