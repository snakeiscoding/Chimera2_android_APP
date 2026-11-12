package com.jht.chimera.ui.machinesettings.pages

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jht.androiduiwidgets.edittext.SimpleUserTextInputProperties
import com.jht.androiduiwidgets.edittext.UserTextInput
import com.jht.chimera.R
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys
import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.ProjectOne
import com.jht.chimera.ui.machinesettings.LEDColorRecyclerAdapter
import com.jht.datamatrixlib.DataMatrix
import com.jht.mastboard.data.LEDItem
import com.jht.mastboard.data.LEDItem.LEDGroup
import com.jht.mastboard.data.LedZone
import com.jht.mastboard.data.UiEventKeys
import com.jht.mastboard.device.I2cBuzzer
import com.jht.mastboard.lib.HapticMotorEvent
import com.jht.widgets.colorpicker.setContent

/**
 * ProjectOne (Onyx) specific additions to the shared Hardware machine settings page.
 *
 * Every view driven from here lives in main/res/layout/hardware.xml and is declared
 * android:visibility="gone" there, so it costs the other flavors nothing. This object makes
 * those views visible and wires them up, and is called from [Hardware.show] only when
 * ProductConfig.isProjectOne() is true.
 *
 * This previously lived in src/projectone as a Hardware subclass, which forced
 * EngineeringScreen to be forked per flavor just so the unqualified name `Hardware` would
 * resolve to the subclass. See CGC ticket for the MES consolidation.
 */
object ProjectOneHardwareExtras {

    /**
     * Preview colour pushed to the frame while the brightness slider is being dragged.
     * Carried over from the original EngineeringDebug implementation, which hardcoded it too.
     */
    private const val LED_PREVIEW_COLOR = 0xFF6C01

    @JvmStatic
    fun apply(context: Context, view: View) {
        hideHeadphoneInsertAStats(view)
        initHapticFeedbackControls(context, view)
        initializeColorPicker(context, view)
    }

    /**
     * Hides the statistics related to the headphone insert A. This is specific to the Onyx platform.
     */
    private fun hideHeadphoneInsertAStats(view: View) {
        view.findViewById<View>(R.id.eng_hardware_headphone_insert_a_label).visibility = View.GONE
        view.findViewById<View>(R.id.eng_hardware_headphone_insert_a).visibility = View.GONE
        view.findViewById<View>(R.id.eng_hardware_insert_a_reset).visibility = View.GONE
    }

    /**
     * Initializes and displays the haptic feedback controls for the Onyx platform.
     */
    private fun initHapticFeedbackControls(context: Context, view: View) {
        // Store references to necessary views
        val hapticsLineBreak: RelativeLayout = view.findViewById(R.id.eng_hardware_haptic_feedback_line_break)
        val enableHapticsCheckBox: CheckBox = view.findViewById(R.id.eng_hardware_haptic_enable_checkbox)
        val hapticsEffectInput: UserTextInput = view.findViewById(R.id.eng_hardware_haptic_effect_input)
        val buzzHapticsBtnLayout: LinearLayout = view.findViewById(R.id.eng_hardware_haptic_btn_layout)
        val buzzHapticsBtn: Button = view.findViewById(R.id.eng_hardware_haptic_btn)

        // Init haptic feedback enable / disable checkbox
        val hapticsEnabled = DataMatrix.getBoolean(SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL)
        enableHapticsCheckBox.isChecked = hapticsEnabled
        enableHapticsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            hapticsEffectInput.clearFocus()
            hapticsEffectInput.isEnabled = isChecked
            buzzHapticsBtn.isEnabled = isChecked
            DataMatrix.putBoolean(SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_ENABLED_ON_SYSTEM_BOOL, isChecked)
        }

        // Construct default parameters for haptic effect ID input
        val hapticEffectProperties = SimpleUserTextInputProperties()
            .label(context.getString(R.string.haptic_effect_tc))
            .hint(context.getString(R.string.effect_id_no_tc))
            .inputType(UserTextInput.InputType.INTEGER)
            .min(I2cBuzzer.MIN_EFFECT_IDENTIFIER.toDouble())
            .max(I2cBuzzer.MAX_EFFECT_IDENTIFIER.toDouble())
            .minChars(I2cBuzzer.MIN_EFFECT_IDENTIFIER.toString().length)
            .maxChars(I2cBuzzer.MAX_EFFECT_IDENTIFIER.toString().length)
            .autoCorrect(true)

        // Init haptic effect ID input
        hapticsEffectInput.inputProperties = hapticEffectProperties
        hapticsEffectInput.text = DataMatrix.getInt(SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_EFFECT_ID_INT).toString()
        hapticsEffectInput.isEnabled = hapticsEnabled
        hapticsEffectInput.setOnTextChanged { hapticEffectText ->
            val validEffectIDRange = I2cBuzzer.MIN_EFFECT_IDENTIFIER..I2cBuzzer.MAX_EFFECT_IDENTIFIER
            val parsedEffectID = hapticEffectText.toIntOrNull()
            val newEffectID = if (parsedEffectID in validEffectIDRange) parsedEffectID else null
            newEffectID?.let {
                DataMatrix.putInt(SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_EFFECT_ID_INT, it)
            }
        }

