@echo off
REM One command. Builds the app image, starts PostgreSQL + the seeded app, ready at localhost:8080.
cd /d "%~dp0"
docker compose -f docker-compose.demo.yml up --build
