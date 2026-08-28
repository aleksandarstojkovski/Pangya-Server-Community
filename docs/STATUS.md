# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **155** opcode success 1:1 / **17** opcode solo fail-stub / **18** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: cutin success `0xE5` per SKIN e CHARACTER (`TourneyBase`/`VersusBase`). SQL `iff_cutin_information` (V25, **192** tabelle) sostituisce `CutinInfomation.iff`; valida uid, character equipaggiato, gruppo/active e condition. Success broadcast `0x18D` u8 1 + 8×u32 + 4 sprite fissi da 40; fail u8 0 + u16 1; GZ resta u16 3.
Prossimo opcode/file C#: daily `0x152`–`0x154`; box-mail `0xEF`; card `0xBD`/`0xCA`; UCC `0xB9`; memorial `0x17F`; GZ first-hole `packet137` pulse.
Blocco: file IFF assenti (pin/cube live, `initComboDef`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~155/197 Channel con happy-path; SQL al posto IFF; fail-stub su card/UCC/memorial/daily/box-mail; zero capture JP S9.

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
