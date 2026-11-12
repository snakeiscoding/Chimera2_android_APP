package com.jht.chimera.lib.tv.switft;


import static com.jht.chimera.lib.tv.switft.PacketConstant.PACKET_MIN_BYTES;

class PacketConstant {
    // HEADER(1 bytes) + CRC(1 byte)
    public static final int PACKET_MIN_BYTES = 2;
}

public enum SwiftCommands {
	/* ID , PACKET size , command , IR code	*/
/*  Sorted by Frank Hung
    SetSwiftPowerOn( 			0x0001, PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'N'}, 	(byte)0x00, 	0),
    SetSwiftPowerOff( 			0x0002, PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'F'}, 	(byte)0x60, 	0),
    SetSwiftVolValue( 	0x0003, PACKET_MIN_BYTES+1, 	new char[]{'S', 'V', 'L'}, 	(byte)0x42, 	0),
    SetSwiftRtnChannel(	 		0x0004, PACKET_MIN_BYTES, 	new char[]{'G', 'C', 'H'},	(byte)0x45, 	4),
    SetSwiftMenu( 		0x0005, PACKET_MIN_BYTES, 	new char[]{'M', 'E', 'M'},	(byte)0x1A, 	0),
*/
    SetSwiftPowerOn( 		    	    0x0001, PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'N'}, 	            (byte)0x00, 	0),
    SetSwiftPowerOff( 			        0x0002, PACKET_MIN_BYTES, 	new char[]{'P', 'O', 'F'}, 	            (byte)0x60, 	0),
    SetSwiftInputSource( 		        0x0003, PACKET_MIN_BYTES, 	new char[]{'S', 'R', 'C'}, 	            (byte)0x08, 	0),
    SetSwiftMute(           		    0x0004, PACKET_MIN_BYTES, 	new char[]{'M', 'U', 'T'}, 	            (byte)0x40, 	0),
    SetSwiftMenu( 	    	            0x0005, PACKET_MIN_BYTES, 	new char[]{'M', 'E', 'M'},	            (byte)0x1A, 	0),
    SetSwiftEnter( 		                0x0006, PACKET_MIN_BYTES, 	new char[]{'E', 'N', 'T'},	            (byte)0x1C, 	0),
    SetSwiftExit( 		                0x0007, PACKET_MIN_BYTES, 	new char[]{'E', 'X', 'T'},	            (byte)0x18, 	0),
    SetSwiftAudioDec( 		            0x0008, PACKET_MIN_BYTES, 	new char[]{'A', 'L', 'T'},	            (byte)0x1B, 	0),
    SetSwiftAudioInc( 		            0x0009, PACKET_MIN_BYTES, 	new char[]{'A', 'R', 'T'},	            (byte)0x1D, 	0),
    SetSwiftChannelUp( 		            0x000A, PACKET_MIN_BYTES, 	new char[]{'C', 'U', 'P'},	            (byte)0x19, 	0),
    SetSwiftChannelDown( 		        0x000B, PACKET_MIN_BYTES, 	new char[]{'C', 'D', 'N'},	            (byte)0x1E, 	0),
    SetSwiftNumber0(                    0x000C, PACKET_MIN_BYTES, 	new char[]{'0', '0', '0'},	            (byte)0x16, 	0),
    SetSwiftNumber1(	                0x000D, PACKET_MIN_BYTES, 	new char[]{'0', '0', '1'},	            (byte)0x0C, 	0),
    SetSwiftNumber2(   	                0x000E, PACKET_MIN_BYTES, 	new char[]{'0', '0', '2'},	            (byte)0x0D, 	0),
    SetSwiftNumber3(                    0x000F, PACKET_MIN_BYTES, 	new char[]{'0', '0', '3'},	            (byte)0x0E, 	0),
    SetSwiftNumber4(                    0x0010, PACKET_MIN_BYTES, 	new char[]{'0', '0', '4'},	            (byte)0x0F, 	0),
    SetSwiftNumber5(                    0x0011, PACKET_MIN_BYTES, 	new char[]{'0', '0', '5'},	            (byte)0x10, 	0),
    SetSwiftNumber6(                    0x0012, PACKET_MIN_BYTES, 	new char[]{'0', '0', '6'},	            (byte)0x11, 	0),
    SetSwiftNumber7(                    0x0013, PACKET_MIN_BYTES, 	new char[]{'0', '0', '7'},	            (byte)0x12, 	0),
    SetSwiftNumber8(                    0x0014, PACKET_MIN_BYTES, 	new char[]{'0', '0', '8'},	            (byte)0x13, 	0),
    SetSwiftNumber9(                    0x0015, PACKET_MIN_BYTES, 	new char[]{'0', '0', '9'},	            (byte)0x14, 	0),
    SetSwiftDash( 		                0x0016, PACKET_MIN_BYTES,   new char[]{'1', '0', '0', '/', '.'},	(byte)0x17, 	0),
    SetSwiftReturn( 		            0x0017, PACKET_MIN_BYTES, 	new char[]{'R', 'T', 'N'},	            (byte)0x15, 	0),
    SetSwiftClosedCaption( 		        0x0018, PACKET_MIN_BYTES, 	new char[]{'C', 'C', 'D'},	            (byte)0x47, 	0),
    SetSwiftAspectRatio(                0x0019, PACKET_MIN_BYTES, 	new char[]{'A', 'P', 'T'},	            (byte)0x43, 	0),
    SetSwiftVChip( 		                0x001A, PACKET_MIN_BYTES, 	new char[]{'V', 'C', 'P'},	            (byte)0x4F, 	0),
    SetSwiftCompositeIn( 		        0x001B, PACKET_MIN_BYTES, 	new char[]{'C', 'B', 'S'},	            (byte)0x59, 	0),
    SetSwiftSVideoIn( 		            0x001C, PACKET_MIN_BYTES, 	new char[]{'S', 'V', 'O'},	            (byte)0x5A, 	0),
    SetSwiftComponentIn(  		        0x001D, PACKET_MIN_BYTES, 	new char[]{'C', 'M', 'P'},	            (byte)0x5B, 	0),
    SetSwiftVGAIn( 		                0x001E, PACKET_MIN_BYTES, 	new char[]{'V', 'G', 'A'},	            (byte)0x5C, 	0),
    SetSwiftTunerIn(     	    	    0x001F, PACKET_MIN_BYTES, 	new char[]{'T', 'N', 'R'},	            (byte)0x5D, 	0),
    SetSwiftHDMIIn( 	    	        0x0020, PACKET_MIN_BYTES, 	new char[]{'H', 'D', 'M'},	            (byte)0x5E, 	0),
    GetSwiftStatus( 		            0x0021, PACKET_MIN_BYTES, 	new char[]{'S', 'T', 'A'},	            (byte)0x5F, 	0),
    SetSwiftEPG( 	    	            0x0022, PACKET_MIN_BYTES, 	new char[]{'E', 'P', 'G'},	            (byte)0x48, 	0),
    SetSwiftTeletext(        		    0x0023, PACKET_MIN_BYTES, 	new char[]{'T', 'X', 'T'},	            (byte)0x54, 	0),
    SetSwiftRed( 		                0x0024, PACKET_MIN_BYTES, 	new char[]{'R', 'E', 'D'},	            (byte)0x55, 	0),
    SetSwiftGreen( 		                0x0025, PACKET_MIN_BYTES, 	new char[]{'G', 'R', 'E'},	            (byte)0x56, 	0),
    SetSwiftYellow( 		            0x0026, PACKET_MIN_BYTES, 	new char[]{'Y', 'E', 'L'},	            (byte)0x57, 	0),
    SetSwiftBlue( 		                0x0027, PACKET_MIN_BYTES, 	new char[]{'B', 'L', 'U'},	            (byte)0x58, 	0),
    GetSwiftTunerType( 		            0x0028, PACKET_MIN_BYTES, 	new char[]{'A', 'D', 'I'},	            (byte)0xFB, 	0),
    SetSwiftFavorite( 		            0x0029, PACKET_MIN_BYTES, 	new char[]{'F', 'A', 'V'},	            (byte)0x66, 	0),
    GetSwiftSoftwareVersion(            0x002A, PACKET_MIN_BYTES, 	new char[]{'V', 'E', 'R'},	            (byte)0x67, 	1),
    GetSwiftManufacturer( 		        0x002B, PACKET_MIN_BYTES, 	new char[]{'M', 'P', 'C'},	            (byte)0x68, 	1),
    GetSwiftComponentOutputMode(        0x002C, PACKET_MIN_BYTES, 	new char[]{'G', 'O', 'M'},	            (byte)0x69, 	1),
    SetSwiftComponentOutput1080i(       0x002D, PACKET_MIN_BYTES, 	new char[]{'O', 'M', '1'},	            (byte)0xE1, 	0),
    SetSwiftComponentOutput480i(        0x002E, PACKET_MIN_BYTES, 	new char[]{'O', 'M', '2'},	            (byte)0xE2, 	0),
    SetSwiftComponentOutput576i(        0x002F, PACKET_MIN_BYTES, 	new char[]{'O', 'M', '3'},	            (byte)0xE3, 	0),
    SetSwiftComponentOutput480p(        0x0030, PACKET_MIN_BYTES, 	new char[]{'O', 'M', '4'},	            (byte)0xE4, 	0),
    SetSwiftComponentOutput576p(        0x0031, PACKET_MIN_BYTES, 	new char[]{'O', 'M', '5'},	            (byte)0xE5, 	0),
    SetSwiftComponentOutput720p(        0x0032, PACKET_MIN_BYTES, 	new char[]{'O', 'M', '6'},	            (byte)0xE6, 	0),
    GetSwiftHDMIOutputMode(             0x0033, PACKET_MIN_BYTES, 	new char[]{'G', 'O', 'H'},	            (byte)0x70, 	1),
    SetSwiftHDMIOutput720p(             0x0034, PACKET_MIN_BYTES, 	new char[]{'O', 'H', '1'},	            (byte)0xE9, 	0),
    SetSwiftHDMIOutput1080p(            0x0035, PACKET_MIN_BYTES, 	new char[]{'O', 'H', '2'},	            (byte)0xEA, 	0),

    SetSwiftCloneOutDataRequire(        0x0036, PACKET_MIN_BYTES, 	new char[]{'C', 'L', 'O'},	            (byte)0xDB, 	1),
    GetSwiftCloneDataLength(            0x0037, PACKET_MIN_BYTES, 	new char[]{'L', 'E', 'N'},	            (byte)0xDC, 	4),
    SetSwiftNotifyStartSendCloneData(   0x0038, PACKET_MIN_BYTES, 	new char[]{'S', 'E', 'N'},	            (byte)0x00, 	0),
    SetSwiftNotifyCloneIn( 		        0x0039, PACKET_MIN_BYTES, 	new char[]{'C', 'L', 'I'},	            (byte)0xDB, 	1),
    SetSwiftNotifyCloneDataLength(      0x003A, PACKET_MIN_BYTES, 	new char[]{'D', 'L', 'H'},              (byte)0xDD, 	1),
    SetSwiftNotifySoftwareUpgrade(      0x003B, PACKET_MIN_BYTES, 	new char[]{'U', 'P', 'G'},	            (byte)0xDB, 	1),
    SetSwiftMuteOn( 		            0x003C, PACKET_MIN_BYTES, 	new char[]{'M', 'U', 'N'},	            (byte)0x40, 	0),
    SetSwiftMuteOff( 		            0x003D, PACKET_MIN_BYTES, 	new char[]{'M', 'U', 'F'},	            (byte)0x40, 	0),
    GetSwiftMuteStatus(                 0x003E, PACKET_MIN_BYTES, 	new char[]{'G', 'M', 'S'},	            (byte)0xEC, 	1),
    SetSwiftVolumeValue(                0x003F, PACKET_MIN_BYTES, 	new char[]{  1,        0}, 	            (byte)0xED, 	1),
    GetSwiftVolumeValue(                0x0040, PACKET_MIN_BYTES, 	new char[]{'G', 'V', 'L'},	            (byte)0xEB, 	1),
    GetSwiftCurrentChannel( 		    0x0041, PACKET_MIN_BYTES, 	new char[]{'G', 'C', 'H'},	            (byte)0x45, 	4),
    GetSwiftTunerTypeVersion(           0x0042, PACKET_MIN_BYTES, 	new char[]{'G', 'V', 'R'},	            (byte)0x46, 	1),
    SetSwiftCurrentChannel( 		    0x0043, PACKET_MIN_BYTES, 	new char[]{'S', 'C', 'H'},              (byte)0x49, 	0),
    SetSwiftDefaultChannel( 		    0x0044, PACKET_MIN_BYTES, 	new char[]{'S', 'D', 'H'},              (byte)0x4A, 	0),
    GetSwiftCloneOutData(               0x0045, PACKET_MIN_BYTES, 	new char[]{'E', 'X', 'S'},	            (byte)0xD0, 	0),
    SetSwiftSendCloneInData(            0x0046, PACKET_MIN_BYTES, 	new char[]{'I', 'M', 'S'},	            (byte)0xD1, 	1),

    SetSwiftTest1(                      0x0047, PACKET_MIN_BYTES, 	new char[]{'A', 'B', 'C'},	            (byte)0x00, 	0),
    SetSwiftTest2(                      0x0048, PACKET_MIN_BYTES, 	new char[]{  1,   2,   3},	            (byte)0x00, 	0),
    SetSwiftTest3( 				        0x0049, PACKET_MIN_BYTES, 	new char[]{},               			(byte)0x00, 	0),

    GetSwiftImportResult(		        0x004A, PACKET_MIN_BYTES, 	new char[]{' ', ' ', ' '},     			(byte)0xDE, 	1),
    GetSwiftUSBHandler(                 0x004B, PACKET_MIN_BYTES, 	new char[]{'U', 'S', 'B'},	            (byte)0x00, 	0),
    SetSwiftAudioChannelSwitch(         0x004C, PACKET_MIN_BYTES, 	new char[]{'A', 'C', 'S'},	            (byte)0x4B, 	0),
    SetSwiftInitializeScanChannel(      0x004D, PACKET_MIN_BYTES, 	new char[]{'I', 'S', 'C'},	            (byte)0xD2, 	0),
    SetSwiftReboot(                     0x004E, PACKET_MIN_BYTES, 	new char[]{'S', 'R', 'T'}, 	            (byte)0x2E, 	0),

    SetSwiftNone( 				        0xFFFF, PACKET_MIN_BYTES, 	new char[]{}, 			                (byte)0xFF, 	0);

    private int _valueID;
    private int _maxMessageSize;
    private char[] _command;
    private byte _irCode;
    private int _rtnDataSize;

    SwiftCommands(int valueID, int maxMessageSize,char[] command,byte irCode,int rtnDataSize) {
        this._valueID = valueID;
        this._maxMessageSize = maxMessageSize;
        this._command = command;
        this._irCode = irCode;
        this._rtnDataSize = rtnDataSize;
    }

    public static SwiftCommands fromInt(int i) {
        for (SwiftCommands b : SwiftCommands.values()) {
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

    public void setrtnDataSize(int dataSize){
        _rtnDataSize = dataSize;
    }

}
