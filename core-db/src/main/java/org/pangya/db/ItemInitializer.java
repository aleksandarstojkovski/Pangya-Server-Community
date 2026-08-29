package org.pangya.db;

import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffItemRecord;
import org.pangya.protocol.iff.IffPartIndex;
import org.pangya.protocol.iff.IffSetItemRecord;
import org.pangya.protocol.iff.PangyaIffLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * C# {@code ItemManager.initItemFromBuyItem} + {@code getItemOfSetItem} for warehouse rows.
 * Phase 1: ITEM, PART, CLUBSET, BALL, SET_ITEM validation paths used by shop/gift/mail.
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

    /** Mail attachment typeid + qntd before {@code checkSetItemOnEmail} / take-mail init. */
    public record MailItemRef(int typeid, int qntd) {}

    private ItemInitializer() {}

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
     * expands set rows, initializes each warehouse member. Empty when any step fails.
     */
    public static List<WarehouseInitRow> resolveMailItems(List<MailItemRef> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        InitContext ctx = new InitContext(0, false, false, true);
        List<WarehouseInitRow> out = new ArrayList<>();
        for (MailItemRef att : items) {
            if (att.typeid() == 0 || att.qntd() <= 0) {
                return List.of();
            }
            if (GamePackets.itemGroupIdentify(att.typeid()) == GamePackets.IFF_GROUP_SET_ITEM) {
                List<WarehouseInitRow> expanded = expandSetItem(false, att.typeid());
                if (expanded.isEmpty()) {
                    return List.of();
                }
                out.addAll(expanded);
            } else if (!isWarehouseMailGroup(att.typeid())) {
                return List.of();
            } else {
                Optional<WarehouseInitRow> row = initFromBuyItem(ctx, att.typeid(), att.qntd(), 0);
                if (row.isEmpty()) {
                    return List.of();
                }
                out.add(row.get());
            }
        }
        return out;
    }

    /** Warehouse groups C# {@code requestTakeItemFomMail} adds via {@code addItem}. */
    public static boolean isWarehouseMailGroup(int typeid) {
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM, GamePackets.IFF_GROUP_PART,
                    GamePackets.IFF_GROUP_BALL, GamePackets.IFF_GROUP_CLUBSET,
                    GamePackets.IFF_GROUP_SET_ITEM -> true;
            default -> false;
        };
    }

    /** Returns empty when C# would zero {@code _item._typeid}. */
    public static Optional<WarehouseInitRow> initFromBuyItem(InitContext ctx, int typeid, int qntd, int time) {
        if (typeid == 0 || qntd <= 0) {
            return Optional.empty();
        }
        if (!PangyaIffLoader.source().isPresent()) {
            return Optional.of(WarehouseInitRow.simple(typeid, qntd));
        }
        return switch (GamePackets.itemGroupIdentify(typeid)) {
            case GamePackets.IFF_GROUP_ITEM -> initItem(typeid, qntd);
            case GamePackets.IFF_GROUP_PART -> initPart(typeid, qntd);
            case GamePackets.IFF_GROUP_CLUBSET -> initClubSet(typeid);
            case GamePackets.IFF_GROUP_BALL -> initBall(typeid, qntd, ctx.chkLevel());
            case GamePackets.IFF_GROUP_SET_ITEM -> initSetItem(typeid, qntd);
            default -> Optional.of(WarehouseInitRow.simple(typeid, qntd));
        };
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
