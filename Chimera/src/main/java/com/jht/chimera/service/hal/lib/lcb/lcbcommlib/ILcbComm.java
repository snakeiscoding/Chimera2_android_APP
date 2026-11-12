package com.jht.chimera.service.hal.lib.lcb.lcbcommlib;


import com.jht.serialport.ISerialPort;
import com.jht.serialport.SerialPortConfiguration;

public interface ILcbComm {

    //Registered Receive callback
	void addReceiveListener(ILcbCommReceive listener);

    //Unregistered Receive callback
    void removeReceiveListener(ILcbCommReceive listener);

    //serial port start
    boolean start(ISerialPort device);


    //serial port start
    boolean restart();

    //serial port stop
    void stop();
    //check CommunicationThread _isRunning
    boolean checkCommunicationThread();
    //check CommunicationThread _bCommThreadExceptFlag
    boolean checkCommThreadExcept();
                
    //erp Stop Uart Port
    void erpStopUartPort();

    //boot up leave LCB Erp and callback status
    void checkBootUpLeaveLcbErp();
    //enter LCB Erp Mode and callback into status
    void enterLcbErpMode();
    //leave LCB Erp Mode and callback status
    void leaveLcbErpMode(String lcb_device_path, SerialPortConfiguration.BaudRate lcb_baud_rate);
    // get the size of the transmit queue
    int getTransmitQueueSize();
    // Command ID 0x70
    //Initialize the LCB
    void setLcbInitial();
    void setLcbInitialHighestPriority();
    // Command ID 0x71
    //Get The LCB specific status
    void getLcbStatus(LcbComm.QueueMode mode);
    void getLcbStatusLowPriority(LcbComm.QueueMode mode);
    // Command ID 0x72
    //Get The LCB most recent error code
    void getLcbErrorCode();
    void getLcbErrorCodeHighPriority();	
    // Command ID 0x73
    //Get the LCB firmware revision
    void getLcbVersion();
    void getLcbVersionHighestPriority();
    // Command ID 0x74
    //Force the LCB to perform a calibration
    void setLcbCalibrate();
    // Command ID 0x75
    //Update program 2 bytes. For LCBA : "JS" => LCB MCU-B ,"JT" => LCB MCU-B Loader ,"JA" => LCB MCU-A ,"JB" => LCB MCU-A Loader
    void setLcbUpdateProgram(byte[] bytesUpdateProgram);
    // Command ID 0x76
    //UCB received the error code already the LCB to allow to skip current error.
    void setLcbSkipCurrError();
    void setLcbSkipCurrErrorHighPriority();
    // Command ID 0x77
    //Special extend command
    void setLcbSpeExtCmd(byte byteCmd);
    // Command ID 0x78
    //Get DCI Version( DCI LCB only)
    void getLcbDciVersion();
    // Command ID 0x79
    //Get DCI Environment
    void getLcbDciEnv();
    // Command ID 0x7A
    //Set CmdEUPsMode command
    void setLcbCmdEUPsMode(byte byteCmd, boolean doPulse);
    // Command ID 0x7B
    //Set the console power to LCB
    void setLcbCmdConsolePower(int watts);
    // Command ID 0x7C
    //Get digital communication protocol version 0x0064 => v1.00
    void getLcbProtocolVersion();
    // Command ID 0x7D
    //Set serial number save to MCB/LCB.
    void setLcbCmdSerialNumber(byte[] bytesCmdData);
    // Command ID 0x7E
    //Treadmill Operate.
//    void setLcbTreadmillOperate(byte[] bytesCmdData);
    void setLcbTreadmillOperate(int treadmillRpm, int inclineLoc);
    // Command ID 0x7F
    // Get Cadence (SPM). Steps per minute from MCB motor current analysis.
    void getLcbTreadmillCadence(LcbComm.QueueMode mode);
    // Command ID 0xF0
    //Motor speed setting and response RPM. LCB Motor RPM setting and require LCB to sent the motor RPM data to UCB
    void setLcbMotorSpeedRtnRPM(int intSpeed);
    // Command ID 0xF1
    //Set motor RPM. Set the motor RPM (Please see Appendix I )Bit 15 : 0 (Motor control signal in normal) Bit 15 : 1 (Motor control signal in reverse)
    void setLcbMotorRPM(int intRPM);
    void setLcbMotorRPMhighestPriority(int intRPM);
    // Command ID 0xF2
    //SPWM add step. Increment the PWM step by the specified value 50~500 (0.01M)
    void setLcbIncPWMStep(int intPWM);
    // Command ID 0xF3
    //PWM decrement step. Decrement the PWM step by the specified value 50~500 (0.01M)
    void setLcbDecPWMStep(int intPWM);
    // Command ID 0xF4
    //PWM stop step. Set PWM step decrement value in Ramp down condition 50~500 (0.01M)
    void setLcbStopPWMStep(int intPWM);
    // Command ID 0xF5
    //Set incline action.
    //0x00: to stop ; 0x01: to rise ; 0xFF: to drop
    void setLcbInclineAction(byte byteAction);
    void setLcbInclineActionHighestPriority(byte byteAction);
    // Command ID 0xF6
    //Set incline location. Set the incline location based on ADC passed as parameter.
    //Bit 15 : 0 (Incline motor control signal in normal) Bit 15 : 1 (Incline motor control signal in reverse) 0~32767
    void setLcbInclineLoc(int intLoc);
    void setLcbInclineLochighestPriority(int intRPM);
    // Command ID 0xF7
    //Set operating status
    //0x00: to stop ; 0x01: to operating
    void setLcbOperatingStatus(byte byteStatus);
    void setLcbOperatingStatusHighestPriority(byte byteStatus);
    // Command ID 0xF8
    //Get the roller RPM
    void getLcbRollerRPM();
    // Command ID 0xF9
    //Get motor RPM
   //Bit 15 : 0 (Motor control signal in normal) Bit 15 : 1 (Motor control signal in reverse)
    void getLcbMotorRPM(LcbComm.QueueMode mode);
    void getOneShotLcbMotorRpmHighPriority();
    // Command ID 0xFA
    //Get incline location
   //Get the incline location ADC value 0~32767
    void getLcbInclineLoc(LcbComm.QueueMode mode);
    // Command ID 0xFB
    //Tune endpoint incline.
    //(1st) data byte:Incline Up software limit setting 0~25.5% of full stroke 0.1%
    //(2nd) data byte :Incline down software limit setting 0~25.5% of full stroke 0.1%
    void setLcbTuneEndIncline(int intPercentage);
    // Command ID 0xFC
    //Tune endpoint incline 2.
    //(1st) data byte:Incline Up software limit setting 0~25.5% of full stroke 0.1%
    //(2nd) data byte :Incline down software limit setting 0~25.5% of full stroke 0.1%
    void setLcbTuneEndIncline2(int intPercentage);
    // Command ID 0xFD
    //Set incline stroke. Incline stroke setting 0~32768 %
    void setLcbInclineStroke(int intPercentage);
    // Command ID 0xFE
    //Set compensation voltage. To set a compensation voltage that use for Electromagnetic temperature compensation 0~65535 0.01V
    void setLcbCompensationVol(int intVol);
    // Command ID 0x90
    //Motor specification set. 1100:1.1k W / 1500:1.5k W / 1800:1.8k W watts
    void setLcbMotorSpecification(int intWatts);
    // Command ID 0x91
    //Get motor type 0x00 : with encoder 0xFF : without encoder
    void getLcbMotorType();
    // Command ID 0x92
    //Get DCI max. data length. Get the equipment max data length
    void getLcbDCIdataMaxLen();
    // Command ID 0x93
    //Set motor power off
    //not under AUTO_CALIBRATE, it is supposed to be used for incline calibration only, the effective time of
   //the command is 1000ms +/- 20ms; for normal incline operation use SET_INCLINE_ACTION
    void setLcbMotorPowerOff(byte byteTime);
    // Command ID 0x94
    //Set DCI FORCE INCLINE OPERATION
    //not under AUTO_CALIBRATE, it is supposed to be used for incline calibration only, the effective time of
   //the command is 1000ms +/- 20ms; for normal incline operation use SET_INCLINE_ACTION
    void setLcbDCIforceIncline(byte byteValue);
    // Command ID 0x95
    //Get Treadmill in used. 0xFF: used , 0x55: non used When the treadmill in non used condition over 5 sec
    //The LCB shall response 0x55 to UCB when receive the 0x95 commend from UCB
    void getLcbTreadmillInUsed(LcbComm.QueueMode mode);
    // Command ID 0x96
    //CmdGetMainMotor Information 12 bytes
    //Byte0~1:Output Freq(0.01Hz) Byte2~3:Output Current(0.1A) Byte4~5:Output Volt(0.1V)
    //Byte6~7:DC Bus Volt(0.1V) Byte8~9:IGBT Temp(0.1C) Byte10~11:Electromagnet Temp(0.1C)
    //Byte12~13:Output Watts (W) Byte14~15:DC Bus Current(0.1V) 
    void getLcbMainMotorInfo(LcbComm.QueueMode mode);
    // Command ID 0x97
    //Get the status of the LCB for treadmill extend
    //(LCB Type 0xC6 0xC7 )
    //bit 0 : TM-Estop( 1: open, 0:close)
    //bit 1 : 
    //bit 2 : 
    //bit 3 : 
    //bit 4 : 
    //bit 5 : 
    //bit 6 : 
    //bit 7 : 
    //bit 8 : 
    //bit 9 : 
    //bit 10 : 
    //bit 11 : 
    //bit 12 : 
    //bit 13~15 : 
    void getLcbTreadmillExtStatus(LcbComm.QueueMode mode);
    // Command ID 0x98
    //Get the status of the LCB for treadmill extend
    //(LCB Type 0xF1 )
    //bit 0 : ECB movement speed 1: slow / 0: fast
    //bit 1 : ECB resistance position 1: up / 0: down
    //bit 2 : ECB Board 1: On / 0: Off
    //Bit  3 ~ 7 :  Reserve  
    void setLcbTreadmillEcbAction(byte byteValue);
    void setLcbTreadmillEcbActionHighestPriority(byte byteValue);
	
