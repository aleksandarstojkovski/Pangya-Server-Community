# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **139** opcode success 1:1 / **33** opcode solo fail-stub / **34** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: comet refill `0xEC` (`requestCometRefill` / `pacote197`) per IFF ITEM+BALL (SQL `pangya_comet_refill` stand-in, no `findItem`/`findBall`): success `0x197` u8 1 + item typeid + ball typeid + u16 ball C0; catch resta sempre u8 0 + 10 zeros. Seed C# typeid `0x1A000105` min 1 max 30 (V16). Mega test `clientCometRefill(0,0)` resta fail. Opposite CLIENT ring-power `0x197`; CLIENT `0xEC` è anche locker-add reply `SERVER_SHOP_BUY`.
Prossimo opcode/file C#: GZ first-hole `packet137` pulse; oppure fail-stub IFF-free (daily `0x152`–`0x154`, workshop `0x164`–`0x16D`, item-buff `0xD8`).
Blocco: file IFF assenti (pin/cube live, `initComboDef`, cutin success `0xE5` `findCutinInfomation`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~139/197 Channel con happy-path; SQL al posto IFF; fail-stub su workshop/card/UCC/cutin/memorial; zero capture JP S9.

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
