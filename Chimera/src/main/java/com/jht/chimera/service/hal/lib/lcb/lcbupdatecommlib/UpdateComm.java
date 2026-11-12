package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

import android.util.Log;

import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.DataUtils;
import com.jht.serialport.ISerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;


public class UpdateComm implements ILcbUpdateComm {
    private static final String TAG = "UpdateComm";
    private UpdatePacketQueue _txPacketQueue = new UpdatePacketQueue();
    private ArrayList<ILcbUpdateCommReceive> _receiveListenerUpdateCommList = new ArrayList<>();
    private UpdatePacket _receivePacket = new UpdatePacket();
    private UpdatePacketQueue _rxPacketQueue = new UpdatePacketQueue();
    private Object _notifyTxPacketSync = new Object();
    private Object _notifyRxPacketSync = new Object();
    private volatile boolean _isPortOpened = false;
    private volatile boolean _isRunning = false;
    private ISerialPort _port = null;
    private InputStream _inStream = null;
    private OutputStream _outStream = null;
    //    private static final int RX_BUFFER_SIZE = 1024;
    private static final int RX_BUFFER_SIZE = 256;
    private static final int RECEIVE_TIMEOUT_MS = 10000;
    private static final int RETRY_DELAY_TIME_MS = 50;
    private static final int LIMITED_RETRY_COUNTER = 3; //all command retry limited
    private int _retryTotalCounter = 0;
    private final static int COMM_LCB_ERROR = 0x04B0;
    private static final int COMMUNICATION_TIMEOUT_MS = 11000;
    private boolean _commUpdateTimeout = false;
    private int _communicationRetries = 3; //every command retry times

    private boolean _isStartParse = false;
    private volatile boolean _isReceiverEnabled = false;

    private UpdateCommands _txCommand = UpdateCommands.CmdNone;
    private UpdateCommands _txRtnCommand = UpdateCommands.CmdNone;
    private UpdateCommands _rxCommand = UpdateCommands.CmdNone;

    private static Thread _updateCommThread = null;
    private static Thread _rxThread = null;

    private static final byte _SFC_ = (byte) 0xD0;
    private static final byte _Start_ = (byte) 0xD1;
    private static final byte _End_ = (byte) 0xD2;
    private static final byte _REV_ = (byte) 0xD3;

    private static final byte _Success_ = (byte) 0x01;
    private static final byte _Fail_ = (byte) 0x00;

//    private static boolean _bStartCodeFlag = false;

    private static final int RECEIVE_DATA_SIZE_RETURN_ERASE_FLASH = 1;
    private static final int RECEIVE_DATA_SIZE_RETURN_WRITE_FLASH = 9;
    private static final int RECEIVE_DATA_SIZE_RETURN_PROGRAM_STATE = 1;
    private static final int RECEIVE_DATA_SIZE_RETURN_FLASH_STATE = 1;
    private static final int RECEIVE_DATA_SIZE_RETURN_CHECK_CODE = 16;
    private static final int RECEIVE_DATA_SIZE_RETURN_UPDATE_MODE = 1;

    private static final int CHECK_CODE_DATA_LEN = 15;

    private byte _writeFlashDataCRC = (byte) 0x00;
    private int _writeFlashAddr = 0x00000000;
    private int _readFlashAddr = 0x00000000;
    private int _readFlashLength = 0;

    public enum ProcessCmdError {
        CMD_SUCCESS,
        CMD_ERROR_DATA_LEN_NOT_MATCH,
        CMD_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH,
        CMD_ERROR_NEW_PACKET_NULL
    }

    public enum ProcessReturnCmdError {
        CMD_RTN_SUCCESS,
        CMD_RTN_ERROR_DATA_LEN,
        CMD_RTN_ERROR_WRITE_DATA_CRC,
        CMD_RTN_ERROR_WRITE_DATA_ADDR,
        CMD_RTN_ERROR_READ_DATA_ADDR,
        CMD_RTN_ERROR_READ_DATA_LEN
    }

