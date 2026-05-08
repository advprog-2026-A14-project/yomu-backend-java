# Yomu Backend Java API

Dokumen ini dibuat dari analisis kode controller, DTO, service, security config, dan dokumentasi parsial di `docs/api`. Semua contoh payload mengikuti konfigurasi aplikasi saat ini.

## Base URL

- Local default: `http://localhost:8081`
- Production: `https://yomu-backend-78a746322ad9.herokuapp.com`
- Staging: `https://yomu-backend-staging-45829228a35b.herokuapp.com`

Semua endpoint menerima dan mengembalikan JSON. Gunakan header:

```http
Content-Type: application/json
Accept: application/json
```

## Quick Start Frontend

Gunakan satu API client di frontend agar semua endpoint membaca wrapper response dengan cara yang sama.

Contoh environment frontend:

```env
NEXT_PUBLIC_YOMU_API_BASE_URL=http://localhost:8081
```

Jika backend dijalankan lewat `docker compose`, port host default dari `docker-compose.yml` adalah `http://localhost:8080`. Jika dijalankan langsung dengan `./gradlew bootRun`, default aplikasi adalah `http://localhost:8081`.

Contoh tipe dan helper TypeScript:

```ts
export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data?: T;
};

export class ApiError extends Error {
  status: number;
  payload?: ApiResponse<unknown>;

  constructor(status: number, message: string, payload?: ApiResponse<unknown>) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

type ApiFetchOptions = RequestInit & {
  token?: string | null;
};

const API_BASE_URL =
  process.env.NEXT_PUBLIC_YOMU_API_BASE_URL ?? "http://localhost:8081";

export async function apiFetch<T>(
  path: string,
  { token, headers, ...init }: ApiFetchOptions = {},
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });

  const payload = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !payload.success) {
    throw new ApiError(response.status, payload.message, payload);
  }

  return payload.data as T;
}

export async function apiFetchVoid(
  path: string,
  options?: ApiFetchOptions,
): Promise<void> {
  await apiFetch<never>(path, options);
}
```

Contoh pemakaian:

```ts
const auth = await apiFetch<AuthData>("/api/v1/auth/login", {
  method: "POST",
  body: JSON.stringify({
    identifier: "pelajar01",
    password: "secret123",
  }),
});

localStorage.setItem("access_token", auth.access_token);

const me = await apiFetch<User>("/api/v1/users/me", {
  token: auth.access_token,
});
```

Catatan konsumsi dari browser:

- Simpan `data.access_token` setelah login/register/Google login, lalu kirim sebagai `Authorization: Bearer <token>` untuk endpoint private.
- Endpoint sukses tanpa data menghilangkan field `data`; pakai helper `apiFetchVoid` untuk `DELETE`, update password, dan submit quiz.
- Endpoint internal `/api/internal/...` bukan untuk browser. Jangan expose `INTERNAL_API_KEY` ke frontend.
- CORS global belum dikonfigurasi di `SecurityConfig`. Untuk local frontend berbeda origin, gunakan dev proxy/rewrite frontend atau tambahkan konfigurasi CORS backend sebelum call langsung dari browser.
- Forum saat ini memakai `{article_id}` bertipe UUID, sedangkan `GET /api/v1/articles` dan `GET /api/v1/quizzes/{article_id}` memakai ID artikel string seperti `art-001`. Frontend perlu mapping ID artikel ke UUID forum, atau backend perlu diseragamkan sebelum forum bisa langsung memakai `article.id`.

## TypeScript Model Utama

