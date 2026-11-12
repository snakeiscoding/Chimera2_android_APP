package com.jht.chimera.lib.language;

import android.content.Context;
import android.content.res.Resources;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.chimera.R;


public class StringHelper {
    public enum CASE {
        LOWER_CASE,
        UPPER_CASE,
        TITLE_CASE
    }


    public enum ServiceTypes {
        Battery,
        BeltDrive,
        BeltGeneratorFlyWheel,
        BeltRunningFlipDeck,
        BeltRunningNewDeck,
        ChainDrive,
        StairDrive,
        ConsoleCable,
        ResetAccumulatedTime,
        ResetAccumulatedDistance,
        ResetAccumulatedFloors,
        ResetHeadphoneCounter,
        RestoreToFactoryDefaults,
        LubeBelt;
    }

    static public String getUnitString(Context context, JHTCommonTypes.DisplayUnit unit, JHTCommonTypes.Unit unitType) {
        return getUnitString(context, unit, unitType, StringHelper.CASE.LOWER_CASE);
    }

    static public String getUnitString(Context context, JHTCommonTypes.DisplayUnit unit, JHTCommonTypes.Unit unitType, StringHelper.CASE stringCase) {
        int id = 0;
        if (unit == null) return "";
        switch (unit) {
            case DISTANCE:
                switch (unitType) {
                    case Metric:
                        switch (stringCase) {
                            case LOWER_CASE:
                                id = R.string.km_lc;
                                break;
                            case UPPER_CASE:
                                id = R.string.km_uc;
                                break;
                            case TITLE_CASE:
                                id = R.string.km_uc;
                                break;
                        }
                        break;
                    case Imperial:
                        switch (stringCase) {
                            case LOWER_CASE:
                                id = R.string.miles_lc;
                                break;
                            case UPPER_CASE:
                                id = R.string.miles_uc;
                                break;
                            case TITLE_CASE:
                                id = R.string.miles_uc;
                                break;
                        }
                        break;
                }
                break;
            case DISTANCE_SMALL:
                switch (unitType) {
                    case Metric:
                        id = R.string.m_lc;
                        break;
                    case Imperial:
                        id = R.string.ft_lc;
                        break;
                }
                break;
            case SPEED:
                switch (unitType) {
                    case Metric:
                        switch (stringCase) {
                            case LOWER_CASE:
                                id = R.string.kph_lc;
                                break;
                            case UPPER_CASE:
                                id = R.string.kph_uc;
                                break;
                            case TITLE_CASE:
                                id = R.string.kph_uc;
                                break;
                        }
                        break;
                    case Imperial:
                        switch (stringCase) {
                            case LOWER_CASE:
                                id = R.string.mph_lc;
                                break;
                            case UPPER_CASE:
                                id = R.string.mph_uc;
                                break;
                            case TITLE_CASE:
                                id = R.string.mph_uc;
                                break;
                        }
                        break;
                }
                break;
            case HEART_RATE:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.bpm_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.bpm_uc;
                        break;
                    case TITLE_CASE:
                        id = R.string.bpm_uc;
                        break;
                }
                break;
            case TIME_SECONDS:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.seconds_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.seconds_tc;
                        break;
                    case TITLE_CASE:
                        id = R.string.seconds_tc;
                        break;
                }
                break;
            case TIME_MINUTES:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.minutes_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.minutes_tc;
                        break;
                    case TITLE_CASE:
                        id = R.string.minutes_tc;
                        break;
                }
                break;
            case TIME_HOURS:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.hours_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.hours_tc;
                        break;
                    case TITLE_CASE:
                        id = R.string.hours_tc;
                        break;
                }
                break;
            case PERCENT:
                id = R.string.percent_lc;
                break;
            case CALORIES:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.cals_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.calories_uc;
                        break;
                    case TITLE_CASE:
                        id = R.string.calories_tc;
                        break;
                }
                break;
            case WEIGHT:
                switch (unitType) {
                    case Metric:
                        switch (stringCase) {
                            case LOWER_CASE:
                                id = R.string.kg_lc;
                                break;
                            case UPPER_CASE:
                                id = R.string.kg_uc;
                                break;
                            case TITLE_CASE:
                                id = R.string.kg_uc;
                                break;
                        }
                        break;
                    case Imperial:
                        switch (stringCase) {
                            case LOWER_CASE:
                                id = R.string.lbs_lc;
                                break;
                            case UPPER_CASE:
                                id = R.string.lbs_uc;
                                break;
                            case TITLE_CASE:
                                id = R.string.lbs_uc;
                                break;
                        }
                        break;
                }
                break;
            case FLOORS:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.floors_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.floors_uc;
                        break;
                    case TITLE_CASE:
                        id = R.string.floors_tc;
                        break;
                }
                break;
            case SPM:
                id = R.string.spm_uc;
                break;
            case RPM:
                id = R.string.rpm_uc;
                break;
            case HEIGHT:
                switch (unitType) {
                    case Metric:
                        id = R.string.cm_lc;
                        break;
                    case Imperial:
                        id = R.string.inches_lc;
                        break;
                }
                break;

            case TIME_YEARS:
                switch (stringCase) {
                    case LOWER_CASE:
                        id = R.string.years_lc;
                        break;
                    case UPPER_CASE:
                        id = R.string.years_uc;
                        break;
                    case TITLE_CASE:
                        id = R.string.years_tc;
                        break;
                }
                break;
            case ADC:
                switch (stringCase) {
                    case LOWER_CASE:
                        return "adc"; //NON-NLS
                    case UPPER_CASE:
                        return "ADC"; //NON-NLS
                    case TITLE_CASE:
                        return "ADC"; //NON-NLS
                }
                break;
        }
        try {
            return context.getResources().getString(id);
        } catch (Resources.NotFoundException ex) {
            return "";
        }
    }

    /**
     * Returns the string for the service type input.
     *
     * @param context     Context that contains the string resources.
     * @param serviceType The service log type.
     * @return A string to display to the user.
     */
    static public String getServiceTypeString(Context context, ServiceTypes serviceType) {
        switch (serviceType) {

            case Battery:
                return context.getResources().getString(R.string.service_item_battery_tc);

            case BeltDrive:
                return context.getResources().getString(R.string.service_item_belt_drive_tc);
            case BeltGeneratorFlyWheel:
                return context.getResources().getString(R.string.service_item_belt_generator_flywheel_tc);
            case BeltRunningFlipDeck:
                return context.getResources().getString(R.string.service_item_belt_running_flip_deck_tc);
            case BeltRunningNewDeck:
                return context.getResources().getString(R.string.service_item_belt_running_new_deck_tc);
            case ChainDrive:
                return context.getResources().getString(R.string.service_item_chain_drive_tc);
            case StairDrive:
                return context.getResources().getString(R.string.service_item_chain_stair_tc);
            case ConsoleCable:
                return context.getResources().getString(R.string.service_item_console_cable_tc);
            case ResetAccumulatedTime:
                return context.getResources().getString(R.string.reset_accumulated_time_tc);
            case ResetAccumulatedDistance:
                return context.getResources().getString(R.string.reset_accumulated_distance_tc);
            case ResetAccumulatedFloors:
                return context.getResources().getString(R.string.reset_accumulated_floors_tc);
            case ResetHeadphoneCounter:
                return context.getResources().getString(R.string.reset_headphone_accumulators_tc);
            case RestoreToFactoryDefaults:
                break;
            case LubeBelt:
                return context.getResources().getString(R.string.lube_belt_tc);
        }
        return "";
    }


}
