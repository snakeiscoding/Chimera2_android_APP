package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DeviceDataProcReceived {
    private static final String TAG = "DeviceDataProcReceived";

    private ArrayList<IDeviceDataReceived> _receiveListenerList = new ArrayList<>();

    private DeviceDataProcReceived() {
    }

    private static class SingletonHolder {
        private static DeviceDataProcReceived Instance = new DeviceDataProcReceived();
    }

    public static DeviceDataProcReceived getInstance() {
        return SingletonHolder.Instance;
    }

    public void setReceiveListenerList(ArrayList<IDeviceDataReceived> deviceDataReceiveListenerList) {
        _receiveListenerList = deviceDataReceiveListenerList;
    }


    /*******************************************************************************
     * Function Name  : analyzeDeviceData(byte[] receivedData)
     * Description    : analyze DeviceData parameter
     * Input          : byte[] receivedData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void analyzeDeviceData(byte[] receivedData) {
        byte cmdByte = receivedData[0];

        int intCmdID = DataUtils.unsignedByteToInt(cmdByte);
        DeviceDataCommands deviceDataCmd = DeviceDataCommands.fromInt(intCmdID);

        switch (deviceDataCmd) {
            case DeviceGetEEPromMemorySizes:
                procDeviceGetEEPromMemorySizesHandler(receivedData);
                break;
            case DeviceGetEEPromMemoryContent:
                procDeviceGetEEPromMemoryContentHandler(receivedData);
                break;
            case DeviceGetECBCurrent:
                procDeviceGetECBCurrentHandler(receivedData);
                break;
            case DeviceGetEStopVale:
                procDeviceGetEStopValeHandler(receivedData);
                break;
            case DeviceGetDCBusStatus:
                procDeviceGetDCBusStatusHandler(receivedData);
                break;
            case DeviceGetLCBversion:
                procDeviceGetLCBversionHandler(receivedData);
                break;
            case DeviceGetControlZoneInfo:
                procDeviceGetControlZoneInfoHandler(receivedData);
                break;
            case DeviceGetUpdateLcbMcuType:
                procDeviceGetUpdateLcbMcuTypeHandler(receivedData);
                break;
            case DeviceGetUpdateLcbModuleName:
                procDeviceGetUpdateLcbModuleNameHandler(receivedData);
                break;
            case DeviceGetUpdateLcbBrand:
                procDeviceGetUpdateLcbBrandHandler(receivedData);
                break;
            case DeviceGetUpdateLcbBootVersion:
                procDeviceGetUpdateLcbBootVersionHandler(receivedData);
                break;
            case DeviceGetUpdateBinAddrMcuType:
                procDeviceGetUpdateBinAddrMcuTypeHandler(receivedData);
                break;
            case DeviceGetUpdateBinAddrModuleName:
                procDeviceGetUpdateBinAddrModuleNameHandler(receivedData);
                break;
            case DeviceGetUpdateBinAddrBrand:
                procDeviceGetUpdateBinAddrBrandHandler(receivedData);
                break;
            case DeviceGetUpdateBinAddrBootVersion:
                procDeviceGetUpdateBinAddrBootVersionHandler(receivedData);
                break;
            case DeviceGetUpdateMappingAddrBinToMcu:
                procDeviceGetUpdateMappingAddrBinToMcuHandler(receivedData);
                break;
            case DeviceSoftwareReset:
                procDeviceSoftwareResetHandler(receivedData);
                break;
            case DeviceEraseLoaderMemory:
                procDeviceEraseLoaderMemoryHandler(receivedData);
                break;
            case DeviceWriteWordLoaderData:
                procDeviceWriteWordLoaderDataHandler(receivedData);
                break;
            case DeviceReadByteData:
                procDeviceReadByteDataHandler(receivedData);
                break;
            case DeviceSavePWM:
                procDeviceSavePWMHandler(receivedData);
                break;
            case DeviceReadPWM:
                procDeviceReadPWMHandler(receivedData);
                break;
            case DeviceGetLCBinformation:
                procDeviceGetLCBinformationHandler(receivedData);
                break;
            case DeviceSetMcbLcbParameter:
                procDeviceSetMcbLcbParameterHandler(receivedData);
                break;
            case DeviceGetMcbLcbParameter:
                procDeviceGetMcbLcbParameterHandler(receivedData);
                break;
        }

    }


    /*******************************************************************************
     * Function Name  : procDeviceGetEEPromMemorySizesHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetEEPromMemorySizes handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetEEPromMemorySizesHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_01) {
            Log.v(TAG, "DeviceData GetEEPromMemorySizes not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_01 - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        int memorySizes = DataUtils.unsignedBytesToInt((byte) getData[1], (byte) getData[0], (byte) 0x00, (byte) 0x00);

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetEEPromMemorySizesHandler(memorySizes);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetEEPromMemoryContentHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetEEPromMemoryContent handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetEEPromMemoryContentHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_02) {
            Log.v(TAG, "DeviceData GetEEPromMemoryContent not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_02 - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetEEPromMemoryContentHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetECBCurrentHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetECBCurrent handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetECBCurrentHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_03) {
            Log.v(TAG, "DeviceData GetECBCurrent not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_03 - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetECBCurrentHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetEStopValeHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetEStopVale handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetEStopValeHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_04) {
            Log.v(TAG, "DeviceData GetEStopVale not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_04 - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetEStopValeHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetDCBusStatusHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetDCBusStatus handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetDCBusStatusHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_05) {
            Log.v(TAG, "DeviceData GetDCBusStatus not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_05 - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetDCBusStatusHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetLCBversionHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetLCBversion handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetLCBversionHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetLCBversionHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetControlZoneInfoHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetControlZoneInfo handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetControlZoneInfoHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        //get the number of control zone
        Byte byteControlZoneCount = byteBuffer.get();
        int intControlZoneCount = DataUtils.unsignedByteToInt(byteControlZoneCount);

        byte[] getData = new byte[(intControlZoneCount * 2 + 1)]; //one control zone use 2 bytes + ControlZoneCount byte

        getData[0] = byteControlZoneCount;

        for (int i = 0; i < intControlZoneCount; i++) {
            getData[2 * i + 1] = byteBuffer.get();
            getData[2 * i + 2] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetControlZoneInfoHandler(getData);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateLcbMcuTypeHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateLcbMcuType handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateLcbMcuTypeHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null) {
                    listener.deviceGetUpdateLcbMcuTypeHandler(getData);
                }
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateLcbModuleNameHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateLcbModuleName handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateLcbModuleNameHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateLcbModuleNameHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateLcbBrandHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateLcbBrand handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateLcbBrandHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateLcbBrandHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateLcbBootVersionHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateLcbBootVersion handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateLcbBootVersionHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateLcbBootVersionHandler(getData);
            }
        }

    }
    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateBinAddrMcuTypeHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateBinAddrMcuType handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateBinAddrMcuTypeHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0D) {
            Log.v(TAG, "DeviceData GetUpdateBinAddrMcuType not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0D - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateBinAddrMcuTypeHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateBinAddrModuleNameHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateBinAddrModuleName handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateBinAddrModuleNameHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0C) {
            Log.v(TAG, "DeviceData GetUpdateBinAddrModuleName not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0C - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateBinAddrModuleNameHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateBinAddrBrandHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateBinAddrBrand handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateBinAddrBrandHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0E) {
            Log.v(TAG, "DeviceData GetUpdateBinAddrBrand not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0E - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateBinAddrBrandHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateBinAddrBootVersionHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateBinAddrBootVersion handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateBinAddrBootVersionHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0F) {
            Log.v(TAG, "DeviceData GetUpdateBinAddrBootVersion not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_0F - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateBinAddrBootVersionHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetUpdateMappingAddrBinToMcuHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetUpdateMappingAddrBinToMcu handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetUpdateMappingAddrBinToMcuHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetUpdateMappingAddrBinToMcuHandler(getData);
            }
        }

    }


    /*******************************************************************************
     * Function Name  : procDeviceSoftwareResetHandler()
     * Description    : process DeviceData SoftwareReset handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceSoftwareResetHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_70) {
            Log.v(TAG, "DeviceData SoftwareReset not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceSoftwareResetHandler();
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procDeviceEraseLoaderMemoryHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData EraseLoaderMemory handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceEraseLoaderMemoryHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_71) {
            Log.v(TAG, "DeviceData EraseLoaderMemory not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        Byte byteStatus = byteBuffer.get();


        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceEraseLoaderMemoryHandler(byteStatus);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : procDeviceWriteWordLoaderDataHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData WriteWordLoaderData handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceWriteWordLoaderDataHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_72) {
            Log.v(TAG, "DeviceData WriteWordLoaderData not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        Byte byteStatus = byteBuffer.get();


        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceWriteWordLoaderDataHandler(byteStatus);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceReadByteDataHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData ReadByteData handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceReadByteDataHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        Byte byteStatus = byteBuffer.get();

        //get the length of data
        Byte byteLenOfData = byteBuffer.get();
        int intLenOfData = DataUtils.unsignedByteToInt(byteLenOfData);

        byte[] getData = new byte[(intLenOfData + 2)]; //Status+ LenOfData byte +data

        getData[0] = byteStatus;
        getData[1] = byteLenOfData;

        for (int i = 0; i < intLenOfData; i++) {
            getData[i + 2] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceReadByteDataHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceSavePWMHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData SavePWM handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceSavePWMHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_80) {
            Log.v(TAG, "DeviceData SavePWM not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        Byte byteStatus = byteBuffer.get();


        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceSavePWMHandler(byteStatus);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceReadPWMHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData ReadPWM handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceReadPWMHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_81) {
            Log.v(TAG, "DeviceData ReadPWM not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedDeviceData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_81 - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceReadPWMHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetLCBinformationHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetLCBinformation handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetLCBinformationHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }


        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetLCBinformationHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceSetMcbLcbParameterHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData SetMcbLcbParameter handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceSetMcbLcbParameterHandler(byte[] receivedDeviceData) {

        if (receivedDeviceData.length != DeviceDataConstants.RECEIVE_DATA_SIZE_CMD_90) {
            Log.v(TAG, "DeviceData SetMcbLcbParameter not match definition size !!");
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        Byte byteStatus = byteBuffer.get();


        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceSetMcbLcbParameterHandler(byteStatus);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procDeviceGetMcbLcbParameterHandler(byte[] receivedDeviceData)
     * Description    : process DeviceData GetMcbLcbParameter handler
     * Input          : byte[] receivedDeviceData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procDeviceGetMcbLcbParameterHandler(byte[] receivedDeviceData) {

        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedDeviceData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        byte[] getData = new byte[receivedDeviceData.length - 1];

        for (int i = 0; i < (receivedDeviceData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }


        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IDeviceDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.deviceGetMcbLcbParameterHandler(getData);
            }
        }

    }

}
