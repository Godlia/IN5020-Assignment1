# IN5020 Assignment 1 — checklist against the handout

Source: `exercise_1.pdf` (H26 version, OneDrive `ass1-rmi.updated`). **Deadline: Thu 24.09.2026 23:59 on Devilry.**
Repo state checked: `origin/main` = `6a2c92c` (Kine, 24.09 12:41), read only — not built or run.

Legend: `[x]` present in the code · `[ ]` missing / not done · ⚠ present but differs from the handout (question for the owner)

> The repo's own `exercise_1.pdf` is the **H25** handout (deadline 18.09.2025). Use the OneDrive copy.
> `git status` shows ~27 modified files, but `git diff --ignore-cr-at-eol` is empty — CRLF noise only. Don't commit it.

---

## 0. Must-have for tonight (blocks the submission)

- [ ] Decide the **naive** variant (see 2.5) — the handout's definition vs the SQLite import
- [ ] Wire the **server cache** + **client cache** and a flag/env to switch them on (see 4)
- [ ] Measure **execution** and **waiting** time on the **server** (see 3.3)
- [ ] Produce `naive_server.txt`, `server_cache.txt`, `client_cache.txt` (see 5)
- [ ] Run each variant at **T = 50** and **T = 20** and draw the graphs (see 6)
- [ ] Report PDF + zip + `docker save` image (see 7)

---

## 1. Proxy (load-balancing server) — Oscar → Vetle

- [x] Remote interface for clients (`RequestServer(zone)`) replies with **address + port**, client then calls the server directly
- [x] Registration API (`RegisterServer`), zones assigned in **ascending** order of registration
- ⚠ Records a **host name** (`SERVER_HOST` = compose service name), handout says **IP address** — fine inside the compose network, but say so in the report
- [x] Prefers the client's own zone when its queue is **< 18**
- [x] Otherwise the **least-loaded** server with queue < 18; ties broken by **clockwise** distance
- [x] All overloaded → same-zone server
- [x] Zone without a server → moved to the closest zone clockwise (6 → 7, 8 → 1 style)
- ⚠ Clockwise ring size = highest registered zone, not a fixed number — correct only when zones 1..n are all registered. With 5 servers up it's fine; check if you ever run < 5
- [x] Load refreshed after every **18** assignments **per server**, on a **separate thread**
- ⚠ **Server side of the refresh:** `Server.getQueueLength()` goes *through* the request queue (`requestQueue.submit(...)`), so it sleeps 80 ms and waits behind every queued request before reading the size. The proxy then sees the length *after* the queue has drained (≈ 0) and may never detect overload. Should reading the length bypass the queue? (Eirik / you)
- [ ] `VERBOSE = true` on every request — consider turning off for the timed runs (console I/O inside a `synchronized` block)
- [ ] Doc comments at method level (handout: "must be well commented")

## 2. Server — Eirik

- [x] Servant implements the four methods: `getPopulationofCountry`, `getNumberofCities`, `getNumberofCountries`, `getNumberofCountriesMM`
- [ ] Handout examples re-checked on the latest main (Norway 3,162,856 · Sweden 9,362,428 · Norway ≥100 000 → 4 · 2 cities ≥ 5 M → 7 · 30 cities 100k–800k → 30). They reproduced on 23.09 — recheck after today's merges
- [x] 2.a Stub registered under a name — every server binds `"server"` on **1099** in its own container (unique by host, not by name/port). Explain in the report why that satisfies "unique names on different ports"
- [x] 2.b Docker image for a group of 4 (see 8)
- [x] 3 Latency sleep **before** enqueuing: 80 ms + |requested zone − server zone| × 30 ms
- ⚠ Server's zone is `0` until `registerWithProxy` returns — requests in that window get the wrong X
- [x] 4 Zone-specific waiting list, **FIFO** (`LinkedBlockingQueue`)
- [x] 4.c **One** execution thread; RMI's own threads act as the acceptor group
- [x] 5 Queue log per server (`QUEUE_LOG_FILE` → `output/serverX-queue.log`)
- ⚠ Log timestamps are ISO-8601 (`Instant.now()`); the graph wants a **Unix timestamp** on the x-axis — convert when plotting, or log epoch millis
- ⚠ **2.5 Naive variant:** handout = "server parses the whole dataset every time a request is made". The code imports the CSV into SQLite once at startup (that *is* pre-processing, which the cache sections forbid). Group decision needed: implement a true naive path, or keep SQLite and justify it in the report

## 3. Client — Eirik

- [x] Parses the input file (`QUERY_FILE` env, falls back to stdin)
- [x] Prints each result + times, and writes an output file
- [x] Line format `<result> <query> (turnaround time: … ms, execution time: … ms, waiting time: … ms, processed by Server <n>)`
- ⚠ **3.1 T-pacing:** invocations are scheduled every `CLIENT_DELAY_MS` (default 50) — good — but on a pool of `max(4, 2 × CPUs)` threads. Each call blocks ≥ 80 ms, so once that many are outstanding, later queries start **late**, i.e. not "regardless of whether the current invocation is finished". Check pool size vs T = 20
- [ ] **T = 20** run: `CLIENT_DELAY_MS=20` is not set anywhere in compose/scripts
- ⚠ **3.3 Timing definitions (Summary of measurements):**
  - *execution* = time the server spends running the request → currently the client's whole RMI call (latency + queue + execution)
  - *waiting* = time on the server's waiting list → currently `turnaround − execution` (≈ proxy lookup + client-side scheduling delay)
  - Needs the server to measure both and return them with the result
