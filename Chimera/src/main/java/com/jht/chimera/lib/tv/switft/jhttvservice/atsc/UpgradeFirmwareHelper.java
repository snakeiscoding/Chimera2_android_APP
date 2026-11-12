package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

import android.util.Log;

import com.jht.chimera.lib.common.datamatrixkeys.TVKeys;
import com.jht.chimera.lib.common.usb.StorageUtils;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.switft.SwiftManager;
import com.jht.datamatrixlib.DataMatrix;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by mutombolin on 2018/09/03.
 */

public class UpgradeFirmwareHelper extends ATSCHelperBase implements Runnable {
    @NonNls private final String TAG = "UpgradeFirmwareHelper";

    private String filename = "";
    private static final String ATSC_US_FIRMWARE = "MSD3553T_8MB.bin"; //NON-NLS
    private static final String ATSC_PROIDIOM_FIRMWARE = "MSD3553T_PI_8MB.bin"; //NON-NLS
    private static final String ATSC_BRAZIL_FIRMWARE = "MSD3663I_8M.bin"; //NON-NLS
    private static final String JAPAN_MST_FIRMWARE = "MST_JHT.bin"; //NON-NLS
	private static final String PROIPTV_FIRMWARE = "MSD3683B_JHT.bin"; //NON-NLS
    private static final int ATSC_US_TUNER_TYPE = 1;
    private static final int ATSC_PROIDIOM_TUNER_TYPE = 2;
    private static final int ATSC_BRAZIL_TUNER_TYPE = 3;
    private static final int JAPAN_MST_TUNER_TYPE = 4;
    private static final int PROIPTV_TUNER_TYPE = 5;

    public UpgradeFirmwareHelper(String filename, TVControllerBase.IOnProgress onProgress) {
        this.filename = filename;
        this.onProgress = onProgress;
    }

    public void start() {
        start(this);
    }


