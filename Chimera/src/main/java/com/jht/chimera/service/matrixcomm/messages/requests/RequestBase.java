package com.jht.chimera.service.matrixcomm.messages.requests;

import androidx.annotation.VisibleForTesting;

import com.jht.chimera.lib.matrixcomm.JSONLocalSocket;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Base class for all request / response messages.
 *
 * Each class should know how to convert to and from JSON.  Each command / request should know
 * how to execute the command and provide the appropriate response.  All responses should be able
 * to get any data they need from the system.
 */
public class RequestBase {
    private final String command;
    private final int version;
    private RequestBase request;
    private int transactionId;
    private boolean hasTransactionId = false;

    /**
     * This is typically used to create a response messge from a given request.  In general the
     * response messages echo the command name from the request and echo any transaction id.
     *
     * @param request   The request that is generating this response.
     */
    RequestBase(RequestBase request) {
        this.request = request;
        command = request.command;
        version = request.version;
        hasTransactionId = request.hasTransactionId;
        transactionId = request.transactionId;
    }

    /**
     * Default constructor to create a basic request.
     *
     * @param command   The command name.
     * @param version   The command version.
     */
    public RequestBase(String command, int version) {
        this.command = command;
        this.version = version;
    }

    /**
     * Parse the command we got from the JSON object.
     *
     * @param object            The command in JSON form.
     * @throws JSONException    If the JSON is missing required fields.
     */
    public RequestBase(JSONObject object) throws JSONException {
        command = object.getString("command");
        version = object.getInt("version");
        hasTransactionId = object.has("transactionId");
        if(hasTransactionId) {
            transactionId = object.getInt("transactionId");
        }
    }

    /**
     *
     * @return  The command / response in JSON form.
     */
    public JSONObject getRequest() {
        JSONObject object = new JSONObject();
        try {
            object.put("command", command);
            object.put("version", version);
            if(hasTransactionId) {
                object.put("transactionId", transactionId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    /**
     * Used to check that we didn't get extra fields.  Useful for testing.
     * @return  The number of fields we would expect.
     */
    @VisibleForTesting
    public int expectedFields() { return hasTransactionId ? 3 : 2; }

    /**
     *
     * @param other Other object to check against.
     * @return      True if the request input matches.
     */
    @Override
    public boolean equals(Object other) {
        if(other instanceof RequestBase) {
            RequestBase otherResponse = (RequestBase)other;
            return otherResponse.command.equals(command) &&
                    otherResponse.version == version;
        }
        return false;
    }

    /**
     * Return the request that generated this message.  Useful for testing.
     * @return  The request that generated the response (or expected request).
     */
    @VisibleForTesting
    public RequestBase request() {
        return request;
    }

    /**
     * @return  Readible string representation of the object.  Useful for testing.
     */
    @Override
    public String toString() {
        return "command " + command + ", vesion " + version;
    }

    /**
     * Execute the command.
     * @param socket    The socket that generated the command.  Used to send responses.
     */
    public void execute(JSONLocalSocket socket) {
        throw new UnsupportedOperationException();
    }

    /**
     * Send the command to the socket given.
     * @param socket    The socket to send the command / resposne to.
     */
    protected void send(JSONLocalSocket socket) {
        socket.sendData(getRequest());
    }

    /**
     * Helper function to send an error response.
     * @param message   The error message.
     * @param socket    Socket to send the response to.
     */
    public void sendErrorResponse(String message, JSONLocalSocket socket) {
        new ErrorResponse(this, message).send(socket);
    }

    /**
     * Helper function to send a general response.
     * @param message   The message to send.
     * @param socket    Socket to send the response to.
     */
    public void sendGeneralResponse(String message, JSONLocalSocket socket) {
        new GeneralResponse(this, message).send(socket);
    }

}
