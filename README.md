# Pangya Server (Java rewrite)

Linux/Docker-native rewrite of the Pangya Season 9 **JP** community emulator
([luismk/Pangya-Server-Community](https://github.com/luismk/Pangya-Server-Community) `Develop`, `Server/JP`).
The JP game client protocol stays **bit-compatible**. This repo is Java only.

## Stack

- Java 21, Gradle multi-module, Netty
- PostgreSQL 16 + Flyway (Jdbi, no JPA)
- Redis
- Five processes: Auth `7777`, Login `10203`, Game `20202`, Ranking `4774`, Messenger `30201`

## Raise the stack

```bash
cp .env.example .env
docker compose up -d postgres redis
./gradlew test
docker compose up --build
```

Health endpoints: Auth `9077/health`, Login `9103`, Game `9202`, Ranking `9474`, Messenger `9302`.

S3/S4/S5: Auth (7777), Login (10203), Game (20202), Ranking (4774) and Messenger (30201) speak the JP Season 9 cipher (`JP.R7.983.00` / packet `2017110200`). Game login dumps JP `principal()` + live warehouse/character from Postgres (seed Nuri + Air Knight). Create-room sends C# `Room.getInfo().ToArray()` (210 bytes). Practice can start (`0x230`/`0x231`/`0x77`); Versus with one player is rejected (`0x253`). Prometheus `/metrics` is on every health port.

Dev login (Flyway V3): user `testuser` / password `testpass`.

## Verify

```bash
./scripts/verify.sh
```

See `docs/EPIC.md` for the slice plan and `docs/protocol-map.md` for packet IDs.

## License

MIT (same as the community C# project).
