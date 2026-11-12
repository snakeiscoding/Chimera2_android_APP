package com.jht.chimera.lib.matrixcomm;

import android.net.LocalSocket;

import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * Manages the read thread and the socket connection.
 */
abstract public class LocalSocketWrapper implements Closeable {

    public interface IOnSocketClose {
        void onClose(LocalSocketWrapper socket);
    }
    private final LocalSocket socket;
    private boolean active = true;
    private final Thread readThread;


    /**
     * Create a socket and start a thread to read the data.
     *
     * @param socket            The socket to manage.
     * @param onClose           Callback when we close the socket.
     */
    public LocalSocketWrapper(@NonNull LocalSocket socket, @NonNull IOnSocketClose onClose) {
        this.socket = socket;

        readThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (active) {
                try {
                    int bytesRead = socket.getInputStream().read(buffer);
                    if (bytesRead < 0 || !active) {
                        break;
                    }
                    parse(buffer, bytesRead);

                } catch (IOException ignored) {
                    break;
                }
            }
            onClose.onClose(this);
        });
        readThread.start();
    }

    /**
     * Send data on the socket.
     *
     * @param data  The data to send.
     */
    public void sendData(byte[] data) {
        try {
            socket.getOutputStream().write(data);
        } catch (IOException ignored) {
            close();
        }
    }


    @Override
    public synchronized void close() {
        // Stop the read thread and wait for it to complete.
        active = false;
        try {
            socket.shutdownInput();
            socket.close();
            // Guard against self-join deadlock when close() is invoked from readThread's onClose callback
            if (Thread.currentThread() != readThread && readThread.isAlive()) {
                readThread.join();
            }
        } catch (IOException | InterruptedException ignored) {
        }
    }

    /**
     * Parse the incoming message.
     * @param message       The message
     * @param size          Size of the message.
     */
    abstract void parse(@NonNull byte[] message, int size);

}
