package com.jht.chimera.lib.common.config.frames;

/*
    Constants for "powerRule" of "FrameConfig"
    This is a pure constants class with no need for outside dependencies.
    Should this be integrated into "FrameConfig" as an inner class or is it better to isolate it?
 */
public final class FramePowerRules {
    // should this be an enum?
    // should there be one list of rules for all segments or does separating for platform make more sense?

    // Power limiting rules numbers per platform. Assigned to frame configs.
    public static final int RULE_NONE = 0; // ZERO is undefined default value for rules
    public static final class Parker {
        public static final int RULE_NO22_USB_VOLUME = 1;   // no 22", limit all USBs, limit volume on 16"
        public static final int RULE_NO22_USB = 2;  // no 22", limit all USBs
        public static final int RULE_NO22 = 3;  // no 22", no other limits
        public static final int RULE_FULL_POWER = 4; // frame run everything
    }

}
