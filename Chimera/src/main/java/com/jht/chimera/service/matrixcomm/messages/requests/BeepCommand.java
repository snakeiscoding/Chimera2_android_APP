package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.androidcommonalgorithms.timer.CountdownTimer;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles the beep command.  For version 1 we can get an optional count and we should
 * beep that many times with a 500 ms delay.  Unsure how useful that is.
 */
public class BeepCommand extends RequestBase {
    private final CountdownTimer timer = new CountdownTimer(0, 500, 1, () -> DataMatrix.sendEvent(HALKeys.ACTION_IO_PLAY_BEEP));

    /**
     * Default constructor used to create the command.
     */
    @VisibleForTesting
    public BeepCommand() {
        super("beep", 1);
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param command           The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public BeepCommand(JSONObject command) throws JSONException {
        super(command);
        timer.setCount(command.optInt("count", 1));
    }

    /**
     * Start the beep timer and respond with an OK.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    @Override
    public void execute(JSONLocalSocket socket) {
        timer.start();
        sendGeneralResponse("ok", socket);
    }

    @Override
    public String toString() {
        return "BeepCommand: " + super.toString();
    }
}
