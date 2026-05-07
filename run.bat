@echo off
echo Building and starting all microservices...
docker compose up --build -d
echo All services are starting up. Use 'docker compose logs -f' to watch the logs.
pause
