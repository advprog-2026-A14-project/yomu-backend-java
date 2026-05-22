# API Service Java - FE Consumption Context

Base URL lokal default: `http://localhost:8081`

Semua response memakai envelope:

```json
{
  "success": true,
  "message": "Pesan operasi",
  "data": {}
}
```

Error memakai format yang sama dengan `success: false`. Field JSON memakai `snake_case`.

Header auth untuk endpoint protected:

```http
Authorization: Bearer <access_token>
Content-Type: application/json
```

Token JWT hanya valid bila akun masih ada, belum dihapus, dan `token_version` masih sama. Setelah password berubah, token lama menjadi tidak valid.

## Role Summary

- `PELAJAR`: membaca artikel, mengambil quiz, submit quiz satu kali per artikel, membuat/edit/hapus komentar sendiri, memberi reaksi forum.
- `ADMIN`: mengelola artikel dan quiz, melihat forum, menghapus komentar siapa pun untuk moderasi.

## Auth

### Register Pelajar

`POST /api/v1/auth/register`

Body:

```json
{
  "username": "budi",
  "display_name": "Budi Santoso",
  "password": "rahasia123",
  "email": "budi@example.com",
  "phone_number": "+628123456789"
}
```

`email` atau `phone_number` minimal salah satu wajib diisi.

Response `data`:

```json
{
  "access_token": "<jwt>",
  "user": {
    "user_id": "uuid",
    "username": "budi",
    "display_name": "Budi Santoso",
    "email": "budi@example.com",
    "phone_number": "+628123456789",
    "role": "PELAJAR"
  }
}
```

### Login

`POST /api/v1/auth/login`

Body:

```json
{
  "identifier": "budi",
  "password": "rahasia123"
}
```

`identifier` bisa `username`, `email`, atau `phone_number`.

### Login Google

`POST /api/v1/auth/google`

Body:

```json
{
  "id_token": "<google_id_token>",
  "username": "budi_google",
  "display_name": "Budi Santoso"
}
```

## Current User

### Get Profile

`GET /api/v1/users/me`

Auth: `PELAJAR` atau `ADMIN`

### Update Profile

`PATCH /api/v1/users/me`

Body minimal salah satu:

```json
{
  "username": "budi_baru",
  "display_name": "Budi Baru"
}
```

### Update Login Identifiers

`PATCH /api/v1/users/me/login-identifiers`

Body minimal salah satu:

```json
{
  "email": "budi.baru@example.com",
  "phone_number": "+628111111111"
}
```

### Update Password

`PATCH /api/v1/users/me/password`

Body:

```json
{
  "current_password": "rahasia123",
  "new_password": "rahasia456"
}
```

### Delete Account

`DELETE /api/v1/users/me`

Akun di-soft-delete. Token aktif setelah itu akan ditolak.

### Batch User Lookup

`GET /api/v1/users/batch?ids=<uuid1>,<uuid2>`

Auth: `PELAJAR` atau `ADMIN`

Dipakai FE bila perlu hydrate data user tambahan.

## Bacaan dan Kuis

### List Artikel

`GET /api/v1/articles?category=News`

Auth: `PELAJAR`

Response `data[]`:

```json
{
  "id": "art-news-001",
  "title": "Judul Bacaan",
  "content": "Isi bacaan",
  "category": "News"
}
```

### Detail Artikel

`GET /api/v1/articles/{id}`

Auth: `PELAJAR`

### Ambil Soal Quiz

`GET /api/v1/quizzes/{article_id}`

Auth: `PELAJAR`

Jika pelajar sudah pernah menyelesaikan quiz artikel ini, API mengembalikan `409`.

Response `data[]` tidak membawa jawaban benar:

```json
{
  "id": "quiz-news-001-1",
  "article_id": "art-news-001",
  "question": "Apa ide utama teks?",
  "options": "A;B;C;D"
}
```

### Submit Quiz

`POST /api/v1/quizzes/{article_id}/submit`

Auth: `PELAJAR`

Body harus mencakup seluruh `quiz_id` dari artikel tersebut:

```json
{
  "answers": [
    {
      "quiz_id": "quiz-news-001-1",
      "answer": "A"
    },
    {
      "quiz_id": "quiz-news-001-2",
      "answer": "C"
    }
  ]
}
```

Response `data`:

```json
{
  "score": 100.0,
  "accuracy": 100.0,
  "correct_count": 2,
  "total_questions": 2
}
```

Submit hanya bisa dilakukan sekali per user per artikel. Setelah submit sukses, backend menyimpan attempt dengan `user_id` dari token, lalu melakukan sync progress quiz ke service Rust. Jika sync gagal, data attempt tetap tersimpan dan event dicatat ke outbox.

## Admin Artikel dan Quiz

Semua endpoint bagian ini butuh role `ADMIN`.

### Create Artikel

