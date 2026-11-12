package com.jht.chimera.ui.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.jht.chimera.lib.common.context.ContextUtil
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys
import com.jht.datamatrixlib.DataMatrix

/**
 * There is currently an OS modification that will not show the dialog box that will asks
 * if the users will allow a bluetooth device to connect. The current behavior is to always
 * accept. This is causing an issue where some Bluetooth headphone/speaker will attempt
 * to connect to the last know device and can connect to the console.
 *
 * This manager will keep track of the known list of headphone/speaker that are allowed to connect.
 * Note that there are currently 2 places that handles the connection of headphone/speaker:
 * 1. The Bluetooth Pairing Activity
 * 2. The BluetoothBackgroundPairing used by the DAPI service to try to connection previously paired
 *    devices for a user
 */
@SuppressLint("StaticFieldLeak")
object HeadphoneDeviceManager {
    private const val TAG = "HeadphoneDeviceManager"

    private val context = ContextUtil.getApplicationContext()
    private val devices: MutableSet<String> = mutableSetOf()
    private val dataMatrix = DataMatrix()
    private var isStarted = false

    private var a2dp: BluetoothA2dp? = null
    private var headset: BluetoothHeadset? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dp = proxy as? BluetoothA2dp
                BluetoothProfile.HEADSET -> headset = proxy as? BluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dp = null
                BluetoothProfile.HEADSET -> headset = null
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleConnectionStateChanged(intent)
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        // listen to bluetooth connection events
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        context?.registerReceiver(receiver, filter)

        // get the a2dp and headset proxy so we can call disconnect
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
        bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)

        // DataMatrix events to update the device list
        dataMatrix.createStringAdapter(HALKeys.ACTION_ADD_BT_HEADPHONE_STRING) { addDevice(it) }
        dataMatrix.createStringAdapter(HALKeys.ACTION_REMOVE_BT_HEADPHONE_STRING) { removeDevice(it) }
        dataMatrix.createEventAdapter(HALKeys.ACTION_SPLASH_RESET) { devices.clear() }
    }

    fun stop() {
        if (!isStarted) return

        runCatching {
            context?.unregisterReceiver(receiver)
            a2dp?.let { proxy -> bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy) }
            headset?.let { proxy -> bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, proxy) }
        }.onFailure {
            Log.d(TAG, "Failed to unregister", it)
        }
        dataMatrix.finish()
        isStarted = false
    }

    fun addDevice(mac: String) {
        devices.add(normalizeMac(mac))
    }

    fun removeDevice(mac: String) {
        devices.remove(normalizeMac(mac))
    }

    private fun handleConnectionStateChanged(intent: Intent) {
        val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) ?: return
        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
        val mac = normalizeMac(device.address)

        // disconnect the device if it's not on the list
        if (state == BluetoothProfile.STATE_CONNECTED || state == BluetoothProfile.STATE_CONNECTING) {
            if (!devices.contains(mac)) {
                Log.d(TAG, "Unauthorized device connected: $mac. Disconnecting and removing bond.")
                disconnectAndUnbond(device, intent.action)
            }
        }
    }

    private fun disconnectAndUnbond(device: BluetoothDevice, action: String?) {
        try {
            when (action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val disconnectMethod = a2dp?.javaClass?.getMethod("disconnect", BluetoothDevice::class.java)
                    disconnectMethod?.invoke(a2dp, device)
                    Log.d(TAG, "A2DP disconnect invoked for ${device.address}")
                }

                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val disconnectMethod = headset?.javaClass?.getMethod("disconnect", BluetoothDevice::class.java)
                    disconnectMethod?.invoke(headset, device)
                    Log.d(TAG, "Headset disconnect invoked for ${device.address}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect device ${device.address} for action=$action", e)
        } finally {
            removeBond(device)
        }
    }

    @SuppressLint("MissingPermission")
    private fun removeBond(device: BluetoothDevice) {
        try {
            if (device.bondState != BluetoothDevice.BOND_NONE) {
                device.javaClass.getMethod("removeBond")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove bond for ${device.address}", e)
        }
    }

    private fun normalizeMac(mac: String): String {
        return mac.trim().uppercase()
    }

}