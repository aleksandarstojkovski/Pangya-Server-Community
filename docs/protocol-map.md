# Protocol map (C# type → Java class → opcode)

Season 9 / C# `Develop` GB only. Fill as packets are ported. Do not invent opcodes.

Framing reference: `PangyaAPI.Network.PangyaPacket.PacketBuffer`.
Cipher: `PangyaAPI.Network.Cryptor.Cipher`.

## Auth (server-to-server)

C# handlers are inline in `AuthServer/AuthServerTcp/AuthServer.cs` and `unit_auth_server_connect.cs` (no enum file).

| Dir | C# | Opcode | Java |
|-----|----|--------|------|
| Auth→child raw | first key packet | `0x00` | `AuthS2s.FIRST_KEY` |
| Child→Auth | register server | `0x01` | `AuthS2s.REGISTER` |
| Auth→child | oid assigned | `0x01` | `AuthS2s.REGISTER_ACK` |
| Child→Auth | disconnect player | `0x02` | `AuthS2s.DISCONNECT_PLAYER` |
| | confirm disconnect | `0x03` | |
| | info player | `0x04` | |
| | confirm info | `0x05` | |
| | command to other server | `0x06` | |
| | reply to other server | `0x07` | |

## Login

C#: `LoginServer/PangyaEnums/PacketLogin.cs` → Java `org.pangya.protocol.login.LoginPackets` (S2 done)

Framing + cipher Java: `org.pangya.protocol.crypto.Cipher`, `MiniLzo`, `CryptoOracle`; Netty `org.pangya.network.netty.PangyaNettyServer`.

| Dir | C# | Opcode |
|-----|----|--------|
| C | `CLIENT_CONNECT` | `0x01` |
| C | `CLIENT_SELECT_GS` | `0x03` |
| C | `CLIENT_USER_MSG` | `0x04` |
| C | `CLIENT_SET_NICK` | `0x06` |
| C | `CLIENT_CONFIRM_SET_NICK` | `0x07` |
| C | `CLIENT_SET_CHARACTER` | `0x08` |
| C | `CLIENT_RECONNECT` | `0x0B` |
| S | `SERVER_CONNECT` | `0x00` |
| S | `SERVER_LOGIN` | `0x01` |
| S | `SERVER_GS_LIST` | `0x02` |
| S | `SERVER_AUTH_KEY_GAME` | `0x03` |
| S | `SERVER_EVENT_PRIZE` | `0x05` |
| S | `SERVER_MACRO_GAME_OPTION` | `0x06` |
| S | `SERVER_MS_LIST` | `0x09` |
| S | `SERVER_AGREEMENT` | `0x0C` |
| S | `SERVER_CHECK_NICK` | `0x0E` |
| S | `SERVER_TUTORIAL` | `0x0F` |
| S | `SERVER_AUTH_KEY_LOGIN` | `0x10` |
| S | `SERVER_CHARACTER_SAVE` | `0x11` |

Login first raw frame (14 bytes, key at index 6): see `LoginServer.cs:159-161`.

CLIENT_CONNECT `0x01` body (`LoginData` in `pangya_login_st.cs`):
`PStr id`, `PStr password`, `byte opt_count`, `uint32 * (opt_count*8/4)`, `PStr mac`.

SERVER_LOGIN `0x01` success (`pacote001` option=0, **GB**):
`byte 0`, `PStr id`, `uint32 uid`, `uint32 cap`, `int32 level`, `int32 10`, `uint16 12`, `PStr nickname`.
Then `0x10` was already sent (auth key login), then `0x02` GS list, `0x09` messenger list, `0x06` macros (9×64-byte `WriteStr`).

GB `requestLogin` hashes MD5 then **overwrites with plaintext** `result.password` — Java stores/compares the client string as sent.

`ServerInfo.ToArray()` is 92 bytes: `WriteStr(nome,40)`, int32 uid/max/curr, `WriteStr(ip,18)`, int32 port, uint32 property, int32 angelic, uint16 event_flag, int16 event_map/app_rate/scratch_rate/img_no.

## Game (subset; full enum in `PacketGame.cs`)

C#: `GameServer/PangyaEnums/PacketGame.cs` → Java `org.pangya.protocol.game.GamePackets` (S3+)

