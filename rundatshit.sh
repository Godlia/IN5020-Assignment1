#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Stopping containers..."
docker compose down

echo "Building project..."
mvn clean compile package

echo "Starting containers..."
docker compose build --no-cache
docker compose up -d
