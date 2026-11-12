package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib;

import android.util.Log;

import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class BinFileManager {
    @NonNls private static final String TAG = "BinFileManager";

    public BinFileManager() {

    }

    public int readBinFileDataChar(String updateFilePath, int ReadAddr, char[] RtnChar, int ReadDataLength) {
        int rtnCharLen = 0;

        if (updateFilePath == null) {
            Log.e(TAG, "Update File Path is null.");
            return -1;
        }

        try {
            FileInputStream fin = new FileInputStream(updateFilePath);
            Reader reader = new BufferedReader(new InputStreamReader(fin, "UTF-8")); //NON-NLS
            fin.skip(ReadAddr);
            rtnCharLen = reader.read(RtnChar, 0, ReadDataLength);
            fin.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception in read bin file FileNotFound !!", e);
        } catch (IOException e) {
            Log.e(TAG, "Exception in read bin file IO error !!", e);
        }

        return rtnCharLen;
    }

    public int readBinFileDataByte(String updateFilePath, int ReadAddr, byte[] RtnByte, int ReadDataLength) {
        int rtnByteLen = 0;

        if (updateFilePath == null) {
            Log.e(TAG, "Update File Path is null.");
            return -1;
        }

        try {
            FileInputStream fin = new FileInputStream(updateFilePath);
            fin.skip(ReadAddr);
            rtnByteLen = fin.read(RtnByte, 0, ReadDataLength);
            fin.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception in read bin file FileNotFound !!", e);
        } catch (IOException e) {
            Log.e(TAG, "Exception in read bin file IO error !!", e);
        }

        return rtnByteLen;
    }
}
