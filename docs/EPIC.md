# EPIC — Pangya Java rewrite (C# JP → Java 21)

**Aggiornato:** 2026-08-29 12:57 UTC · **HEAD:** `5d20738` (`Develop`)
**Fonte comportamento:** `reference/pangya-server-community` (`Server/JP/`, branch C# `Develop`).

> Riscritto da zero nell'audit di recovery (Fase 0). Contiene **solo** ciò che è stato
> verificato con un comando. Le voci "done" hanno accanto comando + esito reale; il resto
> è marcato **non verificato**. Stato dettagliato e output incollati: `docs/STATUS.md`.

---

## Re-scope: MVP = modalità **Torneo (Tourney)**

L'obiettivo è ristretto a un flusso end-to-end **solo per il Torneo**:

**Auth + Login + Game funzionanti end-to-end per la modalità Tourney, con Ranking e
Messenger nella misura minima richiesta dal flusso Torneo.**

### Fuori scope MVP (NON implementare ora — parcheggiati, da validare dopo)
Versus, Practice, Grand Prix (GP), Grand Zodiac, Guild Battle, Pang Battle, Approach,
Chip-in Practice, Special Shuffle Course; manager Character/Card/Caddie non necessari al
Torneo; load test 3000 sessioni (ex-S6).

> **Codice già presente per queste modalità (specialmente GP): NON buttato.** Gran parte
> del lavoro dell'ultimo giorno è GP (PR #6/#7). Resta nel repo come *"già presente, fuori
> scope MVP, da validare"* — non marcato fatto perché non testato nel flusso MVP.

---

## Definition of Done (MVP Torneo) — stato verificato

| # | Criterio | Stato | Evidenza (comando → esito) |
|---|----------|-------|----------------------------|
| 1 | `docker compose up` → PG, Redis, Auth, Login, Game, Ranking, Messenger healthy | ✅ **VERIFICATO** | `docker compose ps` = 7/7 healthy; `curl :{9077,9103,9202,9474,9302}/health` = 5×`ok` |
| 2 | Framing + cipher verdi su fixture | ⚠️ **PARZIALE** | `:core-protocol:test` 103/103 PASS; ma cipher usa vettori **sintetici/strutturali**, non capture C# S9 reali (0 capture nel repo). IFF su archivio reale (20 file test). |
| 3 | Login end-to-end con fake client | ✅ **VERIFICATO** | `:server-login:test` 7/7; `GameFlowIT.fakeClientLogsIn…` + `loginTwoPlayers(...)` verdi in `:server-game:test` |
| 4 | Fake client: entra canale → iscrive Torneo → **gioca fino a fine** → riceve **risultato/ranking** | ⚠️ **PARZIALE** | START coperto: `GameFlowIT.twoPlayersStartTourneyAndReceiveCourse` (crea `TIPO_TOURNEY`, join, start, `GAME_INIT`+`COURSE`) PASS. **Finish+ranking Torneo: NON verificato** (nessun IT porta un Torneo alla fine con assert su ranking). |
| 5 | Nessun crash di sessione abbatte il processo | ❓ **NON VERIFICATO** | da coprire con test/handler try-catch dedicato in Fase 1 |
| 6 | `EPIC.md`/`STATUS.md` riflettono stato reale a fine di ogni turno | ✅ in corso | questo file + `STATUS.md` riscritti con comandi |

---

## Stato per modulo (VERIFICATO — vedi STATUS §2)

```
core-protocol 103 PASS · core-network 9 PASS · core-db 45 (1 flake shared-DB, verde isolato)
server-auth 1 PASS · server-login 7 PASS · server-game 195 PASS
server-ranking 6 PASS · server-messenger 20 PASS      → 386 test, 385 verdi
```

Unico rosso: `FlywayMigrationTest.migratesAndIsIdempotent` — flakiness da DB condiviso in
parallelo, **non** bug di schema (passa isolato). Dettaglio e prova: `STATUS.md` §3.

---

## Copertura Torneo — cosa esiste davvero (VERIFICATO)

- **C# di riferimento presente:** `Game/Base/TourneyBase.cs`, `Game/GameModes/Tourney.cs`,
  `Models/tourney_base_type.cs` (`find … -iname '*tourney*'`).
- **Java:** `GameHandler` gestisce Tourney in molti punti citando il C#
  (`usesTourneyInitialData`, `TourneyBase.sendInitialData`/`checkEndShotOfHole`/
  `finish_tourney`, `tourneyTimeIsOver`, ticket report). `TIPO_TOURNEY` in `GamePackets`.
- **Test:** `twoPlayersStartTourneyAndReceiveCourse` (start ✅), `tourneyReplaySendsRemainingToSender`,
  `tourneyTicketReportSendsNewItemAndLeavesGuestInGame`.
- **Lacuna MVP:** manca un IT che giochi il Torneo **hole-by-hole fino al finish** e
  asserisca `SERVER_GAME_RESULT`/ranking aggiornato. Gli assert finish/`gameResult`
  esistenti sono in contesto stroke/versus, non Torneo.

---

## Inventario opcode (CORREZIONE vs doc vecchi)

| Metrica | Valore reale | Comando |
|---|---|---|
| C# `GameService.cs` `addPacketCall` | **479** | `grep -cE addPacketCall .../GameService.cs` |
| Java `GameHandler` `case CLIENT_*` | **196** | `grep -oE 'case GamePackets.CLIENT_' … \| sort -u \| wc -l` |

Il vecchio "193 C# / 0 mancanti" era **errato**: Java implementa un **sottoinsieme** della
superficie C#. Per l'MVP conta solo il sottoinsieme necessario al Torneo, non la parità totale.

---

## Ambiente Cloud Agent

Docker non è nell'immagine base; installato nested nella VM (snapshot):
`fuse-overlayfs` + `iptables-legacy`, `dockerd` lanciato via `setsid` (helper
`/usr/local/bin/dockerd-nested.sh`). `install` = clone `reference/` + `./gradlew assemble
installDist` + `docker compose build`. `start` = dockerd + `docker compose up -d --build` +
attesa health. Test dipendono da: Postgres+Redis up e clone `reference/` presente.

---

## Mappa C# → Java (verificata presente nel reference)

| C# (JP) | Java |
|---------|------|
| `PangyaAPI.Network.Cryptor.{Cipher,CryptoOracle,MiniLzo}` | `org.pangya.protocol.crypto.*` |
| `PangyaAPI.SQL` + stored proc | `org.pangya.db` (Jdbi + SQL esplicito, no JPA) |
| `Auth/Login/Game/Ranking/MessengerServer` | `:server-{auth,login,game,ranking,messenger}` |
| `Game/Base/TourneyBase`, `Game/GameModes/Tourney` | logica Tourney in `server-game/GameHandler` |
| `server.ini` | `application.yml` + env |

---

## Fase 1 — mappa finish/ranking Torneo (read-only, VERIFICATO)

Ricerca preparatoria (nessuna modifica di codice) per de-rischiare la Fase 1.

**Flusso C# di riferimento** (`grep -niE 'finish_tourney|finish_game|placar|rank' Tourney.cs TourneyBase.cs`):
- `Tourney.finish_tourney(session, option)` (`Tourney.cs:277`) → `finish_game(session, 1)` (`:417`)
  + `requestCalculeRankPlace()` (`:620`) + `requestFinishExpGame()` (`:528/:626`).
- `TourneyBase.sendPlacar` (`:2408`), `getRankPlace` (`:2574`), `requestFinishGame` → `finish_game(session, 6)` (`:2064`).
- Progressione hole: `requestFinishLoadHole` / `requestFinishCharIntro` / `requestFinishHoleData` /
  `requestFinishShot` / `requestFinishHole`.

**Java attuale — cosa c'è / cosa manca:**
- ✅ Risultato Torneo emesso: `SERVER_GAME_RESULT` in `GameHandler` a 2 siti (`grep gameResult(` → righe **3700**, **4018**); la **4018** è il path last-hole `TIPO_TOURNEY` (`GameHandler:4044 if (room.tipo != TIPO_TOURNEY)`).
- ⚠️ Path finish/placar ramifica in modo speciale **solo** su `TIPO_GRAND_PRIX` (`sendGrandPrixFinishDump`); Torneo passa dal path generico (`finishGameExp`/`finishGamePlayerDump`/`sendFinishGameDump`) — **parità Tourney finish non verificata**.
- ❌ **Nessun wiring game→Ranking**: `grep -rniE 'ranking|pangya_rank|rank_config|updateRank|:4774' server-game/src/main` (escludendo `placar/rankIndex` locali) → **0 match**. Il finish partita NON aggiorna il Ranking server / SQL ranking.

> Ambiguità DoD #4 "ranking aggiornato": può significare (i) il **placar/rank in-partita** a fine
> match (in parte presente) oppure (ii) il **Ranking server** globale (porta 4774) — **non wired**.
> Da chiarire con l'utente in Fase 1; assumo entrambi finché non confermato (gate: SQL ranking).

## Piano Fase 1 (dopo conferma utente)

1. **Rendere verde `./gradlew test` in modo deterministico** — de-flakare
   `FlywayMigrationTest` (DB dedicato/DROP, o escluderlo dal run parallelo condiviso).
2. **IT Torneo end-to-end fino al finish + ranking** (DoD #4): entra canale → iscrive
   Torneo → gioca gli hole → `finish` (path last-hole `TIPO_TOURNEY`, `GameHandler:4018`) →
   assert `SERVER_GAME_RESULT` + placar; verificare/aggiungere aggiornamento Ranking.
3. **Robustezza sessione** (DoD #5): una eccezione in un handler non deve abbattere il processo.
4. **Ranking/Messenger** solo quanto serve al flusso Torneo: wiring game→ranking a fine partita
   (oggi assente, 0 match sopra).

---

## Gate (fermarsi e chiedere)

- Byte di protocollo non ricostruibili dal C#.
- Client Pangya binario mancante nell'env (capture S9 = 0).
- Cambio di stack.
- Semantica SQL ambigua che cambia il gameplay.
- **Rollback:** valutato in Fase 0. **Raccomandazione: NIENTE rollback** — il codice è
  sano (385/386 test verdi, IT reali, ancorato al reference); il problema era nei doc, ora
  riscritti. Decisione irreversibile → confermare comunque prima di eventuale rollback.

---

## Regola anti-allucinazione (per tutto il lavoro futuro)

- Ogni riga "done" qui deve avere il comando che l'ha verificata + esito reale (incollato).
- Se un comando non è eseguibile/verificabile → scrivere **non verificato**, mai "fatto".
- Fine di ogni turno: aggiornare `STATUS.md` con data/ora e stato dei comandi chiave (un
  turno che non aggiorna `STATUS.md` non è concluso).
- Prima di scrivere qualcosa non verificato (API C# a memoria, schema SQL, opcode):
  fermarsi e cercarlo in `reference/`.
