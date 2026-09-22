#!/bin/sh
# Health-check helper:  listening <port> [tcp|udp]
# Exits 0 when a socket in this container is listening on <port>. It reads the kernel's socket
# tables in /proc/net/ (port numbers are in hex there), so no extra tools (curl, nc) are needed in the
# image, and it does not open a connection that the server would log as a strange client.
port=$(printf '%04X' "$1")
proto=${2:-tcp}
if [ "$proto" = "udp" ]; then state=07; else state=0A; fi   # 0A = TCP LISTEN, 07 = unconnected UDP
grep -qsi ":$port [0-9A-F]*:0000 $state" /proc/net/$proto /proc/net/${proto}6