```ts
export type Role = "PELAJAR" | "ADMIN";

export type User = {
  user_id: string;
  username: string;
  display_name: string;
  email: string | null;
  phone_number: string | null;
  role: Role;
};

export type AuthData = {
  access_token: string;
  is_new_user?: boolean;
  user: User;
};

export type UserBatchItem = Pick<User, "user_id" | "username" | "display_name">;

export type UserBatchData = {
  users: UserBatchItem[];
  not_found_ids: string[];
};

export type Article = {
  id: string;
  title: string;
  content: string;
  category: string;
};

export type QuizQuestion = {
  id: string;
  article_id: string;
  question: string;
  options: string;
};

export type Comment = {
  id: string;
  article_id: string;
  user_id: string;
  parent_comment_id: string | null;
  content: string;
  created_at: string;
  reaction_count: number;
  replies?: Comment[] | null;
  clan_name?: string | null;
  tier?: string | null;
};

export type Reaction = {
  comment_id: string;
  reaction_type: "UPVOTE";
  reacted: boolean;
  reaction_count: number;
};

export type FailedSyncEvent = {
  event_id: number;
  event_type: "USER_SYNC";
  payload_json: string;
  status: "PENDING" | "FAILED" | "DONE";
  retry_count: number;
  last_error: string | null;
  created_at: string;
  updated_at: string;
};

export type RetrySummary = {
  processed_count: number;
  done_count: number;
  failed_count: number;
};
```

## Alur Konsumsi Frontend

| Layar/Fitur | Endpoint | Data yang dipakai |
| --- | --- | --- |
| Login | `POST /api/v1/auth/login` | Simpan `data.access_token`, render `data.user` |
| Register | `POST /api/v1/auth/register` | Simpan `data.access_token`, render `data.user` |
| Google sign-in | `POST /api/v1/auth/google` | Cek `data.is_new_user` jika frontend perlu onboarding |
| Session restore | `GET /api/v1/users/me` | Validasi token lama saat app dibuka ulang |
| Edit profil | `PATCH /api/v1/users/me` | Replace user state dengan `data` terbaru |
| Edit email/phone | `PATCH /api/v1/users/me/login-identifiers` | Replace user state dengan `data` terbaru |
| List artikel | `GET /api/v1/articles?category=...` | Render daftar `Article[]` |
| Detail artikel | `GET /api/v1/articles/{id}` | Render `Article` |
| Soal kuis | `GET /api/v1/quizzes/{article_id}` | Parse `options` dari string JSON jika ingin array pilihan |
| Submit kuis | `POST /api/v1/quizzes/{article_id}/submit` | Kirim `user_id` dari `me.user_id`; response tanpa data |
| Komentar artikel | `GET /api/v1/forums/{article_uuid}/comments` | Render nested `Comment[]` |
| Buat komentar | `POST /api/v1/forums/{article_uuid}/comments` | Tambahkan `data` ke list komentar |
| Toggle upvote | `POST /api/v1/forums/comments/{comment_id}/reactions` | Update state `reacted` dan `reaction_count` dari `data` |

Status auth yang praktis untuk UI:

- `401`: token hilang/kedaluwarsa atau credential salah. Untuk request private, hapus token lokal dan arahkan ke login.
- `403`: user sudah tidak aktif atau role tidak cukup. Tampilkan akses ditolak atau logout sesuai konteks.
- `409`: konflik data, misalnya username/email/phone sudah dipakai atau kuis sudah pernah dikerjakan.

## Kontrak Response

Semua response API memakai wrapper:

```json
{
  "success": true,
  "message": "Pesan response",
  "data": {}
}
```

Jika tidak ada data, field `data` tidak dikirim:

```json
{
  "success": true,
  "message": "Operasi berhasil"
}
```

Error juga memakai wrapper:

```json
{
  "success": false,
  "message": "Pesan error"
}
```

Konvensi JSON:

- Semua key JSON memakai `snake_case`.
- Waktu memakai format ISO-8601, contoh `2026-01-01T00:00:00Z`.
- ID user dan ID komentar/forum memakai UUID string.
- Error validasi bean akan mengambil pesan field pertama, contoh `username must not be blank`.

## Autentikasi dan Otorisasi

Endpoint yang membutuhkan login memakai JWT:

```http
Authorization: Bearer <access_token>
```

JWT dibuat oleh endpoint auth dan berisi claims:

- `sub`: `user_id` dalam format UUID.
- `role`: `PELAJAR` atau `ADMIN`.
- `iat`: issued at.
- `exp`: expiration.

Role yang dikenal aplikasi:

- `PELAJAR`
- `ADMIN`

Endpoint internal memakai API key:

```http
x-api-key: <INTERNAL_API_KEY>
```

## Matriks Endpoint

