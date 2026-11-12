package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

public enum PTPKeys
{
    DATA_PTP_USER_COOKIE_STRING(new DataDefault<>("")),
    ACTION_PTP_ACTIVITY_LOADED,
    ACTION_PTP_WORKOUTS_LOADED_INT,
    ACTION_PTP_DISPLAY_LOADED,
    DATA_RECOMMENDED_EXERCISES_PARCELABLE,
    /** The loaded assignment and its owner together, so the pair cannot be read half updated. */
    DATA_PTP_LOADED_ASSIGNMENT_PARCELABLE;

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (PTPKeys s : PTPKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    PTPKeys(){
        this.DataDefault = null;
    }

    PTPKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
