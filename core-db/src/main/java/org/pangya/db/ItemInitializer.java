package org.pangya.db;

import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffCaddieRecord;
import org.pangya.protocol.iff.IffItemRecord;
import org.pangya.protocol.iff.IffMascotRecord;
import org.pangya.protocol.iff.IffPartIndex;
import org.pangya.protocol.iff.IffSetItemRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * C# {@code ItemManager.initItemFromBuyItem} + {@code getItemOfSetItem} for warehouse rows.
 * Phase 2: caddie/mascot/card mail take via {@link MailAwardRow}.
 */
public final class ItemInitializer {

    /** C# {@code stItem} fields mapped to {@code pangya_item_warehouse}. */
    public record WarehouseInitRow(
            int typeid,
            int qntdDep,
            short c0,
            short c1,
            short c2,
            short c3,
            short c4,
            int flag,
            int itemType,
            int purchase) {

        public static WarehouseInitRow simple(int typeid, int qntd) {
            return new WarehouseInitRow(typeid, qntd, (short) qntd, (short) 0, (short) 0, (short) 0, (short) 0, 0, 2, 1);
        }
    }

    /** C# {@code initItemFromBuyItem} gift/level flags. */
    public record InitContext(int playerLevel, boolean shop, boolean giftOpt, boolean chkLevel) {}

    /** Mail attachment before {@code checkSetItemOnEmail} / take-mail init. */
    public record MailItemRef(int typeid, int qntd, int flagTime, int tempoQntd) {

        public MailItemRef(int typeid, int qntd) {
            this(typeid, qntd, 0, 0);
        }
    }

    /**
     * Resolved mail award: warehouse row and/or group-specific init used by
     * {@code ItemManager.addItem} routing.
     */
    public record MailAwardRow(
            int typeid,
            int qntd,
            WarehouseInitRow warehouse,
            int rentFlag,
            int caddiePeriodDays,
            int mascotTipo,
            String mascotMessage,
            int mascotTimeDays) {

        public int group() {
            return GamePackets.itemGroupIdentify(typeid);
        }

        public static MailAwardRow warehouse(WarehouseInitRow row) {
            return new MailAwardRow(row.typeid(), row.qntdDep(), row, 0, 0, 0, "", 0);
        }

        public static MailAwardRow caddie(int typeid, int rentFlag, int periodDays) {
            return new MailAwardRow(typeid, 1, null, rentFlag, periodDays, 0, "", 0);
        }

        public static MailAwardRow mascot(int typeid, int tipo, String message, int timeDays) {
            return new MailAwardRow(typeid, 1, null, 0, 0, tipo, message, timeDays);
        }

        public static MailAwardRow card(int typeid, int qntd) {
            return new MailAwardRow(typeid, qntd, null, 0, 0, 0, "", 0);
        }

        public static MailAwardRow character(int typeid) {
            return new MailAwardRow(typeid, 1, null, 0, 0, 0, "", 0);
        }

        public static MailAwardRow skin(int typeid) {
            return new MailAwardRow(typeid, 1, WarehouseInitRow.simple(typeid, 1), 0, 0, 0, "", 0);
        }

        /** Timed skin: {@code rentFlag} = C# {@code flag_time}; {@code caddiePeriodDays} = {@code STDA_C_ITEM_TIME}. */
        public static MailAwardRow skinTimed(int typeid, int flagTime, int itemTime) {
            int whFlag = flagTime == 2 ? 0x20 : 0x40;
            WarehouseInitRow wh = new WarehouseInitRow(
                    typeid, 1, (short) 1, (short) 0, (short) 0, (short) 0, (short) 0, whFlag, 2, 1);
            return new MailAwardRow(typeid, 1, wh, flagTime, itemTime, 0, "", 0);
        }

        /** {@code rentFlag} = C# {@code flag_time}; {@code caddiePeriodDays} = {@code STDA_C_ITEM_TIME}. */
        public static MailAwardRow cadItem(int typeid, int flagTime, int itemTime) {
            return new MailAwardRow(typeid, 1, null, flagTime, itemTime, 0, "", 0);
        }
    }

    private ItemInitializer() {}

