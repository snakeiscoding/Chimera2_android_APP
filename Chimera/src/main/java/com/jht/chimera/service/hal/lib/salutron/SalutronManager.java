package com.jht.chimera.service.hal.lib.salutron;

import android.net.Uri;
import android.util.Log;

import com.jht.androidcommonalgorithms.events.JHTNotification;
import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.service.hal.lib.salutron.salutronCommands.SalutronCommand;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.serialport.SerialPort;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.LinkedBlockingQueue;

public class SalutronManager extends SalutronInterface {

    private static final String TAG = "SalutronManager";
    private FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
    private DataMatrix dataMatrix = new DataMatrix();

    public final static SalutronManager Instance = new SalutronManager();

    private volatile boolean _isPortOpened = false;
    private volatile int _txRetryCounter = 0;

    private static InputStream _inStream = null;
    private static OutputStream _outStream = null;

    private static SerialPort _port = null;
    private static Thread _rxThread = null;
    private static Thread _txThread = null;

    public IHeartRateCallback heartRateCallback;

    private static LinkedBlockingQueue<byte[]> _txCommandQueue = new LinkedBlockingQueue<>();
    private static LinkedBlockingQueue<byte[]> _txBroadcastQueue = new LinkedBlockingQueue<>();
    private static LinkedBlockingQueue<byte[]> _txPacketQueue = new LinkedBlockingQueue<>();

    private static final Object notifyRxPacketSync = new Object();
    private static final Object notifyResetSync = new Object();
    private static final Object notifyDisconnectedSync = new Object();
    private static final Object notifyConnectedSync = new Object();

    public static final ArrayList<HeartRateInfo> DeviceArray = new ArrayList<>();

    private boolean isScanning = false;
    private boolean isWorkout = false;
    private boolean salutronUpdate;

    private int defaultANTid;
    private int mHeartRateSalutron = 0;

    private SalutronDevice.SalutronDeviceParcelable connectDevice;

    public static heartRatePacket packetManager;
    private File _firmware;

    private salutronCommands.DeviceType connectType;

    public enum Control_Command {
        START_WORKOUT,
        STOP_WORKOUT,
        RESTART,
        SCANNING,
        GET_STAT,
        GET_VER,
        GET_MAC,
        RESET,
        START_UPDATE
    }

    public class HeartRateInfo {
        public byte[] id;
        public String name;
        public salutronCommands.DeviceType type;

        public HeartRateInfo(String name, byte[] id, salutronCommands.DeviceType type){
            this.name = name;
            this.id = id;
            this.type = type;
        }
    }

    @Override
    public void registerOnReceiveListener(IHeartRateCallback callback) {

        heartRateCallback = callback;
    }

    @Override
    public void unregisterOnReceiveListener(IHeartRateCallback callback) {
        heartRateCallback = null;
    }

    @Override
    public boolean start(SerialPort device) {

        packetManager = new heartRatePacket();

        if (_isPortOpened) {
            Log.w(TAG, "The serial port had already opened!");
            return true;
        }

        if (device != null) {
            _port = device;
            try {
                _port.open();

            } catch (Exception e) {
//                Log.e(TAG,"Failed to open serial port: " + path);
                Log.w(TAG, e);
                return false;
            }

            _inStream = _port.getInputStream();
            _outStream = _port.getOutputStream();
            if (_inStream == null || _outStream == null) {
                return false;
            }
            _txPacketQueue.clear();
            _txCommandQueue.clear();
            _txBroadcastQueue.clear();
            DeviceArray.clear();

            _rxThread = new RxThread();
            _txThread = new TxThread();
            _txThread.start();
            _rxThread.start();

            _isPortOpened = true;

        } else {
            Log.e(TAG, "serialPort instance is null!");
            return false;
        }

        return _isPortOpened;
    }

