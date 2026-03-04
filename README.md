# Yomu Backend Java

## Prerequisites

- Java 21
- Docker Desktop + Docker Compose

## 1) Clone Repository

```bash
git clone <URL_REPO_BACKEND>
cd yomu-backend-java
```

## 2) Set Environment Variable Password DB

Buat file `.env` di root project:

```env
DB_PASSWORD=your pass
```

`DB_PASSWORD` ini dipakai oleh:
- `docker-compose.yml` (`POSTGRES_PASSWORD`)
- `application.properties` (`spring.datasource.password=${DB_PASSWORD}`)

## 3) Jalankan PostgreSQL dengan Docker Compose

```bash
docker compose up -d
docker compose ps
```

Container database akan expose port:
- Host: `localhost:5433`
- Container: `5432`

## 4) Jalankan Backend

```bash
./gradlew bootRun
```

Untuk Windows PowerShell:

```powershell
./gradlew.bat bootRun
```

Jika tidak bisa run (mis. port `8080` sedang dipakai), jalankan di port lain:

```bash
./gradlew bootRun --args="--server.port=8081"
```

Untuk Windows PowerShell:

```powershell
./gradlew.bat bootRun --args="--server.port=8081"
```

## 5) Test API untuk FE

Endpoint utama:

- `GET http://localhost:8080/api/bacaankuis`

Jika backend dan DB berhasil terkoneksi, response awal biasanya:

```json
[]
```

Response `[]` artinya endpoint sudah aktif dan data masih kosong. Endpoint ini siap dipakai FE.

