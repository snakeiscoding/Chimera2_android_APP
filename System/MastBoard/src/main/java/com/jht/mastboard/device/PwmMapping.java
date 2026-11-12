package com.jht.mastboard.device;

import static com.jht.mastboard.device.I2cPwmController.*;

public class PwmMapping {

    // mapping of PWM
    public static final byte RIGHT_BACKLIGHT_PWM = PWM0; // right backlight (0 high and 100 low)
    public static final byte CPU_FAN_PWM = PWM1;
    public static final byte LEFT_BACKLIGHT_PWM = PWM2;  // left backlight (0 high and 100 low)
    public static final byte LED1_RED_PWM = PWM3;
    public static final byte LED1_GREEN_PWM = PWM4;
    public static final byte LED1_BLUE_PWM = PWM5;
    public static final byte LED2_RED_PWM = PWM6;
    public static final byte LED2_GREEN_PWM = PWM7;
    public static final byte LED2_BLUE_PWM = PWM8;
    public static final byte LED3_RED_PWM = PWM9;
    public static final byte LED3_GREEN_PWM = PWM10;
    public static final byte LED3_BLUE_PWM = PWM11;
    public static final byte LED4_BLUE_PWM = PWM12;
    public static final byte LED4_GREEN_PWM = PWM13;
    public static final byte LED4_RED_PWM = PWM14;

    public enum LED {
        Console,
        Side,
        Button1,
        Button2,
        All
    }

    /**
     * Convert a color to an array list of RGB or BGR color depending the PWM mapping
     * <p>
     * Console, side, and button1 is RGB while button2 is BGR
     *
     * @param color
     * @param led
     * @return int array for the color
     */
    public static int[] convertColorToRGB(int color, LED led, int brightness) {
        int[] values = new int[]{};
        int red = Math.round(((color & 0x00ff0000) >> 16) / 255f * 100 * brightness / 100f);
        int green = Math.round(((color & 0x0000ff00) >> 8) / 255f * 100 * brightness / 100f);
        int blue = Math.round((color & 0x000000ff) / 255f * 100 * brightness / 100f);

        switch (led) {
            case Side:
            case Button1:
            case Button2:
                values = new int[]{red, green, blue};
                break;
            case Console:
                values = new int[]{blue, green, red};
                break;
            case All:
                values = new int[]{
                        red, green, blue, // side
                        red, green, blue, // button 2
                        red, green, blue, // button 1
                        blue, green, red  // console
                };
                break;
        }

        return values;
    }

    /**
     * Get the starting register address for the LED enum
     * @param led
     * @return register address
     */
    public static byte getRegisterAddress(LED led) {
        byte registerAddress = 0;
        switch (led) {
            case Side:
            case All:
                registerAddress = LED1_RED_PWM;
                break;
            case Button2:
                registerAddress = LED2_RED_PWM;
                break;
            case Button1:
                registerAddress = LED3_RED_PWM;
                break;
            case Console:
                registerAddress = LED4_BLUE_PWM;
                break;
        }
        return registerAddress;
    }
}
