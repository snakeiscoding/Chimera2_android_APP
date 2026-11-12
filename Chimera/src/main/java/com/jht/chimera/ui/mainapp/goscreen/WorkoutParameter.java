package com.jht.chimera.ui.mainapp.goscreen;

import static com.jht.androidcommonalgorithms.math.Resolution.compare;

import android.content.Context;
import android.graphics.Paint;

import androidx.annotation.Nullable;

import com.jht.androidcommonalgorithms.math.Resolution;
import com.jht.androiduiwidgets.IInputConfiguration;
import com.jht.enums.UnitMeasurement;
import com.jht.translations.LanguageHelper;

import org.jetbrains.annotations.NonNls;

public class WorkoutParameter implements IInputConfiguration {

    private float value;
    private float min;
    private float max;
    private float defaultValue;
    private float resolution;
    private int precision;
    private String captionText;
    private String originalLabel;
    private String unitText;
    private UnitMeasurement measurement;
    private int maxChars;
    private boolean isPassword = false;
    private boolean isTime = false;
    private int autoHide = 0;

    //Extra flags
    private boolean showProgress = true;
    public int graphColor;
    public Paint.Style graphStyle;
    private String identifier;

    public WorkoutParameter(float min, float max, float defaultValue, float resolution, int precision, String captionText, String unitText, @Nullable UnitMeasurement measurement, @NonNls String identifier) {
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.resolution = resolution == 0 ? 1 : resolution;
        this.precision = precision;
        this.captionText = captionText;
        this.unitText = unitText;
        this.measurement = measurement;
        this.identifier = identifier;
        this.value = defaultValue;
        maxChars = LanguageHelper.getDecimalFormat(resolution()).format(max).length();
    }

    public WorkoutParameter(WorkoutParameter workoutParameter) {
        this(workoutParameter, workoutParameter.graphStyle, workoutParameter.graphColor);
    }

    public WorkoutParameter(WorkoutParameter workoutParameter, String altLabel) {
        this(workoutParameter, workoutParameter.graphStyle, workoutParameter.graphColor, altLabel);
    }

    public WorkoutParameter(WorkoutParameter workoutParameter, Paint.Style graphStyle, int graphColor) {
        this(workoutParameter, workoutParameter.graphStyle, workoutParameter.graphColor, null);

    }

    public WorkoutParameter(WorkoutParameter workoutParameter, Paint.Style graphStyle, int graphColor, String altLabel) {
        if (workoutParameter != null) {
            this.value = workoutParameter.value;
            this.min = workoutParameter.min;
            this.max = workoutParameter.max;
            this.defaultValue = workoutParameter.defaultValue;
            this.resolution = workoutParameter.resolution;
            this.precision = workoutParameter.precision;
            this.originalLabel = workoutParameter.captionText;
            if(altLabel != null && !altLabel.isEmpty()) {
                this.captionText = altLabel;
            }
            else {
                this.captionText = workoutParameter.captionText;
            }
            this.unitText = workoutParameter.unitText;
            this.identifier = workoutParameter.identifier;
            this.graphColor = graphColor;
            this.graphStyle = graphStyle;
            this.maxChars = workoutParameter.maxChars;
        }
    }

    public int precision() {
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
        if (isTime) return Float.toString(value);
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

    public UnitMeasurement measurement(UnitMeasurement measurement) { return this.measurement = measurement; }
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

    public String getOriginalLabel() {
        return originalLabel;
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
        int numDecimalsOfPrecision = Resolution.numDecimals(resolution());
        int safeToIncrement = compare(newValue, max, numDecimalsOfPrecision);
        if (safeToIncrement <= 0) {
            value = (float) newValue;
        } else {
            value = (float) max;
        }
    }

    @Override
    public void decrement() {
        double newValue = value - resolution();
        int numDecimalsOfPrecision = Resolution.numDecimals(resolution());
        int safeToDecrement = compare(newValue, min, numDecimalsOfPrecision);
        if (safeToDecrement >= 0) {
            value = (float) newValue;
        } else {
            value = (float) min;
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
