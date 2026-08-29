package org.pangya.game;

import org.pangya.network.session.Session;
import org.pangya.protocol.game.GamePackets;
import org.pangya.protocol.packet.PacketReader;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory C# {@code Room} subset: {@code RoomInfoEx} + start-game + Practice hole state. */
final class GameRoom {

    private static final SecureRandom RNG = new SecureRandom();

    final GamePackets.RoomInfo info = new GamePackets.RoomInfo();
    int tipo;
    /** C# {@code RoomInfoEx.grand_prix.dados_typeid}; zero outside GP. */
    int grandPrixTypeid;
    final int channelId;
    final List<Session> players = new ArrayList<>();
    final ConcurrentHashMap<Integer, GamePackets.PlayerRoomInfo> playerInfos = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Integer, PlayerShot> shots = new ConcurrentHashMap<>();
    volatile boolean inGame;
    volatile long startMillis;
    volatile GameCourse course;
    /** C# {@code m_weather_lounge} set by GM {@code CCG_CHANGE_WEATHER}. */
    volatile int weatherLounge;
    /** C# Versus {@code m_count_pause}; max {@link GamePackets#VERSUS_PAUSE_MAX}. */
    volatile int pauseCount;
    /** C# Versus {@code finish_char_intro}; cleared when all players have sent {@code 0x34}. */
    final ConcurrentHashMap<Integer, Boolean> charIntro = new ConcurrentHashMap<>();
    /** C# Versus {@code setLoadHole}; cleared when all players have sent {@code 0x11}. */
    final ConcurrentHashMap<Integer, Boolean> loadHole = new ConcurrentHashMap<>();
    /** C# Versus {@code m_player_turn.oid}; 0 until {@code sendReplyFinishLoadHole}. */
    volatile int turnOid;
    /** C# Match {@code changeHole} clear bonus applied once per game. */
    volatile boolean matchClearBonusApplied;
    /** C# Match {@code Team} red/blue pang accumulators (index 0/1). */
    final MatchTeam[] matchTeams = {new MatchTeam(), new MatchTeam()};
    /** C# {@code m_player_report_game} UIDs that already sent {@code 0x3A}. */
    final ConcurrentHashMap<Long, Boolean> reported = new ConcurrentHashMap<>();
    /** C# {@code PersonalShopManager} per-owner shops. */
    final ConcurrentHashMap<Long, PersonalShop> shops = new ConcurrentHashMap<>();
    /**
     * C# {@code PlayerGameInfo.used_item.v_active}: oid → typeid → count/slots
     * from {@code UserEquip.item_slot} at game start.
     */
    final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, ActiveUse>> activeUses =
            new ConcurrentHashMap<>();
    /**
     * C# {@code used_item.v_passive}: oid → typeid → use count this game.
     */
    final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Integer>> passiveUses =
            new ConcurrentHashMap<>();
    /** C# {@code used_item.rate.drop} + {@code angel_wings} for {@code requestInitDrop}. */
    final ConcurrentHashMap<Integer, PlayerDropCtx> dropCtx = new ConcurrentHashMap<>();
    /** C# {@code pgi.finish_item_used}: oid → finished item settlement. */
    final ConcurrentHashMap<Integer, Boolean> finishItemUsed = new ConcurrentHashMap<>();
    /** C# server {@code RateValue.clubset} from {@code SERVERINFO.CLUBMASTERYRATE}. */
    int clubMasteryServerRate = 100;
    /** C# {@code used_item.club}: per-player club mastery accumulator. */
    final ConcurrentHashMap<Integer, ClubMasteryState> clubMastery = new ConcurrentHashMap<>();
    /**
     * C# {@code PlayerGameInfo.flag} ({@link GamePackets#FLAG_GAME_PLAYING} …).
     */
    final ConcurrentHashMap<Integer, Integer> gameFlags = new ConcurrentHashMap<>();
    /** C# {@code PlayerGameInfo.sys_achieve} in-memory counter buffer until {@code finish_and_update}. */
    final ConcurrentHashMap<Long, ConcurrentHashMap<Integer, Integer>> pendingAchievementCounters =
            new ConcurrentHashMap<>();
    /** C# {@code init_first_hole_gz} barrier set by CLIENT {@code 0x137}. */
    final ConcurrentHashMap<Integer, Boolean> gzFirstHole = new ConcurrentHashMap<>();
    /** C# Versus {@code m_timer} generation; increment cancels the running turn. */
    private volatile long turnTimerGen;
    private volatile Thread turnTimer;

