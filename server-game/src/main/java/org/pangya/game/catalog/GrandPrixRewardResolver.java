package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.pangya.db.ItemInitializer;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffRewardSlots;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;

/** C# {@code GrandPrix.sendRewardRankAndGrandPrix} grant eligibility and expansion. */
public final class GrandPrixRewardResolver {

    record GrantChecks(IntPredicate canOverlap, BiPredicate<Long, Integer> ownsAward) {
        static GrantChecks from(InventoryRepository inventory) {
            return new GrantChecks(inventory::itemCanOverlap, inventory::ownsAwardTypeid);
        }
    }

    private GrandPrixRewardResolver() {}

    /**
     * C# {@code sendRewardRankAndGrandPrix}: expands set items via
     * {@code getItemOfSetItem}, initializes each row, skips timed slots when owned.
     */
    public static List<ItemInitializer.MailAwardRow> resolveRewardAwards(
            InventoryRepository inventory, long uid, int playerLevel, IffRewardSlots reward) {
        return resolveRewardAwards(GrantChecks.from(inventory), uid, playerLevel, reward);
    }

    static List<ItemInitializer.MailAwardRow> resolveRewardAwards(
            GrantChecks checks, long uid, int playerLevel, IffRewardSlots reward) {
        if (reward == null) {
            return List.of();
        }
        ItemInitializer.InitContext ctx = new ItemInitializer.InitContext(playerLevel, false, false, true);
        List<ItemInitializer.MailAwardRow> out = new ArrayList<>();
        for (int i = 0; i < IffRewardSlots.SLOTS; i++) {
            int typeid = reward.typeids()[i];
            if (typeid == 0) {
                continue;
            }
            int qntd;
            int rewardTimeDays;
            if (reward.time()[i] > 0) {
                qntd = 1;
                rewardTimeDays = reward.time()[i];
            } else {
                qntd = reward.qntd()[i];
                rewardTimeDays = 0;
            }
            if (qntd <= 0) {
                continue;
            }
            if (!shouldGrant(checks, uid, typeid)) {
                continue;
            }
            if (GamePackets.itemGroupIdentify(typeid) == GamePackets.IFF_GROUP_SET_ITEM) {
                List<ItemInitializer.WarehouseInitRow> components =
                        ItemInitializer.expandSetItem(false, typeid);
                for (ItemInitializer.WarehouseInitRow comp : components) {
                    ItemInitializer.initGrandPrixAward(ctx, comp.typeid(), comp.qntdDep(), 0)
                            .ifPresent(out::add);
                }
            } else {
                ItemInitializer.initGrandPrixAward(ctx, typeid, qntd, rewardTimeDays).ifPresent(out::add);
            }
        }
        return out;
    }

    /** C# {@code (IsCanOverlapped && group != CAD_ITEM) || !ownerItem}. */
    static boolean shouldGrant(GrantChecks checks, long uid, int typeid) {
        int group = GamePackets.itemGroupIdentify(typeid);
        if (checks.canOverlap().test(typeid) && group != GamePackets.IFF_GROUP_CAD_ITEM) {
            return true;
        }
        return !checks.ownsAward().test(uid, typeid);
    }

    static boolean shouldGrant(InventoryRepository inventory, long uid, int typeid) {
        return shouldGrant(GrantChecks.from(inventory), uid, typeid);
    }
}
