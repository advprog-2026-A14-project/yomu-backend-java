# Forum API Contract

## Response Contract
- Semua endpoint harus mengembalikan wrapper JSON:
  - Sukses + data: `{"success": true, "message": "...", "data": ...}`
  - Sukses tanpa data: `{"success": true, "message": "..."}`
  - Error: `{"success": false, "message": "..."}`
- Dilarang return raw object atau raw array dari controller.
- Semua key JSON harus `snake_case`.

## Auth Contract
- Semua endpoint forum wajib header `Authorization: Bearer <JWT>` kecuali GET comments.
- JWT minimal berisi claims:
  - `sub`: `user_id` dalam format UUID string
  - `role`: `PELAJAR` atau `ADMIN`
  - `iat`
  - `exp`

---

## Comment Endpoints

### Create Comment
- Method & path: `POST /api/v1/forums/{article_id}/comments`
- Wajib header `Authorization: Bearer <JWT>`
- Request body:
  - `content` (required, max 5000 karakter)
  - `parent_comment_id` (optional, UUID — jika diisi maka ini adalah reply)
- Behavior:
  - Jika `parent_comment_id` diisi, comment menjadi reply dari comment tersebut
  - Jika `parent_comment_id` null, comment menjadi root comment
- Success response (`201`):
```json
{
  "success": true,
  "message": "Komentar berhasil dibuat",
  "data": {
    "id": "<uuid>",
    "article_id": "<uuid>",
    "user_id": "<uuid>",
    "parent_comment_id": null,
    "content": "...",
    "created_at": "2026-01-01T00:00:00Z",
    "reaction_count": 0
  }
}
```
- Error response:
  - `400` jika `content` kosong atau melebihi 5000 karakter
  - `401` jika tidak ada token / token tidak valid
  - `404` jika `parent_comment_id` tidak ditemukan

### Get Comments by Article
- Method & path: `GET /api/v1/forums/{article_id}/comments`
- Tidak wajib login (public)
- Behavior:
  - Mengembalikan root comments beserta replies (nested)
  - Diurutkan dari terbaru (`created_at DESC`)
- Success response (`200`):
```json
{
  "success": true,
  "message": "Komentar berhasil diambil",
  "data": [
    {
      "id": "<uuid>",
      "article_id": "<uuid>",
      "user_id": "<uuid>",
      "parent_comment_id": null,
      "content": "...",
      "created_at": "2026-01-01T00:00:00Z",
      "reaction_count": 3,
      "replies": [
        {
          "id": "<uuid>",
          "article_id": "<uuid>",
          "user_id": "<uuid>",
          "parent_comment_id": "<uuid>",
          "content": "...",
          "created_at": "2026-01-01T00:00:00Z",
          "reaction_count": 0,
          "replies": []
        }
      ]
    }
  ]
}
```

### Update Comment
- Method & path: `PUT /api/v1/forums/comments/{comment_id}`
- Wajib header `Authorization: Bearer <JWT>`
- Request body:
  - `content` (required, max 5000 karakter)
- Behavior:
  - Hanya pemilik comment yang bisa update
- Success response (`200`):
```json
{
  "success": true,
  "message": "Komentar berhasil diperbarui",
  "data": {
    "id": "<uuid>",
    "article_id": "<uuid>",
    "user_id": "<uuid>",
    "parent_comment_id": null,
    "content": "...",
    "created_at": "2026-01-01T00:00:00Z",
    "reaction_count": 3
  }
}
```
- Error response:
  - `400` jika `content` kosong atau melebihi 5000 karakter
  - `401` jika tidak ada token / token tidak valid
  - `403` jika bukan pemilik comment
  - `404` jika comment tidak ditemukan

### Delete Comment
- Method & path: `DELETE /api/v1/forums/comments/{comment_id}`
- Wajib header `Authorization: Bearer <JWT>`
- Behavior:
  - Pemilik comment bisa delete miliknya sendiri
  - ADMIN bisa delete comment siapapun
  - Replies ikut terhapus (cascade)
- Success response (`200`):
```json
{
  "success": true,
  "message": "Komentar berhasil dihapus"
}
```
- Error response:
  - `401` jika tidak ada token / token tidak valid
  - `403` jika bukan pemilik comment dan bukan ADMIN
  - `404` jika comment tidak ditemukan

---

## Reaction Endpoints

### Add / Toggle Reaction
- Method & path: `POST /api/v1/forums/comments/{comment_id}/reactions`
- Wajib header `Authorization: Bearer <JWT>`
- Request body:
  - `reaction_type` (required, nilai valid: `UPVOTE`)
- Behavior:
  - Jika user belum pernah memberi reaksi dengan `reaction_type` tersebut → tambah reaksi
  - Jika user sudah memberi reaksi dengan `reaction_type` yang sama → hapus reaksi (toggle)
  - Satu user hanya bisa memberi satu reaksi per `reaction_type` per comment
- Success response (`200`):
```json
{
  "success": true,
  "message": "Reaksi berhasil diperbarui",
  "data": {
    "comment_id": "<uuid>",
    "reaction_type": "UPVOTE",
    "reacted": true,
    "reaction_count": 5
  }
}
```
- Error response:
  - `400` jika `reaction_type` tidak valid
  - `401` jika tidak ada token / token tidak valid
  - `404` jika comment tidak ditemukan

### Get Reactions by Comment
- Method & path: `GET /api/v1/forums/comments/{comment_id}/reactions`
- Tidak wajib login (public)
- Success response (`200`):
```json
{
  "success": true,
  "message": "Reaksi berhasil diambil",
  "data": {
    "comment_id": "<uuid>",
    "reactions": [
      {
        "reaction_type": "UPVOTE",
        "count": 5
      }
    ]
  }
}
```
- Error response:
  - `404` jika comment tidak ditemukan

---

## Reaction Type Values
Nilai `reaction_type` yang valid saat ini:
- `UPVOTE`

> Catatan: `reaction_type` menggunakan `varchar` di database sehingga nilai baru dapat ditambahkan di masa depan tanpa perubahan skema.