    @Override
    public void stop() {
        if (_isPortOpened) {
            if (_rxThread!=null) _rxThread.interrupt();
            if (_txThread!=null) _txThread.interrupt();

            if (_port != null) {
                _port.close();
//                _port = null;
                _inStream = null;
                _outStream = null;
            }
            _txPacketQueue.clear();
            _txCommandQueue.clear();
            _txBroadcastQueue.clear();
            _rxThread = null;
            _txThread = null;
            _isPortOpened = false;
        }

    }

    private class RxThread extends Thread {

        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                try {
                    int length;
                    byte[] buffer;
                    byte byteData;


                    byteData = (byte) _inStream.read();
//                        Log.d(TAG,"byteData: " + String.format("%X",byteData));

                    if (salutronUpdate) {
                        if(byteData == (byte) 0xC0){
                            salutronUpdate = false;
//                            byte[] updateBuffer = new byte[6];
//                            updateBuffer[0] = byteData;
//                            String bufferDate = String.format("%X", updateBuffer[0]) + " ";
//                            for (int i = 1; i < 6; i++){
//                                updateBuffer[i] = (byte)_inStream.read();
//                                bufferDate += String.format("%X", updateBuffer[i]) + " ";
//                            }
//                            Log.d(TAG, "[salutron]receive update reply:  " + bufferDate);
                            SalutronManager.this.startUpdate();

                        } else {
                            _txCommandQueue.add(packetManager.buildTxPacket((byte)0x00,SalutronCommand.ENABLE_SERIAL_UPDATE));
                        }
                    } else {

                        if (byteData == (byte) 0x0A) {
                            length = (byte) _inStream.read();
                            buffer = new byte[length - 2];

                            for (int i = 0; i < length - 2; i++) {
                                buffer[i] = (byte) _inStream.read();
                            }

//                        String bufferDate = "";
//                        for(int i = 0; i < buffer.length; i++) {
//                            bufferDate += String.format("%X", buffer[i]) + " ";
//                        }
//                        Log.d(TAG,"receive Packet Data: " + bufferDate);
                            receiveDataHandler(buffer);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "UART read I/O exception", e);
                    break;
                }
//                }
            }
        }
    }


    private void receiveDataHandler(byte[] packet) {

        packetManager.parse(packet, packet.length);
    }

    private class TxThread extends Thread {

        @Override
        public void run() {
            while (!this.isInterrupted()) {

                try {

                    if (!_txCommandQueue.isEmpty() || !_txBroadcastQueue.isEmpty()) {
                        if(_txPacketQueue.isEmpty()) {
                            if (!_txCommandQueue.isEmpty()){
                                _txPacketQueue.put(_txCommandQueue.take());
                            }
                            else {
                                _txPacketQueue.put(_txBroadcastQueue.take());
                            }
                            _txRetryCounter = 0;
                        }
                    }

                    if (!_txPacketQueue.isEmpty()) {
                        if (_txRetryCounter <= 3) {
                            _txRetryCounter++;
                            int responseWaitMs = txHandler(_txPacketQueue.peek());
                            synchronized (notifyRxPacketSync) {
                                notifyRxPacketSync.wait(responseWaitMs);
                            }
                        } else {
                            Log.e(TAG, "Salutron Device no response");
                            _txPacketQueue.poll();
                            _txRetryCounter = 0;
                        }
                    }

                    this.sleep(100);

                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
        }
    }

    private int txHandler(byte[] command) {
        int responseWaitMs = 2000; // default timeout was 2 sec

        /*String data = "";
        for (int i = 0; i < command.length; i++) {
            data += String.format("%X", command[i]) + " ";
        }
        Log.d(TAG,"txHandler write command: " + data);*/

        try {
            if (command[4] == (byte) 0x00) {
                _txPacketQueue.poll();
                _txRetryCounter = 0;
                responseWaitMs = 50; // this command was set to timeout in 50ms
            }
            _outStream.write(command);

        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return responseWaitMs;
    }

    public void setANTId(int id){
        if(id == 0) {
            id = defaultANTid;
            dataMatrix.putInt(SettingKeys.Hardware.DATA_ANT_PLUS_DEVICE_ID_INT,defaultANTid);
        }
        byte[] idArray = ByteBuffer.allocate(4).putInt(id).array();
        Log.d(TAG, "setANTId: " + Arrays.toString(idArray));
        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ANT_ID, idArray[3], idArray[2]));
    }

    public void setBLEName(String name){
        try {
            byte[] nameArray = name.getBytes("UTF-8");
            if(nameArray.length == 0) {
                nameArray = "HRM8700".getBytes("UTF-8"); //NON-NLS
                //dataMatrix.putString(SettingKeys.Hardware.DATA_SALUTRON_BLE_NAME_STRING,"");
            }
            _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.BLE_NAME,nameArray));
            Log.d(TAG, "setBLEName: " + Arrays.toString(nameArray) + " --> " + new String(nameArray,"UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, e);
        }
    }

    public void controlDevice(Control_Command command) {

        if (!_isPortOpened) return;
        Log.d(TAG, "controlDevice: " + command.toString());

        switch (command) {
            case START_WORKOUT:
//                controlDevice(Control_Command.RESET);
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.CONTACT_RR_DISABLE));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.CONTACT_OVERRIDE_OFF));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ANT_RR_DISABLE));

                switch (frameConfig.machineClassType) {
                    case Treadmill:
                        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ENABLE_FE_PROFILE, SalutronCommand.ENABLE_FE_PROFILE.getSubData(), (byte) 0x01));
                        break;
                    case Elliptical:
                    case RecumbentElliptical:
                    case Ascent:
                    case Stepper:
                        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ENABLE_FE_PROFILE, SalutronCommand.ENABLE_FE_PROFILE.getSubData(), (byte) 0x02));
                        break;
                    case Climbmill:
                        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ENABLE_FE_PROFILE, SalutronCommand.ENABLE_FE_PROFILE.getSubData(), (byte) 0x04));
                        break;
                    case Bike:
                        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ENABLE_FE_PROFILE, SalutronCommand.ENABLE_FE_PROFILE.getSubData(), (byte) 0x00));
                        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ENABLE_PWR_PROFILE, SalutronCommand.ENABLE_PWR_PROFILE.getSubData(), (byte) 0x00));
                        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.ENABLE_ANT_BSC_PROFILE));

                        break;
                }
                setANTId(DataMatrix.getInt(SettingKeys.Hardware.DATA_ANT_PLUS_DEVICE_ID_INT));
                setBLEName("HRM8700"); //NON-NLS
                setStationaryBikePageId(DataMatrix.getInt(SettingKeys.Hardware.DATA_ANT_PLUS_PAGE_INT));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.FE_TRANSMISSION_PATTERN, (byte) 0x07));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.BLE_ENABLE_FULLNAME));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.BLE_RR_DISABLE));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.AUTO_CONNECT));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.START, (byte)0x03,(byte)0x14));
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.GET_ANT_STATUS,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF));

                DeviceArray.clear();
                isWorkout = true;
                heartRateCallback.isConnected(false,null);
                break;
            case STOP_WORKOUT:
                isWorkout = false;
                heartRateCallback.isConnected(false,null);
                isScanning = false;
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.STOP));
                heartRateCallback.onHeartRate(0,0);