| Method | Path | Akses | Keterangan |
| --- | --- | --- | --- |
| GET | `/api/v1/health` | Public | Health check |
| POST | `/api/v1/auth/register` | Public | Register akun lokal |
| POST | `/api/v1/auth/login` | Public | Login lokal |
| POST | `/api/v1/auth/google` | Public | Login/register Google |
| GET | `/api/v1/users/me` | JWT | Profil user aktif |
| GET | `/api/v1/users/batch?ids=...` | JWT | Lookup banyak user |
| PATCH | `/api/v1/users/me` | JWT | Update username/display name |
| PATCH | `/api/v1/users/me/password` | JWT | Update password |
| PATCH | `/api/v1/users/me/login-identifiers` | JWT | Update email/phone |
| DELETE | `/api/v1/users/me` | JWT | Soft delete akun |
| GET | `/api/v1/articles` | Public | List artikel |
| GET | `/api/v1/articles/{id}` | Public | Detail artikel |
| GET | `/api/v1/quizzes/{article_id}` | Public | List soal kuis artikel |
| POST | `/api/v1/quizzes/{article_id}/submit` | JWT | Submit hasil kuis |
| GET | `/api/bacaankuis` | Public, legacy | List artikel legacy |
| GET | `/api/v1/bacaankuis` | Public, legacy | List artikel legacy |
| GET | `/api/internal/articles/{article_id}/exists` | Internal API key | Validasi artikel untuk service internal |
| POST | `/api/v1/forums/{article_id}/comments` | JWT | Buat komentar atau reply |
| GET | `/api/v1/forums/{article_id}/comments` | Public | List komentar artikel |
| PUT | `/api/v1/forums/comments/{comment_id}` | JWT pemilik | Update komentar |
| DELETE | `/api/v1/forums/comments/{comment_id}` | JWT pemilik/Admin | Delete komentar |
| POST | `/api/v1/forums/comments/{comment_id}/reactions` | JWT | Toggle reaction |
| GET | `/api/v1/forums/comments/{comment_id}/reactions` | Public | Hitung reaction |
| GET | `/api/v1/admin/failed-sync-events` | JWT role ADMIN | List outbox sync gagal |
| POST | `/api/v1/admin/failed-sync-events/retry` | JWT role ADMIN | Retry outbox sync gagal |

Catatan akses dari kode saat ini:

- Spring Security hanya mengunci route yang disebut eksplisit di `SecurityConfig`; route lain `permitAll`.
- Endpoint tulis forum tetap membutuhkan login di service layer karena memakai `CurrentUser`.
- `POST /api/v1/quizzes/{article_id}/submit` membutuhkan JWT, tetapi `user_id` masih dibaca dari request body dan belum diverifikasi sama dengan `sub` JWT.

## Status Code Umum

| Status | Arti |
| --- | --- |
| 200 | Request sukses |
| 201 | Resource dibuat, dipakai oleh create comment |
| 400 | Request invalid, validasi gagal, payload kosong, UUID/list invalid |
| 401 | Token tidak ada/tidak valid, login gagal, API key internal invalid |
| 403 | Authenticated tetapi tidak punya akses, akun tidak aktif, role bukan ADMIN |
| 404 | Resource tidak ditemukan |
| 409 | Data konflik, duplikat, atau kuis sudah pernah dikerjakan |
| 500 | Error tidak terduga |

## Health

### GET `/api/v1/health`

Akses: Public.

Response `200`:

```json
{
  "success": true,
  "message": "Health check successful",
  "data": {
    "status": "ok"
  }
}
```

## Auth

### POST `/api/v1/auth/register`

Akses: Public.

Register akun lokal. User baru selalu mendapat role `PELAJAR`.

Request body:

```json
{
  "username": "pelajar01",
  "display_name": "Pelajar Satu",
  "password": "secret123",
  "email": "pelajar@example.com",
  "phone_number": "+628111111111"
}
```

Field:

| Field | Tipe | Wajib | Aturan |
| --- | --- | --- | --- |
| `username` | string | Ya | Tidak blank, unik untuk user aktif |
| `display_name` | string | Ya | Tidak blank |
| `password` | string | Ya | Tidak blank |
| `email` | string | Tidak | Format email jika diisi, unik untuk user aktif |
| `phone_number` | string | Tidak | Unik untuk user aktif |

