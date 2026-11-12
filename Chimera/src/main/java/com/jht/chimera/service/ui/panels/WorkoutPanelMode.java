package com.jht.chimera.service.ui.panels;

import com.jht.chimera.lib.common.config.ChimeraEnums;

public enum WorkoutPanelMode {
    NORMAL,
    OVERLAY,
    ;

    public int getWindowType() {
        return (this == NORMAL ? ChimeraEnums.getTypeSystemAlert() : ChimeraEnums.TYPE_SYSTEM_ERROR);
    }
}
