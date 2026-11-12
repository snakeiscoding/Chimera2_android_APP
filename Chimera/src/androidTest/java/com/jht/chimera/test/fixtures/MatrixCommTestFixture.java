package com.jht.chimera.test.fixtures;

import static com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand.GOAL_TYPE.TIME;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.events.JHTDataNotification;
import com.jht.androidcommonalgorithms.util.ThreadHelper;
import com.jht.chimera.lib.common.datamatrixkeys.WorkoutKeys;
import com.jht.chimera.service.matrixcomm.messages.events.EStopEvent;
import com.jht.chimera.service.matrixcomm.messages.events.ErrorCodeEvent;
import com.jht.chimera.service.matrixcomm.messages.events.EventBase;
import com.jht.chimera.service.matrixcomm.messages.events.FullScreenEvent;
import com.jht.chimera.service.matrixcomm.messages.events.KeyEvent;
import com.jht.chimera.service.matrixcomm.messages.events.RFIDEvent;
import com.jht.chimera.service.matrixcomm.messages.events.WorkoutStateEvent;
import com.jht.chimera.service.matrixcomm.messages.requests.ControlStateResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.ErrorResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.GeneralResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.GetEquipmentInfoResponse;
import com.jht.chimera.service.matrixcomm.messages.requests.RequestBase;
import com.jht.chimera.service.matrixcomm.messages.requests.StartWorkoutCommand;
import com.jht.chimera.service.matrixcomm.messages.requests.WorkoutControlCommand;
import com.jht.datamatrixlib.DataMatrix;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * This test fixture is used to test the MatrixCommService that provides a third party API over
 * a local socket for apps like IFIT.
 */
public class MatrixCommTestFixture extends SystemTestFixture {

    // Used to communicate with the main software.
    private final LocalSocket socket = new LocalSocket();

    // The socket name.
    private static final String SOCKET_NAME = "com.jht.matrixcomm";

    // True if the read thread is active.  Use this to shut down the socket read thread.
    private volatile boolean active = true;

    // Used to notify the test of messages received from the main software.
    private final BlockingQueue<JSONObject> messageQueue = new LinkedBlockingQueue<>(10);

    // Read thread.  We want to join it at the end of a test to make sure it shuts down
    // properly.
    private final Thread readThread = new Thread( () -> {
        byte[] buffer = new byte[4096];
        StringBuilder dataRead = new StringBuilder();
        while (active) {
            try {
                int bytesRead = socket.getInputStream().read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                if (active && bytesRead > 0) {
                    dataRead.append(new String(buffer, 0, bytesRead));
                    messageQueue.offer(new JSONObject(dataRead.toString()));
                    dataRead = new StringBuilder();
                }

            } catch (IOException | JSONException ignored) {
            }
        }
    });


    public MatrixCommTestFixture() {
        super(true);

        try {
            socket.connect(new LocalSocketAddress(SOCKET_NAME));
            readThread.start();
        }
        catch (IOException exception) {
            Assert.fail("Unable to open the local socket " + exception);
        }
    }

    @Override
    public void close() {
        // Stop the read thread and wait for it to complete.
        active = false;
        try {
            socket.shutdownInput();
            socket.close();
            if(readThread.isAlive()) {
                readThread.join();
            }
            ThreadHelper.sleepIgnoreInterruption(1000);
        } catch (IOException | InterruptedException ignored) {
        }
    }

