package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

import android.util.Log;

import com.jht.chimera.lib.common.tv.TvEvent;
import com.jht.chimera.lib.tv.TVControllerBase;
import com.jht.chimera.lib.tv.switft.ISwiftCommResponse;
import com.jht.chimera.lib.tv.switft.SwiftManager;

import org.jetbrains.annotations.NonNls;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mutombolin on 2018/09/03.
 */

public class ATSCHelperBase {
    @NonNls private final String TAG = "JCL_ATSCHelperBase";

    public ATSCHelperBase() {
//        this.onATSCTuner = onATSCTuner;
    }

    public void registerCallback() {
        SwiftManager.getInstance().registerSwiftOnReceiveListener(mSwiftCommResponse);
    }

    public enum ProcessType {
        ImportChannelList,
        ExportChannelList,
        UpgradeFirmware;
    }

    protected static final int CHUNKSIZE = 1024;

    protected ATSC_STATUS ioStatus;
    protected boolean inUpdate = false;
    //	protected IOInterface io;
    protected boolean isRunning = false;
    protected TVControllerBase.IOnProgress onProgress;

    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    protected ProcessType type = ProcessType.ImportChannelList;

    private AtomicInteger atomicInteger = new AtomicInteger(0);
    private AtomicInteger atomicLength = new AtomicInteger(0);

    private ByteBuffer byteBuffer;

    private byte mCloneOutRequireResult = 0;
    private byte mNotifyCloneInResult = 0;
    private byte mNotifyCloneDataLengthResult = 0;
    private byte mCloneInDataResult = 0;
    private byte mImportResult = 0;
    private byte mUpgradeResult = 0;
    private int mCloneOutDataLength = 0;
    private List<Byte> mChannelListData = new ArrayList<Byte>();
    private int mTotalReadDataLength = 0;
    private byte mNotifyUpgradeFirmwareResult = 0;


    private ISwiftCommResponse mSwiftCommResponse = new ISwiftCommResponse() {

        public void onSetSwiftNone(byte byteCmd) {
        }

        @Override
        public void onErrorCode(byte byteError) {
        }

        @Override
        public void onPowerOn() {
        }

        @Override
        public void onMenu() {
        }

        @Override
        public void onNumber1() {
        }

        @Override
        public void onNumber2() {
        }

        @Override
        public void onNumber3() {
        }

        @Override
        public void onNumber4() {
        }

        @Override
        public void onNumber5() {
        }

        @Override
        public void onNumber6() {
        }

        @Override
        public void onNumber7() {
        }

        @Override
        public void onNumber8() {
        }

        @Override
        public void onNumber9() {
        }

        @Override
        public void onNumber0() {
        }

        @Override
        public void onDash() {
        }

        @Override
        public void onEnter() {
        }

        @Override
        public void onChannelUp() {
        }

        @Override
        public void onChannelDown() {
        }

        @Override
        public void onVolumeUp() {
        }

        @Override
        public void onVolumeDown() {
        }

        @Override
        public void onPowerOff() {
        }

        @Override
        public void onMute() {
        }

        @Override
        public void onInputSource() {
        }

        @Override
        public void onExit() {
        }

        @Override
        public void onReturn() {
        }

        @Override
        public void onClosedCaption() {
        }

        @Override
        public void onRed() {
        }

        @Override
        public void onGreen() {
        }

        @Override
        public void onYellow() {
        }

        @Override
        public void onBlue() {
        }

        @Override
        public void onAspectRatio() {
        }

        @Override
        public void onVChip() {
        }

        @Override
        public void onEPG() {
        }

        @Override
        public void onTeletext() {
        }

        @Override
        public void onCompositeIn() {
        }

        @Override
        public void onSVideoIn() {
        }

        @Override
        public void onComponentIn() {
        }

        @Override
        public void onVGAIn() {
        }

        @Override
        public void onTunerIn() {
        }

        @Override
        public void onHDMIIn() {
        }

        @Override
        public void onFavorite() {
        }

        @Override
        public void onSetComponent1080i() {
        }

        @Override
        public void onSetComponent480i() {
        }

        @Override
        public void onSetComponent576i() {
        }

        @Override
        public void onSetComponent480p() {
        }

        @Override
        public void onSetComponent576p() {
        }

        @Override
        public void onSetComponent720p() {
        }

        @Override
        public void onSetHDMI720p() {
        }

        @Override
        public void onSetHDMI1080p() {
        }

        @Override
        public void onMuteOn() {
        }

        @Override
        public void onMuteOff() {
        }

        @Override
        public void onGetTunerType() {
/*
            Log.w(TAG, "SwiftCloneChannelActiviry.java onGetTunerType(), payload = " + payload);

            String tunerType = Integer.toHexString(payload & 0xFF);
            textViewTunerType.setText(tunerType);
*/
        }

        @Override
        public void onGetTunerType(byte irCode) {

            Log.w(TAG, "SwiftCloneChannelActiviry.java onGetTunerType(), irCode = " + irCode);

            @NonNls String tunerType = Integer.toHexString(irCode & 0xFF);
            tunerType = "0x" + tunerType;

            Log.w(TAG, "SwiftCloneChannelActiviry.java onGetTunerType(), (Integer.toHexString(irCode & 0xFF) = " + (Integer.toHexString(irCode & 0xFF)));
//			Log.w(TAG, "SwiftCloneChannelActiviry.java onGetTunerType(), result 1 = " + (Integer.toHexString(irCode & 0xFF).toLowerCase() == "fc") + ", result 2 = " + (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("fc")));


            if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("f7"))
                tunerType += " - Pro:idiom Tuner";
            else if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("f8"))
                tunerType += " - DVB-C/T/S Tuner";
            else if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("f9"))
                tunerType += " - DMBT/H Tuner";
            else if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("fa"))
                tunerType += " - Brazil Tuner";
            else if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("fb"))
                tunerType += " - US Tuner";
            else if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("fc"))
                tunerType += " - EU Tuner";
            else if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("fd"))
                tunerType += " - JP Tuner";
