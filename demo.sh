#!/usr/bin/env bash
# One command. Builds the app image, starts PostgreSQL + the seeded app, ready at localhost:8080.
set -euo pipefail
cd "$(dirname "$0")"
exec docker compose -f docker-compose.demo.yml up --build
