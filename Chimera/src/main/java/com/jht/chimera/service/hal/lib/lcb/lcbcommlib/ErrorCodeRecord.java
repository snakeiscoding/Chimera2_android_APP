package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class ErrorCodeRecord {
    private static final String TAG = "ErrorCodeRecord";

    private static final int ERROR_CODE_RECORD_MAX_TIMES = 50;
    private final static int NO_ERROR_RECODE = 0x0000;
    private ArrayList<ErrorCodeRecordItem> _errorRecordList = new ArrayList<ErrorCodeRecordItem>();
    private HashMap<Integer, Integer> _errorCodeRecordListHash = new HashMap<Integer, Integer>();

    private ErrorCodeRecord() {
    }

    private static class SingletonHolder {
        private static ErrorCodeRecord Instance = new ErrorCodeRecord();
    }

    public static ErrorCodeRecord getInstance() {
        return SingletonHolder.Instance;
    }

    /*******************************************************************************
     * Function Name  : addErrorCodeRecordItem()
     * Description    :add ErrorCode to ErrorCodeRecord list Item
     * Input          : int errorCode
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void addErrorCodeRecordItem(int errorCode) {
        int listSize = _errorRecordList.size();
        boolean canAddErrorCodeRecordItem = checkErrorCodeInRecordListHash(errorCode);
        if (!canAddErrorCodeRecordItem)
            return;

        ErrorCodeRecordItem errorCodeRecordItem = new ErrorCodeRecordItem();
        String timeStamp = DateConversion.calendarDateToString(Calendar.getInstance(), "y-M-d H:m:s"); //NON-NLS

        boolean priorityAdd = priorityErrorCodeCheck(errorCode);

        if (errorCodeRecordItem != null) {
            if (listSize < ERROR_CODE_RECORD_MAX_TIMES) {
                if (priorityAdd) {//add priority errorCodeRecordItem to first errorRecordList
                    errorCodeRecordItem.setErrorRecordItem(errorCode, timeStamp);
                    _errorRecordList.add(0, errorCodeRecordItem);
                } else {//add errorCodeRecordItem to last errorRecordList
                    errorCodeRecordItem.setErrorRecordItem(errorCode, timeStamp);
                    _errorRecordList.add(errorCodeRecordItem);
                }
            } else {//errorRecordList is full (> ERROR_CODE_RECORD_MAX_TIMES)
                if (priorityAdd) {//remove errorCodeRecordItem from last errorRecordList and add priority errorCodeRecordItem to first errorRecordList
                    ErrorCodeRecordItem removeErrorCodeRecordItem = _errorRecordList.remove(listSize - 1);
                    _errorCodeRecordListHash.remove(removeErrorCodeRecordItem.getItemErrorCode());
                    errorCodeRecordItem.setErrorRecordItem(errorCode, timeStamp);
                    _errorRecordList.add(0, errorCodeRecordItem);
                } else {//errorRecordList is full can not add  errorCodeRecordItem to errorRecordList
                    _errorCodeRecordListHash.remove(errorCode);
                    Log.v(TAG, "ErrorRecordList is full . ErrorCodeRecordItem greater than ERROR_CODE_RECORD_MAX_TIMES can not add !!");
                }
            }
        } else {
            if (canAddErrorCodeRecordItem)
                _errorCodeRecordListHash.remove(errorCode);
        }
    }

    /*******************************************************************************
     * Function Name  : checkErrorCodeInRecordListHash()
     * Description    :Check error code whether or not it happened InRecordList
     * Input          : int errorCode
     * Output         : None
     * Return         : boolean (0: can not add  ErrorCodeRecordItem 1:  can add  ErrorCodeRecordItem)
     *******************************************************************************/
    private boolean checkErrorCodeInRecordListHash(int errorCode) {

        boolean rtnCanAdd = true;
        int errorCodeRecordListHashSize = _errorCodeRecordListHash.size();

        if (errorCodeRecordListHashSize == 0) {
            _errorCodeRecordListHash.put(errorCode, errorCode);
            rtnCanAdd = true;
        } else {
            //check error code have in ErrorCodeInRecordList hash
            if (_errorCodeRecordListHash.containsKey(errorCode)) {//error code in ErrorCodeInRecordList hash
                rtnCanAdd = false;
            } else {//error code not in ErrorCodeInRecordList hash , add error code in ErrorCodeInRecordList hash
                _errorCodeRecordListHash.put(errorCode, errorCode);
                rtnCanAdd = true;
            }
        }

        return rtnCanAdd;
    }

    /*******************************************************************************
     * Function Name  : addErrorCodeRecordItem()
     * Description    :add ErrorCode to ErrorCodeRecord list Item
     * Input          : int errorCode
     * Output         : None
     * Return         : None
     *******************************************************************************/
    private boolean priorityErrorCodeCheck(int errorCode) {

        boolean rtnPriority = false;

        if ((errorCode == 0x02B2)) {
            rtnPriority = true;
        }

        return rtnPriority;
    }

    /*******************************************************************************
     * Function Name  : getErrorRecordCounter()
     * Description    :The method getErrorRecordCounter()  is get ErrorCodeRecord size
     * Input          :  None
     * Output         : None
     * Return         : int _errorRecordList.size()
     *******************************************************************************/
    public int getErrorRecordCounter() {
        return _errorRecordList.size();
    }


    /*******************************************************************************
     * Function Name  : getLastErrorCodeRecord()
     * Description    :The method getLastErrorCodeRecord()  is get ErrorCodeRecord from list array index = last and remove
     * Input          :  None
     * Output         : None
     * Return         : int rtnErrorCode
     *******************************************************************************/
    public int getLastErrorCodeRecord() {
        int listSize = _errorRecordList.size();
        int rtnErrorCode = NO_ERROR_RECODE;

        if (listSize == 0) {
            rtnErrorCode = NO_ERROR_RECODE;
        } else {
            ErrorCodeRecordItem errorCodeRecordItem = _errorRecordList.remove(listSize - 1);
            rtnErrorCode = errorCodeRecordItem.getItemErrorCode();
            _errorCodeRecordListHash.remove(rtnErrorCode);
//			String timeStamp = errorCodeRecordItem.getItemTimeStamp();
        }

        return rtnErrorCode;
    }

    /*******************************************************************************
     * Function Name  : getErrorCodeRecord()
     * Description    :The method getErrorCodeRecord() is get ErrorCodeRecord from list array index = 0 and remove
     * Input          :  None
     * Output         : None
     * Return         : int rtnErrorCode
     *******************************************************************************/
    public int getErrorCodeRecord() {
        int listSize = _errorRecordList.size();
        int rtnErrorCode = NO_ERROR_RECODE;

        if (listSize == 0) {
            rtnErrorCode = NO_ERROR_RECODE;
        } else {
            ErrorCodeRecordItem errorCodeRecordItem = _errorRecordList.remove(0);
            rtnErrorCode = errorCodeRecordItem.getItemErrorCode();
            _errorCodeRecordListHash.remove(rtnErrorCode);
//			String timeStamp = errorCodeRecordItem.getItemTimeStamp();
        }

        return rtnErrorCode;
    }

    /*******************************************************************************
     * Function Name  : clearAllErrorCodeRecord()
     * Description    :The method clearAllErrorCodeRecord() is clear all ErrorCodeRecord
     * Input          :  None
     * Output         : None
     * Return         : None
     *******************************************************************************/
    public void clearAllErrorCodeRecord() {
        int listSize = _errorRecordList.size();

        _errorRecordList.clear();
        _errorCodeRecordListHash.clear();
    }

}