//            textViewTunerType.setText(tunerType);

        }

        @Override
        public void onGetComponentOutput(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetComponentOutput(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            @NonNls String componentOutput = Integer.toHexString(payload[0] & 0xFF);
            componentOutput = "0x" + componentOutput;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                componentOutput += " - 1080i";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("2"))
                componentOutput += " - 480i";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("3"))
                componentOutput += " - 576i";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("4"))
                componentOutput += " - 480p";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("5"))
                componentOutput += " - 576p";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("6"))
                componentOutput += " - 720p";
//            textViewComponentOutput.setText(componentOutput);
        }

        @Override
        public void onGetHDMIOutput(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetHDMIOutput(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            @NonNls String hdmiOutput = Integer.toHexString(payload[0] & 0xFF);
            hdmiOutput = "0x" + hdmiOutput;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                hdmiOutput += " - 720p";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("2"))
                hdmiOutput += " - 1080p";
//            textViewHDMIOutput.setText(hdmiOutput);
        }

        @Override
        public void onGetStatus(byte irCode) {

            Log.w(TAG, "SwiftCloneChannelActiviry.java onGetStatus(), irCode = " + irCode);

            @NonNls String status = Integer.toHexString(irCode & 0xFF);
            status = "0x" + status;

            Log.w(TAG, "SwiftCloneChannelActiviry.java onGetStatus(), (Integer.toHexString(irCode & 0xFF) = " + (Integer.toHexString(irCode & 0xFF)));

            // if tuner return 5f, tuner is ready
            if (Integer.toHexString(irCode & 0xFF).equalsIgnoreCase("5f"))
                status += " - Ready";
            else
                status += " - Not Ready";
//            textViewStatus.setText(status);
        }

        @Override
        public void onGetManufacturer(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetManufacturer(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            @NonNls String manufacturer = Integer.toHexString(payload[0] & 0xFF);
            manufacturer = "0x" + manufacturer;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                manufacturer += " - AVerMedia";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("2"))
                manufacturer += " - Vivimetric";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("3"))
                manufacturer += " - SwiftInfo";
            else
                manufacturer += " - No definition";
