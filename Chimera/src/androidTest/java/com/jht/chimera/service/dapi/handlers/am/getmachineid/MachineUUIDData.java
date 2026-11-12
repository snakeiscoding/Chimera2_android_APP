package com.jht.chimera.service.dapi.handlers.am.getmachineid;

import com.jacob.CborDecoder;

import java.io.IOException;

public class MachineUUIDData {
    private final String machineId;

    public MachineUUIDData(String machineId) {
        this.machineId = machineId;
    }

    public MachineUUIDData(CborDecoder decoder) throws IOException {
        machineId = decoder.readTextString();
        // Eat up the frame serial number.
        decoder.readTextString();
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof MachineUUIDData) {
            MachineUUIDData data = (MachineUUIDData)object;
            return data.machineId.equals(machineId);
        }
        return false;
    }

    public String machineId() {
        return machineId;
    }

    @Override
    public String toString() {
        return machineId;
    }


}
