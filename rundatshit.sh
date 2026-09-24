#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

cache_types=(FIFO LRU NAIVE)
client_delays_ms=(50 20)
max_parallel=3
projects=()
batch_pids=()
exit_status=0

echo "Building project..."
./mvnw clean compile package

echo "Stopping any previous run..."
docker compose down --remove-orphans

docker compose build --no-cache

cleanup() {
	for project in "${projects[@]}"; do
		docker compose -p "$project" down --remove-orphans >/dev/null 2>&1 || true
	done
}

trap cleanup EXIT

wait_for_batch() {
	for pid in "${batch_pids[@]}"; do
		if ! wait "$pid"; then
			exit_status=1
		fi
	done
	batch_pids=()
}

for cache_type in "${cache_types[@]}"; do
	for client_delay_ms in "${client_delays_ms[@]}"; do
		run_name="${cache_type}${client_delay_ms}"
		project="in5020-${run_name,,}"
		proxy_port=$((1100 + ${#projects[@]}))
		projects+=("$project")

		(
			if [[ "$cache_type" == "NAIVE" ]]; then
				server_cache_mode=NAIVE
				cache_policy=LRU
			else
				server_cache_mode=SERVER
				cache_policy="$cache_type"
			fi

			mkdir -p "output/$run_name"
			echo "Running cache=$cache_type delay=${client_delay_ms}ms (project=$project)"
			CACHE_TYPE="$cache_type" \
			CLIENT_DELAY_MS="$client_delay_ms" \
			RUN_NAME="$run_name" \
			SERVER_CACHE_MODE="$server_cache_mode" \
			CACHE_POLICY="$cache_policy" \
			PROXY_PORT="$proxy_port" \
			docker compose -p "$project" up -d
			docker compose -p "$project" wait client
			docker compose -p "$project" down --remove-orphans
		) &
		batch_pids+=("$!")

		if (( ${#batch_pids[@]} == max_parallel )); then
			wait_for_batch
		fi
	done
done

if (( ${#batch_pids[@]} > 0 )); then
	wait_for_batch
fi

exit "$exit_status"
