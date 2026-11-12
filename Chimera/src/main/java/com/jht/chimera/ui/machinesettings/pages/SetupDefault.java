package com.jht.chimera.ui.machinesettings.pages;

import android.animation.Animator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.jht.androidcommonalgorithms.type.JHTCommonTypes;
import com.jht.androiduiwidgets.edittext.UserTextInput;
import com.jht.chimera.R;
import com.jht.chimera.lib.common.config.FrameConfigManager;
import com.jht.chimera.lib.common.config.ProductConfig;
import com.jht.chimera.lib.common.config.frames.FrameConfig;
import com.jht.chimera.lib.common.conversionlib.WeightConvertUtil;
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys;
import com.jht.chimera.lib.common.datamatrixkeys.UiServiceKeys;
import com.jht.chimera.lib.language.StringLookup;
import com.jht.chimera.lib.widget.CommonRoutines;
import com.jht.chimera.lib.widget.radiogroup.RadioGroup;
import com.jht.chimera.ui.machinesettings.viewpager.SettingPageBase;
import com.jht.datamatrixlib.DataMatrix;
import com.jht.translations.LanguageHelper;


/**
 * Implements the setup defaults page gluing the page to the model layer.
 */
public class SetupDefault extends SettingPageBase {

    private FrameConfig config = FrameConfigManager.getInstance().getFrameConfig();
    private View view;
    private boolean showSimple = false;

    public SetupDefault() {
    }

    public SetupDefault(boolean showSimple) {
        this.showSimple = showSimple;
    }

    @Override
    protected View onCreate(Context context, ViewGroup container) {
        view = LayoutInflater.from(context).inflate(R.layout.setup_default, null);
        container.addView(view);
        return view;
    }


