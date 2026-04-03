# API Documentation

> **Base URL (via Gateway):** `http://localhost:8080`
>
> All requests (except public auth endpoints) require a Bearer token in the `Authorization` header.

---

## Table of Contents

- [Authentication](#authentication)
- [User Management](#user-management)
- [Finance](#finance)
    - [Viewer Endpoints](#finance---viewer)
    - [Admin / Analyst Endpoints](#finance---admin--analyst)
- [Roles & Access Control](#roles--access-control)
- [Response Format](#response-format)
- [Error Handling](#error-handling)

---

## Authentication

**Base path:** `/api/auth`  
All endpoints in this section are **public** (no token required) unless noted.

---

### `POST /api/auth/register`

Register a new user. Sends an OTP to the provided email for verification.

**Request Body:**
```json
{
  "username": "sourabh_gorai",
  "name": "Sourabh Gorai",
  "password": "Secret@123",
  "email": "sourabh@example.com",
  "role": "VIEWER"
}
```

**Constraints:**
| Field | Rules |
|-------|-------|
| `username` | Required, 3–50 chars, alphanumeric + underscores only |
| `name` | Required, max 100 chars |
| `password` | Required, min 8 chars, must contain uppercase, lowercase, digit, and special char (`@$!%*?&_#^`) |
| `email` | Required, valid email format |
| `role` | Optional — defaults to `VIEWER`. Cannot be `ADMIN` (use `/createAdmin`) |

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Registration Successful",
  "data": {
    "usn": "USR20260403A1B2C3",
    "username": "sourabh_gorai",
    "name": "Sourabh Gorai",
    "email": "sourabh@example.com",
    "role": "VIEWER",
    "verified": false
  },
  "timestamp": "2026-04-03T14:00:00"
}
```

---

### `POST /api/auth/login`

Authenticate and receive a JWT token.

**Request Body:**
```json
{
  "username": "sourabh_gorai",
  "password": "Secret@123"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Login Successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { ... }
  },
  "timestamp": "2026-04-03T14:00:00"
}
```

**Errors:**
- `401` — Invalid credentials
- `403` — Account not verified

---

### `POST /api/auth/verify-otp`

Verify email after registration.

**Request Body:**
```json
{
  "email": "sourabh@example.com",
  "otp": "482910"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Email verified",
  "data": null
}
```

---

### `POST /api/auth/resend-verify-otp`

Resend verification OTP to email.

**Request Body:**
```json
{
  "email": "sourabh@example.com"
}
```

---

### `POST /api/auth/forgot-password`

Send a password reset OTP to email.

**Request Body:**
```json
{
  "email": "sourabh@example.com"
}
```

---

### `POST /api/auth/reset-password`

Reset password using OTP received via email.

**Request Body:**
```json
{
  "email": "sourabh@example.com",
  "otp": "482910",
  "newPassword": "NewSecret@456"
}
```

---

### `GET /api/auth/validate-token`

🔒 **Requires:** Any valid token

Validates a JWT and returns the associated user.

**Headers:**
```
Authorization: Bearer <token>
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Token validated successfully",
  "data": { ...user }
}
```

---

### `POST /api/auth/validate-credentials`

Validates username and password without issuing a token.

**Request Body:**
```json
{
  "username": "sourabh_gorai",
  "password": "Secret@123"
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Validated",
  "data": { "valid": true }
}
```

---

### `POST /api/auth/bulk-register`

🔒 **Requires:** `ADMIN`

Register multiple users in one request.

**Request Body:**
```json
{
  "users": [
    {
      "username": "user1",
      "name": "User One",
      "password": "Pass@1234",
      "email": "user1@example.com",
      "role": "VIEWER"
    },
    {
      "username": "user2",
      "name": "User Two",
      "password": "Pass@5678",
      "email": "user2@example.com",
      "role": "ANALYST"
    }
  ]
}
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "summary": { "total": 2, "success": 2, "failed": 0 },
    "results": [
      { "username": "user1", "status": "SUCCESS", "message": "User created. OTP sent to email." },
      { "username": "user2", "status": "SUCCESS", "message": "User created. OTP sent to email." }
    ]
  }
}
```

---

### `POST /api/auth/bulk-verify-otp`

🔒 **Requires:** `ADMIN`

Verify OTPs for multiple users at once.

**Request Body:**
```json
{
  "requests": [
    { "email": "user1@example.com", "otp": "123456" },
    { "email": "user2@example.com", "otp": "654321" }
  ]
}
```

---

## User Management

**Base path:** `/api/users`  
🔒 All endpoints require authentication.

---

### `GET /api/users/getAll`

🔒 **Requires:** `ADMIN`

Fetch all users.

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Fetch 3 records",
  "data": [ { ...user }, { ...user } ]
}
```

---

### `GET /api/users/getAll/paged`

🔒 **Requires:** `ADMIN`

Fetch users with pagination.

**Query Params:**
| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Page number (0-indexed) |
| `size` | `20` | Records per page |

**Example:** `GET /api/users/getAll/paged?page=0&size=10`

---

### `GET /api/users/{usn}`

🔒 **Requires:** `VIEWER`, `ANALYST`, or `ADMIN`

Fetch a specific user by USN.

**Path Variable:** `usn` — e.g., `USR20260403A1B2C3`

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Fetched data successfully",
  "data": {
    "usn": "USR20260403A1B2C3",
    "username": "sourabh_gorai",
    "name": "Sourabh Gorai",
    "email": "sourabh@example.com",
    "role": "VIEWER",
    "verified": true,
    "createdAt": "2026-04-03T12:00:00"
  }
}
```

---

### `PUT /api/users/{usn}`

🔒 **Requires:** `VIEWER`, `ANALYST`, or `ADMIN`

Update user details.

**Request Body:**
```json
{
  "name": "Sourabh Updated",
  "email": "sourabh.updated@example.com"
}
```

---

### `DELETE /api/users/soft/{usn}`

🔒 **Requires:** Own account or `ADMIN`

Soft delete — marks user as inactive without removing from database.

---

### `DELETE /api/users/hard/{usn}`

🔒 **Requires:** Own account or `ADMIN`

Hard delete — permanently removes user from database.

---

### `PUT /api/users/changeRole/{usn}/{role}`

🔒 **Requires:** `ADMIN`

Change a user's role.

**Path Variables:**
- `usn` — target user's USN
- `role` — one of `VIEWER`, `ANALYST`, `ADMIN`

**Example:** `PUT /api/users/changeRole/USR20260403A1B2C3/ANALYST`

---

### `PUT /api/users/changeEmail/{usn}/{email}`

🔒 **Requires:** `VIEWER`, `ANALYST`, or `ADMIN`

Update user's email. Resets verification and sends new OTP.

---

### `POST /api/users/createAdmin`

🔒 **Requires:** `ADMIN`

Create a new admin user.

**Request Body:** Same as `/api/auth/register`

---

### `POST /api/users/getUsers`

🔒 **Requires:** Authenticated (internal use)

Fetch multiple users by a list of USNs.

**Request Body:**
```json
["USR20260403A1B2C3", "USR20260403D4E5F6"]
```

---

### `GET /api/users/validate/{usn}`

🔒 **Requires:** Authenticated (internal use)

Check if a user with the given USN exists.

**Response:** `true` or `false`

---

## Finance

**Base path:** `/api/finance`  
🔒 All endpoints require authentication.

---

### `GET /api/finance/v/getTypes`

🔒 **Requires:** `VIEWER`, `ANALYST`, or `ADMIN`

Get all available transaction types.

**Response `200 OK`:**
```json
{
  "success": true,
  "data": ["INCOME", "EXPENSE"]
}
```

---

### `GET /api/finance/v/getCategories`

🔒 **Requires:** `VIEWER`, `ANALYST`, or `ADMIN`

Get all available categories.

**Response `200 OK`:**
```json
{
  "success": true,
  "data": ["FOOD", "TRANSPORT", "SALARY", "ENTERTAINMENT", ...]
}
```

---

## Finance - Viewer

> Endpoints accessible by `VIEWER`, `ANALYST`, and `ADMIN`.

---

### `POST /api/finance/v/addRecord`

Add a new finance record for the authenticated user.

**Request Body:**
```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "SALARY",
  "note": "Monthly salary"
}
```

**Constraints:**
| Field | Rules |
|-------|-------|
| `amount` | Required, non-negative |
| `type` | Required — `INCOME` or `EXPENSE` |
| `category` | Required — valid `Category` enum value |
| `note` | Optional |

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Successfully added data.",
  "data": {
    "financeId": 1,
    "usn": "USR20260403A1B2C3",
    "name": "Sourabh Gorai",
    "amount": 5000.0,
    "type": "INCOME",
    "category": "SALARY",
    "note": "Monthly salary",
    "isDeleted": false
  }
}
```

---

### `PUT /api/finance/v/updateRecord`

Update an existing finance record. Users can only update their own records; `ADMIN` can update any.

**Request Body:**
```json
{
  "financeId": 1,
  "usn": "USR20260403A1B2C3",
  "amount": 4500.00,
  "type": "INCOME",
  "category": "SALARY",
  "note": "Corrected amount"
}
```

---

### `DELETE /api/finance/v/deleteRecord/{financeId}`

Soft delete a finance record.

**Path Variable:** `financeId` — e.g., `1`

---

### `GET /api/finance/v/getMyRecords`

Fetch all active records for the authenticated user, ordered by date descending.

---

### `GET /api/finance/v/getByType/{type}`

Fetch authenticated user's records filtered by type.

**Path Variable:** `type` — `INCOME` or `EXPENSE`

**Example:** `GET /api/finance/v/getByType/INCOME`

---

### `GET /api/finance/v/getByCategory/{category}`

Fetch authenticated user's records filtered by category.

**Example:** `GET /api/finance/v/getByCategory/FOOD`

---

### `GET /api/finance/v/getMyMonthlyStats/{month}`

Get income/expense stats for a specific month.

**Path Variable:** `month` — format `YYYY-MM`

**Example:** `GET /api/finance/v/getMyMonthlyStats/2026-03`

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "usn": "USR20260403A1B2C3",
    "name": "Sourabh Gorai",
    "totalIncome": 10000.0,
    "totalExpense": 3500.0,
    "netBalance": 6500.0
  }
}
```

---

### `GET /api/finance/v/getMyYearlyStats/{year}`

Get income/expense stats for a full year.

**Path Variable:** `year` — format `YYYY`

**Example:** `GET /api/finance/v/getMyYearlyStats/2026`

---

### `GET /api/finance/v/getMyTotalStats`

Get all-time cumulative income/expense stats for the authenticated user.

---

### `GET /api/finance/v/getCategoryBreakdown`

Get the authenticated user's spending breakdown grouped by category.

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "FOOD": 1200.0,
    "TRANSPORT": 500.0,
    "SALARY": 10000.0
  }
}
```

