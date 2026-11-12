package com.jht.updateservice;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.jht.androidcommonalgorithms.util.SystemCommands;


public class CarrierBoard {
    public static String getCarrierBoard(Context context) {
        String carrierBoard = SystemCommands.getSystemProperty("vendor.product.board_ca"); //NON-NLS
        if (Build.PRODUCT.equals("leo_8mq")) {
            return "Wistron"; //NON-NLS
        }
        if(carrierBoard.isEmpty()) {
            try {
                // Fall back to using the IO version.
                String ioVersion = Settings.Secure.getString(context.getContentResolver(), "chimera_io_version"); //NON-NLS
                String[] parts = ioVersion.split("\\.");
                int major = Integer.parseInt(parts[0]);
                if (major >= 50) {
                    return "F"; //NON-NLS
                } else {
                    return "E"; //NON-NLS
                }
            }
            catch(Exception ex) {
                return "E"; //NON-NLS
            }
        }
        return carrierBoard;
    }

    public static String getBoardVersion(){
        String version = SystemCommands.getSystemProperty("vendor.product.version");
        if (version.isEmpty())
            return "V1";
        else
            return version;
    }

    public static boolean isRevFOrLater(Context context) {
        String board = getCarrierBoard(context);
        //Last item is for special case of wrong main board property
        return Character.getNumericValue(getBoardVersion().charAt(1)) > 1 || board.toLowerCase().compareTo("f") >= 0 || board.toLowerCase().compareTo("c")==0;
    }

    public static boolean isRevEOrOlder(Context context) {
        String board = getCarrierBoard(context);
        return board.toLowerCase().compareTo("e") <= 0;
    }
}
