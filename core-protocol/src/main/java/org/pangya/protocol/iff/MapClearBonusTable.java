package org.pangya.protocol.iff;

/**
 * C# {@code Map.initialize} clear_bonus per {@code ROOM_INFO_COURSE} when IFF
 * Course records do not carry this field.
 */
public final class MapClearBonusTable {

    private MapClearBonusTable() {}

    /** C# {@code sIff.getItemIdentify(typeid)} → course index. */
    public static int courseIndex(int typeid) {
        return typeid & 0x7f;
    }

    /** C# {@code Map.stCtx.clear_bonus} switch on course id. */
    public static int clearBonusForCourse(int courseId) {
        return switch (courseId & 0x7f) {
            case 0 -> 20;   // BLUE_LAGOON
            case 1 -> 50;   // BLUE_WATER
            case 2 -> 55;   // SEPIA_WIND
            case 3 -> 80;   // WIND_HILL
            case 4 -> 65;   // WIZ_WIZ
            case 5 -> 24;   // WEST_WIZ
            case 6 -> 50;   // BLUE_MOON
            case 7 -> 70;   // SILVIA_CANNON
            case 8 -> 40;   // ICE_CANNON
            case 9 -> 55;   // WHITE_WIZ
            case 10 -> 40;  // SHINING_SAND
            case 11 -> 20;  // PINK_WIND
            case 13 -> 80;  // DEEP_INFERNO
            case 14 -> 20;  // ICE_SPA
            case 15 -> 20;  // LOST_SEAWAY
            case 16 -> 40;  // EASTERN_VALLEY
            case 17 -> 360; // CHRONICLE_1_CHAOS
            case 18 -> 70;  // ICE_INFERNO
            case 19 -> 40;  // WIZ_CITY
            case 20 -> 40;  // ABBOT_MINE
            case 21 -> 40;  // MYSTIC_RUINS
            case 64 -> 0;   // GRAND_ZODIAC
            default -> 0;
        };
    }
}
