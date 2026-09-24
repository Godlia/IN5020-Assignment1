#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

backup=false
if (( $# > 1 )) || { (( $# == 1 )) && [[ "$1" != "--backup" ]]; }; then
	echo "Usage: $0 [--backup]" >&2
	exit 2
fi
if [[ "${1:-}" == "--backup" ]]; then
	backup=true
fi

if [[ "$backup" == true ]]; then
	cache_types=(naive-server cache-server client-cache)
else
	cache_types=(FIFO LRU NAIVE)
fi
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
	# ${arr[@]+...} avoids "unbound variable" on empty arrays under set -u in bash 3.2
	for project in ${projects[@]+"${projects[@]}"}; do
		docker compose -p "$project" down --remove-orphans >/dev/null 2>&1 || true
	done
}

trap cleanup EXIT

wait_for_batch() {
	for pid in ${batch_pids[@]+"${batch_pids[@]}"}; do
		if ! wait "$pid"; then
			exit_status=1
		fi
	done
	batch_pids=()
}

for cache_type in "${cache_types[@]}"; do
	for client_delay_ms in "${client_delays_ms[@]}"; do
		if [[ "$backup" == true ]]; then
			run_name="${cache_type}${client_delay_ms}"
			output_filename="${cache_type}.txt"
			case "$cache_type" in
				naive-server)
					server_cache_type=NAIVE
					client_cache_mode=NAIVE
					;;
				cache-server)
					server_cache_type=FIFO
					client_cache_mode=NAIVE
					;;
				client-cache)
					server_cache_type=NAIVE
					client_cache_mode=FIFO
					;;
			esac
		else
			run_name="${cache_type}${client_delay_ms}"
			output_filename=client-output.txt
			server_cache_type="$cache_type"
			client_cache_mode="$cache_type"
		fi
		project="in5020-$(printf '%s' "$run_name" | tr '[:upper:]' '[:lower:]')"
		proxy_port=$((1100 + ${#projects[@]}))
		projects+=("$project")

		(
			if [[ "$server_cache_type" == "NAIVE" ]]; then
				server_cache_mode=NAIVE
				cache_policy=LRU
			else
				server_cache_mode=SERVER
				cache_policy="$server_cache_type"
			fi

			mkdir -p "output/$run_name"
			echo "Running cache=$cache_type delay=${client_delay_ms}ms (project=$project)"
			CACHE_TYPE="$cache_type" \
			SERVER_CACHE_TYPE="$server_cache_type" \
			CLIENT_CACHE_MODE="$client_cache_mode" \
			CLIENT_DELAY_MS="$client_delay_ms" \
			RUN_NAME="$run_name" \
			OUTPUT_FILENAME="$output_filename" \
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
