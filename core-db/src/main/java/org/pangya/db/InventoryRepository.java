package org.pangya.db;

import org.pangya.protocol.game.GamePackets;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Game inventory replacing C# {@code CmdWarehouseItem} / {@code CmdCharacterInfo} / {@code CmdCaddieInfo}. */
public interface InventoryRepository {

    List<GamePackets.WarehouseItem> warehouse(long uid);

    List<GamePackets.CharacterInfo> characters(long uid);

    List<GamePackets.CaddieInfo> caddies(long uid);

    GamePackets.UserEquip userEquip(long uid);

    List<GamePackets.MascotInfo> mascots(long uid);

    /**
     * C# {@code sIff.findMascot} + {@code msg.active}: SQL {@code iff_mascot}
     * stand-in used by {@code requestUpdatePCBangMascot}.
     */
    boolean mascotMessageEnabled(int typeid);

    List<GamePackets.CardInfo> cards(long uid);

    long pang(long uid);

    long cookie(long uid);

    void equipCharacter(long uid, int characterId);

    void equipCaddie(long uid, int caddieId);

    void equipBallAndClub(long uid, int ballTypeid, int clubsetId);

    void equipMascot(long uid, int mascotId);

    void updateCharacterParts(long uid, GamePackets.CharacterInfo character);

    List<GamePackets.CounterItem> counters(long uid);

    List<GamePackets.AchievementInfo> achievements(long uid);

    Optional<ShopItem> shopItem(int typeid);

