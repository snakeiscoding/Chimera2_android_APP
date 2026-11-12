package com.jht.chimera.service.hal.lib.lcb.lcbupdatecommlib.E600ADboard;

public class ADboardLCBinfo {
	private static final String TAG = "ADboardLCBinfo";
	/************************************************************************************
	 *    Flash Data Address, Length and info
	 *************************************************************************************/
	// ADboardLCB base address
	public final int ADboardLCB_BASE_ = 0x08000000;

	// === Erase Block ===>
	// BLOCK_A address offset
	public final int ADboardLCB_BLOCK_A_OFFSET_ = 0x0001FC00;

	// BLOCK_A length
	// Length  = 0x1FFFF - 0x1FC00 +1
	public final int ADboardLCB_BLOCK_A_LEN_ = 0x00000400;

	// BLOCK_B address offset
	public final int ADboardLCB_BLOCK_B_OFFSET_ = 0x00006000;

	// BLOCK_B length
	// Length  = 0x1FBFF - 0x6000 +1
	public final int ADboardLCB_BLOCK_B_LEN_ = 0x00019C00;
	//<=== Erase Block === 

	// === Write Block  ==>
	// BLOCK_1 address offset (Program Code)
	public final int ADboardLCB_BLOCK_1_WRITE_OFFSET_ = 0x00006000;

	// WriteLen = 0x1F7FF - 0x6000 +1
	public final int ADboardLCB_BLOCK_1_WRITE_LEN_ = 0x00019800;

	// BLOCK_2 address offset ( Memory Initial Table)
	public final int ADboardLCB_BLOCK_2_WRITE_OFFSET_ = 0x0001F800;

	// WriteLen = 0x1FBFF - 0x1F800 +1
	public final int ADboardLCB_BLOCK_2_WRITE_LEN_ = 0x000003F0;

	// BLOCK_3 address offset ( Interrupt Vector Table)
	public final int ADboardLCB_BLOCK_3_WRITE_OFFSET_ = 0x0001FC00;

	// WriteLen = 0x1FEFF - 0x1FC00 +1
	public final int ADboardLCB_BLOCK_3_WRITE_LEN_ = 0x000003B0;
	//<=== Write Block ===


	/************************************************************************************
	 *    Bin File Address , Length and info
	 *************************************************************************************/
	// MCU Type address
	public final int ADboardLCB_MCU_TYPE_ADDR_ = 0x0001FF4C;
	//Module Name address
	public final int ADboardLCB_MODULE_NAME_ADDR_ = 0x0001FF60;
	//Module No address
	public final int ADboardLCB_MODULE_NO_ADDR_ = 0x0001FF74;

//	public final char[] ADboardLCB_McuType = {'S', 'T', 'M', '3', '2', 'F', '1', '0', '2', 'R', 'B'};
//	public final char[] ADboardLCB_ModuleName = {'L', 'C', 'B', '-', 'A'};
//	public final char[] ADboardLCB_ModuleNo = {'L', '1', '1', '8', 'A', '1', '_', 'M', 'C', 'U', '-', 'B'};
	public final char[] ADboardLCB_McuType = {'S', 'T', 'M', '3', '2', 'F', '1', '0', '2', 'R', 'B'};
	public final char[] ADboardLCB_ModuleName = {'6', '0', '0', 'E', '_', 'D', 'B'};
	public final char[] ADboardLCB_ModuleNo = {'L', '1', '9', '8', 'A', '1'};
	
	// === Bin File Read Block 1 ==>
	// === Bin File Block 1
	// ADboardLCB bin file base address
	public final int ADboardLCB_BIN_FILE_BASE_ = 0x00000000;
	// BLOCK_1 bin file address offset
	public final int ADboardLCB_BIN_FILE_BLOCK_1_READ_OFFSET_ = 0x00006000;

	// === Bin File Block 2
	// BLOCK_2 bin file address offset
//	public final int ADboardLCB_BIN_FILE_BLOCK_2_READ_OFFSET_ = 0x0001F800;
	public final int ADboardLCB_BIN_FILE_BLOCK_2_READ_OFFSET_ = 0x00000310;

	// === Bin File Block 3
	// BLOCK_3 bin file address offset
	public final int ADboardLCB_BIN_FILE_BLOCK_3_READ_OFFSET_ = 0x0001FC00;

	public ADboardLCBinfo() {
	}
}
