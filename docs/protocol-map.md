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
| Child→Auth | confirm disconnect | `0x03` | `AuthS2s.confirmDisconnectResponse` → `AuthOutbound.sendConfirmDisconnectPlayer` |
| Child→Auth | info player request | `0x04` | `AuthS2s.requestInfoPlayerOnline` |
| Child→Auth | confirm info reply | `0x05` | `AuthS2s.infoPlayerOnlineResponse` |
| Auth→child | shutdown | `0x02` | `AuthS2s.AUTH_SHUTDOWN` → `GameHandler.authShutdown` |
| Auth→child | broadcast notice | `0x03` | `AuthS2s.AUTH_BROADCAST_NOTICE` → `GameHandler.authBroadcastNotice` (`0x42`) |
| Auth→child | broadcast ticker | `0x04` | `AuthS2s.AUTH_BROADCAST_TICKER` → `GameHandler.authBroadcastTicker` (`0xC9`) |
| Auth→child | cube win rare | `0x05` | `AuthS2s.AUTH_BROADCAST_CUBE_WIN_RARE` → `GameHandler.authBroadcastCubeWinRare` (`0x1D3`) |
| Auth→child | disconnect player | `0x06` | `AuthS2s.AUTH_DISCONNECT_PLAYER` → child handlers (login confirms only if local) |
| Auth→child | confirm disconnect ack | `0x07` | `AuthS2s.AUTH_CONFIRM_DISCONNECT` → `LoginHandler.authConfirmDisconnectPlayer` |
| Auth→child | new mail | `0x08` | `AuthS2s.AUTH_NEW_MAIL` → `GameHandler.authNewMailArrived` (`0x210`) |
| Auth→child | new rate | `0x09` | `AuthS2s.AUTH_NEW_RATE` → `GameHandler.authNewRate` |
| Auth→child | reload system | `0x0A` | `AuthS2s.AUTH_RELOAD_SYSTEM` → `GameHandler.authReloadGlobalSystem` |
| Auth→child | info player online | `0x0B` | `AuthS2s.AUTH_INFO_PLAYER_ONLINE` → Child→Auth `0x05` |
| Auth→child | confirm player info | `0x0C` | `AuthS2s.AUTH_CONFIRM_PLAYER_INFO` → messenger login / game resend |
| Auth→child | command to other server | `0x0D` | `AuthS2s.SEND_COMMAND_TO_OTHER` → `onAuthCommand` |
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
| C | `CLIENT_TIMECHECK` | `0x22` | Versus `startTime`; `timeVs>0` → `0x5C` oid when the turn timer fires |
| C | `CLIENT_CLICK` | `0x14` | Versus bar-space; `state==0 && tempo==1` → `0x5C` oid |
| C | `CLIENT_SHOT_ACK` | `0x1C` | cube/coin opt+count; empty IFF cubes → `0xCC` oid + u8 0; Versus `game_broadcast`; Tourney `session_send`; Versus duplicate `finish_shot2` silent |
| S | `SERVER_END_SHOT` | `0xCC` | i32 oid + u8 drop count; count&gt;0 then 128×16-byte `DropItem`; opposite CLIENT locker access |
| C | `CLIENT_USE_ITEM` | `0x17` | u32 typeid; fail silent; success broadcast `0x5A` typeid + i32 seed + oid; catalog + ITEM group stand-in; Versus bans Mulligan Rose `0x1800000E` |
| S | `SERVER_ACTIVE_ITEM` | `0x5A` | u32 typeid + i32 seed + i32 oid; opposite C# `CLIENT_OFFLINE_GAME` |
| C | `CLIENT_REPLAY_ONLINE` | `0x4A` | u32 warehouse typeid; fail silent; consume C0; success `0xA4` u16 remaining; Versus `game_broadcast`; Tourney `session_send` |
| S | `SERVER_REPLAY` | `0xA4` | u16 remaining C0 (`item.stat.qntd_dep`); opposite C# `CLIENT_REQUEST_PANGYA_QUIZ_LEVEL` |
| C | `CLIENT_ACTIVE_AUTO_COMMAND` | `0x156` | empty; not-in-game silent; success silent (passive count++); fail `0x22B` u32 92 (`STDA_ERROR_TYPE.GAME`) if C0&lt;1 else `0x550001` if spent |
| S | `SERVER_AUTO_COMMAND_ACK` | `0x22B` | u32 error; C# success does not send |
| C | `CLIENT_REEMPLOY_CADDIE` | `0x39` | SQL `iff_caddie`; success `0x93` u8 2 + id + pang; catch u8 1 |
| C | `CLIENT_CHANGE_MASCOT` | `0x73` | SQL `iff_mascot`; success `0xE2` u8 4 + id + PStr + pang; catch sbyte -1 |
| C | `CLIENT_UPDATE_PCBANG_MASCOTMSG` | `0x9A` | u8 mode + i32 id + PStr; `0xE2` u8 1 miss/IFF, u8 2 msg&gt;16; success local `u8 mode` (+ id/PStr/pang if 2 or 4). Opposite `SERVER_ADMIT_IDENTITY` |
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
| C | `CLIENT_DELETE_ITEM` | `0x64` | ITEM-group SQL consume; success `0xC5` u8 1 + typeid + qntd + id; else sbyte -1 |
| C | `CLIENT_CADDIE_HOLIDAY_NOTICE` | `0x6B` | invalid/IFF miss silent |
| C | `CLIENT_ENTER_OTHER_CHANNEL` | `0x83` | same numeric as SERVER_INVITE; fail `0x4E` 3 then disconnect |
| C | `CLIENT_GAMEGUARD` | `0x88` | no reply |
| C | `CLIENT_INVITE_RELOGIN` | `0xB4` | log only |
| C | `CLIENT_WIND_NEXT_HOLE` | `0x141` | not-in-room silent; GameBase no-op |
| C | `CLIENT_DAILY_QUEST` | `0x151` | `0x216` unix+0 then `0x225` option 0 + dates + 3 typeids |
| C | `CLIENT_ACCEPT_DAILY_QUEST` | `0x152` | i32 count+achievement ids. SQL quest-stuff mapping creates counter items, sets status 3/accept date, then `0x216` counters + `0x226` option 0 with full AchievementInfoEx rows. Empty/malformed → option 1 + count 0 |
| C | `CLIENT_REWARD_DAILY_QUEST` | `0x153` | SQL QuestItem rewards; removes achievement/quest/counters, adds warehouse rewards, then `0x216` rewards+removed counters + `0x227` option 0 + ids. Empty/malformed → option 500050 + count 0 |
| C | `CLIENT_LEAVE_DAILY_QUEST` | `0x154` | removes achievement/quest/counters, then `0x216` removed counters + `0x228` option 0 + ids. Empty/malformed → option 1 only |
| C | `CLIENT_ACHIEVEMENT` | `0x157` | u32 target uid; loads self/online/offline SQL achievements. Empty map no packet; success `0x22D` u32 0 + duplicated count + compact typeid/id/quest counter values, then `0x22C` i32 0; short/error `0x22C` i32 1 |
| C | `CLIENT_LOLO` | `0x155` | u64 pang + 3×typeid; no IFF card → `0x22A` sys `0x151`; truncated `<20` → full `0x5400150`; success `0xC8` remaining+spent + `0x216` awards + `0x229` tipo + `0x22A` u32 0 + typeid |
| C | `CLIENT_CADIE` | `0x158` | count 0/`>4` → `0x22F` sys `5200451&0xFFFF`; truncated/SQL miss `5200452&0xFFFF`; success `0x216` awards + `0x22F` u32 0 + seq + item |
| C | `CLIENT_REQUEST_MESSENGER_SERVER_LIST` | `0x8B` | `0xFC` u8 count + 92-byte `ServerInfo` (type 3); empty still sends 0 |
| C | `CLIENT_REQUEST_REFRESH_GACHA_TICKETS` | `0x9E` | `0x102` i32×2 tickets + pang + cookie; catch `0x44` u8 `0xE2` |
| C | `CLIENT_ENCHANT` | `0x4B` | opt 1/3 ClubSet: missing warehouse/IFF/enchant/slots/pang → `0xA5` u8 0; success u8 `opt/2+1` + u8 `opt%2` + u8 stat + i32 id + i64 pang. SQL `iff_clubset` SlotStats + `iff_enchant`. Catch always u8 0 |
| C | `CLIENT_INTRUSION` | `0x9D` | option 0 + u16 room: public in-progress non-full Tourney → `0x113` option 3 + room + elapsed ms + time30s + RoomInfo 210. Missing/wrong/private/not-game/full → u8 6 + u8 1 |
| C | `CLIENT_REQ_NEW_BONGDARISHOP_PLAY_NORMAL` | `0x14B` | SQL catalog: `0x216` awards + `0xFB` -1/-3 + `0x21B` u32 0 + coupon 0 + balls; funds `shopSys(0x5900102)` |
| C | `CLIENT_WEB_AUTH_KEY` | `0xFB` | no channel; `0x1AD` i32 1 + PStr 6-char key (empty key writes i16 0) |
| C | `CLIENT_REQ_CHANGE_GAME_SERVER` | `0x119` | u32 uid; unknown → `0x9F`; known GS → `0x1D4` i32 0 + PStr key |
| C | `CLIENT_OPEN_TIKI_REPORT` | `0xAB` | i32 warehouse id + i32 ticket id; valida C1*0x800\|C2 e SQL `ticket_report_catalog`, invalida scroll, poi `0x11A` u32 player-count 0 + SYSTEMTIME. Ogni fail i32 -1 + 16 zero |
| C | `CLIENT_REQ_POINT_SHOP_OPEN` | `0x126` | not blocked → `0x1E7` u32 0 |
| C | `CLIENT_ITEMSTORAGE_REQ_ACCESS` | `0xCC` | empty/`Sanitize` fail → `0x16C` u32 1; seed empty pass → `0x75` |
| C | `CLIENT_ITEMSTORAGE_REQ_STATE` | `0xD3` | no channel; `0x170` u32 0 + u32 2 (no pass) |
| C | `CLIENT_CLUBSETWORKSHOP_REQ_UP_LEVEL` | `0x164` | group ≠ ITEM/CARD → `0x23D` `shopSys(0x5300201)`; missing item `0x5300202`; C0&lt;qntd `0x5300203`; no `iff_item`/`iff_card` `0x5300204`; missing club `0x5300205`; no `iff_clubset` `0x5300206`; tipo -1 `0x5300207`; consume fail `0x5300208`; no limit/prob `0x5300209`; rank==-1 `0x5300210`; no limit row `0x5300211`; empty lottery → full `0x5300200`; truncated → full `0x5300200`. Success persist `C[stat]++` then `0x216` count 1 (type 2) then `0x23D` u32 0 + u32 stat. SQL `iff_item` + `iff_clubset_level_up_*` |
| C | `CLIENT_CLUBSETWORKSHOP_REQ_UP_LEVEL_CONFIRM` | `0x165` | no pending ClubSet → `0x23E` `shopSys(0x5300301)`; no IFF `0x5300302`; stat&gt;4 `0x5300303`; truncated/else full `0x5300300`. Success `0x216` type `0xCC` then `0x23E` u32 0 + stat + id |
| C | `CLIENT_CLUBSETWORKSHOP_REQ_UP_LEVEL_CANCEL` | `0x166` | no pending ClubSet → `0x23F` `shopSys(0x5300251)`; stat&gt;4 `0x5300252`; no IFF `0x5300253`; recovery exhausted `0x5300254`; else full `0x5300250`. Success decrement `C[stat]` + recovery++ then `0x216` type `0xCC` then `0x23F` u32 0 + id |
| C | `CLIENT_OPEN_LUCKY_POUCH` | `0xB2` | generic MyRoom box via SQL `box_mail_catalog`: consume box, add reward, per-reward `0xAA`, then `0x129` u8 0 + box typeid + remaining + count + reward typeid/id/qntd/8 zeros. Any fail u8 1 + 12 zeros |
| C | `CLIENT_COMPLETE_QUEST` | `0xAE` | tipo 0/1/2 success `0x11F` u8 tipo + u8 1 + u32 flags + mail `@ADM`; già fatto `shopSys(0x5300551)`; ordine `shopSys(0x5300554)`; tipo ignoto `0x44` u8 `0xE2` + `shopSys(0x5300552)`. Opposite messenger Friend_List `0x11F`. |
| C | `CLIENT_TAKE_MAIL` | `0x146` | i32 id. ITEM SQL stand-in: `leftItems` + warehouse, `0x216` type 2 empty UCC PStr+status+seq+5 zero (15 byte, non pad Papel 25) poi `0x214` u32 0. Box vuoto/`id≤0` `0x5500100`; no item `pacote214(1)`; group≠ITEM `pacote214(3)`; add fail `pacote214(2)`. Opposite `SERVER_MAIL_TAKE`. |
| C | `CLIENT_HEARTBEAT` | `0xF4` | no reply |
| C | `CLIENT_REQUEST_UPDATE_USER_PLACE` | `0xC1` | sbyte place; no reply |
| C | `CLIENT_USE_TICKET_REPORT` | `0xAA` | Tourney FINISH + level≥6 + typeid `0x1A000041`: `pacote0AA` remaining C0, `0x12A` u32 0, `0x4C` -1, leave 10 (`0x61`+`0x11B` oid). Versus/not-FINISH/no item silent. Opposite `SERVER_NEW_ITEM` |
| C | `CLIENT_ACTIVE_PAWS_EFFECT` | `0x15C` | not-in-room silent |
| C | `CLIENT_ACTIVE_RING_EFFECT` | `0x15D` | not-in-room silent |
| C | `CLIENT_REQ_POINT_SHOP_TP` | `0x127` | not blocked → `0x1E8` u32 0 + u32 tiki pts (seed 0) |
| C | `CLIENT_REQ_POINT_SHOP_EXCHANGE_TP` | `0x128` | u8 count + 16-byte rows (typeid/id/qntd/client value). SQL Tiki item count/points; consume warehouse + persist TP + `0x216`, then `0x1E9` u32 0 + points. IFF/consume/zero errors `0x5200902`/`0x5200903`/`0x5200905` |
| C | `CLIENT_REQ_POINT_SHOP_EXCHANGE_ITEM` | `0x129` | u8 count + 12-byte rows (typeid/qntd/client TP). Existing `pangya_tiki_points_items`; charge TP + add warehouse + `0x216`, then `0x1EA` u32 0 + remaining. Missing/zero/short points/add `0x5200902`/`0x5200905`/`0x5200906`/`0x5200907`; opposite SERVER lucky-pouch |
| C | `CLIENT_CLUBSETWORKSHOP_REQ_UP_RANK` | `0x167` | qntd>0 missing card → `0x240` `shopSys(0x5300351)`; qntd insufficient `shopSys(0x5300532)`; missing club `0x5300353`; no `iff_clubset` `0x5300354`; tipo -1 `0x5300355`; no limit any `0x5300209`; no limit for calcRank+1 `0x5300211`; qntd>4 `0x5300356`; stat capped `0x5300358`; no rank-exp `0x5300359`; mastery `0x5300360`; truncated/else full `0x5300350`. Success persist `C[stat]++` + rank/level/mastery then `0x216` type `0xCC` then `0x240` u32 0 + stat + id. `flag_transformar==1` + SQL original (player non owner) → empty `0x241` invece di `0x240`. Mega typeid 0 qntd 1 resta `shopSys(0x5300351)` |
| C | `CLIENT_USE_ITEM_BUFF` | `0xD8` | typeid 0 → `0x181` `shopSys(0x5500401)`; ITEM+SQL `iff_time_limit_item` success u32 2 + count 1 + typeid + ItemBuff; missing warehouse `shopSys(0x5500402)`; non-ITEM `shopSys(0x5500403)`; no TLI `shopSys(0x5500404)` |
| C | `CLIENT_COMET_REFILL` | `0xEC` | ITEM+BALL SQL `pangya_comet_refill`; success `0x197` u8 1 + item + ball + u16 C0; else u8 0 + 10 zeros. Opposite locker-add `SERVER_SHOP_BUY` |
| C | `CLIENT_OPEN_BOX_MAIL` | `0xEF` | generic/default SQL `box_mail_catalog`: missing/C0/group/IFF/BoxSystem → `0x6300102`–`0x6300106`; consume fail `0x6300110`; truncated/else full `0x6300100`. Success consume box + optional opened marker `0x216`, mailbox reward, then `0xA7` count 1 + `0xAA` count 0 balances + `0x19D` u32 0 + box + reward + qntd |
| C | `CLIENT_ITEMSTORAGE_REQ_GET_ITEM` | `0xCD` | empty locker → `0x16D` pages 0 + page 0 + count 0 |
| C | `CLIENT_ITEMSTORAGE_REQ_GET_PANG` | `0xD5` | `0x172` u64 pang (seed 0) |
| C | `CLIENT_NOTIFY_NOT_DISPLAY_WHISPER` | `0xDE` | named online player gets `0x40` option 4 + nick |
| C | `CLIENT_GM_IDENTITY` | `0x41` | non-GM CHANNEL catch silent |
| C | `CLIENT_MYROOM_CHECK` | `0xB5` | no channel; seed `allow_enter==0` → `0x12B` u32 0 + to_uid |
| C | `CLIENT_USE_CARD_SPECIAL` | `0xBD` | missing/qntd/IFF/non-special → `0x5500352`/`0x5500358`/`0x5500353`/`0x5500354`; unsupported/invalid effect `0x5500355`/`0x5500357`; truncated/else full `0x5500350`. SQL `iff_card` immediate Pang Effect=4 success: consume one + persist Pang + `0x160` u32 0 + card id/typeid + zero part/slot + active 1 + two zero SYSTEMTIME + u16 0 |
| C | `CLIENT_OPEN_CARD_PACK` | `0xCA` | u32 pack typeid + i32 card id. SQL `card_pack_catalog` ordered stand-in; success consume pack + add drawn cards + `0x154` u32 0 and variable rows: pack subgroup 3/4 tail u8 draw-count, card rows tail u32 1. Any fail sends client-compatible u32 1; opposite `SERVER_ONELINE_QUERY` |
| C | `CLIENT_ITEMSTORAGE_REQ_ADD_ITEM` | `0xCE` | PART SQL: `valid=0` + locker_item, `0x139` u16 0 + `0xEC` u8 1 + TradeItem 168 + `0x16E` u32 0 + u64 0 + TradeItem. Count 0 `shopSys(5100404)`; non-PART `shopSys(109)`; missing `shopSys(5100403)`; shop `shopSys(0x5201010)`. No `findPart`/UCC. Opposite attendance `0x16E`. |
| C | `CLIENT_ITEMSTORAGE_REQ_DEL_ITEM` | `0xCF` | `valid=1`/`flag=0`, `0xEC` u8 0 + pang + TradeItem + u8 3 + warehouse 196 + `0x16F` u32 0 + u64 idx + TradeItem. Truncated `5100450`; missing `shopSys(5100451)`; count 0 `shopSys(5100404)`. |
| C | `CLIENT_ITEMSTORAGE_REQ_MAKE_PASS` | `0xD0` | empty → `0x176` u32 1 |
| C | `CLIENT_ITEMSTORAGE_REQ_CHANGE_PASS` | `0xD1` | empty old → `0x174` u32 1 |
| C | `CLIENT_ITEMSTORAGE_REQ_CHANGE_MODE` | `0xD2` | empty pass → `0x173` `shopSys(5100251)` |
| C | `CLIENT_ITEMSTORAGE_REQ_UPDATE_PANG` | `0xD4` | opt 1 deposit / opt 0 withdraw → `0x171` u32 0 + `0xC8` wallet+moved + `0x172` locker; pang&gt;wallet `shopSys(5100352)`; pang&gt;locker `shopSys(5100353)`; opt ignoto `shopSys(5100351)`; pang≤0 `5100350`. Opposite CLIENT earcuff `0x171`. |
| C | `CLIENT_ACTIVE_CUTIN` | `0xE5` | not-in-room/not-in-game silent; GZ `0x18D` u8 0 + u16 3. SQL `iff_cutin_information` stand-in: valida uid + character equipaggiato; SKIN active 0 lookup diretto, CHARACTER active 1 via `cut_in[]` e condition. Success room broadcast `0x18D` u8 1 + typeid/sector/condition + 4 img tipo + tempo + 4×sprite[40]; miss u8 0 + u16 1 |
| C | `CLIENT_EXTEND_RENTAL` | `0xE6` | item_id≤0 / missing / non-PART / no `iff_part` / valor≤0 / pang short → `0x18F` u8 1; success `0xC8` remaining+spent then `0x18F` u8 0 + typeid + id. SQL `iff_part.valor_rental` stand-in; +7 days `EndDate` |
| C | `CLIENT_DELETE_RENTAL` | `0xE7` | item_id≤0 / missing / non-PART / no `iff_part` / valor≤0 → `0x190` u8 1; success u8 0 + typeid + id; SQL `valid=0` stand-in `CmdDeleteRental` |
| C | `CLIENT_UCC_LOAD` | `0xFE` | no reply |
| C | `CLIENT_UCC` | `0xB9` | option 1 i32 item id + owner: owned warehouse item → `0x12E` opt 1 + typeid + PStr idx + owner + WarehouseItem 196 with UCC fields. Unknown/missing → sbyte -1; no channel; opposite CLIENT marker `0x12E` |
| C | `CLIENT_UCC_WEB_KEY` | `0xC9` | opt+uid+seq+item id; valida uid/item, owner online e warehouse item (`0x5100101`–`104`). Success genera web key e manda `0x153` u8 0 + 1 + i32 item + PStr key + seq; opposite `SERVER_ONELINE_MSG` |
| C | `CLIENT_CHECK_ATTENDANCE` | `0x16E` | empty catalog → `0x248` u32 `~0`; success i32 0 + u8 login + now + after + counter (SQL catalog stand-in, no IFF/`addItem`); opposite `SERVER_LOCKER_ADD` |
| C | `CLIENT_ATTENDANCE_LOGIN` | `0x16F` | empty catalog → `0x249` u32 `~0`; success i32 0 + ari (`after` draw); no mailbox GP/bot/fortune; opposite `SERVER_LOCKER_REMOVE` |
| C | `CLIENT_CLUB_WORKSHOP_EVENT` | `0x172` | always `0x24E` 0/3000/0/100/0/10/10; opposite `SERVER_LOCKER_PANG` |
| C | `CLIENT_ENTER_LOBBY_GRAND_PRIX` | `0x176` | `property` bit 11 → lobby dump (no `0xF5`) + `0x250` OK + GP-event bits + v_gpc 0 + f32 avg; already-in-lobby → `0x250` u32 0; opposite `SERVER_LOCKER_MAKE_PASS` |
| C | `CLIENT_LEAVE_LOBBY_GRAND_PRIX` | `0x177` | leave lobby (no `0xF6`) + `0x251` u32 0 |
| C | `CLIENT_ENTER_ROOM_GRAND_PRIX` | `0x179` | SQL `grand_prix_event` active stand-in (optional time/ticket/clear restrictions absent): missing `0x253 shopSys(0x6700001)`, level `0x6700006`, full `0x6700005`, else full `0x6700000`. Success creates/reuses tipo 20 max-30 room with configured name/holes/course/mode/natural/rule and sends normal room enter packets |
| C | `CLIENT_EXIT_ROOM_GRAND_PRIX` | `0x17A` | not-in-room silent |
| C | `CLIENT_ENTER_MY_ROOM` | `0xB7` | channel → `0x168` `PlayerRoomInfoEx` 861 then `0x12D` u32 1 + u16 poster count (seed 0); catch silent; opposite CLIENT workshop transform confirm |
| C | `CLIENT_PLAY_BIG_PAPEL_SHOP` | `0x186` | 10 balls, `0xC8` remaining+0 then `0x216`/`0xFB`/`0x26C`; funds `shopSys(0x5900102)` |
| C | `CLIENT_CHAR_MASTERY_EXPAND` | `0x187` | typeid/id 0 → `0x26E` `shopSys(0x5200651)`; truncated → full `0x5200650`; success `0x216` awards (consume + type `0xCD` mastery) then `0x26E` u32 0 |
| C | `CLIENT_CHAR_STATS_UP` | `0x188` | missing char → `0x26F` `shopSys(0x5200501)`; truncated `ToRead` → full `0x5200500`; success `0xC8` remaining+spent then `0x216` type `0xC9` PCL then `0x26F` u32 0 + stat |
| C | `CLIENT_CHAR_STATS_DOWN` | `0x189` | missing char → `0x270` `shopSys(0x5200551)`; truncated → full `0x5200550`; success `0x216` type `0xC9` then `0x270` u32 0 + stat |
| C | `CLIENT_CHAR_CARD_EQUIP` | `0x18A` | IFF miss → `0x271` `shopSys(0x5200757)`; truncated → full `0x5200750`; success `0x216` (consume + type `0xCB` price/slot) then `0x271` u32 0 + card typeid |
| C | `CLIENT_CHAR_CARD_PATCHER` | `0x18B` | missing Club Patcher → `0x272` `shopSys(0x5200810)`; truncated → full `0x5200800`; success `0x216` (patcher+card consume + type `0xCB`) then `0x272` u32 0 + card typeid |
| C | `CLIENT_CHAR_CARD_REMOVE` | `0x18C` | missing char → `0x273` `shopSys(0x5200851)`; truncated → full `0x5200850`; success `0x216` then `0x273` u32 0 + card typeid |
| C | `CLIENT_TIKI_SHOP_EXCHANGE` | `0x18D` | distinct from CLIENT `0x129`; u32 count, C# precheck count*8 then reads 12-byte typeid/id/qntd rows. SQL tiki pang/mileage metadata; consume, no-bonus mileage rollover into `0x1A0002A7`/Tiki point `0x1A0002A6`, Pang charge, then `0xC8` + `0x216` + `0x274` u32 0 + mileage + bonus. count/truncated/item/consume/add errors `5200451`/`5200452`/`0x52000901`/`0x5200903`/`0x5200904`; else full `0x5200900` |
| C | `CLIENT_FINISH_GAME` aliases | `0xCB` / `0x12C` | same as CLIENT `0x06`; not-in-game silent |
| C | `CLIENT_RING_PAWS_RAINBOW` / power / miracle / paws-set | `0x196`/`0x197`/`0x198`/`0x199` | not-in-room silent; `0x196` opposite `SERVER_LOUNGE_STATE`; `0x197` opposite `SERVER_COMET_REFILL` |
| C | `CLIENT_GZ_INITIAL` / marker / shot-end / chip-in / GZ first / wing / earcuff / glove / ring-ground / assist / Event Arin | `0x12D`/`0x12E`/`0x12F`/`0x131`/`0x137`/`0x138`/`0x171`/`0x180`/`0x181`/`0x184`/`0x185`/`0x192` | not-in-room CHANNEL catch silent; shot-end success `0x1F7` i32 oid + u8 hole + 87-byte echo; chip-in GZ Practice `0x1F2` empty then finish dump; GZ first `0x137` all-player barrier then per player `0x8D` 0 + `0x53` oid + broadcast `0x6D` start + `0x1F4` i32 1 |
| C | `CLIENT_CLUBSETWORKSHOP_TRANSFORM_CONFIRM` | `0x168` | no pending ClubSet → `0x242` `shopSys(0x5300451)`; no source IFF `0x5300452`; no transform IFF `0x5300453`; delete fail `0x5300454`; add fail `0x5300456`; else full `0x5300450`. Success delete source + add original then `0x216` count 2 type 2 + `0x242` u32 0 + typeid + id. Opposite `SERVER_MY_ROOM_CHAR` |
| C | `CLIENT_CLUBSETWORKSHOP_TRANSFORM_CANCEL` | `0x169` | no pending ClubSet → `0x243` `shopSys(0x5300401)`; no IFF `0x5300402`; stat&gt;4 `0x5300403`; else full `0x5300400`. Success `0x243` u32 0 + stat + id |
| C | `CLIENT_CLUBSETWORKSHOP_RECOVERY` | `0x16B` | typeid 0 → `0x246` `shopSys(0x5300151)`; truncated → full `0x5300150`; C0&lt;1 `shopSys(0x5300152)`; missing ClubSet `shopSys(0x5300153)`; no `iff_clubset` `shopSys(0x5300154)`; tipo -1 `shopSys(0x5300155)`; consume fail `shopSys(0x5300156)`; already recovered `shopSys(0x5300157)`; success `0x216` (type 2 consume + type `0xCC` + workshop 23) then `0x246` u32 0 |
| C | `CLIENT_CLUBSETWORKSHOP_TRANSFER` | `0x16C` | missing UCIM → `0x245` `shopSys(0x5300104)`; truncated → full `0x5300100`; C0&lt;qntd `shopSys(0x5300105)`; missing ClubSet `shopSys(0x5300101)`; no `iff_clubset` `shopSys(0x5300102)`; dest tipo -1 `shopSys(0x5300103)`; Rank S `shopSys(0x5300108)`; extra chips `shopSys(0x5300106)`; consume fail `shopSys(0x5300107)`; success `0x216` (type 2 + two type `0xCC`) then `0x245` u32 0. Opposite `SERVER_LOCKER_ACCESS`. SlotStats IFF as zeros |
| C | `CLIENT_CLUBSET_RESET` | `0x16D` | unknown typeid / `s_calcRank==-1` → `0x247` `shopSys(0x5300506)`; missing item `0x5300501`; C0&lt;1 `0x5300502`; missing club `0x5300503`; no `iff_clubset` `0x5300504`; no rank-exp `0x5300505`; consume fail `0x5300507`; truncated → full `0x5300500`. Soft `0x1A000247` / hard `0x1A00024B`: persist then `0x216` count 3 (type 2 + `0xCC` + `0xC9`) then `0x247` u32 0 + typeid + id. Hard also `0xC8` remaining+0 (rank[] stand-in empty). Opposite `SERVER_LOCKER_ITEMS` |
| C | `CLIENT_PLAY_MEMORIAL` | `0x17F` | SQL `memorial_reward_catalog` ordered stand-in. Coin 0/non-ITEM/missing/no IFF/no system → `0x6300301`–`0x6300305`; empty draw `0x6300306`; consume/add `0x6300311`/`0x6300312`; truncated/else full `0x6300300`. Success add rewards + consume coin, `0x216` reward rows then coin row, then `0x264` u32 0 + count + rarity/typeid/qntd |
| C | `CLIENT_UPDATE_INGAME_WEBPAGE` | `0xA1` | sbyte `place`; no reply |
| C | `CLIENT_REQUEST_PANG_INFO` | `0xA2` | `0xC8` only if pang changed |
| C | `CLIENT_JOIN_GALLERY` | `0x3E` | spy enter; locked room + matching password enters (`0x4A`/`0x49`/`0x48`); missing/unlocked/wrong password silent |
| C | `CLIENT_GM_COMMAND` | `0x8F` | non-GM `0x40` red `Nao conseguiu executar o comando.`; `CCG_VISIBLE` lobby `0x46` option 3 + green `Executed Command.`; `CCG_WHISPER`/`CCG_CHANNEL` (i16 4/5 + u16) green OK; `CCG_CHANGE_WEATHER` lounge `0x9E` u16+u8 1 then green OK; not-in-room weather red fail; `CCG_KICK` (u32 oid + u8) leave-room then green OK |
| C | `CLIENT_ACTIVE_AUTO_COMMAND` | `0x156` | not-in-room silent |
| C | `CLIENT_REQUEST_KICK` | `0x61` | log only |
| S | `SERVER_MESSENGER_LIST` | `0xFC` | u8 count + 92-byte rows |
| S | `SERVER_GACHA_COUPON` | `0x102` | i32 normal + i32 partial + u64 pang + u64 cookie |
| S | `SERVER_CLUB_STATS` | `0xA5` | fail u8 0; success kind + ClubSet + stat + id + pang |
| S | `SERVER_INTRUSION` | `0x113` | fail u8 6 + u8 sys; time query u8 3 + 0 + u16 room + u32 elapsed + u32 time30s + RoomInfo |
| S | `SERVER_PAPEL_PLAY` | `0x21B` | u32 0 + i32 coupon + count + balls + pang + cookie; fail u32 sys |
| S | `SERVER_PAPEL_REMAIN` | `0xFB` | i32 remain + i32 flag; unlimited -1/-3; opposite CLIENT web-key `0xFB` |
| S | `SERVER_WEB_AUTH_KEY_ACK` | `0x1AD` | i32 option + PStr key (i16 0 if empty) |
| S | `SERVER_REQ_CHANGE_GAME_SERVER_ACK` | `0x1D4` | i32 option; PStr key only when option 0 |
| S | `SERVER_OPEN_TIKI_REPORT` | `0x11A` | fail i32 -1 + 16 date zeros; success u32 player count + SYSTEMTIME + player rows |
| S | `SERVER_REQ_POINT_SHOP_OPEN_ACK` | `0x1E7` | u32 option |
| S | `SERVER_ITEMSTORAGE_RES_ACCESS` | `0x16C` | u32; opposite CLIENT workshop transfer |
| S | `SERVER_ITEMSTORAGE_RES_STATE` | `0x170` | u32 0 + u32 isLocker |
| S | `SERVER_CLUBSETWORKSHOP_REQ_UP_LEVEL_ACK` | `0x23D` | u32 sys |
| S | `SERVER_LUCKY_POUCH` | `0x129` | fail u8 1 + 12 zeros; success box/remaining/count/reward rows; opposite CLIENT tiki |
| S | `SERVER_TIKI_POINTS` | `0x1E8` | u32 0 + u32 pts |
| S | `SERVER_TIKI_EXCHANGE_TP` | `0x1E9` | u32 error; OK is 0 + pts |
| S | `SERVER_TIKI_EXCHANGE_ITEM` | `0x1EA` | u32 error; OK is 0 + pts |
| S | `SERVER_CLUBSETWORKSHOP_CONFIRM` | `0x23E` | u32 sys |
| S | `SERVER_CLUBSETWORKSHOP_CANCEL` | `0x23F` | u32 sys |
| S | `SERVER_CLUBSETWORKSHOP_RANK` | `0x240` | fail u32 sys; success u32 0 + u32 stat + i32 id |
| S | `SERVER_CLUBSETWORKSHOP_TRANSFORM` | `0x241` | empty dialog |
| S | `SERVER_ITEM_BUFF` | `0x181` | fail u32 sys; success u32 2 + u32 1 + typeid + ItemBuff 65; opposite CLIENT ring-ground |
| S | `SERVER_COMET_REFILL` | `0x197` | fail u8 0 + 10 zeros; success u8 1 + u32 item + u32 ball + u16 C0. Opposite CLIENT ring-power |
| S | `SERVER_BOX_CONSUME` | `0xA7` | u8 count + box typeid/id/u16 remaining |
| S | `SERVER_BOX_MAIL` | `0x19D` | fail u32 sys; success u32 0 + box typeid + reward typeid + i32 qntd |
| S | `SERVER_LOCKER_ITEMS` | `0x16D` | u16 pages + u16 page + u8 count |
| S | `SERVER_LOCKER_PANG` | `0x172` | u64 pang |
| S | `SERVER_MY_ROOM` | `0x12B` | u32 option + u32 to_uid |
| S | `SERVER_LOCKER_MAKE_PASS` | `0x176` | u32 |
| S | `SERVER_LOCKER_CHANGE_PASS` | `0x174` | u32 |
| S | `SERVER_LOCKER_MODE` | `0x173` | u32 |
| S | `SERVER_LOCKER_ADD` | `0x16E` | fail u32 sys; success u32 0 + u64 0 + TradeItem 168. Opposite attendance |
| S | `SERVER_DELETE_CARD` | `0x139` | locker-add prelude u16 0 |
| S | `SERVER_LOCKER_REMOVE` | `0x16F` | u32 sys |
| S | `SERVER_LOCKER_UPDATE_PANG` | `0x171` | u32 sys; opposite CLIENT earcuff |
| S | `SERVER_OPEN_CARD_PACK` | `0x154` | fail u32 1; success u32 0 + consumed-pack row + drawn-card rows; opposite CLIENT daily-quest leave |
| S | `SERVER_USE_CARD` | `0x160` | fail u32 sys; immediate-effect success u32 0 + CardEquipInfo identity/date fields |
| S | `SERVER_EXTEND_RENTAL` | `0x18F` | fail u8 1; success u8 0 + typeid + id after `0xC8` |
| S | `SERVER_DELETE_RENTAL` | `0x190` | fail u8 1; success u8 0 + typeid + id |
| S | `SERVER_WORKSHOP_TRANSFORM_CONFIRM` | `0x242` | fail u32 sys; success u32 0 + u32 typeid + i32 id |
| S | `SERVER_WORKSHOP_TRANSFORM_CANCEL` | `0x243` | fail u32 sys; success u32 0 + u32 stat + i32 id |
| S | `SERVER_WORKSHOP_TRANSFER` | `0x245` | fail u32 sys; success u32 0 after `0x216`. Opposite CLIENT locker access |
| S | `SERVER_WORKSHOP_RECOVERY` | `0x246` | fail u32 sys; success u32 0 after `0x216` |
| S | `SERVER_CLUBSET_RESET` | `0x247` | u32 sys |
| S | `SERVER_MEMORIAL` | `0x264` | fail u32 sys; success u32 0 + count + i32 rarity/u32 typeid/u32 qntd rows |
| S | `SERVER_UCC` | `0x12E` | fail sbyte -1; info success opt/typeid/PStr idx/owner/WarehouseItem; opposite CLIENT marker |
| S | `SERVER_UCC_WEB_KEY` | `0x153` | fail u8 1 + u8 1 + u32; success u8 0 + 1 + item id + PStr key + seq; opposite CLIENT daily-quest reward |
| S | `SERVER_WORKSHOP_EVENT` | `0x24E` | i32 0 + i32 3000 + i32 0 + 4×u8 |
| S | `SERVER_ATTENDANCE` | `0x248` | fail u32 `~0`; success i32 0 + `AttendanceRewardInfo` (no SYSTEMTIME) |
| S | `SERVER_ATTENDANCE_LOGIN` | `0x249` | fail u32 `~0`; success i32 0 + `AttendanceRewardInfo` |
| S | `SERVER_GP_LOBBY` | `0x250` | u32 0 + event types + v_gpc + f32 avg |
| S | `SERVER_GP_LEAVE` | `0x251` | u32 0 |
| S | `SERVER_MY_ROOM_CHAR` | `0x168` | `PlayerRoomInfoEx` 861; opposite CLIENT workshop transform confirm |
| S | `SERVER_MY_ROOM_POSTERS` | `0x12D` | u32 option + u16 count; opposite CLIENT GZ initial |
| S | `SERVER_BIG_PAPEL` | `0x26C` | u32 error |
| S | `SERVER_CHAR_MASTERY` | `0x26E` | fail u32 error; success u32 0 |
| S | `SERVER_CHAR_STATS_UP` | `0x26F` | fail u32 error; success u32 0 + u32 stat |
| S | `SERVER_CHAR_STATS_DOWN` | `0x270` | fail u32 error; success u32 0 + u32 stat |
| S | `SERVER_CHAR_CARD_EQUIP` | `0x271` | fail u32 error; success u32 0 + u32 card typeid |
| S | `SERVER_CHAR_CARD_PATCHER` | `0x272` | fail u32 error; success u32 0 + u32 card typeid |
| S | `SERVER_CHAR_CARD_REMOVE` | `0x273` | fail u32 error; success u32 0 + u32 card typeid |
| S | `SERVER_TIKI_SHOP_EXCHANGE` | `0x274` | fail u32 error; success u32 0 + earned mileage + bonus |
| S | `SERVER_CUTIN` | `0x18D` | fail u8 0 + u16 error; success u8 1 + CutinInformation fields (193-byte body); opposite CLIENT Tiki exchange |
| S | `SERVER_DELETE_ITEM` | `0xC5` | fail sbyte -1; success u8 1 + u32 typeid + u32 qntd + i32 id |
| S | `SERVER_DAILY_QUEST_STAMP` | `0x216` | unix + count; take-mail type 2 uses empty UCC PStr + status + seq + 5 zeros (15), not Papel 25-byte pad. Opposite C# `pacote216` item-update |
| S | `SERVER_MAIL_TAKE` | `0x214` | i32 error; 0 ok after `0x216`. Opposite `CLIENT_TAKE_MAIL` `0x146` |
| S | `SERVER_DAILY_QUEST_INFO` | `0x225` | option + current/accept unix + count + 3×typeid + deletes |
| S | `SERVER_DAILY_QUEST_ACCEPT` | `0x226` | option + count + accepted AchievementInfoEx rows |
| S | `SERVER_DAILY_QUEST_REWARD` | `0x227` | option + count + removed achievement ids |
| S | `SERVER_DAILY_QUEST_LEAVE` | `0x228` | option 0 + count + ids; fail option 1 only |
| S | `SERVER_ACHIEVEMENT_GUI` | `0x22C` | i32 option after GUI data |
| S | `SERVER_ACHIEVEMENT_GUI_DATA` | `0x22D` | u32 0 + duplicated count + compact achievement/quest/counter-value rows |
| S | `SERVER_LOLO_TIPO` | `0x229` | u32 card tipo (NORMAL 0) after compose |
| S | `SERVER_LOLO` | `0x22A` | fail u32 error; success u32 0 + u32 typeid |
| S | `SERVER_CADIE` | `0x22F` | u32 0 + seq + receive item on success; u32 error on fail |
| C | `CLIENT_SHOP_OPEN_ITEMS` | `0x7C` | u32 count + 172-byte `PersonalShopItem`; success `0xEB` u32 1 + nick 22 + uid + items. Count 0/`>10` `shopSys(5200251)`; no shop `shopSys(5200252)`; truncated/IFF/`qntd`/price → full `5200250`. Opposite SERVER master `0x7C` |
| C | `CLIENT_SHOP_BUY` | `0x7D` | u32 owner + item; missing shop `0xEC` `shopSys(5200552)`; truncated/`ToRead`/buy errors full `5200550`. Success `0xEC` both (u8 1 seller / 0 buyer) + `0xED` + seller `0x40` option 7. Opposite SERVER team `0x7D` |
| S | `SERVER_SHOP_ITEMS` | `0xEB` | u32 1 + nick 22 + uid + count + items, or u32 error |
| S | `SERVER_SHOP_BUY` | `0xEC` | u32 1 + u8 remove + u64 pang + item 172 + group u8 + warehouse 196; opposite CLIENT comet `0xEC` |
| S | `SERVER_SHOP_SOLD` | `0xED` | PStr nick + uid + item + i32 (3 empty / 1 remain) |
| S | `SERVER_SHOP_VIEW` | `0xE6` | view-ok u32 1 + nick 22 + PStr name + uid + items; empty/`OPEN_EDIT` full `5200450` |