//                mHeartRateSalutron = 0;
                DeviceArray.clear();
                break;
            case RESTART:
                _txCommandQueue.clear();
                controlDevice(Control_Command.STOP_WORKOUT);
                controlDevice(Control_Command.START_WORKOUT);
                break;
            case SCANNING:
                if(!isScanning) {
                    DeviceArray.clear();
                    _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.SCANNING,(byte)0x01,(byte)0x14));
                    isScanning = true;
                }
                break;
            case GET_STAT:
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.GET_STATUS));
                break;
            case GET_VER:
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.GET_VER));
            case RESET:
                _txCommandQueue.add(packetManager.buildTxPacket((byte)0x00,SalutronCommand.RESET));
                try {
                    synchronized (notifyResetSync) {
                        notifyResetSync.wait(5000);
                    }
                } catch (InterruptedException e) {
                    Log.w(TAG, e);
                }
                break;
            case GET_MAC:
                _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.GET_MAC_ADDRESS));
                break;
            case START_UPDATE:
                salutronUpdate = true;
                controlDevice(Control_Command.STOP_WORKOUT);
                _txCommandQueue.add(packetManager.buildTxPacket((byte)0x00,SalutronCommand.ENABLE_SERIAL_UPDATE));
                UpdateManager.getInstance(ContextUtil.getApplicationContext()).broadcastUpdateStatus(100, 0f);
                break;
        }
        updateWorkoutState();

