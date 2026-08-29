package org.pangya.game.catalog;

import org.pangya.game.util.Lottery;
import org.pangya.protocol.game.GamePackets;

import java.util.ArrayList;
import java.util.List;

/** C# {@code DropSystem.drawSSCTicket} and related hole-end drops. */
public final class HoleDropResolver {

    /** C# {@code ART_RAINBOW_MAGIC_HAT}. */
    public static final int ART_RAINBOW_MAGIC_HAT = 0x1A0001BE;

    /** C# {@code drawSSCTicket} ticket count before lottery (test hook). */
    static int ticketCount(int charMotion, int artefactTypeid) {
        int qntd = 1;
        if (charMotion == 1) {
            qntd *= 2;
        }
        if (artefactTypeid == ART_RAINBOW_MAGIC_HAT) {
            qntd++;
        }
        return qntd;
    }

    private HoleDropResolver() {}

    /**
     * C# {@code DropSystem.drawSSCTicket}. {@code rateDrop} and {@code angelWings}
     * default to 100 / 0 when unknown.
     */
    public static List<GamePackets.DropItem> drawSscTickets(
            int rateSscTicket,
            int courseId,
            int holeNum,
            int artefactTypeid,
            int charMotion,
            int rateDrop,
            int angelWings) {
        return drawSscTickets(
                rateSscTicket,
                courseId,
                holeNum,
                artefactTypeid,
                charMotion,
                rateDrop,
                angelWings,
                null);
    }

    /** {@code seed} non-null for deterministic tests (C# {@code Lottery(ulong)}). */
    public static List<GamePackets.DropItem> drawSscTickets(
            int rateSscTicket,
            int courseId,
            int holeNum,
            int artefactTypeid,
            int charMotion,
            int rateDrop,
            int angelWings,
            Long seed) {
        int qntd = 1;
        if (charMotion == 1) {
            qntd *= 2;
        }
        if (artefactTypeid == ART_RAINBOW_MAGIC_HAT) {
            qntd++;
        }

        Lottery lottery = seed == null ? new Lottery() : new Lottery(seed);
        lottery.push(200, GamePackets.TYPEID_SSC_TICKET);
        int limit = 200;
        float rate = rateSscTicket > 0 ? rateSscTicket / 100.0f : 1.0f;
        if (rateDrop > 100) {
            rate *= rateDrop / 100.0f;
        }
        if (angelWings == 1) {
            rate *= 1.2f;
        }
        limit = Math.max(1, (int) (limit / rate));
        lottery.push(limit, 0);
        lottery.push(limit, 0);

        Lottery.Entry<Integer> draw = lottery.spinRoleta(false);
        if (draw == null || draw.value() == null || draw.value() == 0) {
            return List.of();
        }

        List<GamePackets.DropItem> drops = new ArrayList<>(qntd);
        for (int i = 0; i < qntd; i++) {
            drops.add(new GamePackets.DropItem(
                    GamePackets.TYPEID_SSC_TICKET,
                    courseId,
                    holeNum,
                    1,
                    GamePackets.DROP_TYPE_NORMAL_QNTD));
        }
        return drops;
    }
}
