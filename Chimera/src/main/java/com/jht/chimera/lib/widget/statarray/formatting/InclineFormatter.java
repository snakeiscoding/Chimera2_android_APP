package com.jht.chimera.lib.widget.statarray.formatting;

import android.content.Context;

import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.formatting.FloorFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.statarray.StatValueUpdater;

/**
 * Formats distances in kilometers (float) to be passed to the stat bar for display
 * <p>
 * Created by chris.foyer on 8/23/2017.
 */
public class InclineFormatter implements IStatFormattingLogic, IStatTitles {
    private Context mContext;

    public InclineFormatter(Context context) {
        mContext = context;
    }

    @Override
    public String format(String value) {
        throw new RuntimeException("Incline cannot be formatted from string");
    }

    @Override
    public String format(Integer value) {
        throw new RuntimeException("Incline cannot be formatted from string");
    }

    @Override
    public String format(Double value) {
        throw new RuntimeException("Incline cannot be formatted from double");
    }

    @Override
    public String format(Float value) {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Ascent:
                return FloorFormat.format(value, 0);
            case Treadmill:
                return FloorFormat.format(value, 1);
            default:
                return FloorFormat.format(value, 1);
        }
    }

    @Override
    public void setStatUpdaterCallback(StatValueUpdater.IStatValueUpdaterCallback statUpdaterCallback) {
        // Forcing updates not needed for incline
    }

    @Override
    public String getStatTitle() {
        return StringLookup.getString(R.string.incline_uc, mContext);
    }

    @Override
    public String getStatDefaultValue() {
        FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
        switch (config.machineClassType) {
            case Ascent:
                return "0";
            default:
                return "0.0";
        }
    }
}
