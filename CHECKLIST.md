# IN5020 Assignment 1 — checklist against the handout

Source: `exercise_1.pdf` (H26 version, OneDrive `ass1-rmi.updated`). **Deadline: Thu 24.09.2026 23:59 on Devilry.**
Repo state checked: `origin/main` = `6a2c92c` (24.09 12:41), read only — not built or run.
Updated 24.09 ~17:30 with the TA's answers (below).
Boxes re-ticked 24.09 ~18:00 against `c003f94` (Eirik, 17:40, "Merge branch 'kine'").
Boxes re-ticked 24.09 ~19:05 against `3084034` (Eirik, 18:57, "Merge branch 'main'").
**Boxes re-ticked 24.09 ~19:55 against the six runs in `output/`** (written 19:32–19:43) + the working tree. `HEAD` = `origin/main` = `3084034` still (last fetch 19:00; a fresh fetch was not possible from here). Uncommitted on this machine: `rundatshit.ps1` (staged: `$maxParallel = 1`, stderr handling), `.dockerignore` (staged), `rundatshit.sh` **deleted** (unstaged), `runlinux.sh` + `runmac.sh` **untracked**. `target/assignment1.jar` rebuilt 19:29. The output files were analysed with a script (line counts, zones, server counts, queue maxima); nothing was re-run and nothing in the repo edited except this file.

Legend: `[x]` present in the code · `[ ]` missing / not done · ⚠ present but differs from the handout (question for the owner)

> The repo's own `exercise_1.pdf` is the **H25** handout (deadline 18.09.2025). Use the OneDrive copy.
> `git status` shows ~34 modified files; with `git diff --ignore-cr-at-eol` only the four files above have real changes — the rest is CRLF noise. Don't commit the noise.

---

## TA answers (truongl, course chat) — 24.09

- **FIFO and OLDEST are both run, separately** — not one or the other ("we do all the case seperately")
- **6 runs:** naive, FIFO, OLDEST at T = 50, then the same three at T = 20 — and in a cache run, client and server use the **same** policy
- **Data storage is our choice:** "any database to host the data, or even pure operations in java" → SQLite may stay
- **T = 50 first** → output files + graphs; **then T = 20**, results **added to the same files**, and a **new** graph

---

## What the six runs show (read from `output/`, 24.09 ~19:55)

Run with `rundatshit.ps1` at `$maxParallel = 1` — the timestamps are sequential (FIFO50 → NAIVE20, 19:32:50 → 19:42:54), so **no parallel-stack interference** in these numbers.

| Run | Lines | Total elapsed | Requests reaching servers (A+B+C+D+E) | Client-cache hits (3,166 − that) | Max queue (any server) | Redirected | Lines with the wrong zone |
|---|---|---|---|---|---|---|---|
| FIFO50 | 3,166 + 4 + 1 | 158.5 s | 2,559 | 607 | 5 | 0 | 365 |
| FIFO20 | 3,166 + 4 + 1 | 63.5 s | 2,741 | 425 | 6 | 0 | 352 |
| LRU50 | 3,166 + 4 + 1 | 158.5 s | 2,546 | 620 | 4 | 0 | 364 |
| LRU20 | 3,166 + 4 + 1 | 63.7 s | 2,730 | 436 | 9 | 0 | 360 |
| NAIVE50 | 3,166 + 4 + 1 | 158.5 s | 3,166 | 0 | 2 | 0 | 0 |
| NAIVE20 | 3,166 + 4 + 1 | 63.6 s | 3,166 | 0 | 7 | 0 | 0 |

- **T-pacing works:** 3,166 × 50 ms = 158.3 s and 3,166 × 20 ms = 63.3 s — the totals are the schedule plus the last reply
- **Server cache is visible:** min *execution* time is 91 ms in both naive runs and 82 ms in all four cache runs (800+ lines under 90 ms) — a server-cache hit skips the SQL but still pays the 80 ms latency sleep
- **Client cache hits fewer at T = 20** (~430 vs ~610): more queries are in flight before the first answer for a key is cached, so concurrent misses on the same key all go to a server
- **Queue never reaches 18** (max 9, LRU20) → the proxy's overload rule never fires; **0 redirects in all six runs**. Say it in the report — the load balancing is correct but unexercised at this load
- **Averages barely move between variants** (≈ 105–130 ms): the 80 ms latency dominates, and client-cache hits are counted with their *original* times (see 4)
- ⚠ **Bug visible in the cache runs' output:** 352–365 lines per cache run print a **different zone than the input line** (e.g. input `getPopulationofCountry Guatemala Zone:2` → output `… Guatemala Zone:3 …, processed by Server 3`). `CacheKey` is method + arguments, *no zone*, and a hit returns the stored `QueryResult`, whose `request.originalQuery()` is the earlier query. Naive runs: 0 such lines. Fix (print the current request, mark hits, time hits as ~0 ms) or explain in the report
- Two input lines have **no country** (`getPopulationofCountry Zone:3`, lines 229 and 402) → result **2,177,342** = the sum of the 168 dataset cities whose country name is empty. Consistent, but worth one sentence in the report
- `output/` root still holds the old 12:58 single run (`client-output.txt`, `server*-queue.log`, ISO timestamps) — delete or leave out of the zip