- [x] 3.4 Per-method summary lines (avg turn-around / execution / waiting, min & max turn-around)
- ⚠ Handout says "6 entries" for 4 methods — note the quirk in the report; there's also an extra `Total elapsed time` line
- [ ] Throws `No server available for zone …` if the proxy has no servers yet — make sure the client starts after all 5 have **registered**

## 4. Caching — Kine

- [x] `Cache` class with capacities **SERVER 150** / **CLIENT 45**, FIFO and OLDEST (LRU) eviction, unit tests
- [ ] **Server-side cache used in `Server`** (not referenced anywhere outside `cache/` yet)
- [ ] **Client-side cache used in `Client`** (same)
- [ ] Starts empty, filled per request only — no pre-processing
- [ ] Command-line flag / env to enable server cache, and to tell the client the server has one
- [ ] Choose FIFO or OLDEST per run (and say which in the report)

## 5. Output files

- [ ] `naive_server.txt`
- [ ] `server_cache.txt`
- [ ] `client_cache.txt`
- ⚠ Client writes `OUTPUT_FILE` = `client-output.txt` by default — needs the right name per variant (env per run?)
- [ ] One output line per input query — the input has **3,166** queries (last line has no newline; `wc -l` says 3,165)
- [ ] Keep T = 50 and T = 20 results apart (two sets, or state which T the files are)

## 6. Graphs (T = 50 **and** T = 20)

- [ ] Turn-around time per query — x: query number, y: turn-around time (per variant)
- [ ] Queue length per server (×5) — x: Unix timestamp, y: queue length
- [ ] Plot script or notebook in the repo (`scripts/`?), so the graphs are reproducible

## 7. Deliverables (Devilry, one compressed file)

- [ ] Report PDF: design + implementation description
- [ ] Screenshots of client **and** server running
- [ ] **User guide**: compile, build, deploy, run (Maven → image → compose → outputs) — Vetle
- [ ] Workload division (README's split: Eirik client/server, Oscar→Vetle proxy, Kine cache, Vetle Docker)
- [ ] Zip with everything needed to build and run: source, `pom.xml`, `mvnw`, dataset, input file
- [ ] Source commented at method/algorithm level
- [ ] Ready-to-deploy jar (`target/assignment1.jar`, shaded)
- [ ] Ready-to-deploy **docker image file** (`docker save … -o in5020.tar`; if too big → OneDrive/Drive link in the submission)
- [ ] The three output files + graphs
- [ ] Any other artefact needed (compose file, `entrypoint.sh`, run scripts)

## 8. Docker / Container — Vetle

- [x] `Dockerfile` (temurin 21 JRE) with jar, dataset and `entrypoint.sh`; role via `APP_ROLE`
- [x] `docker-compose.yml`: proxy + 5 servers + client, service-name DNS on one bridge network, `java.rmi.server.hostname` = service name
- [x] Healthchecks for proxy and servers; client waits for all six
- [x] `./output` volume for queue logs and client output
- ⚠ Image needs `target/assignment1.jar` built **on the host first** (`./mvnw package` / `rundatshit.sh`). The comment says "stage 1: compile with Maven", but there is no Maven stage — decide: multi-stage build, or document the host build in the user guide
- ⚠ Each server has `build: .` + its own `image:` name (proxy and client get default names) → **seven** builds / image tags of the same thing. Which one goes to `docker save`? Consider one image name
- ⚠ Server healthcheck = "`server` is bound in my registry", which happens **before** `registerWithProxy` → healthy ≠ registered; the client can start before all 5 have zones
- ⚠ Zone numbers follow registration order, so `serverA` is not guaranteed zone 1 (parallel start). Fine if the report says so; the "processed by Server n" numbers depend on it
- [ ] Variant switches in compose (cache flags, `CLIENT_DELAY_MS`, `OUTPUT_FILE` per run) — or one documented command per run
- [ ] Proxy publishes `1099:1099` on all interfaces — only needed if something outside compose calls it
- [ ] `.dockerignore` still has DockerTest comments/entries — tidy up
- [ ] `scripts/test-all.sh` refers to old profiles/services (`server-zone1…`, `--profile all`) — update or drop
- [ ] `rundatshit.sh` / `.ps1` — rename before handing in?
- [ ] `docker save` tested: file size + `docker load` on a clean machine/WSL

## 9. Housekeeping before hand-in

- [ ] README run instructions match the final pom (`assignment1.jar`, Java 21) and compose
- [ ] CRLF-only modifications not committed; `.sh` files stay LF
- [ ] Everyone's branch merged to `main`; the zip is built from `main`
- [ ] Devilry: submitted before 23:59, whole group registered
