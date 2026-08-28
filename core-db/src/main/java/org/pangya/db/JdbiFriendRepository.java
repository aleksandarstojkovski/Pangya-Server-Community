package org.pangya.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class JdbiFriendRepository implements FriendRepository {

    private static final String FRIENDS_AND_GUILD_SQL = """
            WITH player_guild AS (
                SELECT COALESCE(
                    NULLIF((SELECT "Guild_UID" FROM pangya.account WHERE "UID" = :uid), 0),
                    (SELECT gm."GUILD_UID" FROM pangya.pangya_guild_member gm
                      WHERE gm."MEMBER_UID" = :uid LIMIT 1),
                    0
                ) AS guild_uid
            ),
            combined AS (
                SELECT f.uid_friend
                  FROM pangya.pangya_friend_list f
                 WHERE f.uid = :uid
                UNION ALL
                SELECT gm."MEMBER_UID"
                  FROM pangya.pangya_guild g
                  INNER JOIN pangya.pangya_guild_member gm ON g."GUILD_UID" = gm."GUILD_UID"
                  CROSS JOIN player_guild pg
                 WHERE g."GUILD_UID" = pg.guild_uid
                   AND pg.guild_uid > 0
                   AND gm."MEMBER_STATE_FLAG" < 9
                   AND (g."GUILD_STATE" NOT IN (4, 5)
                        OR g."GUILD_CLOSURE_DATE" IS NULL
                        OR NOW() < g."GUILD_CLOSURE_DATE")
            ),
            grouped AS (
                SELECT uid_friend, COUNT(*)::int AS cnt
                  FROM combined
                 GROUP BY uid_friend
            )
            SELECT COALESCE(a."NICK", '') AS nick,
                   z.uid_friend,
                   COALESCE(y.apelido, 'Friend') AS apelido,
                   COALESCE(y.unknown1, -1) AS unknown1,
                   COALESCE(y.unknown2, 0) AS unknown2,
                   COALESCE(y.unknown3, -1) AS unknown3,
                   COALESCE(y.unknown4, 0) AS unknown4,
                   COALESCE(y.unknown5, 0) AS unknown5,
                   COALESCE(y.unknown6, 0) AS unknown6,
                   COALESCE(y.flag1, -1) AS flag1,
                   COALESCE(y.state_flag, 0) + COALESCE(a."Sex", 0) AS state_flag,
                   COALESCE(ui."level", 0) AS level,
                   CASE WHEN z.cnt = 2 OR y.uid_friend IS NULL THEN z.cnt + 1 ELSE z.cnt END AS player_flag
              FROM grouped z
              LEFT JOIN pangya.pangya_friend_list y
                ON y.uid_friend = z.uid_friend AND y.uid = :uid
              LEFT JOIN pangya.account a ON a."UID" = z.uid_friend
              LEFT JOIN pangya.user_info ui ON ui."UID" = z.uid_friend
             ORDER BY z.uid_friend
            """;

    private final Jdbi jdbi;

    public JdbiFriendRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public List<FriendRow> friends(long uid) {
        return jdbi.withHandle(h -> h.createQuery("""
                        SELECT f.uid_friend, COALESCE(a."NICK", '') AS nick, f.apelido,
                               f.unknown1, f.unknown2, f.unknown3, f.unknown4, f.unknown5, f.unknown6,
                               f.flag1, f.state_flag, COALESCE(ui."level", 0) AS level, 1 AS player_flag
                          FROM pangya.pangya_friend_list f
                          LEFT JOIN pangya.account a ON a."UID" = f.uid_friend
                          LEFT JOIN pangya.user_info ui ON ui."UID" = f.uid_friend
                         WHERE f.uid = :uid
                         ORDER BY f.uid_friend
                        """)
                .bind("uid", uid)
                .map(JdbiFriendRepository::mapRow)
                .list());
    }

    @Override
    public List<FriendRow> friendsAndGuildMembers(long uid) {
        return jdbi.withHandle(h -> h.createQuery(FRIENDS_AND_GUILD_SQL)
                .bind("uid", uid)
                .map(JdbiFriendRepository::mapRow)
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

    private static FriendRow mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
        return new FriendRow(
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
                rs.getInt("state_flag"),
                rs.getInt("level"),
                rs.getInt("player_flag"));
    }
}
