package com.jht.chimera.service.hal.lib.lcb.lcbcommlib.lcbInterface;

import android.os.SystemClock;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import android.util.Log;

import com.jht.chimera.service.hal.lib.frictiontest.TreadmillOperateRxData;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill.ITreadMillHandlerCallback;
import com.jht.chimera.service.hal.lib.lcb.lcbcommlib.machineHandler.Treadmill.TreadMillHandler;

public class TreadMillInterface implements MachineInterfaceBase {
    private static final String TAG = "LCBInterface";
    private ILCBCallback serviceCallback;
    private TreadMillHandler mTmHandler;
    private boolean treadSense = false;
    private boolean reverse = false;
    private boolean currentOC = false;

    // Extend treadmill status
    private int oldStatus = 0;
    private int updateSafetyStCount = 0;
    boolean extendStateOn = false;
    boolean safetykeyDown = false;
    boolean safetykeyLock = false;

    //P+ MCB function
    boolean zeroRPM = false;
    private boolean oldOCdetectStatus = false;
    private boolean autoOCdetect = false;
    private boolean safetykeySwitchLock = false;

    //Skip equal incline value
    private int currentIncline = 0;
    private int lastSetInclineADC = 0;

    private boolean mOverSpeedEnable = false;
    private OverSpeedRule[] overSpeedRules = new OverSpeedRule[]{
                    new OverSpeedRule(TreadMillInterface.OverSpeedRule.OverSpeedType.SPEED_DIFF_STOP ,5.0f,2000,5000),
                    new OverSpeedRule(TreadMillInterface.OverSpeedRule.OverSpeedType.SPEED_DIFF_REDUCE_INCLINE_STOP ,2.0f,1000,2000,3.0f)};

    private int mOverSpeedTargetRpm;
    private int mOverSpeedCurrentRpm;
    
    // Roller guard
    public static final int TM_ROLLER_GUARD_ERROR = 0x02C4;
    private volatile boolean wasRollerGuardTriggered = false;

    // Hybrid TM IR-EStop
    private volatile boolean wasHybridTmIrEStopTriggered = false;
    private volatile boolean pendingIrEStopCheck = false;

    public TreadMillInterface(final ILCBCallback callback) {
        mTmHandler = TreadMillHandler.getInstance();
        this.serviceCallback = callback;
        mTmHandler.setTreadMillHandlerCallback(mTmHandlerCallback);
        mTmHandler.initialTreadMill();

        //Open extend status command
        mTmHandler.setAutoReplyTreadmillExtStatus(true);
    }

    public void triggerRollerGuard() { wasRollerGuardTriggered = true; }
    public boolean isRollerGuardTriggered() { return wasRollerGuardTriggered; }

    public void triggerHybridTmIrEStopCheck() {
        pendingIrEStopCheck = true;
        mTmHandler.getHybridTMIrAddrIrStatus();
    }
    public boolean isHybridTmIrEStopTriggered() { return wasHybridTmIrEStopTriggered; }

