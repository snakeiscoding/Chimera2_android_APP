package com.jht.chimera.mock;

import androidx.annotation.NonNull;

import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.ISerialPort;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * The MockSerialPort implementation extracted from what is now <code>com.jht.serialport</code>.
 */
public class MockSerialPort implements ISerialPort {

    private volatile boolean isOpen = false;

    private final MockSerialPortData hostData;
    private MockSerialPortData peripheralData;

    private final MockSerialInputStream hostDataInputStream;
    private MockSerialInputStream peripheralDataInputStream;
    private final OutputStream hostDataOutputStream;
    private MockSerialPortOutputStream peripheralDataOutputStream;
    private DataMatrix dm;
    private Object inputKey;
    private boolean breakState;

    @SuppressWarnings("unused")
    public MockSerialPort() {
        this(4096);
    }

    public MockSerialPort(int bufferSize) {
        hostData = new MockSerialPortData(bufferSize);
        peripheralData = new MockSerialPortData(bufferSize);
        hostDataInputStream = new MockSerialInputStream(hostData);
        peripheralDataInputStream = new MockSerialInputStream(peripheralData);
        hostDataOutputStream = new MockSerialPortOutputStream(peripheralData);
        peripheralDataOutputStream = new MockSerialPortOutputStream(hostData);

    }

    @SuppressWarnings("unused")
    public MockSerialPort(Object inputKey, Object outputKey) {
        this(inputKey, outputKey, 4096);
    }

    public MockSerialPort(Object inputKey, Object outputKey, int bufferSize) {
        this.inputKey = inputKey;
        breakState = false;
        isOpen = false;
        hostData = new MockSerialPortData(bufferSize);

        dm = new DataMatrix();

        hostDataInputStream = new MockSerialInputStream(hostData);
        hostDataOutputStream = new MockSerialPortOutputStreamDM(outputKey);
    }


    @Override
    public void open() throws SecurityException {
        if(!isOpen) {
            isOpen = true;
            hostData.start();
            if(peripheralData != null) {
                peripheralData.start();
            }
            if(dm != null) {
                dm.createByteArrayAdapter(inputKey, dataRead -> hostData.write(dataRead, 0, dataRead.length), false);
            }
        }
    }
    @Override
    public boolean isOpen() { return isOpen; }


    @Override
    public void close() {
        hostData.stop();
        if(peripheralData != null) {
            peripheralData.stop();
        }
        isOpen = false;
        if(dm != null) {
            dm.finish();
        }
    }

    @Override
    public int serialBreak(boolean enable) {
        breakState = enable; // just track in case we need to know later
        return 0;
    }

    @Override
    public InputStream getInputStream() {
        return hostDataInputStream;
    }

    /**
     * Discards any data that has been received but not yet read.
     * Matches POSIX <code>tcflush(fd, TCIFLUSH)</code>.
     *
     * @return 0 on success, -1 if the port is not open.
     */
    @Override
    public int flushInput() {
        if (!isOpen) {
            return -1;
        }
        hostData.clear();
        return 0;
    }

    @Override
    public OutputStream getOutputStream() {
        return hostDataOutputStream;
    }

    /**
     * Blocks until all data written to the output stream has been transmitted (i.e. moved
     * from the internal queue into the peripheral's input buffer). Matches POSIX
     * <code>tcdrain</code>.
     * <p>
     * In the DataMatrix-backed constructor, {@code peripheralData} is null and writes
     * are synchronous via {@code DataMatrix.putBytes}, so this is effectively a no-op.
     *
     * @return 0 on success, -1 if the port is not open or the calling thread was interrupted.
     */
    @Override
    public int drainOutput() {
        if (!isOpen) {
            return -1;
        }
        if (peripheralData != null) {
            try {
                peripheralData.waitUntilDrained();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        // DM path: DataMatrix.putBytes is synchronous; nothing to wait on.
        return 0;
    }

    public InputStream getPeripheralInputStream() {
        return peripheralDataInputStream;
    }

    public OutputStream getPeripheralOutputStream() {
        return peripheralDataOutputStream;
    }

    @Override
    public void setTraceDebug(boolean debug) {
        // Do nothing.  This is for mocking so we always log stuff.
    }


    private static class MockSerialInputStream extends InputStream {

        private final MockSerialPortData dataStream;
        MockSerialInputStream( MockSerialPortData dataStream) {
            this.dataStream = dataStream;
        }

        /**
         * Read in a byte of data.
         *
         * @return  The byte read.
         */
        @Override
        public int read() {

            // The buffer will contain the byte.
            byte[] buffer = new byte[1];
            int bytesRead = dataStream.read(buffer, 0, 1);
            if(bytesRead <= 0) {
                return -1;
            }
            return buffer[0];
        }

        /**
         * Used to read in several bytes in the buffer.  Note we can read up to the length of the buffer,
         * but we return once we've read some bytes.
         *
         * @param buffer    The buffer to place the bytes.
         * @return          The number of bytes read or -1 on error.
         */
        @Override
        public int read(@NonNull byte[] buffer) {
            return dataStream.read(buffer, 0, buffer.length);
        }

        /**
         * Reads in up to len bytes into the buffer at the specified offset.
         *
         *
         * @param buffer    The buffer to place the data.
         * @param off       The starting offset to place the data.
         * @param len       The maximum number of bytes to read.
         * @return          Bytes read or -1 on error.
         */
        @Override
        public int read(@NonNull byte[] buffer, int off, int len) {
            return dataStream.read(buffer, off, len);

        }
    }

    private static class MockSerialPortOutputStream extends OutputStream {

        private final MockSerialPortData dataStream;
        MockSerialPortOutputStream( MockSerialPortData dataStream) {
            this.dataStream = dataStream;
        }

        /**
         * Write a byte of data.
         *
         */
        @Override
        public void write(int data) {

            dataStream.write(new byte[]{(byte)data}, 0, 1);
        }

        /**
         * Write data to the stream.
         *
         * @param buffer    The buffer with data to write.
         */
        @Override
        public void write(@NonNull byte[] buffer) {
            dataStream.write(buffer, 0, buffer.length);
        }

        /**
         * Write data to the stream.
         *
         *
         * @param buffer    The buffer with data to write.
         * @param off       The starting offset where the data is.
         * @param len       The number of bytes to write.
         */
        @Override
        public void write(@NonNull byte[] buffer, int off, int len) {
            dataStream.write(buffer, off, len);

        }
    }
    private static class MockSerialPortOutputStreamDM extends OutputStream {

        private Object outputKey;
        MockSerialPortOutputStreamDM( Object outputKey) {
            this.outputKey = outputKey;
        }


        public void write(int data) {
            DataMatrix.putBytes(outputKey,new byte[]{(byte)data});
        }

        public void write(@NonNull byte[] buffer) {
            DataMatrix.putBytes(outputKey, buffer);
        }

        public void write(@NonNull byte[] buffer, int off, int len) {
            byte[] data = new byte[len];
            System.arraycopy(buffer, off, data, 0, len);
            DataMatrix.putBytes(outputKey, data);
        }
    }


}