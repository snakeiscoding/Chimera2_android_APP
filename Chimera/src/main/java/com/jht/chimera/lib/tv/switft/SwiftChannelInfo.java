package com.jht.chimera.lib.tv.switft;


import java.nio.charset.Charset;

public class SwiftChannelInfo {
    private static final String TAG = "SwiftChannelInfo";

    private byte [] _majorChanNo = new byte [2];
    private byte [] _minorChanNo = new byte [2];
//    private byte [] _chName      = new byte [33];
    private String name;
    private char _serviceType = 0;
    private char _isFav = 0;
    private char _isSkip = 0;

    public void setMajorChannelNo(byte data[]) {
        if (null == data) {
            _majorChanNo = new byte[0];
            return;
        }
        _majorChanNo = data;
    }

    public byte [] getMajorChanNo(){
        return _majorChanNo;
    }

    public void setMinorChannelNo(byte[] data) {
        if (null == data) {
            _minorChanNo = new byte[0];
            return;
        }
        _minorChanNo = data;
    }

    public byte [] getMinorChanNo(){
        return _minorChanNo;
    }

    public void setChannelName(byte[] data) {
        if(data == null) {
            name = "";
        }
        else {
            name = new String(data, Charset.forName("UTF-8"));
        }
        name = name.replaceAll("\u0000.*", "");
    }

    public String getChannelName() {
        return name;
    }

    public void setServiceType(char data) {
        _serviceType = data;
    }

    public char getServiceType(){
        return _serviceType;
    }

    public void setIsFav(char data) {
        _isFav = data;
    }

    public char getIsFav(){
        return _isFav;
    }

    public void setIsSkip(char data) {
        _isSkip = data;
    }

