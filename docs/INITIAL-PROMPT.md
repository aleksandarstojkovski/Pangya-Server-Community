Sei un Cloud Agent. Riscrivi da zero il Pangya Game Server in Java 21+, bit-compatibile col client esistente. Lavora in autonomia per giorni a turni: implementa, testa, apri/aggiorna PR, fermati solo ai gate. Non chiedere conferma per slice ovvie. Chiedi solo se un gate è ambiguo o se stai per fare una scelta irreversibile (breaking del protocollo client, dump di secret, cambiare lo stack sotto).

## Obiettivo
Portare un emulatore server Pangya (oggi C# / .NET Framework 4.8, 5 processi, SQL Server/ODBC, Windows-only) a uno stack Linux/Docker-native in Java, con la stessa logica di dominio (protocollo binario, sessioni, regole di gioco, schema dati semanticamente equivalente).

Sorgente di verità del comportamento: clone read-only di
https://github.com/luismk/Pangya-Server-Community
branch `Develop`.
Questo repo (workspace cloud) è SOLO la riscrittura Java. Non patchare il C# se non per copiare fixture/commenti nel Java.

## Fuori scope (non fare)
- Client di gioco: non toccarlo, non “migliorare” il protocollo verso il client.
- Cambiare formato/cifratura dei pacchetti client↔server: deve restare bit-compatibile (equivalente a Cipher.ServerEncrypt e framing attuali).
- Scalabilità orizzontale del Game Server con stato match distribuito: il match resta in-process.
- Migrazione dati storici da SQL Server di produzione: si parte da DB PostgreSQL VUOTO. Solo schema equivalente + seed minimi di dev.
- JPA/Hibernate, Kubernetes in questa fase, UI admin, rewrite “carina” non richiesta dal C#.
- Dipendenze Windows (kernel32, dbghelp, Console.Title, DSN ODBC).
- Multi-versione protocollo: una sola, quella del server C# Develop / Season 9.

## Decisioni già prese (non riaprire)
- Java 21+, Netty, virtual thread per lavoro bloccante di dominio, I/O solo su event loop Netty.
- PostgreSQL + Flyway; accesso dati JDBI o MyBatis (SQL espliciti).
- Redis per session lookup cross-server, ban list, cache ranking.
- Gradle multi-modulo: `:core-network`, `:core-protocol`, `:core-db` + un modulo eseguibile per Auth, Login, Game, Ranking, Messenger.
- Config: application.yml + env vars. Log: SLF4J/Logback JSON su stdout. Metriche: Micrometer + OTel (connessioni, pkt/s, latenza dispatch).
- Docker multi-stage Temurin 21 JRE; `docker compose up` = 5 server + Postgres + Redis.
- Comunicazione inter-server: TCP dedicato verso Auth, reconnect automatico (come unit_auth_server_connect).
- Anti-bot/DDoS: logica equivalente a IpDdosFilter.
- Target carico (Fase 6): ≥ 3000 sessioni concorrenti sul Game Server in test sintetico, senza collasso dell’accept loop.

## Definition of Done globale (tutto deve essere vero a fine epic)
- [ ] `docker compose up` avvia Postgres, Redis, Auth, Login, Game, Ranking, Messenger su Linux; healthcheck verdi.
- [ ] Zero P/Invoke / Win32 / ODBC / SQL Server nel Java.
- [ ] Nessuna credenziale in git; solo env / Compose secrets; `.env.example` completo.
- [ ] Schema PostgreSQL semanticamente equivalente a `SQL/bk-schema-mssql-complet2022.sql` del riferimento; Flyway versionato.
- [ ] Protocollo: test di framing + cipher + pacchetti noti (golden bytes dal C# / capture) verdi.
- [ ] Auth + Login: un client di test (Java fake che parla il protocollo, più eventualmente client reale se disponibile in env) completa login e riceve server list / session key.
- [ ] Game: almeno Practice end-to-end (sessione, canale, ingresso partita). Poi tutte le modalità elencate in slice S4.
- [ ] Ranking + Messenger parità funzionale col C# per i flussi documentati nel riferimento.
- [ ] Crash di una sessione non abbatte il processo (test: handler che lancia, server resta su).
- [ ] Ogni server espone metriche: connessioni attive, pkt/s in/out, latenza media dispatch.
- [ ] `./gradlew test` e `docker compose ps` verdi in CI locale (script `scripts/verify.sh`).

## Come lavorare
1. Clona il riferimento C# in `reference/pangya-server-community` (git submodule o clone in cartella gitignored `reference/`; NON committare binari/client pesanti). Mappa i 5 processi e PangyaAPI.*, cipher, packet IDs, schema SQL. Scrivi `docs/EPIC.md` con: mappa C#→Java, piano S0…S6, rischi, comandi di verifica. Committa.
2. Una slice alla volta, in ordine. Ogni slice: codice + test + `scripts/verify.sh` (o sottoinsieme dichiarato in EPIC.md) + commit atomici + aggiornamento EPIC.md (done / next / blocked / evidenza comandi).
3. Non dichiarare done senza output reale dei comandi. Se fallisce, fix prima della slice successiva.
4. Apri UNA PR lunga-vita e aggiornala; se il diff esplode, `/split-to-prs` per moduli già verdi (es. core-network) e tieni l’epic sulla restante.
5. Fixture protocollo: estrai dal C# (Cipher, packet structs) byte attesi; test JUnit con hex dump. Non inventare magic number.
6. Fine turno: EPIC.md deve bastare al turno successivo insieme a git. Niente stato solo in chat.

## Slice (esegui in ordine)

### S0 — Fondamenta
Gradle multi-modulo, Java 21, Checkstyle/Spotless opzionale ma `./gradlew test` gira, Docker Compose (postgres+redis+placeholder o server stub), Flyway con V1 schema tradotto dal SQL Server (tipi: NVARCHAR→TEXT/VARCHAR, DATETIME→TIMESTAMPTZ, identity→GENERATED). README: come alzare lo stack. Nessuna feature di gioco.

Verifica: `docker compose up -d postgres redis` healthy; `./gradlew :core-db:test`; Flyway migrate su Postgres vuoto.

### S1 — core-network + core-protocol
Netty bootstrap, framing a lunghezza come il C#, codec, cipher bit-compatibile, session object, nessun Sleep-polling. Virtual thread solo off event-loop. Test: handshake bytes, encrypt/decrypt roundtrip, un client Netty di test si connette e riceve la chiave di sessione (o l’equivalente del primo scambio C#).

Verifica: `./gradlew :core-protocol:test :core-network:test`.

### S2 — Auth + Login
Parità: pangya_auth_st, CmdAuthKeyLogin, CmdServerList, CmdVerifyID/Pass/Nick, ban IP/MAC (CmdInsertBlockIP/Mac + liste), heartbeat/propagate chiavi verso Login/Game. Redis per session key lookup. Login end-to-end con fake client.

Verifica: compose Auth+Login+db+redis; test integrazione login OK; pacchetti allineati alle golden fixture.

### S3 — Game Server core
Channel, SessionManager, heartbeat verso Auth, reconnect inter-server. UNA modalità: Practice. Un giocatore (fake client) entra in canale e in partita di test. Inventario minimo se il C# lo richiede per Practice; altrimenti stub esplicito in EPIC.md.

Verifica: test integrazione Practice; sessione killata non crasha il process.

### S4 — Game Server completo
1:1 col C#: Versus, Tourney, Practice, Grand Prix, Grand Zodiac, Guild Battle, Pang Battle, Approach, Chip-in Practice, Special Shuffle Course. CharacterManager, CardManager, CaddieManager, AchievementManager. Ogni modalità: test di stato machine + almeno un flusso felice con fake client o unit sul domain portati dal C#.

Verifica: test per modalità; `./gradlew test` moduli game.

### S5 — Ranking + Messenger
Classifiche player/guild persistite; messaggi player/guild e notifiche. Parità col C# sui comandi esistenti.

Verifica: test integrazione + compose completo 5 server.

### S6 — Hardening
Log JSON, Micrometer/OTel, test carico sintetico (Netty clients) verso 3000 sessioni o il massimo raggiungibile in VM (documenta il numero reale in EPIC.md se la VM è piccola). Tuning note GC/Netty. `scripts/verify.sh` = gradle test + compose config + health.

## Gate: fermati e aspetta follow-up umano
- Byte del protocollo non ricostruibili dal C# (serve capture da client reale): elenca packet ID e file C# già letti.
- Serve il client Pangya binario nell’env e non c’è: prosegui con fake client + fixture; non scaricare warez/client da link non ufficiali se i ToS/license lo vietano. Documenta il gap.
- Stai per cambiare lo stack (no Netty, sì Hibernate, Mongo, ecc.).
- Schema SQL con semantica ambigua (collation, locking) su query ranking/inventario: proponi mapping e aspetta solo se due interpretazioni cambiano il gameplay.

## Convenzioni
- Package `org.pangya.*` (o `com.pangya.*` se già nel repo): network / protocol / db / auth / login / game / ranking / messenger.
- Nomi dei Cmd* e packet ID: tieni un registro `docs/protocol-map.md` (C# type → Java class → opcode).
- No eccezioni checked pervasive; errori di sessione isolati.
- SQL esplicito, niente magia ORM.
- Italiano o inglese in commenti: inglese nel codice, italiano ammesso in EPIC.md.

## Prima azione
1. Verifica Java 21, Docker, Git nel VM.
2. Clona il riferimento Develop in `reference/`.
3. Scrivi `docs/EPIC.md` + `docs/protocol-map.md` (skeleton) + `AGENTS.md` con comandi verify.
4. Implementa S0 fino a verde, poi S1, poi avanti. Apri la PR dopo S0.

## Fine di ogni turno (obbligatorio in PR)
Slice completate, comandi lanciati + esito, prossima slice, blocker. Aggiorna EPIC.md.
