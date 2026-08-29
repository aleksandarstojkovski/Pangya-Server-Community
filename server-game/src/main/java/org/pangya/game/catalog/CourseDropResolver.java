package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.pangya.game.util.Lottery;
import org.pangya.protocol.game.GamePackets;

import java.util.ArrayList;
import java.util.List;

/** C# {@code DropSystem.drawCourse}. */
public final class CourseDropResolver {

    /** C# {@code stDropItem.eTIPO.ALL_PROBABILITY}. */
    public static final int TIPO_ALL_PROBABILITY = 0;
    /** C# {@code stDropItem.eTIPO.SEQUENCE_DROP}. */
    public static final int TIPO_SEQUENCE_DROP = 1;
    /** C# {@code stDropItem.eTIPO.LAST_HOLE_PROBABILITY}. */
    public static final int TIPO_LAST_HOLE_PROBABILITY = 2;

    private CourseDropResolver() {}

    public record CourseDropCtx(
            int courseId,
            int holeNum,
            int seqHole,
            int qntdHole,
            int artefactTypeid,
            int charMotion,
            int rateDrop,
            int angelWings) {}

    public static List<GamePackets.DropItem> drawCourse(
            List<InventoryRepository.CourseDropItem> items, CourseDropCtx ctx) {
        return drawCourse(items, ctx, null);
    }

    public static List<GamePackets.DropItem> drawCourse(
            List<InventoryRepository.CourseDropItem> items,
            CourseDropCtx ctx,
            Long seed) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        int ticketQntd = HoleDropResolver.ticketCount(ctx.charMotion(), ctx.artefactTypeid());
        float rate = 1.0f;
        if (ctx.rateDrop() > 100) {
            rate *= ctx.rateDrop() / 100.0f;
        }
        if (ctx.angelWings() == 1) {
            rate *= 1.2f;
        }

        List<GamePackets.DropItem> drops = new ArrayList<>();
        for (InventoryRepository.CourseDropItem item : items) {
            if (!eligible(item, ctx)) {
                continue;
            }
            int prob = probForLottery(item, ctx.qntdHole());
            if (prob <= 0) {
                continue;
            }
            Lottery lottery = seed == null ? new Lottery() : new Lottery(seed);
            lottery.push(prob, item);
            int limit = prob;
            if (limit * rate < 1000) {
                lottery.push((int) (1000 - limit * rate), 0);
            }
            Lottery.Entry<Object> draw = lottery.spinRoleta(false);
            if (draw == null || !(draw.value() instanceof InventoryRepository.CourseDropItem won)) {
                continue;
            }
            for (int i = 0; i < ticketQntd; i++) {
                drops.add(new GamePackets.DropItem(
                        won.typeid(),
                        ctx.courseId(),
                        ctx.holeNum(),
                        won.qntd(),
                        GamePackets.DROP_TYPE_NORMAL_QNTD));
            }
        }
        return drops;
    }

    static boolean eligible(InventoryRepository.CourseDropItem item, CourseDropCtx ctx) {
        if (item.tipo() == TIPO_LAST_HOLE_PROBABILITY && ctx.seqHole() != ctx.qntdHole()) {
            return false;
        }
        if (item.tipo() == TIPO_SEQUENCE_DROP && item.prob6h() > 0 && ctx.seqHole() % item.prob6h() != 0) {
            return false;
        }
        return true;
    }

    static int probForLottery(InventoryRepository.CourseDropItem item, int qntdHole) {
        if (item.tipo() == TIPO_LAST_HOLE_PROBABILITY) {
            return probForHoleCount(item, qntdHole);
        }
        return item.prob3h();
    }

    static int probForHoleCount(InventoryRepository.CourseDropItem item, int qntdHole) {
        return switch (qntdHole) {
            case 3 -> item.prob3h();
            case 6 -> item.prob6h();
            case 9 -> item.prob9h();
            default -> item.prob18h();
        };
    }
}
