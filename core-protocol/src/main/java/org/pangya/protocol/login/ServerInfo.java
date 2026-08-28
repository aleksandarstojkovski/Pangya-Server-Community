package org.pangya.protocol.login;

import org.pangya.protocol.packet.PacketReader;
import org.pangya.protocol.packet.PacketWriter;

/**
 * C# {@code ServerInfo.ToArray()} — 92 bytes.
 * {@code WriteStr(nome,40)}, int32 uid/max/curr, {@code WriteStr(ip,18)},
 * int32 port, uint32 property, int32 angelic, uint16 event_flag,
 * int16 event_map/app_rate/scratch_rate/img_no.
 */
public final class ServerInfo {

    public String name = "";
    public int uid;
    public int maxUser;
    public int currUser;
    public String ip = "";
    public int port;
    public int property;
    public int angelicWings;
    public int eventFlag;
    public short eventMap;
    public short appRate;
    public short scratchRate;
    public short imgNo;

    public byte[] toArray() {
        return new PacketWriter()
                .fixedStr(name, 40)
                .i32(uid)
                .i32(maxUser)
                .i32(currUser)
                .fixedStr(ip, 18)
                .i32(port)
                .u32(property)
                .i32(angelicWings)
                .u16(eventFlag)
                .i16(eventMap)
                .i16(appRate)
                .i16(scratchRate)
                .i16(imgNo)
                .toBytes();
    }

    /** Inverse of {@link #toArray()} for integration tests. */
    public static ServerInfo fromReader(PacketReader reader) {
        ServerInfo info = new ServerInfo();
        info.name = reader.fixedStr(40);
        info.uid = reader.i32();
        info.maxUser = reader.i32();
        info.currUser = reader.i32();
        info.ip = reader.fixedStr(18);
        info.port = reader.i32();
        info.property = reader.u32();
        info.angelicWings = reader.i32();
        info.eventFlag = reader.u16();
        info.eventMap = (short) reader.i16();
        info.appRate = (short) reader.i16();
        info.scratchRate = (short) reader.i16();
        info.imgNo = (short) reader.i16();
        return info;
    }
}
