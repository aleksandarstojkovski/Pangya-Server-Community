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

    /**
     * C# {@code DropSystem.drawManaArtefact}. Returns empty optional when IFF mana
     * pool is empty or the roll loses.
     */
    public static java.util.Optional<GamePackets.DropItem> drawManaArtefact(
            int rateManaArtefact,
            int courseId,
            int holeNum,
            int rateDrop,
            int angelWings,
            List<Integer> manaTypeids) {
        return drawManaArtefact(
                rateManaArtefact, courseId, holeNum, rateDrop, angelWings, manaTypeids, null);
    }

    public static java.util.Optional<GamePackets.DropItem> drawManaArtefact(
            int rateManaArtefact,
            int courseId,
            int holeNum,
            int rateDrop,
            int angelWings,
            List<Integer> manaTypeids,
            Long seed) {
        if (manaTypeids == null || manaTypeids.isEmpty()) {
            return java.util.Optional.empty();
        }
        Lottery lottery = seed == null ? new Lottery() : new Lottery(seed);
        for (int typeid : manaTypeids) {
            lottery.push(200, typeid);
        }
        int limit = 200 * manaTypeids.size();
        float rate = rateManaArtefact > 0 ? rateManaArtefact / 100.0f : 1.0f;
        if (rateDrop > 100) {
            rate *= rateDrop / 100.0f;
        }
        if (angelWings == 1) {
            rate *= 1.2f;
        }
        limit = Math.max(1, (int) (limit / rate));
        lottery.push(limit, 0);

        Lottery.Entry<Integer> draw = lottery.spinRoleta(false);
        if (draw == null || draw.value() == null || draw.value() == 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new GamePackets.DropItem(
                draw.value(), courseId, holeNum, 1, GamePackets.DROP_TYPE_NORMAL_QNTD));
    }

    /**
     * C# {@code DropSystem.drawGrandPrixTicket}. Returns empty when warehouse is at
     * cap or the roll loses (non 9/18-hole games).
     */
    public static java.util.Optional<GamePackets.DropItem> drawGrandPrixTicket(
            int courseId,
            int holeNum,
            int qntdHole,
            int warehouseQntd) {
        return drawGrandPrixTicket(courseId, holeNum, qntdHole, warehouseQntd, null);
    }

    public static java.util.Optional<GamePackets.DropItem> drawGrandPrixTicket(
            int courseId,
            int holeNum,
            int qntdHole,
            int warehouseQntd,
            Long seed) {
        if (warehouseQntd >= GamePackets.GP_TICKET_WAREHOUSE_LIMIT) {
            return java.util.Optional.empty();
        }
        int qntd;
        if (qntdHole == 18 || qntdHole == 9) {
            qntd = (warehouseQntd == GamePackets.GP_TICKET_WAREHOUSE_LIMIT - 1 || qntdHole == 9) ? 1 : 2;
            return java.util.Optional.of(new GamePackets.DropItem(
                    GamePackets.TYPEID_GP_TICKET,
                    courseId,
                    holeNum,
                    qntd,
                    GamePackets.DROP_TYPE_NORMAL_QNTD));
        }
        Lottery lottery = seed == null ? new Lottery() : new Lottery(seed);
        lottery.push(200, GamePackets.TYPEID_GP_TICKET);
        lottery.push(400, 0);
        Lottery.Entry<Integer> draw = lottery.spinRoleta(false);
        if (draw == null || draw.value() == null || draw.value() == 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new GamePackets.DropItem(
                GamePackets.TYPEID_GP_TICKET, courseId, holeNum, 1, GamePackets.DROP_TYPE_NORMAL_QNTD));
    }

    /** C# guaranteed GP ticket quantity on 9/18-hole games (test hook). */
    static int grandPrixTicketQntd(int warehouseQntd, int qntdHole) {
        if (warehouseQntd >= GamePackets.GP_TICKET_WAREHOUSE_LIMIT) {
            return 0;
        }
        if (qntdHole == 18 || qntdHole == 9) {
            return (warehouseQntd == GamePackets.GP_TICKET_WAREHOUSE_LIMIT - 1 || qntdHole == 9) ? 1 : 2;
        }
        return 1;
    }
}
