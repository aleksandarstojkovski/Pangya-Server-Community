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

    record RegistryRow(
            long uid, int currentPosition, int lastPosition, int value, int menu, int item) {}

    record PlayerSnapshot(long uid, String id, String nickname, int level) {}
}
