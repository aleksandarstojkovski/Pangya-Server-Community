package org.pangya.game.catalog;

import org.pangya.db.InventoryRepository;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.iff.IffRewardSlots;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** C# {@code GrandPrix.sendRewardRankAndGrandPrix} grant eligibility. */
public final class GrandPrixRewardResolver {

    public record Grant(int typeid, int qntd) {}

    private GrandPrixRewardResolver() {}

    public static List<Grant> grantsFromReward(
            InventoryRepository inventory, long uid, IffRewardSlots reward) {
        if (reward == null) {
            return List.of();
        }
        Map<Integer, Integer> merged = new LinkedHashMap<>();
        for (int i = 0; i < IffRewardSlots.SLOTS; i++) {
            int typeid = reward.typeids()[i];
            if (typeid == 0 || reward.time()[i] > 0) {
                continue;
            }
            int qntd = reward.qntd()[i];
            if (qntd <= 0 || !shouldGrant(inventory, uid, typeid)) {
                continue;
            }
            merged.merge(typeid, qntd, Integer::sum);
        }
        List<Grant> out = new ArrayList<>(merged.size());
        for (var entry : merged.entrySet()) {
            out.add(new Grant(entry.getKey(), entry.getValue()));
        }
        return out;
    }

    static boolean shouldGrant(InventoryRepository inventory, long uid, int typeid) {
        int group = GamePackets.itemGroupIdentify(typeid);
        if (group == GamePackets.IFF_GROUP_CAD_ITEM) {
            return !inventory.ownsAwardTypeid(uid, typeid);
        }
        if (inventory.itemCanOverlap(typeid)) {
            return true;
        }
        return !inventory.ownsWarehouseTypeid(uid, typeid);
    }
}
