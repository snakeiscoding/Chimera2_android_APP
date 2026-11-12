package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

import android.util.Log;

import com.jht.androidcommonalgorithms.util.Strings;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.switft.SwiftChannelInfo;
import com.jht.chimera.lib.tv.switft.SwiftCrc;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by mutombolin on 2018/09/03.
 */

public class ExportChannelListHelper extends ATSCHelperBase implements Runnable {
    @NonNls private final String TAG = "ExportChannelListHelper";

    private String filename = "";
    private AtomicInteger atomicLength = new AtomicInteger(0);
    private ByteBuffer byteBuffer;
    private boolean toUSB = true;

    // Channel data base structure
    private ArrayList<SwiftChannelInfo> mChannelInfo = new ArrayList<SwiftChannelInfo>();
    private static List<Byte> mChannelListData = new ArrayList<Byte>();
    private int mTotalChannelCount = 0;
    private byte[] mDatabaseHead = new byte[12];
    private byte[] mChannelCount = new byte[2];
    private byte[] mCheckSum = new byte[4];
    private final int DATABASE_HEAD_LENGTH = 12;

    //	private static int mTotalCloneDataLength = 0;
    private static int mCloneDataIndex = 0;
    private static String mCloneOutData = "";
    //	private static int mTotalGetDataLength = 0;
    private static int mLastTotalReadDataLength = -999;