| Dir | C# | Opcode | Notes |
|-----|----|--------|-------|
| S | `CLIENT_CONNECT_TO_SERVER` | `0x3F` | first raw packet |
| S | `SERVER_LOGIN_ACK` | `0x44` | |
| S | `SERVER_CHANNEL_LIST` | `0x4D` | |
| S | `SERVER_CHANNEL_ENTER_ACK` | `0x4E` | |
| S | `SERVER_ROOM_ENTER_RESULT` | `0x49` | |
| C | `CLIENT_REQUEST_LOGIN` | `0x02` | |
| C | `CLIENT_ENTER_CHANNEL` | `0x04` | |
| C | `CLIENT_REQUEST_CREATE_ROOM` | `0x08` | Practice is a room type, not a dedicated opcode |
| C | `CLIENT_REQUEST_JOIN_ROOM` | `0x09` | |
| C | `CLIENT_LEAVE_PRACTICE` | `0x130` | |
| C | `CLIENT_LEAVE_CHIP_IN_PRACTICE` | `0x131` | |

Room types (`RoomInfo.TIPO` in `pangya_game_st.cs`): STROKE=0, MATCH=1, LOUNGE=2, TOURNEY=4, TOURNEY_TEAM=5, GUILD_BATTLE=6, PANG_BATTLE=7, APPROCH=10, GRAND_ZODIAC_INT=11, GRAND_ZODIAC_ADV=13, GRAND_ZODIAC_PRACTICE=14, SPECIAL_SHUFFLE_COURSE=18, **PRACTICE=19**, GRAND_PRIX=20.

Game login CLIENT `0x02` (`GameServer.ReadLoginPacket`): `PStr id`, `uint32 uid`, `uint32 ntreevUID`, `uint16 command`, `PStr authKeyLogin`, `PStr clientVersion`, `uint32 packetVersion` (XOR-encrypted with GUID `{782AE110-2EEF-4c61-B030-A53F17634F7D}`), `uint32 isPcBang`, `PStr authKeyGame`.

Fail `SendLoginAck` writes **uint32** ack. Success `pacote044` option 0 + `principal()` (12512 bytes after opcode+option). Then warehouse `0x73` (196-byte items from `pangya_item_warehouse`), characters `0x70` (513-byte `CharacterInfo` from `pangya_character_information`), caddies `0x71`, equip `0x72` (116 bytes from `pangya_user_equip`), mascots `0xE1`, `0x4D` channel list, then `sendCompleteData` tail (`0x102`, `0x131` Treasure Hunter 21 maps, live `0x21D`/`0x21E` from `pangya_counter_item`/`pangya_achievement`/`pangya_quest`, `0x144`…`0x1B1`). Seeded accounts have empty achievement rows so those packets are still three uint32 zeros.

