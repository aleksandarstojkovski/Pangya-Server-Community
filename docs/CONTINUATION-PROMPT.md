# Pangya Java rewrite — Cloud Agent continuation prompt

Copia tutto il contenuto sotto la linea `---` e incollalo come prompt iniziale del Cloud Agent.

---

Sei un Cloud Agent. Continui la riscrittura Pangya Game Server in Java 21+ (bit-compatibile col client JP Season 9). **Non ripartire da zero.** Il lavoro reale è già su branch git **`Develop`** (Java). L’agente precedente è stato stoppato per allucinazioni su progresso/test — tratta **git + output comandi** come unica verità.

## Branch e repo (correggi doc obsolete)

| Cosa | Branch / path |
|------|----------------|
| **Codice Java (questo repo)** | `Develop` — lavora qui, PR verso `Develop` |
| **Sorgente C# comportamento** | `Develop_luiz` su `https://github.com/luismk/Pangya-Server-Community` |
| **Clone locale read-only** | `reference/pangya-server-community/` (gitignored, **non committare**) |

Aggiorna `AGENTS.md`, `README.md`, `docs/EPIC.md` dove citano branch C# `Develop` → **`Develop_luiz`**.

## Stato presunto (DA REVERIFICARE — non fidarti ciecamente)

Slice (da `docs/EPIC.md` / `docs/STATUS.md`, riconciliati 2026-08-29):

```
S0 [x]  S1 [x]  S2 [x]  S3 [x]  S4 [~]  S5 [~]  S6 [~]
```

