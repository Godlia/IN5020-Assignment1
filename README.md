# IN5020 Group 2

Akkurat nå så er dette basically bare en clone av eksempel prosjektet vi har fått fra TA


## Arbeidsfordeling

* Eirik
  * Client, query-parsing og tester
  * Processing Server, logikk og number crunching
* Oskar
  * Proxy server, loadbalancing
  * Client/Server registration
* Vetle
  * Dockerization
  * Docker Compose
* Kine
  * Caching

**Uklare oppgaver igjen:**

* Køteknikk

Hvis det kommer noe mer opp så bare skriv det her.

Jeg tror ikke det skader at vi har noen ansvar som overlapper, generelt lurt at all kode som blir skrevet blir dobbeltsjekket av noen andre.

## Requirements

* Java 21 (JDK)
* Maven

## Installing and running

All commands are run in the root folder

```bash
mvn clean compile jar:jar
```

### Now open three other terminals and run these commands in each

#### Terminal 1 (RMI registry)

```bash
cd ./target/classes
rmiregistry
```

#### Terminal 2

```bash
java -cp ./target/ass1-1.0-SNAPSHOT.jar com.group2.server.Server
```

#### Terminal 3

```bash
java -cp ./target/ass1-1.0-SNAPSHOT.jar com.group2.client.Client
```
