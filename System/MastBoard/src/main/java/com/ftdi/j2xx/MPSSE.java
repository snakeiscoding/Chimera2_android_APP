package com.ftdi.j2xx;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.ftdi.j2xx.D2xxManager.FtDeviceInfoListNode;
import com.jht.util.ContextHelper;

import static com.ftdi.j2xx.D2xxManager.FT_DEVICE_2232H;
import static com.ftdi.j2xx.D2xxManager.FT_DEVICE_232H;
import static com.ftdi.j2xx.D2xxManager.FT_DEVICE_4232H;
import static com.ftdi.j2xx.D2xxManager.FT_PURGE_RX;
import static com.ftdi.j2xx.D2xxManager.FT_PURGE_TX;
import static com.ftdi.j2xx.MPSSE.FT_STATUS.FT_OK;

import androidx.annotation.NonNull;

public class MPSSE {
    private final String TAG = "MPSSE";

    public static final int MID_NO_CHANNEL_FOUND = 0;
    public static final int MIN_CLOCK_RATE = 0;
    public static final int MAX_CLOCK_RATE = 30000000;
    public static final int MIN_LATENCY_TIMER = 0;
    public static final int MAX_LATENCY_TIMER = 255;
    public static final int USB_INPUT_BUFFER_SIZE = 65536;
    public static final int USB_OUTPUT_BUFFER_SIZE = 65536;
    public static final int DISABLE_EVENT = 0;
    public static final int DISABLE_CHAR = 0;
    public static final int DEVICE_READ_TIMEOUT_INFINITE = 0;
    public static final int DEVICE_WRITE_TIMEOUT = 5000;
    public static final int INTERFACE_MASK_IN = 0x00;
    public static final int INTERFACE_MASK_OUT = 0x01;
    public static final int RESET_INTERFACE = 0;
    public static final int ENABLE_MPSSE = 0X02;

    public static final int MID_LOOPBACK_FALSE = 0;
    public static final int MID_LOOPBACK_TRUE = 1;
    public static final int MID_TURN_ON_LOOPBACK_CMD = 0x84;
    public static final int MID_TURN_OFF_LOOPBACK_CMD = 0x85;

    /*MPSSE Control Commands*/
    public static final int MPSSE_CMD_SET_DATA_BITS_LOWBYTE = 0x80;
    public static final int MPSSE_CMD_SET_DATA_BITS_HIGHBYTE = 0x82;
    public static final int MPSSE_CMD_GET_DATA_BITS_LOWBYTE = 0x81;
    public static final int MPSSE_CMD_GET_DATA_BITS_HIGHBYTE = 0x83;

    public static final int MPSSE_CMD_SEND_IMMEDIATE = 0x87;
    public static final int MPSSE_CMD_ENABLE_3PHASE_CLOCKING = 0x8C;
    public static final int MPSSE_CMD_DISABLE_3PHASE_CLOCKING = 0x8D;
    public static final int MPSSE_CMD_ENABLE_DRIVE_ONLY_ZERO = 0x9E;

    /*MPSSE Data Command - LSB First */
    public static final int MPSSE_CMD_DATA_LSB_FIRST = 0x08;

    /*MPSSE Data Commands - bit mode - MSB first */
    public static final int MPSSE_CMD_DATA_OUT_BITS_POS_EDGE = 0x12;
    public static final int MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE = 0x13;
    public static final int MPSSE_CMD_DATA_IN_BITS_POS_EDGE = 0x22;
    public static final int MPSSE_CMD_DATA_IN_BITS_NEG_EDGE = 0x26;
    public static final int MPSSE_CMD_DATA_BITS_IN_POS_OUT_NEG_EDGE = 0x33;
    public static final int MPSSE_CMD_DATA_BITS_IN_NEG_OUT_POS_EDGE = 0x36;


    /*MPSSE Data Commands - byte mode - MSB first */
    public static final int MPSSE_CMD_DATA_OUT_BYTES_POS_EDGE = 0x10;
    public static final int MPSSE_CMD_DATA_OUT_BYTES_NEG_EDGE = 0x11;
    public static final int MPSSE_CMD_DATA_IN_BYTES_POS_EDGE = 0x20;
    public static final int MPSSE_CMD_DATA_IN_BYTES_NEG_EDGE = 0x24;
    public static final int MPSSE_CMD_DATA_BYTES_IN_POS_OUT_NEG_EDGE = 0x31;
    public static final int MPSSE_CMD_DATA_BYTES_IN_NEG_OUT_POS_EDGE = 0x34;

    /*SCL & SDA directions
    * Capacitive Touch Panel:
    * * Pin: 4
    * * Symbol: SDA
    * * External connection: MPU
    * * Function Description: Serial I2C Data (Requires pull-up resistor)
    * */
    public static final int DIRECTION_SCLIN_SDAIN = 0x10;
    public static final int DIRECTION_SCLOUT_SDAIN = 0x11;
    public static final int DIRECTION_SCLIN_SDAOUT = 0x12;
    public static final int DIRECTION_SCLOUT_SDAOUT = 0x13;

    /*SCL & SDA values*/
    public static final int VALUE_SCLLOW_SDALOW = 0x00;
    public static final int VALUE_SCLHIGH_SDALOW = 0x01;
    public static final int VALUE_SCLLOW_SDAHIGH = 0x02;
    public static final int VALUE_SCLHIGH_SDAHIGH = 0x03;

    /*Data size in bits*/
    public static final int DATA_SIZE_8BITS = 0x07;
    public static final int DATA_SIZE_1BIT = 0x00;

    /* The I2C master should actually drive the SDA line only when the output is LOW. It should
    tristate the SDA line when the output should be high. This tristating the SDA line during high
    output is supported only in FT232H chip. This feature is called DriveOnlyZero feature and is
    enabled when the following bit is set in the options parameter in function I2C_Init */
    public static final int I2C_ENABLE_DRIVE_ONLY_ZERO = 0x0002;


    /*clock*/
    public static final int MID_SET_LOW_BYTE_DATA_BITS_CMD = 0x80;
    public static final int MID_GET_LOW_BYTE_DATA_BITS_CMD = 0x81;
    public static final int MID_SET_HIGH_BYTE_DATA_BITS_CMD = 0x82;
    public static final int MID_GET_HIGH_BYTE_DATA_BITS_CMD = 0x83;
    public static final int MID_SET_CLOCK_FREQUENCY_CMD = 0x86;
    public static final int MID_SET_LOW_BYTE_DATA_BITS_DATA = 0x13;
    public static final int MID_SET_HIGH_BYTE_DATA_BITS_DATA = 0x0F;
    public static final int MID_6MHZ = 6000000;
    public static final int MID_30MHZ = 30000000;

    public static final int DISABLE_CLOCK_DIVIDE = 0x8A;
    public static final int ENABLE_CLOCK_DIVIDE = 0x8B;

    final boolean FT800_232HM = true; // TODO check if this should be true or false

    public static final int MID_ECHO_COMMAND_ONCE = 0;
    public static final int MID_ECHO_COMMAND_CONTINUOUSLY = 1;
    public static final byte MID_ECHO_CMD_1 = (byte) 0xAA;
    public static final byte MID_ECHO_CMD_2 = (byte) 0xAB;
    public static final byte MID_BAD_COMMAND_RESPONSE = (byte) 0xFA;
    public static final byte MID_CMD_NOT_ECHOED = (byte) 0;
    public static final byte MID_CMD_ECHOED = (byte) 1;

    public static final int MID_MAX_IN_BUF_SIZE = 4096;
    byte readBuffer[] = new byte[MID_MAX_IN_BUF_SIZE];
    public static final int MAX_BUF_SIZE_SPI = 64 * 1024;
    byte spiBuffer[] = new byte[MAX_BUF_SIZE_SPI];

    /* Options to I2C_DeviceWrite & I2C_DeviceRead */
    /*Generate start condition before transmitting */
    public static final int I2C_TRANSFER_OPTIONS_START_BIT = 0x00000001;

    /*Generate stop condition before transmitting */
    public static final int I2C_TRANSFER_OPTIONS_STOP_BIT = 0x00000002;

    /*Continue transmitting data in bulk without caring about Ack or nAck from device if this bit
    is not set. If this bit is set then stop transferring the data in the buffer when the device
    nACKs */
    public static final int I2C_TRANSFER_OPTIONS_BREAK_ON_NACK = 0x00000004;

    /* libMPSSE-I2C generates an ACKs for every byte read. Some I2C slaves require the I2C
    master to generate a nACK for the last data byte read. Setting this bit enables working with
    such I2C slaves */
    public static final int I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE = 0x00000008;

    /*Fast transfers prepare a buffer containing commands to generate START/STOP/ADDRESS
       conditions and commands to read/write data. This buffer is sent to the MPSSE in one shot,
       hence delays between different phases of the I2C transfer are eliminated. Fast transfers
       can have data length in terms of bits or bytes. The user application should call
       I2C_DeviceWrite or I2C_DeviceRead with either
       I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES or
       I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BITS bit set to perform a fast transfer.
       I2C_TRANSFER_OPTIONS_START_BIT and I2C_TRANSFER_OPTIONS_STOP_BIT have
       their usual meanings when used in fast transfers, however
       I2C_TRANSFER_OPTIONS_BREAK_ON_NACK and
       I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE are not applicable in fast transfers */
    public static final int I2C_TRANSFER_OPTIONS_FAST_TRANSFER = 0x00000030;/*not visible to user*/

    /* When the user calls I2C_DeviceWrite or I2C_DeviceRead with this bit set then libMPSSE
         packs commands to transfer sizeToTransfer number of bytes, and to read/write
         sizeToTransfer number of ack bits. If data is written then the read ack bits are ignored, if
         data is being read then an acknowledgement bit(SDA=LOW) is given to the I2C slave
         after each byte read */
    public static final int I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES = 0x00000010;

    /* When the user calls I2C_DeviceWrite or I2C_DeviceRead with this bit set then libMPSSE
         packs commands to transfer sizeToTransfer number of bits. There is no ACK phase when
         this bit is set */
    public static final int I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BITS = 0x00000020;

    /* The address parameter is ignored in fast transfers if this bit is set. This would mean that
         the address is either a part of the data or this is a special I2C frame that doesn't require
         an address. However if this bit is not set then 7bit address and 1bit direction will be
         written to the I2C bus each time I2C_DeviceWrite or I2C_DeviceRead is called and a
         1bit acknowledgement will be read after that, which will however be just ignored */
    public static final int I2C_TRANSFER_OPTIONS_NO_ADDRESS = 0x00000040;


    public static final int I2C_CMD_GETDEVICEID_RD = 0xF9;
    public static final int I2C_CMD_GETDEVICEID_WR = 0xF8;

    public static final int I2C_GIVE_ACK = 1;
    public static final int I2C_GIVE_NACK = 0;

    /* 3-phase clocking is enabled by default. Setting this bit in ConfigOptions will disable it */
    public static final int I2C_DISABLE_3PHASE_CLOCKING = 0x0001;

    public static final int START_DURATION_1 = 10;
    public static final int START_DURATION_2 = 20;

    public static final int STOP_DURATION_1 = 10;
    public static final int STOP_DURATION_2 = 10;
    public static final int STOP_DURATION_3 = 10;
    public static final int SEND_ACK = 0x00;
    public static final int SEND_NACK = 0x80;

    public static final int I2C_ADDRESS_READ_MASK = 0x01;    /*LSB 1 = Read*/
    public static final int I2C_ADDRESS_WRITE_MASK = 0xFE;    /*LSB 0 = Write*/