        // Init haptic buzz button
        buzzHapticsBtn.isEnabled = hapticsEnabled
        buzzHapticsBtn.setOnClickListener {
            val validEffectIDRange = I2cBuzzer.MIN_EFFECT_IDENTIFIER..I2cBuzzer.MAX_EFFECT_IDENTIFIER
            val parsedEffectID = hapticsEffectInput.text.toIntOrNull()
            val newEffectID = if (parsedEffectID in validEffectIDRange) parsedEffectID else null
            hapticsEffectInput.clearFocus()
            newEffectID?.let {
                DataMatrix.putInt(SettingKeys.ProjectOne.DATA_HAPTIC_MOTOR_EFFECT_ID_INT, it)
                DataMatrix.putParcelable(SettingKeys.ProjectOne.ACTION_HAPTIC_MOTOR_EVENT_PARCELABLE, HapticMotorEvent.BuzzAllPanels)
            }
        }

        // Make all necessary views visible
        hapticsLineBreak.visibility = View.VISIBLE
        enableHapticsCheckBox.visibility = View.VISIBLE
        hapticsEffectInput.visibility = View.VISIBLE
        buzzHapticsBtnLayout.visibility = View.VISIBLE
        buzzHapticsBtn.visibility = View.VISIBLE
    }

    /**
     * Shows the color picker related to the LED lights on the frame. This is specific to the Onyx platform.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun initializeColorPicker(context: Context, view: View) {
        // show the color picker (hidden by default, since only on onyx)
        view.findViewById<ConstraintLayout>(R.id.color_picker_dialog_container).visibility = View.VISIBLE
        // LED Setup
        val recyclerView = view.findViewById<RecyclerView>(R.id.led_color_selection_recycler_view)
        val composeView = view.findViewById<ComposeView>(R.id.color_picker)
        val ledItems = ArrayList<LEDItem>()
        ledItems.add(LEDItem(LEDGroup.Default))
        ledItems.add(LEDItem(LEDGroup.WorkoutRunning))
        ledItems.add(LEDItem(LEDGroup.WorkoutPaused))
        ledItems.add(LEDItem(LEDGroup.WorkoutSummary))
        // Only show option to configure Concierge LED if it's enabled
        if (DataMatrix.getBoolean(SettingKeys.AssetManagement.DATA_ENABLE_CONCIERGE_BOOL)) {
            ledItems.add(LEDItem(LEDGroup.Concierge))
        }
        val mLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        lateinit var ledColorRecyclerAdapter: LEDColorRecyclerAdapter
        ledColorRecyclerAdapter = LEDColorRecyclerAdapter(context, ledItems) { ledItem: LEDItem ->
            setContent(composeView, ledItem.color, true) { newColor: Int ->
                val position: Int
                when (ledItem.group) {
                    LEDGroup.WorkoutRunning -> {
                        position = 1
                        DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_WORKOUT_RUNNING_COLOR_HEX_INT, newColor)
                    }
                    LEDGroup.WorkoutPaused -> {
                        position = 2
                        DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_WORKOUT_PAUSED_COLOR_HEX_INT, newColor)
                    }
                    LEDGroup.WorkoutSummary -> {
                        position = 3
                        DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_WORKOUT_SUMMARY_COLOR_HEX_INT, newColor)
                    }
                    LEDGroup.Concierge -> {
                        position = 4
                        DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT, newColor)
                    }
                    // Android studio wants an else statement in case the java code produces a null
                    // really it's just LEDGroup.Default
                    else -> {
                        position = 0
                        DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT, newColor)
                    }
                }
                ledItem.color = newColor
                // Show the color on the machine as the user selects it.  UI State will change color back to what it should be when leaving MES
                DataMatrix.putParcelable(ProjectOne.ACTION_LED_COLOR_HEX_PARCELABLE, LEDItem(newColor, LedZone.ALL_EXCEPT_PLAY_STOP))
                ledColorRecyclerAdapter.notifyItemChanged(position)
            }
        }
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.adapter = ledColorRecyclerAdapter
        val resetLEDColorsButton = view.findViewById<Button>(R.id.reset_LED_colors_button)
        resetLEDColorsButton.setOnClickListener {
            DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_DEFAULT_COLOR_HEX_INT, 0xff0000ff.toInt())
            DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_WORKOUT_RUNNING_COLOR_HEX_INT, 0xff00ff00.toInt())
            DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_WORKOUT_PAUSED_COLOR_HEX_INT, 0xff0000ff.toInt())
            DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_WORKOUT_SUMMARY_COLOR_HEX_INT, 0xffff0000.toInt())
            DataMatrix.putInt(SettingKeys.Experimental.DATA_LED_CONCIERGE_COLOR_HEX_INT, 0xffffff00.toInt())
            ledColorRecyclerAdapter.notifyDataSetChanged()
        }
        DataMatrix.putString(UiEventKeys.Key.DATA_UI_EVENT_STRING, UiEventKeys.EVENT.MES.name)
        val seekBarLEDBrightness = view.findViewById<SeekBar>(R.id.seekBarBrightnessLED)
        seekBarLEDBrightness.progress = DataMatrix.getInt(ProjectOne.DATA_LED_BRIGHTNESS_INT)
        seekBarLEDBrightness.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                DataMatrix.putInt(ProjectOne.DATA_LED_BRIGHTNESS_INT, seekBar.progress)
                DataMatrix.putParcelable(ProjectOne.ACTION_LED_COLOR_HEX_PARCELABLE, LEDItem(LED_PREVIEW_COLOR, LedZone.ALL_EXCEPT_PLAY_STOP))
            }
        })
    }
}
