package com.jht.chimera.ui.machinesettings.adapters.service;

import android.view.View;
import android.widget.TextView;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorItemHolderBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.datamatrixkeys.ImportExportKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.language.StringHelper;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.datamatrixlib.DataMatrix;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;


/**
 * This maps a service log item to a view.
 */
class ServiceLogItemHolder extends SelectorItemHolderBase {

    private View selectorView;
    private TextView date;
    private TextView distance;
    private TextView time;
    private TextView serviceType;
    private TextView description;

    /**
     * Cache the views in the holder.
     *
     * @param v The root view.
     */
    ServiceLogItemHolder(View v) {
        super(v);
        selectorView = v;
        date = selectorView.findViewById(R.id.eng_service_date);
        distance = selectorView.findViewById(R.id.eng_service_distance);
        time = selectorView.findViewById(R.id.eng_service_time);
        serviceType = selectorView.findViewById(R.id.eng_service_type);
        description = selectorView.findViewById(R.id.eng_service_description);
    }


    /**
     * The item has changed.  We need to update the views.
     *
     * @param adapter The adapter managing the views.
     * @param item    The item with the view data.
     */
    @Override
    protected void update(final SelectorAdapterBase adapter, final ISelectorItem item, final IOnItemSelected onItemSelected) {

        if (item instanceof ServiceLogItem) {
            selectorView.setBackgroundColor(selectorView.getContext().getResources().getColor(item.getIndex() % 2 == 0 ? R.color.table_even_row_background_color : R.color.table_odd_row_background_color));
            ServiceLogItem serviceLogItem = (ServiceLogItem) item;

            SimpleDateFormat dateFormat = null;
            switch (JHTCommonTypes.DateFormat.values()[DataMatrix.getInt(ImportExportKeys.DATA_CURRENT_DATE_FORMAT_INT)]) {

                case YearMonthDay:
                    dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()); //NON-NLS
                    break;
                case MonthDayYear:
                    dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); //NON-NLS
                    break;
                case DayMonthYear:
                    dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); //NON-NLS
                    break;
                case DayMonthAbrYear:
                    dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); //NON-NLS
                    break;
                default:
                    break;
            }
            if (dateFormat != null) {
                date.setText(dateFormat.format(serviceLogItem.log().logTime.getTime()), null);
            }
            switch (FrameConfigManager.getInstance().getFrameConfig().machineClassType) {
                case Stepper:
                case Climbmill:
                    distance.setText(String.format(Locale.ENGLISH, "%s %.0f", selectorView.getContext().getResources().getString(R.string.floors_tc),
                            serviceLogItem.log().accumulatedFloor));
                    break;
                default:
                    DecimalFormat format = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                    if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) {
                        distance.setText(String.format(Locale.ENGLISH, "%s %s", format.format(
                                serviceLogItem.log().accumulatedDistanceKm), selectorView.getContext().getResources().getString(R.string.km_lc)));
                    } else {
                        distance.setText(String.format(Locale.ENGLISH, "%s %s", format.format(
                                serviceLogItem.log().accumulatedDistanceKm * CommonRoutines.getConvertFactorDistance()),
                                selectorView.getContext().getResources().getString(R.string.miles_lc)));
                    }
                    break;
            }
            int accumulatedTimeSec = serviceLogItem.log().accumulatedTimeSec;
            time.setText(String.format(Locale.ENGLISH, "%d:%02d %s", accumulatedTimeSec / 3600, (accumulatedTimeSec / 60) % 60, selectorView.getContext().getResources().getString(R.string.hours_lc)));
            description.setText(serviceLogItem.log().message);
            serviceType.setText(StringHelper.getServiceTypeString(selectorView.getContext(), StringHelper.ServiceTypes.values()[serviceLogItem.log().serviceType]));
            selectorView.findViewById(R.id.eng_service_log_delete).setOnClickListener(serviceLogItem.onDelete());

        }

    }

}
