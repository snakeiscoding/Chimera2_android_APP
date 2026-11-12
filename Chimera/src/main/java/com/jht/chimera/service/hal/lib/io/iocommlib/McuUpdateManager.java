package com.jht.chimera.service.hal.lib.io.iocommlib;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPacket.CommandType;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class McuUpdateManager {

    private static final String TAG = "McuUpdateManager";

    private BasePacketManager parent;

    public int percent;

    public Context context;

    public int offsetAddress;
    public int apStartAddress;
    public int apEndAddress;
    public int nvramStartAddress;
    public int nvramEndAddress;
    public int writeIndex;
    public int writeTotalPackage;
    private WriteArea writeArea;
    private UpdateArea updateArea;


    public List<byte[]> _updateDateList = new ArrayList<>();

    enum WriteArea {
        AP,
        NVRAM
    }

    enum UpdateArea {
        // MCU Update Area NVRAM is the 64k ram area, AP is off the 64k chip and does not persist if console losses power.
        AP_ONLY,
        NVRAM_ONLY,
        AP_NVRAM,
    }

    public McuUpdateManager() {
        reset();
    }

    private boolean followsChimera2Path() {
        // need to find some way to clean this up, or maybe create v2 of the update commands that are more generic
        switch (ProductConfig.hardware) {
            case Chimera_2:
            case Chimera_3:
            case Vortex:
            case VortexLite:
                return true;
            default:
                return false;
        }
    }

    private void reset() {
        percent = 0;
        offsetAddress = 0x8000000;
        if (followsChimera2Path()){
            writeIndex = 0;
        } else {
            writeIndex = 128; // UPDATE_PARAMETERS sets this anyway?
        }
        writeArea = WriteArea.AP;
        _updateDateList.clear();
    }

    private void sendPacket(McuPacket packet) {
        if (parent != null) parent.sendPacket(packet);
    }

    private void sendUpdateStart() {
        sendPacket(new McuPacket(McuProtocol.McuCommands.START_UPDATE, (byte) 0x6A, (byte) 0x6F, (byte) 0x68, (byte) 0x6E, (byte) 0x73, (byte) 0x6F, (byte) 0x6E));
    }

    private void finishUpdate(Boolean wasSuccess) {
        if (wasSuccess) Log.d(TAG,"MCU update finished");
        else Log.d(TAG,"MCU update failed");
        if (parent != null) parent.finishedUpdate(wasSuccess);
    }

    public void startUpdate(BasePacketManager parent, Context context, Uri uri) {
        //reset();
//        this.uri = uri;
        this.context = context;
        this.parent = parent;

        int index = 0;
        byte[] tempData = new byte[512];
        try (InputStream fileInStream = new DataInputStream(new FileInputStream(uri.getPath()))) {
            int length = (int) new File(uri.getPath()).length();
            int remainDataLength = length;
            byte[] updateData = new byte[length];
            fileInStream.read(updateData);

            for (int i = 0; i < length; i++) {
                tempData[index] = updateData[i];
                if (index < 511 && (i != (length -1)))
                    index++;
                else {
                    _updateDateList.add(tempData);
                    if (remainDataLength > 512){
                        remainDataLength = remainDataLength - 512;
                        tempData = new byte[512];
                    } else {
                        tempData = new byte[remainDataLength];
                    }
                    index = 0;
                    Log.d(TAG,"update data list size: " + _updateDateList.size() + " File length: " + length + " update data index: " + i);
                }
            }

            Log.d(TAG,"update data list size: " + _updateDateList.size() + " File length: " + length);
            //sendPacket(new McuPacket(McuProtocol.McuCommands.OPERATION_MODE)); // why is this needed here ? console would have checked this on startup ?
            sendUpdateStart();

        } catch (Exception e) {
            Log.e(TAG, "Failed to start update", e);
        }

    }


    public void updateProcess(int command, byte[] payload, CommandType commandType) {
        McuProtocol.McuCommands commandCode = McuProtocol.McuCommands.fromInt(command);
        //Log.d(TAG, "command Code: " + command + "  command name:" + commandCode.toString());
        if (commandType == CommandType.UPDATE) {

            switch (commandCode) {
                /*case OPERATION_MODE:
                    sendUpdateStart();
                    break;*/
                case START_UPDATE:
                    new JHTTimer(5000,() -> sendPacket(new McuPacket(McuProtocol.McuCommands.UPDATE_PARAMETERS))).start();
                    broadcastUpdateStatus(command, 0);
                    break;
                case UPDATE_PARAMETERS:
                    apStartAddress = ((payload[3] & 0xFF) << 24) + ((payload[2] & 0xFF) << 16) + ((payload[1] & 0xFF) << 8) + (payload[0] & 0xFF);
                    apEndAddress = ((payload[7] & 0xFF) << 24) + ((payload[6] & 0xFF) << 16) + ((payload[5] & 0xFF) << 8) + (payload[4] & 0xFF);
                    nvramStartAddress = ((payload[16] & 0xFF) << 24) + ((payload[15] & 0xFF) << 16) + ((payload[14] & 0xFF) << 8) + (payload[13] & 0xFF);
                    nvramEndAddress = ((payload[20] & 0xFF) << 24) + ((payload[19] & 0xFF) << 16) + ((payload[18] & 0xFF) << 8) + (payload[17] & 0xFF);
                    if(!BuildConfig.FLAVOR_platform.equals("chimera")) {
                        offsetAddress = Math.min(apStartAddress, nvramStartAddress);
                    }
                    if (followsChimera2Path() || ProductConfig.hardware.equals(ProductConfig.ConsoleHardware.Parker_3)){
                        // Parker 1 didn’t need this setting but Parker 3 has to to fix some
                        // issues. JHT recommended this change
                        writeIndex = 0;
                        writeTotalPackage = _updateDateList.size();
                        updateArea = UpdateArea.AP_ONLY;
                    } else {
                        writeIndex = (apStartAddress - offsetAddress) / 512; // this needs to end up zero ? the assumption seems to be that main is before boot?
                        writeTotalPackage = ((apEndAddress - apStartAddress + 1) + (nvramEndAddress - nvramStartAddress + 1)) / 512;
                        updateArea = UpdateArea.AP_NVRAM;
                    }
                    Log.d(TAG, "AP Start address: " + String.format("%X", apStartAddress) + "  AP End address: " + String.format("%X", apEndAddress));
                    Log.d(TAG, "Nvram Start address: " + String.format("%X", +nvramStartAddress) + "  Nvram End address: " + String.format("%X", nvramEndAddress));
                    Log.d(TAG,"AP writeIndex init: " + writeIndex);
                    Log.d(TAG, "writeTotalPackage: " + writeTotalPackage);
                    sendPacket(new McuPacket(McuProtocol.McuCommands.ERASE, 3000));

                    break;
                // Previous code send the writeCode during erase state.  This
                // resulted in sending the first packet twice which can cause
                // checkusm errors.  It may seem to make sense to send the erase
                // packet in the erase state, but that resulted in the erase not being sent.
                // Did not look further into refactors to figure out why.  This is working now consistently.
                case ERASE:
                case WRITE_PAGE:
                    boolean writeResult = writeCode();
                    Log.d(TAG, "write result: " + writeResult);
                    percent++;
                    broadcastUpdateStatus(command, percent * 100.0f / writeTotalPackage);
                    if (writeResult)
//                        sendPacket(new McuPacket(McuProtocol.McuCommands.READ_PAGE, (byte) 0xF0, (byte) 0xFF, (byte) 0x00, (byte) 0x08, (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00));
                        sendPacket(new McuPacket(McuProtocol.McuCommands.PROGRAM_STATE));
                    break;
                case READ_PAGE:
                    String checkDataString = "";
                    for (int i = 0; i < payload.length; i++)
                        checkDataString += String.format("%X", payload[i]) + " ";

                    Log.d(TAG, "Read code: " + checkDataString);
                    sendPacket(new McuPacket(McuProtocol.McuCommands.PROGRAM_STATE));
                    break;
                case PROGRAM_STATE: //0x00 update not finish.  0x01 update finish
                    if ((payload[0] & 0xFF) == (byte) 0x00)
                        sendPacket(new McuPacket(McuProtocol.McuCommands.PROGRAM_STATE));
                    else {
                        broadcastUpdateStatus(command, 0);
                        JHTTimer rebootTimer = new JHTTimer(() ->
                                sendPacket(new McuPacket(McuProtocol.McuCommands.END_UPDATE)));
                        rebootTimer.start(300);
                    }
                    break;
                case END_UPDATE:
                    finishUpdate(true);
                    break;
            }
        }
    }

    public boolean writeCode() {
        boolean result = false;
        byte[] tempDate = new byte[0];
        byte[] lengthArray = new byte[4];
        byte[] addressArray = new byte[4];

        if (writeArea == WriteArea.AP) {
            Log.d(TAG, "_updateDateList.size(): "+_updateDateList.size()+" writeIndex: "+writeIndex);
            if (apStartAddress < apEndAddress) {
                // if (_updateDateList.size() == writeIndex && followsChimera2Path()){
                if (_updateDateList.size() == writeIndex && updateArea == UpdateArea.AP_ONLY){
                    return true;
                }
                lengthArray = ByteBuffer.allocate(4).putInt(512).array();
                addressArray = ByteBuffer.allocate(4).putInt(apStartAddress).array();

                if (_updateDateList.size() >= writeIndex) {
                    Log.d(TAG, "write ap index: " + writeIndex);
                    tempDate = _updateDateList.get(writeIndex);
                    writeIndex++;
                }
                apStartAddress += 512;
                // Parker 3 removed this I have testing to do around it on chimera.
                //  result = false;
            } else if (apStartAddress >= apEndAddress) {
                Log.d(TAG, "write ap finish");
                writeArea = WriteArea.NVRAM;
                writeIndex = (nvramStartAddress - offsetAddress) / 512;
                Log.d(TAG,"Nvram writeIndex init: " + writeIndex);
                // Parker 3 removed this I have testing to do around it on chimera.
                // result = false;
            }
            Log.d(TAG, " write area: " + writeArea.toString() + "  start address: " + String.format("%X",apStartAddress) + "  " + Arrays.toString(addressArray));
        }

        if (writeArea == WriteArea.NVRAM) {
            // if (followsChimera2Path()){
            if (updateArea == UpdateArea.AP_ONLY){ // we removed this to be followsChimera2Path because we are handling this logic differently now.
                return true;
            }
            if (nvramStartAddress < nvramEndAddress) {

                lengthArray = ByteBuffer.allocate(4).putInt(512).array();
                addressArray = ByteBuffer.allocate(4).putInt(nvramStartAddress).array();

                if (_updateDateList.size() >= writeIndex) {
                    Log.d(TAG, "write nvarm index: " + writeIndex);
                    tempDate = _updateDateList.get(writeIndex);
                    writeIndex++;
                }
                nvramStartAddress += 512;
                // parker 3 removing this.
                // result = false;
            } else if (nvramStartAddress >= nvramEndAddress) {
                Log.d(TAG, "write nvram finish");
                return true;
            }
            Log.d(TAG, " write area: " + writeArea.toString() + "  start address: " + nvramStartAddress + "  " + Arrays.toString(addressArray));
        }


        byte[] writeDate = new byte[tempDate.length + 8];
        writeDate[0] = addressArray[3];
        writeDate[1] = addressArray[2];
        writeDate[2] = addressArray[1];
        writeDate[3] = addressArray[0];

        writeDate[4] = lengthArray[3];
        writeDate[5] = lengthArray[2];
        writeDate[6] = lengthArray[1];
        writeDate[7] = lengthArray[0];
        for (int i = 0; i < writeDate.length - 8; i++) {
            writeDate[i + 8] = tempDate[i];
        }
        sendPacket(new McuPacket(McuProtocol.McuCommands.WRITE_PAGE, writeDate));

        return result;
    }

    public void broadcastUpdateStatus(int statusCommand, float percent) {
        Intent intent = new Intent("com.jht.updateio.result");
        intent.putExtra("status", statusCommand);
        intent.putExtra("percent", percent);
        context.sendBroadcast(intent);
    }
}
