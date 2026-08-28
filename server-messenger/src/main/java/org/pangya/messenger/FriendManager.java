package org.pangya.messenger;

import org.pangya.db.FriendRepository;
import org.pangya.protocol.messenger.MessengerPackets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * C# {@code FriendManager}: in-memory friend + guild-member cache per player.
 */
public final class FriendManager {

    private final Map<Long, FriendRepository.FriendRow> byUid = new ConcurrentHashMap<>();
    private volatile long ownerUid;
    private volatile boolean initialized;

    /** C# {@code init}: load {@code ProcGetFriendAndGuildMemberInfo} into map. */
    public void init(FriendRepository repo, long uid) {
        if (uid <= 0) {
            throw new IllegalArgumentException("friend manager uid is zero");
        }
        clear();
        ownerUid = uid;
        for (FriendRepository.FriendRow row : repo.friendsAndGuildMembers(uid)) {
            byUid.put(row.friendUid(), row);
        }
        initialized = true;
    }

    public void clear() {
        byUid.clear();
        ownerUid = 0;
        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public long ownerUid() {
        return ownerUid;
    }

    /** C# {@code countFriend}: rows with friend flag only. */
    public int countFriend() {
        int count = 0;
        for (FriendRepository.FriendRow row : byUid.values()) {
            if ((row.playerFlag() & MessengerPackets.FRIEND_FLAG) != 0
                    || (row.stateFlag() & MessengerPackets.FLAG_FRIEND) != 0) {
                count++;
            }
        }
        return count;
    }

    /** C# {@code findFriendInAllFriend}. */
    public Optional<FriendRepository.FriendRow> findInAllFriend(long uid) {
        return Optional.ofNullable(byUid.get(uid));
    }

    /** C# {@code findFriend}: registered friend rows only. */
    public Optional<FriendRepository.FriendRow> findFriend(long uid) {
        FriendRepository.FriendRow row = byUid.get(uid);
        if (row == null) {
            return Optional.empty();
        }
        if ((row.playerFlag() & MessengerPackets.FRIEND_FLAG) != 0
                || (row.stateFlag() & MessengerPackets.FLAG_FRIEND) != 0
                || (row.stateFlag() & MessengerPackets.FLAG_REQUEST) != 0) {
            return Optional.of(row);
        }
        return Optional.empty();
    }

    /**
     * C# {@code getAllFriendAndGuildMember}: when {@code excludeBlocked} is true,
     * skip rows with {@code FLAG_BLOCK} (broadcast paths).
     */
    public List<FriendRepository.FriendRow> getAllFriendAndGuildMember(boolean excludeBlocked) {
        List<FriendRepository.FriendRow> out = new ArrayList<>(byUid.size());
        for (FriendRepository.FriendRow row : byUid.values()) {
            if (excludeBlocked && (row.stateFlag() & MessengerPackets.FLAG_BLOCK) != 0) {
                continue;
            }
            out.add(row);
        }
        return out;
    }

    /** C# {@code addFriend} in-memory update (SQL already written by handler). */
    public void putFriend(FriendRepository.FriendRow row) {
        if (row.friendUid() <= 0) {
            throw new IllegalArgumentException("friend uid is zero");
        }
        byUid.merge(row.friendUid(), row, FriendManager::mergeRows);
    }

    public void removeFriend(long uid) {
        byUid.remove(uid);
    }

    public void updateState(long friendUid, int stateFlag) {
        FriendRepository.FriendRow row = byUid.get(friendUid);
        if (row == null) {
            return;
        }
        byUid.put(friendUid, copyRow(row, stateFlag, row.apelido()));
    }

    public void updateApelido(long friendUid, String apelido) {
        FriendRepository.FriendRow row = byUid.get(friendUid);
        if (row == null) {
            return;
        }
        byUid.put(friendUid, copyRow(row, row.stateFlag(), apelido));
    }

    private static FriendRepository.FriendRow mergeRows(
            FriendRepository.FriendRow existing, FriendRepository.FriendRow incoming) {
        if (existing.playerFlag() == incoming.playerFlag()) {
            return incoming;
        }
        if (existing.playerFlag() == 3 || incoming.playerFlag() == 3) {
            return incoming;
        }
        int mergedFlag = existing.playerFlag() | incoming.playerFlag();
        return copyRow(incoming, incoming.stateFlag(), incoming.apelido(), mergedFlag);
    }

    private static FriendRepository.FriendRow copyRow(
            FriendRepository.FriendRow row, int stateFlag, String apelido) {
        return copyRow(row, stateFlag, apelido, row.playerFlag());
    }

    private static FriendRepository.FriendRow copyRow(
            FriendRepository.FriendRow row, int stateFlag, String apelido, int playerFlag) {
        return new FriendRepository.FriendRow(
                row.friendUid(),
                row.nickname(),
                apelido,
                row.unknown1(),
                row.unknown2(),
                row.unknown3(),
                row.unknown4(),
                row.unknown5(),
                row.unknown6(),
                row.flag1(),
                stateFlag,
                row.level(),
                playerFlag);
    }
}
