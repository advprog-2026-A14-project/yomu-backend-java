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

## Internal Endpoint Contract
- Semua endpoint internal Rust menggunakan prefix `/api/internal/...`.
- Wajib header `x-api-key: <ENV_SECRET>`.
- Header hilang atau invalid harus return `401` dengan wrapper error.

## Fault Tolerance Register Flow
- Java menyimpan user terlebih dahulu.
- Java mencoba sinkronisasi ke Rust.
- Jika sinkronisasi gagal/timeout:
  - transaksi simpan user tidak di-rollback
  - tetap return `200`
  - simpan event outbox ke `failed_sync_events`
  - tulis log level `ERROR`
