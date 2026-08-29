# STATUS — Pangya Java 21 JP rewrite (MVP Torneo)

**Branch di lavoro:** `cursor/tourney-mvp-plan-0864` (base `Develop`)  
**HEAD base verificato (2026-08-29T15:08Z):** `04591c8` — *Add initial project documentation for Pangya server rewrite*  
**Fonte comportamento:** `reference/pangya-server-community` (`Server/JP/`, branch C# `Develop_luiz`).  
**Scope attuale:** Auth + Login + Game **solo modalità Torneo**, con Ranking/Messenger minimi.  
**Fuori scope MVP:** Versus, Practice, Grand Prix, Grand Zodiac, Guild Battle, Pang Battle, Approach, Chip-in Practice, SSC; manager char/card/caddie oltre il minimo Torneo; load 3000 sessioni.

---

## Riconferma di questo turno (obbligatoria)

Rilanciati i comandi che STATUS precedente marcava verdi / non verificati. **Non** si è ripartiti da assunzioni del turno Fase 0.

### Discrepanze vs `docs/STATUS.md` Fase 0 (HEAD dichiarato `d79b5f6`)

| Dichiarazione Fase 0 | Verifica ora (2026-08-29T15:05–15:08Z) |
|----------------------|----------------------------------------|
| HEAD = `d79b5f6` (merge PR #7) | HEAD = `04591c8`. Dopo `d79b5f6`: `98bcdf0`/`d4eed76` (reconcile doc), `5d20738` (EPIC 1 riga), `a1ff6cc` (`CONTINUATION-PROMPT.md`), `04591c8` (`INITIAL-PROMPT.md`). **Nessun commit di codice Java dopo `d79b5f6`.** |
| Compose `/health` 5 server **non verificato** | **Verificato verde** — vedi sotto |
| `GameFlowIT.personalShopBuyIncrementsAchievementCounter` FAIL (`no server packet`) | **PASS** in questo turno (`--rerun-tasks`) |
| `FlywayMigrationTest` FAIL «second migrate expected 0, was 4» | **FAIL confermato**, ma **non** è l’idempotenza migrate. Lo stack è riga **129**: `iff_item` `assertEquals(0, itemIff)` — expected 0, was 4. Messaggio custom `"second migrate must be a no-op"` **assente** nel failure. Attribuzione Fase 0 **errata**. |

---

## Healthcheck Compose (questo turno)

```
docker compose up -d postgres redis
# i 5 server erano già in volume/immagine snapshot e sono ripartiti healthy

date -u: 2026-08-29T15:05:37Z
auth       http://127.0.0.1:9077/health -> ok auth HTTP 200
login      http://127.0.0.1:9103/health -> ok login HTTP 200
game       http://127.0.0.1:9202/health -> ok game HTTP 200
ranking    http://127.0.0.1:9474/health -> ok ranking HTTP 200
messenger  http://127.0.0.1:9302/health -> ok messenger HTTP 200

compose ps: postgres, redis, auth, login, game, ranking, messenger
            tutti "Up … (healthy)"
```

DoD MVP «`docker compose up` + healthcheck verdi»: **verificato con rebuild** 2026-08-29T15:26:56Z (`docker compose up --build -d`, `COMPOSE_EXIT=0`). Immagini `workspace-{auth,login,game,ranking,messenger}` create in quel minuto. Ranking container: `GeraRankAll rows=26`. Ricontrollo 15:27:19Z ancora 200 × 5.

---

## Gradle — moduli marcati ok in Fase 0 (rilancio reale)

```
./gradlew --no-daemon --rerun-tasks \
  :core-protocol:test :core-network:test \
  :server-auth:test :server-login:test \
  :server-ranking:test :server-messenger:test
BUILD SUCCESSFUL in 17s
28 actionable tasks: 28 executed
EXIT=0
```

(Primo run senza `--rerun-tasks` era `FROM-CACHE` in 6s — **non contato** come evidenza.)

## Gradle — Flyway (non verde)

```
./gradlew --no-daemon :core-db:test --tests org.pangya.db.FlywayMigrationTest --rerun-tasks
FlywayMigrationTest > migratesAndIsIdempotent() FAILED
    org.opentest4j.AssertionFailedError: expected: <0> but was: <4>
        at FlywayMigrationTest.migratesAndIsIdempotent(FlywayMigrationTest.java:129)
BUILD FAILED
FLYWAY_EXIT=1
```

Riga 129 = `assertEquals(0, itemIff)` su `pangya.iff_item`.  
Query live (stesso Postgres Compose):

```
docker exec workspace-postgres-1 psql -U pangya -d pangya \
  -c "select typeid from pangya.iff_item;"
 436208228
 436207927
 436207964
 436207622
```

`JdbiInventoryRepository` fa `INSERT INTO pangya.iff_item` a runtime. Il test **non DROPPA** lo schema (commento in classe). Le 4 righe sono inquinamento da IT precedenti, non 4 migration Flyway riapplicate. **Non è un fix Torneo** — lasciato aperto, non “migliorato” in questo turno.

## Gradle — GameFlowIT rilevanti Torneo / Practice / shop

```
./gradlew --no-daemon :server-game:test --rerun-tasks \
  --tests …twoPlayersStartTourneyAndReceiveCourse \
  --tests …tourneyReplaySendsRemainingToSender \
  --tests …tourneyTicketReportSendsNewItemAndLeavesGuestInGame \
  --tests …fakeClientLogsInEntersChannelCreatesAndLeavesPractice \
  --tests …personalShopBuyIncrementsAchievementCounter
BUILD SUCCESSFUL in 18s
12 actionable tasks: 12 executed
GAME_IT_EXIT=0
  personalShopBuyIncrementsAchievementCounter          PASSED
  twoPlayersStartTourneyAndReceiveCourse               PASSED
  tourneyReplaySendsRemainingToSender                  PASSED
  fakeClientLogsInEntersChannelCreatesAndLeavesPractice PASSED
  tourneyTicketReportSendsNewItemAndLeavesGuestInGame  PASSED
```

`twoPlayersStartTourneyAndReceiveCourse` arriva a `SERVER_GAME_INIT` + `SERVER_COURSE`. **Non** gioca hole, **non** finish, **non** ranking.

---

## Progresso slice MVP Torneo

| Slice | Stato | Evidenza questo turno |
|-------|-------|------------------------|
| **S-T1** protocollo/cipher | [~] | Tabelle `CryptoOracle` 4096+4096 **identiche** al C# JP (verifica Python 2026-08-29). Test prefix 32 byte C# PASSED. `CipherTest` resta roundtrip sintetico — **nessun ciphertext di rete**. |
| **S-T2** Auth + Login | [x] | `:server-auth:test` + `:server-login:test` EXIT=0. `LoginFlowIT.fakeClientLoginReceivesServerListAndCanSelectGs` riceve `SERVER_AUTH_KEY_LOGIN` 8 char = Redis `getLoginKey(10001)`. |
| **S-T3** canale + iscrizione Torneo | [x] | Coperta da `twoPlayersStartTourneyAndReceiveCourse` + IT S-T4 (stesso path create/join/start). Entrambi PASSED `--rerun-tasks`. |
| **S-T4** partita Torneo e2e | [x] | `tourneyFakeClientPlaysToFinishAndReceivesResult` PASSED (vedi comando sotto). `requestSaveInfo(..., 0)` wired per `TIPO_TOURNEY`. |
| **S-T5** ranking/messenger minimi | [x] | `RankingRuntime` chiama `GeraRankAll` all’avvio. E2e unico: Tourney finish → ranking fake-client BL board (`tourneyFinishRebuildsRankingRegistry` PASSED). Messenger: nessun hook in C# `Tourney.cs`. |
| **S-T6** hardening leggero | [x] | Esistente `SessionIsolationTest.throwingHandlerDoesNotKillServer` PASSED `--rerun-tasks` (due client, handler throw, server resta bound). `SessionLoadIT` 3000 = **fuori scope**, non rieseguito. |

### Presente, fuori scope MVP, non validato

Codice/test già in repo (compila / alcuni IT verdi). **Non** marcati fatti per il MVP:

- Practice e2e (`fakeClientLogsInEntersChannelCreatesAndLeavesPractice` e varianti last-hole)
- Grand Prix parity PR #6–#7 (timer, placar, `requestSaveInfo`, `leaveRoom`)
- Versus / Match / GZ / ticket-report / personal shop / FriendManager
- `SessionLoadIT` 3000 + `/metrics`

---

## DoD MVP Torneo (checklist)

- [x] Compose healthcheck verdi dopo `docker compose up --build -d` (2026-08-29T15:26:56Z; ricontrollo 15:27:19Z ancora 200)
- [~] Protocollo framing + cipher su fixture C# (tabelle oracle 4096 identiche al C#; ciphertext di rete **assente**)
- [x] Login e2e fake client + session key
- [x] Fake client: canale → iscrizione Torneo → partita fino alla fine → risultato + ranking fake-client BL board (`RankingRuntime` auto-GeraRankAll)
- [x] Crash sessione non abbatte il processo — `SessionIsolationTest` PASSED
- [x] EPIC.md + STATUS.md aggiornati in questo turno

### S-T4 comando (incollato)

```
./gradlew --no-daemon :server-game:test --rerun-tasks \
  --tests org.pangya.game.GameFlowIT.tourneyFakeClientPlaysToFinishAndReceivesResult
GameFlowIT > tourneyFakeClientPlaysToFinishAndReceivesResult() PASSED
BUILD SUCCESSFUL in 11s
12 actionable tasks: 12 executed
ST4_EXIT=0
```

### S-T4 + S-T6 regressione (incollato)

```
./gradlew --no-daemon --rerun-tasks \
  :core-network:test --tests org.pangya.network.netty.SessionIsolationTest \
  :server-game:test --tests …tourneyFakeClientPlaysToFinishAndReceivesResult \
    --tests …twoPlayersStartTourneyAndReceiveCourse \
    --tests …tourneyReplaySendsRemainingToSender \
    --tests …tourneyTicketReportSendsNewItemAndLeavesGuestInGame \
    --tests …soloGrandPrixSendsTourneyInit
SessionIsolationTest > throwingHandlerDoesNotKillServer() PASSED
GameFlowIT > soloGrandPrixSendsTourneyInit() PASSED
GameFlowIT > twoPlayersStartTourneyAndReceiveCourse() PASSED
GameFlowIT > tourneyReplaySendsRemainingToSender() PASSED
GameFlowIT > tourneyFakeClientPlaysToFinishAndReceivesResult() PASSED
GameFlowIT > tourneyTicketReportSendsNewItemAndLeavesGuestInGame() PASSED
BUILD SUCCESSFUL in 18s
REG_EXIT=0
```

---

### S-T5 comando (incollato, 2026-08-29T15:20Z)

```
./gradlew --no-daemon --rerun-tasks \
  :core-protocol:test --tests org.pangya.protocol.crypto.CryptoOracleTest \
  :core-db:test --tests org.pangya.db.RankRepositoryTest \
  :server-ranking:test --tests …RankingFlowIT.geraRankAllFirstPageIsServedToFakeClient \
  :server-game:test --tests …GameFlowIT.tourneyFinishRebuildsRankingRegistry
CryptoOracleTest > tablesMatchCSharpLengthsAndKnownPrefix() PASSED
RankRepositoryTest > geraRankAllWritesLevelBoardForEligibleAccounts() PASSED
RankingFlowIT > geraRankAllFirstPageIsServedToFakeClient() PASSED
GameFlowIT > tourneyFinishRebuildsRankingRegistry() PASSED
BUILD SUCCESSFUL in 12s
20 actionable tasks: 20 executed
FINAL_EXIT=0
```

---

### S-T5 + RankingRuntime + compose --build (incollato, 2026-08-29T15:25–15:27Z)

```
./gradlew --no-daemon --rerun-tasks \
  :server-ranking:test --tests org.pangya.ranking.RankingFlowIT \
  :core-db:test --tests org.pangya.db.RankRepositoryTest \
  :server-game:test --tests org.pangya.game.GameFlowIT.tourneyFinishRebuildsRankingRegistry
RankingFlowIT > registryPageAndPlayerFullInfoComeFromSql() PASSED
RankingFlowIT > geraRankAllFirstPageIsServedToFakeClient() PASSED
RankRepositoryTest > geraRankAllWritesLevelBoardForEligibleAccounts() PASSED
GameFlowIT > tourneyFinishRebuildsRankingRegistry() PASSED
BUILD SUCCESSFUL in 17s
19 actionable tasks: 19 executed
FINAL_EXIT=0
```

```
docker compose up --build -d
COMPOSE_EXIT=0
date-u: 2026-08-29T15:26:56Z
auth       http://127.0.0.1:9077/health -> ok auth HTTP 200
login      http://127.0.0.1:9103/health -> ok login HTTP 200
game       http://127.0.0.1:9202/health -> ok game HTTP 200
ranking    http://127.0.0.1:9474/health -> ok ranking HTTP 200
messenger  http://127.0.0.1:9302/health -> ok messenger HTTP 200
compose ps: 7/7 Up … (healthy)
ranking log: GeraRankAll rows=26 (C# CmdUpdateRankRegistry / init_systems)
```

---

## Questo turno

| Campo | Valore |
|-------|--------|
| Data/ora | 2026-08-29T15:27Z |
| Fatto | `RankingRuntime` GeraRankAll all’avvio (C# `init_systems`); e2e Tourney finish → ranking fake-client BL; `docker compose up --build` 7/7 healthy |
| Commit codice | `a3b020a` feat: run GeraRankAll on RankingRuntime start |
| Residuo | Ciphertext golden di rete (gate capture). Flyway `iff_item` invariato. |
| Blocker | Ciphertext di rete assente. Non inventare. |

---

## Gate (fermarsi e attendere umano)

Nessuno aperto. Segnalazioni (non decidere da soli):

- `FlywayMigrationTest` vs `iff_item` runtime insert: lavoro pregresso / isolamento test, **non coperto** come “migrate idempotent” dalla Fase 0.
- Cipher: nessun file golden ciphertext in repo — se si vuole DoD stretto «fixture reali», serve capture; le tabelle C# sono ricostruibili.
