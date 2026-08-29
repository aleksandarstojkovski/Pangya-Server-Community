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
- [ ] Fake client: canale → iscrizione Torneo → partita fino alla fine → risultato e ranking
- [ ] Crash di sessione non abbatte il processo (test esplicito)
- [x] Questo file + `docs/STATUS.md` aggiornati a fine turno

---

## Piano slice (solo MVP Torneo)

```
S-T1 [~]  S-T2 [x]  S-T3 [~]  S-T4 [ ]  S-T5 [~]  S-T6 [ ]
```

### S-T1 — Protocollo / cipher

**Riuso Fase 0 + riconferma questo turno.** Non rifare.

```
./gradlew --no-daemon --rerun-tasks :core-protocol:test :core-network:test
BUILD SUCCESSFUL in 17s
28 actionable tasks: 28 executed   # (insieme ad auth/login/ranking/messenger)
EXIT=0
```

- `CryptoOracleTest.tablesMatchCSharpLengthsAndKnownPrefix` — 4096+4096 e primi byte da `Server/JP/.../CryptoOracle.cs`.
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

**Parziale — riuso test esistente, non chiuso come DoD e2e.**

```
./gradlew --no-daemon :server-game:test --rerun-tasks \
  --tests org.pangya.game.GameFlowIT.twoPlayersStartTourneyAndReceiveCourse
… twoPlayersStartTourneyAndReceiveCourse PASSED
GAME_IT_EXIT=0
```

Copre: `loginToChannel` (`clientEnterChannel`) → `clientCreateRoom(TIPO_TOURNEY)` → `clientJoinRoom` → `clientStartGame` → `SERVER_GAME_INIT` / `SERVER_COURSE` tipo Torneo.

Non copre: hole, finish, ranking. Non aggiungere opcode nuovi qui.

### S-T4 — Partita Torneo end-to-end  ← **prossima**

**Stato:** non verificato. Nessun IT fake-client che, su `TIPO_TOURNEY`, faccia init-hole → shot hole-out → `requestFinishGame` → pacchetti risultato.

Riferimento C# (cercato, non a memoria):

- `Server/JP/GameServer/Game/GameModes/Tourney.cs`
  - `checkEndShotOfHole`: ultimo hole → `0x199`, `finishHole`, `changeHole`
  - `requestFinishData`: drop / placar / treasure hunter
  - `finish_game(..., 6)`: `requestSaveRecordCourse`, **`requestSaveInfo(_session, 0)`** (~riga 1552), exp, `0x244`/`0x24F`/`0xC8`
- Java `GameHandler.finishGamePlayerDump`: `requestSaveInfoFinish` solo se `TIPO_GRAND_PRIX`; Torneo va a `sendFinishGameDump` **senza** `requestSaveInfo`.

Criterio di chiusura S-T4 (comando da incollare, non riassumere):

```
./gradlew --no-daemon :server-game:test --rerun-tasks \
  --tests org.pangya.game.GameFlowIT.<nome_it_torneo_e2e>
# EXIT=0 e assert: result + (user_info persistita se C# lo richiede)
```

Practice last-hole (`clientInitHole(18,…)`) è **fuori scope** come evidenza Torneo; si può copiare il *meccanismo* solo dopo aver verificato in C#/Java che `TIPO_TOURNEY` accetta lo stesso init-hole.

`calcule_shot_to_coin` / `CoinCubeLocationUpdateSystem`: C# `Tourney.requestCalculeShotCoin` li chiama; Java `syncShot` grep 0 match. **Non** è nel DoD «partita fino alla fine + risultato». Non portarli in S-T4 se il finish e2e non li richiede.

### S-T5 — Ranking + Messenger minimi

**Parziale.**

```
:server-ranking:test :server-messenger:test
BUILD SUCCESSFUL … EXIT=0  (--rerun-tasks, questo turno)
```

- Ranking serve `pangya_rank_atual` (`JdbiRankRepository`, C# `CmdRankRegistryInfo` / `RankRegistryManager.initialize`).
- Grep C# JP GameServer: **nessun** write a `pangya_rank_atual` nel finish Torneo. Il registry è un load SQL del RankingServer.
- Minimo Torneo da verificare in S-T5 (dopo S-T4):
  1. Pacchetto placar / `SERVER_GAME_RESULT` al finish (in-game).
  2. `user_info` persistita (`requestSaveInfo` option 0) come C# `Tourney.finish_game`.
  3. Messenger: solo se il flusso Torneo C# lo tocca (invite/presence). Non auditare guild.

Non dichiarare «ranking aggiornato» se si è solo letto una riga seedata a mano in `pangya_rank_atual` (come fa già `RankingFlowIT.registryPageAndPlayerFullInfoComeFromSql`).

### S-T6 — Hardening leggero

**Non iniziato.** Test esplicito: una sessione che lancia / si disconnette in modo ostile **non** termina la JVM del Game server.  
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

**S-T4:** un IT fake-client Torneo che gioca fino al finish e asserisce i pacchetti risultato. Se `user_info` non cambia, allineare `finishGamePlayerDump` a C# `requestSaveInfo(_session, 0)` **solo** per Torneo, con test che lo dimostra.

Nessun nuovo opcode. Nessun lavoro Practice/GP/Versus.

---

## Gate (fermarsi)

- Byte protocollo Torneo non ricostruibili dal C# (serve capture)
- Client Pangya binario mancante
- Cambio stack
- Semantica SQL che cambia il gameplay Torneo
- Altro codice pregresso dubbio non coperto da Fase 0 / da questa riconferma — segnalare, non decidere
