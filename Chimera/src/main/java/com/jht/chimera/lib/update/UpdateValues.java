package com.jht.chimera.lib.update;

import com.jht.chimera.BuildConfig;

/**
 * Gets the correct RSCU type for each console. The equipment type for the console (required to
 * match UpdateService so the console will install software) is not required to equal the software
 * type of the main package (eg lowcostgui equipment type and lcgui sw type).
 *
 * This will handle those cases and should be used as the SW type for RSCU updates.
 */
public class UpdateValues {
    private static final String VISION_EQUIPMENT_TYPE = "vision600e";
    private static final String VISION_SW_TYPE = "v600e";

    private static final String LCGUI_EQUIPMENT_TYPE = "lowcostgui";
    private static final String LCGUI_SW_TYPE = "lcgui";

    /**
     * Returns SW type based on BuildConfig OR static values above.
     *
     * @return
     */
    public static String getOtaSwType() {
        String systemSwType = BuildConfig.UPDATE_EQUIPMENT;
        if (systemSwType.equalsIgnoreCase(VISION_EQUIPMENT_TYPE))  {
            return VISION_SW_TYPE; //NON-NLS
        } else if (systemSwType.equalsIgnoreCase(LCGUI_EQUIPMENT_TYPE)) {
            return LCGUI_SW_TYPE;
        }

        return systemSwType;
    }
}
