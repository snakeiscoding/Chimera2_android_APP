package com.jht.chimera.service.dapi.handlers.am.getmachinestats;

import com.jht.chimera.service.dapi.handlers.DAPIRequest;
import com.jhtp.dapiservice.comm.CborCommands;

public class GetMachineStatsRequest extends DAPIRequest {
    private final String machineUUID;

    public GetMachineStatsRequest(int version, String machineUUID) {
        super(CborCommands.GetMachineStats, version);
        this.machineUUID = machineUUID;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof GetMachineStatsRequest) {
            GetMachineStatsRequest o = (GetMachineStatsRequest)object;
            return super.equals(object) && o.machineUUID.equals(machineUUID);
        }
        return false;
    }

}
