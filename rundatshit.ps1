docker compose down
mvn clean compile package
docker compose build --no-cache
docker compose up -d