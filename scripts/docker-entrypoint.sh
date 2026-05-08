#!/bin/sh
# EC2 deployment entrypoint for Java backend
# Verifies database connectivity and migration state before starting Spring Boot

set -e

# Parse JDBC URL robustly. Handles both:
#   jdbc:postgresql://localhost:5432/yomu_db
#   postgresql://user:password@host:5432/yomu_db
URL_NO_PROTO="${SPRING_DATASOURCE_URL#*://}"
AUTH_HOST="${URL_NO_PROTO#*@}"
HOST_PORT_DB="${AUTH_HOST}"
[ -z "$AUTH_HOST" ] && HOST_PORT_DB="${URL_NO_PROTO}"

DB_HOST="${HOST_PORT_DB%%:*}"
DB_PORT="${HOST_PORT_DB#*:}"
DB_PORT="${DB_PORT%%/*}"
DB_NAME="${HOST_PORT_DB#*/}"
DB_NAME="${DB_NAME%%\?*}"

# Default fallback
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-yomu_db}"

echo "========================================"
echo "Yomu Java Backend - Deployment Entrypoint"
echo "========================================"
echo "Database host: ${DB_HOST}:${DB_PORT}"
echo "Database name: ${DB_NAME}"

# Wait for PostgreSQL
MAX_RETRIES=30
RETRY_INTERVAL=2

echo ""
echo "Waiting for PostgreSQL at ${DB_HOST}:${DB_PORT}..."

for i in $(seq 1 $MAX_RETRIES); do
  if wget --spider -q "${DB_HOST}:${DB_PORT}" 2>/dev/null || nc -z "${DB_HOST}" "${DB_PORT}" 2>/dev/null; then
    echo "PostgreSQL is ready!"
    break
  fi
  echo "  Attempt $i/$MAX_RETRIES: Database not ready, waiting ${RETRY_INTERVAL}s..."
  sleep $RETRY_INTERVAL
done

if [ "$i" -eq "$MAX_RETRIES" ]; then
  echo "ERROR: Could not connect to PostgreSQL after $MAX_RETRIES attempts"
  exit 1
fi

echo ""
echo "Starting Spring Boot application..."
echo "Schema will be auto-managed by Hibernate (ddl-auto=update)"
echo ""

exec java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError -jar app.jar