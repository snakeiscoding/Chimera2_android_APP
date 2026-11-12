package com.jht.chimera.lib.workout;

import android.content.Context;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.math.Resolution;
import com.jht.androiduiwidgets.IInputConfiguration;
import com.jht.enums.UnitMeasurement;
import com.jht.translations.LanguageHelper;

/**
 * Represents an input that should be set before starting a workout. To set the initial condition
 * for this {@link WorkoutInput} you must call {@link #value(double)} to set the value. Whichever
 * value is set will be used as the initial value for the workout.
 */
public class WorkoutInput implements IInputConfiguration {

    private float value;
    private float min;
    private float max;
    private float defaultValue;
    private float resolution;
    private int precision;
    private String captionText;
    private String unitText;
    private UnitMeasurement measurement;
    private int maxChars;
    private boolean isPassword = false;
    private boolean isTime = false;
    private int autoHide = 0;

    //Extra flags
    private boolean showProgress = true;
    private String identifier;

    /**
     * Creates a {@link WorkoutInput} object
     *
     * @param min          The min value for the input
     * @param max          The max value for the input
     * @param defaultValue The default value for the input
     * @param resolution   The resolution of the input
     * @param precision    The number of decimal units of precision for the input
     * @param captionText  The text for the caption
     * @param unitText     The text for the unit
     * @param identifier   An ID that is used to determine which parameter this is
     */
    public WorkoutInput(float min, float max, float defaultValue, float resolution, int precision, String captionText, String unitText, @Nullable UnitMeasurement measurement, String identifier) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.resolution = resolution == 0 ? 1 : resolution;
        this.precision = precision;
        this.captionText = captionText;
        this.unitText = unitText;
        this.measurement = measurement;
        this.identifier = identifier;
        maxChars = LanguageHelper.getDecimalFormat(resolution()).format(max).length();
    }

    /**
     * Use this to pass just the needed information back to the workout controller if you do not
     * have the workout inputs returned from loading the workout available.
     *
     * @param value The value to pass to the workout controller
     * @param identifier The identifier for the parameter
     */
    public WorkoutInput(String identifier, float value) {
        this.value = value;
        this.identifier = identifier;
    }

    public float precision() {
        return precision;
    }

    public float defaultValue() {
        return defaultValue;
    }

    public void defaultValue(float defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public String value() {
        return LanguageHelper.getDecimalFormat(resolution()).format(value);
    }

    @Override
    public double valueAsDouble() {
        return value;
    }

    @Override
    public boolean isPassword() {
        return isPassword;
    }

    @Override
    public boolean isTime() {
        return isTime;
    }

    @Override
    public double min() {
        return min;
    }

    @Override
    public double max() {
        return max;
    }

    @Override
    public double resolution() {
        return resolution;
    }

    @Override
    public int maxChars() {
        return maxChars;
    }

    @Override
    public String units(Context context) {
        return unitText;
    }

    @Override
    public String units() {
        return unitText;
    }

    public UnitMeasurement measurement() { return measurement; }

    @Override
    public String label(Context context) {
        return captionText;
    }

    public String label(String label) {
        return captionText = label;
    }

    @Override
    public String label() {
        return captionText;
    }

    @Override
    public String identifier(String s) {
        identifier = s;
        return s;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public int autoHide() {
        return autoHide;
    }

    @Override
    public String value(String s) {
        if (s != null && !s.isEmpty()) {
            value = Float.parseFloat(s);
        } else {
            value = defaultValue;
        }
        return s;
    }

    @Override
    public String value(double v) {
        value = (float) v;
        return value();
    }

    @Override
    public boolean isPassword(boolean b) {
        isPassword = b;
        return b;
    }

    @Override
    public boolean isTime(boolean b) {
        isTime = b;
        return b;
    }

    @Override
    public double min(double v) {
        min = (float) v;
        return v;
    }

    @Override
    public double max(double v) {
        max = (float) v;
        return v;
    }

    @Override
    public double resolution(double v) {
        precision = Resolution.numDecimals(v);
        resolution = (float) v;
        return v;
    }

    @Override
    public int maxChars(int i) {
        maxChars = i;
        return i;
    }

    @Override
    public String units(String s) {
        unitText = s;
        return s;
    }

    @Override
    public int autoHide(int i) {
        autoHide = i;
        return i;
    }

    @Override
    public void increment() {
        double newValue = value + resolution();
        if (Resolution.compare(newValue, max, resolution()) <= 0) {
            value = (float) newValue;
        }
    }

    @Override
    public void decrement() {
        double newValue = value - resolution();
        if (Resolution.compare(newValue, min, resolution()) >= 0) {
            value = (float) newValue;
        }
    }


    @Override
    public void showProgress(boolean b) {
        showProgress = b;
    }

    @Override
    public boolean showProgress() {
        return showProgress;
    }
}
