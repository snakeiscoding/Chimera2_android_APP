package com.jht.mastboard.device;

import androidx.annotation.NonNull;

public interface MastboardInterface {
    /**
     * Set the GPIO that sets the I2C messages to the left/right side
     *
     * @param side
     */
    void setSpiSide(@NonNull SmartGrip.Side side);

    /**
     * Set the GPIO that sets the SPI messages to the left/right side
     * @param side
     */
    void setI2cSide(@NonNull SmartGrip.Side side);
}
