package org.pangya.db;

import org.pangya.protocol.game.GamePackets;

import java.util.List;

/** Game inventory replacing C# {@code CmdWarehouseItem} / {@code CmdCharacterInfo} / {@code CmdCaddieInfo}. */
public interface InventoryRepository {

    List<GamePackets.WarehouseItem> warehouse(long uid);

    List<GamePackets.CharacterInfo> characters(long uid);

    List<GamePackets.CaddieInfo> caddies(long uid);

    GamePackets.UserEquip userEquip(long uid);

    List<GamePackets.MascotInfo> mascots(long uid);

    List<GamePackets.CardInfo> cards(long uid);

    long pang(long uid);

    long cookie(long uid);
}
