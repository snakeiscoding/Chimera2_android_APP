package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;

import java.util.LinkedList;
import java.util.List;

public enum FeaturedWorkoutKeys {
    /**
    // IFIT_APP_VISIBLE_BOOL: should we actually show the iFit app?
     * Calculated based on other keys, so doesn't need to be persisted,
     * hence being here instead of in SettingKeys.
     * This is managed by the ParkerGoScreenActivity.
     */
    DATA_IFIT_APP_VISIBLE_BOOL(new DataDefault<>(false));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (FeaturedWorkoutKeys s : FeaturedWorkoutKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    FeaturedWorkoutKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
