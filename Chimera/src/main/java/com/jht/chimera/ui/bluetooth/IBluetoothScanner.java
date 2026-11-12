package com.jht.chimera.ui.bluetooth;

public interface IBluetoothScanner extends AutoCloseable {
    /**
     * The type of scanner that this is
     * @return The type of the scanner
     */
    BluetoothScannerType scannerType();

    /**
     *
     * @return  The name as a resource id that we will show for this scanner list.
     */
    int name();

    /**
     *
     * @return  The icon as a resource id that we will show for this scanner list.
     */
    int icon();

    /**
     * This operation must be asynchronous.  Use the scan result to pass back device notifications.
     *
     * @param scanResult  Callback to let the UI know the results.
     */
    void scan(IBluetoothScanResult scanResult);

    /**
     * Stop any current scan.  Do nothing if a scan is not happening.
     */
    void stopScan();

    /**
     * Clear all connected devices.
     */
    void clearDevices();

    @Override
    void close();


}
