package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to notify the third part that the EStop state has changed.
 */
public class EStopEvent extends EventBase {
    private final String value;

    /**
     * Create the estop event.
     * @param set   True if the EStop is engaged.
     */
    public EStopEvent(boolean set) {
        super("emergencystop", 1);
        this.value = set ? "set" : "reset";
    }

    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    public EStopEvent(JSONObject object) throws JSONException {
        super(object);
        value = object.getString("value");
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 1 + super.expectedFields(); }

    @Override
    public boolean equals(Object other) {
        if(other instanceof EStopEvent) {
            EStopEvent otherResponse = (EStopEvent)other;
            return super.equals(other) && otherResponse.value.equals(value);
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
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "EStopEvent: value " + value + ", " +  super.toString();
    }
}