    public ExportChannelListHelper(String filename, TVControllerBase.IOnProgress onProgress) {
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
//		ATSC_STATUS status = ATSC_STATUS.PROCESSING;

        resetParameters();

        // Send 'CLO' command to TV
        Log.d(TAG, "runnable: Send CLO command");
        sendCLOCommand();

        byte cloCommandResult = getCLOCommandResult(1000, 10);
        Log.d(TAG, "run: getCLOCommandResult: " + cloCommandResult);
        if (cloCommandResult != 1) {
            errorExit(null, "send CLO command failed"); //NON-NLS
//            onStatus(ATSC_STATUS.REPLYFAIL);
            onProgress.onComplete(false, ATSC_STATUS.REPLYFAIL);
            stop();
            return;
        }

        // Send 'LEN' command to TV
        Log.d(TAG, "runnable: get Length");
        sendLENCommand();

        int lenCommandResult = getLENCommandResult(1000, 10);
        length = lenCommandResult;
        Log.d(TAG, "run: getLENCommandResult: " + lenCommandResult);
        if (lenCommandResult <= 0) {
            errorExit(null, "get length of channel list from tuner failed"); //NON-NLS
//            onStatus(ATSC_STATUS.REPLYFAIL);
            onProgress.onComplete(false, ATSC_STATUS.REPLYFAIL);
            stop();
            return;
        }

        // Send read export data command to get clone data.
        Log.d(TAG, "runnable: clone data");
        Log.d(TAG, "runnable: length = " + length);
        atomicLength.set(0);
        int dataReadSize = 0;
        int startIndex = 0;
        FileOutputStream fos = null;
        File file = new File(filename);
        int chunkSize = CHUNKSIZE;
        int getLengthStuckcount = 0;
        boolean checkChecksumResult = false;
        boolean usbOK = true;
/*
		String PATH_TO_TEMP = "/data/local/tmp";

		File dirFile = new File(PATH_TO_TEMP);
		dirFile.setReadable(true, false);
		dirFile.setExecutable(true, false);
		dirFile.setWritable(true, false);
*/
        try {
            Log.w(TAG, "ExportChannelListHelper.java run() ########## gate 1, filename = " + filename + ", toUSB = " + toUSB + ", usbOK = " + usbOK);

            if (toUSB) {

                // Delete the file and make sure we make any parent folders needed.
                try {
                    if (file.exists())
                        file.delete();
                } catch (Exception e) {
                    Log.w(TAG, "run: delete file failed", e);
                    usbOK = false;
                }
                file.getParentFile().mkdirs();

                try {
                    if (!file.createNewFile()) {
                        Log.d(TAG, "file " + filename + " already exists.");
                        usbOK = false;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "run: permission denied", e);
//                    onStatus(ATSC_STATUS.PERMISSIONDENIED);
//                    onProgress.onComplete(false, "Permission denied or No such file or directory");
                    usbOK = false;
//                    return;
                }

                Log.w(TAG, "ExportChannelListHelper.java run() ########## gate 2, toUSB = " + toUSB + ", usbOK = " + usbOK + ", file = " + file.getName());

                if(usbOK)
                    fos = new FileOutputStream(file);
            }

            Log.w(TAG, "ExportChannelListHelper.java run() ########## gate 3, getTotalReadDataLength() = " + getTotalReadDataLength() + ", length = " + length);

//            onStatus(ATSC_STATUS.PROCESSING);
            onProgress.onComplete(false, ATSC_STATUS.PROCESSING);


            while (getTotalReadDataLength() < length) {
                Log.w(TAG, "ExportChannelListHelper.java run() ##########, getTotalGetDataLength = " + getTotalReadDataLength() + ", mLastTotalReadDataLength = " + mLastTotalReadDataLength + ", length = " + length + ", mCloneDataIndex = " + mCloneDataIndex);

                if (getTotalReadDataLength() == mLastTotalReadDataLength) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignore) {
                    }
                    getLengthStuckcount++;
                    if (getLengthStuckcount > 100) {
                        errorExit(null, "get clone out data timeout! failed"); //NON-NLS
//                        onStatus(ATSC_STATUS.TIMEOUTFAIL);
                        onProgress.onComplete(false, ATSC_STATUS.TIMEOUTFAIL);

                        if (fos != null) {
                            fos.close();
                        }
                        stop();
                        return;
                    }
                    continue;
                } else {
                    getLengthStuckcount = 0;
                }

//                SwiftManager.getInstance().getSwiftCloneOutData(mCloneDataIndex, 200 /*mTotalCloneDataLength*/);
//				onStatus(ATSC_STATUS.PROCESSING);
                int tmp = (int) (getTotalReadDataLength() * 100) / length;
                if (getLengthStuckcount == 0/* && tmp % 5 == 0*/)
//                    onExportProgress((int) (getTotalReadDataLength() * 100) / length);
                    onProgress.onProgress((int) (getTotalReadDataLength() * 100) / length);

                if (length - getTotalReadDataLength() >= 200) {
//					sendExportData(startIndex, chunkSize);
//					SwiftManager.getInstance().getSwiftCloneOutData(mCloneDataIndex, 200);
                    sendExportData(mCloneDataIndex, 200);
                    mCloneDataIndex++; //mCloneDataIndex += 200;
                } else {
//					SwiftManager.getInstance().getSwiftCloneOutData(mCloneDataIndex, mTotalCloneDataLength - mTotalGetDataLength);
                    sendExportData(mCloneDataIndex, length - getTotalReadDataLength());
                    //mCloneDataIndex += mTotalCloneDataLength - mTotalGetDataLength;

                    Log.w(TAG, "ExportChannelListHelper.java run() ##########, getTotalGetDataLength = " + getTotalReadDataLength() + ", length = " + length + ", mCloneDataIndex = " + mCloneDataIndex + "BBBRRRREEEAAAAKAKKKKKK");

                    break;
                }

                mLastTotalReadDataLength = getTotalReadDataLength();

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignore) {
                }
            }


            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignore) {
            }

            mChannelListData = getCloneData();
            Log.w(TAG, "ExportChannelListHelper.java run() ##########, mChannelListData.size = " + mChannelListData.size());
            Log.w(TAG, "ExportChannelListHelper.java run() ##########, call Parse()");


            // Convert List<Byte> to byte[]
            byte[] data = new byte[mChannelListData.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) mChannelListData.get(i);
            }

            Log.w(TAG, "ExportChannelListHelper.java run() ##########, toUSB = " + toUSB + ", usbOK = " + usbOK);

            if (toUSB && usbOK)
                fos.write(data);

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignore) {
            }
/*
			onStatus(ATSC_STATUS.EXPORTSUCCESS);
			onExportProgress(100);
*/
            if (Parse()) {
                Log.d(TAG, "run: parse result = true");
                checkChecksumResult = true;
                // callback to service for parse channels
//				onParseData(data, data.length);

            } else {
                Log.d(TAG, "run: parse result = false");
                errorExit(null, "parse checksum is different! failed"); //NON-NLS
                onProgress.onComplete(false, ATSC_STATUS.CHECKSUMERROR);

                checkChecksumResult = false;

                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Log.w(TAG, e);
                    }
                }
//				return;
            }

            if (checkChecksumResult)
//                onExportProgress(100);
                onProgress.onProgress(100);

//			onStatus(ATSC_STATUS.EXPORTSUCCESS);
//			onExportProgress(100);

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignore) {
            }


            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "runnable: file output stream close failed.", e);
                }
            }

