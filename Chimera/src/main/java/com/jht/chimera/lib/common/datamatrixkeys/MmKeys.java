package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum MmKeys {
    DATA_MM_EVENT_INDEX_PARCELABLE,
    DATA_PROGRESSION_INTERVAL_TYPE_INT(new DataDefault<>(0)), // The current interval intensity
    DATA_PROGRESSION_WORKOUT_INTERVALS_PARCELABLE(new ParcelableDataDefault()); // Tracks intervals for Progression workout for display

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (MmKeys s : MmKeys.values()) {
            if (s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    MmKeys() {
        this.DataDefault = null;
    }

    MmKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
