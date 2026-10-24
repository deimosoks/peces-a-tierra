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