    // Command ID 0xA0
    //DCI Get Incline Range
    void getLcbDCIinclineRange();
    // Command ID 0xA1
    //DCI Set Incline Range works under MANUAL_CALIBRATION, no commanded PWM, and no elevation movement
    void setLcbDCIinclineRange(int intRange);
    // Command ID 0xA6
    //DCI Get Rpm Speed Range
    void getLcbDCIrpmSpeedRange();
    // Command ID 0xA7
    //DCI Set Rpm Speed Range he speed unit is 0.001MPH; works underMANUAL_CALIBRATION, no commanded PWM, and no elevation movement
    void setLcbDCIrpmSpeedRange(double doubleRange);
    // Command ID 0xAA
    //Set special command for LCB
    void setLcbUniversal(byte[] bytesData, LcbComm.QueueMode mode);
    // Command ID 0xAB
    //DCI Set ManualCalibration
    void setLcbDCIManualCalibration();
    // Command ID 0x50
    //Transmitted LCBDeviceData
    void setLcbDeviceData(byte[] bytesDataTransmitted, LcbComm.QueueMode mode);
    void setLcbDeviceDataHighestPriority(byte[] bytesDataTransmitted);
    // Command ID 0x61
    //Reset ECB value
    void setLcbResetECB();
    // Command ID 0x62
    //Set the ECB position.
    void setLcbECBposition(int intPosition);
    // Command ID 0x63
    //Get ECB RPM 0~200
    void getLcbECBrpm(LcbComm.QueueMode mode);
    void getOneShotLcbECBrpmHighPriority();
    // Command ID 0x64
    //Get ECB ADC
    void getLcbECBadc();
    // Command ID 0x65
    //Set PWM percent.
    //Set resistance device duty cycle unit x
    //Set resistance current for LCB1 type 07 and LCB2 type 0A ) unit mA
    void setLcbPWMpercent(int intValue);
    void setLcbPWMpercentHighestPriority(int intValue);
    // Command ID 0x66
    //Get battery capacity data 0.1V
    void getLcbBatteryVoltage();
    void getLcbBatteryVoltageLowPriority(LcbComm.QueueMode mode);
    // Command ID 0x67
    //Set the Watts.
    void setLcbWatts(int intWatts, LcbComm.QueueMode mode);
    void setLcbWattsHighestPriority(int intWatts);
    // Command ID 0x68
    //Set Set RPM gear ratio.
    void setLcbRPMgearRatio(int intRatio);
    // Command ID 0x69
    //Set the generator pole pair
    void setLcbGeneratorPolePair(int intPair);
    // Command ID 0x6A
    //Set limit RPM for charge.
    //Set the RPM value while actual RPM less then setting value the battery charge will disabled
    void setLcbLimitRPMcharge(int intRPM);
    // Command ID 0x6B
    //Set limit RPM for resistance.
    //Set the RPM value while actual RPM less then setting value the resistance of bike set to zero
    void setLcbLimitRPMresistance(int intRPM);
    void setLcbLimitRPMresistanceHighestPriority(int intRPM);
    // Command ID 0x6C
    //Set machine type (0x00: bike , 0x01 : stepper , 0x02 : ramp incline EP , 0x03 : swing incline EP , 0x04 : TM , 0x05 : ClimbMill , 0x06 : ClimbMill-Retail)
    void setLcbMachineType(byte byteType);
    // Command ID 0x6D
    //Set power off (For self-powered system with battery) minute
    void setLcbPowerOff(int intMinute);
    // Command ID 0x6E
    //Set the battery charge level 0~100 %
    void setLcbBatteryChargeLevel(int intMinute);
    // Command ID 0x6F
    //Set min resistance when incline working (0x00 : disable , 0x01 : enable , 0x02: auto setting)
    void setLcbMinResistanceInclineWorking(byte byteType);
    // Command ID 0x80
    //Set incline percent.
    //Set incline travel speed ( 0: 0% 32768: 100% )
    void setLcbInclinePercent(int intValue);
    // Command ID 0x81
    //Get incline percent data travel speed ( 0: 0% 32768: 100% )
    void getLcbInclinePercent(LcbComm.QueueMode mode);
    // Command ID 0x82
    //Set ECB action Data Values (0x01: upward , 0x80: downward , 0x11: stop)
    void setLcbECBaction(byte byteType);
    // Command ID 0x83
    //Get ECB status Data Values (bit 0: ECB up , bit 1: ECB down , bit 2: ECB stop , bit 3: zero , bits 4-7: reserve)
    void getLcbECBstatus();
    // Command ID 0x84
    //Get ECB count
    void getLcbECBcount();
    // Command ID 0x85
    //Electromagnetic current. (mA)
    void setLcbElectromagneticCurrent(int intmA);
    // Command ID 0x86
    //Set Resistance type
    //b15 ~ b13 : load type b12 ~ b0 : resistance 000: electromagnet exp. 0x123 = 29.1 001: resistor (0.1ohm)
    void setLcbResistanceType(int intOhm);
    // Command ID 0x87
    //Set the battery charge current of no resistance 0 ~ 1000 mA
    void setLcbBeginBatteryCharge(int intmA);
    // Command ID 0x88
    //Get battery the percentage of capacity 0 ~ 100 %
    void getLcbBatteryCapacity();
    // Command ID 0x8A
    //Set the action (include E-STOP) of the LCB for climbmill.
    //0x01 : Stop controlling the speed (non-workout set)
    //0x00 : Start to control the speed (workout set)
    //0xA0 : the 1st control zone Disable and IR Disable
    //0xA1 : the 1st control zone Enable and IR Enable
    //0xA2 : the 1st control zone Disable and IR Enable
    //0xA3 : the 1st control zone Enable and IR Disable
    //0xA5 : the 1st control zone Unlock (use when enterE-STOP Mode)
    //0xA6: the 2nd control zone disable
    //0xA7: the 2nd control zone ensable
    //0xB0 : Detect person Disable
    //0xB1 : Detect person Enable
    void setLcbClimbmillStatus(byte byteValue);
    void setLcbClimbmillStatusHighestPriority(byte byteValue);
    // Command ID 0x8B
    //Get the status of the LCB for climbmill
    //(LCB Type 0x17 before is return 1 byte)
    //bit 0 : the speed control ( 1: start, 0:stop)
    //bit 1 : The function of the 2nd control zone( 0: not use, 1: use)
    //bit 2 : IR Sensor use ( 0: not use, 1: use)
    //bit 3 : the status of the IR Sensor is response at once( 0 : No , 1 : Touch)
    //bit 4 : IR Sensor is once touched( 0 : No , 1 : Touch)
    //bit 5 : The function of the 1st control zone use( 0: not use, 1: use)
    //bit 6 : the status of the control zone is response atonce ( 0 : No , 1 : Touch)
    //bit 7 : control zone is once touched( 0 : No , 1 : Touch)
    //bit 8 : UCB Emergency stop status( 0 : No , 1 : Touch)
    //bit 9 : UCB Emergency stop is once touched( 0 : No , 1 : Touch)
    //bit 10 : Handrail Emergency stop status( 0 : No , 1 : Touch)
    //bit 11 : Handrail Emergency stop is once touched( 0 : No , 1 : Touch)
    //bit 12 : No person during using( 0 : No , 1 : Yes)bit 13~15 : N/A
    void getLcbClimbmillStatus(LcbComm.QueueMode mode);
    // Command ID 0x8C
    //Set the E-STOP parameter (4 ~ 100)
    void setLcbEStopParameter(byte byteValue);
    // Command ID 0x8D
    //Get brake pressure value 0~65535 0.01 Volt
    void getLcbBrakePressureValue(LcbComm.QueueMode mode);
    // Command ID 0x8E
    //Get Watts 0~65535 Watts
    void getLcbWatts(LcbComm.QueueMode mode);
    // Command ID 0x8F
    //Set Calibrate climbmill. When LCB get these input information, LCB will auto start calibration.Weight:200-2000 SPM:200-1700 Time:1-3600
    void setLcbClimbmillCal(byte[] bytesClimbmillCal);
    // Command ID 0x47
    //Set record current Data0:point (0 or 1) Data1-2:current
    void setLcbRecordCurrent(byte[] bytesRecordCurrent);
    // Command ID 0x48
    //Set PWM 0 - 1000(0.0% -100.0%)
    //when calibrating, LCB outputs PWM to change watt
    void setLcbPWM(int intPercentage);
    // Command ID 0x49
    //Set 0:first point 1:second point
    //finished calibrating one point
    void setLcbFinishCalibrating(byte byteValue);
    // Command ID 0x4A
    //Set SPM 0.1 SPM
    //Set the step speed of a climbmill Require 0.1 SPM resolution so the value is X10 24.0 SPM is sent as the value 240
    void setLcbSPM(int intSPM);
    // Command ID 0x4B
    //Get SPM 0.1 SPM
    void getLcbSPM(LcbComm.QueueMode mode);
    void getOneShotLcbSpmHighPriority();
    // Command ID 0x4C
    //Set Calibration Scaling 0.1%
    //Set calibration scaling 10-3000
    void setLcbCalScaling(int intCalScaling);
    // Command ID 0x4D
    //Get calibration scaling 0.1% 10-3000
    void getLcbCalScaling(LcbComm.QueueMode mode);
    // Command ID 0x4E
    //Set frame calibration scaling 0.1%
    //Set frame calibration scaling 10-3000
    void setLcbFrameCalScaling(int intFrameCalScaling);
    // Command ID 0x4F
    //Get frame calibration scaling 0.1% 10-3000
    void getLcbFrameCalScaling();
    // Command ID 0x51
    //Get Sensor Data
    void getLcbIndoorBikeSensorData(LcbComm.QueueMode mode);
    void getOneShotLcbIndoorBikeSensorDataHighPriority();
    // Command ID 0x52
    //Set stepper motor position
    void setLcbIndoorBikeStepperMotorPosition(byte[] bytesCmdData);
    // Command ID 0x53
    //Set calculation parameters of device or algorithm
    void setLcbIndoorBikeCalcParaOfDevice(byte[] bytesCmdData);
    // Command ID 0x54
    //Get calculation parameters of device or algorithm
    void getLcbIndoorBikeCalcParaOfDevice();
    // Command ID 0x55
    //Set the action of spin down 
    void setLcbIndoorBikeActionSpinDown(byte[] bytesData );
    // Command ID 0x56
    //Get the info of spin down
    void getLcbIndoorBikeInfoSpinDown(LcbComm.QueueMode mode);

