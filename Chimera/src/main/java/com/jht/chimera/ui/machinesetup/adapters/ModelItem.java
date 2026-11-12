package com.jht.chimera.ui.machinesetup.adapters;


import android.content.Context;
import android.view.Gravity;

import androidx.annotation.NonNull;

import com.jht.androiduiwidgets.selector.SimpleSelectorItem;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;


/**
 * This is contains data needed to create a row in the error log table.
 */
public class ModelItem extends SimpleSelectorItem {


    private FrameConfigEnum machineModel;
    private int modelStringId;
    private Context context;

    /**
     * Initialize the error log item.
     */
    public ModelItem(Context context, @NonNull FrameConfigEnum machineModel) {
        super("", Gravity.CENTER_VERTICAL | Gravity.START, 20, 0, 0, 0);
        this.machineModel = machineModel;
        this.modelStringId = machineModel.createFrameConfig().MODEL;
        this.context = context;
    }


    /**
     * Used to get the item's label.
     *
     * @return Return the label
     */
    @NonNull
    @Override
    public String toString() {
        return context.getResources().getString(modelStringId);
    }

    // Getters for the data.
    public FrameConfigEnum machineModel() {
        return machineModel;
    }

}