    private ITreadMillHandlerCallback mTmHandlerCallback = new ITreadMillHandlerCallback() {

        @Override
        public void onTreadMillRPM(int intMotorRPM) {
            serviceCallback.onRpm(intMotorRPM);
            if(mOverSpeedEnable){
                onOSrpm(intMotorRPM);
                checkOverSpeed();
            }
        }

        @Override
        public void onTreadMillInclineLoc(int intMotorInclineLoc) {
            currentIncline = intMotorInclineLoc;
            serviceCallback.onInclineADC(intMotorInclineLoc);
            if(mOverSpeedEnable){ onOSadc(intMotorInclineLoc); }
        }

        @Override
        public void onTreadMillInUsed(byte byteStatus) {
            serviceCallback.onPersonInPlace(byteStatus == (byte) 0xFF);//0xFF: used , 0x55: non used
            mTmHandler.setAutoReplyTmSpeedUpInUsed(byteStatus == (byte) 0x55);// non used , cmd speed up

        }

        @Override
        public void onTreadMillMotorInfo(byte[] bytesMotorInfo) {
            serviceCallback.onTreadMillMotorInfo(bytesMotorInfo);

        }

        @Override
        public void onTreadmillExtStatus(int intTreadmillExtStatus) {

            //State change, update status
            if (oldStatus != intTreadmillExtStatus) {
                onExtStatusParser(intTreadmillExtStatus);
                onExtStatusHnadler();
				onAutoOCExtStatusHnadler();
                oldStatus = intTreadmillExtStatus;
            }

            //For unlock MCB success but sometime get 02B1 safety lock
            //Update LCB safetykey state every 400ms*15
            if((updateSafetyStCount++) == 15) {
                onExtStatusHnadler();
                updateSafetyStCount = 0;
            }
        }

        @Override
        public void onTreadMillOperate(TreadmillOperateRxData data) {
            serviceCallback.onTreadMillOperate(data);
        }

        @Override
        public void onTreadMillCadence(byte[] bytesCadenceData) {
            serviceCallback.onTreadMillCadence(bytesCadenceData);
        }

        @Override
        public void onTreadMillServiceSwitch(boolean isBit4Enabled) {
            // This is the status byte bit4 TM safety key handler
            if (!isBit4Enabled && wasRollerGuardTriggered) {
                Log.d(TAG, "Roller guard reset logic from status byte bit4 reset");
                wasRollerGuardTriggered = false;

                // Releases through LCBService.onTmRollerGuard, which clears the MCB lock state,
                // sends the unlock the LCB needs to stop repeating 0x02C4, and refreshes the
                // safety key bitmap.
                serviceCallback.onTmRollerGuard(false);
            }
        }

        @Override
        public void onTreadMillOneShotRPM(int intTreadMillRPM) {
            serviceCallback.onOneShotRpm(intTreadMillRPM);
        }

        @Override
        public void onTreadMillLcbSetRpmValue(int intSetRpm) {
            //setTargetRpm(intSetRpm);
            if(mOverSpeedEnable){
                mOverSpeedTargetRpm = intSetRpm;
            }
        }

        @Override
        public void onTreadMillLcbSetAdcValue(int intSetAdc) {
            if(mOverSpeedEnable){ setTargetAdc(intSetAdc); }
        }

        @Override
        public void onHybridTmIrAddrVersion(int intIrAddrVersion) {
            serviceCallback.onHybridTmIrAddrVersion(intIrAddrVersion);
        }

        @Override
        public void onHybridTmIrAddrIrStatus(int intIrAddrIrStatus) {
            if (pendingIrEStopCheck) {
                // 02B2-triggered IR status query response: determine trigger type
                pendingIrEStopCheck = false;
                if (intIrAddrIrStatus == 0x01) {
                    // IR blocked - trigger Hybrid TM IR-EStop
                    Log.d(TAG, "Hybrid TM IR-EStop triggered, intIrAddrIrStatus=0x01");
                    wasHybridTmIrEStopTriggered = true;
                    serviceCallback.onHybridTmIrAddrIrEStop(true);
                } else if (intIrAddrIrStatus == 0x00) {
                    // Not IR blocked - trigger original EStop
                    Log.d(TAG, "Normal EStop, intIrAddrIrStatus=0x00");
                    serviceCallback.onLCBSafetykeyLockedCode(true);
                }
            } else if (wasHybridTmIrEStopTriggered && intIrAddrIrStatus == 0x00) {
                // Auto-reply recovery: IR blockage cleared while IR-EStop is active
                Log.d(TAG, "Hybrid TM IR-EStop recovery, intIrAddrIrStatus=0x00");
                wasHybridTmIrEStopTriggered = false;
                serviceCallback.onHybridTmIrAddrIrEStop(false);
                serviceCallback.onTreadmillStartUnlock();
            }
            serviceCallback.onHybridTmIrAddrIrStatus(intIrAddrIrStatus);
        }

        @Override
        public void onHybridTmIrAddrErpRtnByte(byte byteRtn) {
            serviceCallback.onHybridTmIrAddrErpRtnByte(byteRtn);
        }

        @Override
        public void onHybridTmIrAddrEnterErpMode(boolean isEnterSuccess) {
            serviceCallback.onHybridTmIrAddrEnterErpMode(isEnterSuccess);
        }

        @Override
        public void onHybridTmIrAddrLeaveErpMode(boolean isLeaveSuccess) {
            serviceCallback.onHybridTmIrAddrLeaveErpMode(isLeaveSuccess);
        }
    };

