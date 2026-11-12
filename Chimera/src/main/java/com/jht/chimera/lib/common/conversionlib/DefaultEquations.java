package com.jht.chimera.lib.common.conversionlib;

import com.jhtp.dapiservice.um.GenderIndentity;

/**
 * Created by chris.foyer on 3/31/2017.
 */

public class DefaultEquations
{
    public static int getDefaultMaxHr(int age)
    {
        // https://www.ntnu.edu/cerg/hrmax-info
        return Math.round (211 - age * .64f);
    }

    public static int getDefaultFtp(GenderIndentity gender)
    {
        if (gender == GenderIndentity.Male) return 200;
        else return 153;
    }

    public static int getDefaultMaxRowWatts(boolean isMale)
    {
        return isMale ? 200 : 153;
    }
}
