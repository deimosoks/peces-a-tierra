# TODO y riesgos aceptados

Decisiones de producto que aplazan un cambio de diseño. No son "descartados": siguen siendo deuda
técnica y deben retomarse cuando se cumpla su disparador.

## Estado al cierre del 2026-10-25 (ola de higiene y correctitud)

- Suite: **40 clases / 217 tests, 213 ejecutados, 4 saltados** (Docker), **0 fallos / 0 errores**.
  Al abrir la ola eran 39 clases / 209 tests; H-27 (6 tests nuevos) y H-42 (2 tests nuevos) suman 8.
  Inventario de seguridad sin cambios: **71 handlers / 69 con `@PreAuthorize`**.
- Cerrados en esta ola: **H-41** (`88178ea`), **H-13** (`49b9978`), **H-40** (`6cd0e67`), **H-29**
  (`3c40f4b`), **H-16** (`0d46cce`), **H-27** (`8aa866d`), **H-42 residual** (`01774f6`), **H-33**
  (`e876e1a`) y **H-37** (`cff2b16`).
- Abiertos: **H-01 (P0)**; altos **H-14, H-15, H-17, H-18, H-19, H-47**; medios **H-32, H-39,
  H-42b, H-51**; bajos/proceso **H-30, H-34, H-35, H-36, H-38, H-44, H-50** y **M-03 residual, M-06,
  M-07, M-09, M-10, M-18, M-21, M-22**.
- Despliegue: `src/main/resources/static/` no cambió; el bundle servido sigue siendo `main-PJ2GMY5K.js`.
  `src (2).zip` ya no está en `HEAD`, pero sigue en el historial (quitarlo exigiría reescribir `master`).
- Siguiente paso recomendado: H-01 (vistas `ai_read` + validador de lista blanca); es el único P0.

### Detalle de los hallazgos cerrados en esta ola

| Hallazgo | Qué se corrigió | Commit | Test de regresión |
|---|---|---|---|
| H-41 — el lote de asistencias se atribuía al primer evento | `AttendanceService.create` rechaza un lote con más de un `serviceEventId` (nueva `MultipleServiceEventsInBatchException`) | `88178ea` | tests nuevos en `AttendanceServiceTest` |
| H-13 — PII, notas pastorales y SQL en logs INFO | `MemberPersistenceService`, `MemberNoteService` y `AiService` dejan de registrar datos personales y notas | `49b9978` | `LogPrivacyTest` |
| H-40 — edades y ventana del dashboard con la zona del servidor | `MemberCategoryDeterminer`, `MemberMapper`, `MemberSpecification` y `DashboardService` calculan en `America/Bogota` | `6cd0e67` | `DashboardServiceTest`, `MemberMapperTest`, `MemberSpecificationTest` |
| H-29 — el reporte excluía el último día | `DateTimeUtils.startOfNextDayUTC` y límite superior exclusivo en el reporte y en bautismos | `3c40f4b` | `AttendanceReportSpecificationTest`, `BaptismSpecificationTest`, `DateTimeUtilsTest` |
| H-16 — CORS `*` con credenciales | `SecurityConfig` usa solo `CORS_ALLOWED_ORIGINS` (lista explícita; vacío = sin cabeceras) | `0d46cce` | `CorsConfigTest` |
| H-27 — discipulado interpretaba hora Bogotá como UTC | `DiscipuladoSpecification` convierte con `DateTimeUtils.toUTC` y aplica el filtro de sede también con DTO nulo | `8aa866d` | `DiscipuladoSpecificationTest` |
| H-42 residual — renombrar `username` sin unicidad de negocio | `UserService.update` valida `existsByUsernameAndIdNot` y lanza `UsernameAlreadyRegister` (409 con mensaje propio) tras comprobar sede y privilegios | `01774f6` | dos tests nuevos en `UserServiceTest` |
| H-33 — `BaseController`/`ApiResponse` y `features/log` sin uso | se eliminan las dos clases y la entidad; los 9 controladores dejan de extender `BaseController` sin tocar rutas, firmas ni códigos | `e876e1a` | ningún test los referenciaba; `EndpointInventoryTest` sigue verde |
| H-37 — dependencia duplicada y `src (2).zip` versionado | una sola declaración de `spring-boot-starter-web`; artefacto eliminado del índice con `git rm` | `cff2b16` | no aplica (build) |

## Estado al cierre de la ronda del 2026-10-25 (H-23…M-01)

