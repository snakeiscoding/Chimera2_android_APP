package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

import android.util.Log;

import com.jht.chimera.lib.tv.TVControllerBase;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by mutombolin on 2018/09/04.
 */

public class ImportChannelListHelper extends ATSCHelperBase implements Runnable {
    @NonNls private final String TAG = "ImportChannelListHelper";

    private String filename = "";
    private boolean fromUSB = false;

    public ImportChannelListHelper(String filename, TVControllerBase.IOnProgress onProgress) {
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

        File file = new File(filename);
//        if (!file.exists()) {
//            errorExit(ATSC_STATUS.PROCESSING, "file " + filename + " is not exist"); //NON-NLS NON-NLS
////            onStatus(ATSC_STATUS.FILENOTEXISTED);
//            onProgress.onComplete(false, ATSC_STATUS.FILENOTEXISTED);
//            return;
//        }

        length = (int) file.length();
        Log.d(TAG, "runanble: file length = " + length);


        // Sending 'CLI' command to Tuner prepare clone
        sendCLICommand();
        byte cliCommandResult = getCLICommandResult(1000, 10);
        Log.d(TAG, "run: getCLICommandResult: " + cliCommandResult);
        if (cliCommandResult != 1) {
            errorExit(null, "send CLI command failed"); //NON-NLS
//            onStatus(ATSC_STATUS.REPLYFAIL);
            onProgress.onComplete(false, ATSC_STATUS.REPLYFAIL);
            stop();
            return;
        }


        // Sending 'DLH' command to Tuner to set data max length command
        sendDLHCommand(length);
        byte dlhCommandResult = getDLHCommandResult(1000, 10);
        Log.d(TAG, "run: getDLHCommandResult: " + dlhCommandResult);
        if (dlhCommandResult != 1) {
            errorExit(null, "send DLH command failed"); //NON-NLS
//            onStatus(ATSC_STATUS.REPLYFAIL);
            onProgress.onComplete(false, ATSC_STATUS.REPLYFAIL);
            stop();
            return;
        }


        // Sending command to set clone  in data  use with UI command
        int dataReadSize = 0;
        int dataWriteSize = 0;
        int startIndex = 0;
        byte[] data = new byte[CHUNKSIZE];
        byte cloneInDataResult = 0;
        int retry = 10;
        int lastDataWriteSize = -999;
        int lastDataWriteSizeStuckCount = 0;

        try {

            fis = new FileInputStream(file);

//            onStatus(ATSC_STATUS.PROCESSING);
            onProgress.onComplete(false, ATSC_STATUS.PROCESSING);

            while (dataWriteSize < length) {

                dataReadSize = fis.read(data, 0, CHUNKSIZE);
                Log.d(TAG, "runnable: dataReadSize = " + dataReadSize +
                        " startIndex = " + startIndex +
                        " dataWriteSize = " + dataWriteSize +
                        " length = " + length);

                if (dataReadSize != -1) {
                    cloneInDataResult = 0;
                    retry = 10;
                    while (cloneInDataResult == 0 && retry > 0) {
                        setCloneInDataResult(cloneInDataResult);
                        sendCloneInData(startIndex, dataReadSize, data);

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ignore) {
                        }

                        cloneInDataResult = getCloneInDataResult(300, 10);
                        retry--;

                        Log.d(TAG, "cloneInDataResult = " + cloneInDataResult + ", retry = " + retry);

                    }
                    if (cloneInDataResult == 1) {
                        startIndex++;
                        dataWriteSize += dataReadSize;

                        Log.w(TAG, "run: call onImportPregress: " + (int) (dataWriteSize * 100) / length);

//                        onImportProgress((int) (dataWriteSize * 100) / length);
                        onProgress.onProgress((int) (dataWriteSize * 100) / length);
                    }
//					if(startIndex >= 3)
//						break;
                } else {
                    fis.close();
                    break;
                }

//				status = getImportStatus(100);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignore) {
                }

            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "runnable: FileNotFoundException ", e);
        } catch (IOException e) {
            Log.e(TAG, "runnable: IOException ", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, "runnable: Close fis failed.", e);
                }
            }
        }
/*
		if (status != ATSC_STATUS.ACKOK) {
			errorExit(status, "set clone data failed");
			return;
		}
*/
//        onImportProgress(100);
        onProgress.onProgress(100);


        // waiting Tuner to feedback import result
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignore) {
        }

//		sendGetImportResult();
        byte getImportResult = 0;
        getImportResult = getImportResult(1000, 20);

        Log.w(TAG, "getImportResult = " + getImportResult);

        if (getImportResult != 0x01) {
//            onStatus(ATSC_STATUS.IMPORTFAIL);
            onProgress.onComplete(false, ATSC_STATUS.IMPORTFAIL);
            errorExit(status, "Import channel list failed"); //NON-NLS
            stop();
            return;
        } else {
//            onStatus(ATSC_STATUS.IMPORTSUCCESS);
            onProgress.onComplete(true, ATSC_STATUS.IMPORTSUCCESS);
        }

//		onATSCTunerCallback(status);
//		closeIO();
        stop();
        Log.d(TAG, "runnable: stop thread.");
    }

}