//            if (checkChecksumResult) {
//
////                onStatus(ATSC_STATUS.EXPORTSUCCESS);
//                onProgress.onComplete(true, ATSC_STATUS.EXPORTSUCCESS);
////				onExportProgress(100);
//
//            } else {
////                onStatus(ATSC_STATUS.CHECKSUMERROR);
//                onProgress.onComplete(false, ATSC_STATUS.CHECKSUMERROR);
//            }

            // compare export content from tuner and from .bin file, just for test should be deleted
/*
			File file2 = new File(filename);
			if (!file2.exists()) {
				errorExit(ATSC_STATUS.PROCESSING, "file " + filename + " is not exist");
				return;
			}

			length = (int)file2.length();
			Log.w(TAG, "runanble: check .bin file size, file2 length = " + length);

			Path path = Paths.get(filename);
			byte[] fileContents =  Files.readAllBytes(path);

			Log.w(TAG, "runanble: fileContents length = " + fileContents.length);

			for(int i=0; i<12; i++) {
				Log.w(TAG, "index = " + i + ", fileContents = " + Integer.toHexString(fileContents[i] & 0xFF));
			}
			for(int i=fileContents.length-10; i<fileContents.length; i++) {
				Log.w(TAG, "index = " + i + ", fileContents = " + Integer.toHexString(fileContents[i] & 0xFF));
			}
*/
            /////////////
/*
			while (status == ATSC_STATUS.ACKOK) {
				if (byteBuffer != null && byteBuffer.array().length > 0) {
					Log.d(TAG, "runnable: byteBuffer size = " + byteBuffer.array().length);
//					fos.write(byteBuffer.array());
					byteBuffer = null;
					onExportProgress((int)(dataReadSize * 100)/length);
				}
				
				if (length - dataReadSize < CHUNKSIZE) {
					chunkSize = length - dataReadSize;
				}
				
				dataReadSize += chunkSize;
				
				Log.d(TAG, "runnable: length = " + length + " dataReadSize = " + dataReadSize + " startIndex = " + startIndex + " chunkSize = " + chunkSize );
				
				if (chunkSize > 0) {
					atomicLength.set(0);
					sendExportData(startIndex, chunkSize);
					startIndex++;
				} else {
//					if (fos != null) {
//						fos.close();
//					}
					break;
				}
				
				status = getStatus(10);
				
				if (status != ATSC_STATUS.ACKOK) {
					break;
				}
				
				byteBuffer = getCloneData(10);
				
				if (byteBuffer == null || byteBuffer.array().length <= 0) {
					status = ATSC_STATUS.PROCESSING;
					break;
				} else {
					atomicLength.set(byteBuffer.array().length);
				}
			}
*/
        } catch (FileNotFoundException e) {
            Log.e(TAG, "runnable: FileNotFoundException", e);
        } catch (IOException e) {
            Log.e(TAG, "runnable: IOException ", e);
        }
//		finally {
//			if (fos != null) {
//				try {
//					fos.close();
//				} catch (IOException e) {
//					Log.e(TAG, "runnable: file output stream close failed.");
//				}
//			}

//			closeIO();
//		}
/*
		if (status != ATSC_STATUS.ACKOK) {
			errorExit(status, "clone data failed");
			return;
		}
*/
//		onExportProgress(100);

        // Convert List<Byte> to byte[]

        try {
            byte[] data = new byte[mChannelListData.size()];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) mChannelListData.get(i);
            }
            if (checkChecksumResult)
                onParseData(data, data.length);
        } catch (Exception e) {
            Log.w(TAG, "run: exception !!", e);
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignore) {
        }

        if (checkChecksumResult) {

//                onStatus(ATSC_STATUS.EXPORTSUCCESS);
            Log.d(TAG, "ccctest throw ATSC_STATUS.EXPORTSUCCESS message");

            onProgress.onComplete(true, ATSC_STATUS.EXPORTSUCCESS);
//				onExportProgress(100);

        } else {
//                onStatus(ATSC_STATUS.CHECKSUMERROR);
            onProgress.onComplete(false, ATSC_STATUS.CHECKSUMERROR);
        }