- Suite: **158 tests en 29 clases, 154 ejecutados, 4 saltados** (Docker), **0 fallos / 0 errores**.
- Cerrados en la ronda: H-23, H-24, H-25, H-28, H-42, H-42a, H-43, H-45, H-49 y, con el despliegue del
  estático, **M-01**. Detalle, tests de regresión y evidencia en `audit/ronda-2026-10-25.md`.
- Abiertos al cerrar esa ronda: **H-01** (P0), **H-41** (cerrado después en `88178ea`), **H-42b**,
  **M-03 residual**, **M-06, M-07, M-09, M-10, M-18, M-21, M-22**, más los medios/bajos H-13 a H-19 y
  H-51.
- Despliegue: último commit de código `eb186ac`; estático `main-PCRBV2VD.js` (34 archivos, 0
  apariciones de `ui-avatars.com`). Sigue sin CI y sin migraciones (`ddl-auto=validate`).
- Siguiente paso recomendado: H-01 (vistas `ai_read` + validador de lista blanca); es el único P0.

### Hallazgos cerrados el 2026-10-25

| Hallazgo | Estado | Fecha | Commit |
|---|---|---|---|
| H-23 — NPE/foto huérfana en el alta sin foto | **RESUELTO** | 2026-10-25 | `af288e2` |
| H-24 — `categoryLocked` ignorado al actualizar integrante | **RESUELTO** | 2026-10-25 | `51a8ecc` |
| H-25 — 409 falso al guardar una subcategoría sin cambiar el nombre | **RESUELTO** | 2026-10-25 | `a6ecb1d` |
| H-28 — cuerpos sin `@Valid`/`@NotNull` en discipulado | **RESUELTO** | 2026-10-25 | `5041ae4` |
| H-42 — violaciones de integridad no controladas devolvían 500 | **RESUELTO** | 2026-10-25 | `2eb48f9` |
| H-42a — nombre de tipo duplicado al actualizar no se detectaba | **RESUELTO** | 2026-10-25 | `eb186ac` |
| H-43 — el filtro de edad perdía el último año de cada rango | **RESUELTO** | 2026-10-25 | `a6e0218` |
| H-45 — permisos repetidos rompían el alta/edición de rol | **RESUELTO** | 2026-10-25 | `9929952` |
| H-49 — logout y cambio de contraseña sin exigir cuenta activa | **RESUELTO** | 2026-10-25 | `8c5e88b` |
| M-01 — el PWA hacía `logout()` ante cualquier 403 | **RESUELTO** | 2026-10-25 | `ed937a8` |
| Estático PWA — bundle de julio con `ui-avatars.com` | **RESUELTO** | 2026-10-25 | `ed937a8` |

## Estado al cierre del 2026-10-24

- Suite: **135 tests, 131 ejecutados, 4 saltados** (Docker), **0 fallos**.
- Abiertos: **H-01** (P0), **M-01** (PWA/403), **M-03 residual** (UUID 404/409), **M-06, M-07,
  M-09, M-10, M-18, M-21, M-22**.
- Despliegue: `master` publicado en `origin/master` (`fdd5ea6`); producción incluye H-01 y M-01
  abiertos. `AI_DB_USER`/`AI_DB_PASS` son obligatorias en el entorno de producción.
- Siguiente paso recomendado: H-01 (vistas `ai_read` + validador) y rebuild del PWA para M-01.

## H-41 — El lote de asistencias debe llevar un solo `serviceEventId` (RESUELTO)

- **Estado**: **RESUELTO** el **2026-10-25** (`88178ea`). El cierre se hizo por la vía defensiva, sin
  cambiar el contrato que consume la PWA.
- **Qué cambió**: `AttendanceService.create` compara los `serviceEventId` del lote y, si hay más de uno,
  aborta con `MultipleServiceEventsInBatchException` (409) antes de deduplicar o de escribir filas. Ya no
  es posible que un lote mixto se atribuya en silencio al primer evento. La deduplicación y la ventana
  horaria siguen usando un único evento porque el lote lo garantiza.
- **Test de regresión**: los 177 líneas añadidas a `AttendanceServiceTest` cubren lote uniforme (se
  guarda), lote mixto (409 y ninguna fila) y el mensaje de la excepción.
- **Deuda que queda (no bloquea)**: el DTO de lote con el evento una sola vez sigue siendo el diseño
  objetivo. Cuando el frontend Angular (`C:\dev\frontendmagico`) se recompile se puede introducir
  `POST /api/attendances/batch` y deprecar el array plano.
