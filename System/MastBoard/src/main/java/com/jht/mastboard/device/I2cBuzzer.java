package com.jht.mastboard.device;

import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Buzzer for DRV2605 that defines the I2C protocol
 */
public class I2cBuzzer extends I2cDevice {
    public final static int MIN_EFFECT_IDENTIFIER = 1;
    public final static int MAX_EFFECT_IDENTIFIER = 115;
    int currEffect = -1;

    /**
     * Constructs an I2cBuzzer instance with the specified I2C address, interface, and transfer options.
     *
     * @param i2cAddress      The I2C address of the buzzer device.
     * @param i2CInterface    The I2C interface to communicate with the device.
     * @param transferOption  The transfer options for the I2C communication.
     */
    public I2cBuzzer(byte i2cAddress, @NonNull I2cInterface i2CInterface, @NonNull Mastboard.TransferOption transferOption) {
        super(i2cAddress, i2CInterface, transferOption);
    }

    @Override
    void init() {

        /*
        True,I2C,7 bit,5A,Write,,"01,00",,,Out of standby,Send
        True,I2C,7 bit,5A,Write,,"02,01",,,Strong click,Send
        True,I2C,7 bit,5A,Write,,"04,00",,,No Realtime playback,Send
        True,I2C,7 bit,5A,Write,,"05,00",,,End of sequence,Send
        True,I2C,7 bit,5A,Write,,"0D,00",,,No overdrive,Send
        True,I2C,7 bit,5A,Write,,"0E,00",,,Sustain pos,Send
        True,I2C,7 bit,5A,Write,,"0F,00",,,Sustain neg,Send
        True,I2C,7 bit,5A,Write,,"10,00",,,break,Send
        True,I2C,7 bit,5A,Write,,"13,00",,,audiomax,Send
        True,I2C,7 bit,5A,Write,,"1A,7F",,,feedback,Send
        True,I2C,7 bit,5A,Write,,"1D,20",,,control3,Send
        True,I2C,7 bit,5A,Write,,"03,01",,,set library,Send
        True,I2C,7 bit,5A,Write,,"01,00",,,intrig,Send
        True,I2C,7 bit,5A,Write,,"04,47",,,strong buzz,Send
        True,I2C,7 bit,5A,Write,,"04",,,,Send
        True,I2C,7 bit,5A,Read,,"1",,,,Send
        True,I2C,7 bit,5A,Write,,"05,00",,,end sequence set,Send
        True,I2C,7 bit,5A,Write,,"0C,01",,,go,Send
         */

        i2cWrite(new byte[]{(byte) 0x01, (byte) 0x00
                , (byte) 0x01} //write to 0x02
        );
        i2cWrite(new byte[]{(byte) 0x04, (byte) 0x00
                , (byte) 0x00} //write to 0x05
        );

        i2cWrite(new byte[]{(byte) 0x0D, (byte) 0x00
                , (byte) 0x00 //write to 0x0E
                , (byte) 0x00 //write to 0x0F
                , (byte) 0x00} //write to 0x10
        );
        i2cWrite(new byte[]{(byte) 0x13, (byte) 0x00});
        i2cWrite(new byte[]{(byte) 0x1A, (byte) 0x7F});
        i2cWrite(new byte[]{(byte) 0x1D, (byte) 0x20});
        i2cWrite(new byte[]{(byte) 0x03, (byte) 0x03}); //library "C"
        i2cWrite(new byte[]{(byte) 0x17, (byte) 0xFF}); //default == 8C

        Log.d("I2cBuzzer", String.format("HAPTIC MOTOR OD_CLAMP 0x17 Register = %x", i2cRead((byte) 0x17)));
    }

    /**
     * Activates the buzzer to produce a haptic effect based on the specified effect identifier.
     * The method ensures the effect identifier is within the valid range and sets the buzzer
     * to produce the corresponding effect.
     *
     * @param effectID The identifier of the haptic effect to be produced by the buzzer.
     *                 It should be between MIN_EFFECT_IDENTIFIER and MAX_EFFECT_IDENTIFIER.
     */
    public void buzz(int effectID) {

        // a sequence can go from 0x04 to 0x0B and must always end in 0. effectID goes from
        // value of 1 to 115 can be used as MSB is 1 when using it as a wait in a sequence
        if (effectID > MAX_EFFECT_IDENTIFIER) effectID = MAX_EFFECT_IDENTIFIER;
        else if (effectID < MIN_EFFECT_IDENTIFIER) effectID = MIN_EFFECT_IDENTIFIER;

        // only configure the strength if it has changed
        if (effectID != currEffect) {
            currEffect = effectID;

            // Start address 0x04. Then will only play one sequence of amplitude strength. Then go bit
            byte[] sequence = new byte[]{(byte) 0x04
                    , (byte) effectID // sequence value 1
                    , 0 // sequence value 2 (currently end of sequence because value is 0)
                    , 0 // sequence value 3
                    , 0 // sequence value 4
                    , 0 // sequence value 5
                    , 0 // sequence value 6
                    , 0 // sequence value 7
                    , 0};// sequence value 8
            i2cWrite(sequence); //strong buzz
        }

        // send buzz command
        i2cWrite(new byte[]{(byte) 0x0C, (byte) 0x01}); //GO (needs to be separate)
    }

}
