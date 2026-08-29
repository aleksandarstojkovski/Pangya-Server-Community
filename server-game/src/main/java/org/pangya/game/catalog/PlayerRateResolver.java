package org.pangya.game.catalog;

import java.util.Collection;
import java.util.List;

import org.pangya.db.InventoryRepository.CardEquipRow;
import org.pangya.db.InventoryRepository.ItemBuffRow;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffAuxPartRecord;
import org.pangya.protocol.iff.IffMascotRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

/**
 * C# {@code GameBase.requestInitItemUsedGame} exp/pang/club/drop rate accumulation
 * ({@code ui.rate}).
 */
public final class PlayerRateResolver {

    /** C# {@code KURAFAITO_RING_CLUBMASTERY}. */
    public static final int KURAFAITO_RING = 0x70210009;

    /** C# {@code passive_item_pang_x2}. */
    private static final int[] PASSIVE_PANG_X2 = {0x1A000001, 0x1A000002, 0x1A0000AE};
    /** C# {@code passive_item_pang_x4}. */
    private static final int[] PASSIVE_PANG_X4 = {0x1A000005, 0x1A0003B7};
    /** C# {@code passive_item_pang_x1_5}. */
    private static final int[] PASSIVE_PANG_X1_5 = {0x1A0001D7, 0x1A0001D8};
    /** C# {@code passive_item_pang_x1_4}. */
    private static final int[] PASSIVE_PANG_X1_4 = {0x1A00025A};
    /** C# {@code passive_item_pang_x1_2}. */
    private static final int[] PASSIVE_PANG_X1_2 = {0x1A000007, 0x1A000008, 0x1A000009, 0x1A00000C};
    /** C# {@code passive_item_exp}. */
    private static final int[] PASSIVE_EXP = {
            0x1A00000A, 0x1A00000B, 0x1A00000D, 0x1A00000E, 0x1A00000F, 0x1A000013, 0x1A000014,
            0x1A00002F, 0x1A000035, 0x1A000084, 0x1A000085, 0x1A000086, 0x1A000090, 0x1A000099,
            0x1A0000AD, 0x1A0000FC,
    };
    /** C# {@code passive_item_club_boost}. */
    private static final int[] PASSIVE_CLUB = {0x1A000338};
    /** C# {@code hat_birthday}. */
    private static final int[] HAT_BIRTHDAY = {
            0x08000885, 0x0805a81c, 0x08080832, 0x080d0836, 0x08100038, 0x0815a047, 0x0818e048,
            0x081d881e, 0x0821203c, 0x08252013, 0x0829200e, 0x082d6000,
    };
    /** C# {@code hat_lua_sol}. */
    private static final int[] HAT_LUA_SOL = {
            0x08018803, 0x0805a828, 0x0809a827, 0x080d083f, 0x0811a823, 0x0815a855, 0x0818e050,
            0x081d8825, 0x0821204a, 0x08252015,
    };

    /** C# {@code UsedItem.rate}: base 100 before bonuses. */
    public record PlayerRates(int exp, int pang, int club, int drop) {
        public static final PlayerRates BASE = new PlayerRates(100, 100, 100, 100);
    }

    private PlayerRateResolver() {}

