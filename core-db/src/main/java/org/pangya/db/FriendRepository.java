package org.pangya.db;

import java.util.List;
import java.util.Optional;

/** Messenger SQL replacing C# {@code ProcAddFriend} / {@code ProcUpdateFriendInfo} / DELETE. */
public interface FriendRepository {

    int FRIEND_LIST_LIMIT = 50;

    List<FriendRow> friends(long uid);

    Optional<FriendRow> find(long uid, long friendUid);

    int count(long uid);

    void add(long uid, FriendRow friend);

    void updateState(long uid, long friendUid, int stateFlag);

    void updateApelido(long uid, long friendUid, String apelido);

    void delete(long uid, long friendUid);

    record FriendRow(
            long friendUid,
            String nickname,
            String apelido,
            int unknown1,
            int unknown2,
            int unknown3,
            int unknown4,
            int unknown5,
            int unknown6,
            int flag1,
            int stateFlag) {}
}