- **S0–S3:** fondamenta, protocol/network, Auth+Login, Game core + Practice — presumibilmente fatti
- **S4–S6:** parziali — GP parity mergeata (PR #6–#7); lacune grep: `calcule_shot_to_coin`, `CoinCubeLocationUpdateSystem`, `requestSaveInfo` fuori GP
- **386 test** nel repo; `GameFlowIT` ~97/98 quando tutto gira
- Doc dice 2 failure noti su `./gradlew test`: `FlywayMigrationTest.migratesAndIsIdempotent` + `GameFlowIT.personalShopBuyIncrementsAchievementCounter`
- Percentuali inventate (“85%”, “43%”) **vietate** — rimosse dai doc, non reintrodurle

## Problema immediato: environment assente

Nel repo **non c’è** `.cursor/environment.json`. L’environment Cloud Agent precedente non è più collegato. **Primo obiettivo del turno 0:**

1. Creare e committare `.cursor/environment.json` (+ Dockerfile in `.cursor/` se serve) con:
   - Java 21 (Temurin)
   - Docker + Docker Compose (per postgres/redis e `scripts/verify.sh`)
   - Git
   - `install` idempotente che esegue almeno:
     ```bash
     git clone --depth 1 --branch Develop_luiz \
       https://github.com/luismk/Pangya-Server-Community.git \
       reference/pangya-server-community || true
     # se esiste già: git -C reference/pangya-server-community fetch && checkout Develop_luiz
     cp -n .env.example .env || true
     docker compose up -d postgres redis
     ./gradlew test --no-daemon || true
     ```
     (L’ultimo comando serve solo a vedere i failure reali — non per dichiarare verde.)
   - `start`: assicurati postgres+redis up se non già healthy
2. Proporre l’environment (build/snapshot) seguendo skill env-setup
3. Aggiornare `AGENTS.md` con branch C# corretto e prerequisiti environment

## Turno 0 — Audit obbligatorio (prima di feature)

Esegui e **incolla output** in EPIC.md / PR (non inventare esiti):

```bash
git checkout Develop && git pull
java -version && docker --version && git --version
test -d reference/pangya-server-community/Server/JP && echo "reference OK" || echo "reference MISSING"
docker compose up -d postgres redis
./gradlew test --no-daemon 2>&1 | tee /tmp/gradle-test.log; echo EXIT=$?
./scripts/verify.sh 2>&1 | tee /tmp/verify.log; echo EXIT=$?
```

(`verify.sh` solo se `./gradlew test` è verde o con subset dichiarato in EPIC.md.)

Aggiorna `docs/EPIC.md` e `docs/STATUS.md` con:

- HEAD commit, elenco failure reali (nome test + modulo)
- Cosa è verificato vs assunto
- Prossima slice concreta

**Regola anti-allucinazione:** vietato scrivere “test verdi”, “slice done”, percentuali, conteggi opcode “173/196 1:1” senza grep/script + output. Se non hai eseguito il comando, scrivi “non verificato”.

## Obiettivo epic

Portare emulatore Pangya C# (.NET Framework 4.8, 5 processi, SQL Server/ODBC, Windows-only) a stack Linux/Docker-native in Java, con stessa logica di dominio (protocollo binario, sessioni, regole di gioco, schema dati semanticamente equivalente).

**Sorgente di verità comportamento:** clone read-only di `https://github.com/luismk/Pangya-Server-Community`, branch **`Develop_luiz`**, path `Server/JP/`.  
**Questo repo:** solo riscrittura Java. Non patchare il C# se non per copiare fixture/commenti nel Java.

## Fuori scope (non fare)

- Client di gioco: non toccarlo, non “migliorare” il protocollo verso il client
- Cambiare formato/cifratura pacchetti client↔server (bit-compatible con `Cipher.ServerEncrypt` e framing attuali)
- Scalabilità orizzontale Game Server con match distribuito
- Migrazione dati storici da SQL Server produzione (DB PostgreSQL vuoto + seed dev)
- JPA/Hibernate, Kubernetes, UI admin, rewrite estetico non richiesto dal C#
- Dipendenze Windows (kernel32, dbghelp, Console.Title, DSN ODBC)
- Multi-versione protocollo — una sola: C# `Develop_luiz` / Season 9 JP

## Decisioni già prese (non riaprire)

- Java 21+, Netty, virtual thread per lavoro bloccante di dominio, I/O solo su event loop Netty
- PostgreSQL + Flyway; accesso dati JDBI (SQL espliciti)
- Redis per session lookup cross-server, ban list, cache ranking
- Gradle multi-modulo: `:core-network`, `:core-protocol`, `:core-db` + moduli eseguibili Auth, Login, Game, Ranking, Messenger
- Config: `application.yml` + env vars. Log: SLF4J/Logback JSON su stdout. Metriche: Micrometer + OTel
- Docker multi-stage Temurin 21 JRE; `docker compose up` = 5 server + Postgres + Redis
- Comunicazione inter-server: TCP verso Auth, reconnect automatico
- Anti-bot/DDoS: logica equivalente a `IpDdosFilter`
- Target carico (S6): ≥ 3000 sessioni concorrenti Game Server in test sintetico

## Definition of Done globale

- [ ] `docker compose up` avvia Postgres, Redis, Auth, Login, Game, Ranking, Messenger su Linux; healthcheck verdi
- [ ] Zero P/Invoke / Win32 / ODBC / SQL Server nel Java
- [ ] Nessuna credenziale in git; solo env / Compose secrets; `.env.example` completo
- [ ] Schema PostgreSQL semanticamente equivalente a `SQL/bk-schema-mssql-complet2022.sql` del riferimento; Flyway versionato
- [ ] Protocollo: test framing + cipher + pacchetti noti (golden bytes dal C# / capture) verdi
- [ ] Auth + Login: fake client completa login e riceve server list / session key
- [ ] Game: Practice end-to-end; poi tutte le modalità S4
- [ ] Ranking + Messenger parità funzionale col C# sui flussi documentati
- [ ] Crash sessione non abbatte il processo
- [ ] Ogni server espone metriche: connessioni, pkt/s, latenza dispatch
- [ ] `./gradlew test` e `scripts/verify.sh` verdi

## Piano slice (riferimento — non rifare ciò che è già su `Develop`)

| Slice | Scope | Note |
|-------|-------|------|
| **S0** | Gradle, Compose, Flyway, stub 5 server | Presumibilmente [x] — 43 migration |
| **S1** | Netty, Cipher, MiniLZO, session | Presumibilmente [x] |
| **S2** | Auth + Login + Redis session key | Presumibilmente [x] |
| **S3** | Game core + Practice | Presumibilmente [x] |
| **S4** | Modalità C# + manager char/card/caddie/achievement | [~] GP deep parity; coin-cube + save-info altri modi aperti |
| **S5** | Ranking + Messenger | [~] moduli + IT base |
| **S6** | Metriche, load 3000, `verify.sh` | [~] SessionLoadIT OK se env completo |

## Prossimo lavoro (dopo audit — rivaluta con output reale)

Ordine suggerito:

1. **Verde `./gradlew test`**
   - `FlywayMigrationTest.migratesAndIsIdempotent` — second migrate no-op (doc: expected 0, was 4)
   - `GameFlowIT.personalShopBuyIncrementsAchievementCounter` — `IllegalStateException: no server packet`
   - Test IFF che richiedono `reference/pangya-server-community/Server/JP/GameServer/data/*.iff` (falliscono se clone assente)
2. **S4:** port `calcule_shot_to_coin` + `CoinCubeLocationUpdateSystem` in `syncShot` (C# `TourneyBase.requestSyncShot`)
3. **S4 residuo:** modalità/manager senza parità verificata vs C#
4. **S5:** profondità Ranking/Messenger vs C#
5. **S6:** hardening, load test, `verify.sh` end-to-end

**Stop:** nessun nuovo opcode finché `./gradlew test` non è verde e la riconciliazione doc è aggiornata.

## Gate — fermati e chiedi umano

- Byte protocollo non ricostruibili dal C# (serve capture client reale): elenca packet ID e file C# già letti
- Client Pangya binario assente in env — prosegui fake client + fixture; non scaricare warez/client da link non ufficiali
- Stai per cambiare lo stack (no Netty, sì Hibernate, Mongo, ecc.)
- Schema SQL semanticamente ambiguo su query ranking/inventario: proponi mapping e aspetta solo se due interpretazioni cambiano il gameplay

## Come lavorare

1. **Non** reimplementare S0–S3 se già mergeati su `Develop`
2. Una slice alla volta; codice + test + commit atomici; aggiorna `docs/EPIC.md` ogni turno (done / next / blocked / evidenza comandi)
3. Non dichiarare done senza output reale dei comandi
4. Fixture protocollo: golden bytes dal C# — non inventare magic number; registro in `docs/protocol-map.md`
5. PR long-vita; branch feature `cursor/<descriptive-name>-4e66`; base `Develop`
6. Fine turno in PR: slice completate, comandi lanciati + exit code, prossima slice, blocker

## Convenzioni

- Package `org.pangya.*`
- Nomi Cmd* e packet ID: `docs/protocol-map.md` (C# type → Java class → opcode)
- No eccezioni checked pervasive; errori di sessione isolati
- SQL esplicito, niente ORM
- Non cambiare byte client-facing
- Commenti codice in inglese; EPIC.md può essere italiano

## Comandi utili

```bash
# S3 verify (postgres + redis + Flyway + Auth/Login/Game/Ranking/Messenger fake client)
./scripts/verify.sh

# Full Gradle tests
./gradlew test

# Infrastructure only
docker compose up -d postgres redis

# Full stack
docker compose up --build
```

## Prima azione (ordine rigido)

1. Setup environment (`.cursor/environment.json` + install reference `Develop_luiz`)
2. Audit comandi → aggiorna EPIC/STATUS con verità misurata
3. Ripristina `./gradlew test` verde
4. Solo dopo: S4 coin-cube / parità gameplay

Non chiedere conferma per slice ovvie. Chiedi solo ai gate o prima di breaking protocollo / dump secret / scelta stack irreversibile.
