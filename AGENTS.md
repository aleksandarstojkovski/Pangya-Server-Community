# Pangya Java rewrite — Cloud Agent notes

This repository is the Java 21 rewrite. The C# source of truth lives in a gitignored clone:

```
git clone --depth 1 --branch Develop https://github.com/luismk/Pangya-Server-Community.git reference/pangya-server-community
```

## Commands

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

## Layout

| Module | Role |
|--------|------|
| `core-protocol` | Cipher tables, packet IDs, framing types |
| `core-network` | Netty bootstrap, sessions, process stub |
| `core-db` | Flyway + Jdbi |
| `server-*` | Auth, Login, Game, Ranking, Messenger |

Slice plan, C#→Java map, and blockers: `docs/EPIC.md`. Packet IDs: `docs/protocol-map.md`.

Do not change client-facing packet bytes. Do not introduce JPA/Hibernate.