---

## 0. Must-have for tonight (blocks the submission)

- [x] **`main` compiles again** (`3084034`) — the duplicate `Object result` in `Client.executeQuery` from the `c003f94` merge is gone (`de35067`); jar rebuilt 19:29
- [ ] **Naive** variant (see 2.5): SQLite is allowed (TA) — write in the report what "naive" means in our code (`CACHE_TYPE=NAIVE` = both caches off)
- [x] Wire the **server cache** + **client cache** and a flag/env to switch them on (see 4) — one env `CACHE_TYPE` = `FIFO` | `LRU` | `NAIVE` switches both, same policy on client and servers — **confirmed by the runs** (server counts and execution minima above)
- [ ] Measure **execution** and **waiting** time on the **server** (see 3.3) — unchanged, still client-side
- [ ] Produce `naive_server.txt`, `server_cache.txt`, `client_cache.txt` (see 5) — the six per-run files exist; renaming/merging into the handout's names is still open
- [x] **6 runs** (TA): naive / FIFO / OLDEST at **T = 50**, then the same three at **T = 20** — **done 19:32–19:43**, one after the other, all six complete (3,166 lines + summaries each)
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
- [x] **Server side of the refresh:** `Server.getQueueLength()` returns `requestQueue.size()` directly (`051effa`)
- ⚠ **Measured in all six runs:** max queue length **9** (LRU20), most runs ≤ 6 → the ≥ 18 rule never fires, **0 of 3,166 queries redirected** in every run, T = 20 included. Expected with 5 servers and one query per 20 ms — say it in the report (and that "processed by Server n" = the query's zone for that reason)
- [x] Proxy `VERBOSE` from env, default `false` (`6a7ebba`). The **server** still prints one line per request (`ServerRequestQueue.submit`) and the client prints "Cache hit for query …" — only console noise, the runs finished fine
- [ ] Doc comments at method level (handout: "must be well commented") — only `cache/` has Javadoc; `client/`, `proxy/`, `server/` have none

## 2. Server

- [x] Servant implements the four methods: `getPopulationofCountry`, `getNumberofCities`, `getNumberofCountries`, `getNumberofCountriesMM`
- [x] Handout examples re-checked (Norway 3,162,856 · Sweden 9,362,428 · Norway ≥100 000 → 4 · 2 cities ≥ 5 M → 7 · 30 cities 100k–800k → 30) — `ServerRepository` unchanged up to `3084034`
- [x] 2.a Stub registered under a name — every server binds `"server"` on **1099** in its own container (unique by host, not by name/port). Explain in the report why that satisfies "unique names on different ports"
- [x] 2.b Docker image for a group of 4 (see 8)
- [x] 3 Latency sleep **before** enqueuing: 80 ms + |requested zone − server zone| × 30 ms — the runs' minimum turnaround (83–84 ms with cache, 93–100 ms naive) matches 80 ms + X = 0 + the query
- ⚠ Server's zone is `0` until `registerWithProxy` returns — requests in that window get the wrong X
- [x] 4 Zone-specific waiting list, **FIFO** (`LinkedBlockingQueue`)
- [x] 4.c **One** execution thread; RMI's own threads act as the acceptor group
- [x] 5 Queue log per server, **per run**: `output/<RUN>/serverX-queue.log` — all 30 logs written, `received` / `enqueued` / `dequeued` events balance in every log
- [x] Log timestamps are **Unix epoch millis** (`656d90a`) — e.g. `1790271012706 received queue-size=0`, ready for the x-axis. Note: three lines per request (received/enqueued/dequeued) — pick which events to plot
- ⚠ **2.5 Naive variant:** handout = "server parses the whole dataset every time a request is made". The code imports the CSV into SQLite once at startup
  → **TA: "any database to host the data, or even pure operations in java"** — SQLite can stay. Still say in the report that naive = no cache, a full query per request

## 3. Client

- [x] Parses the input file (`QUERY_FILE` env, falls back to stdin)
- [x] Prints each result + times, and writes an output file
- [x] Line format `<result> <query> (turnaround time: … ms, execution time: … ms, waiting time: … ms, processed by Server <n>)`
- [x] **3.1 T-pacing:** one virtual thread per query, each sleeping until `i × T` — **confirmed**: 158.5 s at T = 50, 63.5 s at T = 20
- [x] **T = 50 / T = 20:** `rundatshit.ps1` sets 50 and 20 per run
- ⚠ **3.3 Timing definitions (Summary of measurements):**
  - *execution* = time the server spends running the request → currently the client's whole RMI call (latency + queue + execution)
  - *waiting* = time on the server's waiting list → currently `turnaround − execution` (≈ proxy lookup + client-side scheduling delay)
  - Needs the server to measure both and return them with the result — or explain the deviation in the report
- [x] 3.4 Per-method summary lines (avg turn-around / execution / waiting, min & max turn-around) — present in all six files
- ⚠ Handout says "6 entries" for 4 methods — note the quirk in the report; there's also an extra `Total elapsed time` line
- [x] Starts only after all 5 servers have registered — **not guaranteed** by the healthcheck (unchanged, see 8), but **no run hit `No server available`**: all six runs have all 3,166 lines. Leave as a report note
- [ ] Leftovers in `Client.java`: the debug `System.out.println(CACHE_ENABLED)` in `main` (l. 61), and the Norwegian "OSCAR/EIRIK/VETLE -> HER TRENGS DET IMPLEMENTASJON" comment block in `executeQuery` (l. 165–168, now implemented) — remove before hand-in

## 4. Caching

- [x] `Cache` class with capacities **SERVER 150** / **CLIENT 45**, FIFO and OLDEST (LRU) eviction, unit tests
- [x] **Server-side cache used in `Server`** — measured: execution minimum drops from 91 ms (naive) to 82 ms (cache runs). A hit still pays the 80 ms + X·30 latency and waits its turn; only the SQL is skipped. Say so in the report
- [x] **Client-side cache used in `Client`** — measured: 425–620 of 3,166 queries never reach a server in the cache runs, 0 in the naive runs
  - ⚠ **a hit returns the stored `QueryResult` as is** → the output line repeats the **earlier query's zone, times and server** (352–365 wrong-zone lines per cache run, see the table above), the line cannot show that it was a hit, and the averages count the original turnaround instead of ~0 ms. Either fix (new `QueryResult` for the current request, times ≈ 0, maybe a "(client cache)" marker) and re-run the four cache runs, or document it
  - ⚠ `CacheKey` = method + arguments, **without the zone** — a deliberate choice is fine (the answer does not depend on the zone), but then the output must still print the current query
- [x] Starts empty, filled per request only — no pre-processing
- [x] Env to enable the caches — `CACHE_TYPE` per run. Leftovers: compose still also sets `CACHE_MODE`/`CACHE_POLICY` on the servers (ignored when `CACHE_TYPE` is set), and `.env` has `SYSTEM_CACHE_MODE="FIFO"`, which nothing reads
- [x] ~~`CACHE_MODE` is *either* `SERVER` *or* `CLIENT`~~ → resolved in favour of the TA's plan: in a `FIFO`/`LRU` run **both** caches are on with the same policy
- [x] Run **both** FIFO and OLDEST, in separate runs (TA) — `FIFO50/20`, `LRU50/20` done
- [x] One env per run sets the **same** policy on the client and on all five servers (`CACHE_TYPE`)

## 5. Output files

- [ ] `naive_server.txt` — = `NAIVE50/client-output.txt` + `NAIVE20/client-output.txt`
- [ ] `server_cache.txt`
- [ ] `client_cache.txt`
- ⚠ Client writes `output/<RUN>/client-output.txt` → six files (`FIFO50`, `FIFO20`, `LRU50`, `LRU20`, `NAIVE50`, `NAIVE20`), not the handout's names. Rename/merge before hand-in
- [x] One output line per input query — **3,166** lines + 4 summaries + total in all six files (input: last line has no newline; `wc -l` says 3,165)
- [ ] T = 20 results **added** to the T = 50 files (TA) — join `X50` + `X20` and mark where each T starts (a header line, or rely on the summary block)
- [x] ~~Truncating `PrintWriter` wipes the T = 50 results~~ → no longer a problem: each run has its own dir. Still truncates if the same `RUN_NAME` is run twice
- ⚠ **Open:** 6 runs but 3 file names — `FIFO` and `LRU` runs both have **both** caches on: which goes into `server_cache.txt` and which into `client_cache.txt`? Ask truongl, or pick a mapping and state it in the report (e.g. FIFO → `server_cache.txt`, OLDEST → `client_cache.txt`, or both policies in each file)

## 6. Graphs (T = 50 **and** T = 20)

- [ ] Turn-around time per query — x: query number, y: turn-around time (per variant) — data ready in all six files
- [ ] Queue length per server (×5) — x: Unix timestamp, y: queue length — data ready in all 30 logs
- [ ] One set of graphs after the T = 50 runs, a **new** set after the T = 20 runs (TA)
- [x] Queue logs no longer overwrite each other — per-run `output/<RUN>/serverX-queue.log`
- [ ] Plot script or notebook in the repo (`scripts/`?), so the graphs are reproducible

## 7. Deliverables (Devilry, one compressed file)

- [ ] Report PDF: design + implementation description
- [ ] Screenshots of client **and** server running
- [ ] **User guide**: compile, build, deploy, run — README has a short "How to run", but ⚠ it points Linux/Mac users to `rundatshit.sh`, which is **deleted in the working tree** (replaced by untracked `runlinux.sh` / `runmac.sh`, both still `max_parallel=3`). Decide the script names before committing
- [x] Workload division — written in the README (English, `78f4d72`); copy into the report
- [ ] Zip with everything needed to build and run: source, `pom.xml`, `mvnw`, dataset, input file
- [ ] Source commented at method/algorithm level
- [x] Ready-to-deploy jar — `target/assignment1.jar` (shaded, 14.2 MB) rebuilt 19:29; put it in the zip
- [ ] Ready-to-deploy **docker image file** (`docker save … -o in5020.tar`; if too big → OneDrive/Drive link in the submission)
- [ ] The three output files + graphs
- [ ] Any other artefact needed (compose file, `entrypoint.sh`, run scripts)

## 8. Docker / Container

- [x] `Dockerfile` (temurin 21 JRE) with jar, dataset and `entrypoint.sh`; role via `APP_ROLE`
- [x] `docker-compose.yml`: proxy + 5 servers + client, service-name DNS on one bridge network, `java.rmi.server.hostname` = service name
- [x] Healthchecks for proxy and servers; client waits for all six
- [x] `./output` volume for queue logs and client output — **all 36 files came out** (6 client outputs + 30 queue logs)
- ⚠ Image needs `target/assignment1.jar` built **on the host first** (the scripts run `mvnw package` first). The Dockerfile comment says "stage 1: compile with Maven", but there is no Maven stage — document the host build in the user guide
- ⚠ Each server has `build: .` + its own `image:` name, proxy and client get default names (per compose project) — which image goes to `docker save`? Consider one image name
- ⚠ Server healthcheck = "`server` is bound in my registry", which happens **before** `registerWithProxy` → healthy ≠ registered. **Did not bite in any of the six runs** (all complete), but it is a race, not a guarantee
- ⚠ Zone numbers follow registration order, so `serverA` is not guaranteed zone 1 — **seen in the runs**: the per-server request counts are permutations of each other between runs (e.g. 675 requests on serverA in NAIVE50 but on serverC in NAIVE20). "processed by Server n" is the zone, not the letter; say so in the report
- [x] Variant switches in compose — `RUN_NAME`, `CACHE_TYPE`, `CLIENT_DELAY_MS`, `PROXY_PORT`, one compose project per run
- [x] ~~`rundatshit.sh` runs 3 stacks in parallel~~ → the measured runs used `rundatshit.ps1` with **`$maxParallel = 1`** (staged, not committed) — commit that. ⚠ `runlinux.sh`/`runmac.sh` (untracked) and the committed `rundatshit.sh` still say `max_parallel=3` — set them to 1 too, so the user guide reproduces the same numbers
- [x] `rundatshit.ps1` now the 6-run version with `mvnw.cmd`, `RUN_NAME` and per-run output (the README's Windows path works)
- [ ] Proxy publishes `${PROXY_PORT:-1099}:1099` on all interfaces — nothing outside compose needs it; could be dropped
- [ ] `.dockerignore` tidy-up — `book-original` / `docker-tutorial-leftovers` removed (**staged, not committed**); the DockerTest comment is still there
- [ ] `scripts/test-all.sh` refers to old profiles/services (`server-zone1…`, `--profile all`) — update or drop
- [ ] `rundatshit.*` / `runlinux.sh` / `runmac.sh` — final names before handing in
- [ ] `docker save` tested: file size + `docker load` on a clean machine/WSL

## 9. Housekeeping before hand-in

- [ ] README run instructions match the final scripts — `.ps1` ✓, `.sh` ✗ (deleted locally, see 7)
- [ ] Commit the staged `rundatshit.ps1` + `.dockerignore`, decide on `rundatshit.sh` vs `runlinux.sh`/`runmac.sh`; CRLF-only modifications not committed; `.sh` files stay LF
- [ ] Everyone's branch merged to `main`; the zip is built from `main` — as of `3084034` all branches merged; recheck after tonight's pushes
- [ ] `output/` root: old 12:58 run (`client-output.txt`, `server*-queue.log`) — leave out of the zip
- [ ] Devilry: submitted before 23:59, whole group registered
