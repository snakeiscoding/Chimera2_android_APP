package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

/**
 * Class to contain keys that are controlled by the Sprint8 activity in the main app
 */
public enum Sprint8Keys {
    DATA_SHOW_SPRINT8_TOP_BAR_INT(new DataDefault<>(0)),
    DATA_SPRINT8_NUM_SPRINTS_INT(new DataDefault<>(8));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (Sprint8Keys s : Sprint8Keys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    Sprint8Keys(){
        this.DataDefault = null;
    }

    Sprint8Keys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }

    public class Constants {
        /**
         * Constants for {@link #DATA_SHOW_SPRINT8_TOP_BAR_INT}
         */
        public static final int SHOW = 0;
        public static final int SHOW_NONE = 1;

        public static final int MEDIA_TYPE_NONE = 0;
        public static final int MEDIA_TYPE_VIDEO = 1;
        public static final int MEDIA_TYPE_AUDIO = 2;
    }
}
