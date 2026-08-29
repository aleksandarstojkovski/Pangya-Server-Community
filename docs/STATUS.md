# STATUS — Pangya Java 21 JP rewrite

**Aggiornato:** 2026-08-29 13:20 UTC
**HEAD:** `5d20738` (branch di lavoro `cursor/phase0-recovery-audit-0d4c`)
**Fonte comportamento C#:** `reference/pangya-server-community` (`Server/JP/`, branch `Develop`)

## Questo turno (2026-08-29 ~13:20 UTC)

1. **Ri-verifica** (obbligatoria): riletti STATUS/EPIC; ri-lanciato compose health (7/7) e
   `./gradlew --rerun-tasks` completo → riconfermato 386 test, 1 flake `FlywayMigrationTest`.
   Nessuna discrepanza con lo stato dichiarato.
2. **EPIC.md** riscritto come piano slice MVP Torneo (S-T1..S-T6).
3. **S-T1 FATTO**: `./gradlew test` ora **verde deterministico** (vedi §3). Fix: (a)
   `FlywayMigrationTest` su DB dedicato isolato; (b) `build.gradle.kts` serializza i task
   `Test` via build service `maxParallelUsages=1`. Commit `abe9f09`.

**Prossima slice:** S-T4 — IT Torneo end-to-end fino a finish + risultato (`GameHandler:4018`).
**Blocker:** nessuno. Ambiguità aperta (non blocca S-T4): DoD#4 "ranking aggiornato" = placar
in-partita e/o Ranking server globale (wiring assente) — da chiarire, tratto entrambi.

> Questo file è stato **riscritto da zero** in un audit di recovery (Fase 0). Le versioni
> precedenti di `STATUS.md`/`EPIC.md` contenevano affermazioni non verificabili: ogni riga
> qui sotto ha accanto il comando che l'ha prodotta e il suo esito reale. Dove non ho potuto
> verificare, scrivo **non verificato**.

---

## 1. Ambiente (docker compose) — VERIFICATO ✅

```
$ docker compose ps            # tutti e 7 i servizi Up (healthy)
auth       Up (healthy)   7777, 9077
login      Up (healthy)   10203, 9103
game       Up (healthy)   20202, 9202
ranking    Up (healthy)   4774, 9474
messenger  Up (healthy)   30201, 9302
postgres   Up (healthy)   5432
redis      Up (healthy)   6379

$ for p in 9077 9103 9202 9474 9302; do curl -fsS 127.0.0.1:$p/health; done
ok auth
ok login
ok game
ok ranking
ok messenger
```

Docker non è preinstallato nella VM Cloud Agent: è stato installato in modalità nested
(`fuse-overlayfs`, `iptables-legacy`, `dockerd` via `setsid`). Vedi `EPIC.md` §Ambiente.

---

## 2. Test per modulo — VERIFICATO

Comando (esecuzione forzata, nessuna cache, un fallimento non maschera gli altri):

```
$ ./gradlew --no-daemon --continue --rerun-tasks :core-protocol:test :core-network:test \
    :core-db:test :server-auth:test :server-login:test :server-game:test \
    :server-ranking:test :server-messenger:test
GRADLE_EXIT=1
```

Risultati reali estratti dai report JUnit XML:

| Modulo | tests | fail | esito |
|--------|------:|-----:|-------|
| `core-protocol`   | 103 | 0 | **PASS** |
| `core-network`    | 9   | 0 | **PASS** |
| `core-db`         | 45  | 1 | **FAIL** (1 test, vedi §3) |
| `server-auth`     | 1   | 0 | **PASS** |
| `server-login`    | 7   | 0 | **PASS** |
| `server-game`     | 195 | 0 | **PASS** |
| `server-ranking`  | 6   | 0 | **PASS** |
| `server-messenger`| 20  | 0 | **PASS** |
| **Totale**        | **386** | **1** | 385 verdi |