    GameRoom(GamePackets.CreateRoom req, int numero, int masterUid, int ratePang, int rateExp, int channelId) {
        this.tipo = req.tipo();
        this.channelId = channelId;
        info.numero = numero;
        info.maxPlayer = req.maxPlayer();
        info.numPlayer = 0;
        info.holes = req.holes();
        info.course = req.course();
        info.modo = req.modo();
        info.timeVs = req.timeVs();
        info.time30s = req.time30s();
        info.natural = req.natural();
        info.artefato = req.artefato();
        info.master = masterUid;
        info.ratePang = ratePang;
        info.rateExp = rateExp;
        info.thirtyS = 30;
        info.state = 1;
        info.tipoShow = GamePackets.tipoShow(tipo);
        info.tipoEx = GamePackets.tipoEx(tipo);
        if (tipo == GamePackets.TIPO_GRAND_PRIX) {
            info.gpActive = 1;
            info.gpDadosTypeid = grandPrixTypeid;
        }
        RNG.nextBytes(info.key);
        if (tipo == GamePackets.TIPO_PRACTICE) {
            info.name = "Single Player Practice Mode";
        } else {
            info.name = req.name() == null ? "" : req.name();
        }
        String password = req.password() == null ? "" : req.password();
        if (!password.isEmpty()) {
            info.password = password;
            info.senhaFlag = 0;
        } else {
            info.senhaFlag = 1;
        }
    }

    boolean hiddenFromLobby() {
        return GamePackets.hiddenFromLobby(tipo);
    }

    /**
     * C# {@code room.requestChangeInfoRoom}. Returns true when every change was applied.
     */
    synchronized boolean applyInfoChange(PacketReader reader) {
        if (reader.remaining() < 3) {
            return false;
        }
        reader.i16();
        int numInfo = reader.u8();
        if (numInfo <= 0) {
            return false;
        }
        for (int i = 0; i < numInfo; i++) {
            if (reader.remaining() < 1) {
                return false;
            }
            int type = reader.u8();
            switch (type) {
                case GamePackets.ROOM_CHANGE_NAME -> {
                    String title = reader.pstr();
                    if (hiddenFromLobby()) {
                        info.name = "Single Player Practice Mode";
                    } else {
                        info.name = title == null ? "" : title;
                    }
                }
                case GamePackets.ROOM_CHANGE_PASSWORD -> {
                    String pwd = reader.pstr();
                    if (pwd == null) {
                        pwd = "";
                    }
                    info.password = pwd;
                    info.senhaFlag = pwd.isEmpty() ? 1 : 0;
                }
                case GamePackets.ROOM_CHANGE_TIPO -> {
                    int next = reader.u8();
                    if (next >= 0 && next <= GamePackets.TIPO_MAX) {
                        tipo = next;
                        info.tipoShow = GamePackets.tipoShow(tipo);
                        info.tipoEx = GamePackets.tipoEx(tipo);
                    }
                }
                case GamePackets.ROOM_CHANGE_COURSE -> info.course = reader.u8();
                case GamePackets.ROOM_CHANGE_HOLES -> info.holes = reader.u8();
                case GamePackets.ROOM_CHANGE_MODO -> info.modo = reader.u8();
                case GamePackets.ROOM_CHANGE_TIME_VS -> {
                    int seconds = reader.u16();
                    if (seconds > 0) {
                        info.timeVs = seconds * 1000;
                    }
                }
                case GamePackets.ROOM_CHANGE_MAX_PLAYER -> {
                    int max = reader.u8();
                    if (max > players.size()) {
                        info.maxPlayer = max;
                    }
                }
                case GamePackets.ROOM_CHANGE_TIME_30S -> {
                    int minutes = reader.u8();
                    if (minutes > 0) {
                        info.time30s = minutes * 60_000;
                    }
                }
                case GamePackets.ROOM_CHANGE_STATE_FLAG -> info.stateFlag = reader.u8();
                case GamePackets.ROOM_CHANGE_GALLERY -> {
                    int gallery = reader.u8();
                    info.galleryNum = gallery;
                    info.thirtyS = gallery;
                }
                case GamePackets.ROOM_CHANGE_HOLE_REPEAT -> info.holeRepeat = reader.u8();
                case GamePackets.ROOM_CHANGE_FIXED_HOLE -> info.fixedHole = reader.u32();
                case GamePackets.ROOM_CHANGE_ARTEFATO -> info.artefato = reader.u32();
                case GamePackets.ROOM_CHANGE_NATURAL -> info.natural = reader.u32();
                default -> {
                    return false;
                }
            }
        }
        return true;
    }

