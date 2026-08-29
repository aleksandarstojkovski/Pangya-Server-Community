package org.pangya.game.catalog;

import org.pangya.protocol.iff.IffAuxPartRecord;
import org.pangya.protocol.iff.IffMascotRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

/** C# {@code GameBase.requestInitItemUsedGame} drop-rate accumulation for hole drops. */
public final class DropRateResolver {

    private DropRateResolver() {}

    public static int computeDropRate(int[] auxparts, int mascotTypeid) {
        int rate = 100;
        if (auxparts != null) {
            for (int aux : auxparts) {
                if (aux == 0) {
                    continue;
                }
                var part = PangyaIffLoader.auxPart(aux);
                if (part.isPresent()) {
                    rate += rateContribution(part.get().dropRate());
                }
            }
        }
        if (mascotTypeid > 0) {
            var mascot = PangyaIffLoader.mascot(mascotTypeid);
            if (mascot.isPresent()) {
                rate += rateContribution(mascot.get().dropRate());
            }
        }
        return rate;
    }

    /** C# auxpart/mascot rate: values above 100 add {@code rate - 100}, else add raw when &gt; 0. */
    static int rateContribution(int iffRate) {
        if (iffRate > 100) {
            return iffRate - 100;
        }
        if (iffRate > 0) {
            return iffRate;
        }
        return 0;
    }
}
