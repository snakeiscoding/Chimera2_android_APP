package com.jht.chimera.ui.mainapp.goscreen;

import com.jht.androiduiwidgets.IInputConfiguration;
import com.jht.androiduiwidgets.pillbox.PillBox;

public class MinMaxParameterLogic {
    // Copies of several keys from the workout controller script variables
    private static final String MIN_TARGET_WATTS_PAIR = "MIN_TARGET_WATTS"; //NON-NLS
    private static final String MAX_TARGET_WATTS_PAIR = "MAX_TARGET_WATTS"; //NON-NLS
    private static final String MIN_RESISTANCE = "MIN_RESISTANCE"; //NON-NLS
    private static final String MAX_RESISTANCE = "MAX_RESISTANCE"; //NON-NLS
    private static final String MIN_SPEED = "MIN_SPEED"; //NON-NLS
    private static final String MAX_SPEED = "MAX_SPEED"; //NON-NLS


    public static void handleValueUpdated(float value, String identifier, PillBox[] pillBoxes) {
        switch (identifier) {
            case MIN_TARGET_WATTS_PAIR:
                updatePairedValue(MAX_TARGET_WATTS_PAIR, true, value, pillBoxes);
                break;
            case MAX_TARGET_WATTS_PAIR:
                updatePairedValue(MIN_TARGET_WATTS_PAIR, false, value, pillBoxes);
                break;
            case MIN_RESISTANCE:
                updatePairedValue(MAX_RESISTANCE, true, value, pillBoxes);
                break;
            case MAX_RESISTANCE:
                updatePairedValue(MIN_RESISTANCE, false, value, pillBoxes);
                break;
            case MIN_SPEED:
                updatePairedValue(MAX_SPEED, true, value, pillBoxes);
                break;
            case MAX_SPEED:
                updatePairedValue(MIN_SPEED, false, value, pillBoxes);
                break;
        }
    }

    private static void updatePairedValue(String identifier, boolean higher, float newValue, PillBox[] pillBoxes) {
        for (PillBox pillBox : pillBoxes) {
            IInputConfiguration parameter = pillBox.parameterSettings;
            if (parameter.identifier().equals(identifier)) {
                if (higher) {
                    double updatedValue = Math.max(pillBox.parameterSettings.valueAsDouble(), newValue + pillBox.parameterSettings.resolution());
                    if (updatedValue != pillBox.parameterSettings.valueAsDouble()) {
                        pillBox.setValue((float)updatedValue, false);
                    }
                } else {
                    double updatedValue = Math.min(pillBox.parameterSettings.valueAsDouble(), newValue - pillBox.parameterSettings.resolution());
                    if (updatedValue != pillBox.parameterSettings.valueAsDouble()) {
                        pillBox.setValue((float)updatedValue, false);
                    }
                }

                break;
            }
        }
    }
}
