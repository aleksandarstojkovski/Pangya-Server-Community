package org.pangya.db;

import org.jdbi.v3.core.Jdbi;

import java.util.List;
import java.util.Optional;

public final class JdbiFriendRepository implements FriendRepository {

    private final Jdbi jdbi;

    public JdbiFriendRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public List<FriendRow> friends(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT f.uid_friend, COALESCE(a."NICK", '') AS nick, f.apelido,
                               f.unknown1, f.unknown2, f.unknown3, f.unknown4, f.unknown5, f.unknown6,
                               f.flag1, f.state_flag
                          FROM pangya.pangya_friend_list f
                          LEFT JOIN pangya.account a ON a."UID" = f.uid_friend
                         WHERE f.uid = :uid
                         ORDER BY f.uid_friend
                        """)
                .bind("uid", uid)
                .map((rs, ctx) -> new FriendRow(
                        rs.getLong("uid_friend"),
                        rs.getString("nick"),
                        rs.getString("apelido"),
                        rs.getInt("unknown1"),
                        rs.getInt("unknown2"),
                        rs.getInt("unknown3"),
                        rs.getInt("unknown4"),
                        rs.getInt("unknown5"),
                        rs.getInt("unknown6"),
                        rs.getInt("flag1"),
                        rs.getInt("state_flag")))
                .list());
    }

    @Override
    public Optional<FriendRow> find(long uid, long friendUid) {
        return friends(uid).stream().filter(f -> f.friendUid() == friendUid).findFirst();
    }

    @Override
    public int count(long uid) {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT COUNT(*) FROM pangya.pangya_friend_list WHERE uid = :uid")
                .bind("uid", uid)
                .mapTo(Integer.class)
                .one());
    }

    @Override
    public void add(long uid, FriendRow friend) {
        jdbi.useHandle(h -> {
            Integer exists = h.createQuery("""
                            SELECT 1 FROM pangya.pangya_friend_list
                             WHERE uid = :uid AND uid_friend = :fid
                            """)
                    .bind("uid", uid)
                    .bind("fid", friend.friendUid())
                    .mapTo(Integer.class)
                    .findOne()
                    .orElse(null);
            if (exists != null) {
                return;
            }
            h.createUpdate("""
                            INSERT INTO pangya.pangya_friend_list (
                                uid, uid_friend, apelido, unknown1, unknown2, unknown3, unknown4,
                                unknown5, unknown6, flag1, state_flag, flag5
                            ) VALUES (
                                :uid, :fid, :apelido, :u1, :u2, :u3, :u4, :u5, :u6, :flag1, :state, 0
                            )
                            """)
                    .bind("uid", uid)
                    .bind("fid", friend.friendUid())
                    .bind("apelido", friend.apelido() == null ? "Friend" : friend.apelido())
                    .bind("u1", friend.unknown1())
                    .bind("u2", friend.unknown2())
                    .bind("u3", friend.unknown3())
                    .bind("u4", friend.unknown4())
                    .bind("u5", friend.unknown5())
                    .bind("u6", friend.unknown6())
                    .bind("flag1", friend.flag1())
                    .bind("state", friend.stateFlag())
                    .execute();
        });
    }

    @Override
    public void updateState(long uid, long friendUid, int stateFlag) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_friend_list
                           SET state_flag = :state
                         WHERE uid = :uid AND uid_friend = :fid
                        """)
                .bind("state", stateFlag)
                .bind("uid", uid)
                .bind("fid", friendUid)
                .execute());
    }

    @Override
    public void updateApelido(long uid, long friendUid, String apelido) {
        jdbi.useHandle(h -> h.createUpdate("""
                        UPDATE pangya.pangya_friend_list
                           SET apelido = :apelido
                         WHERE uid = :uid AND uid_friend = :fid
                        """)
                .bind("apelido", apelido == null ? "Friend" : apelido)
                .bind("uid", uid)
                .bind("fid", friendUid)
                .execute());
    }

    @Override
    public void delete(long uid, long friendUid) {
        jdbi.useHandle(h -> h.createUpdate(
                        "DELETE FROM pangya.pangya_friend_list WHERE uid = :uid AND uid_friend = :fid")
                .bind("uid", uid)
                .bind("fid", friendUid)
                .execute());
    }
}
