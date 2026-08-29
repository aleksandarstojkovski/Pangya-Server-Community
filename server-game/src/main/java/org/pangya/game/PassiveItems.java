package org.pangya.game;

import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffGroups;

/** C# {@code DefineConstants.passive_item} and passive-item helpers. */
final class PassiveItems {

    static final int TIME_BOOSTER = 0x1A000011;
    static final int AUTO_CALIPER = 0x1A000040;

    /** C# {@code passive_item_exp_1perGame}. */
    private static final int[] PER_GAME_EXP = {0x1A00000F, 0x1A000014};

    /** C# {@code passive_item} warehouse passive typeids tracked in-game. */
    private static final int[] KNOWN_PASSIVE = {
            0x1A00000A, 0x1A00000B, 0x1A00000D, 0x1A00000E, 0x1A00000F, 0x1A000013, 0x1A000014,
            0x1A00002F, 0x1A000035, 0x1A000084, 0x1A000085, 0x1A000086, 0x1A000090, 0x1A000099,
            0x1A0000AD, 0x1A0000FC, 0x1A000001, 0x1A000002, 0x1A0000AE, 0x1A000005, 0x1A0003B7,
            0x1A0001D7, 0x1A0001D8, 0x1A00025A, 0x1A000007, 0x1A000008, 0x1A000009, 0x1A00000C,
            AUTO_CALIPER, TIME_BOOSTER, GamePackets.TYPEID_AUTO_COMMAND, 0x1A0001A0, 0x1A000136,
            0x1A000338,
    };

    private PassiveItems() {}

    /** C# {@code CHECK_PASSIVE_ITEM}: ITEM group with passive sub-group. */
    static boolean isPassiveItem(int typeid) {
        return GamePackets.itemGroupIdentify(typeid) == GamePackets.IFF_GROUP_ITEM
                && IffGroups.subGroupIdentify24(typeid) > 1;
    }

    static boolean isKnownPassive(int typeid) {
        for (int known : KNOWN_PASSIVE) {
            if (known == typeid) {
                return true;
            }
        }
        return false;
    }

    static boolean isPerGameExp(int typeid) {
        for (int known : PER_GAME_EXP) {
            if (known == typeid) {
                return true;
            }
        }
        return false;
    }

    static boolean isAuxPart(int typeid) {
        return typeid >= 0x70000000 && typeid < 0x70010000;
    }

    static boolean isTrackedBall(int ballTypeid) {
        return ballTypeid != 0 && ballTypeid != GamePackets.TYPEID_DEFAULT_BALL;
    }
}
