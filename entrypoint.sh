#!/bin/sh

if [ "$APP_ROLE" = "proxy" ]; then
    exec java -cp /in5020/target/assignment1.jar com.group2.proxy.Proxy
elif [ "$APP_ROLE" = "server" ]; then
    exec java -Djava.rmi.server.hostname="$SERVER_HOST" -cp /in5020/target/assignment1.jar com.group2.server.Server "$SERVER_ZONE"
elif [ "$APP_ROLE" = "client" ]; then
    exec java -cp /in5020/target/assignment1.jar com.group2.client.Client
else
    echo "Set APP_ROLE=proxy|server|client"
    exit 1
fi