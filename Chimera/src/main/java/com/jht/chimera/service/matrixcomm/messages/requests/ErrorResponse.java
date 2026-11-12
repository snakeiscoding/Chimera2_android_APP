package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * The generic error response.
 */
public class ErrorResponse extends GeneralResponse {
    private final String responseMessage;

    /**
     * Create the error response.
     * @param request       The request that generated the error.
     * @param response      The error message.
     */
    public ErrorResponse(RequestBase request, String response) {
        super(request, "error");
        this.responseMessage = response;
    }

    /**
     * Parse the response we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public ErrorResponse(JSONObject object) throws JSONException {
        super(object);
        responseMessage = object.getString("errormessage");
    }

    /**
     * Used to check that we didn't get extra fields.  Useful for testing.
     * @return  The number of fields we would expect.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 1 + super.expectedFields(); }

    @Override
    public boolean equals(Object other) {
        if(other instanceof ErrorResponse) {
            ErrorResponse otherResponse = (ErrorResponse)other;
            return super.equals(other) && otherResponse.responseMessage.toLowerCase(Locale.ENGLISH).equals(responseMessage.toLowerCase(Locale.ENGLISH));
        }
        return false;
    }

    /**
     * @return  The response in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();

        try {
            object.put("result", "error");
            object.put("errormessage", responseMessage);
        } catch (JSONException ignored) {
        }
        return object;
    }

    /**
     * Send the error response to the socket given.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        socket.sendData(getRequest());
    }

    @Override
    public String toString() {
        return "ErrorResponse: responseMessage " + responseMessage + ", " +  super.toString();
    }
}