    @Override
    public void run() {
        Log.d(TAG, "runnable: start Thread");
        int length = 0;
        FileInputStream fis = null;
        ATSC_STATUS status = ATSC_STATUS.PROCESSING;


        // Try to get SwiftInfo tuner type version
        int retryCount = 5;
        while(retryCount > 0) {
            SwiftManager.getInstance().getSwiftTunerTypeVersion();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            Log.d(TAG, "run: retryCount = " + retryCount + ", DATA_TUNER_TYPE_INT = " + DataMatrix.getInt(TVKeys.DATA_TUNER_TYPE_INT));
            if(DataMatrix.getInt(TVKeys.DATA_TUNER_TYPE_INT) != 0)
                break;
            retryCount --;
        }

        String ATSCUSFirmwareName = StorageUtils.getUSBPath().getAbsolutePath() + "/" + ATSC_US_FIRMWARE;
        String ATSCProidiomFirmwareName = StorageUtils.getUSBPath().getAbsolutePath() + "/" + ATSC_PROIDIOM_FIRMWARE;
        String ATSCBrazilFirmwareName = StorageUtils.getUSBPath().getAbsolutePath() + "/" + ATSC_BRAZIL_FIRMWARE;
        String JapanMSTFirmwareName = StorageUtils.getUSBPath().getAbsolutePath() + "/" + JAPAN_MST_FIRMWARE;
        String ProIPTVFirmwareName = StorageUtils.getUSBPath().getAbsolutePath() + "/" + PROIPTV_FIRMWARE;
        int tunerType = DataMatrix.getInt(TVKeys.DATA_TUNER_TYPE_INT);

        // If idenfity tuner type failed, show failed message
        if((tunerType != ATSC_US_TUNER_TYPE) && (tunerType != ATSC_PROIDIOM_TUNER_TYPE) && (tunerType != ATSC_BRAZIL_TUNER_TYPE) && (tunerType != JAPAN_MST_TUNER_TYPE) && (tunerType != PROIPTV_TUNER_TYPE)) {
            Log.w(TAG, "run: identify tuner type failed, TunerType = " + tunerType);
            onProgress.onComplete(false, ATSC_STATUS.IDENTIFYFAIL);
            stop();
            return;
        }

        // Check whether firmware name is match tuner type, if doesn't match, show failed message
        if(tunerType == ATSC_US_TUNER_TYPE)
            filename = ATSCUSFirmwareName;
        else if(tunerType == ATSC_PROIDIOM_TUNER_TYPE)
            filename = ATSCProidiomFirmwareName;
        else if(tunerType == ATSC_BRAZIL_TUNER_TYPE)
            filename = ATSCBrazilFirmwareName;
        else if(tunerType == JAPAN_MST_TUNER_TYPE)
            filename = JapanMSTFirmwareName;
        else if (tunerType == PROIPTV_TUNER_TYPE)
        	filename = ProIPTVFirmwareName;

        Log.d(TAG, "run: tuner type version = " + tunerType + ", filename = " + filename);

        File file = new File(filename);
        if (!file.exists()) {
            Log.w(TAG, "run: file " + filename + " is not existed");
//            onStatus(ATSC_STATUS.FILENOTEXISTED);
            onProgress.onComplete(false, ATSC_STATUS.FILENOTEXISTED);
            stop();
            return;
        } else
            Log.w(TAG, "run: filename = " + filename + "is existed~!");

        length = (int) file.length();
        Log.d(TAG, "runnable: file length = " + length);

/*
		// close io
		Log.d(TAG, "runnable: close io");
		closeIO();
		delay(1000);
*/

//		openIO(Uart.ATSC_TUNER_CONFIG);
//		delay(3000);

        // Sending 'UPG use with  UI command (0x00B0)
        // prepare clone (Enter Update mode )
        Log.d(TAG, "send UPG command");
        sendUPGCommand();
        byte upgCommandResult = getUPGCommandResult(100, 10);
        Log.d(TAG, "run: getUPGCommandResult: " + upgCommandResult);
        if (upgCommandResult != 1) {
            errorExit(null, "send UPG command failed"); //NON-NLS
//            onStatus(ATSC_STATUS.REPLYFAIL);
            onProgress.onComplete(false, ATSC_STATUS.REPLYFAIL);
            stop();
            return;
        }

        // Sending 'DLH' command to Tuner to set data max length command
        sendDLHCommand(length);
        byte dlhCommandResult = getDLHCommandResult(100, 10);
        Log.d(TAG, "run: getDLHCommandResult: " + dlhCommandResult);
        if (dlhCommandResult != 1) {
            errorExit(null, "send DLH command failed"); //NON-NLS
//            onStatus(ATSC_STATUS.REPLYFAIL);
            onProgress.onComplete(false, ATSC_STATUS.REPLYFAIL);
            stop();
            return;
        }

        // reopen serial port in 115200
        Log.d(TAG, "runnable: open port in 115200");
        setBaudRate115200();// --->


        // Sending command to set clone  in data  use with UI command
        Log.d(TAG, "set clone data");
        int dataReadSize = 0;
        int startIndex = 0;
        int dataWriteSize = 0;
        byte[] data = new byte[CHUNKSIZE];
        byte cloneInDataResult = 0;
        int retry = 10;
        int lastDataWriteSize = -999;
        int lastDataWriteSizeStuckCount = 0;
        int failedCount = 0;

        long startTime = 0;

        try {
            fis = new FileInputStream(file);

//            onStatus(ATSC_STATUS.PROCESSING);
            onProgress.onComplete(false, ATSC_STATUS.PROCESSING);

            while (dataWriteSize < length) {

                dataReadSize = fis.read(data, 0, CHUNKSIZE);
                Log.d(TAG, "runnable: dataReadSize = " + dataReadSize +
                        " startIndex = " + startIndex +
                        " dataWriteSize = " + dataWriteSize);

                if (dataReadSize != -1) {
                    cloneInDataResult = 0;
                    retry = 10;
                    while (cloneInDataResult == 0 && retry > 0) {
                        setCloneInDataResult(cloneInDataResult);
                        sendCloneInData(startIndex, dataReadSize, data);

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignore) {
                        }

                        cloneInDataResult = getCloneInDataResult(50, 10);
                        if(cloneInDataResult == 0)
                            failedCount ++;
                        retry--;

                        Log.d(TAG, "cloneInDataResult = " + cloneInDataResult + ", retry = " + retry + ", failCount = " + failedCount);

                    }
                    startIndex++;
                    dataWriteSize += dataReadSize;

                    Log.w(TAG, "run: call onUpdateProgress: " + (int) (dataWriteSize * 100) / length);

//                    onUpdateProgress((int) (dataWriteSize * 100) / length);
                    onProgress.onProgress((int) (dataWriteSize * 100) / length);

//					if(startIndex >= 3)
//						break;
                } else {
                    fis.close();
                    break;
                }

//				status = getImportStatus(100);

                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignore) {
                }

                if (failedCount >= 1000) {
                    onProgress.onComplete(false, ATSC_STATUS.UPGRADEFAIL);
                    errorExit(status, "Upgrade firmware failed"); //NON-NLS
                    Log.d(TAG, "runnable: open port in 9600");
                    setBaudRate9600();
                    fis.close();
                    stop();
                    return;
                }

            }
/*
			while (status == ATSC_STATUS.ACKOK) {
				long elapsed = System.currentTimeMillis() - startTime;
				startTime = System.currentTimeMillis();
				Log.d(TAG, "runnable: elapsed = " + elapsed);
				dataReadSize = fis.read(data, 0, CHUNKSIZE);
				Log.d(TAG, "runnable: dataReadSize = " + dataReadSize + " startIndex = " + startIndex + " dataWriteSize = " + dataWriteSize);
				int count = 3;
				
				if (dataReadSize != -1) {
					status = ATSC_STATUS.PROCESSING;
					while ((status == ATSC_STATUS.PROCESSING || status == ATSC_STATUS.TIMEOUTFAIL) && count > 0) {
						Log.d(TAG, "status = " + status.toString() + " count = " + count);
						setCloneData(startIndex, dataReadSize, data); --->
						
						status = getCloneStatus(10);
						count--;
					}
					
					if (status == ATSC_STATUS.ACKOK) {
						startIndex++;
						dataWriteSize += dataReadSize;
						onUpdateProgress((int)(dataWriteSize * 100)/length);
					}
				} else {
					fis.close();
					break;
				}
			}
*/
        } catch (FileNotFoundException e) {
            Log.e(TAG, "runnable: FileNotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "runnable: IOException", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "runnable: IOException", e);
                }
            }
        }
