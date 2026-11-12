package com.jht.chimera.ui.machinesettings;


/**
 * TODO THIS IS A TEMPORARY PORT SO CODE COMPILES.
 */
public class JHTTypes {

    public enum AspectRatio {
        Fill,
        Auto
    }

    public enum ATSC_STATUS {
        PROCESSING,
        ACKOK,
        TIMEOUTFAIL,
        CHECKSUMERROR,
        OVERINDEXERROR,
        TIMEOUTREPLY,
        REPLYFAIL,
        IMPORTSUCCESS,
        IMPORTFAIL,
        UPGRADESUCCESS,
        UPGRADEFAIL,
        LEAVEUPGRADEMODE,
        EXPORTSUCCESS,
        EXPORTFAIL,
        PERMISSIONDENIED,
        FILENOTEXISTED,
        None;

        public int getValue() {
            return ordinal();
        }
    }

    public enum IPTVProtocol {
        RTSP,
        UDP,
        HTTP,
        HTTPS
    }

    public enum FEATURE_TYPE {
        FEATURE_TYPE_WORKOUT,
        FEATURE_TYPE_APP,
        NUM_FEATURE_TYPES;
    }

    public enum FEATURE_STATE {
        FEATURE_DISABLED,
        FEATURE_ENABLED,
        FEATURE_HIDDEN,
        FEATURE_LIMITED,
        NUM_FEATURE_STATES;
    }
}