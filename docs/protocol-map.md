# Protocol map (C# type → Java class → opcode)

Season 9 / C# `Develop` **JP**. Fill as packets are ported. Do not invent opcodes.

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

Login first raw frame (JP `makeRaw` opcode 0 + int32 key + int32 server uid): see `LoginServer.cs:155-166`.

CLIENT_CONNECT `0x01` body (`LoginData` in `pangya_login_st.cs`):
`PStr id`, `PStr password`, `byte opt_count`, `uint32 * (opt_count*8/4)`, `PStr mac`.

SERVER_LOGIN `0x01` success (`pacote001` option=0, **JP**):
`byte 0`, `PStr id`, `uint32 uid`, `uint32 cap`, `byte 1`, `int32 0`, `byte 1`, `int32 5`, `SYSTEMTIME`, `PStr acess_code` (web key), `uint64 0`, `PStr nickname`.
Then `0x10` was already sent (auth key login), then `0x02` GS list, `0x09` messenger list, `0x06` macros (9×64-byte `WriteStr`).

JP `pacote00F` adds uint32 0, uint32 5, `formatDateLocal(0)`, acess_code.

First-set (after option `0xD8`):
- CLIENT `0x07` PStr nick → `pacote00E` int32 `NICK_CHECK` (0 success + PStr nick; 2 in use; 3 invalid; 5 GM/ADM unless cap≥4; 8 space; 9 same as ID; 12 + uint32 error).
- CLIENT `0x06` PStr nick → `UPDATE account.NICK`, `FIRST_LOGIN=1`; if `FIRST_SET=0` send option `0xD9`, else success login.
- CLIENT `0x08` uint32 typeid, u8 hair, u8 shirts. IFF `findCharacter` is skipped (no IFF files); Java accepts IFF group CHARACTER (`typeid >>> 26 == 1`), hair ≤ 9, shirts == 0. Insert `pangya_character_information`, `ProcFirstSet` essentials (Air Knight + default ball + pang/cookie), `pacote011` uint16 0, then success login. Fail: `pacote011` then `pacote00E` option 12 + 500051.

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
| C | `CLIENT_CHATMSG` | `0x03` | lobby → `0x40`; in-room → room `0x40` |
| C | `CLIENT_CHANGE_ROOM_INFO` | `0x0A` | master; success `0x4A` + lobby `0x47` opt 3 |
| C | `CLIENT_SET_PLAYER_READY` | `0x0D` | invert bit 9; broadcast `0x78` oid + client byte |
| C | `CLIENT_TIMETOALIVE` | `0x01` | no reply |
| C | `CLIENT_ENTER_LOBBY` | `0x81` | `0x46` 4/5 + `0x47` + `0x46` join + empty `0xF5` |
| C | `CLIENT_LEAVE_LOBBY` | `0x82` | `0x46` leave + empty `0xF6` |
| S | `SERVER_CHAT` | `0x40` | option + PStr nick + PStr msg |
| S | `SERVER_USERLIST` | `0x46` | option + count + 200-byte `PlayerLobbyInfo` |
| S | `SERVER_ROOMLIST` | `0x47` | count + option + i16 -1 + 210-byte rooms (no Practice) |
| S | `SERVER_READY` | `0x78` | i32 oid + u8 ready |
| S | `SERVER_ENTER_LOBBY` | `0xF5` | empty |
| C | `CLIENT_WHISPER` | `0x2A` | PStr nick+msg; `0x84` FROM/TO or `0x40` option 6 offline |
| C | `CLIENT_REQUEST_DETAIL_ROOM_INFO` | `0x2D` | u16 room; `0x86` summary + seated players |
| C | `CLIENT_REQUEST_CASH` | `0x3D` | `0x96` u64 cookie from `user_info` |
| C | `CLIENT_REQUEST_USERINFO` | `0x2F` | u32 uid + u8 season; dump then `0x89` err 1 |
| C | `CLIENT_UPDATE_MACRO` | `0x69` | 9×64 Shift_JIS; no reply; `pangya_user_macro` |
| C | `CLIENT_REQUEST_SERVER_LIST` | `0x43` | `0x9F` GS `ServerInfo` 92 + nested channel count (no `0x4D`) |
| C | `CLIENT_REQUEST_RANKADDRESS` | `0x47` | `0xA2` PStr ip + i32 port from RANK list; silent if empty |
| C | `CLIENT_REQUEST_CHANGE_TEAM` | `0x10` | u8 team; broadcast `0x7D` oid + team |
| C | `CLIENT_REQUEST_USERINFO_OFFLINE` | `0x07` | u8 opt + PStr nick; `0xA1` err 0 + uid + MemberInfoEx 299, else err 2 |
| C | `CLIENT_REQUEST_BANISH` | `0x26` | u32 uid; master kicks via leave `0x4C` -1 |
| C | `CLIENT_REQUEST_SERVER_TIME` | `0x5C` | empty; `0xBA` SYSTEMTIME 16 bytes |
| S | `SERVER_WHISPER` | `0x84` | byte 0 sender ack / byte 1 deliver |
| S | `SERVER_DETAIL_ROOM_INFO` | `0x86` | num_player u32 + holes + time + course + tipo + modo + trophy + rows |
| S | `SERVER_PLAYER_INFO` | `0x89` | u32 err (+ season/uid when err>0); GM deny=3 |
| S | `SERVER_CHANGE_TEAM` | `0x7D` | i32 oid + u8 team |
| S | `SERVER_DECISION_ROOM_MASTER` | `0x7C` | i32 oid + i16 0 when master leaves and others remain |
| S | `SERVER_RESPONSE_USERINFO_OFFLINE` | `0xA1` | u8 0 + uid + MemberInfoEx, or u8 2 |
| S | `SERVER_RESPONSE_SERVER_TIME` | `0xBA` | SYSTEMTIME (8×u16) |
| S | `SERVER_SERVER_LIST` | `0x9F` | u8 GS count + 92-byte rows + channel list body |
| S | `SERVER_RESPONSE_RANKADDRESS` | `0xA2` | PStr ip + i32 port |
| C | `CLIENT_EXIT_ROOM` | `0x0F` | remaining `0x4A`+`0x48` opt 2 (oid) + leaver `0x4C` -1 |
| C | `CLIENT_INVITE` | `0xBA` | C# `packet0BA` / `requestInvite`; enum name `CLIENT_CHECK_INVITE` is 0xBA |
| C | `CLIENT_CHECK_INVITE` | `0x29` | C# `packet029` / `requestCheckInvite`; no reply |
| S | `SERVER_EXIT_ROOM_ACK` | `0x4C` | i16 -1 |
| S | `SERVER_INVITE` | `0x83` | delivered to invitee |
| S | `SERVER_INVITE_REPLY` | `0x12F` | u16 0 success or u16 23 fail |
| C | `CLIENT_REQUEST_JOIN_ROOM` | `0x09` | |
| C | `CLIENT_LEAVE_PRACTICE` | `0x130` | |
| C | `CLIENT_LEAVE_CHIP_IN_PRACTICE` | `0x131` | |
| C | `CLIENT_DELETE_ITEM` | `0x64` | no IFF → `0xC5` sbyte -1 |
| C | `CLIENT_CADDIE_HOLIDAY_NOTICE` | `0x6B` | invalid/IFF miss silent |
| C | `CLIENT_ENTER_OTHER_CHANNEL` | `0x83` | same numeric as SERVER_INVITE; fail `0x4E` 3 then disconnect |
| C | `CLIENT_GAMEGUARD` | `0x88` | no reply |
| C | `CLIENT_INVITE_RELOGIN` | `0xB4` | log only |
| C | `CLIENT_WIND_NEXT_HOLE` | `0x141` | not-in-room silent; GameBase no-op |
| C | `CLIENT_DAILY_QUEST` | `0x151` | `0x216` unix+0 then `0x225` option 0 + dates + 3 typeids |
| C | `CLIENT_ACCEPT_DAILY_QUEST` | `0x152` | empty/zero → `0x226` option 1 + count 0 |
| C | `CLIENT_REWARD_DAILY_QUEST` | `0x153` | empty/zero → `0x227` option 500050 + count 0 |
| C | `CLIENT_LEAVE_DAILY_QUEST` | `0x154` | empty/zero → `0x228` option 1 only |
| C | `CLIENT_ACHIEVEMENT` | `0x157` | empty map no packet; short body `0x22C` i32 1 |
| C | `CLIENT_LOLO` | `0x155` | u64 pang + 3×typeid; no IFF card → `0x22A` sys `0x151` |
| C | `CLIENT_CADIE` | `0x158` | count 0/`>4` → `0x22F` sys `5200451&0xFFFF`; IFF miss `5200452&0xFFFF` |
| C | `CLIENT_REQUEST_MESSENGER_SERVER_LIST` | `0x8B` | `0xFC` u8 count + 92-byte `ServerInfo` (type 3); empty still sends 0 |
| C | `CLIENT_REQUEST_REFRESH_GACHA_TICKETS` | `0x9E` | `0x102` i32×2 tickets + pang + cookie; catch `0x44` u8 `0xE2` |
| C | `CLIENT_ENCHANT` | `0x4B` | missing warehouse/IFF → `0xA5` u8 0 |
| C | `CLIENT_INTRUSION` | `0x9D` | missing room → `0x113` u8 6 + u8 1 |
| C | `CLIENT_REQ_NEW_BONGDARISHOP_PLAY_NORMAL` | `0x14B` | empty balls → `0x21B` sys `0x5900103&0xFFFF` |
| C | `CLIENT_UPDATE_INGAME_WEBPAGE` | `0xA1` | sbyte `place`; no reply |
| C | `CLIENT_REQUEST_PANG_INFO` | `0xA2` | `0xC8` only if pang changed |
| C | `CLIENT_JOIN_GALLERY` | `0x3E` | spy enter; fail silent |
| C | `CLIENT_GM_COMMAND` | `0x8F` | non-GM silent |
| C | `CLIENT_ACTIVE_AUTO_COMMAND` | `0x156` | not-in-room silent |
| C | `CLIENT_REQUEST_KICK` | `0x61` | log only |
| S | `SERVER_MESSENGER_LIST` | `0xFC` | u8 count + 92-byte rows |
| S | `SERVER_GACHA_COUPON` | `0x102` | i32 normal + i32 partial + u64 pang + u64 cookie |
| S | `SERVER_CLUB_STATS` | `0xA5` | fail u8 0 |
| S | `SERVER_INTRUSION` | `0x113` | fail u8 6 + u8 sys |
| S | `SERVER_PAPEL_PLAY` | `0x21B` | u32 error |
| S | `SERVER_DELETE_ITEM` | `0xC5` | fail sbyte -1 |
| S | `SERVER_DAILY_QUEST_STAMP` | `0x216` | unix + count |
| S | `SERVER_DAILY_QUEST_INFO` | `0x225` | option + current/accept unix + count + 3×typeid + deletes |
| S | `SERVER_DAILY_QUEST_ACCEPT` | `0x226` | |
| S | `SERVER_DAILY_QUEST_REWARD` | `0x227` | |
| S | `SERVER_DAILY_QUEST_LEAVE` | `0x228` | |
| S | `SERVER_ACHIEVEMENT_GUI` | `0x22C` | i32 option |
| S | `SERVER_LOLO` | `0x22A` | u32 error |
| S | `SERVER_CADIE` | `0x22F` | u32 error |