Minimal salah satu dari `email` atau `phone_number` wajib diisi.

Response `200`:

```json
{
  "success": true,
  "message": "Registrasi berhasil",
  "data": {
    "access_token": "<jwt>",
    "user": {
      "user_id": "00000000-0000-0000-0000-000000000001",
      "username": "pelajar01",
      "display_name": "Pelajar Satu",
      "email": "pelajar@example.com",
      "phone_number": "+628111111111",
      "role": "PELAJAR"
    }
  }
}
```

Error:

- `400`: field wajib kosong, format email invalid, atau email dan phone sama-sama kosong.
- `409`: `username`, `email`, atau `phone_number` sudah dipakai.

### POST `/api/v1/auth/login`

Akses: Public.

Login lokal memakai identifier. Identifier dapat berupa username, email, atau phone number.

Request body:

```json
{
  "identifier": "pelajar01",
  "password": "secret123"
}
```

Resolusi identifier:

- Mengandung `@`: dianggap email.
- Diawali `+` atau semua digit: dianggap phone number.
- Selain itu: dianggap username.

Response `200`:

```json
{
  "success": true,
  "message": "Login berhasil",
  "data": {
    "access_token": "<jwt>",
    "user": {
      "user_id": "00000000-0000-0000-0000-000000000001",
      "username": "pelajar01",
      "display_name": "Pelajar Satu",
      "email": "pelajar@example.com",
      "phone_number": "+628111111111",
      "role": "PELAJAR"
    }
  }
}
```

Error:

- `400`: `identifier` atau `password` blank.
- `401`: identifier tidak ditemukan, password salah, atau akun hanya memakai SSO.
- `403`: akun sudah tidak aktif.

### POST `/api/v1/auth/google`

Akses: Public.

Login dengan Google ID token. Jika `google_sub` belum ada, aplikasi membuat user baru role `PELAJAR`.

Request body:

```json
{
  "id_token": "<google-id-token>",
  "username": "pelajar_google",
  "display_name": "Pelajar Google"
}
```

Field:

| Field | Tipe | Wajib | Aturan |
| --- | --- | --- | --- |
| `id_token` | string | Ya | Diverifikasi ke Google tokeninfo |
| `username` | string | Tidak | Jika diisi harus unik |
| `display_name` | string | Tidak | Jika kosong memakai nama Google atau username |

Jika env `GOOGLE_OAUTH_CLIENT_ID` diisi, claim `aud` token harus sama dengan nilai env tersebut.

Response `200`:

```json
{
  "success": true,
  "message": "Login Google berhasil",
  "data": {
    "is_new_user": true,
    "access_token": "<jwt>",
    "user": {
      "user_id": "00000000-0000-0000-0000-000000000001",
      "username": "pelajar_google",
      "display_name": "Pelajar Google",
      "email": "pelajar.google@example.com",
      "phone_number": null,
      "role": "PELAJAR"
    }
  }
}
```

`is_new_user` bernilai `false` jika user dengan `google_sub` sudah ada.

Error:

- `400`: `id_token` kosong atau tidak valid.
- `403`: akun Google yang ditemukan sudah tidak aktif.
- `409`: username atau email bentrok saat membuat user baru.

## Users

Semua endpoint users membutuhkan:

```http
Authorization: Bearer <jwt>
```

### GET `/api/v1/users/me`

Mengambil profil user aktif berdasarkan `sub` JWT.

Response `200`:

```json
{
  "success": true,
  "message": "Profil pengguna berhasil diambil",
  "data": {
    "user_id": "00000000-0000-0000-0000-000000000001",
    "username": "pelajar01",
    "display_name": "Pelajar Satu",
    "email": "pelajar@example.com",
    "phone_number": "+628111111111",
    "role": "PELAJAR"
  }
}
```

Error:

- `401`: token tidak ada/tidak valid atau user tidak ditemukan.
- `403`: akun sudah tidak aktif.

### GET `/api/v1/users/batch?ids={uuid1},{uuid2}`

