package com.jht.chimera.test.util;

import java.util.Date;

public class CrashData {
    public enum CRASH_TYPE {
        FATAL_EXCEPTION,
        ANR,
    }

    private final CRASH_TYPE crashType;
    private final String app;
    private final Date crashTime;


    public CrashData(CRASH_TYPE crashType) {
        this(crashType, "");
    }

    public CrashData(CRASH_TYPE crashType, String app) {
        this.crashType = crashType;
        this.app = app;
        crashTime = new Date();
    }

    public CRASH_TYPE crashType() { return crashType; }
    public String app() { return app; }
    public Date crashTime() { return crashTime; }

}
