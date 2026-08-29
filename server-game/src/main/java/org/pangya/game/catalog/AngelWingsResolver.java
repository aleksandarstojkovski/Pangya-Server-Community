package org.pangya.game.catalog;

import org.pangya.protocol.game.GamePackets;

/** C# {@code CharacterInfo.AngelEquiped} + {@code GOOD_PLAYER_ICON} gate for drop rate. */
public final class AngelWingsResolver {

    /** C# {@code GOOD_PLAYER_ICON}. */
    public static final float GOOD_PLAYER_ICON = 3.0f;

    /** C# {@code Global.gacha_angel_wings}. */
    private static final int[] GACHA_ANGEL_WINGS = {
        134309903, 134580239, 134842383, 135120911, 135366671, 135661583, 135858191, 136194063,
        136398863, 136661007, 136923153, 137185284, 137447436, 138004492
    };

    private AngelWingsResolver() {}

    /** C# {@code UserInfo.getQuitRate}: {@code quitado * 100 / jogado}. */
    public static float quitRate(long jogado, long quitado) {
        if (jogado <= 0) {
            return 0f;
        }
        return quitado * 100f / jogado;
    }

    /**
     * C# {@code AngelEquiped} when {@code getQuitRate() < GOOD_PLAYER_ICON}. Returns 1 when a
     * gacha angel wing part is equipped (C# also has a duplicate path returning 2; drop boost
     * uses {@code angel_wings == 1} only).
     */
    public static int angelEquipped(GamePackets.CharacterInfo character, float quitRate) {
        if (character == null || character.typeid == 0 || quitRate >= GOOD_PLAYER_ICON) {
            return 0;
        }
        int charIdentify = character.typeid & 0xFF;
        for (int wingTypeid : GACHA_ANGEL_WINGS) {
            if (GamePackets.itemCharIdentify(wingTypeid) != charIdentify) {
                continue;
            }
            int partNum = GamePackets.itemCharPartNumber(wingTypeid);
            if (partNum >= 0
                    && partNum < character.partsTypeid.length
                    && character.partsTypeid[partNum] == wingTypeid) {
                return 1;
            }
        }
        return 0;
    }
}
