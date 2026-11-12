package com.jht.chimera.service.dapi.handlers;

import android.util.Log;

import com.jhtp.dapiservice.comm.CborCommands;

public class DAPIRequest {
    private static final String TAG = "DAPIRequest";
    private final CborCommands command;
    private final int version;

    public DAPIRequest(CborCommands command, int version) {
        this.command = command;
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof DAPIRequest) {
            DAPIRequest o = (DAPIRequest)object;
            Log.d(TAG, "checking request " + o.command.Value + " " + command.Value + " " + o.version + " " + version);
            return o.command.Value == command.Value && o.version == version;
        }
        return false;
    }

    @Override public String toString() {
        return "command: " + command + " version: " + version;
    }

}
