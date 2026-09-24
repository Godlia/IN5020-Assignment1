# IN5020 Assignment 1 — checklist against the handout

Source: `exercise_1.pdf` (H26 version, OneDrive `ass1-rmi.updated`). **Deadline: Thu 24.09.2026 23:59 on Devilry.**
Repo state checked: `origin/main` = `6a2c92c` (24.09 12:41), read only — not built or run.
Updated 24.09 ~17:30 with the TA's answers (below).
**Boxes re-ticked 24.09 ~18:00 against `c003f94`** (Eirik, 17:40, "Merge branch 'kine'") = `origin/main` as last fetched — a fresh `git fetch` was not possible from here, so anything pushed after 17:40 is not reflected. Compiled with `javac` 21; `ServerRepository` run against the real dataset; nothing in the repo edited except this file.

Legend: `[x]` present in the code · `[ ]` missing / not done · ⚠ present but differs from the handout (question for the owner)

> The repo's own `exercise_1.pdf` is the **H25** handout (deadline 18.09.2025). Use the OneDrive copy.
> `git status` shows ~27 modified files, but `git diff --ignore-cr-at-eol` is empty — CRLF noise only. Don't commit it.

---

## TA answers (truongl, course chat) — 24.09

- **FIFO and OLDEST are both run, separately** — not one or the other ("we do all the case seperately")
- **6 runs:** naive, FIFO, OLDEST at T = 50, then the same three at T = 20 — and in a cache run, client and server use the **same** policy
- **Data storage is our choice:** "any database to host the data, or even pure operations in java" → SQLite may stay
- **T = 50 first** → output files + graphs; **then T = 20**, results **added to the same files**, and a **new** graph

---

## 0. Must-have for tonight (blocks the submission)

- [ ] ⚠ **`main` does not compile** (`c003f94`): `Client.java:174: error: variable result is already defined in method executeQuery` — the merge of `c0c6ba7` (Eirik's client cache) and `36568c4` (Kine's cache wiring) left two `Object result` declarations (l.165 and l.174). No jar, no image and no run until this is fixed
- [ ] **Naive** variant (see 2.5): SQLite is allowed (TA) — write in the report what "naive" means in our code
- [ ] Wire the **server cache** + **client cache** and a flag/env to switch them on (see 4) — server ✅ (`CACHE_MODE=SERVER`), client ❌ (two half-caches, see 4)
- [ ] Measure **execution** and **waiting** time on the **server** (see 3.3)
- [ ] Produce `naive_server.txt`, `server_cache.txt`, `client_cache.txt` (see 5) — ⚠ the output file is overwritten on every run today, and the FIFO/OLDEST → file mapping is still open
- [ ] **6 runs** (TA): naive / FIFO / OLDEST at **T = 50**, then the same three at **T = 20**, appended to the same files; new graphs for T = 20 (see 5, 6)
- [ ] Report PDF + zip + `docker save` image (see 7)

---

## 1. Proxy (load-balancing server)

- [x] Remote interface for clients (`RequestServer(zone)`) replies with **address + port**, client then calls the server directly
- [x] Registration API (`RegisterServer`), zones assigned in **ascending** order of registration
- ⚠ Records a **host name** (`SERVER_HOST` = compose service name), handout says **IP address** — fine inside the compose network, but say so in the report
- [x] Prefers the client's own zone when its queue is **< 18**
- [x] Otherwise the **least-loaded** server with queue < 18; ties broken by **clockwise** distance
- [x] All overloaded → same-zone server
- [x] Zone without a server → moved to the closest zone clockwise (6 → 7, 8 → 1 style)
- ⚠ Clockwise ring size = highest registered zone, not a fixed number — correct only when zones 1..n are all registered. With 5 servers up it's fine; check if you ever run < 5
- [x] Load refreshed after every **18** assignments **per server**, on a **separate thread**
- [x] **Server side of the refresh:** `Server.getQueueLength()` now returns `requestQueue.size()` directly, no longer through the queue (`051effa` "skip queue size in queue")
- ⚠ In the last run on disk (`output/serverA-queue.log`, 12:58, T = 50, before the caches) serverA's queue never went above **2**, so the ≥ 18 overload rule never fires at T = 50. Expected — look for redirects in the T = 20 runs, and say it in the report
- [ ] `VERBOSE = true` on every request — consider turning off for the timed runs (console I/O inside a `synchronized` block). Since `2cfe058` the server also prints one line per request (`ServerRequestQueue.submit`, "Was requested zone …")
- [ ] Doc comments at method level (handout: "must be well commented") — only `cache/` has Javadoc; `client/`, `proxy/`, `server/` have none

## 2. Server