    public enum FT_STATUS {
        FT_OK,
        FT_INVALID_HANDLE,
        FT_DEVICE_NOT_FOUND,
        FT_DEVICE_NOT_OPENED,
        FT_IO_ERROR,
        FT_INSUFFICIENT_RESOURCES,
        FT_INVALID_PARAMETER,
        FT_INVALID_BAUD_RATE,

        FT_DEVICE_NOT_OPENED_FOR_ERASE,
        FT_DEVICE_NOT_OPENED_FOR_WRITE,
        FT_FAILED_TO_WRITE_DEVICE,
        FT_EEPROM_READ_FAILED,
        FT_EEPROM_WRITE_FAILED,
        FT_EEPROM_ERASE_FAILED,
        FT_EEPROM_NOT_PRESENT,
        FT_EEPROM_NOT_PROGRAMMED,
        FT_INVALID_ARGS,
        FT_NOT_SUPPORTED,
        FT_OTHER_ERROR,
        FT_DEVICE_LIST_NOT_READY
    }

    public static enum FT_LegacyProtocol {SPI, I2C, JTAG}

    public static D2xxManager2 ftdid2xx = null;
    Context SPIContext;
    public FT_Device ft_device1;
    FT_Device ft_device2;

    public MPSSE() {
        SPIContext = ContextHelper.INSTANCE.getContext();
        try {
            ftdid2xx = D2xxManager2.getInstance(SPIContext);
        } catch (D2xxManager.D2xxException e) {
            e.printStackTrace();
        }
    }

    public int FT_GetNumChannels() {
        return ftdid2xx.createDeviceInfoList(SPIContext);
    }

    public FtDeviceInfoListNode FT_GetChannelInfo(int index) {
        return ftdid2xx.getDeviceInfoListDetail(index);
    }

    public FT_Device FT_OpenChannel(int index) {
        return ftdid2xx.openByIndex(SPIContext, index);
    }

    public void FT_CloseChannel(FT_Device ft_device) {
        ft_device.close();
    }

    /*!
     *
     * This function initializes the channel and the communication parameters associated with it. The
     * function takes variable number of parameters. For example, if channelType is I2C then the
     * number of variable parameters will be equal to the number of members in the structure
     * ChannelConfig. The variable parameters will be passed to this function in the same order as
     * they appear in the structure definition
     * The function performs the USB function specific initialization followed by MPSSE initialization.
     * Once that is done it will configure MPSSE with the legacy protocol specific initializations(SPI/
     * I2C/JTAG) with the help of that parameters that are passed by the caller
     *
     */
    public void FT_InitChannel(FT_LegacyProtocol Protocol, FT_Device ft_device, int clockRate, int latencyTimer, int configOptions) {
        /*Check parameters*/
        if ((clockRate < MIN_CLOCK_RATE) || (clockRate > MAX_CLOCK_RATE) || (latencyTimer <
                MIN_LATENCY_TIMER) || (latencyTimer > MAX_LATENCY_TIMER))
            return;

        /*reset the device*/
        Mid_ResetDevice(ft_device);
        /*Purge*/
        Mid_PurgeDevice(ft_device);
        /*set USB buffer size*/
        Mid_SetUSBParameters(ft_device, USB_INPUT_BUFFER_SIZE, USB_OUTPUT_BUFFER_SIZE);
        /*sets the special characters for the device, disable event and error characters*/
        Mid_SetDeviceSpecialChar(ft_device, (byte) 0, (byte) DISABLE_EVENT, (byte) 0, (byte) DISABLE_CHAR);
        /*SetTimeOut*/
        Mid_SetDeviceTimeOut(ft_device, 5000, DEVICE_WRITE_TIMEOUT);

        /*SetLatencyTimer*/
        Mid_SetLatencyTimer(ft_device, (byte) latencyTimer);
        /*ResetMPSSE*/
        Mid_ResetMPSSE(ft_device);
        /*EnableMPSSEInterface*/
        Mid_EnableMPSSEIn(ft_device);
        /*20110608 - enabling loopback before sync*/
        Mid_SetDeviceLoopbackState(ft_device, MID_LOOPBACK_TRUE);
        /*Sync MPSSE */
        FT_STATUS status = Mid_SyncMPSSE(ft_device);
        if (status != FT_OK) {
            Log.e("d2xx", "Mid_SyncMPSSE: " + status);
        }
        /*wait for USB*/
        SystemClock.sleep(50);
        /*set Clock frequency*/
        Mid_SetClock(ft_device, clockRate);
        SystemClock.sleep(20);
        /*Stop Loop back*/
        Mid_SetDeviceLoopbackState(ft_device, MID_LOOPBACK_FALSE);
        Mid_EmptyDeviceInputBuff(ft_device);

        switch (Protocol) {
            case I2C: {
                /*Set i/o pin states*/
                Mid_SetGPIOLow(ft_device, (byte) MID_SET_LOW_BYTE_DATA_BITS_DATA, (byte) 0x00); //value,direction -- 0 = input, 1 = output, 0=low if output, 1=high if output

                /* The I2C master should actually drive the SDA line only when the output is LOW.
                It should tristate the SDA line when the output should be high. This tristating
                the SDA line during output HIGH is supported only in FT232H chip*/
                int option = configOptions & I2C_ENABLE_DRIVE_ONLY_ZERO;
                if ((ft_device.getDeviceInfo().type == FT_DEVICE_232H) && (option > 0)) {
                    byte[] buffer = new byte[3];
                    buffer[0] = (byte) MPSSE_CMD_ENABLE_DRIVE_ONLY_ZERO;/* MPSSE command */
                    buffer[1] = 0x03; /* LowByte */
                    buffer[2] = 0x00; /* HighByte */
                    FT_Channel_Write(ft_device, 3, buffer, true);
                }
            }
            break;

            case SPI: {
            }
            break;

            case JTAG:
                break;

        }

    }


    /*!
     *
     * This function reads the specified number of bytes from channel.
     * returns the number of bytes read
     */
    public int FT_Channel_Read(FT_Device ft_device, int noOfBytes, byte buffer[]) {
        return ft_device.read(buffer, noOfBytes);
    }

    /*!
     * This function writes the specified number of bytes to the channel
     * returns the number of bytes written
     *
     */
    public int FT_Channel_Write(FT_Device ft_device, int noOfBytes, byte[] buffer, boolean wait) {
        return ft_device.write(buffer, noOfBytes, wait);
    }


    public boolean Mid_CheckMPSSEAvailable(FtDeviceInfoListNode devList) {
        boolean isMPSSEAvailable = false;

        int lastChar = devList.description.length() - 1;

        switch (devList.type) {
            case FT_DEVICE_2232H:
                if ((devList.description.charAt(lastChar) == 0x41) || (devList.description.charAt(lastChar) == 0x42))  //Last character = 0x41 = ASCII "A", 0x42 = ASCII "B"
                {
                    isMPSSEAvailable = true;
                }
                break;
            case FT_DEVICE_4232H:
                if ((devList.description.charAt(lastChar) == 0x41) || (devList.description.charAt(lastChar) == 0x42))  //Last character = 0x41 = ASCII "A", 0x42 = ASCII "B"
                {
                    isMPSSEAvailable = true;
                }
                break;
            case FT_DEVICE_232H:
                isMPSSEAvailable = true;
                break;
            default:
                break;
        }

        return isMPSSEAvailable;
    }

    public void Mid_ResetDevice(FT_Device ft_device) {
        ft_device.resetDevice();
    }

    public void Mid_PurgeDevice(FT_Device ft_device) {
        ft_device.purge((byte) (FT_PURGE_RX | FT_PURGE_TX));
    }

    public void Mid_SetUSBParameters(FT_Device ft_device, int inputBufSize, int outputBufSize) {
        D2xxManager.DriverParameters driverParameters = ft_device.getDriverParameters();
        driverParameters.setMaxBufferSize(inputBufSize);
        driverParameters.setMaxTransferSize(outputBufSize);
        ft_device.setDriverParameters(driverParameters);
    }

    public void Mid_SetDeviceSpecialChar(FT_Device ft_device, byte eventCh, byte eventStatus, byte errorCh, byte errorStatus) {
        ft_device.setChars(eventCh, eventStatus, errorCh, errorStatus);
    }


    public void Mid_SetDeviceTimeOut(FT_Device ft_device, int rdTimeOut, int wrTimeOut) {
        // TODO there doesn't seem to be write timeout equivalent
        D2xxManager.DriverParameters driverParameters = ft_device.getDriverParameters();
        driverParameters.setReadTimeout(rdTimeOut);
        ft_device.setDriverParameters(driverParameters);
    }


    public void Mid_SetLatencyTimer(FT_Device ft_device, byte milliSecond) {
        ft_device.setLatencyTimer(milliSecond);
    }

    public void Mid_ResetMPSSE(FT_Device ft_device) {
        ft_device.setBitMode((byte) INTERFACE_MASK_IN, (byte) RESET_INTERFACE);
    }

    public void Mid_EnableMPSSEIn(FT_Device ft_device) {
        ft_device.setBitMode((byte) INTERFACE_MASK_IN, (byte) ENABLE_MPSSE);
    }


    public FT_STATUS Mid_SyncMPSSE(FT_Device ft_device) {
        FT_STATUS status = FT_STATUS.FT_OK;
        byte cmdEchoed;

        Mid_EmptyDeviceInputBuff(ft_device);
        /*send and receive command*/
        cmdEchoed = Mid_SendReceiveCmdFromMPSSE(ft_device, (byte) MID_ECHO_COMMAND_CONTINUOUSLY, (byte) MID_ECHO_CMD_1);

        if (cmdEchoed == MID_CMD_ECHOED) {
            cmdEchoed = Mid_SendReceiveCmdFromMPSSE(ft_device, (byte) MID_ECHO_COMMAND_ONCE, (byte) MID_ECHO_CMD_2);
            if (cmdEchoed != MID_CMD_ECHOED) {
                status = FT_STATUS.FT_OTHER_ERROR;
            }
        } else {
            status = FT_STATUS.FT_OTHER_ERROR;
        }
        return status;
    }


    /*
     * sends the data and read it back from the device
     * returns cmdEchoed byte
     */
    public byte Mid_SendReceiveCmdFromMPSSE(FT_Device ft_device, byte echoCmdFlag, byte ecoCmd) {
        int bytesInInputBuf = 0;
        int numOfBytesRead = 0;
        int byteCounter;
        byte cmdResponse = MID_CMD_NOT_ECHOED;
        int loopCounter = 0;

        /*initialize cmdEchoed to MID_CMD_NOT_ECHOED*/
        byte cmdEchoed = MID_CMD_NOT_ECHOED;
        /* check whether command has to be sent only once*/
        if (echoCmdFlag == MID_ECHO_COMMAND_ONCE) {
            readBuffer[0] = ecoCmd;
            ft_device.write(readBuffer, 1);
        }

        do {
            /*check whether command has to be sent every time in the loop*/
            if (echoCmdFlag == MID_ECHO_COMMAND_CONTINUOUSLY) {
                readBuffer[0] = ecoCmd;
                ft_device.write(readBuffer, 1);
            }
            /*read the no of bytes available in Receive buffer*/
            bytesInInputBuf = ft_device.getQueueStatus();
            SystemClock.sleep(1);
            if (bytesInInputBuf > 0) {
                if (bytesInInputBuf > MID_MAX_IN_BUF_SIZE) throw new OutOfMemoryError();
                numOfBytesRead = ft_device.read(readBuffer, bytesInInputBuf);
                if (numOfBytesRead > 0) {
                    byteCounter = 0;
                    do {
                        if (byteCounter <= (numOfBytesRead - 1)) {
                            if (readBuffer[byteCounter] == MID_BAD_COMMAND_RESPONSE) {
                                cmdResponse = (byte) MID_BAD_COMMAND_RESPONSE;
                            } else {
                                if (cmdResponse == MID_BAD_COMMAND_RESPONSE) {
                                    if (readBuffer[byteCounter] == ecoCmd) {
                                        cmdEchoed = MID_CMD_ECHOED;
                                    }
                                }
                                cmdResponse = MID_CMD_NOT_ECHOED;
                            }
                        }
                        byteCounter++;
                    } while ((byteCounter < bytesInInputBuf) && (cmdEchoed == MID_CMD_NOT_ECHOED));
                }
            }

            /*for breaking the loop */
            loopCounter++;
            if (loopCounter > MID_MAX_IN_BUF_SIZE) {
                break;
            }
        } while ((cmdEchoed == MID_CMD_NOT_ECHOED));

        return cmdEchoed;
    }


