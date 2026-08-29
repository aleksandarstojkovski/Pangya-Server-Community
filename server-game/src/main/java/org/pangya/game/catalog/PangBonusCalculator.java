package org.pangya.game.catalog;

import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffCourseRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

/** C# {@code GameBase.requestCalculePang} and Practice pang tax. */
public final class PangBonusCalculator {

    /** C# {@code RoomInfo.ROOM_INFO_MODO.M_SHUFFLE}. */
    public static final int MODO_SHUFFLE = 3;

    private PangBonusCalculator() {}

    /**
     * C# {@code requestCalculePang}: item + server + course rate on bonus pang,
     * 90% tax, 20k soft cap.
     */
    public static long calculeBonusPang(
            long shotPang,
            long shotBonusPang,
            int roomRatePang,
            int playerRatePang,
            int courseField,
            int modo) {
        float courseRate = courseRatePang(courseField);
        int baseRate = roomRatePang;
        if (modo == MODO_SHUFFLE) {
            baseRate += 10;
        }
        float itemRate = transfServerRate(playerRatePang);
        float serverRate = transfServerRate(baseRate);
        float pangRate = itemRate * serverRate * courseRate;

        long novoBonus = (long) ((shotPang * pangRate) - shotPang) + shotBonusPang;
        novoBonus = (long) (novoBonus * 0.90f);
        if (novoBonus > 20_000L) {
            novoBonus = 20_000L + (long) ((novoBonus - 20_000L) * 0.1f);
        }
        return Math.max(0L, novoBonus);
    }

    /** C# {@code Practice.requestCalculePang} after {@code base.requestCalculePang}. */
    public static long[] applyPracticePangTax(long pang, long bonusPang, int roomTipo, int modo) {
        if (roomTipo != GamePackets.TIPO_PRACTICE) {
            return new long[] {pang, bonusPang};
        }
        float tax;
        if (modo == GamePackets.MODO_REPEAT) {
            tax = 1.0f / 6.0f;
            if (bonusPang > 20_000L) {
                tax = 0.05f;
            } else if (bonusPang > 10_000L) {
                tax = 0.10f;
            }
        } else {
            tax = 1.0f / 3.0f;
        }
        return new long[] {(long) (pang * tax), (long) (bonusPang * tax)};
    }

    /** C# {@code findCourse((course & 0x7F) | 0x28000000)} {@code RatePang}. */
    public static float courseRatePang(int courseField) {
        int courseId = courseField & 0x7f;
        int typeid = 0x28000000 | courseId;
        return PangyaIffLoader.courses()
                .flatMap(list -> list.stream().filter(c -> c.typeid() == typeid).findFirst())
                .map(IffCourseRecord::ratePang)
                .filter(rate -> rate >= 1.0f)
                .orElse(1.0f);
    }

    static float transfServerRate(int value) {
        return value <= 0 ? 1.0f : value / 100.0f;
    }
}
