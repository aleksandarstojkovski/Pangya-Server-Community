package org.pangya.game;

import org.pangya.protocol.game.GamePackets;

/** C# {@code DefineConstants.motion_item} parts that double SSC ticket drops. */
final class MotionItems {

    private static final int[] MOTION_PARTS = {
        0x08026800, 0x08026801, 0x08026802, 0x08064800, 0x08064801, 0x08064802, 0x08064803,
        0x080A2800, 0x080A2801, 0x080A2802, 0x080E4800, 0x080E4801, 0x080E4802, 0x08122800,
        0x08122801, 0x08122802, 0x0816E801, 0x0816E802, 0x0816E803, 0x0816E805, 0x0816E806,
        0x081A4800, 0x081A4801, 0x081EA800, 0x08228800, 0x08228801, 0x08228802, 0x08228803,
        0x08268800, 0x082A6800, 0x082E4800, 0x082E4801, 0x08320800, 0x08320801, 0x08320802,
        0x083A4800, 0x083A4801, 0x083A4802
    };

    private MotionItems() {}

    /** C# {@code GameBase.checkCharMotionItem}. */
    static boolean hasMotionPart(GamePackets.CharacterInfo character) {
        if (character == null || character.typeid == 0) {
            return false;
        }
        for (int part : character.partsTypeid) {
            if (part == 0) {
                continue;
            }
            for (int motion : MOTION_PARTS) {
                if (part == motion) {
                    return true;
                }
            }
        }
        return false;
    }
}
