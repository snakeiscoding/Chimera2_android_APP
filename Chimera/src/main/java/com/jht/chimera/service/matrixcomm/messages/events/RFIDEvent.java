package com.jht.chimera.service.matrixcomm.messages.events;

import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.rfid.Legic.RFIDChip;
import com.jht.rfid.LegicEnums.RfStandard;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Used to notify the third part that the EStop state has changed.
 */
public class RFIDEvent extends EventBase {
    private final RFIDChip rfidChip;

    /**
     * Create the estop event.
     */
    public RFIDEvent(RFIDChip rfidChip) {
        super("rfid", 1);
        this.rfidChip = rfidChip;
    }

    /**
     * Parse the event we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    @VisibleForTesting
    public RFIDEvent(JSONObject object) throws JSONException {
        super(object);
        RfStandard rfStandard = RfStandard.Unknown;
        String rfType = object.getString("type").toLowerCase(Locale.ENGLISH);
        for(RfStandard rf : RfStandard.values()) {
            if(rf.name().toLowerCase(Locale.ENGLISH).equals(rfType)) {
                rfStandard = rf;
                break;
            }
        }
        rfidChip = new RFIDChip(rfStandard, Strings.hexStringToByteArray(object.optString("value", "")));
    }

    /**
     * @return Number of fields the event should contain.  Useful for testing.
     */
    @Override
    @VisibleForTesting
    public int expectedFields() { return 2 + super.expectedFields(); }

    @Override
    public boolean equals(Object other) {
        if(other instanceof RFIDEvent) {
            RFIDEvent otherResponse = (RFIDEvent)other;
            return super.equals(other) && otherResponse.rfidChip.isChip(rfidChip);
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
            object.put("value", Strings.bytesToHex(rfidChip.uid()));
            switch (rfidChip.rfStandard()) {

                case LegicRf:
                    object.put("type", "LegicRf");
                    break;
                case Iso15693:
                    object.put("type", "Iso15693");
                    break;
                case Iso14443A:
                    object.put("type", "Iso14443A");
                    break;
                case Iso14443B:
                    object.put("type", "Iso14443B");
                    break;
                case InsideSecure:
                    object.put("type", "InsideSecure");
                    break;
                case SonyFelica:
                    object.put("type", "SonyFelica");
                    break;
                case Unknown:
                    object.put("type", "Unknown");
                    break;
            }
        } catch (JSONException ignored) {
        }
        return object;
    }

    @Override
    public String toString() {
        return "RFIDEvent: value " + rfidChip + ", " +  super.toString();
    }
}
