package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;

import java.util.LinkedList;
import java.util.List;

public enum ClubComKeys {
    DATA_CLUB_COM_OPTIONS_PARCELABLE(new ParcelableDataDefault()), // see ClubComOptions class
    ACTION_CLUB_COM_START_SESSION,
    ACTION_CLUB_COM_END_SESSION;

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (ClubComKeys s : ClubComKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    ClubComKeys(){
        this.DataDefault = null;
    }

    ClubComKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}