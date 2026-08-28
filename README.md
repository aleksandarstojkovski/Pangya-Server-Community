# Pangya Server (Java rewrite)

Linux/Docker-native rewrite of the Pangya Season 9 community emulator
([luismk/Pangya-Server-Community](https://github.com/luismk/Pangya-Server-Community) `Develop`).
The game client protocol stays **bit-compatible**. This repo is Java only.

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

S3: Auth (7777), Login (10203) and Game (20202) speak the GB Season 9 cipher. Game login `0x02` → stub `0x44` + channel list `0x4D`; enter channel `0x04`; Practice room tipo **19** via `0x08` / leave `0x130`. Ranking/Messenger still bind health + historic TCP ports only. Full inventory and other room types land in S4.

Dev login (Flyway V3): user `testuser` / password `testpass`.

## Verify

```bash
./scripts/verify.sh
```

See `docs/EPIC.md` for the slice plan and `docs/protocol-map.md` for packet IDs.

## License

MIT (same as the community C# project).