Room types (`RoomInfo.TIPO` in `pangya_game_st.cs`): STROKE=0, MATCH=1, LOUNGE=2, TOURNEY=4, TOURNEY_TEAM=5, GUILD_BATTLE=6, PANG_BATTLE=7, APPROCH=10, GRAND_ZODIAC_INT=11, GRAND_ZODIAC_ADV=13, GRAND_ZODIAC_PRACTICE=14, SPECIAL_SHUFFLE_COURSE=18, **PRACTICE=19**, GRAND_PRIX=20.

Game login CLIENT `0x02` (`GameServer.ReadLoginPacket`): `PStr id`, `uint32 uid`, `uint32 ntreevUID`, `uint16 command`, `PStr authKeyLogin`, `PStr clientVersion`, `uint32 packetVersion` (XOR-encrypted with GUID `{782AE110-2EEF-4c61-B030-A53F17634F7D}`), `uint32 isPcBang`, `PStr authKeyGame`.

Fail `SendLoginAck` writes **uint32** ack. Success `pacote044` option 0 + JP `principal()` (PStr clientVersion only, MemberInfoEx **299**, UserInfo **265**, no 277-byte pad). Then **JP `sendCompleteData` order**: characters `0x70` (513-byte `CharacterInfo` from `pangya_character_information`), caddies `0x71`, warehouse `0x73` (196-byte items from `pangya_item_warehouse`), mascots `0xE1`, equip `0x72` (116 bytes from `pangya_user_equip`), `0x4D` channel list, then tail (`0x102`, `0x131` Treasure Hunter 21 maps, live `0x21D`/`0x21E` from `pangya_counter_item`/`pangya_achievement`/`pangya_quest`, **`0xF1` option 0**, empty **`0x135`**, `0x144`… two `0x25D`; JP does **not** send GB `0x1B1`). Seeded accounts have empty achievement rows so those packets are still three uint32 zeros.

