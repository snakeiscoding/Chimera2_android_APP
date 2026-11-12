package com.jht.chimera.ui.machinesettings.pages;

import static com.jht.androidcommonalgorithms.type.JHTCommonTypes.TimeFormat.Twelve_Hour;

import android.animation.Animator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.chimera.BuildConfig;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datetime.DateTimeUtil;
import com.jht.chimera.lib.common.i18n.DateFormat;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.i18n.TimeZoneStrings;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.radiogroup.RadioGroup;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * Implements the date tune page gluing the page to the model layer.
 */
public class DateTime extends SettingPageBase {

    private DataMatrix dataMatrix = new DataMatrix();

    // Clock timer to keep the clock in sync.
    private PeriodicTimer clockTimer = null;

    // On screen widgets that we cache.
    private CheckBox automaticDateTime;
    private UserTextInput dateText;
    private UserTextInput timeText;
    private OptionSelectorItem dateFormat;
    private RadioGroup timeFormat;
    private RadioGroup units;
    private View view;

    /**
     * This handles the clock timer making sure the date and time field are correct.
     */
    private JHTTimer.IOnTimer onClockTimer = () -> page().post(() -> {
        setCurrentDate();
        setCurrentTime();
    });

    /**
     * Update the date field.
     */
    private void setCurrentDate() {
        final GregorianCalendar c = new GregorianCalendar();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        String dateString = String.format(Locale.ENGLISH,"%02d/%02d/%d", month, day, year);

        // Month is 0 based, just add 1
        int mDateFormatIndex = DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT);
        if (mDateFormatIndex == DateFormat.MONTH_DAY_YEAR.ordinal()) {        // MM/DD/YYYY
            dateString = String.format(Locale.ENGLISH, "%02d/%02d/%d", month + 1, day, year);
        } else if (mDateFormatIndex == DateFormat.DAY_MONTH_YEAR.ordinal()) { // DD/MM/YYYY
            dateString = String.format(Locale.ENGLISH, "%02d/%02d/%d", day, month + 1, year);
        } else if (mDateFormatIndex == DateFormat.YEAR_MONTH_DAY.ordinal()) { // YYYY/MM/DD
            dateString = String.format(Locale.ENGLISH, "%d/%02d/%02d", year, month + 1, day);
        }

