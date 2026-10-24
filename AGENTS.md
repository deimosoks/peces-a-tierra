# AGENTS.md

Spring Boot 3.5.9 / Java 21 REST API with a prebuilt Angular PWA embedded in the jar. Single Gradle module; no frontend source in this repo.

## Commands (Windows/PowerShell)

```powershell
.\gradlew.bat bootRun          # API on :8080
.\gradlew.bat compileJava      # fastest compile check
.\gradlew.bat build            # compile + test + bootJar
.\gradlew.bat test --tests "org.icc.pecesatierra.PecesATierraApplicationTests"
```

No linter, formatter, type checker, CI, or codegen step is configured. Compile + test are the verification steps. `build/`, `bin/`, `.gradle/` are artifacts; never edit them.

## Commits

- One commit = one atomic change. It must compile on its own and must not mix unrelated fixes, refactors, formatting, dependency updates, or test suites.
- The message states what changed, why, and the finding/issue it addresses, e.g. `fix(baptism): persist note and municipality (H-07)`.
- A behaviour change and the regression test that proves it belong in the same commit. Purely independent test additions go in their own commit.
- Never commit `build/`, `.gradle/`, `bin/`, IDE files, secrets, or generated artifacts. Audit reports are committed separately.
- Before committing: `.\gradlew.bat compileJava`; run `.\gradlew.bat test` when behaviour changed.

## Required environment

`src/main/resources/application.properties` has no defaults for `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `GEMINI_API_KEY`, `AI_DB_URL`, `AI_DB_USER`, `AI_DB_PASS`; `PORT` defaults to 8080. `AI_DB_*` is a separate read-only role used only by the AI SQL executor. The `test` profile supplies literal values, so `.\gradlew.bat test` runs without real environment variables.

## Database

- No migrations, `schema.sql`, or seed data exist. `spring.jpa.hibernate.ddl-auto=validate`: PostgreSQL must already contain the schema/tables; Hibernate will not create or change them.
- The `unaccent` PostgreSQL extension is required (used by member search and AI-generated queries); queries fail without it.
- `spring.jpa.open-in-view=false`: access lazy associations only inside transactions or fetch them explicitly (`@EntityGraph`, joins). MapStruct mappers touching lazy fields outside a transaction will throw.
- IDs are UUID `String`s (`GenerationType.UUID`), never numeric.

## Code conventions

- Layout: `features/<domain>/` contains `controller/`, `service/`, `repository/`, `dtos/`, `mapper/`, `exceptions/`; the entity sits at the domain root. Cross-cutting specs live in `utils/specs/`.
- Response style is inconsistent: some controllers extend `BaseController` (`ApiResponse` envelope), others (e.g. `MemberController`) return raw DTOs. Match the controller being edited; don't standardize opportunistically.
- User-facing messages and most logs are Spanish; identifiers and class names are English. Keep new user-facing strings in Spanish.
- Every endpoint is guarded with `@PreAuthorize` combining `hasAuthority(T(...AppPermission).X.name())` (often `|| ADMINISTRATOR`) and `@securityService.isActive(authentication)`. Permission strings are also rows in the `role_permission` table, so adding an `AppPermission` enum value does not grant it to any role.
- Most non-admin operations are branch-scoped: services and specifications compare `user.getMember().getBranch().getId()`. Admins may pass `branchId`. Preserve this in any new query.
- Timestamps are stored in UTC (`OffsetDateTime`), business timezone is `America/Bogota`. Use `DateTimeUtils.toUTC`/`nowUTC` for writes and convert to Colombia on output; don't call local `now()`.
- Throw a dedicated `ApiException` subclass from `features/**/exceptions` for each business error; `GlobalHandlerException` maps it to `ErrorResponseDto`. Anything else becomes a generic 500.

## Frontend

- `src/main/resources/static/` is committed, hashed Angular build output (no `package.json` here). Do not hand-edit `main-*.js`/`chunk-*.js`; rebuild the frontend externally and copy its `dist`.
- `MvcConfig` serves `static/` and falls back to `index.html` for non-`api/` paths (SPA routing). Public routes: `/api/auth/login`, `/api/auth/refresh`, `GET /api/health`, plus static assets; all other `/api/**` need a Bearer JWT.

## AI endpoint caution

`POST /api/ai/chat` sends the user prompt to Gemini, extracts generated SQL, checks it only with `SqlQueryValidator` (keyword/`;` checks), then runs it directly via `JdbcTemplate` in `SqlExecutor`. It passes a dummy `new User()` and applies no branch scoping. Treat changes here as security-sensitive.

## Testing reality

There is only `contextLoads`; no unit, integration, fixture, or Testcontainers setup. Verify behavior through `compileJava`, a manual `bootRun` against a populated database, or by adding focused tests.
