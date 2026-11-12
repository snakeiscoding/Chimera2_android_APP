package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

public enum ConsoleConfigKeys {
    EVENT_CONSOLE_THEME_READY;

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (ConsoleConfigKeys s : ConsoleConfigKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    ConsoleConfigKeys(){
        this.DataDefault = null;
    }

    ConsoleConfigKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
