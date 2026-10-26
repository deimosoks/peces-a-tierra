# AGENTS.md

Spring Boot 3.5.9 / Java 21 REST API with a prebuilt Angular PWA embedded in the jar. Single Gradle module; no frontend source in this repo.

## Commands (Windows/PowerShell)

```powershell
.\gradlew.bat bootRun          # API on :8080
.\gradlew.bat compileJava      # fastest compile check
.\gradlew.bat build            # compile + test + bootJar
.\gradlew.bat test             # full suite (unit + web + integration)
.\gradlew.bat test --tests "org.icc.pecesatierra.unit.user.*"   # focused run
```

No linter, formatter, type checker, CI, or codegen step is configured. Compile + test are the verification steps. `build/`, `bin/`, `.gradle/` are artifacts; never edit them.

## Frontend deploy

`scripts/deploy-frontend.ps1` is the only supported way to refresh the embedded SPA. It builds the Angular app (`npx ng build --configuration production`), copies `dist/icc-tda-frontend/browser` into `src/main/resources/static/`, removes files from the previous bundle that no longer exist, verifies the SHA256 of every copied file and checks that every hash declared in `ngsw.json` exists and matches. It aborts loudly on any mismatch. Paths are parameters: `-FrontendRoot`, `-OutputPath`, `-BackendStatic`, `-Configuration`. Do not hand-edit `static/` and do not copy with tools that rewrite line endings: `ngsw.json` hashes the exact bytes.

## Commits

- One commit = one atomic change. It must compile on its own and must not mix unrelated fixes, refactors, formatting, dependency updates, or test suites.
- The message states what changed, why, and the finding/issue it addresses, e.g. `fix(baptism): persist note and municipality (H-07)`.
- A behaviour change and the regression test that proves it belong in the same commit. Purely independent test additions go in their own commit.
- Never commit `build/`, `.gradle/`, `bin/`, IDE files, secrets, or generated artifacts. Audit reports are committed separately.
- Before committing: `.\gradlew.bat compileJava`; run `.\gradlew.bat test` when behaviour changed.

## Required environment

`src/main/resources/application.properties` has no defaults for `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `GEMINI_API_KEY`, `AI_DB_USER`, `AI_DB_PASS`; `PORT` defaults to 8080. `AI_DB_USER`/`AI_DB_PASS` is a separate read-only role used only by the AI SQL executor; `AI_DB_URL` is optional and defaults to `DB_URL` (same database, different role). Optional with defaults: `JWT_ISSUER` (`peces-a-tierra`), `JWT_AUDIENCE` (`peces-a-tierra-api`), `REFRESH_TOKEN_PEPPER` (pepper of the refresh-token SHA-256; changing it invalidates every live session), `RATE_LIMIT_LOGIN_MAX` (10), `RATE_LIMIT_AI_MAX` (20), `RATE_LIMIT_WINDOW_SECONDS` (60). `CORS_ALLOWED_ORIGINS` is optional: a comma-separated list of explicit CORS origins (default `http://localhost:4200`); empty means no CORS headers because the production SPA is served from the same origin. The `test` profile supplies literal values, so `.\gradlew.bat test` runs without real environment variables. `EXPORT_MAX_ROWS` is optional (default 10000) and caps the rows a single member/attendance export may materialise; above it the request is rejected with 400 instead of truncating.

## Database

- No migrations, `schema.sql`, or seed data exist. `spring.jpa.hibernate.ddl-auto=validate`: PostgreSQL must already contain the schema/tables; Hibernate will not create or change them.
- The `unaccent` PostgreSQL extension is required (used by member search and AI-generated queries); queries fail without it.
- `spring.jpa.open-in-view=false`: access lazy associations only inside transactions or fetch them explicitly (`@EntityGraph`, joins). MapStruct mappers touching lazy fields outside a transaction will throw.
- IDs are UUID `String`s (`GenerationType.UUID`), never numeric.
- `refresh_token.token` stores the **SHA-256 hex (64 chars) plus pepper** of the refresh token, never the plaintext (H-17). There is no migration in the repo: rows written before that change hold plaintext, no longer match and therefore behave as revoked (every user logs in again once). The column only needs room for 64 characters; the entity keeps the `unique` constraint. Rotating `REFRESH_TOKEN_PEPPER` has the same effect: it invalidates all live refresh tokens.
- Access tokens carry `iss`/`aud`/`jti` and the parser requires the configured issuer and audience (H-17). Tokens issued before that change are rejected; the access token lives 10 minutes, so the impact is one renewal after deploy.

## Code conventions

