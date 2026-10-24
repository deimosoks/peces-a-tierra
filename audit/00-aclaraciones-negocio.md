# Aclaraciones de negocio

Reglas confirmadas por el responsable del producto que descartan o matizan hallazgos de la auditoría.
Los borradores y verificaciones originales se conservan como evidencia histórica; este documento
manda sobre ellos para el roadmap.

| Fecha | Hallazgo | Decisión | Motivo | Efecto |
|---|---|---|---|---|
| 2026-10-24 | **H-06** / S-06 | **Descartado** | Un integrante de una sede puede asistir a un evento de otra sede. La asistencia es inter-sede por diseño. | No se valida la sede del evento ni la del integrante en `AttendanceService`; no se añade una prueba que exija rechazo. Se mantiene **H-41** (el lote se asigna al primer `serviceEventId`) como defecto de integridad independiente. |

## Consecuencias

- El consolidado pasa de **51 a 50 hallazgos vigentes** (1 crítica, 5 altas, 21 medias, 23 bajas).
- La Fase 1 del roadmap ya no incluye H-06.
- Cualquier prueba que exija rechazar una asistencia por sede sería incorrecta y no debe escribirse.
- La autorización por permiso (`REGISTER_ATTENDANCE`) y la validación de que el evento exista/esté
  activo y el integrante exista/esté activo siguen vigentes.