- [x] Servant implements the four methods: `getPopulationofCountry`, `getNumberofCities`, `getNumberofCountries`, `getNumberofCountriesMM`
- [x] Handout examples re-checked on the latest main (Norway 3,162,856 · Sweden 9,362,428 · Norway ≥100 000 → 4 · 2 cities ≥ 5 M → 7 · 30 cities 100k–800k → 30) — all five reproduce with `c003f94`'s `ServerRepository` (24.09)
- [x] 2.a Stub registered under a name — every server binds `"server"` on **1099** in its own container (unique by host, not by name/port). Explain in the report why that satisfies "unique names on different ports"
- [x] 2.b Docker image for a group of 4 (see 8)
- [x] 3 Latency sleep **before** enqueuing: 80 ms + |requested zone − server zone| × 30 ms
- ⚠ Server's zone is `0` until `registerWithProxy` returns — requests in that window get the wrong X
- [x] 4 Zone-specific waiting list, **FIFO** (`LinkedBlockingQueue`)
- [x] 4.c **One** execution thread; RMI's own threads act as the acceptor group
- [x] 5 Queue log per server (`QUEUE_LOG_FILE` → `output/serverX-queue.log`)
- [x] Log timestamps are ISO-8601 (`Instant.now()`); the graph wants a **Unix timestamp** on the x-axis — convert when plotting, or log epoch millis
- ⚠ **2.5 Naive variant:** handout = "server parses the whole dataset every time a request is made". The code imports the CSV into SQLite once at startup (that *is* pre-processing, which the cache sections forbid). Group decision needed: implement a true naive path, or keep SQLite and justify it in the report
  → **TA: "any database to host the data, or even pure operations in java"** — SQLite can stay. The answer is about *where the data lives*, not the "parse on every request" wording, so still say in the report that naive = no cache, a full query per request

## 3. Client

- [x] Parses the input file (`QUERY_FILE` env, falls back to stdin)
- [x] Prints each result + times, and writes an output file
- [x] Line format `<result> <query> (turnaround time: … ms, execution time: … ms, waiting time: … ms, processed by Server <n>)`
- [x] **3.1 T-pacing:** one virtual thread per query (`newVirtualThreadPerTaskExecutor`), each sleeping until `i × T` — the old pool-size cap is gone
- [ ] **T = 20** run: the client's default is now **20**, but `docker-compose.yml` sets `CLIENT_DELAY_MS: 10` — neither 50 nor 20. Set it explicitly per run
- ⚠ **3.3 Timing definitions (Summary of measurements):**
  - *execution* = time the server spends running the request → currently the client's whole RMI call (latency + queue + execution)
  - *waiting* = time on the server's waiting list → currently `turnaround − execution` (≈ proxy lookup + client-side scheduling delay)
  - Needs the server to measure both and return them with the result
- [x] 3.4 Per-method summary lines (avg turn-around / execution / waiting, min & max turn-around)
- ⚠ Handout says "6 entries" for 4 methods — note the quirk in the report; there's also an extra `Total elapsed time` line
- [ ] Throws `No server available for zone …` if the proxy has no servers yet — make sure the client starts after all 5 have **registered**

## 4. Caching

- [x] `Cache` class with capacities **SERVER 150** / **CLIENT 45**, FIFO and OLDEST (LRU) eviction, unit tests
- [x] **Server-side cache used in `Server`** — `cached(key, query)` wraps all four methods. It runs *inside* the queued task, so a hit still pays the 80 ms + X·30 latency and waits its turn; only the SQL is skipped. Say so in the report
- [ ] **Client-side cache used in `Client`** — ⚠ there are two:
  - `clientCache` (FIFO, hard-coded) is the one actually used, and it is **always on** — also in the naive and server-cache runs
  - `CACHE` (from `CACHE_MODE=CLIENT` / `CACHE_POLICY`) is looked up and then unused — this is where the compile error is
  - a hit returns the stored `QueryResult` with the **original** times and server number, so the output line cannot show that it was a hit