    // Command ID 0x57
    //Set Erg Mode Settings
    void setLcbIndoorBikeErgModeSettings(byte[] bytesData );

    // Command ID 0x58
    //Set IndoorBike resistance level
   // Set resistance level based on frame design.
   // Level 0 is no resistance
    void setLcbIndoorBikeResistanceLevel(int intValue );

    // Command ID 0x59
    //Get Erg Mode Settings
    void getLcbIndoorBikeErgModeSettings();

    //process I/O E-Stopp error trigger 0x02B2 and Machine stop
    void procEStopMachineStop();
    //process If UCB get estop signal, But MCB not have return 02B2 error code within 5 seconds.Send 024F error.
    void procEStopMcbError();
    //process E-Stopp unlock trigger 0x02B1 to UI
    void procEStopUnlock();
    //process If UCB not get RPM from LCB when already set RPM to LCB within 40 seconds.Send 00A1 error.
    void procTmNoRpmError();
    //process setAdvLoggingStatus.
    void procSetAdvLoggingStatus();

    //===============================================================>
    /* IR board Address 0x26 cmd function*/
    // IR board Address 0x26 Command ID 0x73
    //Get the IR board firmware revision
    void getIrAddrVersion();
    void getIrAddrVersionHighestPriority();

    // IR board Address 0x26 Command ID 0x99
    //Get the IR board Ir Status
    void getIrAddrIrStatus(LcbComm.QueueMode mode);
    void getIrAddrIrStatusHighestPriority();
    //IR board Address 0x26 enter Erp cmd 0x7A Mode and callback into status
    void setIrAddrEnterErpMode();

    //IR board Address 0x26 leave Erp cmd 0x7A Mode and callback into status
    void setIrAddrLeaveErpMode();

    // Ir Addr 0x26 Command ID 0x7A
    //Set Ir Addr CmdEUPsMode command
    void setIrAddrCmdEUPsMode(byte byteCmd, boolean doPulse);

    //<===============================================================
	
}
