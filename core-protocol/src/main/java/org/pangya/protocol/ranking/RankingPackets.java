package org.pangya.protocol.ranking;

import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

import java.util.List;

/**
 * JP {@code PacketRanking.cs} + {@code RankingServer.requestLogin}/{@code sendFirstPage}.
 */
public final class RankingPackets {

    public static final int CLIENT_CONNECT = 0x00;
    public static final int CLIENT_REQUEST_PLAYER_INFO = 0x01;
    public static final int CLIENT_REQ_SEARCH_PLAYER_IN_RANKING = 0x02;

    public static final int SERVER_CONNECT_LOGIN = 0x1388;
    public static final int SERVER_SEND_FIRST_PAGE = 0x1389;
    public static final int SERVER_SEND_PLAYER_FULL_INFO = 0x138A;
    public static final int SERVER_PAGE_NOT_FOUND = 0x138C;

    /** C# {@code onAcceptCompleted} type byte after the session key. */
    public static final int RANK_SERVER_TYPE = 5;

    /** C# {@code ePLAYER_POSITION_RANK_TYPE}. */
    public static final int PPRT_IN_TOP_RANK = 0;
    public static final int PPRT_NOT_RANK = 1;
    public static final int PPRT_NOT_TOP_RANK = 2;

    /** C# {@code SEARCH_OPTION.SO_NICKNAME}. */
    public static final int SEARCH_BY_NICKNAME = 0;
    /** C# {@code SEARCH_OPTION.SO_POSITION}. */
    public static final int SEARCH_BY_POSITION = 1;

    private RankingPackets() {}

    public record RowSummary(int level, int term, int classType, String id, String nickname) {}

    public record SearchDados(int menu, int item, int term, int classType, int page) {}

    public record RegistryRowWithSummary(RegistryRow row, RowSummary summary) {}

    public static byte[] clientLogin(int uid, String id, int menu, int item, int term, int classType, int page) {
        return new PacketWriter()
                .opcode(CLIENT_CONNECT)
                .u32(uid)
                .pstr(id)
                .u8(menu)
                .u8(item)
                .u8(term)
                .u8(classType)
                .u32(page)
                .toBytes();
    }

    public static Login readLogin(PacketReader reader) {
        int uid = reader.u32();
        String id = reader.pstr();
        int menu = reader.remaining() >= 1 ? reader.u8() : 0;
        int item = reader.remaining() >= 1 ? reader.u8() : 0;
        int term = reader.remaining() >= 1 ? reader.u8() : 0;
        int classType = reader.remaining() >= 1 ? reader.u8() : 0;
        int page = reader.remaining() >= 4 ? reader.u32() : 0;
        return new Login(uid, id, menu, item, term, classType, page);
    }

    /**
     * C# {@code sendFirstPage} option 0 with an empty registry: four search bytes,
     * 10 zero bytes (page/pages/count), then {@code PPRT_NOT_TOP_RANK} because
     * {@code search_dados.active} is 0 for a fresh login.
     */
    public static byte[] firstPageOk(int menu, int item, int term, int classType) {
        return firstPage(menu, item, term, classType, List.of(), 0, 0);
    }

