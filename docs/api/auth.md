# Auth API Contract Placeholder

## Response Contract
- Semua endpoint harus mengembalikan wrapper JSON:
  - Sukses + data: `{"success": true, "message": "...", "data": ...}`
  - Sukses tanpa data: `{"success": true, "message": "..."}`
  - Error: `{"success": false, "message": "..."}`
- Dilarang return raw object atau raw array dari controller.
- Semua key JSON harus `snake_case`.

## Public Endpoint Contract
- Semua endpoint public menggunakan prefix `/api/v1/...`.
- Wajib header `Authorization: Bearer <JWT>`.
- JWT minimal berisi claims:
  - `sub`: `user_id` dalam format UUID string
  - `role`: `PELAJAR` atau `ADMIN`
  - `iat`
  - `exp`

## Register Endpoint
- Method & path: `POST /api/v1/auth/register`
- Request body (`snake_case`):
  - `username` (required)
  - `display_name` (required)
  - `password` (required)
  - `email` (optional, format email jika diisi)
  - `phone_number` (optional)
- Aturan validasi tambahan:
  - minimal salah satu dari `email` atau `phone_number` wajib diisi
  - `username`, `email`, dan `phone_number` harus unik untuk user aktif (`deleted_at IS NULL`)
- Success response (`200`):
```json
{
  "success": true,
  "message": "Registrasi berhasil",
  "data": {
    "access_token": "<jwt>",
    "user": {
      "user_id": "<uuid>",
      "username": "...",
      "display_name": "...",
      "email": "...",
      "phone_number": "...",
      "role": "PELAJAR"
    }
  }
}
```
- Error response:
  - `400` jika `email` dan `phone_number` kosong
  - `409` jika `username`/`email`/`phone_number` sudah dipakai
  - Semua error wajib wrapper JSON

## Login Endpoint
- Method & path: `POST /api/v1/auth/login`
- Request body:
  - `identifier` (required) dapat berupa `username` / `email` / `phone_number`
  - `password` (required)
- Aturan resolusi identifier:
  - jika mengandung `@` -> email
  - jika diawali `+` atau numeric penuh -> phone number
  - selain itu -> username
- Success response (`200`):
```json
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "access_token": "<jwt>",
    "user": {
      "user_id": "<uuid>",
      "username": "...",
      "display_name": "...",
      "email": "...",
      "phone_number": "...",
      "role": "PELAJAR"
    }
  }
}
```
- Error response:
  - `401` jika identifier tidak ditemukan atau password salah
  - `401` jika akun SSO-only (`password_hash` kosong) dengan message aman
  - `403` jika akun ditemukan tapi `deleted_at` tidak null

## Me Endpoint
- Method & path: `GET /api/v1/users/me`
- Wajib header `Authorization: Bearer <JWT>`
- Success response (`200`):
```json
{
  "success": true,
  "message": "Profil pengguna berhasil diambil",
  "data": {
    "user_id": "<uuid>",
    "username": "...",
    "display_name": "...",
    "email": "...",
    "phone_number": "...",
    "role": "PELAJAR"
  }
}
```
- Error response:
  - `401` jika token tidak ada/tidak valid atau user tidak ditemukan
  - `403` jika akun sudah tidak aktif (`deleted_at` tidak null)

## Update Account Endpoints
- Method & path: `PATCH /api/v1/users/me`
  - Request body: `{"username": "...", "display_name": "..."}` (minimal salah satu field harus diisi)
  - Error: `400` jika payload kosong, `409` jika `username` sudah dipakai
  - Success `200`: wrapper + data user terbaru
- Method & path: `PATCH /api/v1/users/me/password`
  - Request body: `{"current_password":"...", "new_password":"..."}` (`new_password` wajib)
  - Untuk user local (`password_hash` sudah ada), `current_password` wajib benar
  - Untuk user SSO-only (`password_hash` null), `current_password` boleh null untuk set password pertama
  - Error: `401` jika current password salah
  - Success `200`: wrapper sukses tanpa `data`
- Method & path: `PATCH /api/v1/users/me/login-identifiers`
  - Request body: `{"email":"...", "phone_number":"..."}` (minimal salah satu field harus diisi)
  - Error: `400` jika payload kosong, `409` jika email/phone sudah dipakai
  - Success `200`: wrapper + data user terbaru

## Internal Endpoint Contract
- Semua endpoint internal Rust menggunakan prefix `/api/internal/...`.
- Wajib header `x-api-key: <ENV_SECRET>`.
- Header hilang atau invalid harus return `401` dengan wrapper error.

## Fault Tolerance Register Flow
- Java menyimpan user terlebih dahulu.
- Setelah user tersimpan, Java mencoba sinkronisasi ke Rust:
  - `POST {RUST_ENGINE_BASE_URL}/api/internal/users/sync`
  - header: `x-api-key: INTERNAL_API_KEY`
  - body: `{"user_id":"<uuid>"}`
- Jika sinkronisasi gagal/timeout:
  - transaksi simpan user tidak di-rollback
  - tetap return `200`
  - simpan event outbox ke `failed_sync_events` dengan `event_type=USER_SYNC`, `status=FAILED`
  - tulis log level `ERROR`
- Jika Rust mengembalikan `409 conflict`, perlakukan sebagai idempotent success (tanpa outbox baru).