    @Override
    public void startReplyData(boolean start) {
        //start receive lcb workout data
        mTmHandler.setAutoReplyTmRpm(start);
        mTmHandler.setAutoReplyTmIncline(start);
        mTmHandler.setAutoReplyTmCadence(start && FrameConfigManager.getInstance().getFrameConfig().HAS_CADENCE);
    }

    @Override
    public void unInitial() {
        mTmHandler.unsetTreadMillHandlerCallback();
        mTmHandler.unInitialTreadMill();
        mTmHandler = null;
    }

    //Set treadmill incline reverse
    public void setTreadmillInclineReverse(boolean reverse) { this.reverse = reverse;}


    //set TreadMill RPM
    public void setTreadMillRPM(int intValue) { setTreadMillRPM(intValue, false); }
    public void setTreadMillRPM(int intValue, boolean highPriority) {
        //MCB lock (E-STOP) don't send rpm, it will make the running belt stop immediately
        if(checkSafetyState())
            return;

        mTmHandler.setTreadMillRPM(intValue, highPriority);
    }

    public void setTreadmillStopMachine() {
        //MCB lock (E-STOP) don't send rpm, it will make the running belt stop immediately
        if(checkSafetyState())
            return;

        mTmHandler.setTreadMillRPM(0);
    }

    public void setTreadmillDetectMachine(boolean on) { //Sam modify
        mTmHandler.setAutoReplyTmInUsed(on && treadSense);
        mTmHandler.setAutoReplyTmMotorInfo(on && currentOC);
    }

    //set TreadMill Incline
    public void setTreadMillIncline(int intLoc) {

        int inclineDifference = ((currentIncline - intLoc) < 0) ? -(currentIncline - intLoc) : currentIncline - intLoc;

        //In case set the same incline, incline sometimes move a little bit
        if((lastSetInclineADC == intLoc) && (inclineDifference < 128)) {
            Log.i(TAG, "ACTION_SET_INCLINE_FLOAT : Don't send the same incline ADC to LCB.");
            return;
        }else
            lastSetInclineADC = intLoc;

        /* The lift motor pinout for the Performance TM will be changed to match the Endurance TM.
          This will allow both lift motors to travel the same direction. */

        if(reverse) intLoc |= 0x8000;
        mTmHandler.setTreadMillIncline(intLoc);
    }

    public void setTreadmillOperate(int treadmillRpm, int inclineLoc) {
        //TODO: Test that safety key works with this command
        //MCB lock (E-STOP) don't send rpm, it will make the running belt stop immediately
        if(checkSafetyState()) return;

        if(reverse) inclineLoc |= 0x8000;
        mTmHandler.sendTreadmillOperate(treadmillRpm, inclineLoc);
    }

    //set TreadMill Incline Action for calibration
    public void setTreadMillInclineAction(byte byteValue) {
        mTmHandler.setTreadMillInclineAction(byteValue);
    }

    //Person detection
    public void setTreadMillNoPersonDetect(boolean enable) {
        treadSense = enable;
    }
    //DC bus OC detection
    public void setTreadMillCurrentOcDetect(boolean enable) {
        currentOC = enable;
    }

    //set TreadMill Motor Type
    public void setTreadMillMotorType(int type) {

        if(type > 0)
            mTmHandler.setTreadMillMotorType(type);
    }

    //Get safety switch state from IO
    public void setTreadmillSafetyLock(boolean lock) { safetykeySwitchLock = lock; }

    //for calibration update incline date
    public void setAutoReplyTmIncline(boolean enable) {
        mTmHandler.setAutoReplyTmIncline(enable);
    }

    //get TreadMill One Shot Reply Rpm
    public void getTreadMillOneShotReplyRpm() {
        mTmHandler.getTreadMillOneShotReplyRpm();
    }

    //set TreadMill Ecb Action F1 Ecb board Motor control
    public void setTreadMillEcbAction(byte byteValue) {
        mTmHandler.setTreadMillEcbAction(byteValue);
    }

    //get TreadMill OneShot Reply Incline Adc
    public void getTreadMillOneShotReplyInclineAdc() {
        mTmHandler.getTreadMillOneShotReplyInclineAdc();
    }

    public boolean getExtStatusOn() { return extendStateOn;}

