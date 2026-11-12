package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Called whenever a key is pressed so we can send the event to the third party.
 */
public class KeyEvent extends EventBase {
    public enum KeyState {
        PRESSED,
        RELEASED,
        UNKNOWN;

        public static KeyState fromString(String value) {
            for(KeyState state : KeyState.values()) {
                if(state.toString().toLowerCase(Locale.ENGLISH).equals(value.toLowerCase(Locale.ENGLISH))) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }
    private final String name;
    private final KeyState state;

    /**
     * Create the key event.
     * @param name      The key name.
     * @param state     Pressed or released.
     */
    public KeyEvent(String name, KeyState state) {
        super("key", 1);
        this.name = name;
        this.state = state;
    }

    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    public KeyEvent(JSONObject object) throws JSONException {
        super(object);
        name = object.getString("name");
        state = KeyState.fromString(object.getString("state"));
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 2 + super.expectedFields(); }

    @Override
    public boolean equals(Object other) {
        if(other instanceof KeyEvent) {
            KeyEvent otherResponse = (KeyEvent)other;
            return super.equals(other) && otherResponse.name.equals(name) && otherResponse.state.equals(state);
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
            object.put("name", name);
            object.put("state", state.toString().toLowerCase(Locale.ENGLISH));
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "FullScreenEvent: name " + name + ", state " + state + ", " + super.toString();
    }
}
