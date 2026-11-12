package com.jht.chimera.service.dapi.handlers.am.getmachineid;

import com.jht.chimera.service.dapi.handlers.DAPIRequest;
import com.jhtp.dapiservice.comm.CborCommands;

public class GetMachineUUIDRequest extends DAPIRequest {
    private final MachineUUIDData machineUUIDData;

    public GetMachineUUIDRequest(int version, MachineUUIDData machineUUIDData) {
        super(CborCommands.GetMachineId, version);
        this.machineUUIDData = machineUUIDData;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof GetMachineUUIDRequest) {
            GetMachineUUIDRequest o = (GetMachineUUIDRequest)object;
            return super.equals(object) && o.machineUUIDData.equals(machineUUIDData);
        }
        return false;
    }

    @Override public String toString() {
        return super.toString() + " uuid " + machineUUIDData;
    }

}