    public void setExtStatusOn(boolean on) {

        extendStateOn = on;
        //Open extend status command
        mTmHandler.setAutoReplyTreadmillExtStatus(on);
    }

    private void onExtStatusParser(int st) {
        /*
        For treadmill byte define (status2)
        Bit 0 : MCB safety trigger
        Bit 1 : MCB lock state
        Bit 2 : MCB zero RPM
        Bit 3 : MCB auto OC protection
        */
        byte status[] = {(byte) (st & 0xFF), (byte) ((st >> 8) & 0xFF)};

        safetykeyDown = (status[0] & 0x01) != 0;
        safetykeyLock = (status[0] & 0x02) != 0;
        zeroRPM = (status[0] & 0x04) == 0;
		autoOCdetect = (status[0] & 0x08) != 0;

        Log.i(TAG, "MCB auto OC detect : Extend CMD (0x97) status : "+st +" ,autoOCdetect : " + autoOCdetect );
        Log.i(TAG, "LCB Safety : Extend CMD (0x97) status : "+st +" ,Circuit : " + safetykeyDown + " ,Lock : " + safetykeyLock+",zeroRPM: "+zeroRPM);
    }

    private void onExtStatusHnadler() {

        if (safetykeyDown) {
            serviceCallback.onTreadmillSafetykeyLockState(true);
        } else if (safetykeyLock) //only lock, for quick safetykey down and up
        {
            serviceCallback.onTreadmillSafetykeyLockState(true);
            //only lock, need to send unlock command
            serviceCallback.onTreadmillStartUnlock();
        } else {
            //for lcb auto unlock, send safetykey release
            serviceCallback.onTreadmillSafetykeyLockState(false);
        }
    }

    private void onAutoOCExtStatusHnadler() {
        if (oldOCdetectStatus != autoOCdetect) {
			oldOCdetectStatus = autoOCdetect;
			serviceCallback.onTMautoOCState(autoOCdetect);
        }
    }

    private boolean checkSafetyState() { return safetykeyDown; }

    /**
     * set Treadmill Auto Ramp Up And Last Rpm Value
     */
    public void setTreadmillAutoRampUpAndLastRpmValue(boolean on,int rampUpStage,int rampDownStage) {
        mTmHandler.setAutoReplyTmRampUpAndLastRpmValue(on,rampUpStage,rampDownStage);
    }

    /**
     * initial Treadmill Over Speed
     */
    public void initTreadmillOverSpeed(boolean overSpeedEnable) {
        mOverSpeedEnable = overSpeedEnable;

        for (OverSpeedRule r : overSpeedRules) {
            r.ruleInitial(serviceCallback);
        }
    }

    /**
     * check OverSpeed main function set elapsed Real time trigger onOverSpeed
     */
    public synchronized void checkOverSpeed() {
        long current = SystemClock.elapsedRealtime();

        for (OverSpeedRule r : overSpeedRules) {

            if (r.isOverSpeed(mOverSpeedTargetRpm, mOverSpeedCurrentRpm)) {
                if (r.getElapsedTimeStamp() == 0) {
                    r.setElapsedTimeStamp(current);
                } else if (current - r.getElapsedTimeStamp() > r.getExceedTimeout()) {
                    r.setElapsedTimeStamp(current + r.getDebounceTime());
                    if (r.onOverSpeed())
                        return;
                }
            } else if (current > r.getElapsedTimeStamp())
                r.setElapsedTimeStamp(0);
        }
    }

    /**
     * setTargetAdc set OverSpeedRule Target Incline Adc
     */
    public void setTargetAdc(int adc) {

        for (OverSpeedRule r : overSpeedRules) {
            r.onTargetInclineAdc(adc);
        }
    }

    /**
     * set callback mOverSpeedCurrentRpm
     */
    public void onOSrpm(int rpm) {
        mOverSpeedCurrentRpm = rpm;
        if( mOverSpeedCurrentRpm == 0){
            for (OverSpeedRule r : overSpeedRules) {
                r.setElapsedTimeStamp(0);
            }
        }
    }

    /**
     * set callback mOverSpeedCurrentAdc
     */
    public void onOSadc(int adc) {
        for (OverSpeedRule r : overSpeedRules) {
            r.onCurrentInclineAdc(adc);
        }
    }

