# TODO y riesgos aceptados

Decisiones de producto que aplazan un cambio de diseño. No son "descartados": siguen siendo deuda
técnica y deben retomarse cuando se cumpla su disparador.

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
