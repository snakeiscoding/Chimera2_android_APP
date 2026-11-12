package com.jht.chimera.service.dapi.handlers.am.consolefeatureset;

import com.jacob.CborDecoder;

import java.io.IOException;

public class ConsoleFeatureSetDataV2 extends ConsoleFeatureSetDataV1{
    private final boolean ifit;

    public ConsoleFeatureSetDataV2(String machineId, boolean assetManagement, boolean workoutTracking, boolean ifit) {
        super(machineId, assetManagement, workoutTracking);
        this.ifit = ifit;
    }

    public ConsoleFeatureSetDataV2(CborDecoder decoder) throws IOException {
        super(decoder);
        ifit = decoder.readInt() == 1;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof ConsoleFeatureSetDataV2) {
            ConsoleFeatureSetDataV2 data = (ConsoleFeatureSetDataV2)object;
            return super.equals(object) && data.ifit == ifit;
        }
        return false;
    }

}
