package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import java.util.ArrayList;

public class UniDataTransmitted implements IUniDataTransmitted {
    private static final String TAG = "UniDataTransmitted";

    private static final byte BYTE_ZERO = (byte) 0x00;

    private ILcbComm mLcb = LcbComm.getInstance();
    private UniDataProcReceived mUniDataProcReceived = UniDataProcReceived.getInstance();

    private ArrayList<IUniDataReceived> _receiveListenerList = new ArrayList<>();

    private UniDataTransmitted() {
        mUniDataProcReceived.setReceiveListenerList(_receiveListenerList);
    }

    private static class SingletonHolder {
        private static UniDataTransmitted Instance = new UniDataTransmitted();
    }

    public static UniDataTransmitted getInstance() {
        return SingletonHolder.Instance;
    }

    /*******************************************************************************
     * Function Name  : addReceiveListener()
     * Description    : add IUniDataReceived Listener
     * Input          : IUniDataReceived
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void addReceiveListener(IUniDataReceived listener) {
        synchronized (_receiveListenerList) {
            _receiveListenerList.add(listener);
        }
    }

    /*******************************************************************************
     * Function Name  : removeReceiveListener()
     * Description    : remove IUniDataReceived Listener
     * Input          : IUniDataReceived
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void removeReceiveListener(IUniDataReceived listener) {
        synchronized (_receiveListenerList) {
            _receiveListenerList.remove(listener);
        }
    }

    /*******************************************************************************
     * Function Name  : funUniGetMotoType()
     * Description    : Uni Data GetMotoType. Command 0xAA 0x03
     * Input          : byte byteState //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funUniGetMotoType() {
        byte[] sendData = new byte[UniDataConstants.SEND_GET_MOTO_TYPE_DATA_SIZE_CMD_03];

        sendData[0] = UniSubCommands.GetMotoType.getByteValue();

        genSetLcbUniData(sendData);
    }


    /*******************************************************************************
     * Function Name  : genSetLcbUniData(byte[] inputData)
     * Description    : generate UniData parameter and setLcbUniData 0xAA
     * Input          : byte[] inputData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void genSetLcbUniData(byte[] inputData, LcbComm.QueueMode mode) {
        mLcb.setLcbUniversal(inputData, mode);
    }
    private void genSetLcbUniData(byte[] inputData) {genSetLcbUniData(inputData, LcbComm.QueueMode.AlwaysAdd);}

    /*******************************************************************************
     * Function Name  : funUniSetPFCmode(byte byteState)
     * Description    : Uni Data GetMcbLcbParameter. Command 0xAA 0x23
     * Input          : byte byteState //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funUniSetPFCmode(byte byteState) {
        byte[] sendData = new byte[UniDataConstants.SEND_DATA_SIZE_CMD_23];

        sendData[0] = UniSubCommands.SetPFCmode.getByteValue();
        sendData[1] = (byte) 0x01; /* Read : 0 / Write : 1 */
        sendData[2] = byteState;

        genSetLcbUniData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funUniSetMagnetAngleState(byte byteState)
     * Description    : Uni Data SetMcbLcbParameter. Command 0xAA 0x24
     * Input          : byte byteState //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funUniSetMagnetAngleState(byte byteState) {
        byte[] sendData = new byte[UniDataConstants.SEND_SET_MAGNET_ANGLE_DATA_SIZE_CMD_24];

        sendData[0] = UniSubCommands.MagnetAngleCal.getByteValue();
        sendData[1] = (byte) 0x01; /* Read : 0 / Write : 1 */
        sendData[2] = byteState;

        genSetLcbUniData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funUniGetMagnetAngle()
     * Description    : Uni Data SetMcbLcbParameter. Command 0xAA 0x24
     * Input          : byte byteState //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funUniGetMagnetAngle(LcbComm.QueueMode mode) {
        byte[] sendData = new byte[UniDataConstants.SEND_GET_MAGNET_ANGLE_DATA_SIZE_CMD_24];

        sendData[0] = UniSubCommands.MagnetAngleCal.getByteValue();
        sendData[1] = (byte) 0x00; /* Read : 0 / Write : 1 */

        genSetLcbUniData(sendData, mode);
    }

    /*******************************************************************************
     * Function Name  : funUniSetMcbVoltage(byte byteData)
     * Description    : Uni Data SetMcbLcbParameter. Command 0xAA 0x25
     * Input          : byte byteData //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funUniSetMcbVoltage(byte byteData) {
        byte[] sendData = new byte[UniDataConstants.SEND_SET_MCB_VOLTAGE_DATA_SIZE_CMD_25];

        sendData[0] = UniSubCommands.McbVoltage.getByteValue();
        sendData[1] = (byte) 0x01; /* Read : 0 / Write : 1 */
        sendData[2] = byteData;

        genSetLcbUniData(sendData);
    }

    /*******************************************************************************
     * Function Name  : funUniGetMcbVoltage()
     * Description    : Uni Data SetMcbLcbParameter. Command 0xAA 0x25
     * Input          : LcbComm.QueueMode //[Type]
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void funUniGetMcbVoltage(LcbComm.QueueMode mode) {
        byte[] sendData = new byte[UniDataConstants.SEND_GET_MCB_VOLTAGE_DATA_SIZE_CMD_25];

        sendData[0] = UniSubCommands.McbVoltage.getByteValue();
        sendData[1] = (byte) 0x00; /* Read : 0 / Write : 1 */

        genSetLcbUniData(sendData, mode);
    }
	
}
