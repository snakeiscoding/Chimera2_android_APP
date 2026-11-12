package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sent when we go in and out of full screen mode.
 */
public class FullScreenEvent extends EventBase {
    private final String value;

    /**
     * Create the full screen event.
     * @param value The state of the full screen.
     */
    public FullScreenEvent(String value) {
        super("fullscreen", 1);
        this.value = value;
    }

    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    public FullScreenEvent(JSONObject object) throws JSONException {
        super(object);
        value = object.getString("value");
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 1 + super.expectedFields(); }

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
    public boolean equals(Object other) {
        if(other instanceof FullScreenEvent) {
            FullScreenEvent otherResponse = (FullScreenEvent)other;
            return super.equals(other) && otherResponse.value.equals(value);
        }
        return false;
    }

    @Override
    public String toString() {
        return "FullScreenEvent: value " + value + ", " +  super.toString();
    }
}
