package com.jht.chimera.ui.machinesetup.adapters;


import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.androiduiwidgets.selector.SimpleSelectorItem;
import com.jht.androiduiwidgets.selector.SimpleSelectorItemHolder;
import com.jht.chimera.R;

/**
 * This is contains data needed to create a row in the error log table.
 */
public class MachineItem extends SimpleSelectorItem {


    private JHTCommonTypes.MachineType machineType;
    private Context context;

    /**
     * Initialize the error log item.
     */
    public MachineItem(Context context, @NonNull JHTCommonTypes.MachineType machineType) {
        super("", Gravity.CENTER_VERTICAL | Gravity.START, 20, 0, 0, 0);
        this.machineType = machineType;
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
        switch (machineType) {
            case Treadmill:
                return context.getString(R.string.treadmill_tc);
            case Elliptical:
            case JhtADboardElliptical:
                return context.getString(R.string.elliptical_tc);
            case HybridBike:
                return context.getString(R.string.hybrid_bike_tc);
            case UprightBike:
            case JhtADboardUBike:
                return context.getString(R.string.upright_bike_tc);
            case RecumbentBike:
            case JhtADboardRBike:
                return context.getString(R.string.recumbent_bike_tc);
            case Ascent:
                return context.getString(R.string.ascent_tc);
            case LowerBodyAscent:
                return context.getString(R.string.ascent_lower_body_tc);
            case Stepper:
                return context.getString(R.string.stepper_tc);
            case Climbmill:
                return context.getString(R.string.climbmill_tc);
            case CardioSpinBike:
                return "Virtual Training Cycle";
            case RecumbentElliptical:
                return context.getString(R.string.recumbent_elliptical_tc);
        }
        return machineType.toString();
    }

    // Getters for the data.
    public JHTCommonTypes.MachineType machineType() {
        return machineType;
    }


    @Override
    public SelectorItemHolderBase createViewHolder(Context context, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.machine_selector_item, parent, false);
        return new SimpleSelectorItemHolder(v, false);
    }

}
