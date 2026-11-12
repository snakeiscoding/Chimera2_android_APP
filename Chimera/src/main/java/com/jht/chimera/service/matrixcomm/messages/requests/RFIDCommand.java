package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Controls whether we should actively scan for watches and RFID tags.
 */
public class RFIDCommand extends RequestBase {

    private final boolean scanTags;
    private final boolean scanWatch;

    /**
     * Default constructor used to create the command.
     */
    @VisibleForTesting
    public RFIDCommand(boolean scanTags, boolean scanWatch) {
        super("rfidscan", 1);
        this.scanTags = scanTags;
        this.scanWatch = scanWatch;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param command           The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public RFIDCommand(JSONObject command) throws JSONException {
        super(command);
        scanTags = command.optBoolean("scanTags", false);
        scanWatch = command.optBoolean("scanWatch", false);
    }

    /**
     * @return  The command in JSON form.
     */
    @Override
    public JSONObject getRequest() {
        JSONObject object = super.getRequest();
        try {
            object.put("scanTags", scanTags);
            object.put("scanWatch", scanWatch);
        } catch (JSONException ignored) {
        }
        return object;
    }

    /**
     * Start the beep timer and respond with an OK.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_RFID_TAGS_BOOL, scanTags);
        DataMatrix.putBoolean(HALKeys.DATA_SCAN_WATCHES_BOOL, scanWatch);
        sendGeneralResponse("ok", socket);
    }

    @NonNull
    @Override
    public String toString() {
        return "RFIDCommand: tags " + scanTags + " watch " + scanWatch + " " + super.toString();
    }
}
