# IN5020 Group 2

Akkurat nå så er dette basically bare en clone av eksempel prosjektet vi har fått fra TA

## Requirements
* Java 21 (JDK)
* Maven

## Installing and running
```bash
mvn clean compile jar:jar

# Now open three other terminals and run these commands in each
## Terminal 1 (RMI registry)
cd ./target/classes
rmiregistry

## Terminal 2
java -cp ./target/ass1-1.0-SNAPSHOT.jar com.group2.server.Server

## Terminal 3
java -cp ./target/ass1-1.0-SNAPSHOT.jar com.group2.client.Client
```