    ShopBuyResult buyShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie);

    /**
     * C# gift {@code consomeMoeda} without adding warehouse to the sender.
     * Catalog miss is {@link GamePackets#BUY_FAIL_NOT_BUYABLE}.
     */
    ShopBuyResult giftShopItem(long uid, int typeid, int qntd, int clientPang, int clientCookie);

    /**
     * C# {@code ItemManager.transferItem} for IFF ITEM: move warehouse C0 and pang
     * (seller gets {@link GamePackets#shopSellerGain}).
     */
    PersonalShopMove transferPersonalShop(
            long sellerUid, long buyerUid, int itemId, int typeid, int qntd, long unitPang);

    void setPangCookie(long uid, long pang, long cookie);

    /**
     * C# {@code PlayerInfo.df.pang} from {@code pangya_dolfini_locker}. Missing
     * row is {@code 0} (character-create insert stand-in).
     */
    long dolfiniLockerPang(long uid);

    /**
     * C# {@code requestUpdateDolfiniLockerPang}: opt {@code 1} deposit,
     * opt {@code 0} withdraw. Fail codes are the CHANNEL sys values
     * ({@link GamePackets#LOCKER_PANG_DEPOSIT_ERR} /
     * {@link GamePackets#LOCKER_PANG_WITHDRAW_ERR}).
     */
    LockerPangMoveResult updateDolfiniLockerPang(long uid, int opt, long pang);

    /**
     * C# {@code ProcAddItemDolfiniLocker}: {@code valid=0} then insert
     * {@code pangya_dolfini_locker_item}. Empty when the warehouse row is missing.
     */
    OptionalLong addDolfiniLockerItem(long uid, int itemId);

    /**
     * C# {@code ProcMoveItemDolfiniLocker}: restore {@code valid=1} and
     * {@code flag=0}. Empty when the locker row is missing.
     */
    Optional<GamePackets.WarehouseItem> removeDolfiniLockerItem(long uid, long index, int itemId);

    OptionalLong dolfiniLockerIndex(long uid, int itemId);

    void deleteDolfiniLockerByItemId(long uid, int itemId);

    /**
     * C# {@code CmdTutorialInfo} / {@code pangya.tutorial}. Missing row is zeros.
     */
    TutorialFlags tutorial(long uid);

    /**
     * C# {@code CmdUpdateTutorial}. Upsert stand-in for character-create insert.
     */
    void updateTutorial(long uid, int rookie, int beginner, int advancer);

    void setLevel(long uid, int level);

    void deleteWarehouseByTypeid(long uid, int typeid);

    /** C# {@code CmdDeleteRental}: {@code valid = 0}. */
    boolean deleteWarehouseById(long uid, int itemId);

    /**
     * C# {@code sIff.findPart}: SQL {@code iff_part.valor_rental} stand-in.
     * Empty when the typeid is missing.
     */
    OptionalLong partValorRental(int typeid);

    void upsertPartValorRental(int typeid, long valorRental);

    void deletePartIff(int typeid);

    /** C# {@code CmdExtendRental}: {@code EndDate} = now + 7 days. */
    void setWarehouseEndDate(long uid, int itemId, Instant endDate);

    /**
     * C# {@code PapelShopSystem.dropBalls} / {@code dropBigBall} then warehouse
     * add + pang charge. Empty catalog is {@link GamePackets#PAPEL_PLAY_ERR_BALLS}.
     */
    PapelPlayResult playPapel(long uid, boolean big);

    /**
     * C# {@code requestPayCaddieHolyDay}: SQL {@code iff_caddie.valor_mensal}
     * stand-in. Fail codes are swallowed on the wire as {@code 0x93} u8 1.
     */
    CaddieHolidayResult payCaddieHoliday(long uid, int caddieId);

    /**
     * C# {@code requestChangeMascotMessage}: SQL {@code iff_mascot} stand-in.
     */
    MascotMessageResult changeMascotMessage(long uid, int mascotId, String message);

    /**
     * C# {@code requestCadieCauldronExchange}: SQL {@code cadie_magic_box}
     * stand-in for IFF {@code CadieMagicBox}. {@code seq} is the client ushort;
     * lookup uses {@code seq + 1}.
     */
    CadieExchangeResult cadieExchange(long uid, int seq, int requested, int level, int[] typeids, int[] ids);

    int addCard(long uid, int typeid, int qntd);

    void deleteCardByTypeid(long uid, int typeid);

    /** Insert or increment a warehouse consumable (C# {@code C0}). */
    int addWarehouseItem(long uid, int typeid, int qntd);

    /**
     * C# {@code ItemManager.removeItem} for warehouse ITEM: consume {@code qntd}
     * from C0. Empty when the row is missing or C0 is insufficient. Remaining 0
     * deletes the row ({@code qntd_dep}).
     */
    OptionalInt consumeWarehouseByTypeid(long uid, int typeid, int qntd);

    /**
     * C# {@code CometRefillSystem.findCometRefill}: SQL {@code pangya_comet_refill}
     * stand-in. Empty when the typeid is missing.
     */
    Optional<CometRefill> cometRefill(int typeid);

    /** Test helper: insert or replace a {@code pangya_comet_refill} row. */
    void upsertCometRefill(int typeid, int min, int max);

    void deleteCometRefill(int typeid);

    /**
     * C# {@code CmdAttendanceRewardInfo} / {@code ProcGetAttendanceReward}.
     * Empty when the player has no row (zeros + null {@code last_login}).
     */
    Optional<AttendanceReward> attendanceReward(long uid);

    /** C# {@code CmdUpdateAttendanceReward} / {@code ProcUpdateAttendanceReward}. */
    void upsertAttendanceReward(long uid, AttendanceReward ari);

    void deleteAttendanceReward(long uid);

    /**
     * C# {@code CmdAttendanceRewardItemInfo} rows of
     * {@code pangya_attendance_table_item_reward} for one {@code tipo}.
     */
    List<AttendanceCatalogItem> attendanceCatalog(int tipo);

    List<AttendanceCatalogItem> attendanceCatalogAll();

    /** Test helper: insert one catalog row (SQL stand-in, no IFF {@code IsExist}). */
    void upsertAttendanceCatalog(int typeid, int qntd, int tipo);

    void deleteAttendanceCatalog(int typeid);

    /**
     * C# {@code sIff.findTimeLimitItem}: SQL {@code iff_time_limit_item} stand-in.
     */
    Optional<TimeLimitItem> timeLimitItem(int typeid);

    /** Test helper: insert or replace a TimeLimitItem catalog row. */
    void upsertTimeLimitItem(int typeid, int tipo, int percent, int timeMinutes);

    void deleteTimeLimitItem(int typeid);

    /**
     * C# {@code sIff.findClubSet}: SQL {@code iff_clubset.work_shop_tipo} stand-in.
     * Empty when the typeid is missing.
     */
    OptionalInt clubSetWorkShopTipo(int typeid);

    void upsertClubSetWorkShopTipo(int typeid, int tipo);

    void deleteClubSetIff(int typeid);

    /**
     * C# {@code sIff.findClubSet}: SQL {@code iff_clubset} Stats/SlotStats stand-in.
     * Empty when the typeid is missing.
     */
    Optional<ClubSetIff> clubSetIff(int typeid);

    void upsertClubSetIff(int typeid, int tipo, short[] stats, short[] slots);

    void upsertClubSetIff(int typeid, int tipo, short[] stats, short[] slots, int tipoRankS);

    void upsertClubSetIff(
            int typeid, int tipo, short[] stats, short[] slots, int tipoRankS, int totalRecovery);

    void upsertClubSetIff(
            int typeid,
            int tipo,
            short[] stats,
            short[] slots,
            int tipoRankS,
            int totalRecovery,
            int flagTransformar);

    /** C# {@code sIff.findClubSetOriginal}: SQL rows for a lottery special. */
    boolean clubSetOriginalAny(int specialTypeid);

    List<ClubSetOriginal> clubSetOriginals(int specialTypeid);

    void upsertClubSetOriginal(int specialTypeid, int originalTypeid, short[] slots);

    void deleteClubSetOriginal(int specialTypeid);

    /** C# {@code sIff.findCutinInfomation}: SQL stand-in for CutinInfomation.iff. */
    Optional<CutinIff> cutinIff(int typeid);

    void upsertCutinIff(
            int typeid, int sector, int condition, int[] imageTypes, int tempo, String[] sprites);

    void deleteCutinIff(int typeid);

    /** C# {@code sBoxSystem.findBox/drawBox}: deterministic SQL reward row. */
    Optional<BoxMailReward> boxMailReward(int boxTypeid);

    void upsertBoxMailReward(
            int boxTypeid, int rewardTypeid, int rewardQntd, int openedTypeid, String message);

    void deleteBoxMailReward(int boxTypeid);

    /** C# {@code sIff.findItem}: SQL {@code iff_item} row exists. */
    boolean itemIff(int typeid);

    void upsertItemIff(int typeid);

    void deleteItemIff(int typeid);

    /** C# {@code sIff.findCard}: SQL {@code iff_card} row exists. */
    boolean cardIff(int typeid);

    Optional<CardSpecialIff> cardSpecialIff(int typeid);

    void upsertCardSpecialIff(
            int typeid, int rarity, int probability, int effect, int effectValue, int effectTime);

    void deleteCardIff(int typeid);

    /** C# {@code CardSystem.findCardPack/draws}: ordered deterministic SQL draw. */
    List<CardPackReward> cardPackRewards(int packTypeid);

    void upsertCardPackReward(int packTypeid, int seq, int cardTypeid);

    void deleteCardPackRewards(int packTypeid);

    /**
     * C# {@code ItemManager.removeItem} for cards: consume {@code qntd} from
     * {@code QNTD}. Empty when missing or insufficient. Remaining 0 deletes.
     */
    OptionalInt consumeCardByTypeid(long uid, int typeid, int qntd);

    Optional<short[]> clubSetLevelUpLimit(int tipo, int rank);

    boolean clubSetLevelUpAny(int tipo);

    void upsertClubSetLevelUpLimit(int tipo, int rank, short[] c);

    void deleteClubSetLevelUpLimit(int tipo, int rank);

    Optional<int[]> clubSetLevelUpProb(int tipo);

    void upsertClubSetLevelUpProb(int tipo, int[] c);

    void deleteClubSetLevelUpProb(int tipo);

    /** C# {@code sIff.findClubSetWorkShopRankExp}: SQL row exists. */
    boolean clubSetRankExp(int tipo);

    /** C# {@code ClubSetWorkShopRankUpExp.rank[6]}. Empty when tipo is missing. */
    Optional<int[]> clubSetRankExpRanks(int tipo);

    void upsertClubSetRankExp(int tipo);

    void upsertClubSetRankExp(int tipo, int[] ranks);

    void deleteClubSetRankExp(int tipo);

    /** C# {@code CmdUpdateClubSetWorkshop} {@code F_RESET}: C + workshop C/level/rank/recovery, keep mastery. */
    void resetClubSetWorkshopAndC(long uid, int itemId);

    void setClubSetWorkshop(long uid, int itemId, short[] workshopC, int level, int rank, int recovery);

    /** C# {@code CmdUpdateClubSetStats}: warehouse {@code C0}–{@code C4}. */
    void setWarehouseClubC(long uid, int itemId, short[] c);

    /**
     * C# {@code sIff.findEnchant}: SQL {@code iff_enchant.pang}. Empty when missing.
     */
    OptionalLong enchantPang(int typeid);

    /** C# {@code CmdUpdateClubSetWorkshop} recovery_pts. */
    void setClubSetRecoveryPts(long uid, int itemId, int recoveryPts);

    /** C# {@code CmdUpdateClubSetWorkshop} {@code F_TRANSFER_MASTERY_PTS}. */
    void setClubSetMasteryPts(long uid, int itemId, int masteryPts);

    /** C# {@code PlayerInfo.findItemBuff} by typeid. */
    Optional<ItemBuffRow> itemBuff(long uid, int typeid);

    /** C# {@code ProcUseItemBuff}: insert and return {@code index}. */
    long insertItemBuff(
            long uid, int typeid, int tipo, int percent, Instant useDate, Instant endDate);

    /** C# {@code ProcUpdateItemBuffTime}. */
    void updateItemBuff(long uid, long index, int typeid, int tipo, Instant endDate);

    void deleteItemBuff(long uid, int typeid);

    /**
     * C# {@code requestLoloCardCompose}: SQL {@code iff_card} stand-in for
     * IFF Card + {@code CardSystem.drawsLoloCardCompose}.
     */
    LoloComposeResult loloCompose(long uid, long clientPang, int t0, int t1, int t2);

    /**
     * C# {@code requestCharacterMasteryExpand}: SQL {@code iff_character_mastery}
     * stand-in. {@code level} is {@code PlayerInfo.level}.
     */
    CharMasteryResult expandCharacterMastery(long uid, int typeid, int id, int level);

    /**
     * C# {@code requestCharacterStatsUp}: SQL {@code iff_character} /
     * {@code iff_enchant} / {@code iff_character_mastery} stand-ins.
     * {@code level} is {@code PlayerInfo.level}.
     */
    CharStatsResult characterStatsUp(long uid, int stat, GamePackets.CharacterInfo client, int level);

    /**
     * C# {@code requestCharacterStatsDown}: SQL {@code iff_character} stand-in.
     */
    CharStatsResult characterStatsDown(long uid, int stat, GamePackets.CharacterInfo client);

    /**
     * C# {@code requestCharacterCardEquip}: SQL {@code iff_card} +
     * {@code pangya_card_equip}. {@code slot} is {@code char_card_slot}.
     */
    CharCardResult characterCardEquip(long uid, int charTypeid, int charId, int cardTypeid, int cardId, int slot);

    /**
     * C# {@code requestCharacterCardEquipWithPatcher}: consume Club Patcher
     * {@code 0x1A00018F} and equip slot 4 (character) or 8 (caddie).
     */
    CharCardResult characterCardEquipWithPatcher(
            long uid, int charTypeid, int charId, int cardTypeid, int cardId, int slot);

    /**
     * C# {@code requestCharacterRemoveCard}: consume warehouse removedor,
     * return the card, clear {@code pangya_card_equip}.
     */
    CharCardResult characterRemoveCard(
            long uid, int charTypeid, int charId, int removerTypeid, int removerId, int slot);

    record ShopItem(int typeid, int pangPrice, int cookiePrice, boolean canOverlap) {}

    record ShopBuyResult(
            int code,
            int itemId,
            int typeid,
            int qntdDep,
            long pang,
            long cookie,
            long pangSpent,
            long cookieSpent) {

        public static ShopBuyResult fail(int code) {
            return new ShopBuyResult(code, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    record PersonalShopMove(
            GamePackets.WarehouseItem sellerPacket,
            GamePackets.WarehouseItem buyerPacket,
            long sellerPangAfter,
            long buyerPangAfter,
            long sellerGain) {}

    record PapelPlayResult(
            int code,
            List<GamePackets.PapelBall> balls,
            List<GamePackets.PapelAward> awards,
            long pang,
            long cookie) {

        public static PapelPlayResult fail(int code) {
            return new PapelPlayResult(code, List.of(), List.of(), 0, 0);
        }
    }

    record CaddieHolidayResult(int code, int caddieId, long pang) {

        public static CaddieHolidayResult fail(long pang) {
            return new CaddieHolidayResult(1, 0, pang);
        }
    }

    record MascotMessageResult(int code, int mascotId, String message, long pang) {

        public static MascotMessageResult fail(long pang) {
            return new MascotMessageResult(1, -1, "", pang);
        }
    }

    record CadieExchangeResult(
            int code,
            int seq,
            List<GamePackets.PapelAward> awards,
            int receiveTypeid,
            int receiveId,
            int receiveQntd,
            int qntdDep,
            int flagTime) {

        public static CadieExchangeResult fail(int code) {
            return new CadieExchangeResult(code, 0, List.of(), 0, 0, 0, 0, 0);
        }
    }

    record LoloComposeResult(
            int code,
            long pangAfter,
            long pangSpent,
            List<GamePackets.PapelAward> awards,
            int cardTipo,
            int cardTypeid) {

        public static LoloComposeResult fail(int code) {
            return new LoloComposeResult(code, 0, 0, List.of(), 0, 0);
        }
    }

    record CharMasteryResult(int code, List<GamePackets.PapelAward> awards, int mastery) {

        public static CharMasteryResult fail(int code) {
            return new CharMasteryResult(code, List.of(), 0);
        }
    }

    record CharStatsResult(
            int code,
            long pangAfter,
            long pangSpent,
            byte[] pcl,
            int stat,
            int typeid,
            int id) {

        public static CharStatsResult fail(int code) {
            return new CharStatsResult(code, 0, 0, new byte[5], 0, 0, 0);
        }
    }

    record CharCardResult(int code, List<GamePackets.PapelAward> awards, int cardTypeid) {

        public static CharCardResult fail(int code) {
            return new CharCardResult(code, List.of(), 0);
        }
    }

    record LockerPangMoveResult(int code, long playerPang, long lockerPang, long moved) {

        public static LockerPangMoveResult fail(int code) {
            return new LockerPangMoveResult(code, 0, 0, 0);
        }
    }

    /** C# {@code ctx_comet_refill}: typeid + {@code QntdRange} min/max. */
    record CometRefill(int typeid, int min, int max) {}

    /**
     * C# {@code AttendanceRewardInfoEx} without {@code login} (runtime-only).
     * {@code lastLogin} is SQL {@code last_login}; missing/null means first check.
     */
    record AttendanceReward(
            int counter,
            int nowTypeid,
            int nowQntd,
            int afterTypeid,
            int afterQntd,
            Instant lastLogin) {}

    /** C# {@code AttendanceRewardItemCtx}: catalog draw row. */
    record AttendanceCatalogItem(int typeid, int qntd, int tipo) {}

    /**
     * C# IFF {@code ClubSet}: {@code work_shop.tipo}, {@code Stats.getSlot},
     * {@code SlotStats.getSlot}, {@code work_shop.tipo_rank_s},
     * {@code work_shop.total_recovery}, {@code work_shop.flag_transformar}.
     */
    record ClubSetIff(
            int tipo, short[] stats, short[] slots, int tipoRankS, int totalRecovery, int flagTransformar) {}

    /** C# {@code findClubSetOriginal} row: original typeid + {@code SlotStats}. */
    record ClubSetOriginal(int typeid, short[] slots) {}

    /** C# {@code CutinInformation}: fields serialized by {@code requestActiveCutin}. */
    record CutinIff(
            int typeid, int sector, int condition, int[] imageTypes, int tempo, String[] sprites) {}

    /** C# {@code ctx_box_item} plus Box opened-typeid/message for mail delivery. */
    record BoxMailReward(
            int boxTypeid, int rewardTypeid, int rewardQntd, int openedTypeid, String message) {}

    /** C# IFF Card special effect fields. */
    record CardSpecialIff(int typeid, int effect, int effectValue, int effectTime) {}

    record CardPackReward(int seq, int cardTypeid) {}

    /** C# IFF {@code TimeLimitItem}: {@code type}, {@code percent}, {@code time} minutes. */
    record TimeLimitItem(int typeid, int tipo, int percent, int timeMinutes) {}

    /** C# {@code ItemBuffEx} SQL row ({@code pangya_item_buff}). */
    record ItemBuffRow(
            long index,
            int typeid,
            Instant useDate,
            Instant endDate,
            int tipo,
            int percent,
            int useYn) {}

    record TutorialFlags(int rookie, int beginner, int advancer) {}
}
