# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **137** opcode success 1:1 / **35** opcode solo fail-stub / **36** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: Dolfini locker add `0xCE` / remove `0xCF` (`requestAddDolfiniLockerItem` / `requestRemoveDolfiniLockerItem`) per IFF PART (SQL stand-in, no `findPart`/UCC): add `valid=0` + `pangya_dolfini_locker_item`, `0x139` u16 0 poi `0xEC` u8 1 + TradeItem poi `0x16E` u32 0 + u64 0 + TradeItem; remove `valid=1`/`flag=0`, `0xEC` u8 0 + pang + TradeItem + u8 3 + warehouse 196 poi `0x16F` u32 0 + u64 idx + TradeItem. Non-PART `shopSys(109)`; missing warehouse `shopSys(5100403)`; missing locker `shopSys(5100451)`. Count 0 resta `shopSys(5100404)`. Opposite `0xEC` CLIENT comet / personal-shop buy; `0x139` è anche `SERVER_DELETE_CARD`; `0x16E` è anche attendance.
Prossimo opcode/file C#: GZ first-hole `packet137` pulse; oppure fail-stub IFF-free (daily `0x152`–`0x154`, delete-item `0x64`).
Blocco: file IFF assenti (pin/cube live, `initComboDef`, cutin success `0xE5` `findCutinInfomation`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~137/197 Channel con happy-path; SQL al posto IFF; fail-stub su workshop/card/UCC/cutin/memorial; zero capture JP S9.

## Slice (non dichiarare S4 done)

| Slice | Stato | Nota |
|-------|-------|------|
| S0 | [x] | Gradle, Compose postgres+redis, Flyway, stub 5 server |
| S1 | [x] | Netty + Cipher bit-compat |
| S2 | [x] | Auth + Login + Redis key |
| S3 | [x] | Game core + Practice |
| S4 | [~] | Modi C# + char/card/caddie/achievement: dispatch ampio, profondità fail-stub/IFF |
| S5 | [~] | Ranking + Messenger: hello/login/friend/rank page; non tutta la superficie C# |
| S6 | [x] | `/metrics`, SessionLoadIT 3000, `scripts/verify.sh`, compose health |
