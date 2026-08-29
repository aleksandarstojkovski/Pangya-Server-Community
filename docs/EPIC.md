# EPIC — Pangya Java rewrite (C# JP → Java 21+)

**Obiettivo:** riscrittura bit-compatible con client JP Season 9.  
**Fonte comportamento:** `reference/pangya-server-community` (`Server/JP/`, branch C# `Develop`).  
**Codice attuale:** branch git **`Develop`** — non ricostruire slice già mergeate lì.

**HEAD (2026-08-29):** `d79b5f6` — Merge PR #7 *Grand Prix parity (GP exit, timers, placar, leaveRoom)*.

---

## Progresso slice

```
S0 [x]  S1 [x]  S2 [x]  S3 [x]  S4 [~]  S5 [~]  S6 [~]
```

| Metrica | Valore | Note |
|---------|--------|------|
| **Scheletro (S0–S6)** | **4/7 chiusi, 3 parziali** | S0–S3 done; S4–S6 aperti |
| **Parità client** | **97/98** `GameFlowIT` + **0** capture S9 | IFF da archive reference; pin `.gbin` live non verificato |

Percentuali inventate (**85% / 43%**) rimosse — non verificabili da git/test/grep.

---

## Questo turno (solo `d79b5f6`)

| Campo | Valore |
|-------|--------|
| Commit | `d79b5f6` merge PR #7 |
| Incluso nel merge | GP `requestSaveInfo` (option 0 finish, 1 quit, 5 DC) + `UserInfoMerge` + SQL `user_info`; `leaveRoom` centralizza `deleteGrandPrixPlayer`; batch timer/placar/exit/bot da commit `7fde00f`…`bdc6597` |
| Verifica | `./gradlew test --no-daemon` → **EXIT_CODE=1** |

---

## Verifica `./gradlew test` (Develop, 2026-08-29)

```
./gradlew test --no-daemon
EXIT_CODE=1
```

Failure verificati:

1. `FlywayMigrationTest.migratesAndIsIdempotent` — expected migrate count 0, was 4 (`:core-db:test`)
2. `GameFlowIT.personalShopBuyIncrementsAchievementCounter` — `IllegalStateException: no server packet` (`:server-game:test`)

Moduli con test verdi nel run: `:core-protocol`, `:core-network`, `:server-auth`, `:server-login`, `:server-ranking`, `:server-messenger` (non rieseguiti singolarmente dopo il fail).

---

## Git log `Develop` (ultimi 30 commit, estratto)

```
d79b5f6 Merge PR #7 GP finish extras
7fde00f Persist GP finish stats via requestSaveInfo option 0
4facc86 Wire GP quit requestSaveInfo to user_info SQL
4bd594a Wire GP deletePlayer into generic leaveRoom
901507b Align GP bad-conduct kick with deletePlayer wire order
bc20ad7 Merge PR #6 GP finish extras
6b5bf63 Wire GP exit deletePlayer parity before 0x254
d5b0635 Stop GP rule timer on hole load and hole timeout
f1f023b Fix GP placar ranking (game score not mediaScore)
829a98a Fix GP rule timer stop on sync-shot hole-out
… (GP bot, early exit, rewards IFF, tickets, enter gates — commit bc20ad7…bdc6597)
```

Tema dominante su `Develop`: **Grand Prix parity** (PR #6 + #7). Doc precedenti che citavano FriendManager/attendance come “questo turno” erano **fuori sync**.

---

## Inventario handler Channel (grep)

| | C# `GameService.funcs` | Java `GameHandler` switch |
|--|----------------------|---------------------------|
| Opcode client unici | **193** | **196** (include KEEPALIVE + 2 extra) |
| Mancanti in Java | **0** | — |
| No-op Java `{ }` | — | **9** opcode |

No-op verificati: `0x88` GAMEGUARD, `0xB4` INVITE_RELOGIN, `0x61` REQUEST_KICK, `0xFE` UCC_LOAD, `0x12D` GZ_INITIAL, `0x192` EVENT_ARIN, `0xF4` HEARTBEAT, `0x01` KEEPALIVE, `0x29` CHECK_INVITE.

Affermazione storica “173 success 1:1 / 0 fail-stub”: **non verificata** in questo audit.

---

## Lacune grep (handler / IFF / not ported)

| Pattern | Risultato |
|---------|-----------|
| `CoinCubeLocationUpdateSystem` / `calcule_shot_to_coin` in Java | **0 match** — C# `TourneyBase.requestSyncShot` li chiama; Java `syncShot` no |
| `requestSaveInfo` fuori GP | **non verificato** wiring Tourney/Versus/Match |
| `not ported` | `GlobalCatalogs` auth reload tipo **18** smart calculator |
| `stub` | `GlobalCatalogs` auth reload tipi **12–17** event SQL |
| TODO IFF espliciti | **0** in `server-game/` (solo commenti “not ported” sopra) |

---

## Piano slice (checklist)

| Slice | Scope | Stato Develop |
|-------|-------|---------------|
| **S0** | Gradle, Compose, Flyway, stub 5 server | [x] — 43 migration; test Flyway idempotency **FAIL** |
| **S1** | Netty, Cipher, MiniLZO, session | [x] — test verdi |
| **S2** | Auth + Login + Redis session key | [x] — test verdi |
| **S3** | Game core + Practice | [x] — IT verde |
| **S4** | Modalità C# + manager char/card/caddie/achievement | [~] — dispatch 193/196; GP deep parity mergeato; coin-cube learning + save-info altri modi aperti |
| **S5** | Ranking + Messenger | [~] — moduli + IT base; capture client non verificato |
| **S6** | Metriche, load 3000, `scripts/verify.sh` | [~] — SessionLoadIT OK; **`./gradlew test` FAIL**; verify.sh non eseguito |

---

## Mappa C# → Java (invariata)

| C# (JP) | Java |
|---------|------|
| `PangyaAPI.Network.Cryptor.Cipher` | `org.pangya.protocol.crypto.Cipher` |
| `PangyaAPI.Network.Cryptor.CryptoOracle` | `org.pangya.protocol.crypto.CryptoOracle` |
| `PangyaAPI.Network.Cryptor.MiniLzo` | `org.pangya.protocol.crypto.MiniLzo` |
| `PangyaAPI.SQL` + stored proc | `org.pangya.db` Jdbi + PostgreSQL SQL esplicito |
| `AuthServer` / `LoginServer` / `GameServer` / `RankingServer` / `MessengerServer` | `:server-auth` / `:server-login` / `:server-game` / `:server-ranking` / `:server-messenger` |
| `server.ini` | `application.yml` + env |

502 stored procedures C# **non portate** — equivalente Java = SQL Jdbi per-comando (`V1__pangya_schema.sql` commento).

---

## Schema PostgreSQL

- Flyway **V1–V43** su `Develop`.
- `FlywayMigrationTest` assert **202** tabelle schema `pangya` (doc vecchio “175”: **obsoleto**).
- Seed/cataloghi IFF in migration S4–S6 (shop, GP event, coin_cube, daily quest, …).

---

## Prossima slice (dopo `Develop`, non da S0)

1. **Verde `./gradlew test`** — fix `FlywayMigrationTest` + `GameFlowIT.personalShopBuyIncrementsAchievementCounter`.
2. **S4:** port/wire `calcule_shot_to_coin` + `CoinCubeLocationUpdateSystem` in `syncShot` (C# `TourneyBase` / GP override).

**Stop:** nessun nuovo opcode finché questa riconciliazione doc non è approvata.

---

## Contraddizioni doc precedenti vs `Develop`

Vedi tabella completa in `docs/STATUS.md`. In sintesi: “questo turno” errato, percentuali inventate, S6 marcato done con test rossi, conteggio opcode/stub non allineato, tabelle 175 vs 202, evidenza Compose 2026-08-28 non ri-verificata.
