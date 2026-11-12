package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to notify a third party that an error has occurred.
 */
public class ErrorCodeEvent extends EventBase {
    private final int value;
    private final int repeated;

    /**
     * Create the errorCode event.
     * @param value   error code.
     * @param repeated   > 0 if the error code is repeatedly being sent.
     */
    public ErrorCodeEvent(int value, int repeated) {
        super("errorcode", 1);
        this.value = value;
        this.repeated = repeated;
    }

    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    public ErrorCodeEvent(JSONObject object) throws JSONException {
        super(object);
        value = object.getInt("value");
        repeated = object.getInt("repeated");
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 2 + super.expectedFields(); }

    @Override
    public boolean equals(Object other) {
        if(other instanceof ErrorCodeEvent) {
            ErrorCodeEvent otherResponse = (ErrorCodeEvent)other;
            return super.equals(other) && otherResponse.value == value
                    && otherResponse.repeated == repeated;
        }
        return false;
    }

    /**
     * @return  The event in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();

        try {
            object.put("value", value);
            object.put("repeated", repeated);
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "Error Code: value-" + value + " repeated-" + repeated + ", " +  super.toString();
    }
}
