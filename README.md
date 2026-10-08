# ProbSol Backend Service

Enterprise-grade Spring Boot 3 + SQLite backend service engineered for the **ProbSol** knowledge workspace. Implements the complete REST API specification defined in [BackendExpectation.md](file:///Users/anujtiwari/Desktop/ProbSolBackend/BackendExpectation.md).

---

## 🚀 Quick Start

### Prerequisites
- **Java 21** LTS
- **Maven 3.9+**

### Run Locally
```bash
# Run with Spring Boot Maven plugin
mvn spring-boot:run

# Or run the packaged fat JAR
java -jar target/probsol-backend-1.0.0.jar
```

The server starts on port `8080`: `http://localhost:8080`.

---

## 📖 Swagger / OpenAPI Documentation

Interactive Swagger UI documentation is available directly in the browser:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **Testing Protected Endpoints in Swagger**:
> 1. Call `POST /api/v1/auth/login` in Swagger with the seed credentials.
> 2. Copy the `accessToken`.
> 3. Click the green **Authorize** button at the top of the Swagger page.
> 4. Paste the token into the `bearerAuth` field.
> 5. You can now execute all protected `/api/v1/entries`, `/api/v1/tags`, and `/api/v1/analytics` requests directly from the UI!

---

## 🔑 Default Personal Seed Account

On startup, if no records exist, the database automatically initializes your personal account with sample problems, solutions, and tags ready for daily use:

- **Email**: `anuj@probsol.dev`
- **Password**: `Password123!`
- **Display Name**: `Anuj Tiwari`

You can change these in [application.properties](file:///Users/anujtiwari/Desktop/ProbSolBackend/src/main/resources/application.properties):
```properties
probsol.seed.enabled=true
probsol.seed.email=anuj@probsol.dev
probsol.seed.password=Password123!
probsol.seed.display-name=Anuj Tiwari
```

---

## 🛠 Features & Architecture

1. **Database & Persistence**:
   - **SQLite** database (`probsol.db`) configured with HikariCP connection pool.
   - Preserves all notes across application restarts.
   - Fully optimized compound indexes on `[user_id, created_at]`, `[user_id, status]`, and `[user_id, type]`.
2. **Security & Authentication**:
   - **Spring Security 6** with stateless JWT authentication.
   - Short-lived Access Token (15-min) passed via `Authorization: Bearer <token>`.
   - Long-lived Refresh Token (30 days) stored hashed (SHA-256) in SQLite and set in an `HttpOnly`, `SameSite=Lax` cookie (`probsol_rt`).
   - Secure token rotation upon every refresh request.
3. **Multi-Tenant Data Isolation**:
   - Every note, tag, and query is scoped strictly to the authenticated `user_id`. Users cannot view or mutate another user's records.
4. **Dynamic Querying & Filtering**:
   - `GET /api/v1/entries` supports `q` (title/description search), `type`, `status`, `tags`, `tag_mode=any|all`, `from`, `to`, `sort` (`created_at:desc`, `created_at:asc`, `title:asc`), and 1-based pagination.
5. **CORS Configured**:
   - Defaults to `http://localhost:3000`, `http://localhost:5173`, `http://localhost:4173`, `http://127.0.0.1:3000`, and `http://127.0.0.1:5173`.

---

## 📡 REST API Summary

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/` | Public | Welcome status message |
| `GET` | `/api/v1/health` | Public | Health probe (used by Render) |
| `POST` | `/api/v1/auth/register` | Public | Register new user |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user & issue tokens |
| `POST` | `/api/v1/auth/refresh` | Public / Cookie | Rotate refresh token & issue new access token |
| `POST` | `/api/v1/auth/logout` | Public / Cookie | Clear refresh cookie & revoke session |
| `GET` | `/api/v1/auth/me` | Bearer | Get current user profile |
| `GET` | `/api/v1/entries` | Bearer | List & search entries with pagination |
| `POST` | `/api/v1/entries` | Bearer | Create a problem or solution |
| `GET` | `/api/v1/entries/:id` | Bearer | Get single entry by ID |
| `PATCH` | `/api/v1/entries/:id` | Bearer | Update entry fields |
| `PATCH` | `/api/v1/entries/:id/status` | Bearer | Update entry status (`OPEN` / `SOLVED`) |
| `DELETE` | `/api/v1/entries/:id` | Bearer | Soft delete entry |
| `GET` | `/api/v1/tags` | Bearer | Get user tags with active entry counts |
| `GET` | `/api/v1/analytics/summary` | Bearer | Get counts, solve ratio & top tags |

---

## 🧪 Testing

Run automated tests:
```bash
mvn test
```
All integration tests verify authentication, tenant data isolation, entry CRUD, tag aggregation, and analytics metrics.
