package com.jht.chimera.service.dapi.handlers.am.consolefeatureset;

import com.jacob.CborDecoder;

import java.io.IOException;

public class ConsoleFeatureSetDataV1 {
    private final String machineId;
    private final boolean workoutTracking;
    private final boolean assetManagement;

    public ConsoleFeatureSetDataV1(String machineId, boolean assetManagement, boolean workoutTracking) {
        this.assetManagement = assetManagement;
        this.workoutTracking = workoutTracking;
        this.machineId = machineId;
    }

    public ConsoleFeatureSetDataV1(CborDecoder decoder) throws IOException {
        machineId = decoder.readTextString();
        assetManagement = decoder.readInt() == 1;
        workoutTracking = decoder.readInt() == 1;
    }

    @Override
    public boolean equals(Object object) {
        if(object instanceof ConsoleFeatureSetDataV1) {
            ConsoleFeatureSetDataV1 data = (ConsoleFeatureSetDataV1)object;
            return machineId.equals(data.machineId) && data.assetManagement == assetManagement && data.workoutTracking == workoutTracking;
        }
        return false;
    }

}