    @Override
    public void show(final Animator.AnimatorListener listener, boolean start) {
        super.show(listener, start);
        boolean isMetric = DataMatrix.getBoolean(UiServiceKeys.DATA_DISPLAY_METRIC_VALUES_BOOL);

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_weight);
            if(isMetric) {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.weightKg.getMin())
                        .max(config.weightKg.getMax())
                        .units(CommonRoutines.getUnit())
                        .resolution(1.0)
                        .inputValue(LanguageHelper.getDecimalFormat(1.0).format(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT)))
                );
            }
            else {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.weightLbs.getMin())
                        .max(config.weightLbs.getMax())
                        .units(CommonRoutines.getUnit())
                        .resolution(1.0)
                        .inputValue(LanguageHelper.getDecimalFormat(1.0).format(WeightConvertUtil.convertKilogramsToPounds(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT))))
                );

            }
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorWeight();
                DataMatrix.putFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WEIGHT_KG_FLOAT, value);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_height);
            if(isMetric) {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.heightCm.getMin())
                        .max(config.heightCm.getMax())
                        .maxChars(3)
                        .units(CommonRoutines.getUnit())
                        .inputValue(LanguageHelper.getDecimalFormat(1.0).format(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_HEIGHT_CM_FLOAT))));
            }
            else {
                userTextInput.setInputProperties(userTextInput.getInputProperties()
                        .min(config.heightIn.getMin())
                        .max(config.heightIn.getMax())
                        .maxChars(2)
                        .units(CommonRoutines.getUnit())
                        .inputValue(LanguageHelper.getDecimalFormat(1.0).format(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_HEIGHT_CM_FLOAT) * CommonRoutines.getConvertFactorHeight())));

            }
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorHeight();
                DataMatrix.putFloat(SettingKeys.SetupDefault.DATA_DEFAULT_HEIGHT_CM_FLOAT, value);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_workout_time);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .min(config.workoutTime.getMin())
                    .max(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_TIME_MIN_INT)));
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_TIME_MIN_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_workout_calories);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .min(config.workoutCalories.getMin())
                    .max(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_CALORIES_INT)));
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_CALORIES_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_workout_floors);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .min(config.workoutFloors.getMin())
                    .max(DataMatrix.getInt(SettingKeys.Workout.DATA_MAX_WORKOUT_FLOORS_INT)));
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_FLOORS_INT, CommonRoutines.parseInt(s)));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_workout_distance);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .min(config.workoutDistanceKm.getMin() * CommonRoutines.getConvertFactorDistance())
                    .max(DataMatrix.getFloat(SettingKeys.Workout.DATA_MAX_WORKOUT_DISTANCE_KM_FLOAT) * CommonRoutines.getConvertFactorDistance())
                    .units(CommonRoutines.getUnit()));
            String text = "" + CommonRoutines.round(DataMatrix.getFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT) * CommonRoutines.getConvertFactorDistance(), 1);
            userTextInput.setText(text);
            userTextInput.setOnTextChanged(s -> {
                float value = CommonRoutines.parseFloat(s) / CommonRoutines.getConvertFactorDistance();
                DataMatrix.putFloat(SettingKeys.SetupDefault.DATA_DEFAULT_WORKOUT_DISTANCE_KM_FLOAT, value);
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_age);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .min(config.age.getMin())
                    .max(config.age.getMax()));
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_AGE_INT, CommonRoutines.parseInt(s)));
        }

        {
            // the default value for either resistance or workout program level difficulty pillbox
            // level is not necessarily directly resistance, flowchart indicates this setting defaults 1-20 regardless of frame
            // discussion here> https://bitbucket.org/johnsonhealthtech/chimera2/pull-requests/1654/parker2-pulling-in-vision/diff
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_level);
            userTextInput.setInputProperties(userTextInput.getInputProperties()
                    .min(1)
                    .max(20));
            userTextInput.setText("" + DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT));
            userTextInput.setOnTextChanged(s -> DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_LEVEL_INT, CommonRoutines.parseInt(s)));

            if (ProductConfig.isRetail()) {
                // change the title to use resistance level
                String title = StringLookup.get(R.string.resistance_level_tc).replace(":", "");
                userTextInput.setInputProperties(userTextInput.getInputProperties().label(title));
            }
        }

        {
            String[] list = {StringLookup.get(R.string.male_tc), StringLookup.get(R.string.female_tc), StringLookup.get (R.string.non_binary_gender_tc)};
            RadioGroup radioGroup = view.findViewById(R.id.eng_default_gender);
            radioGroup.linkField(DataMatrix.getInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT), list, LinearLayout.HORIZONTAL);
            radioGroup.setOnOptionChanged(index -> DataMatrix.putInt(SettingKeys.SetupDefault.DATA_DEFAULT_GENDER_ENUM_INT, index));
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_use_baidu);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_USE_BAIDU_BOOL));
            ((UserTextInput) findViewById(R.id.eng_default_web_page)).setReadOnly(checkBox.isChecked());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DataMatrix.putBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_USE_BAIDU_BOOL, isChecked);
                UserTextInput userTextInput = view.findViewById(R.id.eng_default_web_page);
                if (isChecked) {
                    DataMatrix.putString(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING, "www.baidu.com"); //NON-NLS
                    userTextInput.setReadOnly(true);

                } else {
                    DataMatrix.putString(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING, StringLookup.get(R.string.default_web_page)); //NON-NLS
                    userTextInput.setReadOnly(false);
                }
                userTextInput.setText(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING));
            });
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_treo_web_page);
            if (userTextInput != null) {
                userTextInput.setText(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_TREO_URL_STRING));
                userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.SetupDefault.DATA_DEFAULT_TREO_URL_STRING, s));
            }
        }

        {
            CheckBox checkBox = view.findViewById(R.id.eng_survey_post_workout);
            checkBox.setChecked(DataMatrix.getBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> DataMatrix.putBoolean(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_BOOL, isChecked));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_default_web_page);
            userTextInput.setText(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.SetupDefault.DATA_DEFAULT_WEBPAGE_STRING, s));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_survey_mid_workout_URL);
            userTextInput.setText(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.SetupDefault.DATA_DEFAULT_MID_WORKOUT_SURVEY_URL_STRING, s));
        }

        {
            UserTextInput userTextInput = view.findViewById(R.id.eng_survey_post_workout_URL);
            userTextInput.setText(DataMatrix.getString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING));
            userTextInput.setOnTextChanged(s -> DataMatrix.putString(SettingKeys.SetupDefault.DATA_DEFAULT_POST_WORKOUT_SURVEY_URL_STRING, s));
        }


        if (config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Stepper) ||
                config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill) ) {
            findViewById(R.id.eng_default_workout_distance).setVisibility(View.GONE);
            findViewById(R.id.eng_default_workout_floors).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.eng_default_workout_distance).setVisibility(View.VISIBLE);
            findViewById(R.id.eng_default_workout_floors).setVisibility(View.GONE);
        }

        if (ProductConfig.isRetail()) {
            // hide default level for treadmill and climbmill
            if (config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Treadmill) ||
                    config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)) {
                findViewById(R.id.eng_default_level).setVisibility(View.GONE);
            }
        } else {
            findViewById(R.id.eng_default_level).setVisibility(config.machineClassType.equals(JHTCommonTypes.MachineClassificationType.Climbmill)  ? View.GONE : View.VISIBLE);
        }

        if (showSimple) {
            findViewById(R.id.eng_default_workout_distance).setVisibility(View.GONE);
            findViewById(R.id.eng_default_workout_floors).setVisibility(View.GONE);
            findViewById(R.id.eng_default_workout_time).setVisibility(View.GONE);
            findViewById(R.id.eng_default_workout_calories).setVisibility(View.GONE);
            findViewById(R.id.eng_default_level).setVisibility(View.GONE);
        }
    }

}
