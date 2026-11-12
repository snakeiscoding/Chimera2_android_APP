package com.jht.chimera.service.matrixcomm.messages.events;

import android.os.Handler;

import com.jht.chimera.lib.common.config.ChimeraEnums;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Monitor for estop changes and notify third parties on change.  Only send the event if the state
 * changes.
 */
public class EStopEventMonitor {
    private boolean previousEStop;
    public EStopEventMonitor(DataMatrix dataMatrix, Handler handler, JSONSocketServerBase server) {

        previousEStop = isAnySafetyKeyTriggered();
        dataMatrix.createIntAdapter(HALKeys.DATA_SAFETY_KEY_STATUS_INT, skStatus -> handler.post(() -> {
            boolean currentEStop = isAnySafetyKeyTriggered();
            if(currentEStop != previousEStop) {
                server.send(new EStopEvent(currentEStop).getRequest());
                previousEStop = currentEStop;
            }
        }), false);
    }

    public static boolean isAnySafetyKeyTriggered() {
        return !DataMatrix.getBoolean(SettingKeys.Debug.DATA_IS_DEMO_MODE_BOOL) &&
                ChimeraEnums.isAnySafetyErrorTriggered(DataMatrix.getInt(HALKeys.DATA_SAFETY_KEY_STATUS_INT));
    }

}
