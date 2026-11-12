package com.jht.chimera.service.matrixcomm.messages.events;

import android.os.Handler;

import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Monitor for full screen event changes.  Only send the event if the state changes.
 */
public class FullScreenEventMonitor {
    private boolean previousFullScreen;
    public FullScreenEventMonitor(DataMatrix dataMatrix, Handler handler, JSONSocketServerBase server) {

        previousFullScreen = DataMatrix.getInt(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT) == UiServiceKeys.WorkoutPanelState.FULLSCREEN;
        dataMatrix.createIntChangeAdapter(UiServiceKeys.DATA_WORKOUT_PANEL_STATE_INT, value -> handler.post(() -> {
            boolean fullScreen = value == UiServiceKeys.WorkoutPanelState.FULLSCREEN;
            if(fullScreen != previousFullScreen) {
                server.send(new FullScreenEvent(fullScreen ? "true" : "false").getRequest());
                previousFullScreen = fullScreen;
            }
        }), false);


    }
}