    synchronized boolean addPlayer(Session session) {
        if (inGame) {
            return false;
        }
        if (info.numPlayer >= info.maxPlayer && info.maxPlayer > 0) {
            return false;
        }
        if (!players.contains(session)) {
            players.add(session);
            info.numPlayer = players.size();
        }
        return true;
    }

    synchronized void putPlayerInfo(Session session, GamePackets.PlayerRoomInfo info) {
        playerInfos.put(session.oid(), info);
    }

    GamePackets.PlayerRoomInfo playerInfo(Session session) {
        return playerInfos.get(session.oid());
    }

    synchronized List<GamePackets.PlayerRoomInfo> playerInfoSnapshot() {
        List<GamePackets.PlayerRoomInfo> out = new ArrayList<>();
        for (Session session : players) {
            GamePackets.PlayerRoomInfo info = playerInfos.get(session.oid());
            if (info != null) {
                out.add(info);
            }
        }
        return out;
    }

    synchronized void removePlayer(Session session) {
        players.remove(session);
        info.numPlayer = players.size();
        shots.remove(session.oid());
        playerInfos.remove(session.oid());
        charIntro.remove(session.oid());
        loadHole.remove(session.oid());
        activeUses.remove(session.oid());
        passiveUses.remove(session.oid());
        finishItemUsed.remove(session.oid());
        clubMastery.remove(session.oid());
        gameFlags.remove(session.oid());
        pendingAchievementCounters.remove(session.player().uid);
        shops.remove(session.player().uid);
        for (PersonalShop shop : shops.values()) {
            shop.viewers.remove(session.player().uid);
        }
    }

    /**
     * C# {@code room.updateMaster(null)} after the previous master left and
     * {@code m_pGame == null}. Prefers a GM ({@code capability & 4}).
     */
    synchronized Session electMaster() {
        if (players.isEmpty()) {
            return null;
        }
        Session master = null;
        for (Session session : players) {
            if ((session.player().capability & 4) != 0) {
                master = session;
                break;
            }
        }
        if (master == null) {
            master = players.getFirst();
        }
        info.master = (int) master.player().uid;
        info.stateFlag = (master.player().capability & 4) != 0
                ? GamePackets.ROOM_MASTER_GM_FLAG
                : 0;
        GamePackets.PlayerRoomInfo pri = playerInfos.get(master.oid());
        if (pri != null) {
            pri.stateFlag |= GamePackets.PLAYER_MASTER_BIT | GamePackets.PLAYER_READY_BIT;
            playerInfos.put(master.oid(), pri);
        }
        return master;
    }

    synchronized Session findByUid(long uid) {
        for (Session session : players) {
            if (session.player().uid == uid) {
                return session;
            }
        }
        return null;
    }

    synchronized List<Session> snapshot() {
        return List.copyOf(players);
    }

    /** C# Match team id from {@code PlayerRoomInfo.stateFlag} bit 0. */
    int matchTeamId(Session session) {
        GamePackets.PlayerRoomInfo info = playerInfo(session);
        if (info == null) {
            return 0;
        }
        return info.stateFlag & GamePackets.PLAYER_TEAM_BIT;
    }