`POST /api/v1/admin/articles`

```json
{
  "id": "art-news-001",
  "title": "Judul Bacaan",
  "content": "Isi bacaan",
  "category": "News"
}
```

### Update Artikel

`PATCH /api/v1/admin/articles/{id}`

Body minimal salah satu:

```json
{
  "title": "Judul Baru",
  "content": "Isi baru",
  "category": "News"
}
```

### Delete Artikel

`DELETE /api/v1/admin/articles/{id}`

Menghapus artikel beserta quiz, attempt, komentar forum, dan reaksi terkait.

### Create Quiz

`POST /api/v1/admin/articles/{article_id}/quizzes`

```json
{
  "id": "quiz-news-001-1",
  "question": "Apa ide utama teks?",
  "options": "A;B;C;D",
  "answer": "A"
}
```

### Update Quiz

`PATCH /api/v1/admin/quizzes/{id}`

```json
{
  "question": "Pertanyaan baru?",
  "options": "A;B;C;D",
  "answer": "B"
}
```

### Delete Quiz

`DELETE /api/v1/admin/quizzes/{id}`

## Forum

Forum terikat ke artikel lewat `article_id`. Komentar dan reaksi memakai `user_id` dari token, bukan dari body.

### List Comments

`GET /api/v1/forums/{articleId}/comments`

Auth: `PELAJAR` atau `ADMIN`

Response `data[]` berupa tree komentar:

```json
{
  "id": "comment-uuid",
  "article_id": "art-news-001",
  "user_id": "author-uuid",
  "parent_comment_id": null,
  "content": "Komentar",
  "created_at": "2026-05-22T09:00:00Z",
  "updated_at": "2026-05-22T09:00:00Z",
  "author": {
    "user_id": "author-uuid",
    "username": "budi",
    "display_name": "Budi Santoso",
    "role": "PELAJAR"
  },
  "reaction_count": 3,
  "upvote_count": 3,
  "downvote_count": 0,
  "emoji_count": 1,
  "clan_name": "Clan A",
  "tier": "Gold",
  "replies": []
}
```

`reaction_count` adalah jumlah upvote untuk kompatibilitas lama. Gunakan `upvote_count`, `downvote_count`, dan `emoji_count` untuk UI baru.

### Create Comment atau Reply

`POST /api/v1/forums/{articleId}/comments`

Auth: `PELAJAR`

Root comment:

```json
{
  "content": "Komentar baru"
}
```

Reply:

```json
{
  "parent_comment_id": "comment-uuid",
  "content": "Balasan komentar"
}
```

`parent_comment_id` harus berasal dari artikel yang sama.

### Update Comment

`PUT /api/v1/forums/comments/{commentId}`

Auth: `PELAJAR`, hanya pemilik komentar.

```json
{
  "content": "Komentar diperbarui"
}
```

### Delete Comment

`DELETE /api/v1/forums/comments/{commentId}`

Auth:

- `PELAJAR`: hanya komentar sendiri.
- `ADMIN`: komentar siapa pun.

### Toggle Reaction

`POST /api/v1/forums/comments/{commentId}/reactions`

Auth: `PELAJAR`

```json
{
  "reaction_type": "UPVOTE"
}
```

Nilai `reaction_type`: `UPVOTE`, `DOWNVOTE`, `EMOJI`.

Endpoint ini bersifat toggle. Jika reaction yang sama sudah ada, backend akan menghapus reaction tersebut. Jika user mengirim `UPVOTE` saat sudah punya `DOWNVOTE`, downvote akan diganti menjadi upvote, dan sebaliknya.

Response:

```json
{
  "comment_id": "comment-uuid",
  "reaction_type": "UPVOTE",
  "reacted": true,
  "reaction_count": 1
}
```

### Reaction Summary

`GET /api/v1/forums/comments/{commentId}/reactions`

Auth: `PELAJAR` atau `ADMIN`

Response:

```json
{
  "comment_id": "comment-uuid",
  "upvote_count": 3,
  "downvote_count": 1,
  "emoji_count": 2
}
```

## FE Flow Notes

1. Login/register, simpan `access_token`.
2. Untuk pelajar: ambil artikel, buka detail, ambil quiz, submit jawaban. Bila `GET /quizzes/{article_id}` atau submit mengembalikan `409`, tampilkan status selesai dan jangan buka quiz lagi.
3. Untuk forum: panggil list comments dengan token, render `replies` secara nested, gunakan `author.display_name` untuk nama tampil.
4. Untuk admin: gunakan endpoint `/api/v1/admin/**`; token pelajar akan mendapat `403`.
5. Tangani status umum: `400` validasi body, `401` token hilang/invalid/stale, `403` role salah atau bukan owner, `404` resource tidak ditemukan, `409` konflik seperti quiz sudah selesai atau ID sudah dipakai.
