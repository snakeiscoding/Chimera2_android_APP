package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

/**
 * Created by mutombolin on 2018/09/19.
 */

public class ATSCUtils {
    private static final ATSCUtils ourInstance = new ATSCUtils();

    public static ATSCUtils getInstance() {
        return ourInstance;
    }

    private ATSCUtils() {

    }

    public String getChannelNumbers(String major, String minor) {
        String number = "1";

        if (major != null) {
            number = major;
        }

        if (minor == null) {
            minor = "0";
        }

        if ("-1".compareTo(minor) != 0 && "0".compareTo(minor) != 0) {
            number += "-";
            number += minor;
        }
        return number;
    }
}
