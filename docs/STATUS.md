# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **173** opcode success 1:1 / **0** opcode solo fail-stub / **0** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: S5 Messenger — unblock `0x1B` → `0x10D`/`0x115`; alias `0x1F` → `0x119`; guild chat `0x25` → `0x113` u8 1 broadcast; login carica guild da SQL.
Prossimo opcode/file C#: IFF pin/cube/`initComboDef`; GM default-throw/equipDefault; Messenger `0x24` room invite; capture JP S9.
Blocco: file IFF assenti (pin/cube live, `initComboDef`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 39%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~173/197 Channel con happy-path; login dump include `pacote11F` tipo 3; Ranking page/search; Messenger presence/chat/unblock/alias/guild-chat; zero fail-stub/pulse Channel noti ma IFF/capture ancora aperti.

## Slice (non dichiarare S4 done)

| Slice | Stato | Nota |
|-------|-------|------|
| S0 | [x] | Gradle, Compose postgres+redis, Flyway, stub 5 server |
| S1 | [x] | Netty + Cipher bit-compat |
| S2 | [x] | Auth + Login + Redis key |
| S3 | [x] | Game core + Practice |
| S4 | [~] | Modi C# + char/card/caddie/achievement: dispatch ampio, profondità fail-stub/IFF |
| S5 | [~] | Ranking + Messenger: page/search + presence/chat/unblock/alias/guild-chat; room invite/auth callbacks aperti |
| S6 | [x] | `/metrics`, SessionLoadIT 3000, `scripts/verify.sh`, compose health |
