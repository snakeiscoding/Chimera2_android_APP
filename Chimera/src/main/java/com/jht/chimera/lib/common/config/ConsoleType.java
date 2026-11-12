package com.jht.chimera.lib.common.config;

import com.jht.enums.RelativeLocation;

import org.jetbrains.annotations.NonNls;

/**
 * ConsoleType is a data type. ProductConfig provides functionality related to ConsoleType.
 */
public enum ConsoleType {
    unknown(0, "UNKNOWN", 0, 1, 1, 3, 3, null, RelativeLocation.NONE, "UNKNOWN", "unknown"),
    chimera_16(62, "TM756", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.TOP_RIGHT, "Touch", "chimera1"),
    chimera_22(63, "TM757", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.TOP_RIGHT, "Touch XL", "chimera1"),
    athena(70, "FC31", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.BOTTOM_LEFT, "CXV", "chimera1"),
    chimera2_16(62, "TM550", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.TOP_RIGHT, "Touch", "chimera2"),
    chimera2_22(63, "TM551", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.TOP_RIGHT, "Touch XL", "chimera2"),
    athena2(70, "FC31", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.BOTTOM_LEFT, "CXV", "chimera2"),
    xer2(5, "TM751", "TM1026",10, 1024, 600, 4, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE, "XER", "parker1"), // 10 inch
    xir2(6, "TM752", "TM1027", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE, "XIR", "parker1"), // 16 inch (15.6?) initially 1366x768 but was changed
    xur(7, "TM753", "TM1028", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE, "XUR", "parker1"), // 22 inch
    xer2_no_ifit(5, "TM751B", 10, 1024, 600, 4, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE, "XER", "parker1"), // 10 inch
    xir2_no_ifit(6, "TM752C", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE,"XIR", "parker1"), // 16 inch (15.6?) initially 1366x768 but was changed
    xur_no_ifit(7, "TM753B", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE,"XUR", "parker1"), // 22 inch
    ultraThin(12, "TM1032B", 24, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Parker, RelativeLocation.NONE, "Ultra Thin", "ultrathin"), // Ultra thin runs parker on ICR70
    vision15(6, "EP723", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Vision600E, RelativeLocation.NONE,"Vision", "vision600e"),
    vision22(7, "TM743", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Vision600E, RelativeLocation.NONE, "Vision XL", "vision600e"),
    t600xe(72, "TM772", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.T600XE, RelativeLocation.NONE, "Vision T600E", "matrixt600xe"),
    projectOne1(79,"TM547",32, 1920, 1080,6,3, ProductConfig.BuildFlavor.ProjectOne, RelativeLocation.BOTTOM_MIDDLE, "Onyx XL", "projectone"),
    projectOne2(78,"EP635", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.ProjectOne, RelativeLocation.BOTTOM_MIDDLE, "Onyx", "projectone"),
    lcgui16(73, "TM774", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.LCGUI, RelativeLocation.TOP_RIGHT, "Touch Base", "lowcostgui"),
    lcgui22(74, "TM775", 22, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.LCGUI, RelativeLocation.TOP_RIGHT, "Touch Base XL", "lowcostgui"),
    vortex_lite_16_1080p(92, "EP641", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.BOTTOM_LEFT, "Touch Essentials 16", "vortexlite"),
    vortex_lite_23_1080p(93, "TM558", 23, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.BOTTOM_LEFT, "Touch Essentials 24 ", "vortexlite"),
    vortex_16_1080p(94, "TM555", 16, 1366, 768, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.BOTTOM_LEFT, "Touch Pro 16", "vortex"),
    vortex_23_1080p(95, "TM556", 23, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.BOTTOM_LEFT, "Touch Pro 24", "vortex"),
    vortex_23_4k(96, "EP640", 23, 3840, 2160, 6, 3, ProductConfig.BuildFlavor.ProjectOne, RelativeLocation.BOTTOM_MIDDLE, "Onyx 4k 24", "vortex4K"),
    vortex_32_4k(97, "TM557", 32, 3840, 2160, 6, 3, ProductConfig.BuildFlavor.ProjectOne, RelativeLocation.BOTTOM_MIDDLE, "Onyx 4k 32", "vortex4K"),
    tcv_24(98, "FC66", 24, 1920, 1080, 6, 3, ProductConfig.BuildFlavor.Chimera, RelativeLocation.LEFT_MIDDLE, "Virtual Training Cycle", "vortexlite"),
    ;

    public int id;
    public int appGridRowCount;
    public int appGridColCount;
    public int inches;
    public int widthPx;
    public int heightPx;
    protected String prefix;
    protected String prefixJIV; // because JIV apparently did not get the marker file in their flash image (could make prefix an array? or have alternatePrefixes array?)
    ProductConfig.BuildFlavor productFlavorEnum;
    public RelativeLocation rfidLocation;
    public String marketingName;
    public String appStoreFilterName;

    ConsoleType(int id, @NonNls String prefix, int inches, int widthPx, int heightPx, int appGridColCount, int appGridRowCount,
                ProductConfig.BuildFlavor productFlavorEnum, RelativeLocation rfidLocation, String marketingName, String appStoreFilterName) {
        this(id, prefix, null, inches, widthPx, heightPx, appGridColCount, appGridRowCount, productFlavorEnum, rfidLocation, marketingName, appStoreFilterName);
    }
    ConsoleType(int id, @NonNls String prefix, @NonNls String prefixJIV, int inches, int widthPx, int heightPx, int appGridColCount, int appGridRowCount,
                ProductConfig.BuildFlavor productFlavorEnum, RelativeLocation rfidLocation, String marketingName, String appStoreFilterName) {
        this.id = id;
        this.prefix = prefix;
        this.prefixJIV = prefixJIV;
        this.inches = inches;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.appGridRowCount = appGridRowCount;
        this.appGridColCount = appGridColCount;
        this.productFlavorEnum = productFlavorEnum;
        this.rfidLocation = rfidLocation;
        this.marketingName = marketingName;
        this.appStoreFilterName = appStoreFilterName;
    }

    public String getConsolePrefix() {
        if (ProductConfig.Factory.detect().equals(ProductConfig.Factory.JIV)) { // in case they do get a marker file (or maybe this bit is unnecessary?)
            return prefixJIV;
        }
        return prefix;
    }

    public String extractMatchingPrefix(String sn) { // return null if no valid prefix; otherwise return prefix
        if (sn == null) return null;
        // we are now adding prefixes like TR in addition to C and non-C to prefixes, and this will now cover it all
        // this will now cover both "CTM550"(legacy) "TRCTM550"(new)
        if (prefix != null && sn.contains("C" + prefix)) return "C" + prefix;
        if (prefixJIV != null && sn.contains("C" + prefixJIV)) return "C" + prefixJIV;
        // this will now cover both "TM550"(new) "TRTM550"(new)
        if (prefix != null && sn.contains(prefix)) return prefix;
        if (prefixJIV != null && sn.contains(prefixJIV)) return prefixJIV;
        
        return null;
    }

    public static ConsoleType findBySerialNumber(String sn) { // since some prefixes extend others, need the longest match
        ConsoleType result = null;
        String longestPrefix = "";
        for (ConsoleType t : values()) {
            String prefix = t.extractMatchingPrefix(sn);
            if (prefix != null && prefix.length() > longestPrefix.length()) {
                result = t;
                longestPrefix = prefix;
            }
        }
        return result;
    }

}
