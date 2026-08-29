# EPIC — MVP Torneo (Java 21 JP)

**Obiettivo ridotto:** Auth + Login + Game Server end-to-end **solo modalità Torneo** (`TIPO_TOURNEY`), con Ranking e Messenger nella misura minima richiesta da quel flusso.

**Fonte comportamento:** `reference/pangya-server-community` (`Server/JP/`, C# branch `Develop_luiz`).  
**Codice Java:** `Develop` @ `04591c8` (ultimo commit Java: `d79b5f6` merge PR #7 GP).  
**Non ricostruire** S0–S3 / S-T1–S-T2 già verificati verdi in questo turno.

---

## Fuori scope (non implementare, non “migliorare”)

- Versus, Practice, Grand Prix, Grand Zodiac, Guild Battle, Pang Battle, Approach, Chip-in Practice, Special Shuffle Course
- CharacterManager / CardManager / CaddieManager oltre il minimo per entrare e finire un Torneo
- Test di carico 3000 sessioni (`SessionLoadIT`) — fase futura

### Presente, fuori scope MVP, non validato

Lasciato nel repo. **Non** è “fatto” solo perché compila o perché un IT di un’altra modalità è verde.

| Area | Dove | Nota |
|------|------|------|
| Practice e2e | `GameFlowIT.fakeClientLogsInEntersChannelCreatesAndLeavesPractice` | PASSED 2026-08-29; **non** è Torneo |
| GP finish/exit/timer/placar/`requestSaveInfo` | PR #6–#7, `GameHandler` GP branches | Mergeato; **non** validato come Torneo |
| Versus / Match / GZ / shop / ticket-report | `GameHandler` + IT sparsi | Presente; fuori scope |
| Load 3000 + `/metrics` | `SessionLoadIT` | Fuori scope; **non rieseguito** questo turno |

---

## Definition of Done

- [x] `docker compose` Postgres + Redis + Auth + Login + Game + Ranking + Messenger healthcheck verdi — **verificato 2026-08-29T15:05:37Z** (curl `/health` → `ok … HTTP 200` × 5; `compose ps` 7/7 healthy). Rebuild `--build` **non eseguito**.
- [~] Protocollo framing + cipher su fixture C# — tabelle `CryptoOracle` prefix C# + test verdi; ciphertext golden **assente** (vedi S-T1).
- [x] Login e2e fake client, session key ricevuta — `LoginFlowIT` in `:server-login:test` EXIT=0 `--rerun-tasks`.
- [x] Fake client: canale → iscrizione Torneo → partita fino alla fine → `SERVER_GAME_RESULT` + `GeraRankAll` registry (`tourneyFinishRebuildsRankingRegistry` + ranking fake-client EXIT=0)
- [x] Crash di sessione non abbatte il processo — `SessionIsolationTest.throwingHandlerDoesNotKillServer` PASSED
- [x] Questo file + `docs/STATUS.md` aggiornati a fine turno

---

## Piano slice (solo MVP Torneo)

```
S-T1 [~]  S-T2 [x]  S-T3 [x]  S-T4 [x]  S-T5 [x]  S-T6 [x]
```

### S-T1 — Protocollo / cipher

**Riuso Fase 0 + riconferma questo turno.** Non rifare.

```
./gradlew --no-daemon --rerun-tasks :core-protocol:test :core-network:test
BUILD SUCCESSFUL in 17s
28 actionable tasks: 28 executed   # (insieme ad auth/login/ranking/messenger)
EXIT=0
```

- `CryptoOracleTest.tablesMatchCSharpLengthsAndKnownPrefix` — 4096+4096; primi **32** byte copiati da `Server/JP/.../CryptoOracle.cs`. Verifica Python: bin Java **identici** agli array C# (4096+4096).
- `CipherTest` — roundtrip encrypt/decrypt su plaintext **sintetico**, non capture di rete.
- Framing Netty in `:core-network:test` — verde.

**Non verificato:** fixture ciphertext/plaintext catturate da client JP S9 (0 file golden in repo).  
Se serve DoD stretto «golden bytes di rete», **gate capture** — non inventare byte.

### S-T2 — Auth + Login

**Chiuso** (riconferma `--rerun-tasks`).

```
:server-auth:test :server-login:test
BUILD SUCCESSFUL … EXIT=0
```

`LoginFlowIT.fakeClientLoginReceivesServerListAndCanSelectGs`: `SERVER_AUTH_KEY_LOGIN` length 8 == Redis `keys.getLoginKey(10001)`.

### S-T3 — Canale + iscrizione Torneo

**Chiuso** (riuso + S-T4). Stesso path in entrambi gli IT PASSED.

```
GameFlowIT > twoPlayersStartTourneyAndReceiveCourse() PASSED
GameFlowIT > tourneyFakeClientPlaysToFinishAndReceivesResult() PASSED
```

### S-T4 — Partita Torneo end-to-end

**Chiuso** questo turno.

C# `Tourney.finish_game` option 6 (`Tourney.cs` ~1552): `requestSaveInfo(_session, 0)`.  
Java: `GameHandler.finishGamePlayerDump` ora chiama `requestSaveInfo(session, room, 0, false)` se `TIPO_TOURNEY`.

IT: 1-hole (`clientChangeRoomHoles`) → `clientInitHole(1)` → hole-out `DISPLAY_ACERTO_HOLE` → `0x199` → `clientFinishGame` → `SERVER_PRIZE_LIST` / `SERVER_GAME_RESULT` / `SERVER_MY_STATISTICS` + `Jogado`/`JogosNaoSei` +1.

```
./gradlew --no-daemon :server-game:test --rerun-tasks \
  --tests org.pangya.game.GameFlowIT.tourneyFakeClientPlaysToFinishAndReceivesResult
GameFlowIT > tourneyFakeClientPlaysToFinishAndReceivesResult() PASSED
BUILD SUCCESSFUL in 11s
12 actionable tasks: 12 executed
ST4_EXIT=0
```

`calcule_shot_to_coin` / `CoinCubeLocationUpdateSystem`: ancora 0 match in Java. **Non** richiesto dal DoD finish+result. Presente in C# `Tourney.requestCalculeShotCoin` — **presente C#, non portato, fuori criterio S-T4**.

### S-T5 — Ranking + Messenger minimi

**Chiuso** (minimo Torneo).

C# path: `Tourney.finish_game` → `requestSaveRecordCourse` → Ranking `CmdUpdateRankRegistry` (`pangya.GeraRankAll`) → `ProcGetRankRegistryInfo`.

Java: `saveRecordCourse` per `TIPO_TOURNEY` (option 1 se 18 hole last-hole); `RankRepository.geraRankAll()` porta le board user_info + score + per-course. **Non** chiamato automaticamente da `RankingRuntime` (C# lo fa in `init_systems`) — invocato esplicitamente dagli IT.

```
GameFlowIT > tourneyFinishRebuildsRankingRegistry() PASSED
RankingFlowIT > geraRankAllFirstPageIsServedToFakeClient() PASSED
RankRepositoryTest > geraRankAllWritesLevelBoardForEligibleAccounts() PASSED
FINAL_EXIT=0
```

### S-T6 — Hardening leggero

**Chiuso** (test già in repo, riconfermato).

```
./gradlew --no-daemon --rerun-tasks \
  :core-network:test --tests org.pangya.network.netty.SessionIsolationTest
SessionIsolationTest > throwingHandlerDoesNotKillServer() PASSED
```

Due client, sink che lancia `RuntimeException`, `seen==2`, server ancora bound. Catch in `PangyaClientDecryptHandler` virtual thread.  
`SessionLoadIT` 3000 = fuori scope.

---

## Verifica comandi di questo turno (incollati)

Health (2026-08-29T15:05:37Z):

```
auth http://127.0.0.1:9077/health -> ok auth HTTP 200
login http://127.0.0.1:9103/health -> ok login HTTP 200
game http://127.0.0.1:9202/health -> ok game HTTP 200
ranking http://127.0.0.1:9474/health -> ok ranking HTTP 200
messenger http://127.0.0.1:9302/health -> ok messenger HTTP 200
```

Moduli S-T1/S-T2/S-T5 base:

```
./gradlew --no-daemon --rerun-tasks \
  :core-protocol:test :core-network:test \
  :server-auth:test :server-login:test \
  :server-ranking:test :server-messenger:test
BUILD SUCCESSFUL in 17s
28 actionable tasks: 28 executed
EXIT=0
```

Flyway (non nel path critico Torneo; evidenza onesta):

```
FlywayMigrationTest > migratesAndIsIdempotent() FAILED
    expected: <0> but was: <4>
    FlywayMigrationTest.java:129   # iff_item count, NON second migrate
FLYWAY_EXIT=1
```

---

## Mappa C# → Java (invariata)

| C# (JP) | Java |
|---------|------|
| `PangyaAPI.Network.Cryptor.Cipher` | `org.pangya.protocol.crypto.Cipher` |
| `Tourney` / `TourneyBase` | rami `TIPO_TOURNEY` / `usesTourneyInitialData` in `GameHandler` (non classe omonima) |
| `AuthServer` / `LoginServer` / `GameServer` / `RankingServer` / `MessengerServer` | `:server-auth` … `:server-messenger` |

502 stored procedure C# non portate — SQL Jdbi esplicito.

---

## Prossima azione

Residui onesti:

- **S-T1:** ciphertext golden di rete assente — **gate capture**.
- `RankingRuntime` non chiama `GeraRankAll` all’avvio (C# `init_systems` sì) — gli IT lo invocano esplicitamente.
- `FlywayMigrationTest` riga 129 `iff_item` = inquinamento IT.

Nessun nuovo opcode. Nessun lavoro Practice/GP/Versus.

---

## Gate (fermarsi)

- Byte protocollo Torneo non ricostruibili dal C# (serve capture)
- Client Pangya binario mancante
- Cambio stack
- Semantica SQL che cambia il gameplay Torneo
- Altro codice pregresso dubbio non coperto da Fase 0 / da questa riconferma — segnalare, non decidere
