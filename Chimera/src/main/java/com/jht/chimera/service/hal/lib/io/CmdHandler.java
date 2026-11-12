package com.jht.chimera.service.hal.lib.io;

import android.net.Uri;
import android.util.Log;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.service.hal.lib.io.iocommlib.IRMCUPacketManager;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPacketManager;
import com.jht.chimera.service.hal.lib.io.iocommlib.McuPassthroughDevice;
import com.jht.chimera.service.hal.lib.io.iocommlib.PowerType;

public class CmdHandler {
    public final String TAG = "CmdHandler";
    private PeriodicTimer fanStateTimer;

    public void updateIO(Uri uri, String target) {
        if (IRMCUPacketManager.UPDATE_TARGET.equals(target)) {
            IRMCUPacketManager.getInstance().updateIR(uri);
        } else if (McuPacketManager.UPDATE_TARGET.equals(target)) {
            McuPacketManager.getInstance().updateIO(uri);
        } else {
            Log.e(TAG, "Unknown IO update target " + target);
        }
    }

    public void beep() {
        McuPacketManager.getInstance().setTone(1);
    }

    public void enterERP() {
        McuPacketManager.getInstance().enterERP();
    }

    public void enterSuspend() {
        McuPacketManager.getInstance().enterSuspend();
    }

    public void enterReboot() {
        McuPacketManager.getInstance().enterReboot();
    }

    public void getOperationMode(){
        McuPacketManager.getInstance().getOperationMode();
    }

    public void getVersion() {
        McuPacketManager.getInstance().getVersion();
    }

    public void getSafeKeyState() {
        McuPacketManager.getInstance().getSafeKeyState();
        //Log.d("SafeKey","=== Get Safe Key state ===");
    }

    public void getDevicePowerState(byte source) {
        McuPacketManager.getInstance().getDevicePowerState(source);
    }

    public void getExternalPowerState() {
        McuPacketManager.getInstance().getExternalPowerState();
    }

    public void powerMonitor(boolean enable, int time){
        McuPacketManager.getInstance().powerMonitor(enable, time);
    }

    public void getPowerState(byte source){
        McuPacketManager.getInstance().getPowerState(source);
    }

    public void getExtendPowerError(){
        McuPacketManager.getInstance().getExtendPowerError();
    }

    public void getBoot() {
        McuPacketManager.getInstance().getBoot();
    }

    public void setERPLED(int power) {
        McuPacketManager.getInstance().setERPLED(power);
    }

    public void setLEDColor(int LEDNumber, int color){
        McuPacketManager.getInstance().setLEDColor(LEDNumber,color);
    }

    public void getLcbType() {
        McuPacketManager.getInstance().getLcbType();
    }

    private void getFanSpeed(){
        // Don't poll the fan during an IO firmware update; it would interleave with
        // and disrupt the update packet timing on the shared serial port.
        if (McuPacketManager.getInstance().isUpdating()) {
            return;
        }
        McuPacketManager.getInstance().getFanSpeed();
    }

    public void getPowerMonitorState() {
        McuPacketManager.getInstance().getPowerMonitorState();
    }

    public void checkExtendPower(){
        Log.d(TAG, "checkExtendPower");
        setPower(PowerType.ext_14V.getID(), McuPacketManager.enablePower);
        new JHTTimer(1000,() -> {
//            getDevicePowerState(PowerType.ext_14V.getID());
            getPowerState((byte)0x00);
        }
        ).start();

    }

    public void setKeyDebounceTime(int time, int time2, int gapTime){
        McuPacketManager.getInstance().setKeyDebounceTime(time, time2, gapTime);
    }

    public void setWatchdog(int timeout) {
        McuPacketManager.getInstance().setWatchdog(timeout);
    }

    public void sendKeyEvent(boolean isUp, int key_u16) {
        McuPacketManager.getInstance().sendKeyEvent(isUp, key_u16);
    }

    //public void refresh() {
    //    McuPacketManager.getInstance().refresh();
    //}

    public void setTone(int type) {
        if (type >= 0 && type <= 2)
            McuPacketManager.getInstance().setTone(type);
    }

    public void setFanSpeed(int speed) {
        switch (speed) {
            case 0:
                McuPacketManager.getInstance().setFanSpeed(0);
                break;
            case 1:
                McuPacketManager.getInstance().setFanSpeed(50);
                break;
            case 2:
                McuPacketManager.getInstance().setFanSpeed(75);
                break;
            case 3:
                McuPacketManager.getInstance().setFanSpeed(90);
                break;
            case 4:
                McuPacketManager.getInstance().setFanSpeed(100);
                break;
            default:
                McuPacketManager.getInstance().setFanSpeed(speed);
                break;
        }
        if (speed == 0){
            if(fanStateTimer != null) {
                fanStateTimer.stop();
                fanStateTimer = null;
            }
        }
        else {
            if (fanStateTimer == null) {
                fanStateTimer = new PeriodicTimer(5000, 5000, this::getFanSpeed);
                fanStateTimer.start();
            }
        }

    }

    /**
     * Specify the power setting to specific location
     *
     * @param source    A byte to specify the location of the power setting
     * @param _switch      0x00 : Disable, 0x01: Enable, 0x02: Power off / ERP on
     */
    public void setPower(byte source, int _switch) {
        Log.d(TAG, "Power control: " + PowerType.format(source) + " --> " + _switch);
        McuPacketManager.getInstance().setPower(source, _switch);
    }

    public void setRS485Communication(boolean enable) {
        McuPacketManager.getInstance().setRS485Communication(enable);
    }

    public void setQiVoltage(int voltage){
        McuPacketManager.getInstance().setQiVoltage(voltage);
    }

    public void setDebug(boolean enable, int level) {
        if (level >= 0 && level <= 3)
            McuPacketManager.getInstance().setDebug(enable, level);
    }

    /*
    public void setCECconfig(int address) {
        byte[] configArray = ByteBuffer.allocate(4).putInt(address).array();
        McuPacketManager.getInstance().setCECconfig(configArray[0]);
    }
     */

    public void sendCECcommand(byte[] command) {
        McuPacketManager.getInstance().sendCECcommand(command);
    }

    public void getCECResponse() {
        McuPacketManager.getInstance().getCECresponse();
    }

    public void sendIRcommand(byte[] code) {
        if(ProductConfig.hasSeparateIoForIr()) {
            Log.d(TAG, "sendIRcommand(lcgui): ");
            IRMCUPacketManager.getInstance().sendIRcommand(code);
        }else
            McuPacketManager.getInstance().sendIRcommand(code);
    }

    public void sendPassthrough(McuPassthroughDevice device, byte[] payload) {
        McuPacketManager.getInstance().writeToPassthrough(device, payload);
    }

    public void sendIRFrequency(short frequency) {
        McuPacketManager.getInstance().sendIRFrequency(frequency);
    }

    public void reboot(int delayTime) {
        McuPacketManager.getInstance().reboot(delayTime);
    }

    public void sendCanTx(byte[] data) {
        McuPacketManager.getInstance().sendCanTx(data);
    }
}