Lookup banyak user aktif. Maksimal 100 ID per request.

Contoh:

```http
GET /api/v1/users/batch?ids=00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000002
```

Response `200`:

```json
{
  "success": true,
  "message": "Batch pengguna berhasil diambil",
  "data": {
    "users": [
      {
        "user_id": "00000000-0000-0000-0000-000000000001",
        "username": "pelajar01",
        "display_name": "Pelajar Satu"
      }
    ],
    "not_found_ids": [
      "00000000-0000-0000-0000-000000000002"
    ]
  }
}
```

Error:

- `400`: `ids` kosong, ada ID bukan UUID valid, atau jumlah ID lebih dari 100.

### PATCH `/api/v1/users/me`

Update username dan/atau display name. Minimal salah satu field harus diisi.

Request body:

```json
{
  "username": "pelajar_baru",
  "display_name": "Pelajar Baru"
}
```

Response `200`: sama seperti response `GET /api/v1/users/me`, dengan message:

```json
{
  "success": true,
  "message": "Profil berhasil diperbarui",
  "data": {
    "user_id": "00000000-0000-0000-0000-000000000001",
    "username": "pelajar_baru",
    "display_name": "Pelajar Baru",
    "email": "pelajar@example.com",
    "phone_number": "+628111111111",
    "role": "PELAJAR"
  }
}
```

Error:

- `400`: payload tidak berisi `username` maupun `display_name`.
- `409`: username sudah dipakai user aktif lain.

### PATCH `/api/v1/users/me/password`

Update password.

Request body:

```json
{
  "current_password": "secret123",
  "new_password": "newsecret123"
}
```

Aturan:

- `new_password` wajib dan tidak blank.
- Untuk akun lokal, `current_password` wajib benar.
- Untuk akun SSO-only yang belum punya password, `current_password` boleh kosong untuk set password pertama.

Response `200`:

```json
{
  "success": true,
  "message": "Password berhasil diperbarui"
}
```

Error:

- `400`: `new_password` blank.
- `401`: `current_password` salah.

### PATCH `/api/v1/users/me/login-identifiers`

Update email dan/atau phone number. Minimal salah satu field harus diisi.

Request body:

```json
{
  "email": "pelajar.baru@example.com",
  "phone_number": "+628222222222"
}
```

Response `200`:

```json
{
  "success": true,
  "message": "Login identifier berhasil diperbarui",
  "data": {
    "user_id": "00000000-0000-0000-0000-000000000001",
    "username": "pelajar01",
    "display_name": "Pelajar Satu",
    "email": "pelajar.baru@example.com",
    "phone_number": "+628222222222",
    "role": "PELAJAR"
  }
}
```

Error:

- `400`: payload kosong atau format email invalid.
- `409`: email atau phone number sudah dipakai user aktif lain.

### DELETE `/api/v1/users/me`

Soft delete akun dengan mengisi `deleted_at`.

Response `200`:

```json
{
  "success": true,
  "message": "Akun berhasil dihapus"
}
```

Setelah delete:

- Login lokal ke akun tersebut menghasilkan `403`.
- Akses `/api/v1/users/me` dengan token lama menghasilkan `403`.

## Articles dan Bacaan Kuis

### GET `/api/v1/articles`

Akses: Public.

Mengambil daftar artikel. Query parameter `category` opsional dan dicocokkan case-insensitive.

Contoh:

```http
GET /api/v1/articles?category=Edu
```

Response `200`:

```json
{
  "success": true,
  "message": "Daftar bacaan",
  "data": [
    {
      "id": "art-123",
      "title": "Judul Artikel",
      "content": "Isi artikel",
      "category": "Edu"
    }
  ]
}
```

### GET `/api/v1/articles/{id}`

Akses: Public.

Response `200`:

```json
{
  "success": true,
  "message": "Detail bacaan",
  "data": {
    "id": "art-123",
    "title": "Judul Artikel",
    "content": "Isi artikel",
    "category": "Edu"
  }
}
```

Error:

- `404`: artikel tidak ditemukan.

### GET `/api/v1/quizzes/{article_id}`

Akses: Public.

Mengambil soal kuis untuk artikel. Jawaban benar tidak dikirim.

