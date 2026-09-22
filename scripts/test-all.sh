#!/usr/bin/env bash
# Run each assignment profile, wait for "result" containers to exit, and report OK/FAIL.
#   bash scripts/test-all.sh              # Run every profile
#   bash scripts/test-all.sh proxy client # Only run specific ones
# Build first (or let this script do it): docker compose --profile all build
set -u
cd "$(dirname "$0")/.."

# profile -> the services whose exit code decides OK/FAIL (servers keep running and are ignored)
declare -A RESULT=(
    [proxy]="proxy"
    [server]="server-zone1 server-zone2 server-zone3 server-zone4 server-zone5"
    [client]="client"
)
ORDER="proxy server client"
PROFILES="${*:-$ORDER}"
TIMEOUT=${TIMEOUT:-120}

if [ "${SKIP_BUILD:-0}" != "1" ]; then
  docker compose --profile all build || exit 1
fi

# Start clean
docker compose --profile all down -t 2 --remove-orphans >/dev/null 2>&1

summary=""
for p in $PROFILES; do
  echo "================ $p ================"
  docker compose --profile "$p" up -d --no-build --force-recreate >/dev/null 2>&1 \
    || docker compose --profile "$p" up -d --no-build --force-recreate
  svcs=${RESULT[$p]}
  status="OK"
  for s in $svcs; do
    # Wait until every container of the service has exited
    for ((t = 0; t < TIMEOUT; t++)); do
      running=$(docker compose --profile "$p" ps -a --status running --status created --status restarting -q "$s" | wc -l)
      [ "$running" -eq 0 ] && break
      sleep 1
    done
    codes=$(docker compose --profile "$p" ps -a --format '{{.ExitCode}}' "$s")
    [ -z "$codes" ] && status="FAIL(no $s container)"
    for c in $codes; do [ "$c" != "0" ] && status="FAIL($s exit $c)"; done
    [ "$running" -ne 0 ] && status="FAIL($s still running after ${TIMEOUT}s)"
  done
  docker compose --profile "$p" logs --no-color 2>/dev/null | grep -v "^\s*$" | tail -n 40
  docker compose --profile "$p" down -t 2 >/dev/null 2>&1
  echo "---> $p: $status"
  summary+=$(printf '%-14s %s' "$p" "$status")$'\n'
done

echo
echo "================ summary ================"
printf '%s' "$summary"
! grep -q FAIL <<<"$summary"