//            textViewManufacturer.setText(manufacturer);

            Log.w(TAG, "onGetManufacturer: manufacturer = " + (payload[0] & 0xFF));
        }

        public void onGetSoftwareVersion(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetManufacturer(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            @NonNls String softwareVersion = Integer.toHexString(payload[0] & 0xFF);
            softwareVersion = "0x" + softwareVersion;

            int intVersion = payload[0] & 0xFF;
            int high = intVersion / 16;
            int low = intVersion % 16;
            softwareVersion = String.format(Locale.ENGLISH, "%d.%02d", high, low);

            Log.w(TAG, "SwiftCloneChannelActiviry.java onGetSoftwareVersion(), (Integer.toHexString(irCode & 0xFF) = " + (Integer.toHexString(payload[0] & 0xFF)));

//            textViewSoftwareVersion.setText(softwareVersion);
        }

        @Override
        public void onGetMuteStatus(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetMuteStatus(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            @NonNls String muteStatus = Integer.toHexString(payload[0] & 0xFF);
            muteStatus = "0x" + muteStatus;

            // if tuner return 55, tuner is mute
            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("55"))
                muteStatus += " - Mute";
                // if tuner return AA, tuner is no mute
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("AA"))
                muteStatus += " - No Mute";
            else
                muteStatus += " - Not Defined";
//            textViewMuteStatus.setText(muteStatus);
        }

        @Override
        public void onSetVolume(byte[] payload) {
/*
            for(int i=0; i<payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onSetVolume(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            String volumeValue = Integer.toHexString(payload[0] & 0xFF);
            volumeValue = "0x" + volumeValue;
            textViewSetVolume.setText(volumeValue);
*/
        }

        @Override
        public void onGetVolume(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetVolume(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            String volume = Integer.toString(payload[0] & 0xFF);
//            textViewGetVolume.setText(volume);
        }

        @Override
        public void onGetChannel(byte[] payload) {
            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetchannel(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

//            String channel = "";
            int majorNumber = (payload[0] & 0xFF) * 16 + (payload[1] & 0xFF);
            int minorNumber = (payload[2] & 0xFF) * 16 + (payload[3] & 0xFF);
//            textViewGetChannel.setText(String.format("%d.%02d", majorNumber, minorNumber));
        }

        @Override
        public void onSetChannel() {
        }

        @Override
        public void onSetDefaultChannel() {
        }

        @Override
        public void onCloneOutDataRequire(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onCloneOutDataRequire(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mCloneOutRequireResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Ready";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Not Ready";

            Log.w(TAG, "ATSCHelperBase.java onCloneOutDataRequire(), status = " + status);

/*
			textViewCloneOutDataRequire.setText(status);
			mCloneOutData = "";
*/
        }

        @Override
        public void onGetCloneDataLength(byte[] payload) {
            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onGetCloneDataLength(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            int intDataLength = ByteBuffer.wrap(payload).getInt();
            @NonNls String strDataLength = "0x" + Integer.toHexString(payload[0] & 0xFF) + " 0x" + Integer.toHexString(payload[1] & 0xFF) + " 0x" + Integer.toHexString(payload[2] & 0xFF) + " 0x" + Integer.toHexString(payload[3] & 0xFF);
//			textViewGetCloneDataLength.setText(String.format("%s - %d", strDataLength, intDataLength));
            if (payload.length > 0)
                mCloneOutDataLength = intDataLength;
        }

        @Override
        public void onGetCloneOutData(byte[] payload) {
            // bypass first 4 bytes because they means length, not channel list data

            for (int i = 4; i < payload.length; i++) {
//                Log.w(TAG, "SwiftCloneChannelActiviry.java onGetCloneOutData(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);
//				mCloneOutData += String.format("0x%02X ", payload[i] & 0xFF); // String.format("%2s", Integer.toHexString(payload[i] & 0xFF)) + " ";
                mChannelListData.add(payload[i]);
            }

//			textViewGetCloneOutData.setText(mCloneOutData);

            mTotalReadDataLength += payload.length - 4;
//			textViewTotalDataLength.setText(String.format("%d", mTotalReadDataLength));

        }

        @Override
        public void onTest1() {
        }

        @Override
        public void onTest2() {
        }

        @Override
        public void onTest3() {
        }

        @Override
        public void onNotifyCloneIn(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onNotifyCloneIn(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mNotifyCloneInResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Ready";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Not Ready";

            Log.w(TAG, "ATSCHelperBase.java onNotifyCloneIn(), status = " + status);
        }

        @Override
        public void onNotifyCloneDataLength(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onNotifyCloneDataLength(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mNotifyCloneDataLengthResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Ready";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Not Ready";

            Log.w(TAG, "ATSCHelperBase.java onNotifyCloneDataLength(), status = " + status);
        }

        @Override
        public void onSendCloneInData(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onSendCloneInData(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mCloneInDataResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Clone In OK";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Clone In Failed";

            Log.w(TAG, "ATSCHelperBase.java onSendCloneInData(), status = " + status);
        }

        @Override
        public void onGetImportResult(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onGetImportResult(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mImportResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Import Success ";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Import Failed";

            Log.w(TAG, "ATSCHelperBase.java onGetImportResult(), status = " + status);
        }

        @Override
        public void onNotifyUpgradeFirmware(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onNotifyUpgradeFirmware(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mNotifyUpgradeFirmwareResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Ready";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Not Ready";

            Log.w(TAG, "ATSCHelperBase.java onNotifyUpgradeFirmware(), status = " + status);
        }

        @Override
        public void onGetUpgradeResult(byte[] payload) {

            for (int i = 0; i < payload.length; i++)
                Log.w(TAG, "ATSCHelperBase.java onGetUpgradeResult(), index = " + i + ", payload = " + Integer.toHexString(payload[i] & 0xFF) + ", payload length = " + payload.length);

            if (payload.length > 0)
                mUpgradeResult = payload[0];

            @NonNls String status = Integer.toString(payload[0] & 0xFF);
            status = "0x" + status;

            if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("1"))
                status += " - Upgrade Success ";
            else if (Integer.toHexString(payload[0] & 0xFF).equalsIgnoreCase("0"))
                status += " - Upgrade Failed";

            Log.w(TAG, "ATSCHelperBase.java onGetUpgradeResult(), status = " + status);
        }

        @Override
        public void onGetUSBHandler(byte[] payload) {

        }

        @Override
        public void onSetAudioChannelSwitch(byte[] payload) {

        }
    };

    protected synchronized void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    protected void start(Runnable runnable/*, int config*/) {
        if (!isRunning) {

            Log.w(TAG, "start(), call registerSwiftOnReceiveListener()");

//			SwiftManager.getInstance().registerSwiftOnReceiveListener(mSwiftCommResponse);
//			SwiftManager m = SwiftManager.getInstance();
//            m.open();
//			Log.d(TAG, "start: m: " + m);
//			m.a();
            SwiftManager.getInstance().registerSwiftOnReceiveListener(mSwiftCommResponse);
            setIsRunning(true);

            // reset all aparameters
            resetParameters();

//			openIO(config);
            inUpdate = true;
            Thread thread = new Thread(runnable);
            thread.start();
        } else {
            Log.e(TAG, "Process is running");
        }
    }

    protected void resetParameters() {
        mCloneOutRequireResult = 0;
        mNotifyCloneInResult = 0;
        mNotifyCloneDataLengthResult = 0;
        mCloneInDataResult = 0;
        mImportResult = 0;
        mCloneOutDataLength = 0;
        mChannelListData = new ArrayList<Byte>();
        mTotalReadDataLength = 0;
        mNotifyUpgradeFirmwareResult = 0;
        mUpgradeResult = 0;
    }

    protected void openIO(int config) {
/*
		io = new IOInterface(ioCallback, config);
		io.sendExtraSyncByte(true);
		io.setAdvancedLogging(true, true);
		io.setTimeout(10000, 5);
*/
    }

    protected void closeIO() {
/*
		if (io != null) {
			io.stop();
		}
		io = null;
*/
    }

    protected void stop() {
//		closeIO();
        SwiftManager.getInstance().unregisterSwiftOnReceiveListener(mSwiftCommResponse);
    }

    protected void powerOn() {
//		if (io != null) {
//			io.setTVPower(true);
//		}
    }

    protected void powerOff() {
//		if (io != null) {
//			io.setTVPower(false);
//		}
    }

    protected void sendCLOCommand() {
//		SwiftManager m = SwiftManager.getInstance();
        Log.d(TAG, "sendCLOCommand: call setSwiftCloneOutDataRequire()");
        SwiftManager.getInstance().setSwiftCloneOutDataRequire();
//		SwiftServiceWrapper.cloneOutDataRequire();
//		SwiftManager.getInstance().setSwiftCloneOutDataRequire();
    }

    protected byte getCLOCommandResult(int delayMs, int retry) {
        // 0x01: ready, 0x00: not ready
        while (mCloneOutRequireResult == 0 && retry > 0) {
            Log.d(TAG, "getCLOCommandResult: retry: " + retry + "mCloneOutRequireResult: " + mCloneOutRequireResult);
            delay(delayMs);
            wait(1000);
            retry--;
        }
        return mCloneOutRequireResult;
    }

    protected void sendCLICommand() {
        Log.d(TAG, "sendCLOCommand: call setSwiftNotifyCloneIn()");
        SwiftManager.getInstance().setSwiftNotifyCloneIn();
//		if (io != null) {
//			io.sendTVTunerControl(new byte[] {Commands.CLI.getCommand()});
//		}
    }

    protected byte getCLICommandResult(int delayMs, int retry) {
        // 0x01: ready, 0x00: not ready
        while (mNotifyCloneInResult == 0 && retry > 0) {
            Log.d(TAG, "getCLICommandResult: retry: " + retry + "mNotifyCloneInResult: " + mNotifyCloneInResult);
            delay(delayMs);
            wait(1000);
            retry--;
        }
        return mNotifyCloneInResult;
    }

    protected void sendLENCommand() {
//        SwiftManager m = SwiftManager.getInstance();
        Log.d(TAG, "sendLENCommand: call getSwiftCloneDataLength()");
        SwiftManager.getInstance().getSwiftCloneDataLength();
    }

    protected int getLENCommandResult(int delayMs, int retry) {
        while (mCloneOutDataLength <= 0 && retry > 0) {
            Log.d(TAG, "getLENCommandResult: retry: " + retry + ", mCloneOutDataLength: " + mCloneOutDataLength);
            delay(delayMs);
            wait(1000);
            retry--;
        }
        return mCloneOutDataLength;
    }

    protected void sendUPGCommand() {
        Log.d(TAG, "sendUPGCommand: call setSwiftNotifyUpgradeFirmware()");
        SwiftManager.getInstance().setSwiftNotifyUpgradeFirmware();
//		if (io != null) {
//			io.sendTVTunerControl(new byte[] {Commands.UPG.getCommand()});
//		}
    }

    protected byte getUPGCommandResult(int delayMs, int retry) {
        // 0x01: ready, 0x00: not ready
        while (mNotifyUpgradeFirmwareResult == 0 && retry > 0) {
            Log.d(TAG, "getUPGCommandResult: retry: " + retry + "mNotifyUpgradeFirmwareResult: " + mNotifyUpgradeFirmwareResult);
            delay(delayMs);
            wait(100);
            retry--;
        }
        return mNotifyUpgradeFirmwareResult;
    }

    protected void setBaudRate115200() {
        Log.w(TAG, "setBaudRate115200: begin");
        SwiftManager.getInstance().close();
        delay(100);
        SwiftManager.getInstance().open(true);
        Log.w(TAG, "setBaudRate115200: end");
//		if (io != null) {
//			io.setBaudRateTo115200();
//		}
    }

    protected void setBaudRate9600() {
        Log.w(TAG, "setBaudRate9600: begin");
        SwiftManager.getInstance().close();
        delay(100);
        SwiftManager.getInstance().open(false);
        Log.w(TAG, "setBaudRate9600: end");
//		if (io != null) {
//			io.setBaudRateTo9600();
//		}
    }

    protected void sendExportData(int startIndex, int length) {
//		SwiftManager m = SwiftManager.getInstance();
        Log.d(TAG, "sendExportData: call getSwiftCloneOutData(), startIndex = " + startIndex + ", length = " + length);
        SwiftManager.getInstance().getSwiftCloneOutData(startIndex, length);
//		wait(3000);
    }

    protected int getTotalReadDataLength() {
        return mTotalReadDataLength;
    }

    protected List<Byte> getCloneData() {
        return mChannelListData;
//		if (io != null) {
//			io.getATSCTunerCloneData();
//		}
    }

    protected void getChannelListLength() {
//		if (io != null) {
//			io.getATSCTunerChannelListLength();
//		}
    }

    protected void getTunerStatus() {
//		if (io != null) {
//			Log.d(TAG, "getATSCTunerStatus");
//			io.getATSCTunerStatus();
//		}
    }

    protected void sendDLHCommand(int length) {
        Log.d(TAG, "sendDLHCommand: call setSwiftNotifyCloneDataLength()");
        SwiftManager.getInstance().setSwiftNotifyCloneDataLength(length);
//		if (io != null) {
//			io.sendATSCTunerLength((byte)0x05, length);
//		}
    }

    protected byte getDLHCommandResult(int delayMs, int retry) {
        // 0x01: ready, 0x00: not ready
        while (mNotifyCloneDataLengthResult == 0 && retry > 0) {
            Log.d(TAG, "getDLHCommandResult: retry: " + retry + ", mNotifyCloneDataLengthResult: " + mNotifyCloneDataLengthResult);
            delay(delayMs);
            wait(100);
            retry--;
        }
        return mNotifyCloneDataLengthResult;
    }

    protected void sendCloneInData(int startIndex, int dataSize, byte[] data) {
        Log.d(TAG, "sendCloneInData: call setSwiftSendCloneInData()");
        SwiftManager.getInstance().setSwiftSendCloneInData(startIndex, dataSize, data);
//		if (io != null) {
//			io.sendATSCTunerData((byte)0x06, startIndex, dataSize, data);
//			wait(3000);
//		}
    }

    public void setCloneInDataResult(byte value) {
        mCloneInDataResult = value;
    }

    protected byte getCloneInDataResult(int delayMs, int retry) {
        // 0x01: ready, 0x00: not ready
        while (mCloneInDataResult == 0 && retry > 0) {
            Log.d(TAG, "getCloneInDataResult: retry: " + retry + ", mCloneInDataResult: " + mCloneInDataResult);
            delay(delayMs);
            wait(100);
            retry--;
        }
        return mCloneInDataResult;
    }

    protected void sendGetImportResult() {
        Log.d(TAG, "sendGetImportResult: call getSwiftImportResult()");
        SwiftManager.getInstance().getSwiftImportResult();
//		if (io != null) {
//			io.sendATSCTunerData((byte)0x06, startIndex, dataSize, data);
//			wait(3000);
//		}
    }

    protected byte getImportResult(int delayMs, int retry) {
        // 0x01: success, 0x00: failed
        while (mImportResult == 0 && retry > 0) {
            Log.d(TAG, "getImportResult: retry: " + retry + ", mImportResult: " + mImportResult);
            delay(delayMs);
            wait(300);
            retry--;
        }

        Log.d(TAG, "getImportResult: retry: " + retry + ", mImportResult: " + mImportResult);

        return mImportResult;
    }

    protected byte getUpgradeResult(int delayMs, int retry) {
        // 0x01: success, 0x00: failed
        while (mUpgradeResult == 0 && retry > 0) {
            Log.d(TAG, "getUpgradeResult: retry: " + retry + ", mUpgradeResult: " + mUpgradeResult);
            delay(delayMs);
            wait(300);
            retry--;
        }

        Log.d(TAG, "getUpgradeResult: retry: " + retry + ", mUpgradeResult: " + mUpgradeResult);

        return mUpgradeResult;
    }

    protected void onATSCTunerCallback(ATSC_STATUS status) {
/*
		if (type == ProcessType.ExportChannelList) {
			onATSCTuner.onExportStatus(status);
		} else if (type == ProcessType.ImportChannelList) {
			onATSCTuner.onImportStatus(status);
		} else if (type == ProcessType.UpgradeFirmware) {
			onATSCTuner.onUpgradeStatus(status);
		}
*/
    }

    protected void onImportProgress(int percent) {
//        if (onATSCTuner != null) {
//            onATSCTuner.onImportProgress(percent);
//        }
    }

    protected void onExportProgress(int percent) {
//        if (onATSCTuner != null) {
//            onATSCTuner.onExportProgress(percent);
//        }

//        if (onProgress != null)
//            onProgress.onProgress(percent);

    }

    protected void onUpdateProgress(int percent) {
//        if (onATSCTuner != null) {
//            onATSCTuner.onUpdateProgress(percent);
//        }
    }

    protected void onStatus(ATSC_STATUS status) {
//        if (onATSCTuner != null) {
//            onATSCTuner.onStatus(status);
//        }
    }

    protected void onParseData(byte[] data, int length) {
//        if (onATSCTuner != null) {
//            onATSCTuner.onParseData(data, length);
//        }
        TvEvent.sendEvent(data, length);
    }

    protected void notifyWait() {
        lock.lock();
        condition.signal();
        lock.unlock();
    }

    protected void wait(int time) {
        lock.lock();
        try {
            if (!condition.await(time, TimeUnit.MILLISECONDS)) {
                Log.d(TAG, "wait: waiting time elapsed." + time);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "runnable: InterruptedException: ", e);
            Thread.currentThread().interrupt();
        }
        lock.unlock();
    }

    protected void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void errorExit(ATSC_STATUS status, String message) {
        Log.d(TAG, message);
        if (type == ProcessType.UpgradeFirmware) {
            setBaudRate9600();
            delay(3000);
        }
        closeIO();
        onATSCTunerCallback(status);
    }

    protected ATSC_STATUS getCloneStatus(int retry) {
        return getStatus(100, retry);
    }

    protected ATSC_STATUS getImportStatus(int retry) {
        return getStatus(250, retry);
    }

    protected ATSC_STATUS getStatus(int retry) {
        return getStatus(500, retry);
    }

    private ATSC_STATUS getStatus(int delayMs, int retry) {
        atomicInteger.set(0);
        while (atomicInteger.get() == 0 && retry > 0) {
            delay(delayMs);
            getTunerStatus();
            wait(1000);
            retry--;
        }

        return ATSC_STATUS.values()[atomicInteger.get()];
    }

    protected int getLength(int retry) {
        atomicLength.set(0);
        while (atomicLength.get() == 0 && retry > 0) {
            delay(500);
            getChannelListLength();
            wait(1000);
            retry--;
        }

        return atomicLength.get();
    }

    protected ByteBuffer getCloneData(int retry) {
        atomicLength.set(0);
        byteBuffer = null;
        while (atomicLength.get() == 0 && retry > 0) {
            delay(500);
            getCloneData();
            wait(3000);
            retry--;
        }

        return byteBuffer;
    }

    /*
        protected ATSC_STATUS getImportResult(int retry) {
            atomicInteger.set(0);
            while (atomicInteger.get() == 0 && retry > 0) {
                delay(500);
                getTunerStatus();
                wait(1000);
                retry--;
            }

            return ATSC_STATUS.values()[atomicInteger.get()];
        }
    */
    protected ATSC_STATUS getUpgradeResult(int retry) {
        atomicInteger.set(0);
        while (atomicInteger.get() == 0 && retry > 0) {
            delay(500);
            getTunerStatus();
            wait(1000);

            retry--;
        }

        return ATSC_STATUS.values()[atomicInteger.get()];
    }
}
