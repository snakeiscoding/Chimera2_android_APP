package com.jht.chimera.service.hal.lib.io.iocommlib;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.ExecuteAsRoot;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.datamatrixlib.DataMatrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class McuPacketManager extends BasePacketManager implements MCUInterface {


    private static class McuPacketManagerInstance {
        // Note we can suppress this because we only store the application context which is good for
        // the life of the process thus no memory leak.
        @SuppressLint("StaticFieldLeak")
        private static final McuPacketManager mcuPacketManager = new McuPacketManager();
    }
    public static McuPacketManager getInstance() {
        return McuPacketManagerInstance.mcuPacketManager;
    }

    public static final String UPDATE_TARGET = "IO";

    public IMCUCallback callback;
    private PeriodicTimer watchdogTimer = new PeriodicTimer();

    public enum PowerVoltage {
        POWER_12V,
        POWER_14V
    }

    public static int disablePower = 0x00;
    public static int enablePower = 0x01;
    public static int powerOnERPOff = 0x02;



    protected McuPacketManager() {
        super();
        logTag = McuPacketManager.class.getName();

        // should this DM for debug tracing be in the base as well ?
        DataMatrix dataMatrix = new DataMatrix();
        dataMatrix.createBoolAdapter(SettingKeys.Experimental.DATA_LOG_MCU_PACKETS_BOOL, value -> {
            if(port != null) {
                port.setTraceDebug(value);
            }
        });
    }

    @Override
    public void setCallback(IMCUCallback callback) {
        this.callback = callback;
    }

    @Override
    public void getOperationMode() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.OPERATION_MODE));
    }

    @Override
    public void getVersion() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.VERSION));
    }

    @Override
    public void getSafeKeyState() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SAFEKEY));
    }

    @Override
    public void getDevicePowerState(byte source) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.POWER_STATE, source));
    }

    @Override
    public void getExternalPowerState() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.POWER_STATE, (byte) 0x10));
    }

    @Override
    public void getBoot() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.BOOT));
    }

    @Override
    public void getLcbType() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.LCB_TYPE));
    }

    @Override
    public void getFanSpeed() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.FAN_SPEED));
    }

    @Override
    public void getPowerMonitorState() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.HW_MONITOR_STATE));
    }

    @Override
    public void getExtendPowerError() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.HW_MONITOR, (byte)0x02));
    }

    @Override
    public void getPowerState(byte source) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.HW_MONITOR, source));
    }

    @Override
    public void setWatchdog(int timeout) {
        byte[] timeArray = ByteBuffer.allocate(4).putInt(timeout).array();

        if (timeout <= 0) {
            txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.WATCHDOG, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00));

            if (watchdogTimer != null) {
                watchdogTimer.stop();
            }
        } else {
            txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.WATCHDOG, (byte) 0x01, timeArray[3], timeArray[2], timeArray[1], timeArray[0]));

            if (watchdogTimer != null) {
                watchdogTimer.stop();
            }
            watchdogTimer = new PeriodicTimer();
            watchdogTimer.setOnTimer(this::refresh);
            watchdogTimer.start(0, 1000 * 60);
        }
    }

    @Override
    public void sendKeyEvent(boolean isUp, int key_u16) {
        // Normally we receive key events, but it may be possible to send events to io in certain cases?
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.KEY_EVENT, (byte) (isUp ? 1 : 0), (byte) (key_u16 & 0xFF), (byte) (key_u16 >> 8)));
    }

    @Override
    public void refresh() {
        // Note: Don't refresh the watchdog to force the IO watchdog logic to trigger to test the IO reboot counter
        // This key will be updated via adb command line
        // If the IO watchdog refresh is disabled, return
        if (DataMatrix.getBoolean(HALKeys.DATA_IO_DISABLE_WATCHDOG_REFRESH_BOOL)) return;

        //Twice command in once fresh. Avoid mcu loss package.
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.REFRESH));
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.REFRESH));
    }

    @Override
    public void setKeyDebounceTime(int time, int time2, int gapTime) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.KEY_SETTING, (byte) time, (byte) time2, (byte) gapTime));
    }

    @Override
    public void setTone(int type) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.TONE, (byte) type));
    }

    @Override
    public void setFanSpeed(int speed) {
        byte[] speedArray = ByteBuffer.allocate(4).putInt(speed).array();
        //Log.d(TAG, "Set Fan speed: " + new Byte(speedArray[3]).intValue());
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.FAN, speedArray[3]));
    }

    /**
     * Specify the power setting to specific location
     *
     * @param source    A byte to specify the location of the power setting
     * @param _switch      0x00 : Disable, 0x01: Enable, 0x02: Power off / ERP on
     */
    @Override
    public void setPower(byte source, int _switch) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.POWER, source, (byte) _switch));

    }

    @Override
    public void setRS485Communication(boolean enable) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.RS485, (byte) (enable ? 1 : 0)));
    }

    @Override
    public void setQiVoltage(int voltage) {
        // 0 : 5V    1 : 9V
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.QI,(byte)voltage));
    }

    @Override
    public void setERPLED(int power) {
        // 0 : mode 1(chimera) will turn off the led after power on    1 : mode 2(project one) led will
        // keep on even power on complete
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.ERP_LED,(byte)power));
    }

    @Override
    public void setLEDColor(int LEDNumber, int color) {
        int R = (color >> 16) & 0xff;
        int G = (color >> 8 ) & 0xff;
        int B = (color ) & 0xff;

        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.LED_COLOR,(byte) 0x00,(byte)R, (byte)G, (byte)B));
    }

    public void writeToPassthrough(McuPassthroughDevice device, byte[] payload) {
        if (payload.length > McuPacket.MAX_DATA_PAYLOAD_SIZE) {
            Log.e(logTag, "sendPassthrough payload size of " + payload.length + " will be truncated to " + McuPacket.MAX_DATA_PAYLOAD_SIZE);
        }
        // use switch or map or add command to the enum?
        McuProtocol.McuCommands command;
        switch (device) {
            case CAB: command = McuProtocol.McuCommands.SEND_CAB_COMMAND; break;
            case BUMP_SENSOR: command = McuProtocol.McuCommands.SEND_BUMP_SENSOR_COMMAND; break;
            default: Log.w(logTag, "Unsupported passthrough device " + device); return; // bail if unknown
        }
        txCommandQueue.add(new McuPacket(command, 3000, payload));
    }

    @Override
    public void enterERP() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.ERP, (byte) 0x00));
    }

    @Override
    public void enterSuspend() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.ERP, (byte) 0x01));
    }

    @Override
    public void enterReboot() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SOC_EVENT, (byte) 0x01, (byte) 0x00));
    }

    @Override
    public void setDebug(boolean enable, int level) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.DEBUG, (byte) (enable ? 1 : 0), (byte) level));
    }

    @Override
    public void updateIO(Uri uri) {
        // is this delay really needed if we are not stopping and switching port servicing anymore?
        /*try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }*/
        startUpdate(this, context, uri);
    }

    @Override
    public void sendIRcommand(byte[] code) {
        //Log.d(logTag, "Send ir data " + Strings.bytesToHex(code));
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_IR_COMMAND, code));
    }

    @Override
    public void sendIRFrequency(short frequency) {
        // TODO: Need to switch this to use BDC IO version.
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_IR_FREQUENCY, (byte)frequency));
        //_txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_IR_FREQUENCY, frequency == 0 ? (byte)0 : (byte)1));

    }

    @Override
    public void setCECconfig(byte address) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SET_CEC_CONFIG, address));
    }

    @Override
    public void sendCECcommand(byte[] command) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_CEC_COMMAND, command));
    }

    @Override
    public void getCECresponse() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.GET_CEC_RESPONSE));
    }

    @Override
    public void reboot(int delayTime) {
        byte[] timeArray = ByteBuffer.allocate(4).putInt(delayTime).array();
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.REBOOT, (byte) 0x01, timeArray[3]));
    }

    @Override
    public void powerMonitor(boolean enable,int time) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.HW_MONITOR, (byte) (enable ? 1 : 0),(byte) time));
    }

    @Override
    public void sendCanTx(byte[] data) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.CAN_TX, data));
    }

    @Override
    protected void rxProcess(int commandCode, byte[] payload, McuPacket.CommandType commandType) {
        McuProtocol.McuCommands mcuCommand = McuProtocol.McuCommands.fromInt(commandCode);
        //Log.d(logTag, "command Code: " + commandCode + "  command name:" + mcuCommand.toString());
        if(mcuCommand == null) return;

        // treat get operating mode as a non-update command
        if (McuProtocol.McuCommands.OPERATION_MODE.equals(commandCode)) {
            commandType = McuPacket.CommandType.REPLAY;
        }

        result = McuPacketParser.ParseResult.Success;
        if (commandType == McuPacket.CommandType.EVENT) {
            switch (mcuCommand) {
                case HEART_RATE_EVENT:
                    txEventQueue.add(new McuPacket(McuProtocol.McuCommands.HEART_RATE_EVENT));
//                    DataMatrix dataMatrix = new DataMatrix();
//                    Log.d(logTag,"heart rate payload: "+payload[1]);
//                    DataMatrix.putInt(DATA_HEART_RATE_INT,payload[1]);
                    callback.heartRateEvent(payload[1]);
                    break;
                case KEY_EVENT:
//                  Log.d(logTag, "Key event Row: " + String.format("%X", payload[1]) + "  Column:" + String.format("%X", payload[2]) + " event: " + payload[0]);
                    txEventQueue.add(new McuPacket(McuProtocol.McuCommands.KEY_EVENT));
                    ByteBuffer key = ByteBuffer.allocate(2);
                    key.put(payload[1]);
                    key.put(payload[2]);
                    key.flip();
                    short keyValue = key.order(ByteOrder.LITTLE_ENDIAN).getShort();
//                    callback.keyEvent(payload[0],(short)((payload[2] << 8) + payload[1]));
                    String hex = Integer.toHexString(keyValue & 0xffff);
                    Log.d("McuPacketManager","KeyValue Short toHexString: " + hex);
                    callback.keyEvent(payload[0],keyValue);
                    break;
                case SAFEKEY_EVENT:
                    callback.safeKeyEvent(payload[0] == 1);
                    txEventQueue.add(new McuPacket(McuProtocol.McuCommands.SAFEKEY_EVENT));
                    break;
                case DEBUG_MESSAGE:
                    callback.debugMessage(stringFromPayload(payload));
                    txEventQueue.add(new McuPacket(McuProtocol.McuCommands.DEBUG_MESSAGE)); // why do we reply to these events?
                    break;
                case LCB_STATUS:
                    txEventQueue.add(new McuPacket(McuProtocol.McuCommands.LCB_STATUS));
                    break;
                case HW_MONITOR_EVENT:
                        ByteBuffer voltage = ByteBuffer.allocate(2);
                        //console voltage
                        voltage.put(payload[0]);
                        voltage.put(payload[1]);
                        voltage.flip();
                        callback.consolePowerVoltage(voltage.order(ByteOrder.LITTLE_ENDIAN).getShort());
                        voltage.clear();
                        //extend voltage
                        voltage.put(payload[2]);
                        voltage.put(payload[3]);
                        voltage.flip();
                        callback.extendPowerVoltage(voltage.order(ByteOrder.LITTLE_ENDIAN).getShort(),false);
                        voltage.clear();
                        //usb voltage
                        voltage.put(payload[4]);
                        voltage.put(payload[5]);
                        voltage.flip();
                        callback.usbVoltage(voltage.order(ByteOrder.LITTLE_ENDIAN).getShort());
                        voltage.clear();

                        ByteBuffer current = ByteBuffer.allocate(2);
                        //console current
                        current.put(payload[6]);
                        current.put(payload[7]);
                        current.flip();
                        callback.consolePowerCurrent(current.order(ByteOrder.LITTLE_ENDIAN).getShort());
                        current.clear();
                        //extend current
                        current.put(payload[8]);
                        current.put(payload[9]);
                        current.flip();
                        callback.extendPowerCurrent(current.order(ByteOrder.LITTLE_ENDIAN).getShort());
                        current.clear();
                        //extend error
                        callback.extendPowerError(payload[10] == 0);
                    break;


            }
        }

        /*
         * When attempting to get documentation approved for these MCU command bytes
         * The team at JHT has requested CAN_RX and CAN_TX both be classified as transceiver
         * functions, not as a command/event structure. So this check is outside of the above logic
         * that checks the first byte of a command for its "EVENT" classification.
         */
        if (mcuCommand == McuProtocol.McuCommands.CAN_RX){
            callback.canRx(payload);
            return;
        }

        // if this rx is not for the command we sent, bail at this point
        int sentCommandCode = (this.outPacket == null ? 0 : this.outPacket.commandCode);
        if (sentCommandCode != commandCode) return;

        if (McuProtocol.McuStatus.Success.equals(parser.packet.status)) {
            finishTxCommand();
        }
        notifyRxPacketSync.setNotification();

        if (commandType == McuPacket.CommandType.UPDATE) {
            updateProcess(commandCode, payload, commandType);
        } else if (commandType == McuPacket.CommandType.REPLAY) {

            switch (mcuCommand) {
                case SEND_CAB_COMMAND:
                    processPassthroughReceived(McuPassthroughDevice.CAB, payload);
                    break;
                case SEND_BUMP_SENSOR_COMMAND:
                    processPassthroughReceived(McuPassthroughDevice.BUMP_SENSOR, payload);
                    break;
//                case TONE:
//                case FAN:
//                case POWER:
                case ERP:
                    ExecuteAsRoot.runSyncCommand();
                    DataMatrix.sendEvent(HALKeys.ACTION_SHUTDOWN_SYSTEM);
                    break;
//                case DEBUG:
                case LCB_TYPE:
                    callback.lcbType(payload[0]);
                    break;
                case FAN_SPEED:
                    ByteBuffer rpm = ByteBuffer.allocate(2);
                    rpm.put(payload[1]);
                    rpm.put(payload[2]);
                    rpm.flip();
                    int rpmValue = rpm.order(ByteOrder.LITTLE_ENDIAN).getShort();
                    callback.fanSpeed(payload[0],rpmValue);
                    break;
                case WATCHDOG:
                    break;
                case OPERATION_MODE:
                    callback.operationMode(payload[0]);
                    break;
                case VERSION:
                    int version = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).getShort();
                    callback.version(version);
                    break;
                case SAFEKEY:
                    callback.safeKeyStatus(payload[0] == 1);
                    break;
                case POWER_STATE:

                    if(payload[0] == (byte)0x10){
                        ByteBuffer current = ByteBuffer.allocate(2);
                        current.put(payload[3]);
                        current.put(payload[4]);
                        current.flip();
                        int currentValue = current.order(ByteOrder.LITTLE_ENDIAN).getShort();
                        if(payload[1] == 1)
                            callback.externalPower(PowerVoltage.POWER_14V, currentValue);
                        else if(payload[1] == 0)
                            callback.externalPower(PowerVoltage.POWER_12V, currentValue);
                    }
                    else
                        callback.powerState(payload[0],payload[1] == 1);

                    break;
                case BOOT:
                    callback.boot(payload[0] == 1);
                    break;
                case HW_MONITOR_STATE:
                    callback.powerMonitorState(payload[0] == 1);
                    break;
                case SEND_CEC_COMMAND:
                    callback.cecCommand(payload[0]);
                    break;
                case GET_CEC_RESPONSE:
                    callback.getCECresponse(payload);
                    break;
                case SEND_IR_COMMAND:
                    callback.irCommand(true);
                    break;
                case HW_MONITOR:
                    switch(payload[0]){
                        case 0x00:
                            //Voltage
                            ByteBuffer voltage2 = ByteBuffer.allocate(2);
                            //console voltage
                            voltage2.put(payload[1]);
                            voltage2.put(payload[2]);
                            voltage2.flip();
                            callback.consolePowerVoltage(voltage2.order(ByteOrder.LITTLE_ENDIAN).getShort());
                            voltage2.clear();
                            //extend voltage
                            voltage2.put(payload[3]);
                            voltage2.put(payload[4]);
                            voltage2.flip();
                            callback.extendPowerVoltage(voltage2.order(ByteOrder.LITTLE_ENDIAN).getShort(),true);
                            voltage2.clear();
                            //usb voltage
                            voltage2.put(payload[5]);
                            voltage2.put(payload[6]);
                            voltage2.flip();
                            callback.usbVoltage(voltage2.order(ByteOrder.LITTLE_ENDIAN).getShort());
                            voltage2.clear();
                            break;
                        case 0x01:
                            //Current
                            ByteBuffer current = ByteBuffer.allocate(2);
                            //console current
                            current.put(payload[1]);
                            current.put(payload[2]);
                            current.flip();
                            callback.consolePowerCurrent(current.order(ByteOrder.LITTLE_ENDIAN).getShort());
                            current.clear();
                            //extend current
                            current.put(payload[3]);
                            current.put(payload[4]);
                            current.flip();
                            callback.extendPowerCurrent(current.order(ByteOrder.LITTLE_ENDIAN).getShort());
                            current.clear();
                            break;
                        case 0x02:
                            //Power state
                            //extend error
                            callback.extendPowerError(payload[1] == 0);
                            break;
                    }
                    break;
            }
        }
    }

    private void processPassthroughReceived(McuPassthroughDevice device, byte[] payload) {
        if (payload != null && payload.length > 0) {
            Bundle bundle = new Bundle();
            bundle.putString(McuPassthroughKt.PASSTHROUGH_DEVICE_KEY, device.name());
            bundle.putByteArray(McuPassthroughKt.PASSTHROUGH_PAYLOAD_KEY, payload);
            DataMatrix.putParcelable(HALKeys.EVENT_RECEIVE_DATA_FROM_IO_PASSTHROUGH_PARCELABLE, bundle);
        }
    }

}
