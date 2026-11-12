package com.jht.chimera.ui.machinesettings.factory;

import android.annotation.SuppressLint;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jht.androidcommonalgorithms.util.FileUtils;
import com.jht.chimera.lib.common.ExecuteAsRoot;

import java.io.File;
import java.text.DecimalFormat;

@Keep
public class IcrCalibrationSettings {

    // populate with the current hardcoded values which can then be overridden by USB file if needed
    public double ST2 = 1.258E-09;
    public double ST1 = 0.00005974;
    public double ST0 = 0.2514;
    public double beltRatio = 7.9;
    public double radiusMeters = 0.183;
    //public double minADC = 0;
    //public double maxADC = 4294967295L;
    //public double adcScale = -1;
    public double stScale = 1;
    public double nonZeroKGF = 4;

    // for development testing (this pre-populate the ADC measurements at screen load in case we want to test specific values
    public Integer startAdc0 = null;
    public Integer startAdc1 = null;

    private static Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.serializeNulls(); // force writing null values
        return gsonBuilder.create();
    }

    public static IcrCalibrationSettings load(File file) {
        Gson gson = createGson();
        String fileString = FileUtils.readFileToString(file);
        return gson.fromJson(fileString, IcrCalibrationSettings.class);
    }

    public void save(File file) {
        Gson gson = createGson();
        FileUtils.writeToFile(gson.toJson(this), file);
        ExecuteAsRoot.runSyncCommand();
    }

    @SuppressLint("DefaultLocale")
    public String getReport() {
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(100);
        return String.format("ST2: %s ST1: %s ST0: %s\n", df.format(ST2), df.format(ST1), df.format(ST0))
                + String.format("beltRatio: %s, radius: %s\n", df.format(beltRatio), df.format(radiusMeters))
                //+ String.format("minADC: %s, maxADC: %s\n", df.format(minADC), df.format(maxADC))
                //+ String.format("adcScale: %s\n", df.format(adcScale))
                + String.format("stScale: %s nonZeroKGF: %s", df.format(stScale), df.format(nonZeroKGF));
    }

}
