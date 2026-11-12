package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import java.util.ArrayList;

public class DeviceDataTransmitted implements IDeviceDataTransmitted {
    private static final String TAG = "DeviceDataTransmitted";

    private static final byte BYTE_ZERO = (byte) 0x00;

    private ILcbComm mLcb = LcbComm.getInstance();
    private DeviceDataProcReceived mDeviceDataProcReceived = DeviceDataProcReceived.getInstance();

    private ArrayList<IDeviceDataReceived> _receiveListenerList = new ArrayList<>();

    private DeviceDataTransmitted() {
        mDeviceDataProcReceived.setReceiveListenerList(_receiveListenerList);
    }

    private static class SingletonHolder {
        private static DeviceDataTransmitted Instance = new DeviceDataTransmitted();
    }

    public static DeviceDataTransmitted getInstance() {
        return SingletonHolder.Instance;
    }

    /*******************************************************************************
     * Function Name  : addReceiveListener()
     * Description    : add IDeviceDataReceived Listener
     * Input          : IDeviceDataReceived
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void addReceiveListener(IDeviceDataReceived listener) {
        synchronized (_receiveListenerList) {
            _receiveListenerList.add(listener);
        }
    }

    /*******************************************************************************
     * Function Name  : removeReceiveListener()
     * Description    : remove IDeviceDataReceived Listener
     * Input          : IDeviceDataReceived
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void removeReceiveListener(IDeviceDataReceived listener) {
        synchronized (_receiveListenerList) {
            _receiveListenerList.remove(listener);
        }
    }

    /*******************************************************************************
     * Function Name  : genSetLcbDeviceData(byte[] inputData)
     * Description    : generate DeviceData parameter and setLcbDeviceData 0x50
     * Input          : byte[] inputData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void genSetLcbDeviceData(byte[] inputData, LcbComm.QueueMode mode) {
        mLcb.setLcbDeviceData(inputData, mode);
    }
    private void genSetLcbDeviceData(byte[] inputData) {genSetLcbDeviceData(inputData, LcbComm.QueueMode.AlwaysAdd);}

    private void genSetLcbDeviceDataHighestPriority(byte[] inputData) {
        mLcb.setLcbDeviceDataHighestPriority(inputData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetEEPromMemorySizes()
     * Description    : Device Data GetEEPromMemorySizes. Command 0x50 0x01
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetEEPromMemorySizes() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_01];

        sendData[0] = DeviceDataCommands.DeviceGetEEPromMemorySizes.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetEEPromMemoryContent(byte[] bytesAddress)
     * Description    : Device Data GetEEPromMemoryContent. Command 0x50 0x02
     * Input          : byte[] bytesAddress
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetEEPromMemoryContent(byte[] bytesAddress) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_02];

        sendData[0] = DeviceDataCommands.DeviceGetEEPromMemoryContent.getByteValue();
        sendData[1] = bytesAddress[0];
        sendData[2] = bytesAddress[1];

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetECBCurrent()
     * Description    : Device Data GetECBCurrent. Command 0x50 0x03
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetECBCurrent() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_03];

        sendData[0] = DeviceDataCommands.DeviceGetECBCurrent.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetEStopVale()
     * Description    : Device Data GetEStopVale. Command 0x50 0x04
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetEStopVale(LcbComm.QueueMode mode) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_04];

        sendData[0] = DeviceDataCommands.DeviceGetEStopVale.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceData(sendData, mode);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetDCBusStatus()
     * Description    : Device Data GetDCBusStatus. Command 0x50 0x05
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetDCBusStatus() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_05];

        sendData[0] = DeviceDataCommands.DeviceGetDCBusStatus.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetLCBversion()
     * Description    : Device Data GetLCBversion. Command 0x50 0x06
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetLCBversion() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_06];

        sendData[0] = DeviceDataCommands.DeviceGetLCBversion.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceData(sendData);
    }

    @Override
    public void funDeviceGetLCBversionHighestPriority() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_06];

        sendData[0] = DeviceDataCommands.DeviceGetLCBversion.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceDataHighestPriority(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetControlZoneInfo()
     * Description    : Device Data GetControlZoneInfo. Command 0x50 0x07
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetControlZoneInfo() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_07];

        sendData[0] = DeviceDataCommands.DeviceGetControlZoneInfo.getByteValue();
        sendData[1] = BYTE_ZERO;
        sendData[2] = BYTE_ZERO;

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateLcbMcuType()
     * Description    : Device Data GetUpdateLcbMcuType. Command 0x50 0x08
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateLcbMcuType() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_08];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateLcbMcuType.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateLcbModuleName()
     * Description    : Device Data GetUpdateLcbModuleName. Command 0x50 0x09
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
        public void funDeviceGetUpdateLcbModuleName() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_09];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateLcbModuleName.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateLcbBrand()
     * Description    : Device Data GetUpdateLcbBrand. Command 0x50 0x0A
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateLcbBrand() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_0A];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateLcbBrand.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateLcbBootVersion()
     * Description    : Device Data GetUpdateLcbBootVersion. Command 0x50 0x0B
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateLcbBootVersion() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_0B];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateLcbBootVersion.getByteValue();

        genSetLcbDeviceData(sendData);
    }


    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateBinAddrMcuType()
     * Description    : Device Data GetUpdateBinAddrMcuType. Command 0x50 0x0D
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateBinAddrMcuType() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_0D];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateBinAddrMcuType.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateBinAddrModuleName()
     * Description    : Device Data GetUpdateBinAddrModuleName. Command 0x50 0x0C
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateBinAddrModuleName() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_0C];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateBinAddrModuleName.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateBinAddrBrand()
     * Description    : Device Data GetUpdateBinAddrBrand. Command 0x50 0x0E
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateBinAddrBrand() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_0E];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateBinAddrBrand.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateBinAddrBootVersion()
     * Description    : Device Data GetUpdateBinAddrBootVersion. Command 0x50 0x0F
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateBinAddrBootVersion() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_0F];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateBinAddrBootVersion.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetUpdateMappingAddrBinToMcu()
     * Description    : Device Data GetUpdateMappingAddrBinToMcu. Command 0x50 0x11
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetUpdateMappingAddrBinToMcu() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_11];

        sendData[0] = DeviceDataCommands.DeviceGetUpdateMappingAddrBinToMcu.getByteValue();

        genSetLcbDeviceData(sendData);
    }


    /*******************************************************************************
     * Function Name  : funDeviceSoftwareReset()
     * Description    : Device Data SoftwareReset. Command 0x50 0x70
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceSoftwareReset() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_70];

        sendData[0] = DeviceDataCommands.DeviceSoftwareReset.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceEraseLoaderMemory(byte[] bytesAddress)
     * Description    : Device Data EraseLoaderMemory. Command 0x50 0x71
     * Input          : byte[] bytesAddress
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceEraseLoaderMemory(byte[] bytesAddress) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_71];

        sendData[0] = DeviceDataCommands.DeviceEraseLoaderMemory.getByteValue();

        for (int i = 0; i < (DeviceDataConstants.SEND_DATA_SIZE_CMD_71 - 1); i++) {
            sendData[i + 1] = bytesAddress[i];
        }

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceWriteWordLoaderData(byte[] bytesAddressData)
     * Description    : Device Data WriteWordLoaderData. Command 0x50 0x72
     * Input          : byte[] bytesAddressData //[SA][LEN][DA]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceWriteWordLoaderData(byte[] bytesAddressData) {
        byte[] sendData = new byte[bytesAddressData.length + 1];

        sendData[0] = DeviceDataCommands.DeviceWriteWordLoaderData.getByteValue();

        for (int i = 0; i < bytesAddressData.length; i++) {
            sendData[i + 1] = bytesAddressData[i];
        }

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceReadByteData(byte[] bytesAddressLen)
     * Description    : Device Data ReadByteData. Command 0x50 0x73
     * Input          : byte[] bytesAddressLen //[SA][LEN]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceReadByteData(byte[] bytesAddressLen) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_73];

        sendData[0] = DeviceDataCommands.DeviceReadByteData.getByteValue();

        for (int i = 0; i < bytesAddressLen.length; i++) {
            sendData[i + 1] = bytesAddressLen[i];
        }

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceSavePWM(byte[] bytesGroupData)
     * Description    : Device Data SavePWM. Command 0x50 0x80
     * Input          : byte[] bytesGroupData //[Group][DA]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceSavePWM(byte[] bytesGroupData) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_80];

        sendData[0] = DeviceDataCommands.DeviceSavePWM.getByteValue();

        for (int i = 0; i < bytesGroupData.length; i++) {
            sendData[i + 1] = bytesGroupData[i];
        }

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceReadPWM(byte byteGroup)
     * Description    : Device Data ReadPWM. Command 0x50 0x81
     * Input          : byte byteGroup //[Group]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceReadPWM(byte byteGroup) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_81];

        sendData[0] = DeviceDataCommands.DeviceReadPWM.getByteValue();
        sendData[1] = byteGroup;

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetLCBinformation()
     * Description    : Device Data GetLCBinformation. Command 0x50 0xF0
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetLCBinformation() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_F0];

        sendData[0] = DeviceDataCommands.DeviceGetLCBinformation.getByteValue();

        genSetLcbDeviceData(sendData);
    }

    @Override
    public void funDeviceGetLCBinformationHighestPriority() {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_F0];

        sendData[0] = DeviceDataCommands.DeviceGetLCBinformation.getByteValue();

        genSetLcbDeviceDataHighestPriority(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceSetMcbLcbParameter(byte[] bytesTypeData)
     * Description    : Device Data SetMcbLcbParameter. Command 0x50 0x90
     * Input          : byte[] bytesTypeData //[Type][LEN][DA]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceSetMcbLcbParameter(byte[] bytesTypeData) {
        byte[] sendData = new byte[bytesTypeData.length + 1];

        sendData[0] = DeviceDataCommands.DeviceSetMcbLcbParameter.getByteValue();

        for (int i = 0; i < bytesTypeData.length; i++) {
            sendData[i + 1] = bytesTypeData[i];
        }

        genSetLcbDeviceData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funDeviceGetMcbLcbParameter(byte byteType)
     * Description    : Device Data GetMcbLcbParameter. Command 0x50 0x91
     * Input          : byte byteType //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funDeviceGetMcbLcbParameter(byte byteType) {
        byte[] sendData = new byte[DeviceDataConstants.SEND_DATA_SIZE_CMD_91];

        sendData[0] = DeviceDataCommands.DeviceGetMcbLcbParameter.getByteValue();
        sendData[1] = byteType;

        genSetLcbDeviceData(sendData);
    }


}