        dateText.setText(dateString);
    }

    /**
     * Update the time field.
     */
    private void setCurrentTime() {
        String timeString;
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL)) {
            timeString = new SimpleDateFormat("H:mm", Locale.ENGLISH).format(new Date());
        } else {
            timeString = new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date());
        }
        timeText.setText(timeString);
    }

    /**
     * Initialize the page passing in the models.
     */
    public DateTime() {
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.date_time, null);
        container.addView(view);
        return view;
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        automaticDateTime = view.findViewById(R.id.eng_automatic_date_time);
        dateText = view.findViewById(R.id.engineering_date_time_date);
        timeText = view.findViewById(R.id.eng_current_system_time);
        dateFormat = view.findViewById(R.id.eng_date_format);
        timeFormat = view.findViewById(R.id.eng_time_format_option);
        units = view.findViewById(R.id.eng_default_units);

        Calendar maxDate = Calendar.getInstance();
        maxDate.setTimeInMillis(Integer.MAX_VALUE*1000L);
        Calendar minDate = Calendar.getInstance();
        minDate.setTime(BuildConfig.BUILD_TIME);
        dateText.setInputProperties(dateText.getInputProperties().max(maxDate.getTimeInMillis()).min(minDate.getTimeInMillis()));

        OptionSelectorItem timezone = view.findViewById(R.id.eng_time_zone);
        final OptionSelectorItem languageSelect = view.findViewById(R.id.eng_language_select);

        // Initialize the language and time zone options.
        List<String> tmpList = LanguageProperty.getSpinnerList();
        String[] languageList = tmpList.toArray(new String[0]);
        languageSelect.setOptions(new OptionManager(languageList, DataMatrix.getInt(SettingKeys.General.DATA_CONSOLE_LANGUAGE_ENUM_INT)));
        languageSelect.setOnOptionSelected((pos, option) -> {
            LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(pos);
            LocaleUtil.changeLanguageAndUpdateDefault(ContextUtil.getApplicationContext(), lp);
            timeFormat.select(DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL) ? 1 : 0);
            dateFormat.selectItem(DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT));
            units.select(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL) ? 0 : 1);
        });

        // Initialize the language and time zone options.
        TimeZoneStrings timeZoneStrings = new TimeZoneStrings(getContext());
        timezone.setOptions(R.array.timezoneDisplay, timeZoneStrings.getCurrentIndex());
        timezone.setOnOptionSelected((pos, option) -> {
            DataMatrix.putString(SettingKeys.General.DATA_IANA_TIME_ZONE_STRING, timeZoneStrings.getIANAString(pos));
            DataMatrix.putInt(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT, pos);
            DateTimeUtil.setTimezone(timeZoneStrings.getIANAString(pos));
        });

        // Initialize the display settings.
        // Manage the automatic date and time option.
        automaticDateTime.setChecked(DataMatrix.getBoolean(SettingKeys.General.DATA_AUTO_DATE_TIME_BOOL));
        automaticDateTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            DataMatrix.putBoolean(SettingKeys.General.DATA_AUTO_DATE_TIME_BOOL, isChecked);
            //Disable Android auto-update since some NTP servers in the pool are no longer available
            //and avoid update the time from OS and GUI at the same time
            DateTimeUtil.setAutoTime(false);
            timeText.setReadOnly(automaticDateTime.isChecked());
            dateText.setReadOnly(automaticDateTime.isChecked());
            page().postDelayed(() -> {
                if (clockTimer != null) clockTimer.reset();
            }, 1000);
        });


        // Manage the time format option.
        String[] timeFormatList = getContext().getResources().getStringArray(R.array.timeFormatList);
        if (ProductConfig.getConsoleType().inches == 10){
            timeFormat.linkField(DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL) ? 1 : 0, timeFormatList, LinearLayout.VERTICAL);
        } else {
            timeFormat.linkField(DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL) ? 1 : 0, timeFormatList, LinearLayout.HORIZONTAL);
        }
        timeFormat.setOnOptionChanged(index -> {
            DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, index == 1);
            DateTimeUtil.setTimeFormat(index == Twelve_Hour.ordinal());
        });


        // Manage the date format option.
        dateFormat.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.dateFormatList), DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT)));
        dateFormat.setOnOptionSelected((pos, option) -> {
            DataMatrix.putInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT, pos);
            DateTimeUtil.setDateFormat(DateFormat.values()[pos]);
        });

        dateText.setOnDatePicked((view1, year, month, dayOfMonth) -> {
            DateTimeUtil.setDate(year, month, dayOfMonth);
            clockTimer.reset();
        });
        timeText.setOnTimePicked((view12, hourOfDay, minute) -> {
            DateTimeUtil.setTime(hourOfDay, minute);
            clockTimer.reset();
        });

        String[] unitTypes = {StringLookup.get(R.string.metric_tc), StringLookup.get(R.string.imperial_tc)};
        if (ProductConfig.getConsoleType().inches == 10){
            units.linkField(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL) ? 0 : 1, unitTypes, LinearLayout.VERTICAL);
        } else {
            units.linkField(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL) ? 0 : 1, unitTypes, LinearLayout.HORIZONTAL);
        }
        units.setOnOptionChanged(index ->  {
            DataMatrix.putBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL, index == 0);
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, index == 0);
        });

        // Start the clock timer.
        clockTimer = new PeriodicTimer(0, 1000, onClockTimer);
        clockTimer.start();
        updateDateTimeWidgets();

    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        dataMatrix.finish();

        // Destroy the clock timer.
        if (clockTimer != null) {
            clockTimer.stop();
            clockTimer = null;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Used to toggle between the read only and writeable views of the date and time.
     */
    private void updateDateTimeWidgets() {
        timeText.setReadOnly(automaticDateTime.isChecked());
        dateText.setReadOnly(automaticDateTime.isChecked());
    }

}