    /** C# {@code initItemFromEmailItem}: {@code BuyItem.time} from mail row. */
    public static int resolveMailTime(int flagTime, int tempoQntd, int qntd) {
        if (flagTime == 2) {
            if (tempoQntd > 0) {
                return tempoQntd / 24;
            }
            return qntd > 0 ? qntd : 0;
        }
        if (tempoQntd > 0) {
            return tempoQntd;
        }
        return qntd > 0 ? qntd : 0;
    }

    /** C# {@code STDA_TRANSLATE_FLAG_TIME} / {@code STDA_TRANSLATE_FLAG_TIME_TO_HOUR}. */
    public static long stdaTimeSeconds(int flagTime, int itemTime) {
        if (itemTime <= 0) {
            return 0L;
        }
        return switch (flagTime) {
            case 2 -> itemTime * 3600L;
            case 4 -> itemTime * 86400L;
            default -> 0L;
        };
    }

    /**
     * C# {@code getItemOfSetItem}: expand package members with per-item init.
     * Skips character slots; returns empty when IFF unloaded or any member fails init.
     */
    public static List<WarehouseInitRow> expandSetItem(boolean shop, int setTypeid) {
        if (GamePackets.itemGroupIdentify(setTypeid) != GamePackets.IFF_GROUP_SET_ITEM) {
            return List.of();
        }
        Optional<IffSetItemRecord> setOpt = PangyaIffLoader.setItem(setTypeid);
        if (setOpt.isEmpty()) {
            return List.of();
        }
        InitContext ctx = new InitContext(0, shop, false, true);
        IffSetItemRecord set = setOpt.get();
        List<WarehouseInitRow> out = new ArrayList<>();
        for (int i = 0; i < set.packege().total(); i++) {
            int compTypeid = set.packege().itemTypeids()[i];
            if (compTypeid == 0) {
                continue;
            }
            if (GamePackets.itemGroupIdentify(compTypeid) == GamePackets.IFF_GROUP_CHARACTER) {
                continue;
            }
            int compQntd = set.packege().itemQntds()[i];
            if (compQntd <= 0) {
                compQntd = 1;
            }
            Optional<WarehouseInitRow> row = initFromBuyItem(ctx, compTypeid, compQntd, 0);
            if (row.isEmpty()) {
                return List.of();
            }
            out.add(row.get());
        }
        return out;
    }

    /**
     * C# {@code checkSetItemOnEmail} + {@code initItemFromEmailItem} for take-mail:
     * expands set rows, initializes each member. Empty when any step fails.
     */
    public static List<MailAwardRow> resolveMailItems(List<MailItemRef> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        InitContext ctx = new InitContext(0, false, false, true);
        List<MailAwardRow> out = new ArrayList<>();
        for (MailItemRef att : items) {
            if (att.typeid() == 0 || att.qntd() <= 0) {
                return List.of();
            }
            if (GamePackets.itemGroupIdentify(att.typeid()) == GamePackets.IFF_GROUP_SET_ITEM) {
                Optional<IffSetItemRecord> setOpt = PangyaIffLoader.setItem(att.typeid());
                if (setOpt.isEmpty()) {
                    return List.of();
                }
                IffSetItemRecord set = setOpt.get();
                for (int i = 0; i < set.packege().total(); i++) {
                    int compTypeid = set.packege().itemTypeids()[i];
                    if (compTypeid == 0) {
                        continue;
                    }
                    int compQntd = set.packege().itemQntds()[i];
                    if (compQntd <= 0) {
                        compQntd = 1;
                    }
                    Optional<MailAwardRow> row = initMailAward(ctx, compTypeid, compQntd, 0, 0);
                    if (row.isEmpty()) {
                        return List.of();
                    }
                    out.add(row.get());
                }
            } else if (!isMailAwardGroup(att.typeid())) {
                return List.of();
            } else {
                Optional<MailAwardRow> row = initMailAward(
                        ctx, att.typeid(), att.qntd(), att.flagTime(), att.tempoQntd());
                if (row.isEmpty()) {
                    return List.of();
                }
                out.add(row.get());
            }
        }
        return out;
    }

