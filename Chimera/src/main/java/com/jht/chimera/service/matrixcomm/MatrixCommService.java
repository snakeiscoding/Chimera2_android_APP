package com.jht.chimera.service.matrixcomm;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jht.chimera.lib.matrixcomm.JSONSocketServerBase;
import com.jht.chimera.service.matrixcomm.messages.events.EStopEventMonitor;
import com.jht.chimera.service.matrixcomm.messages.events.ErrorCodeEventMonitor;
import com.jht.chimera.service.matrixcomm.messages.events.FullScreenEventMonitor;
import com.jht.chimera.service.matrixcomm.messages.events.KeyEventMonitor;
import com.jht.chimera.service.matrixcomm.messages.events.RFIDMonitor;
import com.jht.chimera.service.matrixcomm.messages.events.WorkoutStateEventMonitor;
import com.jht.chimera.service.matrixcomm.messages.requests.BeepCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.ControlStateCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.ErrorResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.FanCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.FeatureCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.GetEStopStateRequest;
import com.jht.chimera.service.matrixcomm.messages.requests.GetEquipmentInfoRequest;
import com.jht.chimera.service.matrixcomm.messages.requests.RFIDCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.RequestBase;
import com.jht.chimera.service.matrixcomm.messages.requests.SetUserInfoCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.SetWorkoutParameterCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.UpdateGoalCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.WorkoutControlCommand;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.util.ServiceHelper;

import org.jetbrains.annotations.NonNls;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Implements the 'Matrix' side of the protocol documented in
 * https://jhtnaee.atlassian.net/wiki/spaces/CT/pages/986611931/Cardio+Communication+Specification
 */
public class MatrixCommService extends Service {

    @NonNls final String SOCKET_NAME = "com.jht.matrixcomm";
    private JSONSocketServerBase server;
    private final DataMatrix dataMatrix = new DataMatrix();
    private Handler handler;
    private WorkoutStateEventMonitor workoutStateEventMonitor = null;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        ServiceHelper.startForegroundService(this);

        server = new JSONSocketServerBase(SOCKET_NAME, (object, connection) -> handler.post(() -> {
            if (object.has("command") && object.has("version")) {
                try{
                    getCommand(object).execute(connection);
                } catch (JSONException ignored) {
                }
            }
        }));

        new EStopEventMonitor(dataMatrix, handler, server);
        new FullScreenEventMonitor(dataMatrix, handler, server);
        new KeyEventMonitor(dataMatrix, handler, server);
        new RFIDMonitor(dataMatrix, handler, server);
        workoutStateEventMonitor = new WorkoutStateEventMonitor(dataMatrix, handler, server);
        new ErrorCodeEventMonitor(dataMatrix, handler, server);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(server != null) {
            server.close();
            server = null;
        }
        dataMatrix.finish();
        if(workoutStateEventMonitor != null) {
            workoutStateEventMonitor.onDestroy();
            workoutStateEventMonitor = null;
        }
    }

    @NonNull
    private RequestBase getCommand(JSONObject command) throws JSONException {
        String commandName = command.optString("command", "");
        int version = command.optInt("version", 0);
        if (version == 1) {
            switch (commandName) {
                case "beep":
                    return new BeepCommand(command);
                case "control":
                    return new ControlStateCommand(command);
                case "getestopstate":
                    return new GetEStopStateRequest(command);
                case "feature":
                    return new FeatureCommand(command);
                case "getequipmentinfo":
                    return new GetEquipmentInfoRequest(command);
                case "setuserinfo":
                    return new SetUserInfoCommand(command);
                case "setworkoutparameter":
                    return new SetWorkoutParameterCommand(command);
                case "startworkout":
                    return new StartWorkoutCommand(command);
                case "updategoal":
                    return new UpdateGoalCommand(command);
                case "workoutcontrol":
                    return new WorkoutControlCommand(command);
                case "rfidscan":
                    return new RFIDCommand(command);
                case "fan":
                    return new FanCommand(command);
                default:
                    break;
            }
        }
        return new ErrorResponse(new RequestBase(command), "Unrecognized command");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