Room types (`RoomInfo.TIPO` in `pangya_game_st.cs`): STROKE=0, MATCH=1, LOUNGE=2, TOURNEY=4, TOURNEY_TEAM=5, GUILD_BATTLE=6, PANG_BATTLE=7, APPROCH=10, GRAND_ZODIAC_INT=11, GRAND_ZODIAC_ADV=13, GRAND_ZODIAC_PRACTICE=14, SPECIAL_SHUFFLE_COURSE=18, **PRACTICE=19**, GRAND_PRIX=20.

Game login CLIENT `0x02` (`GameServer.ReadLoginPacket`): `PStr id`, `uint32 uid`, `uint32 ntreevUID`, `uint16 command`, `PStr authKeyLogin`, `PStr clientVersion`, `uint32 packetVersion` (XOR-encrypted with GUID `{782AE110-2EEF-4c61-B030-A53F17634F7D}`), `uint32 isPcBang`, `PStr authKeyGame`.

Fail `SendLoginAck` writes **uint32** ack. Success `pacote044` option 0 + JP `principal()` (PStr clientVersion only, MemberInfoEx **299**, UserInfo **265**, no 277-byte pad). Then **JP `sendCompleteData` order**: characters `0x70` (513-byte `CharacterInfo` from `pangya_character_information`), caddies `0x71`, warehouse `0x73` (196-byte items from `pangya_item_warehouse`), mascots `0xE1`, equip `0x72` (116 bytes from `pangya_user_equip`), `0x4D` channel list, **`0x11F` i16 3 + TutorialInfo 12 byte** (C# LoginManager case 5), then tail (`0x102`, `0x131` Treasure Hunter 21 maps, live `0x21D`/`0x21E` from `pangya_counter_item`/`pangya_achievement`/`pangya_quest`, **`0xF1` option 0**, empty **`0x135`**, `0x144`… two `0x25D`; JP does **not** send GB `0x1B1`). Seeded accounts have empty achievement rows so those packets are still three uint32 zeros.

`ChannelInfo.ToArray()` is 77 bytes: `WriteStr(name,64)`, int16 max_user, int16 curr_user, byte id, uint32 flag, uint32 flag2. Channel ids are 0-based from YAML order (C# INI `CHANNEL1` → id 0).

`pacote04E`: byte option (1=ok, 2=full, 3=not found). Enter channel: CLIENT `0x04` + byte channel id.

Practice create CLIENT `0x08` with `tipo==19`. C# enter order: `pacote04A` (int16 -1 + `RoomInfoEx.ToArrayEx` lobby summary) then `pacote049` (int16 0 + `RoomInfoEx.ToArray()` 210 bytes) then `pacote048` list + `pacote048` self. JP `PlayerRoomInfo.ToArray` is **348** bytes; Ex is **861**. Practice uses compact `0x100` (wire option byte 0); Stroke/Match/Lounge/Pang Battle send Ex. Leave CLIENT `0x130`. Exit room CLIENT `0x0F`. Start-game CLIENT `0x0E` → empty `0x230` + empty `0x231` + `0x77` uint32 pang rate. Solo start is allowed only for Practice/GP/Grand Zodiac; Versus with one player returns `0x253` uint32 `0x5900202`. Practice/Tourney then send `0x76` (tipo_show + uint32 1 + SYSTEMTIME) and `0x52` course (18 synthetic holes, cube count 0; pin coords come from CLIENT `0x1A` because IFF files are not in the env). Versus with ≥2 players sends `0x76` player dump (MemberInfoEx + UserInfo + trophy + UserEquip + zero map stats + Character/Caddie/ClubSet/Mascot from SQL) then per-player `0x52` + `0x16A` mascot seed. SSC 2p uses Tourney `0x76`+`0x52` (`tipo_show` 4). GZ INT solo uses `0x76` tipo_show 11. CLIENT `0x1A` → `0x9E` weather + `0x5B` wind + `0x8D` remain-ms (Tourney path). Versus CLIENT `0x11` after **all** in-room players load-ok broadcasts `0x9E` u16 weather + u8 0, `0x5B` wind (synthetic weather/wind/degree 0), and `0x53` i32 oid (hole-start turn, not `0x63`). CLIENT `0x12F` ShotEndLocationData (87 bytes) broadcasts `0x1F7` i32 oid + u8 hole + echo; Versus uses turn oid/hole (`turnOid==0` silent), Tourney/Practice uses the sender; truncated/not-in-game silent. CLIENT `0x36` opt 1 CONTINUE_GO then `0x5B` + `0x63` i32 oid (player turn; same numeric as CLIENT lounge `0x63`). Opt 0 still finishes like `0x37`. `turnOid==0` (no load-ok) is silent. Skip C# `0x115` voice-rate tables (random) and last-hole `0x199` without `acerto_hole`. CLIENT `0x1B` XOR-decrypts 54-byte `ShotSyncData` with the 16-byte room key and broadcasts `0x6E`. CLIENT `0x1C` → `0xCC` empty drop. Equip CLIENT `0x20` type 0 writes `CharacterInfo` parts to `pangya_character_information` (IFF part validation skipped); types 1/3/5/8 update `pangya_user_equip`; ack `0x6B` err 4. Lounge CLIENT `0xEB` in a tipo-2 room broadcasts `0x196` oid + 4×float `StateCharacterLounge` defaults (1,1,1,1). Lobby CLIENT `0x81` (after channel): `pacote046` option 4 (clear, one `PlayerLobbyInfo` 200 bytes) then option 5 (list), `pacote047` option 0 (rooms, Practice/GZ Practice omitted), channel `pacote046` option 1 (join), empty `0xF5`. Leave CLIENT `0x82` → option 2 + empty `0xF6`. Chat CLIENT `0x03` PStr nick+msg → `0x40`. Whisper CLIENT `0x2A` → `0x84` (byte 0 FROM / byte 1 TO) or `0x40` option 6 if offline. Cookie CLIENT `0x3D` → `0x96` u64. Ready CLIENT `0x0D` u8 → invert `PlayerRoomInfo` bit 9, broadcast `0x78` oid + the client byte. Change-room CLIENT `0x0A` (master) → `0x4A` + lobby `0x47` option 3. Keepalive CLIENT `0x01` is a no-op. Buy CLIENT `0x1D`: option, u16 count, `BuyItem`×count (37 bytes), coupon id. Success: optional `0xC8` pang spent, `pacote0AA` (`0xAA` count + typeid/id/time/flag/qntd_dep/SYSTEMTIME/ucc.IDX + pang + cookie) then `0x68` uint32 0 + pang + cookie. Fail `0x68` uint32: 2 price, 4 owned, 6 not buyable, 7 funds, 9 empty, 10 parse/catch. Catalog is SQL `pangya.shop_catalog` (IFF `IsBuyItem` stand-in; seed typeid `0x1A000006` pang 100). Gift CLIENT `0x1F`: level &lt; Beginner E (`6`) → `0x6A` u32 1 before empty/items; empty qntd → 9; catalog miss (`IsGiftItem` stand-in) → 6; funds → 7; mailbox fail → 8; truncated/catch → 10. Success charges the sender without warehouse, stores mailbox `itemNum`, optional `0xC8`/`0x96`, then `0x6A` u32 0 + pang + cookie. Personal shop CLIENT `0x7C` (after open-edit `0x76`) lists 1–10 warehouse ITEM rows that exist in SQL `shop_catalog` (`findCommomItem` / `can_send_mail_and_personal_shop` stand-in); success `0xEB` is session-only. View CLIENT `0x77` while `OPEN_EDIT` or empty stays `0xE6` `5200450`; after listing, view-ok is nick 22 + PStr name + uid + items and increments visit count. Buy CLIENT `0x7D` uses the owner's unit pang × qntd (not the buyer packet price); seller gain is `Math.round(cost * 0.95f)`; `0xEC` warehouse `C0` is the sold qntd; `0xED` i32 3 if the shop vector is empty else 1. Player info CLIENT `0x2F` uid+season → 12 dump packets (`0x157` MemberInfoEx 299, `0x15E` CharacterInfo 513, `0x156` UserEquip 116, `0x158` UserInfo 265, `0x15D` GuildInfo 77 zeros, three empty `0x15C` map stats, `0x15B` 60×i32, empty `0x15A`/`0x159` trophy, `0x257`) then `0x89` err 1. Missing uid → `0x89` err 1 season 0 uid 0. GM deny err 3 (capability bit 4). Macros CLIENT `0x69` 9×64 no reply. GS list CLIENT `0x43` → `0x9F`. Rank CLIENT `0x47` → `0xA2` if a type-4 server is online. Team CLIENT `0x10` → `0x7D`. Room detail CLIENT `0x2D` → `0x86`. Invite CLIENT `0xBA` (C# `packet0BA`) → `0x12F` to sender + `0x83` to target; fail `0x12F` u16 23. Leave CLIENT `0x0F` → remaining `0x4A`+`0x48` option 2 (oid only) + leaver `0x4C` -1. Master leave with remaining players (not in-game) elects a new master (`0x7C` oid + i16 0) instead of destroying the room. Kick CLIENT `0x26` (master) force-leaves the target with `0x4C` -1. Nick lookup CLIENT `0x07` → `0xA1` (found: u8 0 + uid + MemberInfoEx 299; missing: u8 2). In-game Tourney (Practice/GP/GZ/SSC): CLIENT `0x13`/`0x15`/`0x16`/`0x18`/`0x19` reply only to the actor (`0x56`/`0x58`/`0x59`/`0x5D`/`0x60`). Versus (Stroke/Match) broadcasts the same. CLIENT `0x14` click and `0x22` turn-time have no success reply. CLIENT `0x48` load percent broadcasts `0xA3` in Versus only. CLIENT `0x55` u8 0/1 stores whisper; incoming PM to whisper=0 is `0x40` option 6. CLIENT `0x54` team chat (Match/Guild) → `0xB0` PStr nick+msg to the same team bit. Active item CLIENT `0x17` stays no-op without IFF. Daily quest CLIENT `0x151` (channel required) always sends `0x216` unix+0 then `pacote225` (option 0, `current_date` persisted on the session, accept 0, count 0, 3× typeid 0, delete 0) because the server quest table is unseeded and IFF `findQuestItem` is absent. Accept/leave/reward with `num_quest<=0` match C# catch: `0x226` option 1 + count 0, `0x228` option 1 only, `0x227` option 500050 + count 0. Delete item CLIENT `0x64` → `0xC5` sbyte -1 without IFF. Other-channel CLIENT `0x83` reuses `enterChannel` (`0x4E`); missing channel disconnects. Already-in-lobby skips the lobby dump (C# `enterLobby` throws). Not-in-lobby dumps users/rooms without `0xF5`. GameGuard `0x88`, wind-next-hole `0x141`, caddie-holiday notice `0x6B` (IFF miss), and invite-relog `0xB4` have no success reply. Achievement GUI CLIENT `0x157` with empty `map_ai` sends nothing; truncated uid → `0x22C` i32 1. Cadie CLIENT `0x158` count 0 → `0x22F` `shopSys(5200451)`; IFF miss → `shopSys(5200452)`. Lolo CLIENT `0x155` zero typeids → `0x22A` `shopSys(0x5400151)`. Messenger list CLIENT `0x8B` (no channel) → `0xFC` u8 count + 92-byte `ServerInfo` from type-3 `pangya_server_list` (empty still sends 0). Gacha CLIENT `0x9E` (channel) queries warehouse `c0` for typeids `0x1A000080`/`0x1A000083` then `pacote102`; seed has none so zeros + pang 100000 + cookie 0; SQL fail is `0x44` u8 `0xE2` + `0x5300600`. Club-set stats CLIENT `0x4B` opt 1/3 missing warehouse → `0xA5` u8 0. Intrusion CLIENT `0x9D` option 0/1 missing room → `0x113` u8 6 + u8 1. Papel play CLIENT `0x14B` with SQL `pangya_papel_shop_*` (IFF drop stand-in, seed typeid `0x1A000006` price 1000) → `pacote216` awards + `0xFB` -1/-3 + `0x21B` u32 0 + coupon 0 + 1–4 balls (id 0) + pang + cookie. Funds → `0x21B` `shopSys(0x5900102)`. Big CLIENT `0x186` is 10 balls, `0xC8` remaining+0, price 3000. Web-link CLIENT `0xA1` stores sbyte `place` with no reply. Web-guild CLIENT `0xA2`, spy `0x3E`, GM `0x8F` (non-GM), auto-command `0x156` (not in room), and kick-log `0x61` have no success reply. Web-key CLIENT `0xFB` (no channel) → `0x1AD` option 1 + 6-char PStr; SQL fail is option 0 + i16 0. Change-GS CLIENT `0x119` unknown uid resends `0x9F`; known type-1 uid `20202` → `0x1D4` option 0 + PStr game key. Ticket-report CLIENT `0xAB` without ItemManager → `0x11A` i32 -1 + 16 zeros. Legacy Tiki CLIENT `0x126` (not blocked) → `0x1E7` u32 0. Locker access CLIENT `0xCC` empty/`Sanitize` fail → `0x16C` u32 1; seed has no `pangya_dolfini_locker` pass so `"1234"` is `0x75`. Locker state CLIENT `0xD3` (no channel) → `0x170` u32 0 + u32 2. Club workshop CLIENT `0x164` typeid group ≠ ITEM → `0x23D` `shopSys(0x5300201)`; ITEM missing warehouse → `shopSys(0x5300202)`. Lucky pouch CLIENT `0xB2` catch → `0x129` u8 1 + 12 zeros. Tutorial CLIENT `0xAE` tipo not 0/1/2 → `0x44` u8 `0xE2` + `shopSys(0x5300552)`. Heartbeat `0xF4`, place `0xC1`, ticket-use `0xAA` (not in room), paws `0x15C`, and ring `0x15D` have no success reply. Tiki points CLIENT `0x127` (not blocked) → `0x1E8` u32 0 + u32 0. Tiki exchange CLIENT `0x128`/`0x129` count 0 → `0x1E9`/`0x1EA` `shopSys(0x5200905)`. Workshop confirm/cancel CLIENT `0x165`/`0x166` with no pending ClubSet → `0x23E` `shopSys(0x5300301)` / `0x23F` `shopSys(0x5300251)`. Workshop rank CLIENT `0x167` qntd&gt;0 missing card → `0x240` `shopSys(0x5300351)`. Item buff CLIENT `0xD8` typeid 0 → `0x181` `shopSys(0x5500401)`. Comet refill CLIENT `0xEC` catch → `0x197` u8 0 + 10 zeros. Mail box CLIENT `0xEF` typeid 0 → `0x19D` `shopSys(0x6300101)`. Locker items CLIENT `0xCD` empty → `0x16D` pages 0 + page 0 + count 0. Locker pang CLIENT `0xD5` → `0x172` u64 0. Refuse-whisper CLIENT `0xDE` with an online nick sends `0x40` option 4 + that nick to the named player. GM identity CLIENT `0x41` non-GM is silent. My Room CLIENT `0xB5` (no channel) seed `allow_enter==0` → `0x12B` option 0 + to_uid. Dolfini make-pass CLIENT `0xD0` empty → `0x176` u32 1; change-pass CLIENT `0xD1` empty old → `0x174` u32 1; mode CLIENT `0xD2` empty pass → `0x173` `shopSys(5100251)`. Add CLIENT `0xCE` count 0 → `0x16E` `shopSys(5100404)`; remove truncated CLIENT `0xCF` → `0x16F` u32 `5100450`. Update pang CLIENT `0xD4` opt 0 pang 1 (locker 0) → `0x171` `shopSys(5100353)`. Card pack CLIENT `0xCA` catch always `0x154` u32 1. Use-card CLIENT `0xBD` typeid 0 → `0x160` `shopSys(0x5500351)`. Extend/delete rental CLIENT `0xE6`/`0xE7` catch `0x18F`/`0x190` u8 1. Workshop transform confirm/cancel CLIENT `0x168`/`0x169` no pending ClubSet → `0x242` `shopSys(0x5300451)` / `0x243` `shopSys(0x5300401)`. Recovery CLIENT `0x16B` typeid 0 → `0x246` `shopSys(0x5300151)`. Transfer CLIENT `0x16C` missing UCIM → `0x245` `shopSys(0x5300104)` (opposite `SERVER_LOCKER_ACCESS`). Club-set reset CLIENT `0x16D` unknown typeid → `0x247` `shopSys(0x5300506)` (opposite `SERVER_LOCKER_ITEMS`). Memorial CLIENT `0x17F` coin 0 → `0x264` `shopSys(0x6300301)`. Cutin CLIENT `0xE5` in-game without IFF sends `0x18D` u8 0 + u16 1 (Tourney/Versus catch); Grand Zodiac sends u8 0 + u16 3; not-in-room/not-in-game silent. UCC load CLIENT `0xFE` has no reply. UCC CLIENT `0xB9` unknown opt → `0x12E` sbyte -1 (no channel). UCC web-key CLIENT `0xC9` uid 0 → `0x153` u8 1 + u8 1 + `shopSys(0x5100101)`. Club workshop event CLIENT `0x172` always `pacote24E` (0/3000/0/100/0/10/10). Attendance CLIENT `0x16E`/`0x16F` with empty `pangya_attendance_table_item_reward` → `0x248`/`0x249` u32 `~0`. GP lobby CLIENT `0x176` with `property` bit 11 (yaml 2048) and `GP_EVENT=1` dumps lobby without `0xF5` then `0x250` u32 0 + count 1 + type 1 + v_gpc 0 + f32 0; leave CLIENT `0x177` → `0x251` u32 0 (no `0xF6`). GP room CLIENT `0x179` typeid 0 (no IFF) → `0x253` `shopSys(0x6700001)` (=1). Not-in-room silent: GZ/marker/shot-end/chip-in/wing/earcuff/glove/ring-ground/assist/Event Arin `0x12D`/`0x12E`/`0x12F`/`0x131`/`0x137`/`0x138`/`0x171`/`0x180`/`0x181`/`0x184`/`0x185`/`0x192` and GP exit `0x17A`. My Room enter CLIENT `0xB7` (channel) → `0x168` `PlayerRoomInfoEx` 861 (position 0, master+ready, place `0x0A`, `skin[4]=0`) then `0x12D` option 1 + seed poster count 0. Big Papel CLIENT `0x186` unseeded balls → `0x26C` `shopSys(0x5900103)`. Character mastery CLIENT `0x187` typeid/id 0 → `0x26E` `shopSys(0x5200651)`; truncated → full `0x5200650`. Stats up/down CLIENT `0x188`/`0x189` missing char → `0x26F`/`0x270` `shopSys(0x5200501)`/`shopSys(0x5200551)`; truncated `CharacterInfo.ToRead` → full default. Card equip CLIENT `0x18A` IFF miss sends intended `0x271` `shopSys(0x5200757)` (C# `card==null && card.ID` NREs). Patcher CLIENT `0x18B` missing Club Patcher → `0x272` `shopSys(0x5200810)`. Remove card CLIENT `0x18C` missing char → `0x273` `shopSys(0x5200851)`. Tiki shop CLIENT `0x18D` (not `0x129`) count 0/`>5` → `0x274` `shopSys(5200451)`; remaining `< count*8` → `shopSys(5200452)`; truncated count → full `0x5200900`. Finish-game aliases CLIENT `0xCB`/`0x12C` match `0x06` (not-in-game silent). Ring paws rainbow/power/miracle/paws-set CLIENT `0x196`–`0x199` not-in-room silent.

## Ranking

C#: `RankingServer/PangyaEnums/PacketRanking.cs`

Hello is `makeRaw` `0x1388` + int32 key + byte 5 + PStr(`1970-01-01 00:00:00`) (JP `formatDateLocal(0)`).

CLIENT `0x00`: uint32 uid, PStr id, byte menu, byte item, byte term_s5, byte class, uint32 page.
Success `0x1389`: byte 0, four search bytes, then either 10 zero bytes (empty registry) or page/pages/count + rows (`uid, pos, last, value` + C# `RankCharacter.playerInfoToPacket`: u8 level, u8 term, u8 class, PStr id, PStr nick — 7 zero bytes when missing). Trailing byte `PPRT_NOT_TOP_RANK` (2) on fresh login (`search_dados.active==0`). Error: byte option + 14 zeros.
CLIENT `0x02` search: u8 option 0 nickname PStr + search_dados, or option 1 u32 position + search_dados → success `0x138C` byte 0 + four search bytes + page + rows (same tail as `0x1389`) + u16 found position in page; error `0x138C` u8 1.
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

CLIENT `0x12`: uint32 uid, PStr nickname. When auth connector live: Child→Auth `0x04` (`getInfoPlayerOnline`); success after Auth→child `0x0C` → `confirmLoginOnOtherServer` (validates uid/option/id/ip) + `0x2F` byte 0 + uid. When auth disabled (tests): JP shortcut confirms immediately. Fail: `0x2F` byte 1.
Friend list CLIENT `0x14` → `0x30` sub `0x115` + uid + state 4 + OK + 75-byte empty `ChannelPlayerInfo`, then **always** `0x30` sub `0x102` + `ManyPacket.Pagina`. Rows from per-player `FriendManager` cache (loaded on login via `ProcGetFriendAndGuildMemberInfo`). Live `ChannelPlayerInfo` only when target online **and** target's cache has viewer via `findFriendInAllFriend` (C# friend-list row logic).
Add friend CLIENT `0x18` uid+PStr nick → `0x30` sub `0x104` + OK + 65-byte `FriendInfo` + offline ChannelPlayerInfo (online target: live CPI + sub `0x106` to added player). Agree `0x19` → sub `0x109`. Block `0x1A` → sub `0x10C` + `0x10F` to target. Remove `0x1C` uid+PStr nick → sub `0x10B`. Logout CLIENT `0x16` or TCP disconnect → broadcast sub `0x10F`. Check nick CLIENT `0x17` PStr → sub `0x117` OK (code 0 + nick + uid) or error. State CLIENT `0x1D` u8 → broadcast sub `0x115` to friends. Channel CLIENT `0x23` 75-byte CPI → echo `0x115` self + broadcast friends. Chat friend CLIENT `0x1E` uid+PStr msg → sub `0x113` u8 0 to online target. Unblock CLIENT `0x1B` uid → sub `0x10D`; online friend also gets `0x115`. Alias CLIENT `0x1F` uid+PStr apelido → sub `0x119`. Guild chat CLIENT `0x25` PStr msg → sub `0x113` u8 1 to self + online guild members (requires `pangya_guild_member`). Room invite CLIENT `0x24` u32 uid (must match session) → log-only like C#. Login loads guild uid/name via SQL join. Auth S2S `0x0D` (`requestSendCommandToOtherServer`): u32 req_server_uid + i16 command_id + body → `funcs_as` `0x01` accept / `0x02` exit / `0x03` kick (u32 club_id + u32 member_uid): refresh online guild members' `0x30` sub `0x102` pages, broadcast `0x3B` join or `0x3C` leave to other guild members.

| Dir | C# | Opcode |
|-----|----|--------|
| C | `CLIENT_CONNECT_0x12` | `0x12` |
| C | `CLIENT_REQ_USERINFO_OFFLINE` | `0x13` |
| C | `CLIENT_NOTIFY_LOGOUT` | `0x16` |
| C | `CLIENT_REQ_CHECK_NICK` | `0x17` |
| C | `CLIENT_REQ_FRIEND_UNBLOCK` | `0x1B` |
| C | `CLIENT_NOTIFY_UPDATE_MY_STATUS` | `0x1D` |
| C | `CLIENT_REQ_CHAT_FRIEND` | `0x1E` |
| C | `CLIENT_REQ_ASSIGN_APELIDO` | `0x1F` |
| C | `CLIENT_REQ_UPDATE_CHANNEL_INFO` | `0x23` |
| C | `CLIENT_REQ_CHAT_GUILD` | `0x25` |
| C | `CLIENT_NOTIFY_ROOM_INVITE` | `0x24` |
| C | `CLIENT_GUILD_BATTLE_ROOM_INVITE` | `0x28` |
| C | `CLIENT_GIFT_ITEM_NOTIFY` | `0x29` |
| C | guild joined/banish/shield/name notify | `0x2A`–`0x2D` |
| S | `SERVER_CONNECT_0x2E` | `0x2E` |
| S | `SERVER_LOGIN_ACK_0x2F` | `0x2F` |
| S | `SERVER_FRIEND_AND_GUILD_LIST_0x30` | `0x30` |
| S | guild member joined (auth AS `0x01`) | `0x3B` |
| S | guild member left/kicked (auth AS `0x02`/`0x03`) | `0x3C` |
| S | sub register/unblock/apelido/chat/logout | `0x104`/`0x10D`/`0x119`/`0x113`/`0x10F` |

## Golden fixtures

| Fixture | Source | Status |
|---------|--------|--------|
| PUBLIC/PRIVATE_KEY_TABLE | `CryptoOracle.cs` | extracted `core-protocol/.../crypto/*.bin` |
| ServerEncrypt roundtrip | MiniLzo.cs + Cipher.cs | S1 |
| Login 14-byte hello | `LoginServer.cs` | S1/S2 |
| Client captures | real client | **gap**: no official client in env |
