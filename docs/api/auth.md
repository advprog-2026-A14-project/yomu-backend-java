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
