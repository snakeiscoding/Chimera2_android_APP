package com.jht.chimera.ui.machinesetup.pages;

import static com.jht.androidcommonalgorithms.type.JHTCommonTypes.TimeFormat.Twelve_Hour;
import static com.jht.androidcommonalgorithms.type.JHTCommonTypes.TimeFormat.Twenty_Four_Hour;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.timer.JHTTimer;
import com.jht.androidcommonalgorithms.timer.PeriodicTimer;
import com.jht.androiduiwidgets.selector.IOnItemSelected;
import com.jht.androiduiwidgets.selector.ISelectorItem;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SimpleArraySelectorItem;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.context.ContextUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.datetime.DateTimeUtil;
import com.jht.chimera.lib.common.i18n.DateFormat;
import com.jht.chimera.lib.common.i18n.LanguageProperty;
import com.jht.chimera.lib.common.i18n.LocaleUtil;
import com.jht.chimera.lib.common.i18n.TimeZoneStrings;
import com.jht.chimera.lib.widget.radiogroup.RadioGroup;
import com.jht.chimera.ui.machinesetup.MachineSetupPageData;
import com.jht.datamatrixlib.DataMatrix;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class LanguagePage extends MachineSetupPageBase {

    private final SimpleArraySelectorItem items;
    private String[] titles;
    private int titleIndex = 0;
    private int languageSelection = -1;

    // Clock timer to keep the clock in sync.
    private PeriodicTimer clockTimer = null;

    // On screen widgets that we cache.
    private TextView dateText;
    private TextView timeText;

    private RadioGroup units;

    private RadioGroup timeFormat;

    private OptionSelectorItem timezone;
    private OptionSelectorItem dateFormat;

    private boolean isClickable = true;

    private void setCurrentDate() {
        final GregorianCalendar c = new GregorianCalendar();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        String dateString = String.format(Locale.ENGLISH, "%02d/%02d/%d", month, day, year);

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

    @SuppressLint("SimpleDateFormat")
    private void setCurrentTime() {
        String timeString;
        if (DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL)) {
            timeString = new SimpleDateFormat("H:mm").format(new Date());
        } else {
            timeString = new SimpleDateFormat("h:mm a").format(new Date());
        }
        timeText.setText(timeString);
    }


    private final JHTTimer titleTimer = new PeriodicTimer(3000, 3000, () ->
            pageData().title.post(() -> pageData().title.animate().setDuration(350).alpha(0).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationEnd(Animator animation) {
                    titleIndex = (titleIndex + 1) % titles.length;
                    pageData().title.setText(titles[titleIndex]);
                    pageData().title.animate().setDuration(350).alpha(1).setListener(null).start();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    pageData().title.setAlpha(1);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            }).start())
        );

    @SuppressLint("InflateParams")
    public LanguagePage(MachineSetupPageData pageData) {
        super(pageData);
        List<String> tmpList = LanguageProperty.getSpinnerList();
        String[] languageList = tmpList.toArray(new String[0]);
        items = new SimpleArraySelectorItem(languageList, -1, Gravity.START | Gravity.CENTER_VERTICAL, 20, 0, 0, 0);
        Context context = ContextUtil.getApplicationContext();
        if(context != null) {
            titles = context.getResources().getStringArray(R.array.selectlanguage);
        }
    }

    /**
     * This handles the clock timer making sure the date and time field are correct.
     */
    private final JHTTimer.IOnTimer onClockTimer = () -> {
        View page = page();
        if(page != null) {
            page.post(() -> {
                // Update the date field.
                setCurrentDate();
                setCurrentTime();
            });
        }
    };

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        @SuppressLint("InflateParams") View view = LayoutInflater.from(context).inflate(R.layout.language_page, null);
        container.addView(view);

        RecyclerView languageSelector = view.findViewById(R.id.language_selector);
        SelectorDefaultAnimations.setDefaultAnimations(context, languageSelector);
        final SelectorAdapterBase adapter = new SelectorAdapterBase(getContext(), items, new IOnItemSelected() {
            /**
             * Called when the user selects an item.
             *
             * @param item  The item selected.
             */
            @Override
            public void onItemSelected(ISelectorItem item) {
                if ( isClickable ){
                    isClickable = false;

                    pageData().nextButton.setVisibility(View.VISIBLE);
                    languageSelection = item.getIndex();

                    LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(languageSelection);
                    LocaleUtil.changeLanguage(ContextUtil.getApplicationContext(), lp, true);

                    new Handler().postDelayed(() -> {
                        //languageSelector.setVisibility(View.VISIBLE);
                        isClickable = true;

                        DateTimeUtil.setDateFormat(DateFormat.values()[DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT)]);
                        DateTimeUtil.setTimeFormat(!DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL));
                    },100);
                }
            }
        });
        languageSelector.setAdapter(adapter);

        TimeZoneStrings timeZoneStrings = new TimeZoneStrings(context);
        DateTimeUtil.setTimezone(timeZoneStrings.getIANAString());
        DateTimeUtil.setAutoTime(false);
        DateTimeUtil.setDateFormat(DateFormat.values()[DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT)]);
        DateTimeUtil.setTimeFormat(!DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL));

        // Cache the widgets.
        dateText = view.findViewById(R.id.machine_setup_current_date);
        timeText = view.findViewById(R.id.machine_setup_current_time);
        timeFormat = view.findViewById(R.id.machine_setup_time_format_option);
        units = view.findViewById(R.id.default_units);
        dateFormat = view.findViewById(R.id.machine_setup_date_format);
        timezone = view.findViewById(R.id.machine_setup_timezone);

        // Initialize the text for the UI widgets.
        updateText(context);

        // Catch unit changes.
        units.setOnOptionChanged(index ->  {
            DataMatrix.putBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL, index == 0);
            DataMatrix.putBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL, index == 0);
        });


        // Catch timezone changes.
        timezone.setOptions(R.array.timezoneDisplay, timeZoneStrings.getCurrentIndex());
        timezone.setOnOptionSelected((pos, option) -> {
            DataMatrix.putString(SettingKeys.General.DATA_IANA_TIME_ZONE_STRING, timeZoneStrings.getIANAString(pos));
            DataMatrix.putInt(SettingKeys.General.DATA_TIME_ZONE_INDEX_INT, pos);
            DateTimeUtil.setTimezone(timeZoneStrings.getIANAString(pos));
        });

        // Manage the time format option.
        timeFormat.setOnOptionChanged(index -> {
            DataMatrix.putBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL, index == Twenty_Four_Hour.ordinal());
            DateTimeUtil.setTimeFormat(index == Twelve_Hour.ordinal());
        });

        // Manage the date format option.
        dateFormat.setOnOptionSelected((pos, option) -> {
            DataMatrix.putInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT, pos);
            DateTimeUtil.setDateFormat(DateFormat.values()[pos]);
        });

        // Consume physic keyboard enter so barcode scanner doesn't go to next page if next button is visible
        if (pageData().nextButton != null) {
            pageData().nextButton.setOnKeyListener((v, keyCode, event) -> keyCode == KeyEvent.KEYCODE_ENTER);
        }

        return view;
    }

    @Override
    public void updateText(Context context) {
        if(units != null) {
            String[] unitTypes = {context.getResources().getString(R.string.metric_tc), context.getResources().getString(R.string.imperial_tc)};

            //linking will add the radio buttons to the radio group
            units.linkField(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_UNIT_IS_METRIC_BOOL) ? 0 : 1, unitTypes, LinearLayout.HORIZONTAL);
            timezone.setLabel(context.getResources().getString(R.string.time_zone_tc));
            TimeZoneStrings timeZoneStrings = new TimeZoneStrings(context);
            timezone.setOptions(R.array.timezoneDisplay, timeZoneStrings.getCurrentIndex());
            units.setLabel(context.getResources().getString(R.string.units_tc));
            timeFormat.setLabel(context.getResources().getString(R.string.time_format_tc));
            dateFormat.setLabel(context.getResources().getString(R.string.dateformat_tc));

            // Manage the time format option.
            String[] timeFormatList = context.getResources().getStringArray(R.array.timeFormatList);
            timeFormat.linkField(DataMatrix.getBoolean(SettingKeys.General.DATA_CLOCK_FORMAT_24_HOURS_BOOL) ? 1 : 0, timeFormatList, LinearLayout.HORIZONTAL);

            // Manage the date format option.
            dateFormat.setOptions(new OptionManager(getContext().getResources().getStringArray(R.array.dateFormatList), DataMatrix.getInt(SettingKeys.General.DATA_DATE_FORMAT_ENUM_INT)));
        }
    }

    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        pageData().previousButton.setVisibility(View.GONE);
        pageData().nextButton.setVisibility(items.getSelection() != -1 ? View.VISIBLE : View.GONE);
        titleIndex = 0;
        pageData().title.setText(titles[0]);
        titleTimer.start();
        pageData().title.setAlpha(1);
        try {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                // Start the clock timer.
                clockTimer = new PeriodicTimer(0, 1000, onClockTimer);
                clockTimer.start();
            }, 1000);
        }
        catch (Exception e){
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                // Start the clock timer.
                clockTimer = new PeriodicTimer(0, 1000, onClockTimer);
                clockTimer.start();
            }, 5000);
        }


    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);
        titleTimer.stop();
        pageData().title.clearAnimation();
        pageData().title.setAlpha(1);

        // Destroy the clock timer.
        if (clockTimer != null) {
            clockTimer.stop();
            clockTimer = null;
        }
    }

    @Override
    public boolean onNext() {
        titleTimer.stop();
        //LanguageProperty lp = LanguageProperty.getLanguagePropertyFromIndex(languageSelection);
        //LocaleUtil.changeLanguage(ContextUtil.getApplicationContext(), lp, false);
        return true;
    }

}
