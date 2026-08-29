# STATUS — Pangya Java 21 JP rewrite

**Aggiornato:** 2026-08-29 12:57 UTC
**HEAD:** `5d20738` (branch `Develop`; audit su branch `cursor/phase0-recovery-audit-0d4c`)
**Fonte comportamento C#:** `reference/pangya-server-community` (`Server/JP/`, branch `Develop`)

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

---

## 3. Unico test rosso: `FlywayMigrationTest.migratesAndIsIdempotent`

Messaggio reale:

```
org.opentest4j.AssertionFailedError: expected: <0> but was: <4>
   (core-db/.../FlywayMigrationTest.java:23  "second migrate must be a no-op")
```

**Causa individuata (VERIFICATA), NON è un bug di migrazione.** Il test migra il Postgres
Compose **condiviso** (`localhost:5432/pangya`) e non fa DROP dello schema, così altri
moduli/container possono usarlo in parallelo (`org.gradle.parallel=true`, e i container
`auth`/`game` migrano lo stesso DB con `PANGYA_MIGRATE_ON_START`). L'assert di idempotenza
"secondo migrate == 0" si rompe quando un altro `migrate()` gira concorrentemente tra le
due chiamate.

Prova che è contaminazione di stato condiviso e non correttezza schema — lo stesso test
contro un DB **fresco isolato**, single-worker, passa al 100%:

```
$ docker exec <pg> psql -U pangya -c "create database pangya_audit;"
$ PANGYA_TEST_JDBC_URL=jdbc:postgresql://localhost:5432/pangya_audit \
    ./gradlew --no-daemon --rerun-tasks --max-workers=1 :core-db:test
BUILD SUCCESSFUL in 13s     # 45/45 PASS, incl. assert 202 tabelle + tutti i seed
```

Migrazioni reali nel repo: **43 file** `V1..V43` (0 repeatable `R__`). Il test verde
isolato conferma 202 tabelle schema `pangya` e i seed (testuser/testuser2/newuser, shop,
cadie, IFF character/enchant/card/part…).

**Nota storica:** i doc precedenti citavano ANCHE un secondo fallimento
`GameFlowIT.personalShopBuyIncrementsAchievementCounter`. **Non riprodotto**:
`server-game` è 195/195 verde (il test esiste e passa).

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
| `./gradlew --rerun-tasks --continue :*:test` (8 moduli) | EXIT 1 — 385/386, 1 flake shared-DB |
| `./gradlew --max-workers=1 :core-db:test` (DB isolato) | BUILD SUCCESSFUL, 45/45 |
| `grep addPacketCall GameService.cs` | 479 |
| `grep case CLIENT_ GameHandler.java` | 196 |

---

## 8. Prossimo passo

Fase 0 completa. **Gate:** in attesa di conferma utente per iniziare la Fase 1
(re-scope MVP Torneo). Dettagli e Definition of Done in `docs/EPIC.md`.
