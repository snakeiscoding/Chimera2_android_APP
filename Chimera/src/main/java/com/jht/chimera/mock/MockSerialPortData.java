package com.jht.chimera.mock;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.serialport.TestTrace;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The MockSerialPortData implementation extracted from what is now <code>com.jht.serialport</code>.
 */
class MockSerialPortData {

    private final Object dataLock = new Object();
    private final Object drainLock = new Object();
    private byte[] dataBuffer;
    private int dataPos = 0;
    private int pendingWrites = 0;
    private BlockingQueue<SerialPortData> dataQueue = new LinkedBlockingQueue<>();
    private JHTNotification dataNotification = new JHTNotification();
    private volatile boolean isRunning = false;

    public MockSerialPortData(int bufferSize) {
        dataBuffer = new byte[bufferSize];
    }

    public void start()  {
        if(isRunning) {
            return;
        }
        isRunning = true;
        dataQueue.clear();
        synchronized (drainLock) {
            pendingWrites = 0;
        }
        new Thread(() -> {
            synchronized (dataLock) {
                dataPos = 0;
            }
            while(isRunning) {
                SerialPortData data = null;
                try {
                    data = dataQueue.take();
                } catch (InterruptedException ignored) {
                }
                if(!isRunning || data == null || data.data() == null) {
                    synchronized (dataLock) {
                        dataPos = 0;
                        dataNotification.setNotification();
                    }
                    synchronized (drainLock) {
                        pendingWrites = 0;
                        drainLock.notifyAll();
                    }
                    dataQueue.clear();
                    return;
                }

                synchronized (dataLock) {
                    TestTrace.instance().trace("Write data " + dataPos + ": " + Strings.bytesToHex(data.data(), " "));
                    for(int i = 0; i < data.data().length && dataPos < dataBuffer.length; i++) {
                        dataBuffer[dataPos++] = data.data()[i];
                    }
                    dataNotification.setNotification();
                }
                synchronized (drainLock) {
                    if (pendingWrites > 0) pendingWrites--;
                    drainLock.notifyAll();
                }
            }

            synchronized (dataLock) {
                dataPos = 0;
                dataNotification.setNotification();
            }
            synchronized (drainLock) {
                pendingWrites = 0;
                drainLock.notifyAll();
            }
            dataQueue.clear();
        }).start();
    }

    public void write(byte[] data, int offset, int size) {
        byte[] dataToAdd = new byte[size];
        System.arraycopy(data, offset, dataToAdd, 0, size);
        synchronized (drainLock) {
            pendingWrites++;
        }
        dataQueue.offer(new SerialPortData(dataToAdd));
    }

    public void stop() {
        isRunning = false;
        dataQueue.offer(new SerialPortData(null));
    }

    public int read(byte[] data, int offset, int size) {
        if(!isRunning) {
            return -1;
        }
        dataNotification.waitForNotification();
        synchronized (dataLock) {
            if(dataPos == 0 || !isRunning) {
                return -1;
            }
            int readSize = Math.min(dataPos, size);
            System.arraycopy(dataBuffer, 0, data, offset, readSize);
            byte[] readDebugData = new byte[readSize];
            System.arraycopy(data, 0, readDebugData, 0, readSize);
            TestTrace.instance().trace("Reading data " + dataPos + ", " + readSize + ": " + Strings.bytesToHex(readDebugData, " "));
            if(readSize < dataPos) {
                System.arraycopy(dataBuffer, readSize, dataBuffer, 0, dataPos - readSize);
            }
            dataPos -= readSize;
            return readSize;
        }
    }

    /**
     * Discards any pending writes still in the queue and any buffered data not yet read.
     * Matches POSIX <code>tcflush(fd, TCIFLUSH)</code> semantics.
     * <p>
     * Note: there is a narrow race where a write that has already been taken off the queue
     * by the producer thread but not yet appended to {@code dataBuffer} will land after this
     * call returns. In practice this does not occur in tests because flushes happen at quiet
     * points in the interaction.
     */
    public void clear() {
        synchronized (dataLock) {
            dataQueue.clear();
            dataPos = 0;
        }
        synchronized (drainLock) {
            pendingWrites = 0;
            drainLock.notifyAll();
        }
        TestTrace.instance().trace("Flushed buffer");
    }

    /**
     * Blocks until every byte offered via {@link #write(byte[], int, int)} has been appended
     * to the internal buffer (i.e. is "on the wire" and available to a reader).
     * Matches POSIX <code>tcdrain</code>: returns once the data has been transmitted, not
     * once the peer has consumed it. Returns early if the port stops while waiting.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting.
     */
    public void waitUntilDrained() throws InterruptedException {
        synchronized (drainLock) {
            while (isRunning && pendingWrites > 0) {
                drainLock.wait(50);
            }
        }
    }


    private static class SerialPortData {
        private byte[] data;
        SerialPortData(byte[] data) {
            this.data = data;
        }
        byte[] data() { return data; }
    }

}