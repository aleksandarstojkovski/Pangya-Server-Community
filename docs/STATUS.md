# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **159** opcode success 1:1 / **13** opcode solo fail-stub / **14** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: memorial `0x17F` (`requestPlayMemorial`). SQL ordinato `memorial_reward_catalog` (V29, **195** tabelle) sostituisce `MemorialSystem.findCoin/drawCoin`; valida coin ITEM/warehouse/`iff_item`, aggiunge reward, consuma coin, poi `0x216` reward+coin e `0x264` u32 0 + rarity/typeid/qntd. Catch CHANNEL `shopSys`; else full `0x6300300`.
Prossimo opcode/file C#: daily `0x152`–`0x154`; UCC `0xB9`; Tiki success `0x128`/`0x129`/`0x18D`; GZ first-hole `packet137` pulse; GP enter `0x179`; ticket open `0xAB`.
Blocco: file IFF assenti (pin/cube live, `initComboDef`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~159/197 Channel con happy-path; SQL al posto IFF; fail-stub su UCC/daily/Tiki/GP/ticket; zero capture JP S9.

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
