package com.jht.chimera.service.hal.lib.hardware;

import java.util.HashMap;

class ParkerHRDriverSim implements IDriver {
    final private HashMap<String, Integer> deviceMap = new HashMap<>();
    static final private int HAND_GRIP_FD = 1;
    static final private int POLAR_5KHZ_FD = 2;

    public int demoPolarHeartRate = 0;
    public int demoHandgripHeartRate = 0;

    ParkerHRDriverSim() {
        deviceMap.put("/dev/hr_heart", HAND_GRIP_FD);
        deviceMap.put("/dev/wl_heart", POLAR_5KHZ_FD);
    }

    // mFd = Driver.safeOpen(devName);
    // Driver.safeClose(mFd);
    /**
     * int ret = Driver.waitForRead(mTaskHandGrip.getFd(), 100);
     *                     if (ret > 0) {
     *                         mHeartRateHandGrip = Driver.readInt(mTaskHandGrip.getFd()); // ? it is strange this uses read() return instead of buf read
     *
     * int ret = Driver.waitForRead(mTaskPolar5kz.getFd(), 100);
     *                     if (ret > 0) {
     *                         ret = Driver.read(mTaskPolar5kz.getFd(), data);
     *                         // can we rely it always giving us 2 bytes or do we need to handle partials?
     *                         if (ret == 2) {
     *                             //Log.d(TAG, "polar: " + String.valueOf(data[0] & 0xff) + ", " + String.valueOf(data[1] & 0xff));
     *                             mHeartRatePolar5kz = data[0]  & 0xff;
     *                         }
     *                     }
     *
     mTaskHandGrip.start("/dev/hr_heart", mRunHand); //NON-NLS
     mTaskPolar5kz.start("/dev/wl_heart", mRunPolar); //NON-NLS
     */

    @Override
    public int safeOpen(String name) {
        Integer fd = deviceMap.get(name);
        return fd == null ? -1 : fd;
    }

    @Override
    public void safeClose(int fd) {
        // Do nothing.
    }

    @Override
    public int readInt(int fd) {
        if(fd == HAND_GRIP_FD) {
            return demoHandgripHeartRate;
        }
        return -1;
    }

    @Override
    public int read(int fd, byte[] data) {
        if(fd == POLAR_5KHZ_FD) {
            if (data.length < 2) {
                return -1;
            }
            data[0] = (byte) demoPolarHeartRate;
            return 2;
        }
        return -1;
    }

    @Override
    public int write(int fd, byte[] data) {
        return -1;
    }

    @Override
    public int waitForRead(int fd, int timeoutMs) {
        return 1;
    }

}