//        Log.d(TAG,"TX Queue size after put: " + _txCommandQueue.size() + " tx Thread is alive: " + _txThread.isAlive());

    }

    private void startUpdate() {
        stop();
        new JHTTimer(2000,() -> {
            UpdateManager.getInstance(ContextUtil.getApplicationContext()).startUpdate(_port, Uri.fromFile(_firmware));
        }).start();
    }

    public void setStationaryBikePageId(int page){
        try {
            Log.d(TAG, "set stationary bike page " + page);
            if(_txCommandQueue != null && SalutronManager.packetManager != null) {
                if (page == 0)
                    _txCommandQueue.add(SalutronManager.packetManager.buildTxPacket(salutronCommands.SalutronCommand.FEC_STATIONARY_BIKE_PAGE_21));
                else if (page == 1)
                    _txCommandQueue.add(SalutronManager.packetManager.buildTxPacket(salutronCommands.SalutronCommand.FEC_STATIONARY_BIKE_PAGE_25));
            }
        } catch(Exception ignored) {}
    }

    public void updateWorkoutState(){
        heartRateCallback.onScanning(isScanning);
        heartRateCallback.onWorkout(isWorkout);
    }

    private ByteBuffer antBuffer = ByteBuffer.allocate(4096);
    public void broadcastANT(boolean isPage21) {
        antBuffer.clear();
        if (packetManager != null) {
            //antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_16));
            //antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_17));
            //antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_18));
            //antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_80));
            //antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.PWR_PAGE_16));

            /*
            _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_16));
            _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_17));
            _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_18));
            _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_80));
            _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.PWR_PAGE_16));
             */

            switch (frameConfig.machineClassType) {
                case Treadmill:
                    antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_19));
                    // _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_19));
                    break;
                case Elliptical:
                case RecumbentElliptical:
                case Ascent:
                case Stepper:
                    antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_20));
                    // _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_20));
                    break;
                case Climbmill:
                    antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_23));
                    // _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_23));
                    break;
                case Bike:
                    if(isPage21) {
                        Log.d(TAG, "Sending page 21");
                        antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_21,true));
                        // _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_21,true));
                    }
                    else {
                        Log.d(TAG, "Sending page 25");
                        antBuffer.put(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_25,false));
                        // _txBroadcastQueue.add(packetManager.buildANTProfilePacket(salutronCommands.ProfilePage.FE_PAGE_25,false));
                    }
                    break;
            }
            //Log.d("bdc_ttyS6", "Adding tranfer queue " + antBuffer.position());
            _txBroadcastQueue.add(Arrays.copyOf(antBuffer.array(), antBuffer.position()));
        }
    }

    public void resetFTMSconfig() {
        _txBroadcastQueue.add(packetManager.buildFTMSPacket(salutronCommands.FTMS.SET_FTMS_CONFIG));
    }

    public void broadcastFTMS() {
        switch (frameConfig.machineClassType) {
            case Treadmill:
                SalutronManager.Instance.broadcastFTMSTreadMill();
                break;
            case Bike:
                SalutronManager.Instance.broadcastFTMSIndoorBike();
                break;
            case Elliptical:
            case RecumbentElliptical:
                SalutronManager.Instance.broadcastFTMSCrossTrainer();
                break;
            case Stepper:
                SalutronManager.Instance.broadcastFTMSStepClimber();
                break;
            case Ascent:
                SalutronManager.Instance.broadcastFTMSCrossTrainer();
                break;
            case Climbmill:
                SalutronManager.Instance.broadcastFTMSStairClimber();
                break;
        }
    }

    public void broadcastFTMSTreadMill() {
        if (packetManager != null) {
            _txBroadcastQueue.add(packetManager.buildFTMSPacket(salutronCommands.FTMS.SEND_FTMS_TREADMILL));
        }

    }

    public void broadcastFTMSIndoorBike() {

        if (packetManager != null) {
            _txBroadcastQueue.add(packetManager.buildFTMSPacket(salutronCommands.FTMS.SEND_FTMS_INDOORBIKE));
        }

    }

    public void broadcastFTMSCrossTrainer() {

        if (packetManager != null) {
            _txBroadcastQueue.add(packetManager.buildFTMSPacket(salutronCommands.FTMS.SEND_FTMS_CROSSTRAINER));
        }

    }

    public void broadcastFTMSStepClimber() {
        if (packetManager != null) {
            _txBroadcastQueue.add(packetManager.buildFTMSPacket(salutronCommands.FTMS.SEND_FTMS_STEP_CLIMBER));
        }

    }

    public void broadcastFTMSStairClimber() {

        if (packetManager != null) {
            _txBroadcastQueue.add(packetManager.buildFTMSPacket(salutronCommands.FTMS.SEND_FTMS_STAIR_CLIMBER));
        }
    }

    public void connectDevice(SalutronDevice.SalutronDeviceParcelable connectDevice) {
        Log.d(TAG, "connectDevice");

        disconnectDevice(false, 2000);

        this.connectDevice = connectDevice;
        connectType = connectDevice.type;

        byte[] connectPacket = packetManager.buildConnectPacket(connectType, this.connectDevice.id);
        _txCommandQueue.add(connectPacket);

        try {
            synchronized (notifyConnectedSync) {
                notifyConnectedSync.wait();
                isScanning = false;
            }
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }
    }

    public void disconnectDevice(boolean scan, int delayMS){
        Log.d(TAG, "disconnectDevice ");
        disconnectNotification.reset();
        _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.DISCONNECT_ALL));
        disconnectNotification.waitForNotification(delayMS);
        isScanning = false;
        if(scan) {
            controlDevice(Control_Command.SCANNING);
        }
    }

    public void sendDevices(){
        for(HeartRateInfo device: DeviceArray){
            SalutronDevice.sendDevice(device.name,device.id,device.type);
            Log.d(TAG, "sendDevices: " + device.name);
        }
    }

    private JHTTimer salutronHeartRateTimeout = new JHTTimer(10000,() -> {
        heartRateCallback.onHeartRate(0, 0);
    });

    public void receiveMessage(short classSub, byte[] data) {
        Log.d(TAG,"Class Sub: " + String.format("%X",classSub));
        salutronCommands.DeviceCommands deviceDataCmd = salutronCommands.DeviceCommands.fromInt(classSub);

        if (deviceDataCmd != null) {

            String packet = "";
            for (int i = 0; i < data.length; i++) {
                packet += String.format("%X", data[i]) + " ";
            }

            Log.d(TAG, "receiveMessage: " + deviceDataCmd.toString() + "  |  data length: " + data.length + "  |  data: " + packet);

            switch (deviceDataCmd) {
                case HRM_BLE_name:
                case HRM_BLE_FullName:
                    byte[] bleName = new byte[data.length - 7];
                    byte[] bleId = new byte[6];
                    int seprator = 0;

                    for (int i = 0; i < data.length; i++) {
                        if(data[i] != 0x1F)
                            bleName[i] = data[i];
                        else {
                            seprator = i;
                            break;
                        }
                    }

                    for(int i = seprator + 1; i < data.length; i++){
                        bleId[i - (seprator + 1)] = data[i];
                    }

                    try {
                        String bleNameUTF8 = new String(bleName,"UTF-8");
                        Log.d(TAG, "receiveMessage: BLE Name -- " + bleNameUTF8);
                        SalutronDevice.sendDevice(bleNameUTF8,bleId,salutronCommands.DeviceType.BLE);
                        DeviceArray.add(new HeartRateInfo(bleNameUTF8,bleId,salutronCommands.DeviceType.BLE));
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, e);
                    }
                    break;
                case ANT_Device:
                    byte[] antId = new byte[4];
                    String antName = "";
                    for (int i = 0; i < 2; i++) {
                        antId[i] = data[i + 1];
                    }
//                    hrm_ANT.rssi = data[3];

                    if(data[0] == (byte)0x78) {
                        antId[2] = (byte)((data[4] >> 4) & 0x0F);
                        antName = "ANT+ HR: DEVICE " + ByteBuffer.wrap(antId).order(ByteOrder.LITTLE_ENDIAN).getInt(); //NON-NLS
                        antId[2] = data[4];
                        DeviceArray.add(new HeartRateInfo(antName,antId,salutronCommands.DeviceType.ANT_HR));
                        SalutronDevice.sendDevice(antName,antId,salutronCommands.DeviceType.ANT_HR);
                    } else if(data[0] == (byte)0x26){
                        antId[2] = (byte)((data[4] >> 4) & 0x0F);
                        antName = "ANT+ EXD: DEVICE " + ByteBuffer.wrap(antId).order(ByteOrder.LITTLE_ENDIAN).getInt(); //NON-NLS
                        antId[2] = data[4];
                        DeviceArray.add(new HeartRateInfo(antName,antId,salutronCommands.DeviceType.ANT_EXD));
                        SalutronDevice.sendDevice(antName,antId,salutronCommands.DeviceType.ANT_EXD);
                    }
                    Log.d(TAG, "receiveMessage: ANT Name -- " + antName);

                    break;
                case BLE_HR_DATA:
                case ANT_HR_DATA:
                case WL5K_HR_DATA:
                case CONTACT_HR_DATA:
//                    int value = data[0] & 0x00FF;
//                    Log.d(TAG, "onHeartRate: mHeartRateSalutron " + mHeartRateSalutron + "  value " + value);
//                    if(mHeartRateSalutron > 0) {
//                        Log.d(TAG, "onHeartRate: mHeartRateSalutron > 0");
//                        if (value - mHeartRateSalutron > 50) {
//                            Log.d(TAG, "onHeartRate: return(50)");
//                            return;
//                        }
//                    }
//                    else if(mHeartRateSalutron == 0){
//                        Log.d(TAG, "onHeartRate: mHeartRateSalutron == 0");
//                        if(value - mHeartRateSalutron > 220){
//                            Log.d(TAG, "onHeartRate: return(220)");
//                            return;
//                        }
//                    }

                    heartRateCallback.onHeartRate(data[0] & 0x00FF,deviceDataCmd.getValue());
//                    mHeartRateSalutron = value;
                    salutronHeartRateTimeout.reset();
                    break;
                case HRM_EVENT:
                    eventCodeHandler(data[0]);
                    break;
                case Status:
                    deviceStatus(data[0]);
                    break;
                case ACLK:
                    if (_txPacketQueue != null) {
                        _txPacketQueue.poll();
                        _txRetryCounter = 0;
                        synchronized (notifyRxPacketSync) {
                            notifyRxPacketSync.notifyAll();
                        }
                    }
                    break;
                case Firmware_Version:
                    try {
                        String salutronVersion = new String(data,"UTF-8");
                        Log.d(TAG, "receiveMessage: Firmware version -> " + salutronVersion);
                        heartRateCallback.version(salutronVersion);
                    } catch (UnsupportedEncodingException e) {
                        Log.w(TAG, e);
                    }
                    break;
                case Contact_Stat:
                    if(data[0] == 0x03){
                        Log.d(TAG,"Contact hand on");
                        heartRateCallback.blink(true);
                    }
                    else if (data[0] == 0x04){
                        Log.d(TAG,"Contact hand off");
                        heartRateCallback.onHeartRate(0, 0);
                        heartRateCallback.stopBlink();
                    }
                    break;

                case BLE_MAC:
                    ByteBuffer mac = ByteBuffer.allocate(2);
                    mac.put(data[4]);
                    mac.put(data[5]);
                    mac.flip();
                    defaultANTid = mac.order(ByteOrder.LITTLE_ENDIAN).getShort();
                    Log.d(TAG, "receiveMessage: default ANT id " + defaultANTid);
                    break;
                case ANT_STATUS:
                    BitSet statusSet = BitSet.valueOf(data);
                    String set = "";
                    for(int i = 0; i < statusSet.length(); i++)
                        set += statusSet.get(i) + " ";
                    Log.d(TAG,"ANT Status set: " + set);

                    break;
                case RESET:
                    if(data[0] == 0)
                        synchronized (notifyResetSync){
                            notifyResetSync.notifyAll();
                            Log.d(TAG,"initialized finish");
                        }
                    break;
                case Disconnect_all_success:
                    if(data[0] == 1) {
//                        Log.e(TAG, "receiveMessage: disconnect success");
                        heartRateCallback.isConnected(false, null);
//                    resetStateTimer();
                        disconnectNotification.setNotification();
                        heartRateCallback.onHeartRate(0,0);
                        mHeartRateSalutron = 0;
//                        controlDevice(Control_Command.SCANNING);
                    }

            }
        }

    }

    private JHTNotification disconnectNotification = new JHTNotification();

    private void deviceStatus(short _status) {
        salutronCommands.Status status = salutronCommands.Status.fromInt(_status);
        if (status != null) {
            Log.d(TAG, "deviceStatus: " + status);
            switch (status) {
                case STAT_Disconnected:
                    break;
                case STAT_Connected:
                    break;
                case STAT_Scanning:
                    isScanning = true;
                    break;
                case STAT_WorkoutStart:
                    isWorkout = true;
                    break;
                case STAT_WorkoutStop:
                    isWorkout = false;
                    break;
                case STAT_5KHZ_Active:
                case STAT_Contat_Active:
                case STAT_FEC_Status:
                    break;
            }
            updateWorkoutState();
        }
    }


    private void eventCodeHandler(short eventcode) {

        salutronCommands.HRM_Event eventCode = salutronCommands.HRM_Event.fromInt(eventcode);
        if (eventCode != null) {
            Log.e(TAG, "Event: " + eventCode.toString());
            switch (eventCode) {
                case CON_lost:
                    heartRateCallback.onHeartRate(0,0);
                    heartRateCallback.isConnected(false, null);
                    mHeartRateSalutron = 0;
                    break;
                case Relearn:
                case CT_Relearn:
                    break;
                case Scanning_Done:
                    isScanning = false;
                    break;
                case Device_connected:
                    heartRateCallback.isConnected(true, connectDevice);
                    synchronized (notifyConnectedSync){
                        notifyConnectedSync.notifyAll();
                    }
                    break;
                case Device_not_found:
                    heartRateCallback.isConnected(false, null);
                    DataMatrix.sendEvent(HALKeys.EVENT_SALUTRON_DEVICE_CONNECTION_FAIL);
                    synchronized (notifyConnectedSync){
                        notifyConnectedSync.notifyAll();
                    }
                    _txCommandQueue.add(packetManager.buildTxPacket(SalutronCommand.DISCONNECT_ALL));

                    connectType = null;

                    break;
            }
            updateWorkoutState();
        }
    }

    public void setFirmware(File firmware) {
        _firmware = firmware;
    }

}