- [x] Starts empty, filled per request only — no pre-processing (both caches are filled on a miss only)
- [ ] Command-line flag / env to enable server cache, and to tell the client the server has one — env `CACHE_MODE` = `NAIVE` | `SERVER` | `CLIENT` and `CACHE_POLICY` = `FIFO` | `LRU` (default `LRU` = OLDEST) exist, read by `Server` (and `Client`'s unused `CACHE`); **neither is set in `docker-compose.yml`**. The client is not told the server has a cache
- ⚠ `CACHE_MODE` is *either* `SERVER` *or* `CLIENT`. That fits the handout's three files, but not the TA's 6-run plan (both caches on, same policy, in one run). Pick one reading as a group
- [ ] Run **both** FIFO and OLDEST, in separate runs (TA) — not a choice between them
- [ ] One flag/env per run sets the **same** policy on the client and on all five servers (TA: same method on client and server in a run)

## 5. Output files

- [ ] `naive_server.txt`
- [ ] `server_cache.txt`
- [ ] `client_cache.txt`
- ⚠ Client writes `OUTPUT_FILE` = `client-output.txt` by default — needs the right name per variant (env per run?)
- [x] One output line per input query — the input has **3,166** queries (last line has no newline; `wc -l` says 3,165). Last run on disk (`output/client-output.txt`, 13:01, T = 50, no cache): 3,166 lines + 4 summaries + total, 158.5 s
- [ ] T = 20 results **appended** to the T = 50 files (TA) — mark where each T starts (a header line, or rely on the summary block) so the report and the plots can split them
- ⚠ `Client.writeOutputFile` opens `new PrintWriter(outputFile)`, which **truncates** → the T = 20 run wipes the T = 50 results. Needs append mode, or join the two runs' files before hand-in (read on `c0c6ba7`, not run)
- ⚠ **Open:** 6 runs but 3 file names — which file does the FIFO run write, and which the OLDEST run (both caches are on in each)? Ask truongl, or pick a mapping and state it in the report

## 6. Graphs (T = 50 **and** T = 20)

- [ ] Turn-around time per query — x: query number, y: turn-around time (per variant)
- [ ] Queue length per server (×5) — x: Unix timestamp, y: queue length
- [ ] One set of graphs after the T = 50 runs, a **new** set after the T = 20 runs (TA)
- ⚠ `ServerRequestQueue.openLog` opens `QUEUE_LOG_FILE` with a truncating `PrintWriter` too → each run overwrites the previous run's queue logs. Copy `output/server*-queue.log` away after every run, or set a per-run `QUEUE_LOG_FILE`
- [ ] Plot script or notebook in the repo (`scripts/`?), so the graphs are reproducible

## 7. Deliverables (Devilry, one compressed file)

- [ ] Report PDF: design + implementation description
- [ ] Screenshots of client **and** server running
- [ ] **User guide**: compile, build, deploy, run (Maven → image → compose → outputs)
- [ ] Workload division (who did what — see README)
- [ ] Zip with everything needed to build and run: source, `pom.xml`, `mvnw`, dataset, input file
- [ ] Source commented at method/algorithm level
- [ ] Ready-to-deploy jar (`target/assignment1.jar`, shaded) — the one on disk is from 12:57, before the cache commits; cannot be rebuilt until `main` compiles
- [ ] Ready-to-deploy **docker image file** (`docker save … -o in5020.tar`; if too big → OneDrive/Drive link in the submission)
- [ ] The three output files + graphs
- [ ] Any other artefact needed (compose file, `entrypoint.sh`, run scripts)

## 8. Docker / Container

- [x] `Dockerfile` (temurin 21 JRE) with jar, dataset and `entrypoint.sh`; role via `APP_ROLE`
- [x] `docker-compose.yml`: proxy + 5 servers + client, service-name DNS on one bridge network, `java.rmi.server.hostname` = service name
- [x] Healthchecks for proxy and servers; client waits for all six
- [x] `./output` volume for queue logs and client output
- ⚠ Image needs `target/assignment1.jar` built **on the host first** (`./mvnw package` / `rundatshit.sh`). The comment says "stage 1: compile with Maven", but there is no Maven stage — decide: multi-stage build, or document the host build in the user guide
- ⚠ Each server has `build: .` + its own `image:` name (proxy and client get default names) → **seven** builds / image tags of the same thing. Which one goes to `docker save`? Consider one image name
- ⚠ Server healthcheck = "`server` is bound in my registry", which happens **before** `registerWithProxy` → healthy ≠ registered; the client can start before all 5 have zones
- ⚠ Zone numbers follow registration order, so `serverA` is not guaranteed zone 1 (parallel start). Fine if the report says so; the "processed by Server n" numbers depend on it
- [ ] Variant switches in compose (cache flags, `CLIENT_DELAY_MS`, `OUTPUT_FILE` per run) — or one documented command per run (**6 runs**, see the TA answers). Today compose sets no `CACHE_MODE`/`CACHE_POLICY`, `CLIENT_DELAY_MS: 10`, and one fixed `client-output.txt`
- [ ] Proxy publishes `1099:1099` on all interfaces — only needed if something outside compose calls it
- [ ] `.dockerignore` still has DockerTest comments/entries — tidy up
- [ ] `scripts/test-all.sh` refers to old profiles/services (`server-zone1…`, `--profile all`) — update or drop
- [ ] `rundatshit.sh` / `.ps1` — rename before handing in?
- [ ] `docker save` tested: file size + `docker load` on a clean machine/WSL

## 9. Housekeeping before hand-in

- [ ] README run instructions match the final pom (`assignment1.jar`, Java 21) and compose
- [ ] CRLF-only modifications not committed; `.sh` files stay LF
- [ ] Everyone's branch merged to `main`; the zip is built from `main` — as of the 17:40 fetch every remote branch (`eirik`, `oscarhr`, `simpledocker`, `vetle2209…`, `vetle23092601`) and `kine` is merged; recheck after tonight's pushes
- [ ] Devilry: submitted before 23:59, whole group registered
