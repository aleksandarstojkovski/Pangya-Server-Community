# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **131** opcode success 1:1 / **40** opcode solo fail-stub / **42** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **194** di quelli (manca `0x9A` PCBang mascot; `0x174`/`0x175` sono no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + `0x9A` + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: Tourney ticket-report `0xAA` → `pacote0AA` remaining C0 + `finish_game(1)` (`0x12A`/`0x4C`/`0x244`/`0x24F`) + `leaveRoom(..., 10)` (`0x61`+`0x11B` agli altri; niente `0x4C` extra). FINISH = last-hole `0x31` su `qntd_hole`. Versus / non-FINISH / C0&lt;1 / level&lt;6 silenziosi.
Prossimo opcode/file C#: `Channel.requestUpdatePCBangMascot` (`packet09A`) → `0xE2`/`0xEE`; oppure GZ first-hole pulse `0x137`.
Blocco: file IFF assenti (pin/cube live, `initComboDef`, cutin success `0xE5` `findCutinInfomation`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~130/197 Channel con happy-path; SQL al posto IFF; fail-stub su workshop/card/UCC/cutin/memorial; zero capture JP S9.

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
