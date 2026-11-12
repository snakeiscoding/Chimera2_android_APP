package com.jht.chimera.lib.matrixcomm;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a local server socket.  This allows us to have several connections at once.  This will
 * note the socket in callback for messages received so we can route responses to the correct socket.
 */
public class JSONSocketServerBase implements Closeable
{
    public static final String TAG = "MatrixCommServer";
    public static final boolean DEBUG = false;

    private final Thread listenThread;
    private volatile boolean active = true;
    private LocalServerSocket socket;
    private final List<JSONLocalSocket> localSockets = new ArrayList<>();

    /**
     * Start the server socket and watch for incoming connections.
     *
     * @param name              The socket name.
     * @param onJSONMessage     Callback when we get a JSON message.
     */
    public JSONSocketServerBase(@NonNull String name, @NonNull JSONLocalSocket.OnJSONMessage onJSONMessage) {
        try {
            socket = new LocalServerSocket(name);
            if (DEBUG) {
                Log.d(TAG, "[Lifecycle] Server socket successfully created on address: " + name);
            }
        } catch (IOException e) {
            if (DEBUG) {
                Log.e(TAG, "[Lifecycle] Failed to create LocalServerSocket on address: " + name, e);
            }
        }
        listenThread = new Thread(() -> {
            while(active) {
                try {
                    LocalSocket localSocket = socket.accept();
                    if(localSocket == null || !active) return;
                    JSONLocalSocket socket = new JSONLocalSocket(localSocket, onJSONMessage, (s) -> {
                        if(s instanceof JSONLocalSocket) {
                            s.close();
                            localSockets.remove((JSONLocalSocket)s);
                            if (DEBUG) {
                                Log.d(TAG, "[Connection] Client disconnected [Socket Hash: " + System.identityHashCode(s) + "]. Active connections remaining: " + localSockets.size());
                            }
                        }
                    });
                    localSockets.add(socket);
                    if (DEBUG) {
                        Log.d(TAG, "[Connection] New client connected [Socket Hash: " + System.identityHashCode(socket) + "]. Total active connections: " + localSockets.size());
                    }

                } catch (IOException e) {
                    if (DEBUG && active) {
                        Log.e(TAG, "[Lifecycle] IOException during socket accept loop", e);
                    }
                }
            }
        });
        listenThread.start();
    }

    @Override
    public synchronized void close() {
        if (DEBUG) {
            Log.d(TAG, "[Lifecycle] Closing JSONSocketServerBase with " + localSockets.size() + " active connection(s)");
        }
        for(JSONLocalSocket localSocket : localSockets) {
            localSocket.close();
        }
        // Stop the read thread and wait for it to complete.
        active = false;
        try {
            if (socket != null) {
                socket.close();
            }
            if (listenThread.isAlive()) {
                listenThread.join();
            }
        } catch (IOException | InterruptedException e) {
            if (DEBUG) {
                Log.e(TAG, "[Lifecycle] Exception during server socket close", e);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "[Lifecycle] JSONSocketServerBase closed successfully");
        }
    }

    /**
     * Send a message to all connections.
     * @param object    Message to send.
     */
    public synchronized void send(@NonNull JSONObject object) {
        if (DEBUG) {
            Log.d(TAG, "[Data Pass] [Broadcast] Broadcasting payload to " + localSockets.size() + " connection(s) -> " + object.toString());
        }
        for(JSONLocalSocket s : localSockets) {
            s.sendData(object);
        }
    }

}
