package com.jht.chimera.ui.machinesettings.pages;

import static com.jht.chimera.lib.common.config.frames.FrameConfigEnum.Climbmill_Lifestyle;

import android.animation.Animator;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.recyclerview.widget.RecyclerView;

import com.jht.androidcommonalgorithms.math.ValueStepRange;
import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.SimpleInputConfiguration;
import com.jht.androiduiwidgets.edittext.IUserTextInputProperties;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.androiduiwidgets.selector.OptionManager;
import com.jht.androiduiwidgets.selector.OptionSelectorItem;
import com.jht.androiduiwidgets.selector.SelectorAdapterBase;
import com.jht.androiduiwidgets.selector.SelectorDefaultAnimations;
import com.jht.androiduiwidgets.selector.SelectorItemBase;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.data.RangeInfo;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfigEnum;
import com.jht.chimera.lib.common.conversionlib.ConversionHelper;
import com.jht.chimera.lib.common.conversionlib.SpeedConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.common.formatting.BasicNumberFormat;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.workout.SpecialWorkouts;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.FeaturedContentHelper;
import com.jht.chimera.lib.workout.workoutlist.workoutmap.WorkoutMap;
import com.jht.chimera.ui.machinesettings.adapters.specialworkouts.SpecialWorkoutSelectorItem;
import com.jht.chimera.ui.machinesettings.dialogs.ProjectDialogFactory;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.data.WorkoutItem;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.translations.LanguageHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Implements the eng_workout page gluing the page to the model layer.
 */
public class Workout extends SettingPageBase {
    private static final String TAG = "Workout";
    private final FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();

    private SelectorAdapterBase adapter;
    private RecyclerView table;
    private SelectorItemBase base;

    private View view;
    private boolean showSimple = false;

    private FeaturedContentHelper featuredContentHelper;
    private WorkoutMap workoutMap = WorkoutMap.Companion.createWorkoutMap();

    public Workout() {}

