package org.pangya.db;

import org.pangya.protocol.game.GamePackets;

import java.util.List;
import java.util.Optional;

/** Ranking SQL replacing C# {@code ProcGetRankRegistryInfo} / {@code CmdRankRegistryInfo}. */
public interface RankRepository {

    List<RegistryRow> registry();

    List<RegistryRow> page(int menu, int item, int page);

    Optional<PlayerSnapshot> playerSnapshot(long uid);

    List<RegistryRow> overallForPlayer(long uid);

    Optional<GamePackets.CharacterInfo> character(long uid);

    /** C# {@code RankCharacter.playerInfoToPacket} source (account + level). */
    Optional<RowSummary> rowSummary(long uid);

    Optional<RegistryRow> findInMenu(int menu, int item, long uid);

    Optional<RegistryRow> findByPosition(int menu, int item, int position);

    Optional<RegistryRow> findByNickname(int menu, int item, String nickname);

    /**
     * C# {@code pangya.GeraRankAll} via {@code CmdUpdateRankRegistry}.
     * Rebuilds {@code pangya_rank_atual} from {@code user_info} / {@code pangya_record}.
     * @return number of registry rows written, or 0 if the swap was skipped
     */
    int geraRankAll();

    record RegistryRow(
            long uid, int currentPosition, int lastPosition, int value, int menu, int item) {}

    record PlayerSnapshot(long uid, String id, String nickname, int level) {}

    record RowSummary(int level, int term, int classType, String id, String nickname) {}
}
