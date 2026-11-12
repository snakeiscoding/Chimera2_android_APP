package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for event messages.
 */
public class EventBase {
    private final String event;
    private final int version;

    /**
     * Create the event.
     * @param event     Name of the event.
     * @param version   Message format version.
     */
    EventBase(String event, int version) {
        this.event = event;
        this.version = version;
    }


    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    EventBase(JSONObject object) throws JSONException {
        event = object.getString("event");
        version = object.getInt("version");
    }

    /**
     * @return  The event in JSON form.
     */
    public JSONObject getRequest() {
        JSONObject object = new JSONObject();
        try {
            object.put("event", event);
            object.put("version", version);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @VisibleForTesting
    public int expectedFields() { return 2; }

    @Override
    public boolean equals(Object other) {
        if(other instanceof EventBase) {
            EventBase otherResponse = (EventBase)other;
            return otherResponse.event.equals(event) &&
                    otherResponse.version == version;
        }
        return false;
    }

    @Override
    public String toString() {
        return "event " + event + ", vesion " + version;
    }

}