    void resetMatchTeams() {
        matchClearBonusApplied = false;
        for (MatchTeam team : matchTeams) {
            team.pang = 0;
            team.bonusPang = 0;
        }
    }

    /**
     * C# {@code Match.requestUpdateTeamPang}: copy team pang/bonus onto each
     * member's {@link PlayerShot} before finish-game credit.
     */
    void mergeMatchTeamPangToPlayers() {
        for (Session member : snapshot()) {
            MatchTeam team = matchTeams[matchTeamId(member)];
            GameRoom.PlayerShot shot = shots.computeIfAbsent(member.oid(), id -> new PlayerShot());
            shot.pang = team.pang;
            shot.bonusPang = team.bonusPang;
        }
    }

    /** C# {@code Team} pang/bonus running totals during Match. */
    static final class MatchTeam {
        long pang;
        long bonusPang;
    }

    /**
     * C# Versus {@code setFinishCharIntroAndCheckAllFinishCharIntroAndClear}.
     */
    synchronized boolean markCharIntro(Session session) {
        charIntro.put(session.oid(), Boolean.TRUE);
        return charIntro.size() >= players.size();
    }

    synchronized void clearCharIntro() {
        charIntro.clear();
    }

    /**
     * C# Versus {@code setLoadHole} then {@code checkAllLoadHole}.
     */
    synchronized boolean markLoadHole(Session session) {
        loadHole.put(session.oid(), Boolean.TRUE);
        return loadHole.size() >= players.size();
    }

    synchronized void clearLoadHole() {
        loadHole.clear();
    }

    /**
     * C# {@code init_turn_hole_start} then {@code getNextPlayerTurnHole}: join order
     * (equal hole-start scores keep {@code m_players} order).
     */
    synchronized int startHoleTurn() {
        if (players.isEmpty()) {
            turnOid = 0;
            return 0;
        }
        turnOid = players.getFirst().oid();
        return turnOid;
    }

