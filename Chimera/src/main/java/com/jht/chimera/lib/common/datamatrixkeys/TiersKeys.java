package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum TiersKeys {
    DATA_UPDATE_TIERS_BOOL(new DataDefault<>(false)),
    DATA_FEATURE_SET_PARCELABLE(new ParcelableDataDefault()),
    DATA_GUEST_FEATURE_SET_PARCELABLE(new ParcelableDataDefault()),
    DATA_UPGRADE_PARCELABLE(new ParcelableDataDefault()),
    ACTION_UPGRADE_RESULT_PARCELABLE(new ParcelableDataDefault()),
    DATA_USING_TIERS_BOOL(new DataDefault<>(false));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (TiersKeys s : TiersKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    TiersKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
    public static boolean usingTiers() {
        return DataMatrix.getBoolean(TiersKeys.DATA_USING_TIERS_BOOL, false);
    }
}
