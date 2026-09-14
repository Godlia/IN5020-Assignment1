# IN5020 Group 2

Akkurat nå så er dette basically bare en clone av eksempel prosjektet vi har fått fra TA


## Arbeidsfordeling

* Eirik
  * Client, query-parsing og tester
  * Processing Server, logikk og number crunching
* Oscar
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

Linux / WSL / macOS:
```bash
./mvnw clean compile jar:jar
```
Windows:
```bash
.\mvnw.cmd clean compile jar:jar
```

**Temp forklaring til medstudenter**
Jeg leste meg opp litt, og forstod at det er lurt å ha en Maven wrapper, altså en Maven i prosjektet, så vi deler Maven versjon og øker reproduserbarhet. Jeg har ingen erfraring med dette, men prøvde å sette det opp. Åpen for tilbakemeldinger \
\- Oscar

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