    /**
     * C# {@code requestCalculePlayerTurn} after hole-start popped the first player.
     */
    synchronized int rotateTurn() {
        if (turnOid == 0 || players.isEmpty()) {
            return 0;
        }
        int idx = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).oid() == turnOid) {
                idx = i;
                break;
            }
        }
        turnOid = players.get((idx + 1) % players.size()).oid();
        return turnOid;
    }

    /**
     * C# {@code VersusBase.startTime} / {@code MakeTime(m_ri.time_vs)}.
     * {@code timeVs == 0} does not start a timer (IT rooms).
     */
    void startTurnTimer(int millis, Runnable onTimeout) {
        stopTurnTimer();
        if (millis <= 0 || onTimeout == null) {
            return;
        }
        long gen = ++turnTimerGen;
        turnTimer = Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(millis);
                if (gen == turnTimerGen) {
                    onTimeout.run();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
    }

    void stopTurnTimer() {
        turnTimerGen++;
        Thread timer = turnTimer;
        turnTimer = null;
        if (timer != null) {
            timer.interrupt();
        }
    }

    void broadcast(byte[] packet) {
        for (Session session : snapshot()) {
            session.send(packet);
        }
    }

    static final class PlayerShot {
        int hole;
        float x;
        float y;
        float z;
        int shotState;
        int tempo;
        /** C# {@code PlayerGameInfo.bar_space} state for Versus timeout. */
        int barState;
        /** C# {@code PlayerGameInfo.tempo} (1 after turn timer, distinct from shot tempo). */
        int turnTempo;
        /** C# {@code pgi.data.time_out}. */
        int timeOuts;
        /** C# {@code PlayerGameInfo.degree} set by GM versus wind. */
        int degree;
        /** C# {@code PlayerGameInfo.shot_data_for_cube}. */
        byte[] shotEndLocation;
        /** C# Versus {@code PlayerGameInfo.finish_shot2}. */
        int finishShot2;
        /** C# {@code shot_sync.state_shot.display} ulState from last sync. */
        int displayState;
        /** C# {@code pgi.data.pang} running total from client sync. */
        long pang;
        /** C# {@code pgi.data.bonus_pang} running total (+ server clear bonus). */
        long bonusPang;
        /** C# {@code pgi.location} before last sync (for long-putt distance). */
        float lastX;
        float lastY;
        float lastZ;
        /** C# {@code pgi.shot_data.acerto_pangya_flag} from {@code CLIENT_SHOT}. */
        byte acertoPangyaFlag;
        /** C# {@code pgi.data.tacada_num} strokes on the current hole. */
        int tacadaNum;
        /** C# {@code pgi.finish_game}; set by {@code requestFinishGame}. */
        boolean finishGame;
        /** Parsed {@code UserInfoEx} from {@code CLIENT_MY_STATISTICS}. */
        GamePackets.UserInfoEx userInfo;
        /** C# {@code pgi.data.exp} from {@code requestFinishExpGame}. */
        int gameExp;
        /** C# {@code pgi.progress.tacada} per-hole stroke counts. */
        final int[] holeTacada = new int[18];
        /** C# {@code pgi.progress.par_hole} per-hole par values. */
        final int[] holePar = new int[18];
        /** C# {@code pgi.drop_list.v_drop} accumulated hole-end drops for {@code requestSaveDrop}. */
        final java.util.List<GamePackets.DropItem> holeDrops = new java.util.ArrayList<>();
    }

    boolean allPlayersFinishedGame() {
        int finished = 0;
        for (Session member : snapshot()) {
            PlayerShot shot = shots.get(member.oid());
            if (shot != null && shot.finishGame) {
                finished++;
            }
        }
        return finished > 0 && finished == players.size();
    }

    /** C# {@code UsedItem.Active}: use count vs equipped slot indices. */
    static final class ActiveUse {
        int count;
        final java.util.List<Integer> slotIndices = new java.util.ArrayList<>();
    }

    /**
     * C# {@code requestIniItemUsedGame} item_slot loop.
     */
    void initActiveItems(int oid, int[] itemSlot) {
        ConcurrentHashMap<Integer, ActiveUse> uses = new ConcurrentHashMap<>();
        if (itemSlot != null) {
            for (int i = 0; i < itemSlot.length; i++) {
                int slotIndex = i;
                int typeid = itemSlot[i];
                if (typeid == 0) {
                    continue;
                }
                uses.compute(typeid, (k, current) -> {
                    if (current == null) {
                        current = new ActiveUse();
                    }
                    current.slotIndices.add(slotIndex);
                    return current;
                });
            }
        }
        activeUses.put(oid, uses);
    }

    /**
     * C# {@code v_active} find + count++. False when missing or already spent.
     */
    boolean tryUseActive(int oid, int typeid) {
        ConcurrentHashMap<Integer, ActiveUse> uses = activeUses.get(oid);
        if (uses == null) {
            return false;
        }
        ActiveUse use = uses.get(typeid);
        if (use == null || use.count >= use.slotIndices.size()) {
            return false;
        }
        use.count++;
        return true;
    }

    /** C# {@code DropSystem.stCourseInfo} + {@code used_item.rate} per player. */
    record PlayerDropCtx(int rateDrop, int rateExp, int ratePang, int rateClub, int angelWings, int charMotion) {}

    void initDropCtx(int oid, int rateDrop, int rateExp, int ratePang, int rateClub, int angelWings, int charMotion) {
        dropCtx.put(oid, new PlayerDropCtx(rateDrop, rateExp, ratePang, rateClub, angelWings, charMotion));
    }

    PlayerDropCtx dropCtx(int oid) {
        return dropCtx.getOrDefault(oid, new PlayerDropCtx(100, 100, 100, 100, 0, 0));
    }

    /** C# {@code requestInitItemUsedGame} passive warehouse / ball / auxpart registration. */
    void initPassiveItem(int oid, int typeid) {
        passiveUses.computeIfAbsent(oid, k -> new ConcurrentHashMap<>()).putIfAbsent(typeid, 0);
    }

    /**
     * {@code 0} success (count++). {@link GamePackets#STDA_ERROR_TYPE_GAME} when
     * warehouse C0 &lt; 1. {@link GamePackets#AUTO_COMMAND_ERR_USED} when missing
     * from {@code v_passive} or already spent.
     */
    int tryUsePassive(int oid, int typeid, int warehouseQntd) {
        if (warehouseQntd < 1) {
            return GamePackets.STDA_ERROR_TYPE_GAME;
        }
        ConcurrentHashMap<Integer, Integer> uses = passiveUses.get(oid);
        if (uses == null || !uses.containsKey(typeid)) {
            return GamePackets.AUTO_COMMAND_ERR_USED;
        }
        int used = uses.get(typeid);
        if (used >= warehouseQntd) {
            return GamePackets.AUTO_COMMAND_ERR_USED;
        }
        uses.put(typeid, used + 1);
        return 0;
    }

    /** C# {@code GameBase.requestUpdateItemUsedGame} per-hole passive / ball / auxpart counts. */
    void updatePassiveOnHoleFinish(int oid, int ballTypeid, int[] auxparts) {
        ConcurrentHashMap<Integer, Integer> uses = passiveUses.get(oid);
        if (uses == null) {
            return;
        }
        for (var entry : uses.entrySet()) {
            int typeid = entry.getKey();
            if (typeid == PassiveItems.TIME_BOOSTER || typeid == GamePackets.TYPEID_AUTO_COMMAND) {
                continue;
            }
            if (PassiveItems.isPassiveItem(typeid) && PassiveItems.isPerGameExp(typeid)) {
                entry.setValue(entry.getValue() + 1);
            }
        }
        if (PassiveItems.isTrackedBall(ballTypeid) && uses.containsKey(ballTypeid)) {
            uses.merge(ballTypeid, 1, Integer::sum);
        }
        if (auxparts != null) {
            for (int aux : auxparts) {
                if (PassiveItems.isAuxPart(aux) && uses.containsKey(aux)) {
                    uses.merge(aux, 1, Integer::sum);
                }
            }
        }
    }

    /** C# {@code requestFinishItemUsedGame}: final +1 for {@code passive_item_exp_1perGame}. */
    void finishExpPerGamePassive(int oid) {
        ConcurrentHashMap<Integer, Integer> uses = passiveUses.get(oid);
        if (uses == null) {
            return;
        }
        for (int typeid : uses.keySet()) {
            if (PassiveItems.isPerGameExp(typeid)) {
                uses.merge(typeid, 1, Integer::sum);
            }
        }
    }

    /** C# {@code UsedItem.club}: equipped clubset mastery accumulation. */
    static final class ClubMasteryState {
        int clubTypeid;
        int clubId;
        int accumulated;
        float clubRate = 1.0f;
        int playerRateClub = 100;
    }

    /** C# {@code requestIniItemUsedGame} clubset block. */
    void initClubMastery(int oid, int clubTypeid, int clubId, float clubRate, int playerRateClub) {
        if (clubTypeid == 0 || clubId == 0) {
            return;
        }
        ClubMasteryState state = new ClubMasteryState();
        state.clubTypeid = clubTypeid;
        state.clubId = clubId;
        state.clubRate = clubRate;
        state.playerRateClub = playerRateClub;
        clubMastery.put(oid, state);
    }

    /** C# {@code GameBase.requestUpdateItemUsedGame} club mastery increment. */
    void updateClubMasteryOnHoleFinish(int oid) {
        ClubMasteryState state = clubMastery.get(oid);
        if (state == null) {
            return;
        }
        int increment = (int) (10.0f
                * state.clubRate
                * transfServerRate(clubMasteryServerRate)
                * transfServerRate(state.playerRateClub));
        state.accumulated += Math.max(0, increment);
    }

    ClubMasteryState clubMasteryState(int oid) {
        return clubMastery.get(oid);
    }

    static float transfServerRate(int value) {
        return value <= 0 ? 1.0f : value / 100.0f;
    }

    boolean hasFinishItemUsed(int oid) {
        return Boolean.TRUE.equals(finishItemUsed.get(oid));
    }

    void markFinishItemUsed(int oid) {
        finishItemUsed.put(oid, true);
    }

    /**
     * C# {@code PlayerGameInfo.flag}. Missing oid is {@link GamePackets#FLAG_GAME_PLAYING}.
     */
    int gameFlag(int oid) {
        return gameFlags.getOrDefault(oid, GamePackets.FLAG_GAME_PLAYING);
    }

    void setGameFlag(int oid, int flag) {
        gameFlags.put(oid, flag);
    }

    /** C# {@code sys_achieve.incrementCounter} during a game (flushed at {@code finish_and_update}). */
    void addPendingAchievementCounter(long uid, int counterTypeid, int delta) {
        if (counterTypeid == 0 || delta == 0) {
            return;
        }
        pendingAchievementCounters
                .computeIfAbsent(uid, k -> new ConcurrentHashMap<>())
                .merge(counterTypeid, delta, Integer::sum);
    }

    /** Takes and clears pending counters for one player at game finish. */
    java.util.Map<Integer, Integer> takePendingAchievementCounters(long uid) {
        ConcurrentHashMap<Integer, Integer> pending = pendingAchievementCounters.remove(uid);
        return pending == null ? java.util.Map.of() : java.util.Map.copyOf(pending);
    }

    void clearPendingAchievementCounters() {
        pendingAchievementCounters.clear();
    }

    void initGameFlags() {
        gameFlags.clear();
        for (Session member : snapshot()) {
            gameFlags.put(member.oid(), GamePackets.FLAG_GAME_PLAYING);
        }
    }

    static final class PersonalShop {
        final long ownerUid;
        final String ownerNick;
        volatile String name = "";
        volatile int visitCount;
        volatile long pangSale;
        /** C# {@code STATE.OPEN} after a successful item listing. */
        volatile boolean open;
        final List<GamePackets.PersonalShopItem> items = new ArrayList<>();
        final ConcurrentHashMap<Long, Boolean> viewers = new ConcurrentHashMap<>();

        PersonalShop(long ownerUid, String ownerNick) {
            this.ownerUid = ownerUid;
            this.ownerNick = ownerNick == null ? "" : ownerNick;
        }
    }

    static final class ShopReply {
        final boolean broadcast;
        final byte[] packet;

        ShopReply(boolean broadcast, byte[] packet) {
            this.broadcast = broadcast;
            this.packet = packet;
        }
    }

    ShopReply openEditShop(Session session) {
        long uid = session.player().uid;
        String nick = session.player().nickname;
        shops.computeIfAbsent(uid, k -> new PersonalShop(uid, nick));
        return new ShopReply(true, GamePackets.shopEditOk(nick, (int) uid));
    }

    ShopReply cancelEditShop(Session session) {
        PersonalShop shop = shops.get(session.player().uid);
        if (shop == null) {
            return new ShopReply(false, GamePackets.shopCancelFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CANCEL_NONE)));
        }
        return new ShopReply(true, GamePackets.shopCancelOk(session.player().nickname));
    }

    ShopReply closeShop(Session session) {
        PersonalShop shop = shops.remove(session.player().uid);
        if (shop == null) {
            return new ShopReply(false, GamePackets.shopEditFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CLOSE_NONE)));
        }
        return new ShopReply(true, GamePackets.shopCloseOk(session.player().nickname, (int) session.player().uid));
    }

    ShopReply changeShopName(Session session, String name) {
        if (name == null || name.isEmpty()) {
            return new ShopReply(false, GamePackets.shopNameFail(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_EMPTY)));
        }
        long uid = session.player().uid;
        for (PersonalShop other : shops.values()) {
            if (other.ownerUid != uid && name.equals(other.name)) {
                return new ShopReply(false, GamePackets.shopNameFail(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_DUP)));
            }
        }
        PersonalShop shop = shops.get(uid);
        if (shop == null) {
            return new ShopReply(false, GamePackets.shopNameFail(GamePackets.shopSys(GamePackets.SHOP_ERR_NAME_NONE)));
        }
        shop.name = name;
        return new ShopReply(true, GamePackets.shopNameOk(name, (int) uid, session.player().nickname));
    }

    byte[] visitCountShop(Session session) {
        PersonalShop shop = shops.get(session.player().uid);
        if (shop == null) {
            return GamePackets.shopVisitFail(GamePackets.shopSys(GamePackets.SHOP_ERR_VISIT_NONE));
        }
        return GamePackets.shopVisitOk(shop.visitCount);
    }

    byte[] pangShop(Session session) {
        PersonalShop shop = shops.get(session.player().uid);
        if (shop == null) {
            return GamePackets.shopPangFail(GamePackets.shopSys(GamePackets.SHOP_ERR_PANG_NONE));
        }
        return GamePackets.shopPangOk(shop.pangSale);
    }

    synchronized byte[] viewShop(Session session, long ownerUid) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null) {
            return GamePackets.shopViewFail(GamePackets.shopSys(GamePackets.SHOP_ERR_VIEW_NONE));
        }
        if (!shop.open || shop.items.isEmpty()) {
            return GamePackets.shopViewFail(GamePackets.SHOP_ERR_VIEW_DEFAULT);
        }
        long uid = session.player().uid;
        if (shop.viewers.containsKey(uid)) {
            return GamePackets.shopViewFail(GamePackets.SHOP_ERR_VIEW_DEFAULT);
        }
        if (shop.viewers.size() >= GamePackets.SHOP_VISIT_LIMIT) {
            return GamePackets.shopViewFail(GamePackets.shopSys(GamePackets.SHOP_ERR_VIEW_LIMIT));
        }
        shop.viewers.put(uid, Boolean.TRUE);
        shop.visitCount++;
        return GamePackets.shopViewOk(shop.ownerNick, shop.name, (int) shop.ownerUid, List.copyOf(shop.items));
    }

    synchronized boolean listShopItems(long uid, List<GamePackets.PersonalShopItem> items) {
        PersonalShop shop = shops.get(uid);
        if (shop == null) {
            return false;
        }
        shop.items.clear();
        for (GamePackets.PersonalShopItem item : items) {
            shop.items.add(item.copy());
        }
        shop.open = true;
        return true;
    }

    synchronized boolean shopIsOpen(long ownerUid) {
        PersonalShop shop = shops.get(ownerUid);
        return shop != null && shop.open;
    }

    synchronized boolean shopHasViewer(long ownerUid, long uid) {
        PersonalShop shop = shops.get(ownerUid);
        return shop != null && shop.viewers.containsKey(uid);
    }

    synchronized GamePackets.PersonalShopItem findListedItem(long ownerUid, int itemId) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null || itemId <= 0) {
            return null;
        }
        for (GamePackets.PersonalShopItem item : shop.items) {
            if (item.id == itemId) {
                return item;
            }
        }
        return null;
    }

    synchronized int consumeListedItem(long ownerUid, int itemId, int qntd) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null) {
            return 0;
        }
        shop.items.removeIf(item -> {
            if (item.id != itemId) {
                return false;
            }
            if (item.qntd <= qntd) {
                return true;
            }
            item.qntd -= qntd;
            return false;
        });
        return shop.items.size();
    }

    synchronized void addPangSale(long ownerUid, long gain) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop != null) {
            shop.pangSale += gain;
        }
    }

    synchronized List<Session> shopSoldTargets(long ownerUid) {
        List<Session> out = new ArrayList<>();
        Session owner = findByUid(ownerUid);
        if (owner != null) {
            out.add(owner);
        }
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null) {
            return out;
        }
        for (Long uid : shop.viewers.keySet()) {
            Session viewer = findByUid(uid);
            if (viewer != null && viewer != owner) {
                out.add(viewer);
            }
        }
        return out;
    }

    byte[] closeViewShop(Session session, long ownerUid) {
        PersonalShop shop = shops.get(ownerUid);
        if (shop == null) {
            return GamePackets.shopCloseViewFail(GamePackets.shopSys(GamePackets.SHOP_ERR_CLOSE_VIEW_NONE));
        }
        if (shop.viewers.remove(session.player().uid) == null) {
            return GamePackets.shopCloseViewFail(GamePackets.SHOP_ERR_CLOSE_VIEW_DEFAULT);
        }
        return GamePackets.shopCloseViewOk();
    }
}