    public Workout(boolean showSimple) {
        this.showSimple = showSimple;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.eng_workout, null);
        container.addView(view);
        table = view.findViewById(R.id.special_workout_table);
        SelectorDefaultAnimations.setDefaultAnimations(context, table);
        featuredContentHelper = new FeaturedContentHelper(workoutMap);
        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);

        String strFrameType = DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING);
        FrameConfig frameConfig = FrameConfigManager.getInstance().getFrameConfig();
        FrameConfig stockConfig = FrameConfigEnum.getFrameTypeFromString(strFrameType).createFrameConfig();


        base = new SelectorItemBase(new ArrayList<>());
        for (SpecialWorkouts.SpecialWorkout workout : SpecialWorkouts.getSpecialWorkouts()) {
            if (DataMatrix.getBoolean(workout.key())) {
                addSpecialWorkoutToList(workout);
            }
        }
        adapter = new SelectorAdapterBase(getContext(), base, null);
        table.setAdapter(adapter);
        View addSpecialWorkoutButton = view.findViewById(R.id.add_special_workout);

        addSpecialWorkoutButton.setOnClickListener(v -> {
            SimpleInputConfiguration input = new SimpleInputConfiguration();
            input.isPassword(true);
            ProjectDialogFactory.keypadDialog(v, input, () -> {
                for (SpecialWorkouts.SpecialWorkout workout : SpecialWorkouts.getSpecialWorkouts()) {
                    if (workout.active()) {
                        if (input.value().equals(workout.keycode())) {
                            if (!DataMatrix.getBoolean(workout.key())) {
                                if (ProductConfig.isCommercial()) {
                                    if (!workout.keycode().equals(SpecialWorkouts.IFIT_KEYCODE)) {
                                        DataMatrix.putBoolean(workout.key(), true);
                                        addSpecialWorkoutToList(workout);
                                        adapter.notifyItemInserted(base.numChildren() - 1);
                                    }
                                } else {
                                    DataMatrix.putBoolean(workout.key(), true);
                                    addSpecialWorkoutToList(workout);
                                    adapter.notifyItemInserted(base.numChildren() - 1);
                                }
                            }
                        }
                    }
                }
            }, true);
        });

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_workout_time);
            userTextInput.setOnTextChanged(s -> {
                int value = CommonRoutines.parseInt(s);
                DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT, value);

                if (value < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT)) {
                    DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT, value);
                }
            });
            userTextInput.setInputProperties(userTextInput.getInputProperties().inputValue("" + DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_workout_resistance);
            Object key = config.isLifestyle ? SettingKeys.Workout.DATA_MAX_LIFESTYLE_CLIMBMILL_LEVEL_INT : SettingKeys.Workout.DATA_MAX_CLIMBMILL_LEVEL_INT;
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(key, CommonRoutines.parseInt(s)));
            userTextInput.setInputProperties(userTextInput.getInputProperties().max(config.resistance.getMaxModel()).inputValue("" + DataMatrix.getInt(key)));
            userTextInput.setVisibility(config.machineType.equals(JHTCommonTypes.MachineType.Climbmill) ? View.VISIBLE : View.GONE);
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_workout_calories);
            userTextInput.setOnTextChanged(s -> {
                int value = CommonRoutines.parseInt(s);
                DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT, CommonRoutines.parseInt(s));

                if (value < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT)) {
                    DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT, value);
                }
            });
            userTextInput.setInputProperties(userTextInput.getInputProperties().maxChars(String.format(Locale.ENGLISH, "%d", (int) config.workoutCalories.getMaxModel()).length()).max(config.workoutCalories.getMaxModel()).min(config.workoutCalories.getMinModel()).inputValue("" + DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT)));
            // userTextInput.setInputProperties(userTextInput.getInputProperties().inputValue("" + DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_workout_floors);
            userTextInput.setOnTextChanged(s -> {
                int value = CommonRoutines.parseInt(s);
                DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT, CommonRoutines.parseInt(s));

                if (value < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT)) {
                    DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT, value);
                }
            });
            userTextInput.setInputProperties(userTextInput.getInputProperties().max(config.workoutFloors.getMaxModel()).maxChars(String.format(Locale.ENGLISH, "%d", (int) config.workoutFloors.getMaxModel()).length()).min(config.workoutFloors.getMinModel()).inputValue("" + DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_workout_distance);
            String text = "" + CommonRoutines.round
                    (DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT)
                                        * CommonRoutines.getConvertFactorDistance(), 1);
            if (DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL)) {
                if (config.workoutDistanceKm.getMaxModel() < DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT)){
                    text = ""+config.workoutDistanceKm.getMaxModel();
                }
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.workoutDistanceKm.getMinModel())
                        .max(config.workoutDistanceKm.getMaxModel())
                        .maxChars(5)
                        .units(JHTCommonTypes.Unit.Metric)
                        .displayUnits(JHTCommonTypes.DisplayUnit.DISTANCE)
                        .resolution(0.1)
                        .inputValue(text));
            } else {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.workoutDistanceMi.getMinModel())
                        .max(config.workoutDistanceMi.getMaxModel())
                        .maxChars(4)
                        .units(JHTCommonTypes.Unit.Imperial)
                        .displayUnits(JHTCommonTypes.DisplayUnit.DISTANCE)
                        .resolution(0.1)
                        .inputValue(text));
            }
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorDistance();
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT, value);
                if (value < DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT)) {
                    DataMatrix.putFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT, value);
                }
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_pause_time);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Workout.DATA_PAUSE_TIME_MIN_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Workout.DATA_PAUSE_TIME_MIN_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_cool_down_time);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Workout.DATA_COOLDOWN_TIME_INT));
            userTextInput.setOnTextChanged(s -> {
                int coolDownTime = CommonRoutines.parseInt(s);
                UserTextInput maxAdjust = view.findViewById(R.id.eng_max_adjust_cool_down_time);
                maxAdjust.setInputProperties(maxAdjust.getInputProperties().min(coolDownTime));
                if (DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_COOLDOWN_TIME_INT) < coolDownTime) {
                    DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_COOLDOWN_TIME_INT, coolDownTime);
                    maxAdjust.setText("" + coolDownTime);
                }

                DataMatrix.putInt(SettingKeys.Workout.DATA_COOLDOWN_TIME_INT, CommonRoutines.parseInt(s));
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_adjust_cool_down_time);
            userTextInput.setInputProperties(userTextInput.getInputProperties().min(DataMatrix.getInt(SettingKeys.Workout.DATA_COOLDOWN_TIME_INT)));
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_COOLDOWN_TIME_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Workout.DATA_MAX_COOLDOWN_TIME_INT, CommonRoutines.parseInt(s)));
        }


        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_start_speed);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .units(CommonRoutines.getUnit())
                    .min(stockConfig.speedKph.getMin() * CommonRoutines.getConvertFactorSpeed())
                    .max(2.3 * CommonRoutines.getConvertFactorSpeed())
                    .resolution(0.1));
            String text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT) * CommonRoutines.getConvertFactorSpeed(), 1);
            userTextInput.setText(text);
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorSpeed();
                DataMatrix.putFloat(SettingKeys.Workout.DATA_START_SPEED_KPH_FLOAT, value);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_max_speed);
            boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
            RangeInfo speedRange = isMetric ? stockConfig.speedKph : stockConfig.speedMph;
            RangeInfo currentSpeedRange = isMetric ? frameConfig.speedKph : frameConfig.speedMph;
            IUserTextInputProperties textInput = userTextInput.getInputProperties()
                    .units(CommonRoutines.getUnit())
                    .resolution(0.1)
                    .min(3.2 * CommonRoutines.getConvertFactorSpeed())
                    .max(speedRange.getMaxModel());
            userTextInput.setInputProperties(textInput);

            String text = BasicNumberFormat.format(currentSpeedRange.getMax(), 1);
            Log.e(TAG, "speed: " + currentSpeedRange.getMax() + ", other: " + text);
            userTextInput.setText(text);
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s);
                updateSpeedValue(stockConfig, value, isMetric);
            });
        }

        {
            UserTextInput maxIncline = view.findViewById(R.id.eng_max_incline);
            ValueStepRange range = new ValueStepRange(config.inclineLimit.getMinModel(), config.inclineLimit.getMaxModel(), config.inclineLimit.stepSize);
            double resolution = Math.pow(10, -stockConfig.inclineLimit.precision);
            maxIncline.setInputProperties(maxIncline.getInputProperties()
                    .units(CommonRoutines.getUnit())
                    .min(stockConfig.inclineLimit.getMinModel())
                    .max(stockConfig.inclineLimit.getMaxModel())
                    .maxChars(resolution < 1 ? 4 : 3)
                    .resolution(resolution)
                    .inputType(resolution < 1 ? UserTextInput.InputType.DOUBLE : UserTextInput.InputType.INTEGER));
            maxIncline.setText(LanguageHelper.getDecimalFormat(resolution).format(DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT)));
            maxIncline.setOnTextChanged(s -> {
                double value = range.fix(CommonRoutines.parseFloat(s));
                String realValue = LanguageHelper.getDecimalFormat(resolution).format(value);

                if(!realValue.equals(s)) {
                    maxIncline.post(() -> maxIncline.setText(realValue));
                }
                else {
                    DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_INCLINE_FLOAT, (float) value);
                }
            });
        }
        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_start_speed_reverse);
            if (userTextInput != null) {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .units(CommonRoutines.getUnit())
                        .min(stockConfig.speedKph.getMin() * CommonRoutines.getConvertFactorSpeed())
                        .max(2.3 * CommonRoutines.getConvertFactorSpeed())
                        .resolution(0.1));
                String text = "" + CommonRoutines.round(0.16f * CommonRoutines.getConvertFactorSpeed(), 1); // start speed reverse is non-editable and static 0.1 mph speed
                userTextInput.setText(text);
                userTextInput.disable();
                if (!frameConfig.TM_SUPPORTS_REVERSE_BELT) {
                    userTextInput.setVisibility(View.GONE);
                }
            }
        }

            {
                UserTextInput userTextInput = view.findViewById(R.id.eng_max_speed_reverse);
                if (userTextInput != null) {
                    boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);
                    RangeInfo speedRange = isMetric ? stockConfig.speedReverseKph : stockConfig.speedReverseMph;
                    Float settingValue = isMetric ? DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_REVERSE_FLOAT) :
                            DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_REVERSE_FLOAT);
                    IUserTextInputProperties textInput = userTextInput.getInputProperties()
                            .units(CommonRoutines.getUnit())
                            .resolution(0.1)
                            .min(3.2 * CommonRoutines.getConvertFactorSpeed())
                            .max(speedRange.getMax());
                    userTextInput.setInputProperties(textInput);
                    String text = BasicNumberFormat.format(settingValue, 1);
                    Log.e(TAG, "reverse speed: " + speedRange + ", other: " + text);
                    userTextInput.setText(text);
                    userTextInput.setOnTextChanged(s -> {
                        float value = CommonRoutines.parseFloat(s);
                        updateReverseSpeedValue(stockConfig, value, isMetric);
                    });
                    if (!frameConfig.TM_SUPPORTS_REVERSE_BELT) {
                        userTextInput.setVisibility(View.GONE);
                    }
                }
            }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_keyboard_threshold_tm);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .units(CommonRoutines.getUnit())
                    .min(stockConfig.speedKph.getMin() * CommonRoutines.getConvertFactorSpeed())
                    .max(6.4 * CommonRoutines.getConvertFactorSpeed())
                    .resolution(0.1));
            String text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.Workout.DATA_KEYBOARD_DISABLE_THRESHOLD_SPEED_KPH_FLOAT) * CommonRoutines.getConvertFactorSpeed(), 1);
            userTextInput.setText(text);
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorSpeed();
                DataMatrix.putFloat(SettingKeys.Workout.DATA_KEYBOARD_DISABLE_THRESHOLD_SPEED_KPH_FLOAT, value);
            });
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_elevation_above_5000_ft);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Workout.DATA_ELEVATION_ABOVE_5000_FT_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Workout.DATA_ELEVATION_ABOVE_5000_FT_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.show_marathon_mode);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Workout.DATA_SHOW_MARATHON_MODE_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Workout.DATA_SHOW_MARATHON_MODE_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_tread_sense);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Workout.DATA_TREAD_SENSE_ENABLED_BOOL, isChecked));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_tread_sense_timeout);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Workout.DATA_TREAD_SENSE_TIMEOUT_SEC_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Workout.DATA_TREAD_SENSE_TIMEOUT_SEC_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_tread_sense_countdown);
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.Workout.DATA_TREAD_SENSE_COUNTDOWN_SEC_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.Workout.DATA_TREAD_SENSE_COUNTDOWN_SEC_INT, CommonRoutines.parseInt(s)));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_estop_enable);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Workout.DATA_CM_ESTOP_ENABLE_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Workout.DATA_CM_ESTOP_ENABLE_BOOL, isChecked));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_detect_person);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.Workout.DATA_CM_DETECT_PERSON_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.Workout.DATA_CM_DETECT_PERSON_BOOL, isChecked));
        }

        updateFeaturedWorkoutSetting();

        findViewById(R.id.eng_max_workout_floors).setVisibility(View.GONE);
        findViewById(R.id.eng_max_workout_distance).setVisibility(View.GONE);
        findViewById(R.id.eng_max_incline).setVisibility(View.GONE);
        findViewById(R.id.eng_start_speed).setVisibility(View.GONE);
        findViewById(R.id.eng_max_speed).setVisibility(View.GONE);
        findViewById(R.id.eng_tread_sense).setVisibility(View.GONE);
        findViewById(R.id.eng_tread_ascent_settings_linebreak).setVisibility(View.GONE);
        findViewById(R.id.eng_tread_settings_linebreak).setVisibility(View.GONE);
        findViewById(R.id.eng_tread_sense_timeout).setVisibility(View.GONE);
        findViewById(R.id.eng_tread_sense_countdown).setVisibility(View.GONE);
        findViewById(R.id.eng_keyboard_threshold_tm).setVisibility(View.GONE);
        findViewById(R.id.eng_cm_settings_linebreak).setVisibility(View.GONE);
        findViewById(R.id.eng_estop_enable).setVisibility(View.GONE);
        //findViewById(R.id.eng_estop_sensitivity).setVisibility(View.GONE);
        findViewById(R.id.eng_detect_person).setVisibility(View.GONE);


        switch (config.machineClassType) {

            case Treadmill:
                findViewById(R.id.eng_keyboard_threshold_tm).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_max_workout_distance).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_start_speed).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_max_speed).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_max_incline).setVisibility(View.VISIBLE);

                findViewById(R.id.eng_tread_sense).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tread_ascent_settings_linebreak).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tread_settings_linebreak).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tread_sense_timeout).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_tread_sense_countdown).setVisibility(View.VISIBLE);


                break;
            case Ascent:
                findViewById(R.id.eng_tread_ascent_settings_linebreak).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_max_incline).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_max_workout_distance).setVisibility(View.VISIBLE);
                view.findViewById(R.id.eng_elevation_above_5000_ft).setVisibility(View.GONE);
                break;
            case Stepper:
                findViewById(R.id.eng_max_workout_floors).setVisibility(View.VISIBLE);
                view.findViewById(R.id.eng_elevation_above_5000_ft).setVisibility(View.GONE);
                break;
            case Climbmill:
                findViewById(R.id.eng_max_workout_floors).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_cm_settings_linebreak).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_estop_enable).setVisibility(!FrameConfigEnum.getFrameTypeFromString(DataMatrix.getString(SettingKeys.General.DATA_FRAME_TYPE_STRING)).equals(Climbmill_Lifestyle) ? View.VISIBLE : View.GONE);
                //findViewById(R.id.eng_estop_sensitivity).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_detect_person).setVisibility(View.VISIBLE);
                findViewById(R.id.eng_speed_incline_settings_linebreak).setVisibility(View.GONE);
                view.findViewById(R.id.eng_elevation_above_5000_ft).setVisibility(View.GONE);

                if (ProductConfig.isRetail()) {
                    if (showSimple) {
                        findViewById(R.id.eng_cm_settings_linebreak).setVisibility(View.GONE);
                        findViewById(R.id.eng_estop_enable).setVisibility(View.GONE);
                    }
                    findViewById(R.id.eng_detect_person).setVisibility(View.GONE);
                }
                break;
            default:
                findViewById(R.id.eng_max_workout_distance).setVisibility(View.VISIBLE);
                view.findViewById(R.id.eng_elevation_above_5000_ft).setVisibility(View.GONE);
                break;
        }

        if (ProductConfig.isRetail()) {
            if (showSimple) {
                //findViewById(R.id.eng_max_workout_time).setVisibility(View.GONE);
                //findViewById(R.id.eng_max_workout_calories).setVisibility(View.GONE);
                //findViewById(R.id.eng_max_workout_distance).setVisibility(View.GONE);
                findViewById(R.id.eng_max_workout_floors).setVisibility(View.GONE);
                findViewById(R.id.eng_max_workout_resistance).setVisibility(View.GONE);
                findViewById(R.id.eng_max_incline).setVisibility(View.GONE);
                findViewById(R.id.eng_tread_ascent_settings_linebreak).setVisibility(View.GONE);
                findViewById(R.id.eng_start_speed).setVisibility(View.GONE);
                findViewById(R.id.eng_max_speed).setVisibility(View.GONE);
                findViewById(R.id.eng_max_incline).setVisibility(View.GONE);
            }

            findViewById(R.id.eng_max_adjust_cool_down_time).setVisibility(View.GONE);
            findViewById(R.id.eng_tread_sense).setVisibility(View.GONE);
            findViewById(R.id.eng_tread_ascent_settings_linebreak).setVisibility(View.GONE);
            findViewById(R.id.eng_tread_settings_linebreak).setVisibility(View.GONE);
            findViewById(R.id.eng_tread_sense_timeout).setVisibility(View.GONE);
            findViewById(R.id.eng_tread_sense_countdown).setVisibility(View.GONE);
            setVisible(findViewById(R.id.eng_start_speed_reverse), View.GONE);
            setVisible(findViewById(R.id.eng_max_speed_reverse), View.GONE);
        }
    }

    private void addSpecialWorkoutToList(SpecialWorkouts.SpecialWorkout workout) {
        base.addItem(new SpecialWorkoutSelectorItem(workout, item -> {
            base.removeItem(item.getIndex());
            adapter.notifyItemRemoved(item.getIndex());
            updateFeaturedWorkoutSetting();
        }));
        updateFeaturedWorkoutSetting();
    }

    private void updateFeaturedWorkoutSetting() {
        if (ProductConfig.isRetail()) {
            // Parker does not need to handle this logic
            return;
        }

        //Reinit the carousel options to cover adding and removing of special workouts on this screen
        featuredContentHelper.init(workoutMap);
        List<Integer> carouselOptions = featuredContentHelper.getCarouselWorkoutOptionIds();
        List<Integer> selectedWorkouts = featuredContentHelper.getCarouselWorkoutIds();

        if (selectedWorkouts.size() != 3) {
            Log.e("Workout", "error, we only support 3 workouts here in the settings screen right now!");
            return;
        }

        List<OptionSelectorItem> optionSelectorItems = new ArrayList<>();
        optionSelectorItems.add(view.findViewById(R.id.eng_featured_workout_selector_1));
        optionSelectorItems.add(view.findViewById(R.id.eng_featured_workout_selector_2));
        optionSelectorItems.add(view.findViewById(R.id.eng_featured_workout_selector_3));

        // We currently always have three selected workouts
        for (int i = 0; i < 3; i++) {
            OptionSelectorItem optionSelector = optionSelectorItems.get(i);
            int featuredWorkoutId = selectedWorkouts.get(i);

            // Populate necessary information for the option selector
            String[] optionTitles = new String[carouselOptions.size()];
            int selectedIndex = carouselOptions.indexOf(featuredWorkoutId);
            for (int j = 0; j < carouselOptions.size(); j++) {
                WorkoutItem workout = workoutMap.getWorkout(carouselOptions.get(j));
                if (workout != null) {
                    optionTitles[j] = StringLookup.get(workout.getTitleId());
                }
            }

            optionSelector.setOptions(new OptionManager(optionTitles, selectedIndex));

            int index = i;
            optionSelector.setOnOptionSelected((pos, option) -> {
                setFeaturedWorkout(index, carouselOptions.get(pos));
            });
        }
    }

    private void setFeaturedWorkout(int panel, int selectedId) {
        List<Integer> featuredItems = featuredContentHelper.getCarouselWorkoutIds();
        if (panel <= featuredItems.size()) {
            featuredItems.set(panel, selectedId);
            featuredContentHelper.setCarouselWorkouts(featuredItems);
        }
    }

    private void updateSpeedValue(FrameConfig stockConfig, float value, boolean isMetric) {
        if (isMetric) {

            float mph = SpeedConvertUtil.convertKphToMph(value);
            Log.e(TAG, "mph: " + mph);
            float mphRounded = ConversionHelper.round(mph, 1);
            Log.e(TAG, "mphRounded: " + mphRounded);
            float kphRounded = ConversionHelper.round(value, 1);

            Log.e(TAG, "stockConfig speedMPH max: " + stockConfig.speedMph.getMaxModel());
            if (mphRounded >= stockConfig.speedMph.getMaxModel()) {
                mphRounded = stockConfig.speedMph.getMaxModel();
            }
            Log.e(TAG, "METRIC.  KPH: " + kphRounded + ", MPH: " + mphRounded);
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT, kphRounded);
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT, mphRounded);
        } else {
            float kph = SpeedConvertUtil.convertMphToKph(value);
            float mphRounded = ConversionHelper.round(value, 1);
            float kphRounded = ConversionHelper.round(kph, 1);
            Log.e(TAG, "stockConfig speedKPH max: " + stockConfig.speedKph.getMaxModel());
            if (kphRounded >= stockConfig.speedKph.getMaxModel()) {
                kphRounded = stockConfig.speedKph.getMaxModel();
            }

            Log.e(TAG, "IMPERIAL.  KPH: " + kphRounded + ", MPH: " + mphRounded);
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_FLOAT, kphRounded);
                DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_FLOAT, mphRounded);
        }
    }

    private void updateReverseSpeedValue(FrameConfig stockConfig, float value, boolean isMetric) {
        if (isMetric) {
            float mph = SpeedConvertUtil.convertKphToMph(value);
            Log.e(TAG, "mph: " + mph);
            float mphRounded = ConversionHelper.round(mph, 1);
            Log.e(TAG, "mphRounded: " + mphRounded);
            float kphRounded = ConversionHelper.round(value, 1);

            Log.e(TAG, "stockConfig speedMPH max: " + stockConfig.speedReverseMph.getMaxModel());
            if (mphRounded >= stockConfig.speedReverseMph.getMaxModel()) {
                mphRounded = stockConfig.speedReverseMph.getMaxModel();
            }
            Log.e(TAG, "METRIC.  KPH: " + kphRounded + ", MPH: " + mphRounded);
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_REVERSE_FLOAT, kphRounded);
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_REVERSE_FLOAT, mphRounded);
        } else {
            float kph = SpeedConvertUtil.convertMphToKph(value);
            float mphRounded = ConversionHelper.round(value, 1);
            float kphRounded = ConversionHelper.round(kph, 1);
            Log.e(TAG, "stockConfig speedKPH max: " + stockConfig.speedReverseKph.getMaxModel());
            if (kphRounded >= stockConfig.speedReverseKph.getMaxModel()) {
                kphRounded = stockConfig.speedReverseKph.getMaxModel();
            }

            Log.e(TAG, "IMPERIAL.  KPH: " + kphRounded + ", MPH: " + mphRounded);
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_KPH_REVERSE_FLOAT, kphRounded);
            DataMatrix.putFloat(SettingKeys.Workout.DATA_MAX_SPEED_MPH_REVERSE_FLOAT, mphRounded);

        }
    }

    @Override
    public void hide(final Animator.AnimatorListener listener, boolean start) {
        super.hide(listener, start);

        // clamp the default workout setting to the max in case the max have changed
        if (DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT) < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT)) {
            DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT, DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT));
        }
        if (DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT) < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT)) {
            DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT, DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT));
        }
        if (DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT) < DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT)) {
            DataMatrix.putFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT, DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT));
        }
        if (DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT) < DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT)) {
            DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT, DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT));
        }

    }


}
