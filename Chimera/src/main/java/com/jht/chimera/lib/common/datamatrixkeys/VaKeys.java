package com.jht.chimera.lib.common.datamatrixkeys;

import com.jht.datamatrixlib.DataDefault;
import com.jht.datamatrixlib.ParcelableDataDefault;
import com.jht.virtualactive.systeminterface.VAServiceSystemInterface;

import java.util.LinkedList;
import java.util.List;

/**
 * Contains the keys relating to VA workouts or setup.
 */
public enum VaKeys {
    /**
     * Action:  This is sent before starting the Virtual Active workout to allow for it to have time
     *          to configure everything prior to the workout starting.
     *
     *          This value will be cleared on the workout ending.
     */
    ACTION_SELECT_WORKOUT_UNIQUEID_STRING,
    /**
     * This is the last value that was selected by the user.
     */
    DATA_SELECTED_WORKOUT_UNIQUEID_STRING(new DataDefault<>("")),
    DATA_VA_ITEMS_PARCELABLE(new ParcelableDataDefault()),
    /**
     * @see VAServiceSystemInterface#canDisplayProfileGraph()
     */
    DATA_VA_CAN_DISPLAY_APP_ICON_BOOL(new DataDefault<>(false));

    public static List<DataDefault<?>> sDefaultValuesList = new LinkedList<>();

    static {
        for (VaKeys s : VaKeys.values()) {
            if(s.DataDefault != null) {
                sDefaultValuesList.add(s.DataDefault);
            }
        }
    }

    public DataDefault<?> DataDefault;

    VaKeys(){
        this.DataDefault = null;
    }

    VaKeys(DataDefault<?> DataDefault) {
        this.DataDefault = DataDefault;
        this.DataDefault.name = name();
    }
}
