package com.jht.chimera.service.matrixcomm.messages.events;

import android.os.Handler;

import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Monitor for error code changes and notify third parties on change.  Only send the event if the state
 * changes?
 */
public class ErrorCodeEventMonitor {
    private int previousErrorCode;
    private int previousRepeated;
    public ErrorCodeEventMonitor(DataMatrix dataMatrix, Handler handler, JSONSocketServerBase server) {

        dataMatrix.createIntAdapter(HALKeys.DATA_LCB_ERROR_CODE_INT, errorCode -> handler.post(() -> {
            if(errorCode != previousErrorCode) {
                server.send(new ErrorCodeEvent(errorCode, 0).getRequest());
                previousErrorCode = errorCode;
                previousRepeated = 0;
            } else {
                server.send(new ErrorCodeEvent(errorCode, previousRepeated++).getRequest());
            }
        }), false);
    }
}