`ChannelInfo.ToArray()` is 77 bytes: `WriteStr(name,64)`, int16 max_user, int16 curr_user, byte id, uint32 flag, uint32 flag2. Channel ids are 0-based from YAML order (C# INI `CHANNEL1` → id 0).

`pacote04E`: byte option (1=ok, 2=full, 3=not found). Enter channel: CLIENT `0x04` + byte channel id.

Practice create CLIENT `0x08` with `tipo==19`. C# enter order: `pacote04A` (int16 -1 + `RoomInfoEx.ToArrayEx` lobby summary) then `pacote049` (int16 0 + `RoomInfoEx.ToArray()` 210 bytes) then `pacote048` list + `pacote048` self. `PlayerRoomInfo.ToArray` is **341** bytes (C# `SIZE_STRUCT` comment 348 is padded); Ex is 854. Practice uses compact `0x100` (wire option byte 0); Stroke/Match/Lounge/Pang Battle send Ex. Leave CLIENT `0x130`. Exit room CLIENT `0x0F`. Start-game CLIENT `0x0E` → empty `0x230` + empty `0x231` + `0x77` uint32 pang rate. Solo start is allowed only for Practice/GP/Grand Zodiac; Versus with one player returns `0x253` uint32 `0x5900202`. Practice/Tourney then send `0x76` (tipo_show + uint32 1 + SYSTEMTIME) and `0x52` course (18 synthetic holes, cube count 0; pin coords come from CLIENT `0x1A` because IFF files are not in the env). Versus with ≥2 players sends `0x76` player dump (MemberInfoEx + UserInfo + trophy + UserEquip + zero map stats + Character/Caddie/ClubSet/Mascot from SQL) then per-player `0x52` + `0x16A` mascot seed. CLIENT `0x1A` → `0x9E` weather + `0x5B` wind + `0x8D` remain-ms. CLIENT `0x1B` XOR-decrypts 54-byte `ShotSyncData` with the 16-byte room key and broadcasts `0x6E`. CLIENT `0x1C` → `0xCC` empty drop. Equip CLIENT `0x20` type 0 writes `CharacterInfo` parts to `pangya_character_information` (IFF part validation skipped); types 1/3/5/8 update `pangya_user_equip`; ack `0x6B` err 4. Buy CLIENT `0x1D` without IFF catalog → `0x68` uint32 10.

## Ranking

C#: `RankingServer/PangyaEnums/PacketRanking.cs`

Hello is `makeRaw` `0x1388` + int32 key + byte 5 + PStr(`1970-01-01 00:00:00.000`) (C# `formatDateLocal(0)` as UTC).

CLIENT `0x00`: uint32 uid, PStr id, byte menu, byte item, byte term_s5, byte class, uint32 page.
Success `0x1389`: byte 0, four search bytes, then either 10 zero bytes (empty registry) or page/pages/count + rows (`uid, pos, last, value` + 7 zero character bytes). Trailing byte `PPRT_NOT_TOP_RANK` (2) on fresh login (`search_dados.active==0`). Error: byte option + 14 zeros.
CLIENT `0x01` player info → `0x138A` byte 0 + uid + id/nick 22 + level u16 + 513-byte CharacterInfo from `pangya_rank_atual_character` or live `pangya_character_information` + overall flag. Registry SQL is `pangya.pangya_rank_atual` left-joined with `pangya_rank_antes` (C# `ProcGetRankRegistryInfo`).

| Dir | C# | Opcode |
|-----|----|--------|
| C | `CLIENT_CONNECT` | `0x00` |
| C | `CLIENT_REQUEST_PLAYER_INFO` | `0x01` |
| C | `CLIENT_REQ_SEARCH_PLAYER_IN_RANKING` | `0x02` |
| S | `SERVER_CONNECT_LOGIN` | `0x1388` |
| S | `SERVER_SEND_FIRST_PAGE` | `0x1389` |
| S | `SERVER_SEND_PLAYER_FULL_INFO` | `0x138A` |

## Messenger

C#: `MessengerServer/PangyaEnums/Definition.cs`

Hello is `makeRaw` `0x2E` + byte 1 + byte 1 + uint32 key.

CLIENT `0x12`: uint32 uid, PStr nickname. Success `0x2F` byte 0 + uint32 uid. Fail byte 1.
Friend list CLIENT `0x14` → `0x30` sub `0x115` + uid + state 4 + OK + 75-byte empty `ChannelPlayerInfo`.
Add friend CLIENT `0x18` uid+PStr nick → `0x30` sub `0x104` + OK + 65-byte `FriendInfo` + offline ChannelPlayerInfo. Agree `0x19` → sub `0x109`. Block `0x1A` → sub `0x10C`. Remove `0x1C` → sub `0x10B`. Rows in `pangya.pangya_friend_list` (C# `ProcAddFriend` / `ProcUpdateFriendInfo` / DELETE).

| Dir | C# | Opcode |
|-----|----|--------|
| C | `CLIENT_CONNECT_0x12` | `0x12` |
| S | `SERVER_CONNECT_0x2E` | `0x2E` |
| S | `SERVER_LOGIN_ACK_0x2F` | `0x2F` |
| S | `SERVER_FRIEND_AND_GUILD_LIST_0x30` | `0x30` |
| S | friend/guild chat family | `0x100+` | see Definition.cs |

## Golden fixtures

| Fixture | Source | Status |
|---------|--------|--------|
| PUBLIC/PRIVATE_KEY_TABLE | `CryptoOracle.cs` | extracted `core-protocol/.../crypto/*.bin` |
| ServerEncrypt roundtrip | MiniLzo.cs + Cipher.cs | S1 |
| Login 14-byte hello | `LoginServer.cs` | S1/S2 |
| Client captures | real client | **gap**: no official client in env |
