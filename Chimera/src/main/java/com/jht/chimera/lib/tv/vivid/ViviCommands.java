package com.jht.chimera.lib.tv.vivid;

class PacketConstant {
    // HEADER(1 bytes) + CRC(1 byte)
    public static final int PACKET_MIN_BYTES = 2;
}

public enum ViviCommands {
	/* ID , PACKET size , command , IR code	*/
/*  Sorted by Frank Hung
    SetViviPowerOn( 			0x0001, PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'N'}, 	(byte)0x00, 	0),
    SetViviPowerOff( 			0x0002, PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'F'}, 	(byte)0x60, 	0),
    SetViviVolValue( 	0x0003, PACKET_MIN_BYTES+1, 	new char[]{'S', 'V', 'L'}, 	(byte)0x42, 	0),
    SetViviRtnChannel(	 		0x0004, PACKET_MIN_BYTES, 	new char[]{'G', 'C', 'H'},	(byte)0x45, 	4),
    SetViviMenu( 		0x0005, PACKET_MIN_BYTES, 	new char[]{'M', 'E', 'M'},	(byte)0x1A, 	0),
*/
    SetViviPowerOn( 		    	    0x0001, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'N'}, 	            (byte)0x00, 	0),
    SetViviPowerOff( 			        0x0002, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'F'}, 	            (byte)0x60, 	0),
    SetViviInputSource( 		        0x0003, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'S', 'R', 'C'}, 	            (byte)0x08, 	0),
    SetViviMute(           		        0x0004, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'M', 'U', 'T'}, 	            (byte)0x40, 	0),
    SetViviMenu( 	    	            0x0005, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'M', 'E', 'M'},	            (byte)0x1A, 	0),
    SetViviEnter( 		                0x0006, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'E', 'N', 'T'},	            (byte)0x1C, 	0),
    SetViviExit( 		                0x0007, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'E', 'X', 'T'},	            (byte)0x18, 	0),
    SetViviAudioDec( 		            0x0008, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'A', 'L', 'T'},	            (byte)0x1B, 	0),
    SetViviAudioInc( 		            0x0009, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'A', 'R', 'T'},	            (byte)0x1D, 	0),
    SetViviChannelUp( 		            0x000A, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'U', 'P'},	            (byte)0x19, 	0),
    SetViviChannelDown( 		        0x000B, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'D', 'N'},	            (byte)0x1E, 	0),
    SetViviNumber0(                     0x000C, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '0'},	            (byte)0x16, 	0),
    SetViviNumber1(	                    0x000D, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '1'},	            (byte)0x0C, 	0),
    SetViviNumber2(   	                0x000E, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '2'},	            (byte)0x0D, 	0),
    SetViviNumber3(                     0x000F, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '3'},	            (byte)0x0E, 	0),
    SetViviNumber4(                     0x0010, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '4'},	            (byte)0x0F, 	0),
    SetViviNumber5(                     0x0011, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '5'},	            (byte)0x10, 	0),
    SetViviNumber6(                     0x0012, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '6'},	            (byte)0x11, 	0),
    SetViviNumber7(                     0x0013, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '7'},	            (byte)0x12, 	0),
    SetViviNumber8(                     0x0014, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '8'},	            (byte)0x13, 	0),
    SetViviNumber9(                     0x0015, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'0', '0', '9'},	            (byte)0x14, 	0),
    SetViviDash( 		                0x0016, PacketConstant.PACKET_MIN_BYTES,   new char[]{'1', '0', '0', '/', '.'},	(byte)0x17, 	0),
    SetViviReturn( 		                0x0017, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'R', 'T', 'N'},	            (byte)0x15, 	0),
    SetViviClosedCaption( 		        0x0018, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'C', 'D'},	            (byte)0x47, 	0),
    SetViviAspectRatio(                 0x0019, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'A', 'P', 'T'},	            (byte)0x43, 	0),
    SetViviVChip( 		                0x001A, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'V', 'C', 'P'},	            (byte)0x4F, 	0),
    SetViviCompositeIn( 		        0x001B, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'B', 'S'},	            (byte)0x59, 	0),
    SetViviSVideoIn( 		            0x001C, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'S', 'V', 'O'},	            (byte)0x5A, 	0),
    SetViviComponentIn(  		        0x001D, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'M', 'P'},	            (byte)0x5B, 	0),
    SetViviVGAIn( 		                0x001E, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'V', 'G', 'A'},	            (byte)0x5C, 	0),
    SetViviTunerIn(     	    	    0x001F, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'T', 'N', 'R'},	            (byte)0x5D, 	0),
    SetViviHDMIIn( 	    	            0x0020, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'H', 'D', 'M'},	            (byte)0x5E, 	0),
    GetViviStatus( 		                0x0021, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'S', 'T', 'A'},	            (byte)0x5F, 	0),
    SetViviEPG( 	    	            0x0022, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'E', 'P', 'G'},	            (byte)0x48, 	0),
    SetViviTeletext(        		    0x0023, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'T', 'X', 'T'},	            (byte)0x54, 	0),
    SetViviRed( 		                0x0024, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'R', 'E', 'D'},	            (byte)0x55, 	0),
    SetViviGreen( 		                0x0025, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'R', 'E'},	            (byte)0x56, 	0),
    SetViviYellow( 		                0x0026, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'Y', 'E', 'L'},	            (byte)0x57, 	0),
    SetViviBlue( 		                0x0027, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'B', 'L', 'U'},	            (byte)0x58, 	0),
    GetViviTunerType( 		            0x0028, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'A', 'D', 'I'},	            (byte)0xFB, 	0),
    SetViviFavorite( 		            0x0029, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'F', 'A', 'V'},	            (byte)0x66, 	0),
    GetViviSoftwareVersion(             0x002A, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'V', 'E', 'R'},	            (byte)0x67, 	0),
    GetViviManufacturer( 		        0x002B, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'M', 'P', 'C'},	            (byte)0x68, 	0),
    GetViviComponentOutputMode(         0x002C, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'O', 'M'},	            (byte)0x69, 	0),
    SetViviComponentOutput1080i(        0x002D, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'M', '1'},	            (byte)0xE1, 	0),
    SetViviComponentOutput480i(         0x002E, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'M', '2'},	            (byte)0xE2, 	0),
    SetViviComponentOutput576i(         0x002F, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'M', '3'},	            (byte)0xE3, 	0),
    SetViviComponentOutput480p(         0x0030, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'M', '4'},	            (byte)0xE4, 	0),
    SetViviComponentOutput576p(         0x0031, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'M', '5'},	            (byte)0xE5, 	0),
    SetViviComponentOutput720p(         0x0032, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'M', '6'},	            (byte)0xE6, 	0),
    GetViviHDMIOutputMode(              0x0033, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'O', 'H'},	            (byte)0x70, 	0),
    SetViviHDMIOutput720p(              0x0034, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'H', '1'},	            (byte)0xE9, 	0),
    SetViviHDMIOutput1080p(             0x0035, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'O', 'H', '2'},	            (byte)0xEA, 	0),

    SetViviCloneOutData(                0x0036, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'L', 'O'},	            (byte)0xDB, 	0),
    GetViviCloneDataLength(             0x0037, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'L', 'E', 'N'},	            (byte)0xDC, 	0),
    SetViviNotifyStartSendCloneData(    0x0038, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'S', 'E', 'N'},	            (byte)0x00, 	0),
    SetViviNotifyCloneIn( 		        0x0039, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'C', 'L', 'I'},	            (byte)0xDB, 	0),
    SetViviNotifyCloneDataLength(       0x003A, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'D', 'L', 'H', 0, 0, 0, 0},  (byte)0xDD, 	0),
    SetViviNotifySoftwareUpgrade(       0x003B, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'U', 'P', 'G'},	            (byte)0xDB, 	0),
    SetViviMuteOn( 		                0x003C, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'M', 'U', 'N'},	            (byte)0x40, 	0),
    SetViviMuteOff( 		            0x003D, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'M', 'U', 'F'},	            (byte)0x40, 	0),
    GetViviMuteStatus(                  0x003E, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'M', 'S'},	            (byte)0xEC, 	0),
    SetViviVolumeValue(                 0x003F, PacketConstant.PACKET_MIN_BYTES, 	new char[]{  0,   1, 100},	            (byte)0xED, 	0),
    GetViviVolumeValue(                 0x0040, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'V', 'L'},	            (byte)0xEB, 	0),
    GetViviCurrentChannel( 		        0x0041, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'C', 'H'},	            (byte)0x45, 	0),
    GetViviTunerTypeVersion(            0x0042, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'G', 'V', 'R'},	            (byte)0x46, 	0),
    SetViviCurrentChannel( 		        0x0043, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'S', 'C', 'H', 0, 0, 0, 0},  (byte)0x49, 	0),
    SetViviDefaultChannel( 		        0x0044, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'S', 'D', 'H', 0, 0, 0, 0},  (byte)0x4A, 	0),
    GetViviCloneOutData(                0x0045, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'E', 'X', 'S', 0, 0, 0, 0, 0, 0, 0, 0},	            (byte)0xD0, 	0),
    SetViviSendCloneInData(             0x0046, PacketConstant.PACKET_MIN_BYTES, 	new char[]{'I', 'M', 'S', 0, 0, 0, 0, 0, 0, 0, 0},	            (byte)0xD1, 	0),

    SetViviNone( 				        0xFFFF, PacketConstant.PACKET_MIN_BYTES, 	new char[]{}, 			(byte)0xFF, 	0);
	
    private int _valueID;
    private int _maxMessageSize;
    private char[] _command;
    private byte _irCode;
    private int _rtnDataSize;

    ViviCommands(int valueID, int maxMessageSize,char[] command,byte irCode,int rtnDataSize) {
        this._valueID = valueID;
        this._maxMessageSize = maxMessageSize;
        this._command = command;
        this._irCode = irCode;
        this._rtnDataSize = rtnDataSize;
    }

    public static ViviCommands fromInt(int i) {
        for (ViviCommands b : ViviCommands.values()) {
            if (b._valueID == i) {
                return b;
            }
        }
        return null;
    }

    public int getValueID() {
        return _valueID;
    }

    public int getMaxMessageSize() {
        return _maxMessageSize;
    }

    public char[] getCommand() {
        return _command;
    }

    public byte getIrCode() {
        return _irCode;
    }

    public int getrtnDataSize() {
        return _rtnDataSize;
    }
	
}