    /**
     * Here we are setting up the system to send an event and then making sure we get the correct
     * event.
     *
     * @param expectedEvent The event we expect.
     * @param event         Callback that performs actions to create the event.
     * @return              The event received.  The workout state event cannot be predicted so is
     *                      checked by the calling code.
     */
    public EventBase checkEvent(@NonNull EventBase expectedEvent, @NonNull Runnable event) {
        messageQueue.clear();
        event.run();
        JSONObject response = null;
        try {
            response = messageQueue.poll(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
        Assert.assertNotNull(response);
        Assert.assertEquals(expectedEvent.expectedFields(), response.length());
        try {
            EventBase retVal = parseEvent(response);
            Assert.assertEquals(expectedEvent, parseEvent(response));
            return retVal;
        } catch (JSONException exception) {
            Assert.fail("Unable to parse message " + response + " " + exception);
        }
        return null;
    }

    /**
     * Make a call and make sure we get the expected response.
     *
     * @param expectedResponse  The response expected from Chimera.
     */
    public void checkResponse(@NonNull RequestBase expectedResponse) {
        JSONObject response = null;
        try {
            messageQueue.clear();
            socket.getOutputStream().write(expectedResponse.request().getRequest().toString().getBytes());
            while(response == null) {
                try {
                    response = messageQueue.poll(2000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }
                Assert.assertNotNull(response);
                if(response.has("event")) {
                    response = null;
                }
            }
            Assert.assertEquals(response.toString(), expectedResponse.expectedFields(), response.length());
                Assert.assertEquals(expectedResponse, parseResponse(response));
        } catch (JSONException | IOException exception) {
            Assert.fail("Unable to parse message " + response + " " + exception);
        }

    }

    /**
     * We got a response.  Parse it.
     * @param response      The response in JSON format.
     * @return              The response as an object.
     * @throws JSONException    If the parse fails.  We use get calls to ensure the fields exist.
     */
    private RequestBase parseResponse(JSONObject response) throws JSONException {
        if(response.has("command")) {
            switch (response.getString("command")) {
                case "getequipmentinfo":
                    return new GetEquipmentInfoResponse(response);
                case "control":
                    if (response.has("errormessage")) {
                        return new ErrorResponse(response);
                    }
                    return new ControlStateResponse(response);
                default:
                    if (response.has("errormessage")) {
                        return new ErrorResponse(response);
                    }
                    return new GeneralResponse(response);
            }
        }
        return null;
    }

    /**
     * We got an event.  Parse it.
     * @param response      The event in JSON format.
     * @return              The event as an object.
     * @throws JSONException    If the parse fails.  We use get calls to ensure the fields exist.
     */
    private EventBase parseEvent(JSONObject response) throws JSONException {
        if(response.has("event")) {
            switch(response.getString("event")) {
                case "emergencystop":
                    return new EStopEvent(response);
                case "fullscreen":
                    return new FullScreenEvent(response);
                case "key":
                    return new KeyEvent(response);
                case "workoutstate":
                    return new WorkoutStateEvent(response);
                case "rfid":
                    return new RFIDEvent(response);
                case "error":
                    return new ErrorCodeEvent(response);
            }
        }
        return null;
    }

    public void checkPassiveWorkout(WorkoutControlCommand.ACTION action, boolean expectedResult) {
        JHTDataNotification<Boolean> passiveWorkoutEvent = new JHTDataNotification<>();
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createBoolAdapter(WorkoutKeys.ACTION_PASSIVE_WORKOUT_BOOL, passiveWorkoutEvent::setData);
        checkResponse(new GeneralResponse(new WorkoutControlCommand(action),  "ok"));
        Boolean passiveWorkout = passiveWorkoutEvent.waitForData(1000);
        dataMatrix.finish();
        Assert.assertNotNull(passiveWorkout);
        Assert.assertEquals(expectedResult, passiveWorkout);
    }
    /**
     * For many tests we need to start a workout.  This starts a normal 30 minute workout.
     */
    public void startNormalWorkout() {
        checkResponse(new GeneralResponse(new StartWorkoutCommand(TIME, 30 * 60, 0, 0, true), "ok"));
        ThreadHelper.sleepIgnoreInterruption(7000);
    }

    /**
     * After several commands we need to wait for the system to update itself (e.g., update current speed)
     */
    public void waitForSystem() {
        ThreadHelper.sleepIgnoreInterruption(200);
    }

}