    private UpdateComm() {
    }

    private static class SingletonHolder {
        private static UpdateComm Instance = new UpdateComm();
    }

    public static UpdateComm getInstance() {
        return SingletonHolder.Instance;
    }


    /*******************************************************************************
     * Function Name  : addReceiveListener()
     * Description    : add ILcbUpdateCommReceive Listener
     * Input          : ILcbUpdateCommReceive
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void addReceiveListener(ILcbUpdateCommReceive listener) {
        _receiveListenerUpdateCommList.add(listener);
    }

    /*******************************************************************************
     * Function Name  : removeReceiveListener()
     * Description    : remove ILcbUpdateCommReceive Listener
     * Input          : ILcbUpdateCommReceive
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void removeReceiveListener(ILcbUpdateCommReceive listener) {
        _receiveListenerUpdateCommList.remove(listener);
    }

    /*******************************************************************************
     * Function Name  : start()
     * Description    : serial port start and start _updateCommThread & _rxThread
     * Input          : SerialPort device, String path, int baudRate
     * Output         : None
     * Return         : true : open port success , false : open port fail
     *******************************************************************************/
    @Override
    public boolean start(ISerialPort device) {
        if (_isPortOpened) {
            Log.w(TAG, "The LCB update communication serial port had already opened!");
            return true;
        }

        if (_isRunning) {
            Log.w(TAG, "Update Communication thread already running");
            return true;
        }

        if (null != device) {
            _port = device;
            try {
                _port.open();
            } catch (IOException e) {
                Log.w(TAG, e);
                return false;
            } catch (SecurityException e2) {
                Log.w(TAG, e2);
                return false;
            }
            _outStream = _port.getOutputStream();
            _inStream = _port.getInputStream();
            if ((null == _inStream) || (null == _outStream)) {
                return false;
            }

            // remove all packets from queue
            _txPacketQueue.clear();
            _rxPacketQueue.clear();

            _updateCommThread = new CommunicationThread();
            _rxThread = new LcbRxThread();

            _updateCommThread.start();
            _rxThread.start();

            _isPortOpened = true;
        } else {
            Log.e(TAG, "serialPort instance is null!");
            return false;
        }

        return true;
    }

