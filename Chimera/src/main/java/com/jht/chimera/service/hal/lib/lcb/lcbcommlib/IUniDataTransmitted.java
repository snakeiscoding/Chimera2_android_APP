package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;

public interface IUniDataTransmitted {

    void addReceiveListener(IUniDataReceived listener);
	
    void removeReceiveListener(IUniDataReceived listener);
	
    // Sub Command 0x03
    // Mcb 0x03: (Motor type) (Read)
    // 0x00 , 0xFF , 0xAA , 0x01 , 0x03 , CRC
    void funUniGetMotoType();

   /*******
	* Package format :
	* UCB->MCB : 0x00 + 0xFF + 0xAA + Length + Sub-CMD + W + Data byte + CRC
	* MCB->UCB : 0x01 + Status + 0xAA + 0x01 + Sub-CMD + CRC
	* UCB->MCB : 0x00 + 0xFF + 0xAA + Length + Sub-CMD + R + CRC
	* MCB->UCB : 0x01 + Status + 0xAA + Length + Sub-CMD + Data byte + CRC
	* Length = Sub-CMD + R/W + Data byte
	* Read : 0x00 / Write : 0x01
	*******/
    // Sub Command 0x23
    // Set MCB PFC mode ON/OFF (0x00 enable , 0x01 disable)
    void funUniSetPFCmode(byte byteState);

    // Sub Command 0x24
    // Lcb 0xC6 Magnet Angle State(0x01(write) 0X00 -> Start to tuning magnet ,0X00 -> Get magnet angle)
	// If MCB is not complete, MCB will always send 0xFF  0xFF.
	// If MCB is complete, MCB will show magnet angle value to console. (ex  Angle = 155.3)
	// One decimal place (Data range : 0~3600)
	// Tuning time is 40 second.
    void funUniSetMagnetAngleState(byte byteState);
    void funUniGetMagnetAngle(LcbComm.QueueMode mode);
	
    // Sub Command 0x25
    // Lcb 0xF1 MCB Voltage 0x00(read) 0X01(write)
    void funUniSetMcbVoltage(byte byteData);
    void funUniGetMcbVoltage(LcbComm.QueueMode mode);
	
}