/*
		if (status != ATSC_STATUS.ACKOK) {
			errorExit(status, "set clone data failed");
			return;
		}
*/
//        onUpdateProgress(100);
        onProgress.onProgress(100);


        // waiting Tuner to feedback import result
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignore) {
        }


        // check de result
        byte getImportResult = 0;
        getImportResult = getImportResult(1000, 20);

        Log.w(TAG, "getImportResult = " + getImportResult);

        if (getImportResult != 0x01) {
//            onStatus(ATSC_STATUS.UPGRADEFAIL);
            onProgress.onComplete(false, ATSC_STATUS.UPGRADEFAIL);
            errorExit(status, "Upgrade firmware failed"); //NON-NLS
            Log.d(TAG, "runnable: open port in 9600");
            setBaudRate9600();// --->
            stop();
            return;
        } else {
//			onStatus(ATSC_STATUS.IMPORTSUCCESS);
        }

        // check df result
        byte getUpgradeResult = 0;
        getUpgradeResult = getUpgradeResult(1000, 20);

        Log.w(TAG, "getUpgradeResult = " + getImportResult);

        if (getUpgradeResult != 0x01) {
            errorExit(status, "Upgrade failed"); //NON-NLS
            onProgress.onComplete(false, ATSC_STATUS.UPGRADEFAIL);
            stop();
//			return;
        } else {
//            onStatus(ATSC_STATUS.UPGRADESUCCESS);
            onProgress.onComplete(true, ATSC_STATUS.UPGRADESUCCESS);
        }

        // reopen serial port in 9600
        Log.d(TAG, "runnable: open port in 9600");
        setBaudRate9600();// --->

//		onATSCTunerCallback(status);
//		closeIO();
        stop();
        Log.d(TAG, "runnable: stop thread.");
    }
}