    /**
     * TreadMill OverSpeed Rule class
     */
    public static class OverSpeedRule {
        private static final String TAG = "OverSpeedRule";
        private OverSpeedType mOverSpeedType = OverSpeedType.NONE;
        private float mOverSpeedDiffMph = 0;
        private int mOverSpeedDiffRpm = 0;
        private int mOverSpeedExceedTimeout = 0;
        private int mOverSpeedDebounceTime = 0;
        private float mOverSpeedReduceIncline = 0;
        long timeStamp;
        private int mOverSpeedTargetAdc = 0;
        private int mOverSpeedCurrentAdc = 0;
        private int mInclineAdcTolerance = 0;
        /* The minimum unit of incline is 0.5, and the middle value is used to determine the ADC's tolerance. */
        private static final float OVER_SPEED_INCLINE_TOLERANCE = 0.25f;
        private float mOverSpeedTargetIncline = 0;
        private float mOverSpeedCurrentIncline = 0;
        private ILCBCallback mServiceCallback;
        /*
        For TM Over Speed define
        byte 0x00 : None
        byte 0x01 : Speed difference machine stops
        byte 0x02 : Speed difference reduce incline
        byte 0x03 : Speed difference incline 0 , machine stops
        */
        public enum OverSpeedType {
            SPEED_DIFF_STOP(0x01),
            SPEED_DIFF_REDUCE_INCLINE_STOP(0x02),
            NONE(0x00);

            private final byte type;

            OverSpeedType(int type) {
                this.type = (byte) type;
            }

            public byte getType() {
                return this.type;
            }
        }

        /**
         * OverSpeedRule class
         * OverSpeedType : SPEED_DIFF_STOP , 4 param
         *
         * @param bOSType : SPEED_DIFF_STOP ,OverSpeed difference Mph ,ExceedTimeout ,DebounceTime
         */
        public OverSpeedRule(OverSpeedType bOSType, float fOSdifferenceMph, int iOSExceedTimeout, int iOSDebounceTime) {
            this.mOverSpeedType = bOSType;
            this.mOverSpeedDiffMph = fOSdifferenceMph;
            this.mOverSpeedExceedTimeout = iOSExceedTimeout;
            this.mOverSpeedDebounceTime = iOSDebounceTime;
        }

        /**
         * OverSpeedRule class
         * OverSpeedType : SPEED_DIFF_REDUCE_INCLINE_STOP , 5 param
         *
         * @param bOSType : SPEED_DIFF_REDUCE_INCLINE_STOP ,OverSpeed difference Mph ,ExceedTimeout ,DebounceTime ,reduceIncline
         */
        public OverSpeedRule(OverSpeedType bOSType, float fOSdifferenceMph, int iOSExceedTimeout, int iOSDebounceTime, float fReduceIncline) {
            this.mOverSpeedType = bOSType;
            this.mOverSpeedDiffMph = fOSdifferenceMph;
            this.mOverSpeedExceedTimeout = iOSExceedTimeout;
            this.mOverSpeedDebounceTime = iOSDebounceTime;
            this.mOverSpeedReduceIncline = fReduceIncline;
        }

        /**
         * SPEED_DIFF_STOP/SPEED_DIFF_REDUCE_INCLINE_STOP trigger then ServiceCallback
         * SPEED_DIFF_STOP : onTreadmillOSstopWorkout
         * SPEED_DIFF_REDUCE_INCLINE_STOP : onTreadmillOSstopWorkout / onTreadmillOSreduceIncline
         */
        public boolean onOverSpeed() {
            switch (mOverSpeedType) {
                case SPEED_DIFF_STOP: {
                    mServiceCallback.onTreadmillOSstopWorkout(OverSpeedType.SPEED_DIFF_STOP);
                    return true;
                }
                case SPEED_DIFF_REDUCE_INCLINE_STOP: {
                    float targetIncline = mOverSpeedTargetIncline;
                    if (targetIncline == 0) {

                        mServiceCallback.onTreadmillOSstopWorkout(OverSpeedType.SPEED_DIFF_REDUCE_INCLINE_STOP);
                        return true;
                    } else if (mOverSpeedCurrentAdc < (mOverSpeedTargetAdc + mInclineAdcTolerance)) {
                        mServiceCallback.onTreadmillOSreduceIncline(OverSpeedType.SPEED_DIFF_REDUCE_INCLINE_STOP, mOverSpeedReduceIncline);
                        return true;
                    }
                    return false;
                }
            }

            return false;
        }

