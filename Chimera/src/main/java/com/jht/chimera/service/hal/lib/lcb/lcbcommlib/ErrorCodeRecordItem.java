package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public class ErrorCodeRecordItem {

    private static final String TAG = "ErrorCodeRecordItem";
    private int _errorCode;
    private String _timeStamp;

    ErrorCodeRecordItem() {
    }

    public void setErrorRecordItem(int errorCode, String timeStamp) {
        _errorCode = errorCode;
        _timeStamp = timeStamp;
        //Log.e(TAG, String.format("Error code record from lcb errorCode = 0x%2X", errorCode)+", timeStamp =" + timeStamp);
    }

    public int getItemErrorCode() {
        return _errorCode;
    }

    public String getItemTimeStamp() {
        return _timeStamp;
    }
}

