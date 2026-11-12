package com.jht.chimera.lib.workout.va;

import org.jetbrains.annotations.NonNls;

/**
 * Class for utility methods for VA
 */
public class VaUtil {

    /**
     * PTP has a list of workouts only defined by certain names. Since the names don't perfectly line
     * up this is a map from our unique ID to the name defined by PTP/web for each workout so we can
     * know if the workout is available or not.
     *
     * @param vaUniqueID The ID from VA
     * @return The name known by PTP
     */
    public static String getPtpVaTitleString(@NonNls String vaUniqueID) {
        @NonNls String vaLocationName;
        switch (vaUniqueID) {
            case "1601B":
                vaLocationName = "Alberta Badlands Challenge Bike"; // NON-NLS
                break;
            case "1101R":
                vaLocationName = "Alberta Rockies Run"; // NON-NLS
                break;
            case "202R":
                vaLocationName = "American Northeast Run"; // NON-NLS
                break;
            case "1601H":
                vaLocationName = "American Southwest 2 Hike"; // NON-NLS
                break;
            case "1602B":
                vaLocationName = "Argentine Patagonia Bike"; // NON-NLS
                break;
            case "1602H":
                vaLocationName = "Argentine Patagonia Hike"; // NON-NLS
                break;
            case "1601R":
                vaLocationName = "Argentine Patagonia Run"; // NON-NLS
                break;
            case "1001B":
                vaLocationName = "Bay Area Bike"; // NON-NLS
                break;
            case "1301B":
                vaLocationName = "Big Island Bike"; // NON-NLS
                break;
            case "1301H":
                vaLocationName = "Big Island Hike"; // NON-NLS
                break;
            case "1602R":
                vaLocationName = "Big Island Run"; // NON-NLS
                break;
            case "1101H":
                vaLocationName = "British Columbia Hike"; // NON-NLS
                break;
            case "1603B":
                vaLocationName = "British Columbia Rockies Bike"; // NON-NLS
                break;
            case "1001H":
                vaLocationName = "British Columbia Sea to Sky Hike"; // NON-NLS
                break;
            case "1604B":
                vaLocationName = "British Columbia Sea-to-Sky Challenge Bike"; // NON-NLS
                break;
            case "1201R":
                vaLocationName = "Buenos Aires Run"; // NON-NLS
                break;
            case "1603R":
                vaLocationName = "California Central Coast Run"; // NON-NLS;
                break;
            case "1001R":
                vaLocationName = "Canadian West Coast Run"; // NON-NLS
                break;
            case "707B":
                vaLocationName = "Champagne Bike"; // NON-NLS
                break;
            case "802B":
                vaLocationName = "Chilean Patagonia Bike"; // NON-NLS
                break;
            case "802H":
                vaLocationName = "Chilean Patagonia Hike"; // NON-NLS
                break;
            case "802R":
                vaLocationName = "Chilean Patagonia Run"; // NON-NLS
                break;
            case "1605B":
                vaLocationName = "Col de la Colombiere Challenge Bike"; // NON-NLS
                break;
            case "1701B":
                vaLocationName = "Columbia River Gorge Bike"; // NON-NLS
                break;
            case "1702B":
                vaLocationName = "Costa Rica Bike"; // NON-NLS
                break;
            case "1401H":
                vaLocationName = "Costa Rica Forests Hike"; // NON-NLS
                break;
            case "1604R":
                vaLocationName = "Costa Rica Run"; // NON-NLS
                break;
            case "1603H":
                vaLocationName = "Costa Rica Waterfalls Hikes"; // NON-NLS
                break;
            case "1501B":
                vaLocationName = "East Taiwan Bike"; // NON-NLS
                break;
            case "1102R":
                vaLocationName = "Germany Run"; // NON-NLS
                break;
            case "1604H":
                vaLocationName = "Kauai Hike"; // NON-NLS
                break;
            case "1605R":
                vaLocationName = "Kauai Run"; // NON-NLS
                break;
            case "1703B":
                vaLocationName = "Languedoc Roussillon Bike"; // NON-NLS
                break;
            case "1801B":
                vaLocationName = "Loire Valley Bike"; // NON-NLS
                break;
            case "1201H":
                vaLocationName = "Maui Hike"; // NON-NLS
                break;
            case "1201B":
                vaLocationName = "Maui Mountains to Sea Bike"; // NON-NLS
                break;
            case "1802B":
                vaLocationName = "Maui Road to Hana Challenge Bike"; // NON-NLS
                break;
            case "1701R":
                vaLocationName = "Maui Run"; // NON-NLS
                break;
            case "1803B":
                vaLocationName = "Meadows in the Sky Parkway Challenge Bike"; // NON-NLS
                break;
            case "901H":
                vaLocationName = "New Zealand Hike"; // NON-NLS
                break;
            case "1804B":
                vaLocationName = "North Island, New Zealand Bike"; // NON-NLS
                break;
            case "901R":
                vaLocationName = "North Island, New Zealand Run"; // NON-NLS
                break;
            case "1807B":
                vaLocationName = "Northern France Bike"; // NON-NLS
                break;
            case "1605H":
                vaLocationName = "Northern France Hike"; // NON-NLS
                break;
            case "1702R":
                vaLocationName = "Northern France Run"; // NON-NLS
                break;
            case "1805B":
                vaLocationName = "Northern Italy Bike"; // NON-NLS
                break;
            case "104R":
                vaLocationName = "Northern Rockies Run"; // NON-NLS
                break;
            case "1806B":
                vaLocationName = "Oahu Bike"; // NON-NLS
                break;
            case "1703R":
                vaLocationName = "Oahu Run"; // NON-NLS
                break;
            case "1401B":
                vaLocationName = "Panama Bike"; // NON-NLS
                break;
            case "1701H":
                vaLocationName = "Panama Hike"; // NON-NLS
                break;
            case "1704R":
                vaLocationName = "Panama Run"; // NON-NLS
                break;
            case "1702H":
                vaLocationName = "Paris Hike"; // NON-NLS
                break;
            case "1705R":
                vaLocationName = "Paris Run"; // NON-NLS
                break;
            case "1401R":
                vaLocationName = "San Francisco Run"; // NON-NLS
                break;
            case "1301R":
                vaLocationName = "Santiago and Valparaiso Run"; // NON-NLS
                break;
            case "1703H":
                vaLocationName = "Sierra Nevada 2 Hike"; // NON-NLS
                break;
            case "902R":
                vaLocationName = "South Island, New Zealand Run"; // NON-NLS
                break;
            case "1501R":
                vaLocationName = "Taipei Run"; // NON-NLS
                break;
            case "1501H":
                vaLocationName = "Taiwan Hike"; // NON-NLS
                break;
            default:
                vaLocationName = ""; // NON-NLS
                break;
        }
        return vaLocationName;
    }

