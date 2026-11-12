package com.jht.chimera.lib.widget.athena;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;

public class IntervalViewDataHolder
{
    public int intervalNumber;
    public String timeString;
    public SpannableStringBuilder distanceString;
    public String typeString;
    public Drawable background;

    public IntervalViewDataHolder(int intervalNumber, String timeString, SpannableStringBuilder distanceString, String typeString)
    {
        this.intervalNumber = intervalNumber;
        this.timeString = timeString;
        this.distanceString = distanceString;
        this.typeString = typeString;
    }

    public static ArrayList<IntervalViewDataHolder> createIntervalDataList() {
        ArrayList<IntervalViewDataHolder> contacts = new ArrayList<IntervalViewDataHolder>();

        return contacts;
    }
}