    public static byte[] firstPage(
            int menu, int item, int term, int classType, List<RegistryRowWithSummary> rows, int page, int pages) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_SEND_FIRST_PAGE)
                .u8(0)
                .u8(menu)
                .u8(item)
                .u8(term)
                .u8(classType);
        if (rows == null || rows.isEmpty()) {
            w.zero(10);
        } else {
            w.u32(page);
            w.u32(pages);
            w.u16(rows.size());
            for (RegistryRowWithSummary entry : rows) {
                RegistryRow row = entry.row();
                w.u32((int) row.uid());
                w.u32(row.currentPosition());
                w.u32(row.lastPosition());
                w.i32(row.value());
                writeRowSummary(w, entry.summary());
            }
        }
        w.u8(PPRT_NOT_TOP_RANK);
        return w.toBytes();
    }

    /**
     * C# {@code sendPageFoundPlayer}: {@code 0x138C} with the page containing the found player.
     */
    public static byte[] searchPageFound(
            int menu,
            int item,
            int term,
            int classType,
            List<RegistryRowWithSummary> rows,
            int page,
            int pages,
            int foundPositionInPage) {
        PacketWriter w = new PacketWriter()
                .opcode(SERVER_PAGE_NOT_FOUND)
                .u8(0)
                .u8(menu)
                .u8(item)
                .u8(term)
                .u8(classType)
                .u32(page)
                .u32(pages)
                .u16(rows.size());
        for (RegistryRowWithSummary entry : rows) {
            RegistryRow row = entry.row();
            w.u32((int) row.uid());
            w.u32(row.currentPosition());
            w.u32(row.lastPosition());
            w.i32(row.value());
            writeRowSummary(w, entry.summary());
        }
        w.u16(foundPositionInPage);
        return w.toBytes();
    }

    /** C# search catch / not found: {@code 0x138C} u8 1. */
    public static byte[] searchPageError() {
        return new PacketWriter().opcode(SERVER_PAGE_NOT_FOUND).u8(1).toBytes();
    }

    public static byte[] clientSearchByNickname(String nickname, SearchDados dados) {
        PacketWriter w = new PacketWriter()
                .opcode(CLIENT_REQ_SEARCH_PLAYER_IN_RANKING)
                .u8(SEARCH_BY_NICKNAME)
                .pstr(nickname);
        writeSearchDados(w, dados);
        return w.toBytes();
    }

    public static byte[] clientSearchByPosition(int position, SearchDados dados) {
        PacketWriter w = new PacketWriter()
                .opcode(CLIENT_REQ_SEARCH_PLAYER_IN_RANKING)
                .u8(SEARCH_BY_POSITION)
                .u32(position);
        writeSearchDados(w, dados);
        return w.toBytes();
    }

    public static SearchRequest readSearch(PacketReader reader) {
        int option = reader.u8();
        if (option == SEARCH_BY_NICKNAME) {
            String nickname = reader.pstr();
            SearchDados dados = readSearchDados(reader);
            return new SearchRequest(option, nickname, 0, dados);
        }
        if (option == SEARCH_BY_POSITION) {
            int position = reader.u32();
            SearchDados dados = readSearchDados(reader);
            return new SearchRequest(option, "", position, dados);
        }
        throw new IllegalArgumentException("invalid ranking search option " + option);
    }

    public static SearchDados readSearchDados(PacketReader reader) {
        int menu = reader.remaining() >= 1 ? reader.u8() : 0;
        int item = reader.remaining() >= 1 ? reader.u8() : 0;
        int term = reader.remaining() >= 1 ? reader.u8() : 0;
        int classType = reader.remaining() >= 1 ? reader.u8() : 0;
        int page = reader.remaining() >= 4 ? reader.u32() : 0;
        return new SearchDados(menu, item, term, classType, page);
    }

    private static void writeSearchDados(PacketWriter w, SearchDados dados) {
        w.u8(dados.menu())
                .u8(dados.item())
                .u8(dados.term())
                .u8(dados.classType())
                .u32(dados.page());
    }

    private static void writeRowSummary(PacketWriter w, RowSummary summary) {
        if (summary == null) {
            w.zero(7);
            return;
        }
        w.u8(summary.level())
                .u8(summary.term())
                .u8(summary.classType())
                .pstr(summary.id() == null ? "" : summary.id())
                .pstr(summary.nickname() == null ? "" : summary.nickname());
    }

    /**
     * C# {@code sendPlayerFullInfo}. Without a rank-character snapshot Java still answers from
     * {@code account}/{@code user_info} and writes a zero {@code CharacterInfo}.
     */
    public static byte[] playerFullInfo(PlayerInfo info, byte[] character, List<RegistryRow> overall) {
        PacketWriter w = new PacketWriter().opcode(SERVER_SEND_PLAYER_FULL_INFO).u8(0);
        w.u32((int) info.uid());
        w.fixedStr(info.id(), 22);
        w.fixedStr(info.nickname(), 22);
        w.u16(info.level());
        if (character != null && character.length > 0) {
            w.bytes(character);
        } else {
            w.zero(513);
        }
        if (overall == null || overall.isEmpty()) {
            w.u8(1);
        } else {
            w.u8(0);
            for (RegistryRow row : overall) {
                if (row.currentPosition() > 10000) {
                    w.zero(8);
                } else {
                    w.u32(row.currentPosition());
                    w.u32(row.lastPosition());
                }
                w.i32(row.value());
            }
        }
        return w.toBytes();
    }

    public static byte[] playerFullInfoError() {
        return new PacketWriter().opcode(SERVER_SEND_PLAYER_FULL_INFO).u8(1).toBytes();
    }

    public static PlayerInfoRequest readPlayerInfo(PacketReader reader) {
        int uid = reader.u32();
        String id = reader.remaining() >= 2 ? reader.pstr() : "";
        int active = reader.remaining() >= 1 ? reader.u8() : 0;
        return new PlayerInfoRequest(uid, id, active);
    }

    public record RegistryRow(long uid, int currentPosition, int lastPosition, int value) {}

    public record PlayerInfo(long uid, String id, String nickname, int level) {}

    public record PlayerInfoRequest(int uid, String id, int active) {}

    /** C# {@code sendFirstPage} option != 0: option byte + 14 zeros. */
    public static byte[] firstPageError(int option) {
        return new PacketWriter()
                .opcode(SERVER_SEND_FIRST_PAGE)
                .u8(option)
                .zero(14)
                .toBytes();
    }

    public record Login(int uid, String id, int menu, int item, int term, int classType, int page) {}

    public record SearchRequest(int option, String nickname, int position, SearchDados dados) {}
}
