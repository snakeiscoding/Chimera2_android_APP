package com.jht.chimera.lib.common.config;

import android.os.Build;

import com.jht.chimera.lib.common.SystemPropertyUtil;
import com.jht.chimera.lib.common.datamatrixkeys.HALKeys;
import com.jht.datamatrixlib.DataMatrix;

public class CarrierBoard {
    public static String getCarrierBoard() {
        String carrierBoard = SystemPropertyUtil.get("ro.boot.board_ca"); //NON-NLS
        if(carrierBoard.isEmpty()) {
            if (Build.PRODUCT.equals("leo_8mq")) {
                return "Wistron"; //NON-NLS
            }
            try {
                // Fall back to using the IO version.
                String ioVersion = DataMatrix.getString(HALKeys.DATA_VERSION_IO_STRING);
                String[] parts = ioVersion.split("\\.");
                int major = Integer.parseInt(parts[0]);
                if (major >= 50) {
                    return "VerF"; //NON-NLS
                } else {
                    return "VerE"; //NON-NLS
                }
            }
            catch(Exception ex) {
                return "VerE"; //NON-NLS
            }
        }
        return carrierBoard;
    }

    public static boolean isRevFOrLater() {
        String board = getCarrierBoard();
        return board.toLowerCase().compareTo("verf") >= 0;
    }

    public static boolean isRevEOrOlder() {
        String board = getCarrierBoard();
        return board.toLowerCase().compareTo("vere") <= 0;
    }
}