---

### `GET /api/finance/v/getMonthlyTrend/{year}`

Get the authenticated user's month-by-month income/expense trend for a year.

**Example:** `GET /api/finance/v/getMonthlyTrend/2026`

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "2026-01": { "totalIncome": 10000.0, "totalExpense": 3000.0, "netBalance": 7000.0 },
    "2026-02": { "totalIncome": 10000.0, "totalExpense": 4200.0, "netBalance": 5800.0 }
  }
}
```

---

## Finance - Admin / Analyst

> Endpoints accessible by `ADMIN` and `ANALYST` only.

---

### `GET /api/finance/a/getAll/{isDeleted}`

Fetch all records across all users.

**Path Variable:** `isDeleted` — `true` for deleted records, `false` for active

**Example:** `GET /api/finance/a/getAll/false`

---

### `GET /api/finance/a/getAllByType/{type}`

Fetch all active records filtered by type across all users.

**Example:** `GET /api/finance/a/getAllByType/EXPENSE`

---

### `GET /api/finance/a/getAllByCategory/{category}`

Fetch all active records filtered by category across all users.

---

### `GET /api/finance/a/getMyMonthlyStats/{month}`

Get platform-wide income/expense stats for a specific month.

**Example:** `GET /api/finance/a/getMyMonthlyStats/2026-03`

---

### `GET /api/finance/a/getMyYearlyStats/{year}`

Get platform-wide income/expense stats for a full year.

---

### `GET /api/finance/a/getMyTotalStats`

Get all-time platform-wide cumulative stats.

---

### `GET /api/finance/a/getCategoryBreakdown`

Get platform-wide spending breakdown grouped by category.

---

### `GET /api/finance/a/getTypeBreakdown`

Get platform-wide breakdown grouped by transaction type.

**Response `200 OK`:**
```json
{
  "success": true,
  "data": {
    "INCOME": 150000.0,
    "EXPENSE": 62000.0
  }
}
```

---

### `GET /api/finance/a/getMonthlyTrend/{year}`

Get platform-wide month-by-month trend for a year.

---

## Roles & Access Control

| Role | Description |
|------|-------------|
| `VIEWER` | Regular user — can only access and manage their own data |
| `ANALYST` | Can view all finance data (read-only admin-level finance access) |
| `ADMIN` | Full access — user management, bulk operations, all finance data |

| Endpoint | Method | VIEWER | ANALYST | ADMIN |
|----------|--------|--------|---------|-------|
| `/api/auth/register` | POST | ✅ | ✅ | ✅ |
| `/api/auth/login` | POST | ✅ | ✅ | ✅ |
| `/api/auth/verify-otp` | POST | ✅ | ✅ | ✅ |
| `/api/auth/resend-verify-otp` | POST | ✅ | ✅ | ✅ |
| `/api/auth/forgot-password` | POST | ✅ | ✅ | ✅ |
| `/api/auth/reset-password` | POST | ✅ | ✅ | ✅ |
| `/api/auth/validate-credentials` | POST | ✅ | ✅ | ✅ |
| `/api/auth/validate-token` | GET | ✅ | ✅ | ✅ |
| `/api/auth/bulk-register` | POST | ❌ | ❌ | ✅ |
| `/api/auth/bulk-verify-otp` | POST | ❌ | ❌ | ✅ |
| `/api/users/getAll` | GET | ❌ | ❌ | ✅ |
| `/api/users/getAll/paged` | GET | ❌ | ❌ | ✅ |
| `/api/users/changeRole/{usn}/{role}` | PUT | ❌ | ❌ | ✅ |
| `/api/users/createAdmin` | POST | ❌ | ❌ | ✅ |
| `/api/users/validate/**` | GET | ✅ | ✅ | ✅ |
| `/api/users/getUsers` | POST | ✅ | ✅ | ✅ |
| `/api/users/**` (get, update, delete) | GET/PUT/DELETE | ✅ | ✅ | ✅ |
| `/api/finance/v/**` | GET/POST/PUT/DELETE | ✅ | ✅ | ✅ |
| `/api/finance/a/**` | GET | ❌ | ✅ | ✅ |

---

## Response Format

All endpoints return a consistent envelope:

```json
{
  "success": true,
  "message": "Human readable message",
  "data": { },
  "timestamp": "2026-04-03T14:00:00.000"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `success` | `boolean` | `true` on success, `false` on error |
| `message` | `string` | Summary of the result |
| `data` | `any` | Response payload (null on errors or void responses) |
| `timestamp` | `string` | ISO-8601 timestamp of the response |

---

## Error Handling

All errors follow the same structure:

```json
{
  "timestamp": "2026-04-03T14:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register",
  "fields": {
    "password": "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character",
    "email": "Email must be a valid email address"
  }
}
```

> The `fields` object is only present on validation errors (`400`).

### HTTP Status Codes

| Code | Meaning |
|------|---------|
| `200` | Success |
| `400` | Bad Request / Validation failed |
| `401` | Unauthorized — missing or invalid token |
| `403` | Forbidden — insufficient role or unverified account |
| `404` | Resource not found |
| `409` | Conflict — duplicate username or email |
| `500` | Internal Server Error |