> Questo era il **baseline pre-fix** (l'unico rosso era il flake shared-DB). Dopo S-T1 il run
> parallelo completo è **386/386 verde deterministico** — vedi §3.

---

## 3. Flakiness shared-DB — RISOLTA in S-T1 ✅

**Prima (pre-fix):** in run parallelo (`org.gradle.parallel=true`) fallivano a rotazione
assert basati su contatori/idempotenza, perché più moduli migravano/mutavano lo **stesso**
DB `pangya` concorrentemente. Esempi osservati: `FlywayMigrationTest.migratesAndIsIdempotent`
(`expected <0> but was <4>`) e, in run diversi, `GameFlowIT.attendanceCheck…`
(`expected <3> but was <0>`) / `personalShopBuy…`. **Prova di causa:** run **single-worker**
completo già verde (386/386), quindi non era un bug di schema/logica ma isolamento test.

**Fix (S-T1, commit `abe9f09`):**
1. `FlywayMigrationTest` migra un **DB dedicato** (`pangya_flyway_it`, drop+create) → idempotenza deterministica.
2. `build.gradle.kts`: build service `pangyaSharedDatabase` (`maxParallelUsages=1`) su tutti i task `Test` → serializzati (condividono il DB), resto del build parallelo.

**Dopo (post-fix), `./gradlew test` parallel default, 2 run consecutivi:**
```
$ ./gradlew --no-daemon --rerun-tasks test    (x2)
BUILD SUCCESSFUL in 2m / 1m 57s     # tests=386  failures+errors=0
```

Migrazioni reali nel repo: **43 file** `V1..V43` (0 repeatable). Il run verde conferma 202
tabelle schema `pangya` e i seed (testuser/testuser2/newuser, shop, cadie, IFF …).

---

## 4. Qualità dei test (anti-allucinazione) — VERIFICATO

- `@Test` totali: **386**. Test "sempre-verdi" (`assertTrue(true)`, `assertEquals(1,1)`): **0** trovati.
- File di test **senza alcun** `assert/verify/fail`: **0**.
- `GameFlowIT` sono veri IT end-to-end: un fake client si connette, attende opcode reali e
  asserisce i campi delle risposte del server (es. `awaitOpcode(...)` + `assertEquals`).
- **20** file di test in `core-protocol` leggono l'**archivio C# reale** da
  `reference/pangya-server-community/...` (IFF). Verifica indiretta: prima del clone del
  reference questi test fallivano con `missing .../pangya_jp.iff`.
- Cipher/framing (`core-protocol/crypto`): verdi, ma su **vettori sintetici/strutturali**
  (round-trip encrypt/decrypt + formato framing), **non** su capture reali del client S9.
- Capture reali client JP Season 9 nel repo: **0** (invariato rispetto all'ammissione dei doc vecchi).

---

## 5. Allucinazioni dei doc precedenti (corrette qui)

| Affermazione doc vecchi | Realtà verificata | Comando |
|---|---|---|
| "C# GameService = **193** opcode client; Java **196**; **0 mancanti** / quasi-parità" | `GameService.cs` registra **479** `addPacketCall`; Java `GameHandler` ha **196** `case`. Java copre un **sottoinsieme**, NON parità. | `grep -cE addPacketCall .../GameService.cs` → **479**; `grep -oE 'case GamePackets.CLIENT_' GameHandler.java \| sort -u \| wc -l` → **196** |
| "S6 [x] done" | `./gradlew test` **non è verde** in run completo (1 flake, §3) | vedi §2 |
| "personalShopBuy… FAIL (questo turno)" | **PASS** (195/195 server-game) | §2 |
| Percentuali di parità (85%/43%) | inventate, non misurabili | — |

**Il codice, invece, appare ben ancorato**: cita classi C# reali presenti in `reference/`
(es. `TourneyBase.cs`, `Tourney.cs`, `tourney_base_type.cs`), legge IFF reali, i test sono
sostanziali. Le allucinazioni erano concentrate **nei doc**, non nel codice → **niente rollback** (vedi EPIC §Gate rollback).

---

## 6. Lavoro dell'ultimo giorno (git) — VERIFICATO

```
$ git log --since="2026-08-27" --oneline    # tema dominante
d79b5f6 Merge PR #7  Grand Prix parity (requestSaveInfo, leaveRoom)
bc20ad7 Merge PR #6  Grand Prix finish extras
… ~40 commit "GP …" (bot, timer, placar, finish, early exit, rewards)
98bcdf0 / 5d20738  reconcile docs
```

Quasi tutto il lavoro dell'ultimo giorno è **Grand Prix (GP)**, che è **fuori dallo scope
MVP** (vedi EPIC). Non buttato: parcheggiato, da validare più avanti.

---

## 7. Comandi chiave e esito (riepilogo)

| Comando | Esito |
|---|---|
| `docker compose ps` + `curl /health` (x5) | 7/7 healthy, 5/5 `ok` |
| `./gradlew --rerun-tasks test` (parallel default, **post-S-T1**, x2) | **BUILD SUCCESSFUL, 386/386, 0 fail** (deterministico) |
| `./gradlew --rerun-tasks --continue :*:test` (pre-fix) | EXIT 1 — 385/386, 1 flake shared-DB |
| `./gradlew --max-workers=1 :core-db:test` (DB isolato) | BUILD SUCCESSFUL, 45/45 |
| `grep addPacketCall GameService.cs` | 479 |
| `grep case CLIENT_ GameHandler.java` | 196 |
| `grep -rniE 'ranking\|pangya_rank\|:4774' server-game/src/main` (no placar/rankIndex) | **0 match** → game NON aggiorna Ranking a fine partita |
| `grep gameResult( server-game/src/main` | 2 siti (righe 3700, 4018); 4018 = last-hole `TIPO_TOURNEY` |

---

## 8. Prep Fase 1 (read-only, VERIFICATO — nessuna modifica codice)

Mappato il flusso finish/ranking Torneo C#→Java per de-rischiare la Fase 1 (dettaglio in
`EPIC.md` §"Fase 1 — mappa finish/ranking Torneo"). Sintesi verificata:
- Risultato Torneo (`SERVER_GAME_RESULT`) **emesso** dal path last-hole `TIPO_TOURNEY` (`GameHandler:4018`).
- Finish/placar ramifica special-case **solo** su Grand Prix; parità **Tourney finish non verificata**.
- **Wiring game→Ranking assente** (0 match). DoD #4 "ranking aggiornato": placar in-partita
  parziale; Ranking server globale non collegato.

---

## 9. Prossimo passo

Fase 0 completa; MVP Torneo avviato. **S-T1 fatto** (baseline verde deterministico).
**Prossima slice: S-T4** — IT Torneo end-to-end fino a finish + risultato/ranking
(path last-hole `TIPO_TOURNEY`, `GameHandler:4018`). Piano slice completo in `docs/EPIC.md`.
