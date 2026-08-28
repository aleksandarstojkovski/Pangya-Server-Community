# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **143** opcode success 1:1 / **29** opcode solo fail-stub / **30** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: workshop recovery `0x16B` (`requestClubSetWorkShopRecoveryPts` / `0x216`+`0x246`) per warehouse recovery item + ClubSet by id + SQL `iff_clubset.work_shop_tipo` stand-in (no IFF `findClubSet`): consume type 2 then type `0xCC` + `ClubSetWorkshop::ToArray` 23 byte con `recovery_pts=0`; catch else `0x5300150`. Catalogo V18 vuoto (mega `clientWorkshopTypeidClub(RECOVERY, 0, 0)` resta `shopSys(0x5300151)`). Skip achievement `0x6C4000A6`. Persist `Recovery_Pts=0` prima di `0x216`/`0x246`.
Prossimo opcode/file C#: workshop transfer `0x16C` / up-level `0x164`; oppure GZ first-hole `packet137` pulse; daily `0x152`–`0x154`; box-mail `0xEF`.
Blocco: file IFF assenti (pin/cube live, `initComboDef`, cutin success `0xE5` `findCutinInfomation`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~143/197 Channel con happy-path; SQL al posto IFF; fail-stub su workshop remaining/card/UCC/cutin/memorial; zero capture JP S9.

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
