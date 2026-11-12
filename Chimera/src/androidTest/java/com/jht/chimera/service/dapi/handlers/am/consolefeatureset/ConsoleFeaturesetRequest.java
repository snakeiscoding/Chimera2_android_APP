package com.jht.chimera.service.dapi.handlers.am.consolefeatureset;

import com.jht.chimera.service.dapi.handlers.DAPIRequest;
import com.jhtp.dapiservice.comm.CborCommands;

public class ConsoleFeaturesetRequest extends DAPIRequest {
    private final ConsoleFeatureSetDataV1 consoleFeatureSetData;

    public ConsoleFeaturesetRequest(int version, ConsoleFeatureSetDataV1 consoleFeatureSetData) {
        super(CborCommands.ConsoleFeatureSet, version);
        this.consoleFeatureSetData = consoleFeatureSetData;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof ConsoleFeaturesetRequest) {
            ConsoleFeaturesetRequest o = (ConsoleFeaturesetRequest)object;
            return super.equals(object) && o.consoleFeatureSetData.equals(consoleFeatureSetData);
        }
        return false;
    }

}
