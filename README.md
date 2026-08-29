# Pangya Server (Java rewrite)

Linux/Docker-native rewrite of the Pangya Season 9 **JP** community emulator
([luismk/Pangya-Server-Community](https://github.com/luismk/Pangya-Server-Community) `Develop`, `Server/JP`).
The JP game client protocol stays **bit-compatible**. This repo is Java only.

## Stack

- Java 21, Gradle multi-module, Netty
- PostgreSQL 16 + Flyway (Jdbi, no JPA)
- Redis
- Five processes: Auth `7777`, Login `10203`, Game `20202`, Ranking `4774`, Messenger `30201`

## Deploy

Requires Docker Engine with Compose v2. Java 21 and Gradle are **not** required on the host when you deploy with Compose.

### 1. Configure

```bash
git clone https://github.com/aleksandarstojkovski/Pangya-Server-Community.git
cd Pangya-Server-Community
cp .env.example .env
```

Edit `.env`:

| Variable | Purpose |
|----------|---------|
| `POSTGRES_PASSWORD` | Postgres password (default `pangya`) |
| `PANGYA_IP` | Address the JP client uses after login. `127.0.0.1` if the client is on the same host; otherwise the server LAN/WAN IP |

Login advertises Game and Messenger using `PANGYA_IP` (override with `PANGYA_GAME_IP` / `PANGYA_MESSENGER_IP` if those hosts differ).

### 2. Build and start

```bash
docker compose up --build -d
```

Wait until all seven services are healthy (first build compiles the five Java servers):

```bash
docker compose ps
```

### 3. Check health

```bash
curl -fsS http://127.0.0.1:9077/health   # auth
curl -fsS http://127.0.0.1:9103/health   # login
curl -fsS http://127.0.0.1:9202/health   # game
curl -fsS http://127.0.0.1:9474/health   # ranking
curl -fsS http://127.0.0.1:9302/health   # messenger
```

Each response is `ok <name>`. Prometheus scrapes `/metrics` on the same health ports.

### 4. Point the JP client

Configure the client at **Login** `PANGYA_IP:10203`. Auth, Game, Ranking, and Messenger are reached on the ports below (published on `0.0.0.0`).

### 5. Logs, restart, stop

```bash
docker compose logs -f --tail=100
docker compose restart
docker compose down          # keeps the Postgres volume
docker compose down -v       # also drops pangya-pg (all accounts)
```

### Published ports

Compose binds every listed port on `0.0.0.0` (all host interfaces). Open the **client** ports on any host firewall / security group.

| Service | Client (JP) | Health `/health` + `/metrics` |
|---------|-------------|-------------------------------|
| Auth | **7777** | 9077 |
| Login | **10203** | 9103 |
| Game | **20202** | 9202 |
| Ranking | **4774** | 9474 |
| Messenger | **30201** | 9302 |
| Postgres | 5432 (internal / nested Docker) | — |
| Redis | 6379 (internal / nested Docker) | — |

`5432` and `6379` are published so the Java containers can reach them via `host.docker.internal`. Do not expose them on the public internet.

Example firewall (Ubuntu `ufw`), client-only:

```bash
sudo ufw allow 7777/tcp
sudo ufw allow 10203/tcp
sudo ufw allow 20202/tcp
sudo ufw allow 4774/tcp
sudo ufw allow 30201/tcp
```

### Seed accounts (Flyway)

| User | Password | Notes |
|------|----------|--------|
| `testuser` | `testpass` | Dev account with first-set (V3) |
| `newuser` | `testpass` | First-set flow (V7) |

## Local development

```bash
cp .env.example .env
docker compose up -d postgres redis
./gradlew test
docker compose up --build
```

S3/S4/S5: Auth (7777), Login (10203), Game (20202), Ranking (4774) and Messenger (30201) speak the JP Season 9 cipher (`JP.R7.983.00` / packet `2017110200`). Game login dumps JP `principal()` + live warehouse/character from Postgres (seed Nuri + Air Knight). Create-room sends C# `Room.getInfo().ToArray()` (210 bytes). Practice can start (`0x230`/`0x231`/`0x77`); Versus with one player is rejected (`0x253`). Prometheus `/metrics` is on every health port.

## Verify

```bash
./scripts/verify.sh
```

See `docs/EPIC.md` for the slice plan and `docs/protocol-map.md` for packet IDs.

## License

MIT (same as the community C# project).