    /*******************************************************************************
     * Function Name  : stop()
     * Description    : serial port stop
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    @Override
    public void stop() {
        if (_isPortOpened) {
            if (_isRunning) {
                _isRunning = false;
                _updateCommThread.interrupt();
                _rxThread.interrupt();

                // remove all packets from queue
                _txPacketQueue.clear();
                _rxPacketQueue.clear();

                // wake receive wait in thread
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.notifyAll();
                }
                // wake thread
                synchronized (_notifyTxPacketSync) {
                    _notifyTxPacketSync.notifyAll();
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                _updateCommThread = null;
                _rxThread = null;
            }

            try {
                _port.close();
            } catch (IOException ignored) {
            }
            _port = null;

            _isPortOpened = false;
        }
    }

    /*******************************************************************************
     * Function Name  : receiveDataHandler()
     * Description    : receive uart port data byte array
     * Input          : byte array
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void receiveDataHandler(byte[] data, int length) {
        // append and parse received data
        UpdatePacket.ParseUpdateResult parseUpdateResult;

        for (int index = 0; index < length; index++) {
            Byte b = data[index];
            parseUpdateResult = _receivePacket.parseUpdate(b);

            if ((parseUpdateResult == UpdatePacket.ParseUpdateResult.InvalidStartCode)) {
                Log.e(TAG, "parseUpdateResult = " + parseUpdateResult);
                resetRxPacket();
            } else if (parseUpdateResult == UpdatePacket.ParseUpdateResult.Success) {
                //reset communication update timeout
                _commUpdateTimeout = false;

                _rxPacketQueue.add(_receivePacket.clone());
                synchronized (_notifyRxPacketSync) {
                    _notifyRxPacketSync.notify();
                }
                resetRxPacket();
            }
        }
    }

    private class LcbRxThread extends Thread {
        LcbRxThread() { super("LcbUpTxThread"); }
        byte[] rxBuffer = new byte[RX_BUFFER_SIZE];
        int bytes = 0;

        @Override
        public void run() {
            super.run();

            _isRunning = true;

            while (_isRunning && !isInterrupted()) {
                try {
                    bytes = _inStream.read(rxBuffer);

                    if (bytes < 0)
                        Log.e(TAG, "Update Read error!");
                    else {
                        if (getReceiverEnable())
                            receiveDataHandler(rxBuffer, bytes);
                    }

                } catch (IOException e) {
                    _isRunning = false;
                    Log.e(TAG, "Update UART read I/O exception: ", e);
                } catch (Exception ex) {
                    _isRunning = false;
                    Log.e(TAG, "Exception in Update LcbRxThread ! ", ex);
                }
            }
        }
    }


    /*******************************************************************************
     * Function Name  : CommunicationThread
     * Description    : Communication Thread class
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private class CommunicationThread extends Thread {
        CommunicationThread() { super("LcbUpCommThread"); }
        @Override
        public void run() {
            super.run();

            _isRunning = true;

            try {
                while (_isRunning && !isInterrupted()) {
                    // wait if nothing to send
                    if (_txPacketQueue.size() == 0) {
                        synchronized (_notifyTxPacketSync) {
                            _commUpdateTimeout = true;
                            //_notifyTxPacketSync.wait();
                            _notifyTxPacketSync.wait(COMMUNICATION_TIMEOUT_MS);
                            if ((_commUpdateTimeout == true) && (_txPacketQueue.size() == 0)) {
                                Log.e(TAG, "update lcb communication timeout (5 sec) error!");
                                procCommTimeOutErrorHandler();
                            }
                        }
                    }


                    if (_txPacketQueue.size() > 0) {
                        // dequeue packet
                        UpdatePacket packet = _txPacketQueue.remove();

                        int retries = 0;
                        while ((retries < _communicationRetries) && (packet != null)) {
                            setReceiverEnable(true);
                            byte[] txPacket = packet.buildTxPacket();

                            // reset rx packet
                            resetRxPacket();

                            // write packet
                            _outStream.write(txPacket);

                            //set _txCommand
                            _txCommand = packet.getCommand();

                            //set _txRtnCommand
                            _txRtnCommand = packet.getRtnCommand();

                            // wait for reply
                            synchronized (_notifyRxPacketSync) {
                                _notifyRxPacketSync.wait(RECEIVE_TIMEOUT_MS);
                            }
                            // break if closing
                            if (!_isRunning) {
                                break;
                            }
                            // process packet if available
                            if (_rxPacketQueue.size() > 0) {
                                UpdatePacket rxPacket = _rxPacketQueue.remove();

                                //set _rxCommand
                                _rxCommand = rxPacket.getCommand();
                                //check _txRtnCommand and _rxCommand are same
                                //reset retry total counter limited
                                _retryTotalCounter = 0;
                                if (_txRtnCommand.Value != _rxCommand.Value) {//_txRtnCommand  _rx Command not same Retry
//                                    Log.d(TAG, String.format("Rx = %x ,TxRtn = %x command not same , retry.", _rxCommand.Value, _txRtnCommand.Value));
                                    //rxPacket command error retry delay 50 ms
                                    Thread.sleep(RETRY_DELAY_TIME_MS);
                                } else {
                                    processPacket(rxPacket);
                                    break;
                                }
                            } else {
                                Log.e(TAG, "Tx[" + _txCommand + "] No result, retrying..." + retries);
                                //timeout or no rxPacket retry delay 50 ms
                                try {
                                    Thread.sleep(RETRY_DELAY_TIME_MS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                            //counter total tries
                            _retryTotalCounter++;

                            retries++; //for one command
                            setReceiverEnable(false); // discard any unwanted received noise packets
                        }
                        if (retries >= _communicationRetries) {
                            Log.e(TAG, String.format("Update One command retry 3 times Tx = %x communication failed!", _txCommand.Value));
                        }
                        if (_retryTotalCounter >= LIMITED_RETRY_COUNTER) {
                            Log.e(TAG, "Update All command retry 3 times , Communication error!");
                            procCommTimeOutErrorHandler();
                        }

                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                _isRunning = false;
                Log.e(TAG, "Update Exception in communication thread! ", ex);
            }

            Log.v(TAG, "Update CommunicationThread-");
        }
    }

    private synchronized void setReceiverEnable(boolean enabled) {
        _isReceiverEnabled = enabled;
    }

    private synchronized boolean getReceiverEnable() {
        return _isReceiverEnabled;
    }

    /*******************************************************************************
     * Function Name  : resetRxPacket
     * Description    : reset _receivePacket
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void resetRxPacket() {
        // reset rx packet
        _receivePacket.reset();
    }

    /*******************************************************************************
     * Function Name  : processPacket
     * Description    : process receive complete Packet
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void processPacket(UpdatePacket packet) {
        UpdateCommands command = packet.getCommand();
        ProcessReturnCmdError rtnReceiveCmdError = ProcessReturnCmdError.CMD_RTN_SUCCESS;

        switch (command) {
            case CmdReturnEraseFlash:
                rtnReceiveCmdError = procReturnEraseFlashHandler(packet);
                break;
            case CmdReturnWriteFlash:
                rtnReceiveCmdError = procReturnWriteFlashHandler(packet);
                break;
            case CmdReturnReadFlash:
                rtnReceiveCmdError = procReturnReadFlashHandler(packet);
                break;
            case CmdReturnProgramState:
                rtnReceiveCmdError = procReturnProgramStateHandler(packet);
                break;
            case CmdReturnFlashState:
                rtnReceiveCmdError = procReturnFlashStateHandler(packet);
                break;
            case CmdReturnCheckCode:
                rtnReceiveCmdError = procReturnCheckCodeHandler(packet);
                break;
            case CmdReturnProduceID:
                rtnReceiveCmdError = procReturnProduceIDHandler(packet);
                break;
            case CmdReturnMcuType:
                rtnReceiveCmdError = procReturnMcuTypeHandler(packet);
                break;
            case CmdReturnMcuID:
                rtnReceiveCmdError = procReturnMcuIDHandler(packet);
                break;
            case CmdReturnUpdateMode:
                rtnReceiveCmdError = procReturnUpdateModeHandler(packet);
                break;
        }

        if (rtnReceiveCmdError != ProcessReturnCmdError.CMD_RTN_SUCCESS) {
            for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
                listener.processRtnReceiveCmdErrorHandler(rtnReceiveCmdError);
            }
        }
    }

    /*******************************************************************************
     * Function Name  : sendPacket
     * Description    : send Packet
     * Input          : LcbPacket
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void sendPacket(UpdatePacket packet) {
        _txPacketQueue.add(packet);
        synchronized (_notifyTxPacketSync) {
            _notifyTxPacketSync.notify();
        }
    }
/*
    private static int checkReceiveDataStartByte(byte[] data) {
        int rtnStartByte = -1;//-1 mean no StartByte

        for (int i = 0; i < data.length; i++) {

            if (i < (data.length - 1)) {    //check data[0]~data[data.length-1] byte
                if (data[i] == _Start_) {    //normal check data[i]   |XXD1XXXXD2 | or |XXD1|XXXX|XXD2
                    rtnStartByte = i;
                    _bStartCodeFlag = true;
                    return rtnStartByte;
                }

            } else {//check data[data.length-1] byte
                if ((i == (data.length - 1)) && (_bStartCodeFlag == false) && (data[i] == _Start_)) {    //exception check  |XXXXXXD1|and |XXXX|XXD2   data[data.length - 1] == D1
                    rtnStartByte = i;
                    _bStartCodeFlag = true;
                    return rtnStartByte;
                }
            }

        }

        return -1;
    }
*/

