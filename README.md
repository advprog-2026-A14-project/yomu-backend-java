# Yomu Backend Java

Backend utama untuk modul autentikasi, user management, bacaan-kuis, dan integrasi sinkronisasi ke Rust engine.

## Prerequisites
- Java 21
- Docker Desktop + Docker Compose

## 1) Clone Repository
```bash
git clone <URL_REPO_BACKEND>
cd yomu-backend-java
```

## 2) Set Environment Variables
Buat file `.env` di root project:

```env
DB_PASSWORD=your_db_password
JWT_SECRET=your_min_32_char_secret
INTERNAL_API_KEY=your_internal_api_key
RUST_ENGINE_BASE_URL=http://localhost:8080
GOOGLE_OAUTH_CLIENT_ID=your_google_web_client_id.apps.googleusercontent.com
```

Kegunaan variabel:
- `DB_PASSWORD`: password PostgreSQL.
- `JWT_SECRET`: signing secret JWT untuk endpoint `/api/v1/...`.
- `INTERNAL_API_KEY`: API key untuk komunikasi internal Java -> Rust (`x-api-key`).
- `RUST_ENGINE_BASE_URL`: base URL Rust engine untuk user sync.
- `GOOGLE_OAUTH_CLIENT_ID`: audience validasi Google ID token (disarankan diisi untuk keamanan).

## 3) Jalankan PostgreSQL dengan Docker Compose
```bash
docker compose up -d
docker compose ps
```

Container database expose port:
- Host: `localhost:5433`
- Container: `5432`

## 4) Jalankan Backend
```bash
./gradlew bootRun
```

Windows PowerShell:
```powershell
./gradlew.bat bootRun
```

Jika port `8080` terpakai:
```bash
./gradlew bootRun --args="--server.port=8081"
```

Windows PowerShell:
```powershell
./gradlew.bat bootRun --args="--server.port=8081"
```

## 5) Verifikasi Build dan Test
```bash
./gradlew test
```

Windows PowerShell:
```powershell
./gradlew.bat test
```

## 6) Alur Kolaborasi Tim Setelah Merge ke `main`
Jika branch auth/infrastruktur sudah merge ke `main`, anggota tim modul lain wajib mulai dari state terbaru agar integrasi tidak patah.

Langkah kerja standar per anggota:
1. Sinkronkan branch lokal:
```bash
git checkout main
git pull origin main
```
2. Buat branch fitur modul masing-masing:
```bash
git checkout -b feature/<nama-modul-anda>
```
3. Jalankan backend dan test dulu untuk memastikan baseline hijau:
```bash
./gradlew test
./gradlew bootRun
```
4. Kerjakan fitur secara modular di package sendiri (contoh: `bacaankuis`, `modul-x`) tanpa ubah kontrak global auth.
5. Pastikan endpoint modul mengikuti kontrak response dan security (lihat bagian "Kontrak Integrasi Wajib").
6. Sebelum push, jalankan test lagi:
```bash
./gradlew test
```
7. Push branch + buat PR ke `main`.

## 7) Kontrak Integrasi Wajib untuk Semua Modul
Semua modul baru (termasuk `bacaankuis`) harus kompatibel dengan fondasi auth yang sudah ada.

Aturan wajib:
- Semua response API wajib wrapper: `{"success":..., "message":..., "data":...}`.
- JSON menggunakan `snake_case`.
- Jangan return raw array/object dari controller.
- Endpoint publik gunakan prefix `/api/v1/...`.
- Endpoint yang butuh login harus memakai JWT `Authorization: Bearer <token>`.
- Untuk akses user login, gunakan helper security/context yang sudah ada (bukan parsing token manual di controller).
- Untuk kasus error, lempar custom exception agar ditangani global exception handler (jangan HTML error default).

Contoh integrasi modul `bacaankuis` setelah pull:
- Jika endpoint `bacaankuis` butuh user login, tempatkan di `/api/v1/...` dan biarkan security filter existing memverifikasi JWT.
- Jika butuh `user_id` login, ambil dari `CurrentUser` di service layer.
- Jika modul perlu sinkronisasi lintas service, gunakan pola outbox agar gagal integrasi tidak merusak transaksi utama.

## 8) Penggunaan di Luar Repo (Consumer Integration)
Bagian ini untuk tim lain di luar repo Java (frontend, Rust service, QA).

Frontend / Client API:
- Login lokal: `POST /api/v1/auth/login`.
- Register lokal: `POST /api/v1/auth/register`.
- Login Google: `POST /api/v1/auth/google`.
- Profile saat ini: `GET /api/v1/users/me`.
- Update akun: `PATCH /api/v1/users/me`, `PATCH /api/v1/users/me/password`, `PATCH /api/v1/users/me/login-identifiers`.
- Delete akun: `DELETE /api/v1/users/me`.
- Batch user lookup: `GET /api/v1/users/batch?ids=...`.

Rust engine integration:
- Endpoint internal Rust dipanggil dari Java: `POST {RUST_ENGINE_BASE_URL}/api/internal/users/sync`.
- Header wajib: `x-api-key: <INTERNAL_API_KEY>`.
- Jika Rust down/timeout, Java tetap sukses ke client dan simpan event ke outbox (`failed_sync_events`).

Admin operational flow:
- `GET /api/v1/admin/failed-sync-events` untuk lihat event gagal.
- `POST /api/v1/admin/failed-sync-events/retry` untuk retry manual.
- Scheduler retry otomatis jalan tiap 5 menit untuk status `FAILED/PENDING` (bisa dikontrol lewat config).

## 9) Struktur Folder dan Fungsinya
Folder penting di `src/main/java/id/ac/ui/cs/advprog/yomubackendjava`:

- `auth`: login/register/google auth dan issuance JWT.
- `security`: filter JWT, security config, handler 401/403, helper current user.
- `user`: endpoint profile, update account, batch user lookup, mapper user DTO.
- `bacaankuis`: domain fitur bacaan/kuis.
- `outbox`: pencatatan failed sync event, retry service, scheduler retry.
- `integration`: adapter client ke service lain.
- `admin`: endpoint operasional admin (monitor/retry outbox).
- `common`: wrapper response, exception global, util/config shared lintas modul.
- `health`: endpoint health check.

Dokumentasi API:
- `docs/api/auth.md`
- `docs/api/admin-outbox.md`

## 10) Apakah Fondasi Ini Bisa Dipakai untuk Fitur Tambahan?
Ya, fondasi ini memang dibuat reusable.

Yang bisa langsung dipakai ulang:
- Wrapper response + exception handler global untuk modul baru.
- JWT security untuk endpoint user/admin.
- Outbox + retry flow untuk integrasi ke service eksternal lain, bukan hanya Rust user sync.
- Batch endpoint pattern untuk kebutuhan aggregasi data modul lain.
- Scheduler retry untuk background recovery task tambahan.

Rekomendasi saat menambah fitur baru:
- Buat package modular baru (contoh: `progress`, `recommendation`, `leaderboard`).
- Pakai kontrak API yang sama agar frontend tidak perlu special case per modul.
- Jika ada call antar service eksternal, pakai pola outbox/fault-tolerance yang sama.