- **Contexto histórico**: antes de `88178ea` el servicio leía solo el `serviceEventId` del primer
  elemento (`AttendanceService.java:58`) y construía todas las filas con él (`:106-107`); un cliente
  externo podía descuadrar reportes y dashboard. Era integridad de datos, no una fuga entre sedes.
- **Diseño objetivo (referencia)**:
  ```java
  class AttendanceBatchRequestDto {
      @NotBlank String serviceEventId;
      @NotEmpty @Valid List<AttendanceEntryRequestDto> attendances;
  }
  class AttendanceEntryRequestDto {
      @NotBlank String memberId;
      @NotNull LocalDateTime attendanceDate;
      String note;
  }
  ```

## H-42b — `Services.name` no tiene restricción única (ABIERTO)

- **Estado**: **ABIERTO**. El residual de H-42 sobre el renombrado de `username` se cerró aparte en
  `01774f6` (ola del 2026-10-25); este sigue siendo un hallazgo distinto y separado: la
  tabla `member_types` tiene `unique` en `name` y por eso el alta duplicada acaba en 409; la tabla
  `services` no lo tiene.
- **Problema actual**: `Services.java:22` declara `@Column(nullable = false)` sin `unique = true`, y
  `ServiceService.create`/`update` (`:31`, `:60`) no llaman a ningún `existsByName` equivalente al de
  `MemberTypeService:33,66`. Dos servicios con el mismo nombre conviven sin error: **duplicado
  silencioso**, no 500. El handler 409 de H-42 no lo cubre porque la base no lo rechaza.
- **Cierre propuesto**: validación de nombre único en `ServiceService` (con su excepción `ApiException`
  y su test) y, aparte, `unique` en la tabla, que es un cambio de esquema: hoy no hay migraciones y
  `ddl-auto=validate` no crea ni altera índices.
- **Riesgo**: reportes y filtros por servicio se duplican o se atribuyen al servicio equivocado; no
  es una fuga entre sedes.

## H-01 — Rol de base de datos de solo lectura para el endpoint de IA

- **Estado**: **rol creado en Neon** el 2026-10-24 y `SqlExecutor` cableado a un `JdbcTemplate`
  dedicado (`aiJdbcTemplate`) que usa `DB_URL` (o `AI_DB_URL` si se define) con las credenciales
  `AI_DB_USER`/`AI_DB_PASS`, `maxRows=1000` y `queryTimeout=10`. El template usa un
  `DriverManagerDataSource` interno (sin pool) para no retirar el `DataSourceAutoConfiguration` de
  JPA ni `@ServiceConnection` de Testcontainers.
- **Rol**: `peces_ai_readonly`, creado **por SQL**, nunca con el botón "Add role" de la consola: los
  roles creados desde consola/CLI/API reciben membresía en `neon_superuser` (`CREATEDB`,
  `CREATEROLE`, `BYPASSRLS`, `pg_read_all_data`/`pg_write_all_data`).
- **Acceso temporal**: `SELECT` sobre todas las tablas de `public` más `ALTER DEFAULT PRIVILEGES`,
  para no bloquear el trabajo. Es un riesgo aceptado y explícito.
- **Riesgo aceptado**: mientras el rol tenga `SELECT` sobre `public`, un SQL generado por el LLM
  puede leer `users.password_hash`, `refresh_token` y PII de todas las sedes. El rol limita la
  escritura, no la confidencialidad. **H-01 sigue abierto.**
- **TODO**:
  1. Crear el esquema `ai_read` con vistas que excluyan columnas sensibles y PII.
  2. Revocar `SELECT` sobre `public`; dejar `USAGE` + `SELECT` solo sobre `ai_read`.
  3. Definir `AI_DB_URL` con `currentSchema=ai_read` para la conexión de IA, dejando `DB_URL` para la app.
  4. Añadir filtro por sede (columna `branch_id` en las vistas + `SET LOCAL app.branch_id`, o RLS).
  5. Endurecer `SqlQueryValidator` y el prompt: lista blanca de vistas, sin `UNION`/comentarios/`;`.
  6. Rotar la contraseña (la actual quedó expuesta en el chat), guardarla en el gestor de secretos y
     no commitearla.
  7. Verificar con `information_schema.role_table_grants` que no puede leer `users`/`refresh_token`.
  8. Añadir una prueba de integración que espere `permission denied` al consultar `password_hash`.
  9. Valorar Hikari si sube el tráfico; hoy el `DriverManagerDataSource` abre una conexión por consulta.