    public char getIsSkip(){
        return _isSkip;
    }
/*
    private final byte    InitialNumber = (byte)0xF5;
    private final int  PACKET_HEADER_SIZE = 1;
    private final int     PACKET_CRC_SIZE = 1;
    private final int     PACKET_MIN_SIZE = PACKET_HEADER_SIZE + PACKET_CRC_SIZE;
    private final int      RX_BUFFER_SIZE = 256;

    private boolean _all_data_has_arrived = false;
    private byte []              _payload = new byte[0];
    private byte []               _rxData = new byte[0];
    private int                  _rxIndex = 0;
    private int         _parsedDataLength = 0;
    private byte _rxStatus;
    private byte _rxIrCode;
    private int _returnDataSize = 0;


    private volatile SwiftCommands _message = SwiftCommands.SetSwiftNone;
    private ParseState        _parseState = ParseState.PARSE_BYTE_INITIALl_NUMBER;
    private Object              _dataLock = new Object();
    private Object               _msgLock = new Object();

    public enum ParseResult {
        Success,
        Incomplete,
        InvalidSyncByte,
        InvalidRtnCommand,
        InvalidCrc,
        BufferOverflow
    }

    private enum ParseState {
        PARSE_BYTE_INITIALl_NUMBER,
        PARSE_BYTE_STATUS,
        PARSE_BYTE_IR_CODE,
        PARSE_BYTE_DATA_PAYLOAD,
        PARSE_BYTE_CRC
    }
*/
    public SwiftChannelInfo() {}
/*
    public SwiftChannelInfo(SwiftCommands msg) {
        setMessageText(msg);
    }

    public synchronized SwiftCommands getMessage() {
        synchronized (_msgLock) {
            return _message;
        }
    }

    public synchronized void setMessageText(SwiftCommands msg) {
        synchronized (_msgLock) {
            _message = msg;
        }
    }
	
    public synchronized byte getRxStatus() {
        synchronized (_msgLock) {
        return _rxStatus;
        }
    }

    public synchronized byte getRxIrCode() {
        synchronized (_msgLock) {
        return _rxIrCode;
        }
    }

    public synchronized byte [] getData() {
        synchronized (_dataLock) {
            return _payload;
        }
    }

    public synchronized void setData(byte data[]) {
        synchronized (_dataLock) {
            if (null == data) {
                _payload = new byte[0];
                return;
            }
            _payload = data;
        }
    }

    public synchronized void setReturnDataSize(int dataSize) {



//        synchronized (_dataLock) {
            _returnDataSize = dataSize;

        Log.d(TAG, "SwiftChannelInfo.java setReturnDataSize(), _returnDataSize = " + _returnDataSize);

//        }
    }

    public synchronized int getReturnDataSize() {
//        synchronized (_dataLock) {

        Log.d(TAG, "SwiftChannelInfo.java getReturnDataSize(), _returnDataSize = " + _returnDataSize);

        return _returnDataSize;
//        }
    }

    public synchronized byte [] buildTxPacket() {
    	char[] charsCommand = _message.getCommand();
        byte msg[] = new byte[PACKET_MIN_SIZE + charsCommand.length + _payload.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(msg);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        if(charsCommand[0] == 'E' && charsCommand[1] == 'X' && charsCommand[2] == 'S') {
            Log.d(TAG, "%%%%%%%%%%%%%%%%%%%%%% SwiftChannelInfo.java buildTxPacket(), set return data size = " + _returnDataSize);

            _message.setrtnDataSize(_returnDataSize);
        }

        // sync bytes
        byteBuffer.put(InitialNumber);
		
        // message command
        for(int i = 0 ; i < charsCommand.length ; i++)
        {
            Log.d(TAG, "SwiftChannelInfo.java buildTxPacket(), charsCommand = " + charsCommand[i]);

            byte byteChar = (byte)(0xFF & (int)charsCommand[i]);
	        byteBuffer.put(byteChar);
        }
		
        // data payload
        for (byte b : _payload) {
            byteBuffer.put(b);
        }
		
        // checksum - one byte
        byteBuffer.put(SwiftCrc.calCrc(msg, msg.length));

        return msg;
    }

    public synchronized ParseResult parse(byte data, final SwiftCommands expectedMessage) {
        if (0 == _rxData.length) {
            _rxData = new byte[RX_BUFFER_SIZE];
            _rxIndex = 0;
        }

        Log.w(TAG, "SwiftChannelInfo.java parse(), _parseState = " + _parseState + ", _rxIndex = " + _rxIndex + ", data = " + data + ", value = " + Integer.toHexString(data & 0xFF));


        _rxData[_rxIndex] = data;

        switch (_parseState) {
            case PARSE_BYTE_INITIALl_NUMBER:
                if ((_rxIndex == 0) && (InitialNumber == data))
                    _parseState = ParseState.PARSE_BYTE_STATUS;
                else
                    return ParseResult.InvalidSyncByte;
                break;

            case PARSE_BYTE_STATUS:
                if (_rxIndex == 1) {
	                byte Status = data;
	                _rxStatus = Status;
	                _parseState = ParseState.PARSE_BYTE_IR_CODE;
                }
                break;

            case PARSE_BYTE_IR_CODE:
                if (_rxIndex == 2) {
	                byte irCode = data;
	                _rxIrCode = irCode;
                    int dataSize = _message.getrtnDataSize();

                    Log.w(TAG, "SwiftChannelInfo.java parse(), IN PARSE_BYTE_IR_CODE, _rxIrCode = " + Integer.toHexString(data & 0xFF) + " , dataSize = " + dataSize);

                    if (dataSize == 0) {  //it's a message without data payload
	                        _parseState = ParseState.PARSE_BYTE_CRC;
	                        _all_data_has_arrived = true;
			        }else{
				        setData(new byte[dataSize]);
	                	_parseState = ParseState.PARSE_BYTE_DATA_PAYLOAD;
				        _parsedDataLength = 0;
			        }
                }
                break;

            case PARSE_BYTE_DATA_PAYLOAD:
                if (_rxIndex >= 3) {
                    _payload[_parsedDataLength++] = data;
                    if (_message.getrtnDataSize() == _parsedDataLength) {  //all payload data were received
                        _parseState = ParseState.PARSE_BYTE_CRC;
                        _all_data_has_arrived = true;
                    }
                }
                break;

            case PARSE_BYTE_CRC:
                if (_all_data_has_arrived) {
                    byte parseCRC = _rxData[_rxIndex];
                    _parseState = ParseState.PARSE_BYTE_INITIALl_NUMBER;

                    byte calculatedCrc = SwiftCrc.calCrc(_rxData, _rxIndex);
                    if (parseCRC != calculatedCrc) {
                        Log.e(TAG, String.format("CRC checksum didn't match! Parsed=%d, Calculated=%d", parseCRC, calculatedCrc));
                        return ParseResult.InvalidCrc;
                    }else{
                        if (expectedMessage.getValueID() == _message.getValueID() ){
                            return ParseResult.Success;
                        }else{
                            return ParseResult.InvalidRtnCommand;
                        }
                    }
                }
                break;
        }

        ++_rxIndex;
        if (((expectedMessage != null) && (_rxIndex > (expectedMessage.getrtnDataSize()+4)))
                || (_rxIndex >= RX_BUFFER_SIZE)) {

            Log.w(TAG, "SwiftChannelInfo.java parse(), expectedMessage = " + expectedMessage + ", _rxIndex = " + _rxIndex + ", expectedMessage.getrtnDataSize()+4 = " + expectedMessage.getrtnDataSize()+4);
            Log.w(TAG, "!!!! SwiftChannelInfo.java parse(), RETURN ParseResult.BufferOverflow!!!");

            reset();
            return ParseResult.BufferOverflow;
        }

        Log.w(TAG, "!!!! SwiftChannelInfo.java parse(), RETURN ParseResult.Incomplete!!!");
        return ParseResult.Incomplete;
    }

    public synchronized void reset() {
        Log.w(TAG, "SwiftChannelInfo.java reset() is called !!");

                _rxData = new byte[0];
                     _payload = new byte[0];
                     _rxIndex = 0;
			_rxStatus = 0;	
			_rxIrCode = 0;	
                  _parseState = ParseState.PARSE_BYTE_INITIALl_NUMBER;
            _parsedDataLength = 0;
        _all_data_has_arrived = false;
    }

    public synchronized SwiftChannelInfo clone() {
        SwiftChannelInfo copy = new SwiftChannelInfo();
        byte []   data = getData();

        copy._message = SwiftCommands.fromInt(getMessage().getValueID());
        copy._payload = Arrays.copyOf(data, data.length);
        copy._rxIrCode = getRxIrCode();

        Log.w(TAG, "SwiftChannelInfo.java clone(), getReturnDataSize() = " +getReturnDataSize());

        copy._returnDataSize = getReturnDataSize();

        return copy;
    }
*/
}
