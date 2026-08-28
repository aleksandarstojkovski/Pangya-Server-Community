# STATUS — Pangya Java 21 JP rewrite

Sorgente di verità: clone C# `reference/pangya-server-community` (`Server/JP/`, branch `Develop`).
Questo repo è solo la riscrittura Java. **S4 non è done.**

## Progresso

S0 [x] S1 [x] S2 [x] S3 [x] S4 [~] S5 [~] S6 [x]

S4 profondità: **164** opcode success 1:1 / **8** opcode solo fail-stub / **9** stimati rimanenti dal C# Channel

Conteggio Channel: **197** handler `packet_func_sv` registrati in `GameService.init_Packets`. Java ha uno `switch` per **195** di quelli (mancano `0x174`/`0x175`, no-op anche in C#). Success 1:1 = happy-path wire C# raggiungibile (SQL stand-in ammesso). Fail-stub = Java manda solo il catch C#; il success C# vuole IFF/`ItemManager`. Rimanenti ≈ fail-stub + GZ first-hole pulse `0x137`.

## Questo turno

Fatto: nuovo Tiki item exchange `0x18D` (`requestTikiShopExchangeItem`). V33 aggiunge pang/mileage/bonus metadata a `legacy_tiki_item_value` (restano **198** tabelle); conserva precheck C# count*8 ma legge row da 12, consuma item, rollover mileage→Tiki point, persiste pang, poi `0xC8` + `0x216` + `0x274` u32 0+earned mileage+bonus. Percorso no-bonus deterministico.
Prossimo opcode/file C#: daily `0x152`–`0x154`; UCC `0xB9`; GZ first-hole `packet137` pulse; lucky-pouch `0xB2`.
Blocco: file IFF assenti (pin/cube live, `initComboDef`); nessuna capture client JP Season 9.

Percentuale epic: **scheletro 85%** / **parità client reale 35%**.

- Scheletro: S0–S3 e S6 chiusi (Gradle, Cipher, Auth/Login, Practice, Ranking/Messenger core, metriche 3000, compose `/health`). S4/S5 aperti.
- Parità client reale: ~164/197 Channel con happy-path; SQL al posto IFF; fail-stub su UCC/daily/lucky-pouch; zero capture JP S9.

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
