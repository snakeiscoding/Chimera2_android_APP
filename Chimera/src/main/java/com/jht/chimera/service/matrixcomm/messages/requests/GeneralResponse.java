package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used for generic responses to the command.  Typically this is an OK message, but sometimes
 * can include an error message in the response.
 */
public class GeneralResponse extends RequestBase {
    private final String response;

    /**
     * Create the general response.
     * @param request       The request that generated the response.
     * @param response      The response message.
     */
    public GeneralResponse(RequestBase request, String response) {
        super(request);
        this.response = response;
    }

    /**
     * Parse the response we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public GeneralResponse(JSONObject object) throws JSONException {
        super(object);
        response = object.getString("result");
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
        if(other instanceof GeneralResponse) {
            GeneralResponse otherResponse = (GeneralResponse)other;
            return super.equals(other) && otherResponse.response.equals(response);
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
            object.put("result", response);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "GeneralResponse: response " + response + ", " +  super.toString();
    }
}