Response `200`:

```json
{
  "success": true,
  "message": "Soal kuis ditemukan",
  "data": [
    {
      "id": "quiz-1",
      "article_id": "art-123",
      "question": "Pertanyaan kuis?",
      "options": "[\"A\",\"B\",\"C\",\"D\"]"
    }
  ]
}
```

Catatan: `options` saat ini bertipe string sesuai model `Quiz`.

### POST `/api/v1/quizzes/{article_id}/submit`

Akses: JWT.

Submit hasil kuis dan sinkronisasi ke Rust quiz history.

Request body:

```json
{
  "user_id": "00000000-0000-0000-0000-000000000001",
  "score": 80,
  "accuracy": 75
}
```

`article_id` pada body akan ditimpa oleh path variable `{article_id}`.

Field:

| Field | Tipe | Wajib | Aturan |
| --- | --- | --- | --- |
| `user_id` | UUID | Ya | User yang mengerjakan kuis |
| `score` | number | Ya | 0 sampai 100 |
| `accuracy` | number | Ya | 0 sampai 100 |

Response `200`:

```json
{
  "success": true,
  "message": "Jawaban berhasil dikirim"
}
```

Error:

- `400`: body kosong, `user_id` kosong, `article_id` kosong, `score`/`accuracy` di luar 0-100.
- `401`: JWT tidak ada/tidak valid.
- `409`: user sudah pernah mengerjakan kuis artikel tersebut.

Catatan integrasi: jika sinkronisasi ke Rust gagal, exception ditangkap di client sync dan response Java tetap sukses setelah attempt lokal tersimpan.

### GET `/api/bacaankuis`

Akses: Public, legacy/deprecated.

### GET `/api/v1/bacaankuis`

Akses: Public, legacy/deprecated.

Kedua endpoint legacy mengembalikan semua artikel:

```json
{
  "success": true,
  "message": "Bacaan kuis fetched",
  "data": [
    {
      "id": "art-123",
      "title": "Judul Artikel",
      "content": "Isi artikel",
      "category": "Edu"
    }
  ]
}
```

Gunakan `/api/v1/articles` dan `/api/v1/quizzes` untuk integrasi baru.

## Internal Articles

### GET `/api/internal/articles/{article_id}/exists`

Akses: Internal API key.

Header wajib:

```http
x-api-key: <INTERNAL_API_KEY>
```

Response `200`:

```json
{
  "success": true,
  "message": "Artikel valid",
  "data": {
    "exists": true,
    "category_id": 1,
    "category_name": "Edu"
  }
}
```

Error:

- `401`: API key hilang, kosong, atau salah.
- `404`: artikel tidak ditemukan di core DB.

Catatan: `category_id` saat ini selalu bernilai `1` dari implementasi `ArticleService`.

## Forum Comments

Path variable forum memakai UUID:

- `{article_id}`: UUID artikel.
- `{comment_id}`: UUID komentar.

### POST `/api/v1/forums/{article_id}/comments`

Akses: JWT.

Buat root comment atau reply. Jika `parent_comment_id` diisi, komentar dibuat sebagai reply.

Request body:

```json
{
  "parent_comment_id": null,
  "content": "Komentar baru"
}
```

Field:

| Field | Tipe | Wajib | Aturan |
| --- | --- | --- | --- |
| `parent_comment_id` | UUID/null | Tidak | Harus ID komentar yang ada jika diisi |
| `content` | string | Ya | Tidak blank, maksimal 5000 karakter |

Response `201`:

```json
{
  "success": true,
  "message": "Komentar berhasil dibuat",
  "data": {
    "id": "11111111-1111-1111-1111-111111111111",
    "article_id": "22222222-2222-2222-2222-222222222222",
    "user_id": "00000000-0000-0000-0000-000000000001",
    "parent_comment_id": null,
    "content": "Komentar baru",
    "created_at": "2026-01-01T00:00:00Z",
    "reaction_count": 0,
    "replies": null,
    "clan_name": null,
    "tier": null
  }
}
```

Error:

- `400`: content kosong atau lebih dari 5000 karakter.
- `401`: login tidak ada/tidak valid.
- `404`: `parent_comment_id` tidak ditemukan.