        /**
         * each OverSpeedRule according to the numerical initialization
         *
         * @param serviceCallback : serviceCallback
         */

        public void ruleInitial(ILCBCallback serviceCallback) {
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            mOverSpeedDiffRpm = (int) frameConfig.speedMph.interpolate(mOverSpeedDiffMph, frameConfig.speedRPM);
            mServiceCallback = serviceCallback;

            /* OverSpeed incline adc tolerance trigger */
            float diffInclineAdc = (float)(frameConfig.INCLINE_ADC_MAX - frameConfig.INCLINE_ADC_MIN);
            float diffInclineModel = frameConfig.incline.getMaxModel() - frameConfig.incline.getMinModel();
            mInclineAdcTolerance = (int) ((diffInclineAdc / diffInclineModel) * OVER_SPEED_INCLINE_TOLERANCE);
        }

        /**
         * according to the rules check is OverSpeed
         *
         * @param targetRpm , currentRpm
         * @return is OverSpeed true/false.
         */
        public boolean isOverSpeed(int targetRpm, int currentRpm) {
            boolean rtnIsOverSpeed = false;
            switch (mOverSpeedType) {
                case SPEED_DIFF_STOP: {
                    rtnIsOverSpeed = currentRpm - targetRpm > mOverSpeedDiffRpm && targetRpm > 0;

                    return rtnIsOverSpeed;
                }
                case SPEED_DIFF_REDUCE_INCLINE_STOP: {
                    rtnIsOverSpeed = currentRpm - targetRpm >= mOverSpeedDiffRpm && targetRpm > 0;

                    return rtnIsOverSpeed;
                }
            }

            return false;
        }

        public long getExceedTimeout() {
            return mOverSpeedExceedTimeout;
        }


        public long getElapsedTimeStamp() {
            return timeStamp;
        }


        public void setElapsedTimeStamp(long time) {
            timeStamp = time;
        }


        public long getDebounceTime() {
            return mOverSpeedDebounceTime;
        }

        /**
         * set mOverSpeedTargetAdc
         *
         */
        public void onTargetInclineAdc(int targetAdc) {
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();

            mOverSpeedTargetAdc = targetAdc;


            mOverSpeedTargetIncline = (float) frameConfig.inclineAdc.interpolate(mOverSpeedTargetAdc, frameConfig.incline);
        }
        /**
         * set mOverSpeedCurrentAdc
         *
         */
        public void onCurrentInclineAdc(int currentAdc) {
            FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
            mOverSpeedCurrentAdc = currentAdc;
            mOverSpeedCurrentIncline = (float) frameConfig.inclineAdc.interpolate(mOverSpeedCurrentAdc, frameConfig.incline);
        }
    }

    //get HybridTM Ir Addr Version
    public void getHybridTMIrAddrVersion() {
        mTmHandler.getHybridTMIrAddrVersion();
    }

//20260413 ESDV-1184 Vortex-Chimera modify Hybrid TM IR-EStop no send 02B2 & 02B1 error code to UI.>
    //get HybridTM Ir Addr Ir Status
    public void getHybridTMIrAddrIrStatus() {
        mTmHandler.getHybridTMIrAddrIrStatus();
    }
//<20260413 ESDV-1184 Vortex-Chimera modify Hybrid TM IR-EStop no send 02B2 & 02B1 error code to UI.

    //set Hybrid TM Ir Addr auto detect Ir Status
    public void setHybridTMIrAddrIrDetection(boolean bEnable) {
        mTmHandler.setAutoReplyIrAddrIrStatus(bEnable);
    }

    //set HybridTM Ir Addr enter ERP
    public void setHybridTMIrAddrEnterErpMode() {
        mTmHandler.setHybridTMIrAddrEnterErpMode();
    }

    //set HybridTM Ir Addr leave ERP
    public void setHybridTMIrAddrLeaveErpMode() {
        mTmHandler.setHybridTMIrAddrLeaveErpMode();
    }

}
