package com.jht.chimera.ui.bluetooth;

import androidx.annotation.Nullable;

public interface IBluetoothDevice extends AutoCloseable {
    /**
     *
     * @return  The name to display on the selector widget.
     */
    String displayName();

    /**
     *
     * @return  The device MAC address so we avoid duplicates in the device list.
     */
    String macAddress();

    /**
     *
     * @return  True if we are fully connected to teh device.
     */
    boolean connected();

    /**
     *
     * @return  True if we are connecting to the device.
     */
    boolean connecting();

    /**
     * This operation must be done asynchronously.  It is expected to return immediately.  It should
     * return a pincode if the connection requires a pincode.  This way we can show this to the user.
     *
     * @param result  Used to pass back the connection result.
     * @return        The pincode if there is one.  Can be null or empty if there is not a pincode.
     */
    @Nullable
    String connect(IBluetoothConnectionResult result);

    /**
     * Cancel a current connection.  This should disconnect from the device.  Do nothing if we are
     * currently disconnected and not actively connecting.
     */
    void cancelConnection();

    /**
     * Disconnect from the device.
     */
    void disconnect(boolean sendAnalytics);

    @Override
    void close();

}
