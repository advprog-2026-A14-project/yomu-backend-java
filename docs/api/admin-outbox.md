# Admin Outbox API

## Authorization
- Semua endpoint wajib JWT role `ADMIN`.
- Role selain ADMIN akan mendapat `403` wrapper error.

## List Failed Sync Events
- Method & path: `GET /api/v1/admin/failed-sync-events`
- Success response:
```json
{
  "success": true,
  "message": "Daftar failed sync events berhasil diambil",
  "data": {
    "events": [
      {
        "event_id": 1,
        "event_type": "USER_SYNC",
        "payload_json": "{\"user_id\":\"<uuid>\"}",
        "status": "FAILED",
        "retry_count": 0,
        "last_error": "timeout",
        "created_at": "2026-03-06T00:00:00Z",
        "updated_at": "2026-03-06T00:00:00Z"
      }
    ]
  }
}
```

## Retry Failed Sync Events
- Method & path: `POST /api/v1/admin/failed-sync-events/retry`
- Request body:
  - retry by ids:
```json
{
  "event_ids": [1, 2, 3]
}
```
  - retry all:
```json
{
  "retry_all": true
}
```
- Success response:
```json
{
  "success": true,
  "message": "Retry failed sync events selesai",
  "data": {
    "processed_count": 3,
    "done_count": 2,
    "failed_count": 1
  }
}
```

## Retry Rules
- Untuk event `USER_SYNC`:
  - Rust status `201` atau `409` -> `status = DONE`
  - selain itu / exception -> `status = FAILED`, `retry_count + 1`, `last_error` diperbarui

## Scheduled Retry (Optional)
- Scheduler aktif setiap 5 menit (`0 */5 * * * *`).
- Memproses event `FAILED`/`PENDING` dengan `retry_count < outbox.retry.max-attempts` (default `5`).
