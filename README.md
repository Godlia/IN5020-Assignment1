# IN5020 Group 2

## How to run

### Requirements
#### System requirements for running computer
DockerDesktop

#### Main tool requirements (included in project)
* Java 21 (JDK)
* Maven (A wrapper has been included for convenience)

## Installing and running

If on Windows, run:
```bash
rundatshit.ps1
```
If on Linux or Mac, run:
```bash
rundatshit.sh
```



## Workload distribution

* Eirik
  * Client, query-parsing and tester
  * Processing Server, logic and number crunching
* Oscar
  * Proxy / load balancer
  * Servers registration
  * Clients request to Proxy
* Vetle
  * Dockerization
  * Docker Compose
* Kine
  * Caching