# STATUS — Pangya Java 21 JP rewrite

**Branch di riferimento:** `Develop`  
**HEAD (2026-08-29):** `d79b5f6` — Merge PR #7 *Java rewrite: Grand Prix parity (GP exit, timers, placar, leaveRoom)*  
**Fonte comportamento:** `reference/pangya-server-community` (`Server/JP/`, branch C# `Develop`).

---

## Progresso slice (checklist onesta)

| Slice | Stato | Evidenza su `Develop` |
|-------|-------|------------------------|
| **S0** | [x] | Gradle multi-modulo, Compose postgres+redis, Flyway V1–V43, stub 5 server |
| **S1** | [x] | `:core-protocol:test` + `:core-network:test` Cipher/Netty verdi in `./gradlew test` |
| **S2** | [x] | Auth S2S + Login fake-client IT verdi |
| **S3** | [x] | Practice leave/start IT verde (`GameFlowIT.fakeClientLogsInEntersChannelCreatesAndLeavesPractice`) |
| **S4** | [~] | 196 opcode client dispatchati in `GameHandler`; GP finish/exit/timer/placar/`requestSaveInfo` mergeati (PR #6–#7). Lacune verificate sotto. |
| **S5** | [~] | Moduli `:server-ranking` + `:server-messenger` + IT base verdi; profondità guild/presence non auditata vs client reale |
| **S6** | [~] | `SessionLoadIT` 3000 + `/metrics` verdi; **`./gradlew test` exit code 1** (2 failure); `scripts/verify.sh` **non eseguito** in questo turno |

**Scheletro (S0–S6):** 4 slice chiuse (S0–S3), 3 parziali (S4–S6).  
**Parità client:** fake-client `GameFlowIT` **97/98** pass; **0** capture JP Season 9 in repo/env; IFF binari testati solo da archive `reference/` (pin `.gbin` live **non verificato**).

---

## `./gradlew test` su `Develop` (2026-08-29)

```
./gradlew test --no-daemon
EXIT_CODE=1
```

| Modulo | Esito | Failure |
|--------|-------|---------|
| `:core-db:test` | **FAIL** | `FlywayMigrationTest.migratesAndIsIdempotent` — second migrate expected 0, was 4 |
| `:server-game:test` | **FAIL** | `GameFlowIT.personalShopBuyIncrementsAchievementCounter` — `IllegalStateException: no server packet` |
| Altri moduli | PASS | non verificato singolarmente oltre al summary Gradle |

Totale `@Test` nel repo: **386** (195 in `:server-game`).

---

## Inventario opcode Channel (grep, non profondità gameplay)

| Metrica | Valore | Fonte |
|---------|--------|-------|
| C# client `funcs.addPacketCall` | **193** opcode unici | `GameService.cs` |
| Java `case GamePackets.CLIENT_*` | **196** | `GameHandler.java` |
| C# senza case Java | **0** | diff script su `Develop` |
| Java extra vs C# | **3** (`0x01` KEEPALIVE, `0x9A`, `0x173`) | idem |
| Handler Java **solo no-op** `{ }` | **9** | GAMEGUARD, INVITE_RELOGIN, REQUEST_KICK, UCC_LOAD, GZ_INITIAL, EVENT_ARIN, HEARTBEAT, KEEPALIVE, CHECK_INVITE |

Conteggio “173 success 1:1 / 0 fail-stub” nei doc precedenti: **non verificato** in questo audit; contraddice i 9 no-op e il test personal-shop rotto.

---

## Lacune verificate (grep codice)

| Area | Stato | Evidenza |
|------|-------|---------|
| `CoinCubeLocationUpdateSystem` / `calcule_shot_to_coin` | **assente** | nessun match in `server-game/` |
| `requestSaveInfo` persistenza `user_info` | **solo GP** | wired in `deleteGrandPrixPlayer` / finish / early exit; Tourney/Versus/Match **non verificato** |
| Smart calculator reload (auth tipo 18) | **not ported** | `GlobalCatalogs.java:70` |
| Event SQL reload (tipi 12–17) | **stub log** | `GlobalCatalogs.java:69` |
| Capture client JP S9 | **assente** | non verificato |
| Pin `.gbin` runtime | **non verificato** | IFF test usano archive reference |

---

## Questo turno (solo ultimo commit su `Develop`)

| Campo | Valore |
|-------|--------|
| Commit | `d79b5f6` (merge PR #7) |
| Contenuto | GP parity batch: `requestSaveInfo` option 0/1/5 + `UserInfoMerge` + load/update `user_info`; `leaveRoom` → `deleteGrandPrixPlayer`; timer/placar/exit da PR #6 inclusi nel merge |
| Test | `./gradlew test` → **exit 1** (vedi sopra) |
| Doc precedenti | **obsoleti** — citavano FriendManager/attendance come “questo turno” |

---

## Prossima slice (prima voce non verde su `Develop`)

1. **Ripristinare `./gradlew test` verde** — `FlywayMigrationTest` (idempotenza migrate) + `GameFlowIT.personalShopBuyIncrementsAchievementCounter`.
2. Poi (S4): **`calcule_shot_to_coin` / `CoinCubeLocationUpdateSystem`** — C# `TourneyBase.requestSyncShot` chiama questi; Java `syncShot` non li invoca (grep zero match).

Non ripartire da S0. **Nessun nuovo opcode** finché la riconciliazione doc non è approvata.

---

## Contraddizioni risolte (doc vecchi vs `Develop`)

| Doc vecchio | Realtà `Develop` |
|------------|------------------|
| “Questo turno: FriendManager / attendance bonus” | HEAD = merge GP PR #7 |
| “Scheletro 85% / parità 43%” | Percentuali inventate — rimosse |
| S6 [x] | `./gradlew test` fallisce |
| “173 opcode / 0 fail-stub” | 9 no-op Java; profondità non auditata |
| “175 tabelle pangya” | `FlywayMigrationTest` expect **202** tabelle |
| Compose `/health` 5 server (2026-08-28) | **non verificato** questo turno |