    /*
     * This function  sets the pin state to 0x13 for low bits data and 0x0f for high bit data
     */
    public void Mid_SetGPIOLow(FT_Device ft_device, byte value, byte direction) {
        byte inputBuffer[] = new byte[10];
        int bytesWritten = 0;
        int bufIdx = 0;

        pinStateB |= direction;
        pinStateB |= (value << 8);

        inputBuffer[bufIdx++] = (byte) MID_SET_LOW_BYTE_DATA_BITS_CMD;
        inputBuffer[bufIdx++] = value;//0x13;
        inputBuffer[bufIdx++] = direction;//0x13;
        ft_device.write(inputBuffer, bufIdx);
    }

    public FtDeviceInfoListNode Mid_GetFtDeviceType(FT_Device ft_device) {
        return ft_device.getDeviceInfo();
    }

    public void Mid_SetClock(FT_Device ft_device, int clock) {
        byte inputBuffer[] = new byte[10];
        int bufIdx = 0;
        byte valueH, valueL;
        int value;

        switch (ft_device.getDeviceInfo().type) {
            default:/* Assuming all new chips will he HS MPSSE devices */
                /* Fall through */
            case FT_DEVICE_2232H:
            case FT_DEVICE_4232H:
            case FT_DEVICE_232H:
                if (clock <= MID_6MHZ) {
                    inputBuffer[0] = (byte) ENABLE_CLOCK_DIVIDE;
                    ft_device.write(inputBuffer, 1);
                    value = (MID_6MHZ / clock) - 1;
                } else {
                    inputBuffer[0] = (byte) DISABLE_CLOCK_DIVIDE;
                    ft_device.write(inputBuffer, 1);
                    value = (MID_30MHZ / clock) - 1;
                }
                break;
        }
        /*calculate valueH and ValueL*/
        valueL = (byte) value;
        valueH = (byte) (value >> 8);
        /*set the clock*/
        inputBuffer[bufIdx++] = (byte) MID_SET_CLOCK_FREQUENCY_CMD;
        inputBuffer[bufIdx++] = valueL;
        inputBuffer[bufIdx++] = valueH;
        ft_device.write(inputBuffer, bufIdx);
    }

    public void Mid_SetDeviceLoopbackState(FT_Device ft_device, int loopBackFlag) {
        byte inputBuffer[] = new byte[1];
        int bufIdx = 0;

        if (loopBackFlag == MID_LOOPBACK_FALSE) {
            inputBuffer[bufIdx++] = (byte) MID_TURN_OFF_LOOPBACK_CMD;
        } else {
            inputBuffer[bufIdx++] = (byte) MID_TURN_ON_LOOPBACK_CMD;
        }
        ft_device.write(inputBuffer);
    }


    public void Mid_EmptyDeviceInputBuff(FT_Device ft_device) {
        int bytesInInputBuf = 0;
        int numOfBytesRead = 0;

        bytesInInputBuf = ft_device.getQueueStatus();
        if (bytesInInputBuf > 0) {
            do {
                if (bytesInInputBuf > MID_MAX_IN_BUF_SIZE) {
                    numOfBytesRead = ft_device.read(readBuffer);
                    bytesInInputBuf = bytesInInputBuf - numOfBytesRead;
                } else {
                    numOfBytesRead = ft_device.read(readBuffer, bytesInInputBuf);
                    bytesInInputBuf = bytesInInputBuf - numOfBytesRead;
                }
            } while (bytesInInputBuf != 0);
        }
    }