`ChannelInfo.ToArray()` is 77 bytes: `WriteStr(name,64)`, int16 max_user, int16 curr_user, byte id, uint32 flag, uint32 flag2. Channel ids are 0-based from YAML order (C# INI `CHANNEL1` → id 0).

`pacote04E`: byte option (1=ok, 2=full, 3=not found). Enter channel: CLIENT `0x04` + byte channel id.

Practice create CLIENT `0x08` with `tipo==19`. C# enter order: `pacote04A` (int16 -1 + `RoomInfoEx.ToArrayEx` lobby summary) then `pacote049` (int16 0 + `RoomInfoEx.ToArray()` 210 bytes) then `pacote048` list + `pacote048` self. JP `PlayerRoomInfo.ToArray` is **348** bytes; Ex is **861**. Practice uses compact `0x100` (wire option byte 0); Stroke/Match/Lounge/Pang Battle send Ex. Leave CLIENT `0x130`. Exit room CLIENT `0x0F`. Start-game CLIENT `0x0E` → empty `0x230` + empty `0x231` + `0x77` uint32 pang rate. Solo start is allowed only for Practice/GP/Grand Zodiac; Versus with one player returns `0x253` uint32 `0x5900202`. Practice/Tourney then send `0x76` (tipo_show + uint32 1 + SYSTEMTIME) and `0x52` course (18 synthetic holes, cube count 0; pin coords come from CLIENT `0x1A` because IFF files are not in the env). Versus with ≥2 players sends `0x76` player dump (MemberInfoEx + UserInfo + trophy + UserEquip + zero map stats + Character/Caddie/ClubSet/Mascot from SQL) then per-player `0x52` + `0x16A` mascot seed. SSC 2p uses Tourney `0x76`+`0x52` (`tipo_show` 4). GZ INT solo uses `0x76` tipo_show 11. CLIENT `0x1A` → `0x9E` weather + `0x5B` wind + `0x8D` remain-ms. CLIENT `0x1B` XOR-decrypts 54-byte `ShotSyncData` with the 16-byte room key and broadcasts `0x6E`. CLIENT `0x1C` → `0xCC` empty drop. Equip CLIENT `0x20` type 0 writes `CharacterInfo` parts to `pangya_character_information` (IFF part validation skipped); types 1/3/5/8 update `pangya_user_equip`; ack `0x6B` err 4. Lounge CLIENT `0xEB` in a tipo-2 room broadcasts `0x196` oid + 4×float `StateCharacterLounge` defaults (1,1,1,1). Lobby CLIENT `0x81` (after channel): `pacote046` option 4 (clear, one `PlayerLobbyInfo` 200 bytes) then option 5 (list), `pacote047` option 0 (rooms, Practice/GZ Practice omitted), channel `pacote046` option 1 (join), empty `0xF5`. Leave CLIENT `0x82` → option 2 + empty `0xF6`. Chat CLIENT `0x03` PStr nick+msg → `0x40`. Whisper CLIENT `0x2A` → `0x84` (byte 0 FROM / byte 1 TO) or `0x40` option 6 if offline. Cookie CLIENT `0x3D` → `0x96` u64. Ready CLIENT `0x0D` u8 → invert `PlayerRoomInfo` bit 9, broadcast `0x78` oid + the client byte. Change-room CLIENT `0x0A` (master) → `0x4A` + lobby `0x47` option 3. Keepalive CLIENT `0x01` is a no-op. Buy CLIENT `0x1D`: option, u16 count, `BuyItem`×count (37 bytes), coupon id. Success: optional `0xC8` pang spent, `pacote0AA` (`0xAA` count + typeid/id/time/flag/qntd_dep/SYSTEMTIME/ucc.IDX + pang + cookie) then `0x68` uint32 0 + pang + cookie. Fail `0x68` uint32: 2 price, 4 owned, 6 not buyable, 7 funds, 9 empty, 10 parse/catch. Catalog is SQL `pangya.shop_catalog` (IFF `IsBuyItem` stand-in; seed typeid `0x1A000006` pang 100). Player info CLIENT `0x2F` uid+season → 12 dump packets (`0x157` MemberInfoEx 299, `0x15E` CharacterInfo 513, `0x156` UserEquip 116, `0x158` UserInfo 265, `0x15D` GuildInfo 77 zeros, three empty `0x15C` map stats, `0x15B` 60×i32, empty `0x15A`/`0x159` trophy, `0x257`) then `0x89` err 1. Missing uid → `0x89` err 1 season 0 uid 0. GM deny err 3 (capability bit 4). Macros CLIENT `0x69` 9×64 no reply. GS list CLIENT `0x43` → `0x9F`. Rank CLIENT `0x47` → `0xA2` if a type-4 server is online. Team CLIENT `0x10` → `0x7D`. Room detail CLIENT `0x2D` → `0x86`. Invite CLIENT `0xBA` (C# `packet0BA`) → `0x12F` to sender + `0x83` to target; fail `0x12F` u16 23. Leave CLIENT `0x0F` → remaining `0x4A`+`0x48` option 2 (oid only) + leaver `0x4C` -1. Master leave with remaining players (not in-game) elects a new master (`0x7C` oid + i16 0) instead of destroying the room. Kick CLIENT `0x26` (master) force-leaves the target with `0x4C` -1. Nick lookup CLIENT `0x07` → `0xA1` (found: u8 0 + uid + MemberInfoEx 299; missing: u8 2). In-game Tourney (Practice/GP/GZ/SSC): CLIENT `0x13`/`0x15`/`0x16`/`0x18`/`0x19` reply only to the actor (`0x56`/`0x58`/`0x59`/`0x5D`/`0x60`). Versus (Stroke/Match) broadcasts the same. CLIENT `0x14` click and `0x22` turn-time have no success reply. CLIENT `0x48` load percent broadcasts `0xA3` in Versus only. CLIENT `0x55` u8 0/1 stores whisper; incoming PM to whisper=0 is `0x40` option 6. CLIENT `0x54` team chat (Match/Guild) → `0xB0` PStr nick+msg to the same team bit. Active item CLIENT `0x17` stays no-op without IFF. Daily quest CLIENT `0x151` (channel required) always sends `0x216` unix+0 then `pacote225` (option 0, `current_date` persisted on the session, accept 0, count 0, 3× typeid 0, delete 0) because the server quest table is unseeded and IFF `findQuestItem` is absent. Accept/leave/reward with `num_quest<=0` match C# catch: `0x226` option 1 + count 0, `0x228` option 1 only, `0x227` option 500050 + count 0. Delete item CLIENT `0x64` → `0xC5` sbyte -1 without IFF. Other-channel CLIENT `0x83` reuses `enterChannel` (`0x4E`); missing channel disconnects. Already-in-lobby skips the lobby dump (C# `enterLobby` throws). Not-in-lobby dumps users/rooms without `0xF5`. GameGuard `0x88`, wind-next-hole `0x141`, caddie-holiday notice `0x6B` (IFF miss), and invite-relog `0xB4` have no success reply. Achievement GUI CLIENT `0x157` with empty `map_ai` sends nothing; truncated uid → `0x22C` i32 1. Cadie CLIENT `0x158` count 0 → `0x22F` `shopSys(5200451)`; IFF miss → `shopSys(5200452)`. Lolo CLIENT `0x155` zero typeids → `0x22A` `shopSys(0x5400151)`. Messenger list CLIENT `0x8B` (no channel) → `0xFC` u8 count + 92-byte `ServerInfo` from type-3 `pangya_server_list` (empty still sends 0). Gacha CLIENT `0x9E` (channel) queries warehouse `c0` for typeids `0x1A000080`/`0x1A000083` then `pacote102`; seed has none so zeros + pang 100000 + cookie 0; SQL fail is `0x44` u8 `0xE2` + `0x5300600`. Club-set stats CLIENT `0x4B` opt 1/3 missing warehouse → `0xA5` u8 0. Intrusion CLIENT `0x9D` option 0/1 missing room → `0x113` u8 6 + u8 1. Papel play CLIENT `0x14B` with unseeded balls → `0x21B` `shopSys(0x5900103)`. Web-link CLIENT `0xA1` stores sbyte `place` with no reply. Web-guild CLIENT `0xA2`, spy `0x3E`, GM `0x8F` (non-GM), auto-command `0x156` (not in room), and kick-log `0x61` have no success reply.

## Ranking

C#: `RankingServer/PangyaEnums/PacketRanking.cs`

Hello is `makeRaw` `0x1388` + int32 key + byte 5 + PStr(`1970-01-01 00:00:00`) (JP `formatDateLocal(0)`).

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
Friend list CLIENT `0x14` → `0x30` sub `0x115` + uid + state 4 + OK + 75-byte empty `ChannelPlayerInfo`, then **always** `0x30` sub `0x102` + `ManyPacket.Pagina` (byte pagina, u16 total, u16 current). Empty list still sends pagina=1, total=0, current=0 (`FRIEND_PAG_LIMIT` 30). Friend rows: FriendInfo 65 + ChannelPlayerInfo 75 (live or offline -1s + icon 5) + cUnknown + level + state + flag.
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
