package com.jht.chimera.service.matrixcomm.messages.events;

import android.os.Handler;

import com.jht.androidcommonalgorithms.util.NotificationThread;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.datamatrixlib.DataMatrix;

/**
 * Monitor for workout state changes and notify the third parties when the state changes.  Only
 * send when it changes.  We use a notification thread here to synchronize the calls so we
 * don't flood the communication channel or send duplicate states.
 *
 * For example, let's say we get 4 change notifications while we are in the middle of sending an
 * event.  In that case it the notification thread will be primed to run again after we are done
 * sending the event, but it will only run once afterwards.
 */
public class WorkoutStateEventMonitor {
    private final NotificationThread workoutStateThread = new NotificationThread();
    public WorkoutStateEventMonitor(DataMatrix dataMatrix, Handler handler, JSONSocketServerBase server) {
        workoutStateThread.start(() -> handler.post(() -> {
            // We want to avoid sending if time elapsed is 0 and we are stopped.  This happens when
            // we are about to start the workout.
            if(server != null && !(DataMatrix.getInt(WorkoutKeys.DATA_RUN_STATE_INT) == WorkoutKeys.Constants.RUN_STATE_STOP && DataMatrix.getInt(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT) == 0)) {
                server.send(new WorkoutStateEvent().getRequest());
            }
        }));
        dataMatrix.createIntChangeAdapter(WorkoutKeys.DATA_RUN_STATE_INT, (value) -> workoutStateThread.wakeup(), false);
        dataMatrix.createIntChangeAdapter(WorkoutKeys.DATA_TIME_ELAPSED_MS_INT, (value) -> workoutStateThread.wakeup(), false);
        dataMatrix.createIntChangeAdapter(UiServiceKeys.DATA_PAUSE_TIME_REMAINING_INT, (value) -> workoutStateThread.wakeup(), false);
        dataMatrix.createFloatChangeAdapter(HALKeys.DATA_TARGET_SPEED_KPH_FLOAT, (value) -> workoutStateThread.wakeup(), false);
        dataMatrix.createFloatChangeAdapter(HALKeys.DATA_TARGET_INCLINE_FLOAT, (value) -> workoutStateThread.wakeup(), false);
        dataMatrix.createIntChangeAdapter(HALKeys.DATA_RESISTANCE_INT, (value) -> workoutStateThread.wakeup(), false);
    }

    public void onDestroy() {
        workoutStateThread.stop();
    }
}
