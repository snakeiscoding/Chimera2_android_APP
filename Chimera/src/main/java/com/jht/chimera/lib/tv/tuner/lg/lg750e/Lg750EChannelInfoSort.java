package com.jht.chimera.lib.tv.tuner.lg.lg750e;

import com.jht.serialport.peripheral.tuner.channel.IChannel;

import java.util.Comparator;

class Lg750EChannelInfoSort implements Comparator<IChannel> {

    @Override
    public int compare(IChannel channel1, IChannel channel2) {
        try {
            String number1 = channel1.number().replace("-", ".");
            String number2 = channel2.number().replace("-", ".");
            String[] number1Parts = number1.split("\\.");
            String[] number2Parts = number2.split("\\.");
            if (number1Parts.length > 0 && number2Parts.length > 0) {

                int majorNumber1 = Integer.parseInt(number1Parts[0]);
                int majorNumber2 = Integer.parseInt(number2Parts[0]);
                if (majorNumber1 == majorNumber2) {
                    if (number1Parts.length > 1 && number2Parts.length > 1) {
                        int minorNumber1 = Integer.parseInt(number1Parts[1]);
                        int minorNumber2 = Integer.parseInt(number2Parts[1]);
                        return minorNumber1 - minorNumber2;
                    } else if (number1Parts.length > 1) {
                        return 1;
                    } else if (number2Parts.length > 1) {
                        return -1;
                    }
                    return 0;
                }
                return majorNumber1 - majorNumber2;
            }
            return channel1.number().compareTo(channel2.number());
        } catch (Exception ex) {
            return channel1.number().compareTo(channel2.number());
        }
    }
}