    /**
     * Gets the ID for the VA location from the name that's defined in PTP. The inverse of
     * {@link #getPtpVaTitleString(String)}.
     *
     * @param vaTitle The title of the va workout in ptp
     * @return The ID for the VA course
     */
    public static String getVaUniqueIdFromPtpTitle(@NonNls String vaTitle) {
        @NonNls String vaId;
        switch (vaTitle) {
            case "Alberta Badlands Challenge Bike": // NON-NLS
                vaId = "1601B"; // NON-NLS
                break;
            case "Alberta Rockies Run": // NON-NLS
                vaId = "1101R"; // NON-NLS
                break;
            case "American Northeast Run": // NON-NLS
                vaId = "202R"; // NON-NLS
                break;
            case "American Southwest 2 Hike": // NON-NLS
                vaId = "1601H"; // NON-NLS
                break;
            case "Argentine Patagonia Bike": // NON-NLS
                vaId = "1602B"; // NON-NLS
                break;
            case "Argentine Patagonia Hike": // NON-NLS
                vaId = "1602H"; // NON-NLS
                break;
            case "Argentine Patagonia Run": // NON-NLS
                vaId = "1601R"; // NON-NLS
                break;
            case "Bay Area Bike": // NON-NLS
                vaId = "1001B"; // NON-NLS
                break;
            case "Big Island Bike": // NON-NLS
                vaId = "1301B"; // NON-NLS
                break;
            case "Big Island Hike": // NON-NLS
                vaId = "1301H"; // NON-NLS
                break;
            case "Big Island Run": // NON-NLS
                vaId = "1602R"; // NON-NLS
                break;
            case "British Columbia Hike": // NON-NLS
                vaId = "1101H"; // NON-NLS
                break;
            case "British Columbia Rockies Bike": // NON-NLS
                vaId = "1603B"; // NON-NLS
                break;
            case "British Columbia Sea to Sky Hike": // NON-NLS
                vaId = "1001H"; // NON-NLS
                break;
            case "British Columbia Sea-to-Sky Challenge Bike": // NON-NLS
                vaId = "1604B"; // NON-NLS
                break;
            case "Buenos Aires Run": // NON-NLS
                vaId = "1201R"; // NON-NLS
                break;
            case "California Central Coast Run": // NON-NLS
                vaId = "1603R"; // NON-NLS
                break;
            case "Canadian West Coast Run": // NON-NLS
                vaId = "1001R"; // NON-NLS
                break;
            case "Champagne Bike": // NON-NLS
                vaId = "707B"; // NON-NLS
                break;
            case "Chilean Patagonia Bike": // NON-NLS
                vaId = "802B"; // NON-NLS
                break;
            case "Chilean Patagonia Hike": // NON-NLS
                vaId = "802H"; // NON-NLS
                break;
            case "Chilean Patagonia Run": // NON-NLS
                vaId = "802R"; // NON-NLS
                break;
            case "Col de la Colombiere Challenge Bike": // NON-NLS
                vaId = "1605B"; // NON-NLS
                break;
            case "Columbia River Gorge Bike": // NON-NLS
                vaId = "1701B"; // NON-NLS
                break;
            case "Costa Rica Bike": // NON-NLS
                vaId = "1702B"; // NON-NLS
                break;
            case "Costa Rica Forests Hike": // NON-NLS
                vaId = "1401H"; // NON-NLS
                break;
            case "Costa Rica Run": // NON-NLS
                vaId = "1604R"; // NON-NLS
                break;
            case "Costa Rica Waterfalls Hikes": // NON-NLS
                vaId = "1603H"; // NON-NLS
                break;
            case "East Taiwan Bike": // NON-NLS
                vaId = "1501B"; // NON-NLS
                break;
            case "Germany Run": // NON-NLS
                vaId = "1102R"; // NON-NLS
                break;
            case "Kauai Hike": // NON-NLS
                vaId = "1604H"; // NON-NLS
                break;
            case "Kauai Run": // NON-NLS
                vaId = "1605R"; // NON-NLS
                break;
            case "Languedoc Roussillon Bike": // NON-NLS
                vaId = "1703B"; // NON-NLS
                break;
            case "Loire Valley Bike": // NON-NLS
                vaId = "1801B"; // NON-NLS
                break;
            case "Maui Hike": // NON-NLS
                vaId = "1201H"; // NON-NLS
                break;
            case "Maui Mountains to Sea Bike": // NON-NLS
                vaId = "1201B"; // NON-NLS
                break;
            case "Maui Road to Hana Challenge Bike": // NON-NLS
                vaId = "1802B"; // NON-NLS
                break;
            case "Maui Run": // NON-NLS
                vaId = "1701R"; // NON-NLS
                break;
            case "Meadows in the Sky Parkway Challenge Bike": // NON-NLS
                vaId = "1803B"; // NON-NLS
                break;
            case "New Zealand Hike": // NON-NLS
                vaId = "901H"; // NON-NLS
                break;
            case "North Island, New Zealand Bike": // NON-NLS
                vaId = "1804B"; // NON-NLS
                break;
            case "North Island, New Zealand Run": // NON-NLS
                vaId = "901R"; // NON-NLS
                break;
            case "Northern France Bike": // NON-NLS
                vaId = "1807B"; // NON-NLS
                break;
            case "Northern France Hike": // NON-NLS
                vaId = "1605H"; // NON-NLS
                break;
            case "Northern France Run": // NON-NLS
                vaId = "1702R"; // NON-NLS
                break;
            case "Northern Italy Bike": // NON-NLS
                vaId = "1805B"; // NON-NLS
                break;
            case "Northern Rockies Run": // NON-NLS
                vaId = "104R"; // NON-NLS
                break;
            case "Oahu Bike": // NON-NLS
                vaId = "1806B"; // NON-NLS
                break;
            case "Oahu Run": // NON-NLS
                vaId = "1703R"; // NON-NLS
                break;
            case "Panama Bike": // NON-NLS
                vaId = "1401B"; // NON-NLS
                break;
            case "Panama Hike": // NON-NLS
                vaId = "1701H"; // NON-NLS
                break;
            case "Panama Run": // NON-NLS
                vaId = "1704R"; // NON-NLS
                break;
            case "Paris Hike": // NON-NLS
                vaId = "1702H"; // NON-NLS
                break;
            case "Paris Run": // NON-NLS
                vaId = "1705R"; // NON-NLS
                break;
            case "San Francisco Run": // NON-NLS
                vaId = "1401R"; // NON-NLS
                break;
            case "Santiago and Valparaiso Run": // NON-NLS
                vaId = "1301R"; // NON-NLS
                break;
            case "Sierra Nevada 2 Hike": // NON-NLS
                vaId = "1703H"; // NON-NLS
                break;
            case "South Island, New Zealand Run": // NON-NLS
                vaId = "902R"; // NON-NLS
                break;
            case "Taipei Run": // NON-NLS
                vaId = "1501R"; // NON-NLS
                break;
            case "Taiwan Hike": // NON-NLS
                vaId = "1501H"; // NON-NLS
                break;
            default:
                vaId = "1601B"; // NON-NLS
                break;
        }
        return vaId;
    }
}