### GET `/api/v1/forums/{article_id}/comments`

Akses: Public.

Mengambil root comments beserta replies nested. Root comment diurutkan terbaru lebih dulu.

Response `200`:

```json
{
  "success": true,
  "message": "Komentar berhasil diambil",
  "data": [
    {
      "id": "11111111-1111-1111-1111-111111111111",
      "article_id": "22222222-2222-2222-2222-222222222222",
      "user_id": "00000000-0000-0000-0000-000000000001",
      "parent_comment_id": null,
      "content": "Root comment",
      "created_at": "2026-01-01T00:00:00Z",
      "reaction_count": 3,
      "clan_name": "Nusantara",
      "tier": "GOLD",
      "replies": [
        {
          "id": "33333333-3333-3333-3333-333333333333",
          "article_id": "22222222-2222-2222-2222-222222222222",
          "user_id": "00000000-0000-0000-0000-000000000002",
          "parent_comment_id": "11111111-1111-1111-1111-111111111111",
          "content": "Reply",
          "created_at": "2026-01-01T00:01:00Z",
          "reaction_count": 0,
          "clan_name": null,
          "tier": null,
          "replies": []
        }
      ]
    }
  ]
}
```

### PUT `/api/v1/forums/comments/{comment_id}`

Akses: JWT pemilik komentar.

Request body:

```json
{
  "content": "Konten diperbarui"
}
```

Response `200`:

```json
{
  "success": true,
  "message": "Komentar berhasil diperbarui",
  "data": {
    "id": "11111111-1111-1111-1111-111111111111",
    "article_id": "22222222-2222-2222-2222-222222222222",
    "user_id": "00000000-0000-0000-0000-000000000001",
    "parent_comment_id": null,
    "content": "Konten diperbarui",
    "created_at": "2026-01-01T00:00:00Z",
    "reaction_count": 3,
    "replies": null,
    "clan_name": null,
    "tier": null
  }
}
```

Error:

- `400`: content kosong atau lebih dari 5000 karakter.
- `401`: login tidak ada/tidak valid.
- `403`: user bukan pemilik komentar.
- `404`: komentar tidak ditemukan.

### DELETE `/api/v1/forums/comments/{comment_id}`

Akses: JWT pemilik komentar atau role `ADMIN`.

Response `200`:

```json
{
  "success": true,
  "message": "Komentar berhasil dihapus"
}
```

Error:

- `401`: login tidak ada/tidak valid.
- `403`: user bukan pemilik komentar dan bukan ADMIN.
- `404`: komentar tidak ditemukan.

## Forum Reactions

Reaction type yang valid saat ini hanya:

- `UPVOTE`

### POST `/api/v1/forums/comments/{comment_id}/reactions`

Akses: JWT.

Toggle reaction. Jika user belum memberi `UPVOTE`, request akan menambah reaction. Jika sudah, request akan menghapus reaction.

Request body:

```json
{
  "reaction_type": "UPVOTE"
}
```

Response `200` saat reaction ditambahkan:

```json
{
  "success": true,
  "message": "Reaksi berhasil diperbarui",
  "data": {
    "comment_id": "11111111-1111-1111-1111-111111111111",
    "reaction_type": "UPVOTE",
    "reacted": true,
    "reaction_count": 5
  }
}
```

Response `200` saat reaction dihapus:

```json
{
  "success": true,
  "message": "Reaksi berhasil diperbarui",
  "data": {
    "comment_id": "11111111-1111-1111-1111-111111111111",
    "reaction_type": "UPVOTE",
    "reacted": false,
    "reaction_count": 4
  }
}
```

Error:

- `400`: `reaction_type` kosong.
- `401`: login tidak ada/tidak valid.
- `404`: komentar tidak ditemukan.

### GET `/api/v1/forums/comments/{comment_id}/reactions`

Akses: Public.

Mengambil jumlah reaction `UPVOTE`.

Response `200`:

```json
{
  "success": true,
  "message": "Reaksi berhasil diambil",
  "data": {
    "comment_id": "11111111-1111-1111-1111-111111111111",
    "reaction_type": "UPVOTE",
    "reacted": false,
    "reaction_count": 5
  }
}
```

