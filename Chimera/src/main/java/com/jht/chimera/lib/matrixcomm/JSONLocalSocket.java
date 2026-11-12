package com.jht.chimera.lib.matrixcomm;

import android.net.LocalSocket;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Extend the local socket for API's that pass JSON objects back and forth.
 */
public class JSONLocalSocket extends LocalSocketWrapper {

    private static final int MAX_MESSAGE_SIZE = 4096 * 2;
    private final OnJSONMessage onJSONMessage;
    private StringBuilder currentMessage = new StringBuilder();
    public interface OnJSONMessage {
        void onJSONMessage(JSONObject object, JSONLocalSocket connection);
    }

    /**
     * Start the socket and store the callbacks.
     * @param socket            The socket to manage.
     * @param onJSONMessage     Callback when we get a JSON message.
     * @param onClose           Callback when the socket is closed.
     */
    public JSONLocalSocket(@NonNull LocalSocket socket, @NonNull OnJSONMessage onJSONMessage, @NonNull IOnSocketClose onClose) {
        super(socket, onClose);
        this.onJSONMessage = onJSONMessage;

    }

    /**
     * Send the JSON data.
     *
     * @param data  Data to send.
     */
    public synchronized void sendData(JSONObject data) {
        if (JSONSocketServerBase.DEBUG) {
            Log.d(JSONSocketServerBase.TAG, "[Data Pass] [Socket Hash: " + System.identityHashCode(this) + "] Outbound payload sent -> " + data.toString());
        }
        sendData(data.toString().getBytes());
    }

    /**
     *
     * @param message       The message
     * @param size          Size of the message.
     */
    @Override
    public void parse(@NonNull byte[] message, int size) {
        // Append the data to the current buffer.
        currentMessage.append(new String(message, 0, size));

        while(currentMessage.length() > 0) {
            int startPosition = -1;
            int endPosition = -1;
            int depth = 0;

            // Find JSON.  Look for the opening and closing bracket.
            String messageToParse = currentMessage.toString();
            for (int i = 0; i < messageToParse.length(); i++) {
                if (startPosition == -1) {
                    if (messageToParse.charAt(i) == '{') {
                        depth++;
                        startPosition = i;
                    }
                } else {
                    if (messageToParse.charAt(i) == '{') {
                        depth++;
                    } else if (messageToParse.charAt(i) == '}') {
                        depth--;
                        if (depth == 0) {
                            endPosition = i;
                            break;
                        }
                    }
                }

            }
            if(endPosition != -1) {
                // Strip the JSON from the buffer.
                String jsonMessage = messageToParse.substring(startPosition, endPosition + 1);
                currentMessage = new StringBuilder();
                if (endPosition != messageToParse.length() - 1) {
                    currentMessage.append(messageToParse.substring(endPosition + 1));
                }
                // Parse the JSON.
                try {
                    if (JSONSocketServerBase.DEBUG) {
                        Log.d(JSONSocketServerBase.TAG, "[Data Pass] [Socket Hash: " + System.identityHashCode(this) + "] Inbound frame parsed -> " + jsonMessage);
                    }
                    onJSONMessage.onJSONMessage(new JSONObject(jsonMessage), this);
                } catch (JSONException ignored) {
                    // Drop this part of the message.
                    if (JSONSocketServerBase.DEBUG) {
                        Log.e(JSONSocketServerBase.TAG, "[Data Pass] [Socket Hash: " + System.identityHashCode(this) + "] Invalid JSON message received -> " + jsonMessage);
                    }
                }
            }
            else {
                // We did not find the end.  For sanity drop buffer if it exceeds the expected maximum message size.
                if(currentMessage.length() > MAX_MESSAGE_SIZE) {
                    currentMessage = new StringBuilder();
                }
                return;
            }
        }
    }

}