Runbook con el SQL: `audit/scripts/neon-ai-readonly.sql`.

## M-01 — El PWA hace `logout()` ante cualquier 403

- **Estado**: **RESUELTO** el **2026-10-25** por el despliegue del build de la remediación
  (`ed937a8`). Queda solo como referencia histórica del defecto.
- **Problema (bundle anterior `main-2X72WS5U.js`)**:
  `if(i.status===403)return n.logout(),Cs(()=>i)`. El backend devuelve 403 para permisos denegados
  (H-11), así que un usuario autenticado sin permiso perdía la sesión al instante. Antes recibía 401 y
  el interceptor intentaba refrescar.
- **Verificación (bundle desplegado `main-PCRBV2VD.js`)**: la única rama que hace `logout()` es
  `n.status===403 && r.url.includes("/users/@me")`, es decir, el sondeo de sesión; un 403 de permiso en
  cualquier otro endpoint ya no cierra la sesión. Es el comportamiento que pedía la primera de las
  opciones barajadas en el 2026-10-24 (distinguir 403 de permiso de sesión inválida).
- **Residual**: si `/users/@me` responde 403 por una cuenta desactivada, el cliente cierra la sesión,
  que es el comportamiento buscado (H-49).

## Pendientes del bloque M-02..M-24 (2026-10-24)

Los IDs M-02 a M-24 se corrigieron en `audit/fixes-m02-m24.md` salvo los siguientes, que quedan
abiertos por decisión de alcance o por no ser verificables en esta máquina:

- **M-03 residual — oráculo de existencia de UUID (404 vs 409)**: los oráculos de datos
  (CC, asistencias, maestro) están cerrados, pero un no-admin aún distingue un recurso inexistente
  (404) de uno de otra sede (409) en `MemberService.update/delete/updateActive`,
  `UserService.create` y `BaptismService.create/invalid`. Cerrarlo exige devolver 404 también para
  recursos de otra sede, un cambio de contrato 409→404; no se aplicó. Riesgo bajo/medio de
  enumeración. Disparador: endurecer anti-enumeración o cambiar el contrato.
- **M-06 — pool/timeout del datasource de IA**: no se implementó. Un `HikariDataSource` interno
  cambia el ciclo de vida de la conexión de IA y no hay entorno con Neon para probar el fallo rápido;
  el driver PostgreSQL ya aplica `connectTimeout=10 s`. Disparador: subida de tráfico del endpoint o
  incidente de agotamiento de hilos.
- **M-07 — rol de IA en Neon**: **NO VERIFICABLE** sin acceso a Neon. Sigue el runbook
  `audit/scripts/neon-ai-readonly.sql` y el riesgo aceptado de H-01.
- **M-09 — mensaje accionable si faltan `AI_DB_USER`/`AI_DB_PASS`**: no se implementó. Hoy el arranque
  falla con el placeholder sin resolver, que ya es fail-closed y está documentado en `AGENTS.md`.
  Queda como nota de despliegue.
- **M-10 — bean explícito del `JdbcTemplate` principal**: no se implementó. `SqlExecutor` usa
  `@Qualifier("aiJdbcTemplate")` y hoy no hay ningún consumidor de `JdbcTemplate` por tipo; añadir el
  bean obliga a construir el `JdbcTemplate` principal a mano y retira la autoconfiguración.
- **M-18 — matriz completa de 69 endpoints**: no se completó. Requiere parametrizar las 69 filas del
  CSV con permiso esperado y un usuario que solo tenga ese permiso; es un cambio grande y separado del
  resto. La matriz actual cubre 4/69 y `PreAuthorizeSpelTest` sigue usando un superusuario.
- **M-21 — `Clock` en `DateTimeUtils`**: no se implementó. El test actual es determinista salvo por la
  tolerancia de 1 s; inyectar `Clock` obliga a tocar el constructor usado por todos los specs.
- **M-22 / H-01**: fuera de alcance por instrucción explícita (pertenece a H-01: vistas `ai_read`,
  RLS, validador SQL y `new User()`).

M-01 (PWA y 403) quedó **RESUELTO** el 2026-10-25 con el despliegue del bundle (`ed937a8`): el bloque
M-02..M-24 no lo tocó, lo cerró el rebuild del frontend.