Catatan: untuk endpoint GET, field `reacted` selalu `false` karena response hanya membangun count publik dan tidak membaca user saat ini.

Error:

- `404`: komentar tidak ditemukan.

## Admin Outbox

Semua endpoint admin membutuhkan JWT dengan role `ADMIN`.

```http
Authorization: Bearer <admin-jwt>
```

Role selain `ADMIN` akan mendapat `403`.

### GET `/api/v1/admin/failed-sync-events`

Mengambil maksimal 100 event outbox dengan status `FAILED` atau `PENDING`, urut `created_at` ascending.

Response `200`:

```json
{
  "success": true,
  "message": "Daftar failed sync events berhasil diambil",
  "data": {
    "events": [
      {
        "event_id": 1,
        "event_type": "USER_SYNC",
        "payload_json": "{\"user_id\":\"00000000-0000-0000-0000-000000000001\"}",
        "status": "FAILED",
        "retry_count": 0,
        "last_error": "timeout",
        "created_at": "2026-01-01T00:00:00Z",
        "updated_at": "2026-01-01T00:00:00Z"
      }
    ]
  }
}
```

### POST `/api/v1/admin/failed-sync-events/retry`

Retry outbox tertentu atau semua event retryable.

Request body retry by IDs:

```json
{
  "event_ids": [1, 2, 3]
}
```

Request body retry semua:

```json
{
  "retry_all": true
}
```

Jika `retry_all` bernilai `true`, service mengambil maksimal 100 event status `FAILED` atau `PENDING`.

Response `200`:

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

Error:

- `400`: `event_ids` kosong dan `retry_all` tidak `true`.
- `401`: token tidak ada/tidak valid.
- `403`: role bukan `ADMIN`.

Status event:

- `PENDING`
- `FAILED`
- `DONE`

Event type:

- `USER_SYNC`

## Integrasi Keluar ke Rust Engine

Aplikasi Java juga memanggil service Rust. Endpoint ini bukan endpoint yang disediakan aplikasi Java, tetapi penting untuk integrasi deployment.

### User Sync

Dipanggil setelah register lokal atau Google user baru.

```http
POST {RUST_ENGINE_BASE_URL}/api/internal/users/sync
x-api-key: <INTERNAL_API_KEY>
Content-Type: application/json
```

Body:

```json
{
  "user_id": "00000000-0000-0000-0000-000000000001"
}
```

Status Rust `201` dan `409` dianggap sukses. Status retryable: `408`, `429`, dan `5xx`. Jika tetap gagal setelah retry internal, Java menyimpan event `USER_SYNC` ke `failed_sync_events`.

### Quiz History Sync

Dipanggil setelah submit kuis berhasil disimpan lokal.

```http
POST {RUST_ENGINE_BASE_URL}/api/internal/quiz-history/sync
x-api-key: <INTERNAL_API_KEY>
Content-Type: application/json
```

Body:

```json
{
  "user_id": "00000000-0000-0000-0000-000000000001",
  "article_id": "art-123",
  "score": 80,
  "accuracy": 75
}
```

Jika request ke Rust gagal, exception ditangkap dan Java tidak menggagalkan response submit kuis.

## Environment Terkait API

| Env | Default | Fungsi |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | Port server lokal |
| `JWT_SECRET` | secret testing panjang | Signing secret JWT, minimal 32 karakter |
| `INTERNAL_API_KEY` | `any_random_key_for_internal_sync` | API key endpoint internal dan call ke Rust |
| `GOOGLE_OAUTH_CLIENT_ID` | kosong | Audience validasi Google ID token |
| `RUST_ENGINE_BASE_URL` | `http://localhost:8081` | Base URL Rust engine untuk sync |

## Contoh Alur Client

1. Register atau login:

```http
POST /api/v1/auth/login
```

2. Simpan `data.access_token`.

3. Panggil endpoint private:

```http
GET /api/v1/users/me
Authorization: Bearer <access_token>
```

4. Untuk endpoint admin, pastikan token dibuat untuk user dengan role `ADMIN`.
