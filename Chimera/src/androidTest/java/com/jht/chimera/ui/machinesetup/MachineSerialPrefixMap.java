package com.jht.chimera.ui.machinesetup;

import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;

class MachineSerialPrefixMap {
    private final FrameConfigEnum machine;

    MachineSerialPrefixMap(FrameConfigEnum machine) {
        this.machine = machine;
    }

    FrameConfigEnum machine() {
        return machine;
    }

}
