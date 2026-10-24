# TODO y riesgos aceptados

Decisiones de producto que aplazan un cambio de diseño. No son "descartados": siguen siendo deuda
técnica y deben retomarse cuando se cumpla su disparador.

## Estado al cierre del 2026-10-24

- Suite: **135 tests, 131 ejecutados, 4 saltados** (Docker), **0 fallos**.
- Abiertos: **H-01** (P0), **M-01** (PWA/403), **M-03 residual** (UUID 404/409), **M-06, M-07,
  M-09, M-10, M-18, M-21, M-22**.
- Siguiente paso recomendado: H-01 (vistas `ai_read` + validador) y rebuild del PWA para M-01.

## H-41 — El lote de asistencias debe llevar un solo `serviceEventId`

- **Estado**: aceptado temporalmente el **2026-10-24**. No se implementa en este ciclo.
- **Problema actual**: `POST /api/attendances` recibe `List<AttendanceRequestDto>` y cada elemento
  trae su propio `serviceEventId`, pero `AttendanceService.create` solo lee el del primero
  (`AttendanceService.java:58`) y construye todas las filas con ese evento (`:106-107`). La
  deduplicación (`:71`, `:118`) y la ventana horaria (`:94-101`) también usan solo el primer evento.
- **Diseño objetivo**: un DTO de lote con el evento una sola vez y una lista de asistentes.
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
  El servicio carga el evento una vez, valida estado/ventana una vez y rechaza integrantes
  duplicados en el lote. Eliminar `serviceDate`, que hoy se recibe y no se usa.
- **Bloqueo**: la PWA comprometida en `src/main/resources/static/` envía un array plano con
  `serviceEventId` repetido en cada elemento. El contrato nuevo le devolvería 400. No se puede
  editar el bundle a mano (`AGENTS.md`); hay que recompilar el frontend externamente o introducir
  `POST /api/attendances/batch` y dejar el endpoint actual deprecado durante una transición.
- **Riesgo aceptado**: hoy la PWA manda un solo evento, así que el defecto está latente. Un cliente
  externo, un script o una futura UI multi-evento puede atribuir asistencias al evento equivocado y
  descuadrar reportes/dashboard. No es una fuga entre sedes; es integridad de datos.
- **Disparadores para retomarlo**: rebuild del frontend Angular, aparición de un cliente API
  distinto de la PWA, o soporte de selección multi-evento.

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

- **Estado**: aceptado temporalmente el **2026-10-24**. No se toca el bundle precompilado.
- **Problema**: `src/main/resources/static/main-2X72WS5U.js` contiene
  `if(i.status===403)return n.logout(),Cs(()=>i)`. El backend ahora devuelve 403 para permisos
  denegados (H-11), así que un usuario autenticado sin permiso pierde la sesión al instante. Antes
  recibía 401 y el interceptor intentaba refrescar.
- **Opciones**:
  1. Recompilar la PWA para distinguir 403 de permiso (mostrar "sin permisos", sin logout) de 401 de
     sesión.
  2. Mantener 401 para denegaciones de permiso hasta que el cliente esté migrado (reabre H-11).
  3. Añadir un código de error estable en el cuerpo y actualizar el interceptor en el rebuild.
- **Riesgo aceptado**: logout inesperado y pérdida de trabajo en el cliente; no es un bypass de
  seguridad.
- **Disparador**: rebuild del frontend Angular o cambio del contrato de errores.

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

M-01 (PWA y 403) sigue en la sección anterior: el bloque M-02..M-24 no tocó el bundle ni el contrato
de 403.
