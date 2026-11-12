package com.jht.mastboard.device;

import android.graphics.Bitmap;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.jht.mastboard.ui.LCDPresentation;

/**
 * LCD for ST7789 that defines the SPI protocol
 */
public class SpiLCD {


    //If you cannot find much information on ST7789V, can also look up ILI9341 -- the registers set is VERY similar
    private final int ST7789_NOP = 0x00;
    private final int ST7789_SWRESET = 0x01;
    private final int ST7789_RDDID = 0x04;
    private final int ST7789_RDDST = 0x09;

    private final int ST7789_SLPIN = 0x10;
    private final int ST7789_SLPOUT = 0x11;
    private final int ST7789_PTLON = 0x12;
    private final int ST7789_NORON = 0x13;

    private final int ST7789_RDMODE = 0x0A;
    private final int ST7789_RDMADCTL = 0x0B;
    private final int ST7789_RDPIXFMT = 0x0C;
    private final int ST7789_RDIMGFMT = 0x0A;
    private final int ST7789_RDSELFDIAG = 0x0F;

    private final int ST7789_INVOFF = 0x20;
    private final int ST7789_INVON = 0x21;
    private final int ST7789_GAMMASET = 0x26;
    private final int ST7789_DISPOFF = 0x28;
    private final int ST7789_DISPON = 0x29;

    private final int ST7789_CASET = 0x2A;
    private final int ST7789_PASET = 0x2B;
    private final int ST7789_RAMWR = 0x2C;
    private final int ST7789_RAMRD = 0x2E;

    private final int ST7789_PTLAR = 0x30;
    private final int ST7789_MADCTL = 0x36;
    private final int ST7789_PIXFMT = 0x3A;

    private final int ST7789_FRMCTR1 = 0xB1;
    private final int ST7789_FRMCTR2 = 0xB2;
    private final int ST7789_FRMCTR3 = 0xB3;
    private final int ST7789_INVCTR = 0xB4;
    private final int ST7789_DFUNCTR = 0xB6;

    private final int ST7789_PWCTR1 = 0xC0;
    private final int ST7789_PWCTR2 = 0xC1;
    private final int ST7789_PWCTR3 = 0xC2;
    private final int ST7789_PWCTR4 = 0xC3;
    private final int ST7789_PWCTR5 = 0xC4;
    private final int ST7789_VMCTR1 = 0xC5;
    private final int ST7789_VMCTR2 = 0xC7;

    private final int ST7789_RDID1 = 0xDA;
    private final int ST7789_RDID2 = 0xDB;
    private final int ST7789_RDID3 = 0xDC;
    private final int ST7789_RDID4 = 0xDD;

    private final int ST7789_GMCTRP1 = 0xE0;
    private final int ST7789_GMCTRN1 = 0xE1;

    private final int ST7789_PWCTR6 = 0xFC;

    private final int ST7789_BLACK = 0x0000;
    private final int ST7789_BLUE = 0x001F;
    private final int ST7789_RED = 0xF800;
    private final int ST7789_GREEN = 0x07E0;
    private final int ST7789_CYAN = 0x07FF;
    private final int ST7789_MAGENTA = 0xF81F;
    private final int ST7789_YELLOW = 0xFFE0;
    private final int ST7789_WHITE = 0xFFFF;

    private final int MADCTL_MY = 0x80;
    private final int MADCTL_MX = 0x40;
    private final int MADCTL_MV = 0x20;
    private final int MADCTL_ML = 0x10;
    private final int MADCTL_RGB = 0x00;
    private final int MADCTL_BGR = 0x08;
    private final int MADCTL_MH = 0x04;


    SpiInterface spi;
    boolean isScreenOn = false;

    private final int numpixels = LCDPresentation.WIDTH * LCDPresentation.HEIGHT;
    private final int numpixels_x2 = LCDPresentation.WIDTH * LCDPresentation.HEIGHT * 2;

    private final int[] buffered_bitmap = new int[numpixels];
    private final int[] buffered_image = new int[numpixels];
    private byte[] bitmap_bytes = new byte[numpixels_x2];

    public SpiLCD(@NonNull SpiInterface spiInterface) {
        this.spi = spiInterface;
        init();
    }

    void init() {
        ST7789_setup();
    }

    public void reset() {
        init();
    }

    private void turnScreenStateST7789(boolean on, boolean isInit) {
        if (isScreenOn == on && !isInit) return;
        isScreenOn = on;
        spi.spiSend((byte) (on ? ST7789_DISPON : ST7789_DISPOFF), new byte[]{});
    }

