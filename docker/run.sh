#!/bin/sh
# Entrypoint of the container image: run a Java main class with the image's classpath.
#   docker compose run --rm client com.group2.client.Client
# JAVA_OPTS (env) carries JVM flags such as -Djava.rmi.server.hostname=server-zone1
# `exec` replaces the shell with java, so java becomes PID 1 and receives docker's SIGTERM.
set -e
if [ "$#" -eq 0 ]; then
  echo "usage: <main class> [args...]   e.g. com.group2.server.Server" >&2
  exit 2
fi
exec java $JAVA_OPTS -cp "$APP_CLASSPATH" "$@"
