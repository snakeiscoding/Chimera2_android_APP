package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Response to the control state message.  We echo back the value and result in the message.
 */
public class ControlStateResponse extends RequestBase {
    private final String result;
    private final double value;

    /**
     * Create the response based on the request.
     * @param request   The request that generated the response.
     * @param result    The result of the request.
     * @param value     The resulting value. (Current speed, incline, or resistance).
     */
    public ControlStateResponse(RequestBase request, String result, double value) {
        super(request);
        this.result = result;
        this.value = value;
    }

    /**
     * Parse the response we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public ControlStateResponse(JSONObject object) throws JSONException {
        super(object);
        result = object.getString("result");
        value = object.getDouble("value");
    }


    /**
     * Used to check that we didn't get extra fields.  Useful for testing.
     * @return  The number of fields we would expect.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 2 + super.expectedFields(); }

    /**
     * @return  The response in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("result", result);
            object.put("value", value);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof ControlStateResponse) {
            ControlStateResponse otherResponse = (ControlStateResponse)other;
            return super.equals(other) &&
                    otherResponse.result.equals(result) &&
                    Math.abs(otherResponse.value - value) < 0.1;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ControlStateResponse: result " + result + ", value " + value + ", " + super.toString();
    }
}
