package com.jht.chimera.lib.tv.switft.jhttvservice.atsc;

/**
 * Created by mutombolin on 2018/08/27.
 */

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
    IDENTIFYFAIL,
    NETWORKERROR,
    None;

    public int getValue() {
        return ordinal();
    }
}