    // Used to populate the LCD screen. This ST7789 example I had started with was pulled from here : https://github.com/saxond/ST7789Java .
    //They claimed to have ported it over from adafruit's arduino example here : https://home.et.utwente.nl/slootenvanf/2018/03/19/touch-screen-examples/
    //, which I am now porting into Windows, then ported into Android.
    private void ST7789_setup() {
        turnScreenStateST7789(false, true);

        spi.spiSend((byte) ST7789_SWRESET, new byte[]{(byte) 0x00});

        SystemClock.sleep(130);  //"If Software Reset is applied during Sleep Out mode, it will be necessary to wait 120ms before sending Sleep out command."

        spi.spiSend((byte) 0xEF, new byte[]{(byte) 0x03, (byte) 0x80, (byte) 0x02});
        spi.spiSend((byte) 0xCF, new byte[]{(byte) 0x00, (byte) 0xC1, (byte) 0x30});
        spi.spiSend((byte) 0xED, new byte[]{(byte) 0x64, (byte) 0x03, (byte) 0x12, (byte) 0x81});
        spi.spiSend((byte) 0xE8, new byte[]{(byte) 0x85, (byte) 0x00, (byte) 0x78});
        spi.spiSend((byte) 0xCB, new byte[]{(byte) 0x39, (byte) 0x2C, (byte) 0x00, (byte) 0x34, (byte) 0x02});
        spi.spiSend((byte) 0xF7, new byte[]{(byte) 0x20});
        spi.spiSend((byte) 0xEA, new byte[]{(byte) 0x00, (byte) 0x00});

        spi.spiSend((byte) ST7789_PWCTR1, new byte[]{(byte) 0x23}); // Power control , VRH[5:0]
        spi.spiSend((byte) ST7789_PWCTR2, new byte[]{(byte) 0x10}); //Power control , SAP[2:0];BT[3:0]
        spi.spiSend((byte) ST7789_VMCTR1, new byte[]{(byte) 0x2B, (byte) 0x2B}); // VCM control      0x3E, (byte) 0x28      (byte) 0x12, (byte) 0x48
        spi.spiSend((byte) ST7789_VMCTR2, new byte[]{(byte) 0xC0}); // VCM control2                               0x80

        spi.spiSend((byte) ST7789_INVON, new byte[]{});    // Color Inversion ON
        spi.spiSend((byte) ST7789_MADCTL, new byte[]{(byte) MADCTL_MY | MADCTL_MX | MADCTL_MV | MADCTL_RGB});

        spi.spiSend((byte) ST7789_PIXFMT, new byte[]{(byte) 0x55});
        spi.spiSend((byte) ST7789_FRMCTR1, new byte[]{(byte) 0x00, (byte) 0x18});
        spi.spiSend((byte) ST7789_DFUNCTR, new byte[]{(byte) 0x08, (byte) 0x82, (byte) 0x27}); //Display Function Control
        spi.spiSend((byte) ST7789_GAMMASET, (new byte[]{(byte) 0x01})); // Gamma curve selected

        spi.spiSend((byte) ST7789_SLPOUT, new byte[]{});    // Exit Sleep
        SystemClock.sleep(120);

        // setup address window
        byte[] data = new byte[]{0, 0, 0, 0}; // x1, y1, x2, y2
        data[2] = (byte) ((LCDPresentation.WIDTH - 1) >> 8);
        data[3] = (byte) (LCDPresentation.WIDTH - 1);
        spi.spiSend((byte) ST7789_CASET, data);

        data[2] = (byte) ((LCDPresentation.HEIGHT - 1) >> 8);
        data[3] = (byte) ((LCDPresentation.HEIGHT - 1));
        spi.spiSend((byte) ST7789_PASET, data);
    }

    private void drawRGBBitmap(int[] bitmap) {

        int bitmap_index = 0;
        for (int j = 0; j < LCDPresentation.HEIGHT; j++) {
            for (int i = 0; i < LCDPresentation.WIDTH; i++) {
                byte highbyte = (byte) (bitmap[j * LCDPresentation.WIDTH + i] >> 8);
                byte lowbyte = (byte) (bitmap[j * LCDPresentation.WIDTH + i]);

                bitmap_bytes[bitmap_index] = highbyte;
                bitmap_bytes[bitmap_index + 1] = lowbyte;
                bitmap_index = bitmap_index + 2;
            }
        }

        spi.spiSend((byte) ST7789_RAMWR, bitmap_bytes);
        turnScreenStateST7789(true, false); //open the display after drawing the picture complete
    }

    public void drawBitmap(Bitmap bitmap) {
        int pixel, rgb_16;
        byte red, green, blue;
        // cache the pixel buffer to optimize function since getPixel(x,y) is slow
        bitmap.getPixels(buffered_bitmap, 0, LCDPresentation.WIDTH, 0, 0, LCDPresentation.WIDTH, LCDPresentation.HEIGHT);
        for (int x = 0; x < LCDPresentation.WIDTH; x++) {
            for (int y = 0; y < LCDPresentation.HEIGHT; y++) {
                pixel = buffered_bitmap[y * LCDPresentation.WIDTH + x];
                blue = (byte) (pixel & 0xFF);
                green = (byte) ((pixel >> 8) & 0xFF);
                red = (byte) ((pixel >> 16) & 0xFF);
                rgb_16 = ((((red >> 3) & 0x1F) << 11) | (((green >> 2) & 0x3F) << 5) | ((blue >> 3) & 0x1F));
                buffered_image[y * LCDPresentation.WIDTH + x] = rgb_16;
            }
        }
        drawRGBBitmap(buffered_image);
    }

    /**
     * Check if the LCD is responding
     *
     * Read 0x0A register to determine if we are
     * properly connected. With the wide range of responses from the 0A
     * register, it was decided to simplify to checking
     * just 0x00 and 0xFF as invalid responses.
     *
     * Notes on responses from the previous implementation, based
     * on datasheet information
     * (from https://newhavendisplay.com/content/datasheets/ST7789V.pdf) page 170:
     *
     * -104:   indicates a 'DISON 0' (display on is FALSE) response (-104,0 buffer).
     * -100:   indicates DISON 1 (display on is TRUE) response (-100, 0 buffer)
     * -1:     indicates needs to count towards error code if -1 (invalid response)
     *
     * @return true if there's a response; otherwise false
     */
    public boolean isResponding() {
        byte[] rcv_buff = new byte[2];
        spi.spiRead((byte) 0x0A, rcv_buff);
        if (rcv_buff[0] != (byte) 0x00 && rcv_buff[0] != (byte) 0xFF) {
            return true;
        } else {
            return false;
        }
    }
}