- Layout: `features/<domain>/` contains `controller/`, `service/`, `repository/`, `dtos/`, `mapper/`, `exceptions/`; the entity sits at the domain root. Cross-cutting specs live in `utils/specs/`.
- Response style is inconsistent: some controllers extend `BaseController` (`ApiResponse` envelope), others (e.g. `MemberController`) return raw DTOs. Match the controller being edited; don't standardize opportunistically.
- User-facing messages and most logs are Spanish; identifiers and class names are English. Keep new user-facing strings in Spanish.
- Every endpoint except `POST /api/auth/login` and `GET /api/health` (69 of 71 handlers) is guarded with `@PreAuthorize` combining `hasAuthority(T(...AppPermission).X.name())` (often `|| ADMINISTRATOR`) and `@securityService.isActive(authentication)`. Permission strings are also rows in the `role_permission` table, so adding an `AppPermission` enum value does not grant it to any role.
- Most non-admin operations are branch-scoped: services and specifications compare `user.getMember().getBranch().getId()`. Admins may pass `branchId`. Preserve this in any new query.
- Timestamps are stored in UTC (`OffsetDateTime`), business timezone is `America/Bogota`. Use `DateTimeUtils.toUTC`/`nowUTC` for writes and convert to Colombia on output; don't call local `now()`.
- Throw a dedicated `ApiException` subclass from `features/**/exceptions` for each business error; `GlobalHandlerException` maps it to `ErrorResponseDto`. Unhandled `DataIntegrityViolationException` maps to 409 (details stay in the log); anything else becomes a generic 500.
- `configurations/RateLimitFilter` limits `POST /api/auth/login` and `POST /api/ai/chat` with an in-memory fixed window per key (`utils/security/InMemoryRateLimiter`, bounded and evicting; it does not share state between instances). Exceeding it returns 429 with `ErrorResponseDto`. Keying uses `getRemoteAddr()`, not `X-Forwarded-For`, on purpose.

## Frontend

- `src/main/resources/static/` is committed, hashed Angular build output (no `package.json` here); the deployed bundle is `main-O2DCXEYA.js`. The real source is **`C:\dev\frontendmagico`** (Angular 21, remote `github.com/deimosoks/peces-a-tierra-frontend`). `C:\dev\flow frontend` is an obsolete lineage: do not use it. Rebuild in `frontendmagico` and deploy with `scripts/deploy-frontend.ps1`; never hand-edit `main-*.js`/`chunk-*.js`.
- `MvcConfig` serves `static/` and falls back to `index.html` for non-`api/` paths (SPA routing). Public routes: `/api/auth/login`, `/api/auth/refresh`, `GET /api/health`, plus static assets; all other `/api/**` need a Bearer JWT.

## AI endpoint caution

`POST /api/ai/chat` sends the user prompt to Gemini, extracts generated SQL, checks it only with `SqlQueryValidator` (keyword/`;` checks), then runs it via the dedicated `aiJdbcTemplate` (`ai.datasource.*` / `AI_DB_*`, read-only role). It passes a dummy `new User()` and applies no branch scoping. The validator is still a blacklist and H-01 remains open; treat changes here as security-sensitive. Since H-14 the SQL result is passed through `utils/ai/ResultRedactor` before it reaches the prompt: PII-named columns are masked, at most 50 rows are sent and a numeric summary covers the rest. Add any new sensitive column name to `ResultRedactor.PII_COLUMN_FRAGMENTS`.

## Testing reality

The suite has 47 classes and 262 tests: unit tests under `src/test/java/.../unit`, MockMvc/security tests
under `.../web`, and Testcontainers integration tests (`ApplicationContextIT`, `AttendanceExportIT`).
`.\gradlew.bat test` runs everything; the 4 integration tests are skipped when Docker is absent
(`@Testcontainers(disabledWithoutDocker = true)`), so the expected result is 262 tests / 0 failures /
0 errors / 4 skipped. (The wave of 2026-10-25 that closed H-41/H-13/H-40/H-29/H-16 left 39 classes /
209 tests; the H-27/H-42 wave added `DiscipuladoSpecificationTest` and two tests to `UserServiceTest`;
the H-14/H-19/H-18/H-17 wave added `ResultRedactorTest`, `InMemoryRateLimiterTest`,
`RateLimitFilterTest`, `RateLimitFilterRegistrationTest`, `JwtServiceTest`, `RefreshTokenServiceTest`
and two login-timing tests to `AuthServiceTest`; the H-32/H-39/H-42b wave added `ServiceServiceTest` and
new tests in `MemberServiceTest`, `AttendanceServiceTest`, `RoleServiceTest` and `DiscipuladoServiceTest`.)
`EndpointInventoryTest` pins the handler inventory at **71 handlers / 69 with `@PreAuthorize`** and
cross-checks `src/test/resources/security/endpoint-matrix.csv`, so adding or removing an endpoint
requires updating the constants and the CSV. Recent additions cover the discipulado date conversion and
null-filter branch scoping (`DiscipuladoSpecificationTest`, H-27), username uniqueness on rename
(`UserServiceTest`, H-42), the age-filter boundaries (`MemberSpecificationTest`), and the note contract
(`MemberNoteControllerTest`). The 409 handler added one test to each of the pre-existing
`GlobalHandlerExceptionTest` and `GlobalHandlerExceptionMvcTest`; neither is a new class. There is no
JaCoCo coverage, no CI pipeline and no linter.

The Gradle wrapper (8.14.3) does not start under the system JDK 25 (`major version 69`); run the build
with `JAVA_HOME` pointing at JDK 21 (H-38 still open).

Verify behaviour with `compileJava`, `test`, or a manual `bootRun` against a populated
database. Behaviour changes need a regression test in the same commit.
