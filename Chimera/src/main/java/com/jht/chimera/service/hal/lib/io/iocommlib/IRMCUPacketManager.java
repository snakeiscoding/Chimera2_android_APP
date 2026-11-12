package com.jht.chimera.service.hal.lib.io.iocommlib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.jht.chimera.lib.common.app.AndroidSetting;
import com.jht.serialport.ISerialPort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IRMCUPacketManager extends BasePacketManager {

    private static class IRMcuPacketManagerInstance {
        // Note we can suppress this because we only store the application context which is good for
        // the life of the process thus no memory leak.
        @SuppressLint("StaticFieldLeak")
        private static final IRMCUPacketManager irmcuPacketManager = new IRMCUPacketManager();
    }
    public static IRMCUPacketManager getInstance() {
        return IRMcuPacketManagerInstance.irmcuPacketManager;
    }

    public static final String UPDATE_TARGET = "IR";

    protected IRMCUPacketManager() {
        super();
        logTag = "IRMCUPacketManager";
    }

    public void sendIRcommand(byte[] code) {
        //Log.d(logTag, "LCGC Send ir data " + Strings.bytesToHex(code) + ", length = " + code.length);
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_IR_COMMAND, code));
    }

    public void sendIRFrequency(short frequency) {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_IR_FREQUENCY, (byte)frequency));
        //_txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.SEND_IR_FREQUENCY, frequency == 0 ? (byte)0 : (byte)1));
    }

    public void getIRVersion() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.VERSION));
    }

    public void getOperationMode() {
        txCommandQueue.add(new McuPacket(McuProtocol.McuCommands.OPERATION_MODE));
    }

    @Override
    public boolean start(ISerialPort device, Context context) {
        boolean ok = super.start(device, context);
        if (ok) {
            getIRVersion(); // get fw version right away
            getOperationMode();
        }
        return ok;
    }


    public void updateIR(Uri uri) {
        // is this delay really needed if we are not stopping and switching port servicing anymore?
        /*try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Log.w(TAG, e);
        }*/
        startUpdate(this, context, uri);
    }

    @Override
    protected void finishedUpdate(boolean success) {
        super.finishedUpdate(success);
        // update version info?
        if (success) {
            getIRVersion();
        }
    }

    @Override
    protected void rxProcess(int commandCode, byte[] payload, McuPacket.CommandType commandType){
        McuProtocol.McuCommands mcuCommand = McuProtocol.McuCommands.fromInt(commandCode);
        //Log.d(logTag, "command Code: " + commandCode + "  command name:" + mcuCommand.toString());
        if(mcuCommand == null) return;

        if (commandType == McuPacket.CommandType.EVENT) {
            switch (mcuCommand) {
                case DEBUG_MESSAGE:
                    Log.e(logTag, "Message: " + stringFromPayload(payload));
                    break;
            }
        }

        // if this rx is not for the command we sent, bail at this point
        int sentCommandCode = (this.outPacket == null ? 0 : this.outPacket.commandCode);
        if (sentCommandCode != commandCode) return;

        if (McuProtocol.McuStatus.Success.equals(parser.packet.status)) {
            finishTxCommand();
        }
        notifyRxPacketSync.setNotification();

        if (commandType == McuPacket.CommandType.UPDATE) { // not sure this "if" is really needed? these are checking for specific commands anyway?
            updateProcess(commandCode, payload, commandType);
        }

        switch (mcuCommand) {
            case SEND_IR_COMMAND:
                //Log.d(logTag, "IR rxProcess: SEND_IR_COMMAND success");
                break;
            case SEND_IR_FREQUENCY:
                //Log.d(logTag, "IR rxProcess: SEND_IR_FREQUENCY success");
                break;
            case VERSION:
                int version = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).getShort();
                String versionString = "" + (version >> 8 & 0x00ff) + "." + (version & 0x00ff);
                //Log.d(logTag, "IR rxProcess: read version " + versionString);
                //DataMatrix.putString(HALKeys.DATA_VERSION_IO_STRING, "" + versionString);
                AndroidSetting.putString(context.getContentResolver(), "parker_ir_version", versionString + ".0.0"); //NON-NLS
                break;
            case OPERATION_MODE:
                if(payload[0] != 0) {
                    Log.e(logTag, "IR error: MCU is in bootloader mode");
                }
                break;
        }

    }

    @Override
    protected void rxError(int commandCode) {
        if (McuProtocol.McuCommands.VERSION.equals(commandCode)) {
            AndroidSetting.putString(context.getContentResolver(), "parker_ir_version", null); // clear previous recorded version?
        }
    }

}
