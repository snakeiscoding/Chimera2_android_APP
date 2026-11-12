package com.jht.chimera.service.matrixcomm.messages.events;

import android.os.Handler;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.rfid.Legic.RFIDChip;

/**
 * Monitor for estop changes and notify third parties on change.  Only send the event if the state
 * changes.
 */
public class RFIDMonitor {
    private boolean previousEStop;
    public RFIDMonitor(DataMatrix dataMatrix, Handler handler, JSONSocketServerBase server) {

        dataMatrix.createParcelableAdapter(HALKeys.EVENT_RFID_TAG_DETECTED_PARCELABLE, RFIDChip.class, chip ->
            server.send(new RFIDEvent(chip).getRequest()));
    }
}