//		onATSCTunerCallback(ATSC_STATUS.ACKOK);

        stop();
        Log.d(TAG, "runnable: stop Thread");

    }

    public void resetParameters() {
        mChannelInfo = new ArrayList<SwiftChannelInfo>();
        mChannelListData = new ArrayList<Byte>();
        mTotalChannelCount = 0;
        mDatabaseHead = new byte[12];
        mChannelCount = new byte[2];
        mCheckSum = new byte[4];

        mCloneDataIndex = 0;
        mCloneOutData = "";
        mLastTotalReadDataLength = -999;
    }

    public boolean Parse() {
        String information = "";
        boolean parseResult = false;

        for (int i = 0; i < DATABASE_HEAD_LENGTH; i++)
            mDatabaseHead[i] = mChannelListData.get(i);

        mTotalChannelCount = mChannelListData.get(12) * 256 + mChannelListData.get(13);
        Log.w(TAG, "ExportChannelListHelper.java Parse(), get(12) = " + mChannelListData.get(12) + ", get(13) = " + mChannelListData.get(13) + ", mTotalChannelCount = " + mTotalChannelCount);

        // get parsed check sum
        mCheckSum[0] = mChannelListData.get(mChannelListData.size() - 4);
        mCheckSum[1] = mChannelListData.get(mChannelListData.size() - 3);
        mCheckSum[2] = mChannelListData.get(mChannelListData.size() - 2);
        mCheckSum[3] = mChannelListData.get(mChannelListData.size() - 1);

        byte[] majorChanNo = new byte[2];
        byte[] minorChanNo = new byte[2];
        byte[] chName = new byte[33];
        char serviceType = 0;
        char isFav = 0;
        char isSkip = 0;

        // Get each channel information
        int index = 0;
        int channelNameIndex = 0;
        int totalParsedChannelCount = 0;
        for (int i = 14; i < mChannelListData.size(); i++) {

//            Log.w(TAG, "ExportChannelListHelper.java Parse(), i = " + i + ", index = " + index + ", data = " + mChannelListData.get(i));

            if (index == 0) {
                majorChanNo[0] = mChannelListData.get(i);
//                Log.w(TAG, "ExportChannelListHelper.java Parse(), majorChanNo[0] = " + majorChanNo[0]);

            } else if (index == 1) {
                majorChanNo[1] = mChannelListData.get(i);
//                Log.w(TAG, "ExportChannelListHelper.java Parse(), majorChanNo[1] = " + majorChanNo[1]);

            } else if (index == 2) {
                minorChanNo[0] = mChannelListData.get(i);
//                Log.w(TAG, "ExportChannelListHelper.java Parse(), minorChanNo[0] = " + minorChanNo[0]);

            } else if (index == 3) {
                minorChanNo[1] = mChannelListData.get(i);
//                Log.w(TAG, "ExportChannelListHelper.java Parse(), minorChanNo[1] = " + minorChanNo[1]);

            } else if (index >= 4 && index <= 36) {
                chName[channelNameIndex] = (mChannelListData.get(i).byteValue());
                channelNameIndex++;
            } else if (index == 37) {
                serviceType = (char) (mChannelListData.get(i).byteValue());
//                Log.w(TAG, "ExportChannelListHelper.java Parse() HHHHHHHHHH, serviceType = " + serviceType + ", mChannelListData.get(i) = " + mChannelListData.get(i));
            } else if (index == 38) {
                isFav = (char) (mChannelListData.get(i).byteValue());
//                Log.w(TAG, "ExportChannelListHelper.java Parse() HHHHHHHHHH, isFav = " + isFav + ", mChannelListData.get(i) = " + mChannelListData.get(i));
            } else if (index == 39) {
                isSkip = (char) (mChannelListData.get(i).byteValue());
//                Log.w(TAG, "ExportChannelListHelper.java Parse() HHHHHHHHHH, isSkip = " + isSkip + ", mChannelListData.get(i) = " + mChannelListData.get(i));
            }

            index++;

            if (index == 40) {
//                Log.w(TAG, "ExportChannelListHelper.java Parse() BEFORE mChannelInfo.size() = " + mChannelInfo.size());
/*
                for(int j=0; j<mChannelInfo.size(); j++) {
                    Log.w(TAG, "ExportChannelListHelper.java Parse() BEFORE @@@@@@@@@@, j = " + j + ", getMajorChanNo = " + bytesToHex(channelInfo.getMajorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() BEFORE @@@@@@@@@@, j = " + j + ", getMinorChanNo = " + bytesToHex(channelInfo.getMinorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() BEFORE ##########, j = " + j + ", getMajorChanNo = " + bytesToHex(mChannelInfo.get(j).getMajorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() BEFORE ##########, j = " + j + ", getMinorChanNo = " + bytesToHex(mChannelInfo.get(j).getMinorChanNo()));
                }
*/
                // add parsed channel info
                SwiftChannelInfo channelInfo = new SwiftChannelInfo();
                channelInfo.setMajorChannelNo(majorChanNo);
                channelInfo.setMinorChannelNo(minorChanNo);
                channelInfo.setChannelName(chName);
                channelInfo.setServiceType(serviceType);
                channelInfo.setIsFav(isFav);
                channelInfo.setIsSkip(isSkip);

/*
                    Log.w(TAG, "ExportChannelListHelper.java Parse() PLAN TO ADD @@@@@@@@@@, getMajorChanNo = " + bytesToHex(channelInfo.getMajorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() PLAN TO ADD @@@@@@@@@@, getMinorChanNo = " + bytesToHex(channelInfo.getMinorChanNo()));
*/
                mChannelInfo.add(channelInfo);

                // Clear all local parameters
                majorChanNo = new byte[2];
                minorChanNo = new byte[2];
                chName = new byte[33];
                serviceType = 0;
                isFav = 0;
                isSkip = 0;

                Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER mChannelInfo.size() = " + mChannelInfo.size());

                for (int j = 0; j < mChannelInfo.size(); j++) {
                    /*
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER @@@@@@@@@@, j = " + j + ", getMajorChanNo = " + bytesToHex(channelInfo.getMajorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER @@@@@@@@@@, j = " + j + ", getMinorChanNo = " + bytesToHex(channelInfo.getMinorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER ##########, j = " + j + ", getMajorChanNo = " + bytesToHex(mChannelInfo.get(j).getMajorChanNo()));
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER ##########, j = " + j + ", getMinorChanNo = " + bytesToHex(mChannelInfo.get(j).getMinorChanNo()));
                    */
                    /*
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER ##########, j = " + j + ", ServiceType = " + mChannelInfo.get(j).getServiceType());
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER ##########, j = " + j + ", IsFav = " + mChannelInfo.get(j).getIsFav());
                    Log.w(TAG, "ExportChannelListHelper.java Parse() AFTER ##########, j = " + j + ", IsSkip = " + mChannelInfo.get(j).getIsSkip());
                    */

                }
                index = 0;
                channelNameIndex = 0;

                totalParsedChannelCount++;

                // all channels are pasted
                if (totalParsedChannelCount >= mTotalChannelCount)
                    break;
            }
        }

        information += String.format("Head: %s\n", Strings.bytesToHex(mDatabaseHead));
        information += String.format("Total Channel Count = %d\n", mTotalChannelCount);

        for (int i = 0; i < mChannelInfo.size(); i++) {
            information += "\n";
            information += String.format("Channel Number %d:\n", i + 1);
            information += String.format("majorChanNo = %s\n", Strings.bytesToHex(mChannelInfo.get(i).getMajorChanNo()));
            information += String.format("minorChanNo = %s\n", Strings.bytesToHex(mChannelInfo.get(i).getMinorChanNo()));
            information += String.format("chName = %s\n", String.valueOf(mChannelInfo.get(i).getChannelName()));
            information += String.format("serviceType = 0x%02X\n", (int) mChannelInfo.get(i).getServiceType());
            information += String.format("isFav = 0x%02X\n", (int) mChannelInfo.get(i).getIsFav());
            information += String.format("isSkip = 0x%02X\n", (int) mChannelInfo.get(i).getIsSkip());
        }

        int calculatedCheckSum = SwiftCrc.calCrc(mChannelListData, mChannelListData.size() - 4);

        information += String.format("\nParsed Check Sum: %s, %d", Strings.bytesToHex(mCheckSum), unsignedBytesToInt(mCheckSum[3], mCheckSum[2], mCheckSum[1], mCheckSum[0]));
        information += String.format("\nCalculated Check Sum: %s, %d\n", Integer.toHexString(calculatedCheckSum), calculatedCheckSum);
        information += String.format("\nTotal data length: %d", mChannelListData.size());

        Log.w(TAG, "Parse: result = " + information);
//        information += String.format("\nCalculated Check Sum: %d\n", calculatedCheckSum);

        if (calculatedCheckSum == unsignedBytesToInt(mCheckSum[3], mCheckSum[2], mCheckSum[1], mCheckSum[0])) {
            parseResult = true;
        } else {
            parseResult = false;
        }

//		textViewParseResult.setText(information);
        return true; //parseResult;
    }

    public static int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    public static int unsignedBytesToInt(byte lsb, byte msb) {
        return (unsignedByteToInt(lsb) + (unsignedByteToInt(msb) << 8));
    }

    public static int unsignedBytesToInt(byte b0, byte b1, byte b2, byte b3) {
        return (unsignedByteToInt(b0)
                + (unsignedByteToInt(b1) << 8))
                + (unsignedByteToInt(b2) << 16)
                + (unsignedByteToInt(b3) << 24);
    }
}