    /** C# {@code checkSetItemOnEmail} preview rows for {@code pacote212}. */
    public static List<byte[]> mailInfoItems(List<MailItemRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return List.of();
        }
        List<MailAwardRow> resolved = resolveMailItems(refs);
        if (resolved.isEmpty()) {
            return refs.stream()
                    .map(r -> GamePackets.mailInfoItem(
                            -1, r.typeid(), 0, r.qntd(), 0, 0L, 0L, 0, 0, "", 0))
                    .toList();
        }
        return resolved.stream()
                .map(r -> {
                    int qntd = r.warehouse() != null ? r.warehouse().qntdDep() : r.qntd();
                    int flagTime = r.rentFlag();
                    int tempoQntd = r.caddiePeriodDays();
                    if (r.group() == GamePackets.IFF_GROUP_MASCOT) {
                        tempoQntd = r.mascotTimeDays();
                        flagTime = r.mascotTimeDays() > 0 ? 4 : 0;
                    }
                    return GamePackets.mailInfoItem(
                            -1, r.typeid(), flagTime, qntd, tempoQntd, 0L, 0L, 0, 0, "", 0);
                })
                .toList();
    }

    /** Groups C# shop {@code initItemFromBuyItem} can grant (set items use {@code expandSetItem}). */
    public static boolean isShopAwardGroup(int typeid) {
        return isMailAwardGroup(typeid)
                && GamePackets.itemGroupIdentify(typeid) != GamePackets.IFF_GROUP_SET_ITEM;
    }

    /**
     * C# {@code initItemFromBuyItem} for shop buy / box grant: same award rows as mail
     * but {@code BuyItem.time} drives rental days ({@code flag_time=4}).
     */
    public static Optional<MailAwardRow> initShopAward(InitContext ctx, int typeid, int qntd, int buyTime) {
        return initBoxAward(ctx, typeid, qntd, buyTime);
    }

    /** C# box {@code BuyItem} before {@code initItemFromBuyItem}: qntd vs rental days. */
    public record BoxAwardParams(int qntd, int buyTime) {}

    /**
     * C# {@code requestOpenBoxMyRoom}/{@code requestOpenBoxMail}: maps draw qntd to
     * {@code BuyItem.qntd/time}, then {@code initItemFromBuyItem}.
     */
    public static BoxAwardParams boxAwardParams(int typeid, int drawQntd) {
        if (drawQntd <= 0) {
            return new BoxAwardParams(0, 0);
        }
        return new BoxAwardParams(drawQntd, 0);
    }

    /**
     * C# GP {@code sendRewardRankAndGrandPrix}: {@code initItemFromBuyItem} with
     * {@code shop=false}, {@code chk_level=1}. Timed slots set {@code flag_time=4}
     * and warehouse {@code flag=0x40} before {@code addItem}.
     */
    public static Optional<MailAwardRow> initGrandPrixAward(
            InitContext ctx, int typeid, int qntd, int rewardTimeDays) {
        if (rewardTimeDays <= 0) {
            return initBoxAward(ctx, typeid, qntd, 0);
        }
        int flagTime = 4;
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET -> {
                Optional<WarehouseInitRow> base = initFromBuyItem(ctx, typeid, 1, 0);
                if (base.isEmpty()) {
                    yield Optional.empty();
                }
                WarehouseInitRow wh = base.get();
                WarehouseInitRow timed = new WarehouseInitRow(
                        wh.typeid(),
                        wh.qntdDep(),
                        wh.c0(),
                        wh.c1(),
                        wh.c2(),
                        wh.c3(),
                        wh.c4(),
                        0x40,
                        wh.itemType(),
                        wh.purchase());
                yield Optional.of(new MailAwardRow(typeid, 1, timed, flagTime, rewardTimeDays, 0, "", 0));
            }
            default -> initBoxAward(ctx, typeid, 1, rewardTimeDays);
        };
    }

    /** Box / lucky-pouch grant row (shop init with box qntd/time mapping). */
    public static Optional<MailAwardRow> initBoxAward(InitContext ctx, int typeid, int drawQntd) {
        BoxAwardParams params = boxAwardParams(typeid, drawQntd);
        return initBoxAward(ctx, typeid, params.qntd(), params.buyTime());
    }

    public static Optional<MailAwardRow> initBoxAward(InitContext ctx, int typeid, int qntd, int buyTime) {
        if (typeid == 0 || qntd <= 0) {
            return Optional.empty();
        }
        int flagTime = buyTime > 0 ? 4 : 0;
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET ->
                    initFromBuyItem(ctx, typeid, qntd, buyTime).map(MailAwardRow::warehouse);
            case GamePackets.IFF_GROUP_CADDIE -> initCaddie(typeid, buyTime);
            case GamePackets.IFF_GROUP_MASCOT -> initMascot(typeid, flagTime, buyTime, qntd);
            case GamePackets.IFF_GROUP_CARD -> initCard(typeid, qntd);
            case GamePackets.IFF_GROUP_CHARACTER -> initCharacter(typeid);
            case GamePackets.IFF_GROUP_SKIN_WAREHOUSE -> initSkin(typeid, flagTime, buyTime, qntd);
            case GamePackets.IFF_GROUP_CAD_ITEM -> initCadItem(typeid, flagTime, buyTime, qntd);
            default -> Optional.empty();
        };
    }

    /** Mail attachment for box reward before {@code sendMessageWithItem}. */
    public static Optional<MailItemRef> boxMailRef(InitContext ctx, int typeid, int drawQntd) {
        return initBoxAward(ctx, typeid, drawQntd).map(row -> {
            int flagTime = row.rentFlag();
            int tempoQntd = row.caddiePeriodDays();
            int qntd = row.warehouse() != null ? row.warehouse().qntdDep() : row.qntd();
            if (row.group() == GamePackets.IFF_GROUP_MASCOT) {
                tempoQntd = row.mascotTimeDays();
                flagTime = row.mascotTimeDays() > 0 ? 4 : 0;
            }
            return new MailItemRef(typeid, qntd, flagTime, tempoQntd);
        });
    }

    /** Groups C# {@code requestTakeItemFomMail} initializes via {@code initItemFromEmailItem}. */
    public static boolean isMailAwardGroup(int typeid) {
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET,
                    GamePackets.IFF_GROUP_SET_ITEM, GamePackets.IFF_GROUP_CADDIE,
                    GamePackets.IFF_GROUP_MASCOT, GamePackets.IFF_GROUP_CARD,
                    GamePackets.IFF_GROUP_CHARACTER, GamePackets.IFF_GROUP_SKIN_WAREHOUSE,
                    GamePackets.IFF_GROUP_CAD_ITEM -> true;
            default -> false;
        };
    }

    /** @deprecated use {@link #isMailAwardGroup(int)} */
    @Deprecated
    public static boolean isWarehouseMailGroup(int typeid) {
        return isMailAwardGroup(typeid)
                && GamePackets.itemGroupIdentify(typeid) != GamePackets.IFF_GROUP_CADDIE
                && GamePackets.itemGroupIdentify(typeid) != GamePackets.IFF_GROUP_MASCOT
                && GamePackets.itemGroupIdentify(typeid) != GamePackets.IFF_GROUP_CARD;
    }

    /** Returns empty when C# would zero {@code _item._typeid}. */
    public static Optional<WarehouseInitRow> initFromBuyItem(InitContext ctx, int typeid, int qntd, int time) {
        if (typeid == 0 || qntd <= 0) {
            return Optional.empty();
        }
        if (!PangyaIffLoader.source().isPresent()) {
            return Optional.of(WarehouseInitRow.simple(typeid, qntd));
        }
        Optional<WarehouseInitRow> row = switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM -> initItem(typeid, qntd);
            case GamePackets.IFF_GROUP_PART -> initPart(typeid, qntd);
            case GamePackets.IFF_GROUP_CLUBSET -> initClubSet(typeid);
            case GamePackets.IFF_GROUP_BALL -> initBall(typeid, qntd, ctx.chkLevel());
            case GamePackets.IFF_GROUP_SET_ITEM -> initSetItem(typeid, qntd);
            default -> Optional.empty();
        };
        if (row.isPresent()) {
            return row;
        }
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET ->
                    Optional.of(WarehouseInitRow.simple(typeid, qntd));
            default -> Optional.empty();
        };
    }

    private static Optional<MailAwardRow> initMailAward(
            InitContext ctx, int typeid, int qntd, int flagTime, int tempoQntd) {
        if (typeid == 0 || qntd <= 0) {
            return Optional.empty();
        }
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET ->
                    initFromBuyItem(ctx, typeid, qntd, 0).map(MailAwardRow::warehouse);
            case GamePackets.IFF_GROUP_CADDIE -> initCaddie(typeid, 0);
            case GamePackets.IFF_GROUP_MASCOT -> initMascot(typeid, flagTime, tempoQntd, qntd);
            case GamePackets.IFF_GROUP_CARD -> initCard(typeid, qntd);
            case GamePackets.IFF_GROUP_CHARACTER -> initCharacter(typeid);
            case GamePackets.IFF_GROUP_SKIN_WAREHOUSE -> initSkin(typeid, flagTime, tempoQntd, qntd);
            case GamePackets.IFF_GROUP_CAD_ITEM -> initCadItem(typeid, flagTime, tempoQntd, qntd);
            default -> Optional.empty();
        };
    }

    private static Optional<MailAwardRow> initSkin(int typeid, int flagTime, int tempoQntd, int qntd) {
        if (!PangyaIffLoader.source().isPresent()) {
            return timedSkinOrPermanent(typeid, flagTime, tempoQntd, qntd);
        }
        if (!PangyaIffLoader.skinIndex().contains(typeid)) {
            return Optional.empty();
        }
        return timedSkinOrPermanent(typeid, flagTime, tempoQntd, qntd);
    }

    private static Optional<MailAwardRow> timedSkinOrPermanent(
            int typeid, int flagTime, int tempoQntd, int qntd) {
        if (flagTime == 2 && tempoQntd > 0) {
            return Optional.of(MailAwardRow.skinTimed(typeid, 2, tempoQntd));
        }
        int days = resolveMailTime(flagTime, tempoQntd, qntd);
        if (days > 0) {
            return Optional.of(MailAwardRow.skinTimed(typeid, 4, days));
        }
        return Optional.of(MailAwardRow.skin(typeid));
    }

    private static Optional<MailAwardRow> initCadItem(int typeid, int flagTime, int tempoQntd, int qntd) {
        if (!PangyaIffLoader.source().isPresent()) {
            return timedCadItemOrPermanent(typeid, flagTime, tempoQntd, qntd);
        }
        if (!PangyaIffLoader.caddieItemIndex().contains(typeid)) {
            return Optional.empty();
        }
        return timedCadItemOrPermanent(typeid, flagTime, tempoQntd, qntd);
    }

    private static Optional<MailAwardRow> timedCadItemOrPermanent(
            int typeid, int flagTime, int tempoQntd, int qntd) {
        if (flagTime == 2 && tempoQntd > 0) {
            return Optional.of(MailAwardRow.cadItem(typeid, 2, tempoQntd));
        }
        int days = resolveMailTime(flagTime, tempoQntd, qntd);
        if (days > 0) {
            return Optional.of(MailAwardRow.cadItem(typeid, 4, days));
        }
        return Optional.of(MailAwardRow.cadItem(typeid, 0, 0));
    }

    private static Optional<MailAwardRow> initCharacter(int typeid) {
        if (!PangyaIffLoader.source().isPresent()) {
            return Optional.of(MailAwardRow.character(typeid));
        }
        if (PangyaIffLoader.character(typeid).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(MailAwardRow.character(typeid));
    }

    private static Optional<MailAwardRow> initCaddie(int typeid, int buyTime) {
        if (!PangyaIffLoader.source().isPresent()) {
            int period = buyTime > 0 ? buyTime : 0;
            return Optional.of(MailAwardRow.caddie(
                    typeid, period > 0 ? GamePackets.CADDIE_RENT_HOLIDAY : 1, period));
        }
        Optional<IffCaddieRecord> caddieOpt = PangyaIffLoader.caddie(typeid);
        if (caddieOpt.isEmpty()) {
            return Optional.empty();
        }
        IffCaddieRecord caddie = caddieOpt.get();
        if (caddie.valorMensal() > 0) {
            int period = buyTime > 0 ? buyTime : 30;
            return Optional.of(MailAwardRow.caddie(typeid, GamePackets.CADDIE_RENT_HOLIDAY, period));
        }
        return Optional.of(MailAwardRow.caddie(typeid, 1, 0));
    }

    private static Optional<MailAwardRow> initMascot(int typeid, int flagTime, int tempoQntd, int qntd) {
        if (!PangyaIffLoader.source().isPresent()) {
            return timedMascotOrPermanent(typeid, flagTime, tempoQntd, qntd);
        }
        Optional<IffMascotRecord> mascotOpt = PangyaIffLoader.mascot(typeid);
        if (mascotOpt.isEmpty()) {
            return Optional.empty();
        }
        return timedMascotOrPermanent(typeid, flagTime, tempoQntd, qntd, mascotOpt.get());
    }

    private static Optional<MailAwardRow> timedMascotOrPermanent(
            int typeid, int flagTime, int tempoQntd, int qntd) {
        return timedMascotOrPermanent(typeid, flagTime, tempoQntd, qntd, null);
    }

    private static Optional<MailAwardRow> timedMascotOrPermanent(
            int typeid, int flagTime, int tempoQntd, int qntd, IffMascotRecord mascot) {
        String message = mascot != null && mascot.messageActive() ? "PangYa SuperSS" : "";
        int days = resolveMailTime(flagTime, tempoQntd, qntd);
        if (days > 0) {
            return Optional.of(MailAwardRow.mascot(typeid, 1, message, days));
        }
        return Optional.of(MailAwardRow.mascot(typeid, 0, message, 0));
    }

    private static Optional<MailAwardRow> initCard(int typeid, int qntd) {
        if (!PangyaIffLoader.source().isPresent()) {
            return Optional.of(MailAwardRow.card(typeid, qntd));
        }
        if (!PangyaIffLoader.cardIndex().contains(typeid)) {
            return Optional.empty();
        }
        return Optional.of(MailAwardRow.card(typeid, qntd));
    }

    private static Optional<WarehouseInitRow> initItem(int typeid, int qntd) {
        Optional<IffItemRecord> itemOpt = PangyaIffLoader.item(typeid);
        if (itemOpt.isEmpty()) {
            return Optional.empty();
        }
        IffItemRecord item = itemOpt.get();
        int slot0 = item.statsPower();
        int qntdDep = qntd;
        short c0;
        if (slot0 > 0 && (slot0 == 1 || slot0 == qntd)) {
            qntdDep = qntd / slot0;
            c0 = (short) slot0;
        } else if (slot0 > 0) {
            c0 = (short) slot0;
        } else {
            c0 = (short) qntd;
        }
        return Optional.of(new WarehouseInitRow(typeid, qntdDep, c0, (short) 0, (short) 0, (short) 0, (short) 0, 0, 2, 1));
    }

    private static Optional<WarehouseInitRow> initPart(int typeid, int qntd) {
        IffPartIndex parts = PangyaIffLoader.partIndex();
        if (!parts.contains(typeid)) {
            return Optional.empty();
        }
        int flag = parts.isUcc(typeid) ? 5 : 0;
        int typeItem = parts.typeItem(typeid);
        return Optional.of(new WarehouseInitRow(
                typeid, qntd, (short) 1, (short) 0, (short) 0, (short) 0, (short) 0, flag, typeItem, 1));
    }

    private static Optional<WarehouseInitRow> initClubSet(int typeid) {
        if (PangyaIffLoader.clubSet(typeid).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new WarehouseInitRow(typeid, 1, (short) 1, (short) 0, (short) 0, (short) 0, (short) 0, 0, 2, 1));
    }

    private static Optional<WarehouseInitRow> initBall(int typeid, int qntd, boolean chkLevel) {
        if (PangyaIffLoader.ballIndex().isEmpty()) {
            return Optional.of(WarehouseInitRow.simple(typeid, qntd));
        }
        if (PangyaIffLoader.ballIndex().stackSize(typeid).isEmpty()) {
            return Optional.empty();
        }
        int stack = PangyaIffLoader.ballStackSize(typeid);
        int qntdDep = qntd;
        short c0;
        if (stack <= 0) {
            c0 = (short) qntd;
        } else if (chkLevel) {
            c0 = (short) qntd;
        } else if (qntd != stack) {
            c0 = (short) qntd;
            qntdDep = 1;
        } else {
            c0 = (short) stack;
            if (qntd > 0) {
                qntdDep = qntd / stack;
            }
        }
        return Optional.of(new WarehouseInitRow(typeid, qntdDep, c0, (short) 0, (short) 0, (short) 0, (short) 0, 0, 2, 1));
    }

    private static Optional<WarehouseInitRow> initSetItem(int typeid, int qntd) {
        if (PangyaIffLoader.setItem(typeid).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new WarehouseInitRow(typeid, qntd, (short) qntd, (short) 0, (short) 0, (short) 0, (short) 0, 0, 2, 1));
    }
}
