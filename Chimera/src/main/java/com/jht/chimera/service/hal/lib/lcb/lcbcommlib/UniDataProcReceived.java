package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class UniDataProcReceived {
    private static final String TAG = "UniDataProcReceived";

    private ArrayList<IUniDataReceived> _receiveListenerList = new ArrayList<>();

    private UniDataProcReceived() {
    }

    private static class SingletonHolder {
        private static UniDataProcReceived Instance = new UniDataProcReceived();
    }

    public static UniDataProcReceived getInstance() {
        return SingletonHolder.Instance;
    }

    public void setReceiveListenerList(ArrayList<IUniDataReceived> uniDataReceiveListenerList) {
        _receiveListenerList = uniDataReceiveListenerList;
    }


    /*******************************************************************************
     * Function Name  : analyzeUniData(byte[] receivedData)
     * Description    : analyze UniData parameter
     * Input          : byte[] receivedData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void analyzeUniData(byte[] receivedData) {
        byte cmdByte = receivedData[0];

        int intCmdID = DataUtils.unsignedByteToInt(cmdByte);
        UniSubCommands uniDataCmd = UniSubCommands.fromInt(intCmdID);

        if (uniDataCmd == null) {
            Log.v(TAG, "UniData analyzeUniData unknown command id " + intCmdID);
            return;
        }

        switch (uniDataCmd) {
            case SetPFCmode:
                procUniSetPFCmodeHandler(receivedData);
                break;
            case MagnetAngleCal:
                procUniMagnetAngleStateHandler(receivedData);
                break;
            case McbVoltage:
                procUniMcbVoltageHandler(receivedData);
                break;				
            case GetMotoType:
                procUniGetMotoTypeHandler(receivedData);
                break;				
        }

    }

    /*******************************************************************************
     * Function Name  : procUniSetPFCmodeHandler(byte[] receivedUniData)
     * Description    : process UniData SetPFCmode handler
     * Input          : byte[] receivedUniData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procUniSetPFCmodeHandler(byte[] receivedUniData) {

        if (receivedUniData.length != UniDataConstants.RECEIVE_DATA_SIZE_CMD_23) {
            Log.v(TAG, "UniData SetPFCmode not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedUniData.length - 2];/* one is cmd byte , another is data length*/
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedUniData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        //skip data length byte
        Byte lengthByte = byteBuffer.get();

        for (int i = 0; i < (UniDataConstants.RECEIVE_DATA_SIZE_CMD_23 - 2); i++) {
            getData[i] = byteBuffer.get();
        }
        

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IUniDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.uniSetPFCmodeHandler(getData);
            }
        }

    }


    /*******************************************************************************
     * Function Name  : procUniMagnetAngleStateHandler(byte[] receivedUniData)
     * Description    : process UniData MagnetAngleState handler
     * Input          : byte[] receivedUniData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procUniMagnetAngleStateHandler(byte[] receivedUniData) {

	if(receivedUniData.length == 1)//only cmd byte , no data
		return;
	

        byte[] getData = new byte[receivedUniData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedUniData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (receivedUniData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IUniDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.uniMagnetAngleStateHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procUniMcbVoltageHandler(byte[] receivedUniData)
     * Description    : process UniData McbVoltage handler
     * Input          : byte[] receivedUniData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procUniMcbVoltageHandler(byte[] receivedUniData) {

	if(receivedUniData.length == 1)//only cmd byte , no data
		return;
	

        byte[] getData = new byte[receivedUniData.length - 1];
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedUniData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        for (int i = 0; i < (receivedUniData.length - 1); i++) {
            getData[i] = byteBuffer.get();
        }

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IUniDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.uniMcbVoltageHandler(getData);
            }
        }

    }

    /*******************************************************************************
     * Function Name  : procUniGetMotoTypeHandler(byte[] receivedUniData)
     * Description    : process UniData GetMotoType handler
     * Input          : byte[] receivedUniData
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void procUniGetMotoTypeHandler(byte[] receivedUniData) {

        if (receivedUniData.length != UniDataConstants.RECEIVE_GET_MOTO_TYPE_DATA_SIZE_CMD_03) {
            Log.v(TAG, "UniData GetMotoType not match definition size !!");
            return;
        }

        byte[] getData = new byte[receivedUniData.length - 1];/* cmd byte */
        ByteBuffer byteBuffer = ByteBuffer.wrap(receivedUniData);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        //skip command byte
        Byte cmdByte = byteBuffer.get();

        Short shortMotorType = byteBuffer.getShort();
        int intMotorType = DataUtils.shortToInt(shortMotorType);

        synchronized (_receiveListenerList) {
            int listSize = _receiveListenerList.size();
            for (int j = 0; j < listSize; j++) {
                IUniDataReceived listener = _receiveListenerList.get(j);

                if (listener != null)
                    listener.uniGetMotoTypeHandler(intMotorType);
            }
        }

    }

}