    public void SetGPIOOutput(Pins pin, GPIO_Value value) {
        //All of the pins were defaulted to inputs, but if this is called, then this pin must be desired to be an output
        //int pinState;       // this is organized as ACVal ACDirection ADVal ADDirection (device1 only)
//MPSSE.FT_WriteGPIOHighByte(ft_device1, (byte) 0x80, (byte) 0x7F);    // device, dir, value.  AD7-->low.    0x00000080
        if (((pin.dir_mask & 0x000000FF) != 0) && ((pin.dir_mask & 0x80000000) == 0)) { // is AD port
            pinState |= pin.dir_mask; //set to output if it isn't already

            if (value == GPIO_Value.HIGH) {
                pinState |= pin.val_mask; //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinState &= (~pin.val_mask); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinState & 0x0000FF00) >> 8);
            byte dir = (byte) (pinState & 0x000000FF);

            FT_WriteGPIOHighByte(ft_device1, dir, val);
        } else if (((pin.dir_mask & 0x00FF0000) != 0) && ((pin.dir_mask & 0x80000000) == 0)) { // is AC port
            pinState |= pin.dir_mask; //set to output if it isn't already

            if (value == GPIO_Value.HIGH) {
                pinState |= pin.val_mask; //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinState &= (~pin.val_mask); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinState & 0xFF000000) >> 24);
            byte dir = (byte) ((pinState & 0x00FF0000) >> 16);

            FT_WriteGPIO(ft_device1, dir, val);
        } else if (((pin.dir_mask & 0x000000FF) != 0) && ((pin.dir_mask & 0x80000000) != 0)) { //is BD port
            pinStateB |= pin.dir_mask; //set to output if it isn't already

            if (value == GPIO_Value.HIGH) {
                pinStateB |= (pin.val_mask & 0x7FFFFFFF); //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinStateB &= (~(pin.val_mask & 0x7FFFFFFF)); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            FT_WriteGPIOHighByte(ft_device2, dir, val);
        } else if (((pin.dir_mask & 0x00FF0000) != 0) && ((pin.dir_mask & 0x80000000) != 0)) { //is BC port
            pinStateB |= pin.dir_mask; //set to output if it isn't already

            if (value == GPIO_Value.HIGH) {
                pinStateB |= (pin.val_mask & 0x7FFFFFFF); //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinStateB &= (~(pin.val_mask & 0x7FFFFFFF)); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinStateB & 0xFF000000) >> 24);
            byte dir = (byte) ((pinStateB & 0x00FF0000) >> 16);

            FT_WriteGPIO(ft_device2, dir, val);
        }
        //else ERROR
    }
    public void SetGPIOInput(Pins pin, GPIO_Value value) {
        //All of the pins were defaulted to inputs, but if this is called, then this pin must be desired to be an output
        //int pinState;       // this is organized as ACVal ACDirection ADVal ADDirection (device1 only)
//MPSSE.FT_WriteGPIOHighByte(ft_device1, (byte) 0x80, (byte) 0x7F);    // device, dir, value.  AD7-->low.    0x00000080
        if (((pin.dir_mask & 0x000000FF) != 0) && ((pin.dir_mask & 0x80000000) == 0)) { // is AD port
            pinState &= (0xFF&pin.dir_mask);
            if (value == GPIO_Value.HIGH) {
                pinState |= pin.val_mask; //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinState &= (~pin.val_mask); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinState & 0x0000FF00) >> 8);
            byte dir = (byte) (pinState & 0x000000FF);

            FT_WriteGPIOHighByte(ft_device1, dir, val);
        } else if (((pin.dir_mask & 0x00FF0000) != 0) && ((pin.dir_mask & 0x80000000) == 0)) { // is AC port
            pinState &= (0xFF&pin.dir_mask);
            if (value == GPIO_Value.HIGH) {
                pinState |= pin.val_mask; //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinState &= (~pin.val_mask); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinState & 0xFF000000) >> 24);
            byte dir = (byte) ((pinState & 0x00FF0000) >> 16);

            FT_WriteGPIO(ft_device1, dir, val);
        } else if (((pin.dir_mask & 0x000000FF) != 0) && ((pin.dir_mask & 0x80000000) != 0)) { //is BD port

            pinStateB &= (0xFF&pin.dir_mask);
            if (value == GPIO_Value.HIGH) {
                pinStateB |= (pin.val_mask & 0x7FFFFFFF); //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinStateB &= (~(pin.val_mask & 0x7FFFFFFF)); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            FT_WriteGPIOHighByte(ft_device2, dir, val);
        } else if (((pin.dir_mask & 0x00FF0000) != 0) && ((pin.dir_mask & 0x80000000) != 0)) { //is BC port

            pinStateB &= (0xFF&pin.dir_mask);
            if (value == GPIO_Value.HIGH) {
                pinStateB |= (pin.val_mask & 0x7FFFFFFF); //will be | with 0's and a 1, or & with F's and a 0
            } else {
                pinStateB &= (~(pin.val_mask & 0x7FFFFFFF)); //will be | with 0's and a 1, or & with F's and a 0
            }

            byte val = (byte) ((pinStateB & 0xFF000000) >> 24);
            byte dir = (byte) ((pinStateB & 0x00FF0000) >> 16);

            FT_WriteGPIO(ft_device2, dir, val);
        }
        //else ERROR
    }
    // return bytes written
    private synchronized int FT_WriteGPIO(FT_Device ft_device, byte dir, byte value) { //only works for AC port (Low Byte).
        byte buffer[] = new byte[3];
        int bufIdx = 0;

        if (FT800_232HM) {
            buffer[bufIdx++] = (byte) MPSSE_CMD_SET_DATA_BITS_HIGHBYTE;
            buffer[bufIdx++] = value;
            buffer[bufIdx++] = dir;
        } else {
            buffer[bufIdx++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[bufIdx++] = value;
            buffer[bufIdx++] = dir;
        }
        return ft_device.write(buffer);
    }


    public GPIO_Value ReadGPIOPin(FT_Device ft_device, Pins pin) { //does not force the pin to input.  The value of output pins can also be read through this function
        //INPUT REQUIREMENT : specify ftdevice1 for AD/AC ports, ftdevice2 for BD/BC ports
        byte tmp;
        GPIO_Value retval = GPIO_Value.ERROR;

        if (countSetBits((int) pin.val_mask) == 1) { //can only return value of one bit -- use ReadGPIOPinByte for checking multiple pins at same time.  return GPIO_Value.ERROR if multiple bits are requested through this function.
            if ((pin.dir_mask & 0x000000FF) != 0) { // is AD or BD port
                tmp = FT_ReadGPIOHighByte(ft_device);
                retval = ((tmp & (pin.dir_mask)) != 0) ? GPIO_Value.HIGH : GPIO_Value.LOW;
            } else if ((pin.dir_mask & 0x00FF0000) != 0) { // is AC or BC port
                tmp = FT_ReadGPIO(ft_device);
                retval = ((tmp & (pin.dir_mask >> 16)) != 0) ? GPIO_Value.HIGH : GPIO_Value.LOW;
            }
        } else {
            Log.d("ReadGPIOPin error : ", "Use the ReadGPIOByte instead.");
        }

        return retval;
    }


    public byte ReadGPIOPinByte(Pins pin) {
        //returns the full byte of the port that the pin belongs to.
        //INPUT REQUIREMENT : specify ftdevice1 for AD/AC ports, ftdevice2 for BD/BC ports
        byte tmp = 0x00;

        if (((pin.dir_mask & 0x000000FF) != 0) && ((pin.dir_mask & 0x80000000) == 0)) { // is AD port
            tmp = FT_ReadGPIOHighByte(ft_device1);
        } else if (((pin.dir_mask & 0x00FF0000) != 0) && ((pin.dir_mask & 0x80000000) == 0)) { // is AC port
            tmp = FT_ReadGPIO(ft_device1);
        } else if (((pin.dir_mask & 0x000000FF) != 0) && ((pin.dir_mask & 0x80000000) != 0)) { //is BD port
            tmp = FT_ReadGPIOHighByte(ft_device2);
        } else if (((pin.dir_mask & 0x00FF0000) != 0) && ((pin.dir_mask & 0x80000000) != 0)) { //is BC port
            tmp = FT_ReadGPIO(ft_device2);
        }

        return tmp;
    }


    private static int countSetBits(int n) {  //Brian Kernighan’s algorithm
        int count = 0;
        while (n > 0) {
            n &= (n - 1);
            count++;
        }
        return count;
    }


    // returns value Input state of the 8 GPIO lines(1=high)
    private synchronized byte FT_ReadGPIO(FT_Device ft_device) {
        byte gpioValue;
        byte buffer[] = new byte[2];
        int bytesToTransfer = 0;
        byte readBuffer[] = new byte[10];

        if (FT800_232HM) {
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_GET_DATA_BITS_HIGHBYTE;
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;
        } else {
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_GET_DATA_BITS_LOWBYTE;
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;
        }
        ft_device.write(buffer, 2);

        int readBytes = ft_device.read(readBuffer, 1);
        gpioValue = readBuffer[0];
        return gpioValue;
    }


    private byte FT_ReadGPIOHighByte(FT_Device ft_device) {
        byte gpioValue;
        byte buffer[] = new byte[2];
        int bytesToTransfer = 0;
        byte readBuffer[] = new byte[10];

        if (FT800_232HM) {
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_GET_DATA_BITS_LOWBYTE;
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;
        } else {
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_GET_DATA_BITS_HIGHBYTE;
            buffer[bytesToTransfer++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;
        }

        ft_device.write(buffer, 2);

        int readBytes = ft_device.read(readBuffer, 1);
        gpioValue = readBuffer[0];
        return gpioValue;
    }


    private synchronized int FT_WriteGPIOHighByte(FT_Device ft_device, byte dir, byte value) {
        byte buffer[] = new byte[3];
        int bufIdx = 0;

        if (FT800_232HM) {
            buffer[bufIdx++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[bufIdx++] = value;
            buffer[bufIdx++] = dir;
        } else {
            buffer[bufIdx++] = (byte) MPSSE_CMD_SET_DATA_BITS_HIGHBYTE;
            buffer[bufIdx++] = value;
            buffer[bufIdx++] = dir;
        }
        return ft_device.write(buffer);
    }


    public int Mid_GetQueueStatus(FT_Device ft_device) {
        return ft_device.getQueueStatus();
    }



    /* Bit definition of the transferOptions parameter in SPI_Read, SPI_Write & SPI_Transfer  */

    /* transferOptions-Bit0: If this bit is 0 then it means that the transfer size provided is in bytes */
    public static final int SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES = 0x00000000;
    /* transferOptions-Bit0: If this bit is 1 then it means that the transfer size provided is in bits */
    public static final int SPI_TRANSFER_OPTIONS_SIZE_IN_BITS = 0x00000001;
    /* transferOptions-Bit1: if BIT1 is 1 then CHIP_SELECT line will be enabled at start of transfer */
    public static final int SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE = 0x00000002;
    /* transferOptions-Bit2: if BIT2 is 1 then CHIP_SELECT line will be disabled at end of transfer */
    public static final int SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE = 0x00000004;
    /* transferOptions-Bit3: if BIT3 is 1 then LSB will be processed first */
    public static final int SPI_TRANSFER_OPTIONS_LSB_FIRST = 0x00000008;


    /* Bit definition of the Options member of configOptions structure */
    public static final int SPI_CONFIG_OPTION_MODE_MASK = 0x00000003;
    public static final int SPI_CONFIG_OPTION_MODE0 = 0x00000000;
    public static final int SPI_CONFIG_OPTION_MODE1 = 0x00000001;
    public static final int SPI_CONFIG_OPTION_MODE2 = 0x00000002;
    public static final int SPI_CONFIG_OPTION_MODE3 = 0x00000003;

    public static final int SPI_CONFIG_OPTION_CS_MASK = 0x0000001C;        /* 111 00 */
    public static final int SPI_CONFIG_OPTION_CS_DBUS3 = 0x00000000;        /* 000 00 */
    public static final int SPI_CONFIG_OPTION_CS_DBUS4 = 0x00000004;        /* 001 00 */
    public static final int SPI_CONFIG_OPTION_CS_DBUS5 = 0x00000008;        /* 010 00 */
    public static final int SPI_CONFIG_OPTION_CS_DBUS6 = 0x0000000C;        /* 011 00 */
    public static final int SPI_CONFIG_OPTION_CS_DBUS7 = 0x00000010;        /* 100 00 */

    public static final int SPI_CONFIG_OPTION_CS_ACTIVELOW = 0x00000020;

    int clockRate;
    int latencyTimer;
    int configOptions;
    int pinState;       // this is organized as ACVal ACDirection ADVal ADDirection (device1 only).  Does not reflect input values, only directions
    int pinStateB;       // this is organized as BCVal BCDirection BDVal BDDirection (device2 only).

    public enum Pins {
        //AD0 (0x00000001), //Shouldn't set these in this project -- configured by the SPI_InitChannel function
        //AD1 (0x00000002), //Shouldn't set these in this project -- configured by the SPI_InitChannel function
        //AD2 (0x00000004), //Shouldn't set these in this project -- configured by the SPI_InitChannel function
        AD3(0x00000008),
        AD4(0x00000010),
        AD5(0x00000020),
        AD6(0x00000040),
        AD7(0x00000080),

        AC0(0x00010000),
        AC1(0x00020000),
        AC2(0x00040000),
        AC3(0x00080000),
        AC4(0x00100000),
        AC5(0x00200000),
        AC6(0x00400000),
        AC7(0x00800000),

        //due to lack of unsigned 64bit int's that support >> operations, will reuse the MSB to detect A vs B device and then remove that bit before use.
        BD0(0x80000001), //Shouldn't set these in this project -- configured by the I2C_InitChannel function
        BD1(0x80000002), //Shouldn't set these in this project -- configured by the I2C_InitChannel function
        BD2(0x80000004), //Shouldn't set these in this project -- configured by the I2C_InitChannel function
        BD3(0x80000008),
        BD4(0x80000010),
        BD5(0x80000020),
        BD6(0x80000040),
        BD7(0x80000080),

        BC0(0x80010000),
        BC1(0x80020000),
        BC2(0x80040000),
        BC3(0x80080000),
        BC4(0x80100000),
        BC5(0x80200000),
        BC6(0x80400000),
        BC7(0x80800000),

//        BOTH_SPI_CS_PINS(AC0.dir_mask | AC1.dir_mask), //AC0 and AC1.  This method only works if both pins are on the same port.
//        BOTH_SPI_DC_PINS(AC2.dir_mask | AC3.dir_mask), //AC2 and AC3.  This method only works if both pins are on the same port.
        BOTH_I2C_INT_PINS(BD3.dir_mask | BD4.dir_mask),
        BOTH_UART_INT_PINS(BD6.dir_mask | BD7.dir_mask),
        BOTH_QI_STATUS_PINS(BC0.dir_mask | BC1.dir_mask),
        ALL_AC_PINS(0x00FF0000),
        ALL_AD_PINS(0x000000FF),
        ALL_BC_PINS(0x80FF0000),
        ALL_BD_PINS(0x800000FF);

        public long dir_mask;
        public long val_mask;

        Pins(long mask) {
            this.dir_mask = mask;
            this.val_mask = mask << 8;
        }
    }

    public enum GPIO_Value {
        HIGH(true),
        LOW(false),
        ERROR(false);
        private boolean isHigh;

        GPIO_Value(boolean is_high) {
            this.isHigh = is_high;
        }
    }

    public boolean isFullyInit() {
        return isSpiChannelInit() && isI2CChannelInit();
    }

    public boolean isSpiChannelInit() {
        return ft_device1 != null;
    }

    public boolean isI2CChannelInit() {
        return ft_device2 != null;
    }

    /*
     * This function initializes the channel and the communication parameters associated with it
     */
    public void SPI_InitChannel(int clockRate, int latencyTimer, int configOptions) {
        byte[] buffer = new byte[5];
        int noOfBytes = 0;
        int mode;

        ft_device1 = FT_OpenChannel(0); //SPI on channel 0 / A-bus
        Log.d(TAG, "ft_device1: " + ft_device1);

        /*This is to ensure that the library puts the lines to correct directions
        even if wrong values are passed by the user */
        /* Set initial direction of line SCLK  as OUT */
        pinState |= 0x00000001;/*Note: Direction is out if bit is 1!!! */
        /* Set initial direction of  MOSI line as OUT */
        pinState |= 0x00000002;
        /* Set initial direction of MISO line as IN */
        pinState &= 0xFFFFFFFB;
        /* Set initial direction of CS line as OUT */
        pinState |= ((1 << ((configOptions & SPI_CONFIG_OPTION_CS_MASK) >> 2)) << 3);

        /*Set initial state of clock line*/
        mode = (configOptions & SPI_CONFIG_OPTION_MODE_MASK);
        switch (mode) {
            case 0:
            case 1:
                /* clock idle low */
                pinState &= 0xFFFFFEFF;
                break;

            case 2:
            case 3:
                /* clock idle high */
                pinState |= 0x00000100;
                break;
            default:
                break;
        }

        FT_InitChannel(FT_LegacyProtocol.SPI, ft_device1, clockRate, latencyTimer, configOptions);

        /* Set the directions and values to the lines */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;/*MPSSE command*/
        buffer[noOfBytes++] = (byte) ((pinState & 0xFF00) >> 8);
        buffer[noOfBytes++] = (byte) (pinState & 0x00FF); /*Dir*/
        FT_Channel_Write(ft_device1, noOfBytes, buffer, true);

        this.clockRate = clockRate;
        this.latencyTimer = latencyTimer;
        this.configOptions = configOptions;
    }

    /*!
     * Closes a channel and frees all resources that were used by it
     */
    public void SPI_CloseChannel(FT_Device ft_device) {
        byte dir, val;
        byte buffer[] = new byte[5];
        int noOfBytes = 0;
        int noOfBytesTransferred;

        /* Retrieve final state values for the lines */
        dir = (byte) ((pinState & 0x00FF0000) >> 16);
        val = (byte) ((pinState & 0xFF000000) >> 24);

        /* Set lines to final state */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;/* MPSSE command */
        buffer[noOfBytes++] = val; /*Value*/
        buffer[noOfBytes++] = dir; /*Direction*/
        FT_Channel_Write(ft_device, noOfBytes, buffer, true);

        FT_CloseChannel(ft_device);
    }

    /*!
     * This function reads the specified number of bits or bytes from the SPI device
     *
     */
    public int SPI_Read(FT_Device ft_device, byte buffer[], int sizeToTransfer, int transferOptions) {
        byte tmpByte[] = new byte[1];
        byte bitsToTransfer = 0;
        byte lsb = 0;
        int sizeTransferred = 0;

        if ((transferOptions & SPI_TRANSFER_OPTIONS_LSB_FIRST) > 0) {
            lsb = MPSSE_CMD_DATA_LSB_FIRST;
        }

        if ((transferOptions & SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE) > 0) {
            /* Enable CHIPSELECT line for the channel */
            SPI_ToggleCS(ft_device, true);
        }

        if ((transferOptions & SPI_TRANSFER_OPTIONS_SIZE_IN_BITS) > 0) {/*sizeToTransfer is in bits*/
            while (sizeTransferred < sizeToTransfer) {
                if ((sizeToTransfer - sizeTransferred) >= 8) {
                    bitsToTransfer = 8;
                } else {
                    bitsToTransfer = (byte) (sizeToTransfer - sizeTransferred);
                }
                SPI_Read8bits(ft_device, tmpByte, bitsToTransfer, lsb);
                buffer[(sizeTransferred + 1) / 8] = tmpByte[0];
                sizeTransferred += bitsToTransfer;
            }
        } else {/*sizeToTransfer is in bytes*/
            int noOfBytesTransferred = 0, CurrentXferSize = 0;
            byte cmdBuffer[] = new byte[4];
            ///ChannelConfig * config = NULL;
            byte mode;

            /*mode is given by bit1-bit0 of ChannelConfig.Options*/
            mode = (byte) (configOptions & SPI_CONFIG_OPTION_MODE_MASK);
            /* Command to write 8bits */
            switch (mode) {
                case 0:
                    cmdBuffer[0] = (byte) (MPSSE_CMD_DATA_IN_BYTES_POS_EDGE | lsb);
                    break;
                case 1:
                    cmdBuffer[0] = (byte) (MPSSE_CMD_DATA_IN_BYTES_NEG_EDGE | lsb);
                    break;
                case 2:
                    cmdBuffer[0] = (byte) (MPSSE_CMD_DATA_IN_BYTES_NEG_EDGE | lsb);
                    break;
                case 3:
                    cmdBuffer[0] = (byte) (MPSSE_CMD_DATA_IN_BYTES_POS_EDGE | lsb);
                    break;
                default:
                    break;
            }

            sizeTransferred = 0;
            while (sizeTransferred < sizeToTransfer) {

                CurrentXferSize = ((sizeToTransfer - sizeTransferred) > 64 * 1024) ? 64 * 1024 : (sizeToTransfer - sizeTransferred);
                /* length LSB */
                cmdBuffer[1] = (byte) ((CurrentXferSize - 1) & 0x000000FF);
                /* length MSB */
                cmdBuffer[2] = (byte) (((CurrentXferSize - 1) & 0x0000FF00) >> 8);

                /*Command MPSSE to send data to PC immediately */
                cmdBuffer[3] = (byte) MPSSE_CMD_SEND_IMMEDIATE;
                FT_Channel_Write(ft_device, cmdBuffer.length, cmdBuffer, true);
                noOfBytesTransferred = FT_Channel_Read(ft_device, CurrentXferSize, spiBuffer);
                // copy data read to buffer
                System.arraycopy(spiBuffer, 0, buffer, sizeTransferred, noOfBytesTransferred);
                sizeTransferred += noOfBytesTransferred;
            }
        }

        if ((transferOptions & SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE) > 0) {
            /* Disable CHIPSELECT line for the channel */
            SPI_ToggleCS(ft_device, false);
        }

        return sizeTransferred;
    }

    /*!
     * \brief Writes data to a SPI slave device
     */
    public int SPI_Write(byte[] buffer, int sizeToTransfer, int transferOptions) {
        byte tmpByte;
        byte bitsToTransfer = 0;
        byte lsb = 0;
        int sizeTransferred = 0;

        if ((transferOptions & SPI_TRANSFER_OPTIONS_LSB_FIRST) > 0) {
            lsb = MPSSE_CMD_DATA_LSB_FIRST;
        }

        if ((transferOptions & SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE) > 0) {
            /* enable CHIPSELECT line for the channel */
            SPI_ToggleCS(ft_device1, true);
        }

        if ((transferOptions & SPI_TRANSFER_OPTIONS_SIZE_IN_BITS) > 0) {/* sizeToTransfer is in bits */

            /* loop until all the bits are transferred */
            while (sizeTransferred < sizeToTransfer) {
                if ((sizeToTransfer - sizeTransferred) >= 8) {
                    bitsToTransfer = 8;
                } else {
                    bitsToTransfer = (byte) (sizeToTransfer - sizeTransferred);
                }
                tmpByte = buffer[(sizeTransferred + 1) / 8];
                SPI_Write8bits(ft_device1, tmpByte, bitsToTransfer, lsb);
                sizeTransferred += bitsToTransfer;
            }
        } else {/* sizeToTransfer is in bytes */
            int noOfBytesTransferred = 0, CurrentXferSize = 0;
            byte[] cmdBuffer = new byte[3];
            byte mode;

            /*mode is given by bit1-bit0 of ChannelConfig.Options*/
            mode = (byte) (configOptions & SPI_CONFIG_OPTION_MODE_MASK);
            /* Command to write 8bits */
            switch (mode) {
                case SPI_CONFIG_OPTION_MODE0:
                case SPI_CONFIG_OPTION_MODE3:
                    cmdBuffer[0] = (byte) (MPSSE_CMD_DATA_OUT_BYTES_NEG_EDGE | lsb);
                    break;
                case SPI_CONFIG_OPTION_MODE1:
                case SPI_CONFIG_OPTION_MODE2:
                    cmdBuffer[0] = (byte) (MPSSE_CMD_DATA_OUT_BYTES_POS_EDGE | lsb);
                    break;
                default:
                    break;
            }

            sizeTransferred = 0;

            long start = System.currentTimeMillis();
            while (sizeTransferred < sizeToTransfer) {

                CurrentXferSize = Math.min(MAX_BUF_SIZE_SPI,(sizeToTransfer - sizeTransferred));
                /* length low byte */
                cmdBuffer[1] = (byte) ((CurrentXferSize - 1) & 0x000000FF);
                /* length high byte */
                cmdBuffer[2] = (byte) (((CurrentXferSize - 1) & 0x0000FF00) >> 8);
                /* write command */
                FT_Channel_Write(ft_device1, cmdBuffer.length, cmdBuffer, true);
                // copy data write to buffer
                System.arraycopy(buffer, sizeTransferred, spiBuffer, 0, CurrentXferSize);
                /* write data */
                noOfBytesTransferred = FT_Channel_Write(ft_device1, CurrentXferSize, spiBuffer, true);
                sizeTransferred += noOfBytesTransferred;

            }
        }

        if ((transferOptions & SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE) > 0) {
            /* disable CHIPSELECT line for the channel */
            SPI_ToggleCS(ft_device1, false);
        }
        return sizeTransferred;
    }


    /*!
     * This function transfers data in both directions between a SPI master and a slave. One bit is
     * clocked out and one bit is clocked in during every clock.
     */
    public int SPI_ReadWrite(FT_Device ft_device, byte inBuffer[],
                             byte outBuffer[], int sizeToTransfer, int transferOptions) {
        byte tmpByte[] = new byte[1];
        byte mode;
        byte bitsToTransfer = 0;
        int noOfBytesTransferred = 0;
        byte cmdBuffer[] = new byte[10]; // TODO why do we send 10 bytes ????? Is this a bug???
        int sizeTransferred = 0;

        /*mode is given by bit1-bit0 of ChannelConfig.Options*/
        mode = (byte) (configOptions & SPI_CONFIG_OPTION_MODE_MASK);

        if ((transferOptions & SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE) > 0) {
            /* enable CHIPSELECT line for the channel */
            SPI_ToggleCS(ft_device, true);
        }

        /* start of transfer */
        if ((transferOptions & SPI_TRANSFER_OPTIONS_SIZE_IN_BITS) > 0) {/* sizeToTransfer is in bits */
            /* Command to write 8bits */
            switch (mode) {
                case SPI_CONFIG_OPTION_MODE0:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BITS_IN_POS_OUT_NEG_EDGE;
                    break;
                case SPI_CONFIG_OPTION_MODE1:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BITS_IN_NEG_OUT_POS_EDGE;
                    break;
                case SPI_CONFIG_OPTION_MODE2:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BITS_IN_NEG_OUT_POS_EDGE;
                    break;
                case SPI_CONFIG_OPTION_MODE3:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BITS_IN_POS_OUT_NEG_EDGE;
                    break;
                default:
                    break;
            }
            while (sizeTransferred < sizeToTransfer) {
                if ((sizeToTransfer - sizeTransferred) >= 8) {
                    bitsToTransfer = 8;
                } else {
                    bitsToTransfer = (byte) (sizeToTransfer - sizeTransferred);
                }
                cmdBuffer[1] = (byte) (bitsToTransfer - 1); /*takes value 0 for 1 bit; 7 for 8 bits*/
                cmdBuffer[2] = outBuffer[(sizeTransferred + 1) / 8];

                /*Write command and data*/
                noOfBytesTransferred = FT_Channel_Write(ft_device, 3, cmdBuffer, true);
                if (3 > noOfBytesTransferred) {/*timeout occured if FT_OK is returned but transferred length is requested len*/
                    Log.e("d2xxx", "Timeout occured. RequestedTxLen=3 TxLen=" + noOfBytesTransferred);
                }

                /*Read from buffer*/
                noOfBytesTransferred = FT_Channel_Read(ft_device, 1, tmpByte);
                if (1 > noOfBytesTransferred) {/*timeout occured if FT_OK is returned but transferred length is requested len*/
                    Log.e("d2xxx", "Timeout occured. RequestedTxLen=1 TxLen=" + noOfBytesTransferred);
                }
                // copy the read buffer
                for (int i = 0; i < noOfBytesTransferred; i++) {
                    inBuffer[(sizeTransferred + 1) / 8] = tmpByte[0];
                }

                sizeTransferred += bitsToTransfer;
            }
        } else {
            int CurrentXferSize = 0;
            /* Command to write 8bits */
            switch (mode) {
                case SPI_CONFIG_OPTION_MODE0:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BYTES_IN_POS_OUT_NEG_EDGE;
                    break;
                case SPI_CONFIG_OPTION_MODE1:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BYTES_IN_NEG_OUT_POS_EDGE;
                    break;
                case SPI_CONFIG_OPTION_MODE2:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BYTES_IN_NEG_OUT_POS_EDGE;
                    break;
                case SPI_CONFIG_OPTION_MODE3:
                    cmdBuffer[0] = MPSSE_CMD_DATA_BYTES_IN_POS_OUT_NEG_EDGE;
                    break;
                default:
                    break;
            }

            ///
            sizeTransferred = 0;
            while (sizeTransferred < sizeToTransfer) {

                CurrentXferSize = ((sizeToTransfer - sizeTransferred) > 64 * 1024) ? 64 * 1024 : (sizeToTransfer - sizeTransferred);
                /* length LSB */
                cmdBuffer[1] = (byte) ((CurrentXferSize - 1) & 0x000000FF);
                /* length MSB */
                cmdBuffer[2] = (byte) (((CurrentXferSize - 1) & 0x0000FF00) >> 8);


                /*Write command*/
                FT_Channel_Write(ft_device, cmdBuffer.length, cmdBuffer, true);

                // copy data write to buffer
                System.arraycopy(outBuffer, sizeTransferred, spiBuffer, 0, CurrentXferSize);
                FT_Channel_Write(ft_device, CurrentXferSize, spiBuffer, true);

                noOfBytesTransferred = FT_Channel_Read(ft_device, CurrentXferSize, spiBuffer);
                // copy data read to buffer
                System.arraycopy(spiBuffer, 0, inBuffer, sizeTransferred, noOfBytesTransferred);

                sizeTransferred += noOfBytesTransferred;
            }
        }
        /* end of transfer */

        if ((transferOptions & SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE) > 0) {
            /* disable CHIPSELECT line for the channel */
            SPI_ToggleCS(ft_device, false);
        }
        return sizeTransferred;
    }


    /*!
     * Reads the logic state of the SPI MISO line without clocking the bus
     */
    public boolean SPI_IsBusy(FT_Device ft_device) {
        boolean isBusy;
        int noOfBytes = 0;
        byte buffer[] = new byte[10];

        /*Enable CS*/
        SPI_ToggleCS(ft_device, true);
        /*Send command to read*/
        buffer[noOfBytes++] = (byte) MPSSE_CMD_GET_DATA_BITS_LOWBYTE;
        buffer[noOfBytes++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;
        FT_Channel_Write(ft_device, noOfBytes, buffer, true);

        /*Read*/
        noOfBytes = 1;
        FT_Channel_Read(ft_device, noOfBytes, buffer);
        if (0 == (buffer[0] & 0x04)) {
            isBusy = false;
        } else {
            isBusy = true;
        }

        /*Disable CS*/
        SPI_ToggleCS(ft_device, false);
        return isBusy;
    }

    /*!
     * This function changes the chip select line that is to be used to communicate to the SPI slave
     * configOptions Provides a way to select the chip select line & SPI mode
     *	BIT1-0=CPOL-CPHA:	00 - MODE0 - data captured on rising edge, propagated on falling
     *						01 - MODE1 - data captured on falling edge, propagated on rising
     *						10 - MODE2 - data captured on falling edge, propagated on rising
     *						11 - MODE3 - data captured on rising edge, propagated on falling
     *	BIT4-BIT2: 000 - A/B/C/D_DBUS3=ChipSelect
     *			 : 001 - A/B/C/D_DBUS4=ChipSelect
     * 			 : 010 - A/B/C/D_DBUS5=ChipSelect
     *			 : 011 - A/B/C/D_DBUS6=ChipSelect
     *			 : 100 - A/B/C/D_DBUS7=ChipSelect
     *	BIT5: ChipSelect is active high if this bit is 0
     *	BIT6 -BIT31		: Reserved
     */
    public void SPI_ChangeCS(FT_Device ft_device, int configOptions) {
        byte mode;
        byte buffer[] = new byte[5];
        int noOfBytes = 0;

        /* Replace config options with new values */
        this.configOptions = configOptions;
        /* Ensure new CS lins is set as OUT */
        pinState |= ((1 << ((configOptions & SPI_CONFIG_OPTION_CS_MASK) >> 2)) << 3);

        /*Set initial state of clock line*/
        mode = (byte) (configOptions & SPI_CONFIG_OPTION_MODE_MASK);
        switch (mode) {
            case 0:
            case 1:
                /* clock idle low */
                pinState &= 0xFEFF;
                break;

            case 2:
            case 3:
                /* clock idle high */
                pinState |= 0x0100;
                break;
            default:
                break;
        }

        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;/* MPSSE command */
        buffer[noOfBytes++] = (byte) ((pinState & 0xFF00) >> 8);/*Val*/
        buffer[noOfBytes++] = (byte) (pinState & 0x00FF); /*Dir*/

        FT_Channel_Write(ft_device, noOfBytes, buffer, true);
    }


    /*!
     * This function turns ON/OFF the chip select line associated with the channel
     */
    private void SPI_ToggleCS(FT_Device ft_device, boolean state) {
        byte buffer[] = new byte[5];
        int i = 0;
        int noOfBytesTransferred;
        byte value, oldValue, direction;

        if (!state) {
            SystemClock.sleep(2);
        }

        boolean activeLow = (configOptions & SPI_CONFIG_OPTION_CS_ACTIVELOW) > 0 ? true : false;

        direction = (byte) (pinState & 0x00FF);//20110718
        direction |= ((1 << ((configOptions & SPI_CONFIG_OPTION_CS_MASK) >> 2)) << 3);

        //oldValue = (byte)(8>>config->currentPinState);
        oldValue = (byte) ((pinState & 0xFF00) >> 8);//20110718
        value = (byte) ((1 << ((configOptions & SPI_CONFIG_OPTION_CS_MASK) >> 2)) << 3);


        if ((true == state && false == activeLow) || (false == state && true == activeLow))
            value = (byte) (oldValue | value); /* set the CS line high */
        if ((true == state && true == activeLow) || (false == state && false == activeLow))
            value = (byte) (oldValue & ~value);/* set the CS line low */

        pinState = (pinState & 0xFFFF0000) | ((int) value << 8) | direction;

        /*MPSSE command to set low bytes*/
        buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[i++] = value;        /*value*/
        buffer[i++] = direction;    /*direction*/
        FT_Channel_Write(ft_device, i, buffer, true);
    }

    public void close() {
        if (ft_device1 != null) {
            FT_CloseChannel(ft_device1);
            ft_device1 = null;
            Log.d(TAG, "usbDetached ft_device1: " + ft_device1);
        }
        if (ft_device2 != null) {
            FT_CloseChannel(ft_device2);
            ft_device2 = null;
            Log.d(TAG, "usbDetached ft_device2: " + ft_device2);
        }
    }

    /*!
     * \brief Writes 8 or less bits to the SPI device
     *
     * This function is called by SPI_Write to write 8 or few number of bits
     *
     * \param[in] handle Handle of the channel
     * \param[in] byte Data of length 8 or less bits
     * \param[in] len Length of data in bits(maximum 8)
     * \return Returns status code of type FT_STATUS(see D2XX Programmer's Guide)
     * \sa
     * \note
     * \warning
     */
    private void SPI_Write8bits(FT_Device ft_device, byte data, byte len, byte lsb) {
        int noOfBytes = 0;
        byte buffer[] = new byte[10];
        byte mode;

        /*mode is given by bit1-bit0 of ChannelConfig.Options*/
        mode = (byte) (configOptions & SPI_CONFIG_OPTION_MODE_MASK);
        /* Command to write 8bits */
        switch (mode) {
            case 0:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE | lsb);
                break;
            case 1:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_OUT_BITS_POS_EDGE | lsb);
                break;
            case 2:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_OUT_BITS_POS_EDGE | lsb);
                break;
            case 3:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE | lsb);
                break;
            default:
                break;
        }
        buffer[noOfBytes++] = (byte) (len - 1);/* 1bit->arg=0, for 8bits->arg=7 */
        buffer[noOfBytes++] = data;
        FT_Channel_Write(ft_device, noOfBytes, buffer, true);
    }


    /*!
     * This function is called by SPI_Read to read 8 or few number of bits
     */
    private void SPI_Read8bits(FT_Device ft_device, byte data[], byte len, byte lsb) {
        int noOfBytes = 0;
        byte buffer[] = new byte[10];
        byte mode;

        /*mode is given by bit1-bit0 of ChannelConfig.Options*/
        mode = (byte) (configOptions & SPI_CONFIG_OPTION_MODE_MASK);
        /* Command to write 8bits */
        switch (mode) {
            case 0:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_IN_BITS_POS_EDGE | lsb);
                break;
            case 1:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_IN_BITS_NEG_EDGE | lsb);
                break;
            case 2:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_IN_BITS_NEG_EDGE | lsb);
                break;
            case 3:
                buffer[noOfBytes++] = (byte) (MPSSE_CMD_DATA_IN_BITS_POS_EDGE | lsb);
                break;
            default:
                break;
        }
        buffer[noOfBytes++] = (byte) (len - 1);/* 1bit->arg=0, for 8bits->arg=7 */

        /*Command MPSSE to send data to PC immediately */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;

        FT_Channel_Write(ft_device, noOfBytes, buffer, true);

        noOfBytes = 1;
        FT_Channel_Read(ft_device, noOfBytes, buffer);

        data[0] = buffer[0];
    }


    /*!
     * This function initializes the channel and the communication parameters associated with it
     */
    public void I2C_InitChannel(int clockRate, int latencyTimer, int configOptions) {
        byte buffer[] = new byte[3];//3
        int noOfBytesToTransfer;

        ft_device2 = FT_OpenChannel(1);
        Log.d(TAG, "ft_device2: " + ft_device2);

        if ((configOptions & I2C_DISABLE_3PHASE_CLOCKING) != 0) {/* Adjust clock rate if 3phase clocking should be enabled */
            clockRate = (clockRate * 3) / 2;
        }
        FT_InitChannel(FT_LegacyProtocol.I2C, ft_device2, clockRate, latencyTimer, configOptions);

        if ((configOptions & I2C_DISABLE_3PHASE_CLOCKING) == 0) {
            noOfBytesToTransfer = 1;
            buffer[0] = (byte) MPSSE_CMD_ENABLE_3PHASE_CLOCKING;/* MPSSE command */
            FT_Channel_Write(ft_device2, noOfBytesToTransfer, buffer, true);
        }
    }

    /*!
     * Closes a channel and frees all resources that were used by it
     */
    public void I2C_CloseChannel(FT_Device ft_device) {
        FT_CloseChannel(ft_device);
    }

    /*!
     * This function reads the specified number of bytes from an addressed I2C slave
     */
    public synchronized int I2C_DeviceRead(int deviceAddress,
                                           int sizeToTransfer, byte buffer[], int options) {
        boolean ack = false;
        int i;
        Mid_PurgeDevice(ft_device2);
        int sizeTransferred = 0;

        if ((options & I2C_TRANSFER_OPTIONS_FAST_TRANSFER) > 0) {
            sizeTransferred = I2C_FastRead(ft_device2, deviceAddress, sizeToTransfer, buffer, options);
        } else {
            /* Write START bit */
            if ((options & I2C_TRANSFER_OPTIONS_START_BIT) > 0) {
                I2C_Start(ft_device2);
            }

            /* Write device address (with LSB=1 => READ)  & Get ACK */
            ack = I2C_WriteDeviceAddress(ft_device2, deviceAddress, true, false);

            if (!ack) /*ack bit set actually means device nAcked*/ {
                /* LOOP until sizeToTransfer */
                byte tmpByte[] = new byte[1];
                for (i = 0; ((i < sizeToTransfer)); i++) {
				/* Read byte to buffer & Give ACK
				(or nACK if it is last byte and I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE is set)*/
                    I2C_Read8bitsAndGiveAck(ft_device2, tmpByte, (i < (sizeToTransfer - 1)) ? true :
                            ((options & I2C_TRANSFER_OPTIONS_NACK_LAST_BYTE) > 0 ? false : true));
                    buffer[i] = tmpByte[0];
                }

                sizeTransferred = i;
                if (sizeTransferred != sizeToTransfer) {
                    Log.e(TAG, "Error: sizeToTransfer = " + sizeToTransfer + ", sizeTransferred = " + sizeTransferred);
                } else {
                    /* Write STOP bit */
                    if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
                        I2C_Stop(ft_device2);
                    }
                }
            } else {
                /* Write STOP bit */
                if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
                    I2C_Stop(ft_device2);
                }
            }
        }
        return sizeTransferred;
    }

    /*!
     * This function writes the specified number of bytes from an addressed I2C slave
     */
    public synchronized int I2C_DeviceWrite(int deviceAddress,
                                            int sizeToTransfer, byte buffer[], int options) {
        boolean ack = false;
        int i;
        int sizeTransferred = 0;

        Mid_PurgeDevice(ft_device2);

        if ((options & I2C_TRANSFER_OPTIONS_FAST_TRANSFER) > 0) {
            sizeTransferred = I2C_FastWrite(ft_device2, deviceAddress, sizeToTransfer, buffer, options);
        } else {
            /* Write START bit */
            if ((options & I2C_TRANSFER_OPTIONS_START_BIT) > 0) {
                I2C_Start(ft_device2);
            }

            /* Write device address (with LSB=0 => WRITE) & Get ACK*/
            ack = I2C_WriteDeviceAddress(ft_device2, deviceAddress, false, false);

            if (!ack) /*ack bit set actually means device nAcked*/ {
                /* LOOP until sizeToTransfer */
                for (i = 0; ((i < sizeToTransfer)); i++) {
                    /* Write byte to buffer & Get ACK */
                    ack = I2C_Write8bitsAndGetAck(ft_device2, buffer[i]);
                    if (ack) {
					/* add bit in options to return with error if device nAcked
					sizeTransferred = number of correctly transfered bytes */
                        if ((options & I2C_TRANSFER_OPTIONS_BREAK_ON_NACK) > 0) {
						/*status = FT_FAILED_TO_WRITE_DEVICE;
						break;*/

                            /* Write STOP bit */
                            if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
                                I2C_Stop(ft_device2);
                            }
                        }
                    }
                }
                sizeTransferred = i;
                if (sizeTransferred != sizeToTransfer) {
                    Log.e(TAG, "Error: sizeToTransfer = " + sizeToTransfer + ", sizeTransferred = " + sizeTransferred);
                } else {
                    /* Write STOP bit */
                    if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
                        I2C_Stop(ft_device2);
                    }
                }
            } else {
                /* Write STOP bit */
                if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
                    I2C_Stop(ft_device2);
                }
            }
        }
        return sizeTransferred;
    }


    /*!
     * This function writes 8 bits of data to the I2C bus and gets the ack bit from the device
     */
    private boolean I2C_Write8bitsAndGetAck(FT_Device ft_device, byte data) {
        boolean ack = false;
        byte buffer[] = new byte[20];
        byte inBuffer[] = new byte[3];
        int noOfBytes = 0;
        int noOfBytesTransferred = 0;

        /* Set direction */

        pinStateB |= Pins.BD1.dir_mask; //SDA OUT
        pinStateB |= Pins.BD0.dir_mask; //SCL OUT
        /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
        else{ */
        pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
        pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

        byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
        byte dir = (byte) (pinStateB & 0x000000FF);

        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[noOfBytes++] = (byte) val; //VALUE_SCLLOW_SDALOW;
        buffer[noOfBytes++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

        /* Command to write 8 bits */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
        buffer[noOfBytes++] = (byte) DATA_SIZE_8BITS;
        buffer[noOfBytes++] = data;

        /* Set SDA to input mode before reading ACK bit */
        pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
        pinStateB |= Pins.BD0.dir_mask; //SCL OUT
        /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
        else{ */
        pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
        pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

        val = (byte) ((pinStateB & 0x0000FF00) >> 8);
        dir = (byte) (pinStateB & 0x000000FF);

        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[noOfBytes++] = (byte) val; //VALUE_SCLLOW_SDALOW;
        buffer[noOfBytes++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

        /* Command to get ACK bit */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_DATA_IN_BITS_POS_EDGE;
        buffer[noOfBytes++] = (byte) DATA_SIZE_1BIT;

        /* Command MPSSE to send data to PC immediately */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;

        long start = System.currentTimeMillis();
        noOfBytesTransferred = FT_Channel_Write(ft_device, noOfBytes, buffer, true);

        if (noOfBytes != noOfBytesTransferred) {
            Log.e(TAG, "I2C_Write8bitsAndGetAck FT_Channel_Write: noOfBytes != noOfBytesTransferred");
        } else {
            noOfBytes = 1;
            noOfBytesTransferred = 0;
            SystemClock.sleep(1);
            noOfBytesTransferred = FT_Channel_Read(ft_device, noOfBytes, inBuffer);
            if (noOfBytes != noOfBytesTransferred) {
                Log.e(TAG, "I2C_Write8bitsAndGetAck FT_Channel_Read: noOfBytes != noOfBytesTransferred");
            } else {
                ack = (boolean) ((inBuffer[0] & 0x01) > 0);
            }
        }

        return ack;
    }

    /*!
     * This function reads 8 bits of data from the I2C bus and then writes an ack bit to the bus
     */
    private void I2C_Read8bitsAndGiveAck(FT_Device ft_device, byte data[], boolean ack) {
        byte buffer[] = new byte[20];
        byte inBuffer[] = new byte[3];
        int noOfBytes = 0;
        int noOfBytesTransferred = 0;

        /* Set pin directions - SCL is output driven low, SDA is input (set high but does not matter) */
        pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
        pinStateB |= Pins.BD0.dir_mask; //SCL OUT
        /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
        else{ */
        pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
        pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

        byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
        byte dir = (byte) (pinStateB & 0x000000FF);

        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[noOfBytes++] = (byte) val; //VALUE_SCLLOW_SDALOW;
        buffer[noOfBytes++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

        /* Command to read 8 bits */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_DATA_IN_BITS_POS_EDGE;
        buffer[noOfBytes++] = (byte) DATA_SIZE_8BITS;

        /* Set directions to make SDA drive out. Pre-set state of SDA first though to avoid glitch */
        if (ack) {
            /* We will drive the ACK bit to a '0' so pre-set pin to a '0' */
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            dir = (byte) (pinStateB & 0x000000FF);

            buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[noOfBytes++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            buffer[noOfBytes++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

            /* Clock out the ack bit as a '0' on negative edge */
            buffer[noOfBytes++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
            buffer[noOfBytes++] = (byte) DATA_SIZE_1BIT;
            buffer[noOfBytes++] = (byte) SEND_ACK;
        } else {
            /* We will release the ACK bit to a '1' so pre-set pin to a '1' by making it an input */
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            dir = (byte) (pinStateB & 0x000000FF);

            buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[noOfBytes++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            buffer[noOfBytes++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

            /* Clock out the ack bit as a '1' on negative edge - never actually seen on line since SDA is input but burns off one bit time */
            buffer[noOfBytes++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
            buffer[noOfBytes++] = (byte) DATA_SIZE_1BIT;
            buffer[noOfBytes++] = (byte) SEND_NACK;
        }

        /* Back to Idle */
        pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
        pinStateB |= Pins.BD0.dir_mask; //SCL OUT
        /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
        else{ */
        pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
        pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

        val = (byte) ((pinStateB & 0x0000FF00) >> 8);
        dir = (byte) (pinStateB & 0x000000FF);

        buffer[noOfBytes++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[noOfBytes++] = (byte) val; //VALUE_SCLLOW_SDALOW;
        buffer[noOfBytes++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

        /* Command MPSSE to send data to PC immediately */
        buffer[noOfBytes++] = (byte) MPSSE_CMD_SEND_IMMEDIATE;

        long start = System.currentTimeMillis();
        noOfBytesTransferred = FT_Channel_Write(ft_device, noOfBytes, buffer, true);

        if (noOfBytes != noOfBytesTransferred) {
            Log.e(TAG, "I2C_Read8bitsAndGiveAck FT_Channel_Write: noOfBytes != noOfBytesTransferred");
        } else {
            noOfBytes = 1;
            noOfBytesTransferred = 0;

            SystemClock.sleep(1);
            noOfBytesTransferred = FT_Channel_Read(ft_device, noOfBytes, inBuffer);
            if (noOfBytes != noOfBytesTransferred) {
                Log.e(TAG, "I2C_Read8bitsAndGiveAck FT_Channel_Read: noOfBytes != noOfBytesTransferred");
            } else {
                data[0] = inBuffer[0];
            }
        }

    }


    /*!
     * This function allocates memory locally, makes MPSSE command frames to write each data
     * byte/bit, makes MPSSE command frames to read the acknowledgement bits, and then writes
     * all these to the MPSSE in one shot. This function is useful where delays between START, DATA
     * and STOP phases are not prefered.
     */
    private int I2C_FastWrite(FT_Device ft_device, int deviceAddress,
                             int sizeToTransfer, byte buffer[], int options) {
        int sizeTransferred = 0;
        int i = 0; /* index of cmdBuffer that is filled */
        int j = 0; /* scratch register */
        int sizeTotal;
        int bytesRead;
        int bytesToTransfer;
        byte tempAddress;


        if ((options & I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES) == 0) {
            // Fast read supports I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES only, not I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BITS
            Log.e(TAG, "Fast read supports I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES only, not I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BITS");
            return 0;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Allocate COMMAND buffer #############

        bytesToTransfer = sizeToTransfer;

        /* Calculate size of required buffer */
        sizeTotal = 0 /* for send immediate */
                + (bytesToTransfer * (6 + 5)) /* the size of data itself */
                + (((options & I2C_TRANSFER_OPTIONS_NO_ADDRESS) == 0) ? (11) : (0)) /* for address byte */
                + (((options & I2C_TRANSFER_OPTIONS_START_BIT) > 0) ? ((START_DURATION_1 +
                START_DURATION_2 + 1) * 3) : 0) /* size required for START */
                + (((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) ? ((STOP_DURATION_1 +
                STOP_DURATION_2 + STOP_DURATION_3 + 1) * 3) : 0); /* size for STOP */

        /* Allocate buffers */
        byte outBuffer[] = new byte[sizeTotal];


        //--------------------------------------------------------------------------------------------
        // ############## Write START command #############
        if ((options & I2C_TRANSFER_OPTIONS_START_BIT) > 0) {
            /* SCL high, SDA high */
            for (j = 0; j < START_DURATION_1; j++) {
                pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;
            }
            /* SCL high, SDA low */
            for (j = 0; j < START_DURATION_2; j++) {
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
            }
            /*SCL low, SDA low */
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write ADDRESS #############
        if ((options & I2C_TRANSFER_OPTIONS_NO_ADDRESS) == 0) {
            tempAddress = (byte) deviceAddress;
            tempAddress = (byte) (tempAddress << 1);
            tempAddress = (byte) (tempAddress & I2C_ADDRESS_WRITE_MASK);

            /*set direction*/
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

            /* write address + direction bit */
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_8BITS;
            outBuffer[i++] = (byte) tempAddress;

            /* Set SDA to input mode before reading ACK bit */
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

            /* Command to get ACK bit */
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_IN_BITS_POS_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_1BIT;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write ACTUAL DATA #############
        for (j = 0; j < bytesToTransfer; j++) {
            /*set direction*/
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

            /* Command to write 8 bits */
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_8BITS;
            outBuffer[i++] = buffer[j];

            /* Set SDA to input mode before reading ACK bit */
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            dir = (byte) (pinStateB & 0x000000FF);


            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

            /* Command to get ACK bit */
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_IN_BITS_POS_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_1BIT;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write STOP command #############
        if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
            /* SCL low, SDA low */
            for (j = 0; j < STOP_DURATION_1; j++) {
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
            }
            /* SCL high, SDA low */
            for (j = 0; j < STOP_DURATION_2; j++) {
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
            }
            /* SCL high, SDA high */
            for (j = 0; j < STOP_DURATION_3; j++) {
                pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;
            }

            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB &= (~(Pins.BD0.dir_mask & 0x7FFFFFFF)); //SCL IN
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
            pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLIN_SDAIN; /* Tristate the SCL & SDA pins */
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write commands #############
        bytesRead = FT_Channel_Write(ft_device, i, outBuffer, true);
        sizeTransferred = sizeToTransfer;


        //--------------------------------------------------------------------------------------------
        // ############## Read ACKS #############
        /* Read ack of address */
        if ((options & I2C_TRANSFER_OPTIONS_NO_ADDRESS) == 0) {
            byte addAck[] = new byte[1];
            bytesRead = FT_Channel_Read(ft_device, 1, addAck);
        }

        /* Read 1bit ack after each 8bits written */
        byte inBuffer[] = new byte[bytesToTransfer];
        bytesRead = FT_Channel_Read(ft_device, bytesToTransfer, inBuffer);
        // TODO???
        //if (ack) {/* Copy the ack bits into the ack buffer if provided */
        //    INFRA_MEMCPY(ack, inBuffer, bytesRead);
        //}

        return sizeTransferred;
    }


    /*!
     * This function allocates memory locally, makes MPSSE command frames to read each data
     * byte/bit, makes MPSSE command frames to write the acknowledgement bits, and then writes
     * all these to the MPSSE in one shot. This function is useful where delays between START, DATA
     * and STOP phases are not prefered.
     */
    private int I2C_FastRead(FT_Device ft_device, int deviceAddress,
                            int sizeToTransfer, byte buffer[], int options) {
        int sizeTransferred = 0;
        int i = 0; /* index of cmdBuffer that is filled */
        int j = 0; /* scratch register */
        int sizeTotal;
        int bytesRead;
        int bytesToTransfer;
        byte tempAddress;

        if ((options & I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES) == 0) {
            // Fast read supports I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES only, not I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BITS
            Log.e(TAG, "Fast read supports I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BYTES only, not I2C_TRANSFER_OPTIONS_FAST_TRANSFER_BITS");
            return 0;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Allocate COMMAND buffer #############

        bytesToTransfer = sizeToTransfer;

        /* Calculate size of required buffer */
        sizeTotal = 0 /* for send immediate */
                + (bytesToTransfer * (8 + 6)) /* the size of data itself */
                + (((options & I2C_TRANSFER_OPTIONS_NO_ADDRESS) == 0) ? (11) : (0)) /* for address byte */
                + (((options & I2C_TRANSFER_OPTIONS_START_BIT) > 0) ? ((START_DURATION_1 +
                START_DURATION_2 + 1) * 3) : 0) /* size required for START */
                + (((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) ? ((STOP_DURATION_1 +
                STOP_DURATION_2 + STOP_DURATION_3 + 1) * 3) : 0); /* size for STOP */

        /* Allocate buffers */
        byte outBuffer[] = new byte[sizeTotal];

        //--------------------------------------------------------------------------------------------
        // ############## Write START command #############
        if ((options & I2C_TRANSFER_OPTIONS_START_BIT) > 0) {
            /* SCL high, SDA high */
            for (j = 0; j < START_DURATION_1; j++) {
                pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;
            }
            /* SCL high, SDA low */
            for (j = 0; j < START_DURATION_2; j++) {
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
            }
            /*SCL low, SDA low */
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write ADDRESS #############
        if ((options & I2C_TRANSFER_OPTIONS_NO_ADDRESS) == 0) {
            tempAddress = (byte) deviceAddress;
            tempAddress = (byte) (tempAddress << 1);
            tempAddress = (byte) (tempAddress | I2C_ADDRESS_READ_MASK);

            /*set direction*/
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

            /* write address + direction bit */
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_8BITS;
            outBuffer[i++] = (byte) tempAddress;

            /* Set SDA to input mode before reading ACK bit */
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

            /* Command to get ACK bit */
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_IN_BITS_POS_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_1BIT;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Read ACTUAL DATA #############
        for (j = 0; j < bytesToTransfer; j++) {
            /*set direction*/
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

            /*Command to read 8 bits*/
            outBuffer[i++] = (byte) MPSSE_CMD_DATA_IN_BITS_POS_EDGE;
            outBuffer[i++] = (byte) DATA_SIZE_8BITS;

            /*Command MPSSE to send data to PC immediately */
            if (j < bytesToTransfer - 1)// then ack
            {
                // We will drive the ACK bit to a '0' so pre-set pin to a '0'
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

                val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

                // Clock out the ack bit as a '0' on negative edge
                outBuffer[i++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
                outBuffer[i++] = (byte) DATA_SIZE_1BIT;
                outBuffer[i++] = (byte) SEND_ACK;
            } else// otherwise nack last byte
            {
                // We will release the ACK bit to a '1' so pre-set pin to a '1' by making it an input
                pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

                val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;

                // Clock out the ack bit as a '1' on negative edge - never actually seen on line since SDA is input but burns off one bit time
                outBuffer[i++] = (byte) MPSSE_CMD_DATA_OUT_BITS_NEG_EDGE;
                outBuffer[i++] = (byte) DATA_SIZE_1BIT;
                outBuffer[i++] = (byte) SEND_NACK;
            }

            /*Back to Idle*/
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write STOP command #############
        if ((options & I2C_TRANSFER_OPTIONS_STOP_BIT) > 0) {
            /* SCL low, SDA low */
            for (j = 0; j < STOP_DURATION_1; j++) {
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
            }
            /* SCL high, SDA low */
            for (j = 0; j < STOP_DURATION_2; j++) {
                pinStateB |= Pins.BD1.dir_mask; //SDA OUT
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDALOW;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
            }
            /* SCL high, SDA high */
            for (j = 0; j < STOP_DURATION_3; j++) {
                pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
                pinStateB |= Pins.BD0.dir_mask; //SCL OUT
                /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
                pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
                pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

                byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
                byte dir = (byte) (pinStateB & 0x000000FF);

                outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
                outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
                outBuffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN;
            }
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB &= (~(Pins.BD0.dir_mask & 0x7FFFFFFF)); //SCL IN
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
            pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
            pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            outBuffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            outBuffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
            outBuffer[i++] = (byte) dir; //DIRECTION_SCLIN_SDAIN; /* Tristate the SCL & SDA pins */
        }


        //--------------------------------------------------------------------------------------------
        // ############## Write commands #############
        bytesRead = FT_Channel_Write(ft_device, i, outBuffer, true);
        sizeTransferred = sizeToTransfer;

        //--------------------------------------------------------------------------------------------
        // ############## Read ACKS #############
        /* Read ack of address */
        if ((options & I2C_TRANSFER_OPTIONS_NO_ADDRESS) == 0) {
            byte addAck[] = new byte[1];
            bytesRead = FT_Channel_Read(ft_device, 1, addAck);
        }

        //--------------------------------------------------------------------------------------------
        // ############## Read ACTUAL DATA #############
        /* Read the actual data from the MPSSE-chip into the host system */
        bytesRead = FT_Channel_Read(ft_device, bytesToTransfer, buffer);

        return sizeTransferred;
    }


    /*!
     * This function writes the direction and address bits to the I2C bus, and then gets the ACK
     *
     */
    private boolean I2C_WriteDeviceAddress(FT_Device ft_device, int deviceAddress,
                                   boolean direction, boolean AddLen10Bit) {
        boolean ack = false;
        byte tempAddress;
        if (!AddLen10Bit) {/* 7bit addressing */
            tempAddress = (byte) deviceAddress;
            tempAddress = (byte) (tempAddress << 1);
            if (direction)
                tempAddress = (byte) (tempAddress | I2C_ADDRESS_READ_MASK);
            else
                tempAddress = (byte) (tempAddress & I2C_ADDRESS_WRITE_MASK);

            ack = I2C_Write8bitsAndGetAck(ft_device, tempAddress);
            if (ack) {
                Log.e(TAG, "Didn't receive an ACK from the addressed device, " + deviceAddress);
            }
        } else {/* 10bit addressing */
            /* TODO: Add support for 10bit addressing */
            Log.e(TAG, "10 bit addressing yet to be supported");
        }
        return ack;
    }


    /*!
     * This function generates the I2C start condition on the bus.
     */
    private void I2C_Start(FT_Device ft_device) {
        FT_STATUS status;
        byte buffer[] = new byte[(START_DURATION_1 + START_DURATION_2 + 1) * 3];
        int i = 0, j = 0;
        int noOfBytesTransferred;

        /* SCL high, SDA high */
        for (j = 0; j < START_DURATION_1; j++) {
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
            pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
            buffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN; // Make this input instead to let line be pulled up
        }
        /* SCL high, SDA low */
        for (j = 0; j < START_DURATION_2; j++) {
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[i++] = (byte) val; //VALUE_SCLHIGH_SDALOW;
            buffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
        }
        /*SCL low, SDA low */
        pinStateB |= Pins.BD1.dir_mask; //SDA OUT
        pinStateB |= Pins.BD0.dir_mask; //SCL OUT
        /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
        pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
        pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

        byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
        byte dir = (byte) (pinStateB & 0x000000FF);

        buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
        buffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;

        noOfBytesTransferred = FT_Channel_Write(ft_device, i, buffer, true);

    }

    /*!
     * This function generates the I2C stop condition on the bus.
     */
    private void I2C_Stop(FT_Device ft_device) {
        byte buffer[] = new byte[(STOP_DURATION_1 + STOP_DURATION_2 + STOP_DURATION_3 + 1) * 3];
        int i = 0, j = 0;
        int noOfBytesTransferred;

        /* SCL low, SDA low */
        for (j = 0; j < STOP_DURATION_1; j++) {
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB &= (~(Pins.BD0.val_mask & 0x7FFFFFFF)); //SCL LOW

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[i++] = (byte) val; //VALUE_SCLLOW_SDALOW;
            buffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
        }
        /* SCL high, SDA low */
        for (j = 0; j < STOP_DURATION_2; j++) {
            pinStateB |= Pins.BD1.dir_mask; //SDA OUT
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB &= (~(Pins.BD1.val_mask & 0x7FFFFFFF)); //SDA LOW        //}
            pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[i++] = (byte) val; //VALUE_SCLHIGH_SDALOW;
            buffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAOUT;
        }
        /* SCL high, SDA high */
        for (j = 0; j < STOP_DURATION_3; j++) {
            pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
            pinStateB |= Pins.BD0.dir_mask; //SCL OUT
            /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
                else{ */
            pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
            pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

            byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
            byte dir = (byte) (pinStateB & 0x000000FF);

            buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
            buffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
            buffer[i++] = (byte) dir; //DIRECTION_SCLOUT_SDAIN; // Make this input instead to let line be pulled up
        }

        pinStateB &= (~(Pins.BD1.dir_mask & 0x7FFFFFFF)); //SDA IN
        pinStateB &= (~(Pins.BD0.dir_mask & 0x7FFFFFFF)); //SCL IN
        /*    if(value == GPIO_Value.HIGH){ pinStateB |= (pin.val_mask & 0x7FFFFFFF); /* will be | with 0's and a 1, or & with F's and a 0 */  /* }
            else{ */
        pinStateB |= (Pins.BD1.val_mask & 0x7FFFFFFF); //SDA HIGH        //}
        pinStateB |= (Pins.BD0.val_mask & 0x7FFFFFFF); //SCL HIGH

        byte val = (byte) ((pinStateB & 0x0000FF00) >> 8);
        byte dir = (byte) (pinStateB & 0x000000FF);

        buffer[i++] = (byte) MPSSE_CMD_SET_DATA_BITS_LOWBYTE;
        buffer[i++] = (byte) val; //VALUE_SCLHIGH_SDAHIGH;
        buffer[i++] = (byte) dir; //DIRECTION_SCLIN_SDAIN; /* Tristate the SCL & SDA pins */

        noOfBytesTransferred = FT_Channel_Write(ft_device, i, buffer, true);
    }

}