    public static PlayerRates compute(
            List<ItemBuffRow> itemBuffs,
            List<CardEquipRow> cardEquips,
            Collection<Integer> passiveTypeids,
            GamePackets.CharacterInfo character,
            int mascotTypeid) {
        int exp = 100;
        int pang = 100;
        int club = 100;
        int drop = 100;

        if (itemBuffs != null) {
            for (ItemBuffRow buff : itemBuffs) {
                int tipo = PangyaIffLoader.timeLimitItem(buff.typeid())
                        .map(t -> t.tipo())
                        .orElse(buff.tipo());
                int percent = PangyaIffLoader.timeLimitItem(buff.typeid())
                        .map(t -> t.percent())
                        .orElse(buff.percent());
                switch (tipo) {
                    case GamePackets.ITEM_BUFF_TIPO_YAM -> exp += percent;
                    case 2, 3 -> { // RAINBOW, RED
                        exp += percent > 0 ? percent : 100;
                        pang += percent > 0 ? percent : 100;
                    }
                    case 4 -> exp += percent > 0 ? percent : 100; // GREEN
                    case 5 -> pang += percent > 0 ? percent : 100; // YELLOW
                    default -> { }
                }
            }
        }

        int charId = character == null ? 0 : character.id;
        int charTypeid = character == null ? 0 : character.typeid;
        if (cardEquips != null) {
            for (CardEquipRow card : cardEquips) {
                int sub = GamePackets.itemSubGroupIdentify22(card.cardTypeid());
                if (card.partsId() == charId
                        && card.partsTypeid() == charTypeid
                        && sub == GamePackets.CARD_SUB_NPC) {
                    if (card.efeito() == 2) {
                        exp += card.efeitoQntd();
                    } else if (card.efeito() == 1) {
                        pang += card.efeitoQntd();
                    }
                } else if (card.partsId() == 0
                        && card.partsTypeid() == 0
                        && sub == GamePackets.CARD_SUB_TYPE_SPECIAL) {
                    if (card.efeito() == 3) {
                        exp += card.efeitoQntd();
                    } else if (card.efeito() == 2) {
                        pang += card.efeitoQntd();
                    } else if (card.efeito() == 34) {
                        club += card.efeitoQntd();
                    }
                }
            }
        }

        if (passiveTypeids != null) {
            for (int typeid : passiveTypeids) {
                if (cSharpIndexOfNotLast(PASSIVE_PANG_X2, typeid)) {
                    pang += 200;
                }
                if (cSharpIndexOfNotLast(PASSIVE_PANG_X4, typeid)) {
                    pang += 400;
                }
                if (cSharpIndexOfNotLast(PASSIVE_PANG_X1_5, typeid)) {
                    pang += 50;
                }
                if (cSharpIndexOfNotLast(PASSIVE_PANG_X1_4, typeid)) {
                    pang += 40;
                }
                if (cSharpIndexOfNotLast(PASSIVE_PANG_X1_2, typeid)) {
                    pang += 20;
                }
            }
            for (int typeid : passiveTypeids) {
                if (contains(PASSIVE_EXP, typeid)) {
                    exp += 200;
                }
            }
            for (int typeid : passiveTypeids) {
                if (contains(PASSIVE_CLUB, typeid)) {
                    club += 200;
                }
            }
        }

        if (character != null) {
            for (int part : character.partsTypeid) {
                if (contains(HAT_BIRTHDAY, part)) {
                    exp += 20;
                }
                if (contains(HAT_LUA_SOL, part)) {
                    exp += 20;
                    pang += 20;
                }
            }
            if (character.auxparts != null) {
                for (int aux : character.auxparts) {
                    if (aux == KURAFAITO_RING) {
                        club += 10;
                    }
                }
            }
            if (character.auxparts != null) {
                for (int aux : character.auxparts) {
                    if (aux == 0 || GamePackets.itemGroupIdentify(aux) != GamePackets.IFF_GROUP_AUX_PART) {
                        continue;
                    }
                    var part = PangyaIffLoader.auxPart(aux);
                    if (part.isEmpty()) {
                        continue;
                    }
                    IffAuxPartRecord ap = part.get();
                    pang += DropRateResolver.rateContribution(ap.pangRate());
                    exp += DropRateResolver.rateContribution(ap.expRate());
                    drop += DropRateResolver.rateContribution(ap.dropRate());
                }
            }
        }

        if (mascotTypeid > 0) {
            var mascot = PangyaIffLoader.mascot(mascotTypeid);
            if (mascot.isPresent()) {
                IffMascotRecord m = mascot.get();
                pang += DropRateResolver.rateContribution(m.pangRate());
                exp += DropRateResolver.rateContribution(m.expRate());
                drop += DropRateResolver.rateContribution(m.dropRate());
            }
        }

        return new PlayerRates(exp, pang, club, drop);
    }

    /**
     * C# {@code Array.IndexOf(arr, typeid) != arr.length - 1}: not-found (-1) and all
     * indices except the last match.
     */
    static boolean cSharpIndexOfNotLast(int[] arr, int typeid) {
        int idx = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == typeid) {
                idx = i;
                break;
            }
        }
        return idx != arr.length - 1;
    }

    private static boolean contains(int[] arr, int typeid) {
        for (int v : arr) {
            if (v == typeid) {
                return true;
            }
        }
        return false;
    }
}