    /*******************************************************************************
     * Function Name  : procCommTimeOutErrorHandler()
     * Description    :process CommunicationThread reveive timeout and communication timeout error (0x04B0) handler
     * Input          : None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private void procCommTimeOutErrorHandler() {
        int intLcbErrorCode = COMM_LCB_ERROR;

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.commTimeoutErrorHandler(intLcbErrorCode);
        }
    }

    /*******************************************************************************
     * Function Name  : lcbEraseFlashProcess()
     * Description    : LCB EraseFlash Process 0x10
     * Input          : int EraseAddr, int EraseLength , byte LCB_type
     * Output         : ReturnEraseFlash 0x11
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbEraseFlashProcess(int EraseAddr, int EraseLength) {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdEraseFlash, UpdateCommands.CmdReturnEraseFlash);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        //set data
        byte[] data = new byte[8]; //Start address : 4	Length : 4
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte byteData;

        for (int i = 0; i < 4; i++) {
            byteData = (byte) (EraseAddr & 0x000000FF);
            byteBuffer.put((byte) byteData);
            EraseAddr >>= 8;
        }

        for (int j = 0; j < 4; j++) {
            byteData = (byte) (EraseLength & 0x000000FF);
            byteBuffer.put((byte) byteData);
            EraseLength >>= 8;
        }

        packet.setData(data);
        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnEraseFlashHandler()
     * Description    : LCB ReturnEraseFlash Process 0x11
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnEraseFlashHandler(UpdatePacket packet) {
        byte[] data = packet.getData();
        boolean isEraseOK = false;

        if (data.length != RECEIVE_DATA_SIZE_RETURN_ERASE_FLASH)
            return ProcessReturnCmdError.CMD_RTN_ERROR_DATA_LEN;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        Byte eraseState = byteBuffer.get();

        if (eraseState == (byte) 0x00) {
            //Erase Fail
            isEraseOK = false;
        } else if (eraseState == (byte) 0x01) {
            //Erase OK
            isEraseOK = true;
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnEraseFlashHandler(isEraseOK);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbWriteFlashProcess()
     * Description    : LCB WriteFlash Process 0x12
     * Input          : int WriteAddr, int WriteLength , byte[] WriteData
     * Output         : ReturnWriteFlash 0x13
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbWriteFlashProcess(int WriteAddr, int WriteLength, byte[] WriteData) {
        //check WriteData length equal to WriteLength
        if (WriteData.length != WriteLength) {
            return ProcessCmdError.CMD_ERROR_DATA_LEN_NOT_MATCH;
        }

        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdWriteFlash, UpdateCommands.CmdReturnWriteFlash);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        int CurrAdrr = 0;

        CurrAdrr = WriteAddr;

        //set data
        byte[] data = new byte[4 + WriteLength]; //Start address : 4	Length : WriteLength
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte byteData;

        for (int i = 0; i < 4; i++) {
            //==> Address
            byteData = (byte) (CurrAdrr & 0x000000FF);
            byteBuffer.put((byte) byteData);
            CurrAdrr >>= 8;
        }

        for (int j = 0; j < WriteLength; j++) {
            //==>Data
            byteData = (byte) (WriteData[j]);
            byteBuffer.put((byte) byteData);
        }

        //calculatedCrc
        _writeFlashDataCRC = UpdateCrc8.doUpdateCrc8(WriteData, WriteData.length);

        //set _writeFlashAddr
        _writeFlashAddr = WriteAddr;

        packet.setData(data);
        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnWriteFlashHandler()
     * Description    : LCB ReturnWriteFlash Process 0x13
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnWriteFlashHandler(UpdatePacket packet) {
        byte[] data = packet.getData();

        if (data.length != RECEIVE_DATA_SIZE_RETURN_WRITE_FLASH)
            return ProcessReturnCmdError.CMD_RTN_ERROR_DATA_LEN;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] address = new byte[4];
        byte[] dataLength = new byte[4];

        for (int i = 0; i < 4; i++) {
            address[i] = byteBuffer.get();
        }
        int intAddress = DataUtils.unsignedBytesToInt((byte) address[0], (byte) address[1], (byte) address[2], (byte) address[3]);

        for (int j = 0; j < 4; j++) {
            dataLength[j] = byteBuffer.get();
        }
        int intDataLength = DataUtils.unsignedBytesToInt((byte) dataLength[0], (byte) dataLength[1], (byte) dataLength[2], (byte) dataLength[3]);

        byte crcData = byteBuffer.get();

        // Compare CRC
        if (crcData == _writeFlashDataCRC) {
            // Commpare Address
            if (intAddress != _writeFlashAddr) {
                return ProcessReturnCmdError.CMD_RTN_ERROR_WRITE_DATA_ADDR;
            }
        } else {
            return ProcessReturnCmdError.CMD_RTN_ERROR_WRITE_DATA_CRC;
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnWriteFlashHandler();
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbReadFlashProcess()
     * Description    : LCB ReadFlash Process 0x14
     * Input          : int ReadAddr, int ReadLength
     * Output         : ReturnReadFlash 0x15
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbReadFlashProcess(int ReadAddr, int ReadLength) {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdReadFlash, UpdateCommands.CmdReturnReadFlash);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        int CurrAdrr = 0, CurrLength;

        CurrAdrr = ReadAddr;
        CurrLength = ReadLength;

        //set data
        byte[] data = new byte[8]; //Start address : 4	Length : 4
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte byteData;

        //Address
        for (int i = 0; i < 4; i++) {
            byteData = (byte) (CurrAdrr & 0x000000FF);
            byteBuffer.put((byte) byteData);
            CurrAdrr >>= 8;
        }

        //Length
        for (int j = 0; j < 4; j++) {
            byteData = (byte) (CurrLength & 0x000000FF);
            byteBuffer.put((byte) byteData);
            CurrLength >>= 8;
        }

        //set _readFlashAddr
        _readFlashAddr = ReadAddr;

        //set _readFlashLength
        _readFlashLength = ReadLength;

        packet.setData(data);
        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnReadFlashHandler()
     * Description    : LCB ReturnReadFlash Process 0x15
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnReadFlashHandler(UpdatePacket packet) {
        byte[] data = packet.getData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] address = new byte[4];
        byte[] readData = new byte[data.length - 4];

        for (int i = 0; i < 4; i++) {
            address[i] = byteBuffer.get();
        }
        int intAddress = DataUtils.unsignedBytesToInt((byte) address[0], (byte) address[1], (byte) address[2], (byte) address[3]);

        // Commpare Address
        if (intAddress != _readFlashAddr) {
            return ProcessReturnCmdError.CMD_RTN_ERROR_READ_DATA_ADDR;
        }

        // Commpare Data Length
        if ((data.length - 4) != _readFlashLength) {
            return ProcessReturnCmdError.CMD_RTN_ERROR_READ_DATA_LEN;
        }

        for (int j = 0; j < (data.length - 4); j++) {
            readData[j] = byteBuffer.get();

        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnReadFlashHandler(readData);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbProgramStateProcess()
     * Description    : LCB ProgramState Process 0x20
     * Input          : None
     * Output         : ReturnProgramState 0x21
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbProgramStateProcess() {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdProgramState, UpdateCommands.CmdReturnProgramState);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnProgramStateHandler()
     * Description    : LCB ReturnProgramState Process 0x21
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnProgramStateHandler(UpdatePacket packet) {
        byte[] data = packet.getData();

        if (data.length != RECEIVE_DATA_SIZE_RETURN_PROGRAM_STATE) {
            return ProcessReturnCmdError.CMD_RTN_ERROR_DATA_LEN;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte byteProgramState = byteBuffer.get();
/*
        if (byteProgramState == (byte) 0x00) {
            //NonUpdated
        } else if (byteProgramState == (byte) 0x01) {
            //Updated
        } else if (byteProgramState == (byte) 0xFF) {
            //block data is wrong
        }
*/
        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnProgramStateHandler(byteProgramState);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbFlashUnlockProcess()
     * Description    : LCB FlashUnlock Process 0x24
     * Input          : byte[] password
     * Output         : ReturnFlashState 0x25
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbFlashUnlockProcess(byte[] password) {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdFlashUnlock, UpdateCommands.CmdReturnFlashState);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        //set data
        byte[] data = new byte[password.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byte byteData;

        for (int j = 0; j < password.length; j++) {
            //==>Data
            byteData = (byte) (password[j]);
            byteBuffer.put((byte) byteData);
        }

        packet.setData(data);
        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnFlashStateHandler()
     * Description    : LCB ReturnFlashState Process 0x25
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnFlashStateHandler(UpdatePacket packet) {
        byte[] data = packet.getData();
        boolean isUnlock = false;

        if (data.length != RECEIVE_DATA_SIZE_RETURN_FLASH_STATE)
            return ProcessReturnCmdError.CMD_RTN_ERROR_DATA_LEN;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte byteFlashState = byteBuffer.get();

        if (byteFlashState == (byte) 0x00) {
            //Lock
            isUnlock = false;
        } else if (byteFlashState == (byte) 0x01) {
            //Unlock
            isUnlock = true;
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnFlashStateHandler(isUnlock);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbWriteCheckCodeProcess()
     * Description    : LCB WriteCheckCode Process 0x26
     * Input          : byte[] checkCode //15 bytes
     * Output         : ReturnFlashState 0x27
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbWriteCheckCodeProcess(byte[] checkCode) {
        //check checkCode length = 15
        if (checkCode.length != CHECK_CODE_DATA_LEN) {
            return ProcessCmdError.CMD_ERROR_CHECK_CODE_DATA_LEN_NOT_MATCH;
        }

        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdWriteCheckCode, UpdateCommands.CmdReturnCheckCode);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        //set data
        byte[] data = new byte[CHECK_CODE_DATA_LEN + 1]; //CheckCode length + CRC
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte calculatedCrc = UpdateCrc8.doUpdateCrc8(checkCode, checkCode.length);

        for (byte b : checkCode) {
            byteBuffer.put((byte) b);
        }

        byteBuffer.put((byte) calculatedCrc);

        packet.setData(data);
        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnCheckCodeHandler()
     * Description    : LCB ReturnFlashState Process 0x27
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnCheckCodeHandler(UpdatePacket packet) {
        byte[] data = packet.getData();
        boolean isSuccess = false;

        if (data.length != RECEIVE_DATA_SIZE_RETURN_CHECK_CODE)
            return ProcessReturnCmdError.CMD_RTN_ERROR_DATA_LEN;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] chackCode = new byte[CHECK_CODE_DATA_LEN];

        for (int i = 0; i < CHECK_CODE_DATA_LEN; i++) {
            chackCode[i] = byteBuffer.get();
        }

        Byte crcData = byteBuffer.get();

        byte calculatedCrc = UpdateCrc8.doUpdateCrc8(chackCode, chackCode.length);

        if (crcData == calculatedCrc) {
            //ok
            isSuccess = true;
        } else {
            //CheckCode CRC fail
            isSuccess = false;
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnCheckCodeHandler(isSuccess);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbReadProduceIDProcess()
     * Description    : LCB ReadProduceID Process 0x30
     * Input          : None
     * Output         : ReturnProduceID 0x31
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbReadProduceIDProcess() {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdReadProduceID, UpdateCommands.CmdReturnProduceID);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnProduceIDHandler()
     * Description    : LCB ReturnProduceID Process 0x31
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnProduceIDHandler(UpdatePacket packet) {
        byte[] data = packet.getData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] produceID = new byte[data.length];

        for (int j = 0; j < (data.length); j++) {
            produceID[j] = byteBuffer.get();
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnProduceIDHandler(produceID);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbReadMcuTypeProcess()
     * Description    : LCB ReadMcuType Process 0x32
     * Input          : None
     * Output         : ReturnMcuType 0x33
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbReadMcuTypeProcess() {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdReadMcuType, UpdateCommands.CmdReturnMcuType);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnMcuTypeHandler()
     * Description    : LCB ReturnMcuType Process 0x33
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnMcuTypeHandler(UpdatePacket packet) {
        byte[] data = packet.getData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] mcuType = new byte[data.length];

        for (int j = 0; j < (data.length); j++) {
            mcuType[j] = byteBuffer.get();
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnMcuTypeHandler();
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbReadMcuIDProcess()
     * Description    : LCB ReadMcuID Process 0x34
     * Input          : None
     * Output         : ReturnMcuID 0x35
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbReadMcuIDProcess() {

        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdReadMcuID, UpdateCommands.CmdReturnMcuID);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnMcuIDHandler()
     * Description    : LCB ReturnMcuID Process 0x35
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnMcuIDHandler(UpdatePacket packet) {
        byte[] data = packet.getData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] McuID = new byte[data.length];

        for (int j = 0; j < (data.length); j++) {
            McuID[j] = byteBuffer.get();
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnMcuIDHandler();
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbReadUpdateModeProcess()
     * Description    : LCB ReadUpdateMode Process 0x40
     * Input          : None
     * Output         : ReturnUpdateMode 0x41
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbReadUpdateModeProcess() {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdReadUpdateMode, UpdateCommands.CmdReturnUpdateMode);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnUpdateModeHandler()
     * Description    : LCB ReturnUpdateMode Process 0x41
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnUpdateModeHandler(UpdatePacket packet) {
        byte[] data = packet.getData();
        boolean isUpdateMode = false;

        if (data.length != RECEIVE_DATA_SIZE_RETURN_UPDATE_MODE)
            return ProcessReturnCmdError.CMD_RTN_ERROR_DATA_LEN;

        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byte byteUpdateMode = byteBuffer.get();

        if (byteUpdateMode == (byte) 0x00) {
            //Normal Mode
            isUpdateMode = false;
        } else if (byteUpdateMode == (byte) 0x01) {
            //Update Mode
            isUpdateMode = true;
        }

        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnUpdateModeHandler(isUpdateMode);
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : lcbPowerOffProcess(byte delaySec)
     * Description    : LCB PowerOff Process 0x46
     * Input          : byte delaySec
     * Output         : ReturnUpdateMode 0x47
     * Return         : ProcessCmdError
     *******************************************************************************/
    public ProcessCmdError lcbPowerOffProcess(byte delaySec) {
        //new UpdatePacket and set sendCommand rtnsendCommand
        UpdatePacket packet = new UpdatePacket(UpdateCommands.CmdPowerOff, UpdateCommands.CmdReturnPowerOff);
        if (packet == null) {
            return ProcessCmdError.CMD_ERROR_NEW_PACKET_NULL;
        }

        //set data
        byte[] data = new byte[1]; //delaySec length
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.put((byte) delaySec);

        packet.setData(data);
        sendPacket(packet);

        return ProcessCmdError.CMD_SUCCESS;
    }

    /*******************************************************************************
     * Function Name  : procReturnPowerOffHandler()
     * Description    : LCB ReturnPowerOff Process 0x47
     * Input          : UpdatePacket packet
     * Output         : None
     * Return         : ProcessReturnCmdError
     *******************************************************************************/
    private ProcessReturnCmdError procReturnPowerOffHandler(UpdatePacket packet) {
        for (ILcbUpdateCommReceive listener : _receiveListenerUpdateCommList) {
            listener.returnPowerOffHandler();
        }

        return ProcessReturnCmdError.CMD_RTN_SUCCESS;
    }
}
