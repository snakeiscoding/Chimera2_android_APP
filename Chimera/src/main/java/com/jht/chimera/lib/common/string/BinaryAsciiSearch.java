package com.jht.chimera.lib.common.string;

import java.nio.charset.StandardCharsets;

/*
    Class to search byte stream for a particular string
 */
public class BinaryAsciiSearch {
    private int matchIndex; // points the current byte we are looking for
    private final byte[] matchString;

    public BinaryAsciiSearch(String match, boolean includeNullTerminator) {
        byte[] temp = match.getBytes(StandardCharsets.US_ASCII);
        if (includeNullTerminator) {
            matchString = new byte[temp.length + 1];
            System.arraycopy(temp, 0, matchString, 0, temp.length);
            matchString[matchString.length-1] = 0; // in theory java would have initialized the array to zeroes, so this may be redundant
        } else {
            matchString = temp;
        }
        reset();
    }

    public void reset() {
        matchIndex = 0; // go back to searching for the first byte
    }

    // return true if match was found
    public boolean search(byte v) {
        if (v == matchString[matchIndex]) {
            ++matchIndex;
            if (matchIndex >= matchString.length) {
                reset(); // should we automatically reset or rely on caller to make that choice?
                return true;
            }
        } else {
            reset();
        }
        return false;
    }

    public boolean search(byte[] array) {
        for (byte v : array) {
            if (search(v)) return true;
        }
        return false;
    }

}
