# Fase 0 — Correcciones aplicadas

- **Rama**: `audit/arquitectura-y-seguridad`
- **Fecha**: 2026-10-23
- **Alcance**: los 7 arreglos de la Fase 0 (H-02, H-07, H-08, H-09, H-31, H-46, H-48).
- **Restricciones respetadas**: sin commits, sin cambio de rama, sin tests, sin dependencias nuevas,
  sin tocar `audit/borrador-*.md`, `audit/verificacion-*.md` ni `audit/INFORME-FINAL.md`.

## Cambios

| ID | Archivo | Líneas antes | Líneas después | Qué cambió |
|---|---|---|---|---|
| H-09 | `features/user/controller/UserController.java` | 111 | 111 | `hasAuthority(org...AppPermission)` → `hasAuthority(T(org...AppPermission))` en el `@PreAuthorize` de `DELETE /api/users/{userId}`. Único cambio; no se tocaron otros `@PreAuthorize`. |
| H-48 | `features/service/controller/ServiceController.java` | 55-59 | 55-61 | Se envolvió `DELETE_SERVICE \|\| ADMINISTRATOR` en paréntesis para que `&& @securityService.isActive(authentication)` aplique también al admin. Se mantuvo el estilo de texto bloque. |
| H-02 | `utils/specs/AttendanceSpecification.java` | 37, 60-64 | 37-78, 80-84 | El predicado de sede salió del `if (dto != null)`. No-admin: filtra siempre por `user.member.branch.id`. Admin: mantiene `dto != null && dto.getBranchId() != null`. El resto de filtros y su orden interno no cambiaron. |
| H-07 | `features/baptism/service/BaptismService.java` | 62, 68 | 62, 68 | `.neighborhood(note)` → `.note(note)`; `.municipality(getCity())` → `.municipality(getMunicipality())`. Se eliminó el `.neighborhood(getNote())` duplicado. |
| H-08 | `features/baptism/service/BaptismService.java` | 54, 87 | 54, 87 | Ambas comparaciones de sede pasaron de `Branch.equals(...)` a `getBranch().getId().equals(user.getMember().getBranch().getId())`. No se añadió `equals/hashCode` a la entidad. |
| H-31 | `features/member/service/MemberService.java` | 180, 203 | 180-181, 204 | Se capturó `member.getPublicId()` antes del borrado y `pictureUtils.delete(...)` recibe el `public_id`, no la URL. `pictureUrl` se conserva únicamente para el log/booleano. |
| H-46 | `utils/RestTemplateConfig.java` | 10-11 | 13-18 | `RestTemplate` construido con `SimpleClientHttpRequestFactory`: connect timeout 5 s y read timeout 60 s mediante `Duration`. Firma del bean e inyección sin cambios. |

Resumen de diff: 6 archivos modificados, 25 inserciones y 14 eliminaciones.

## Resultado de compilación

Comando ejecutado con JDK 21:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat compileJava
```

```text
> Task :compileJava

BUILD SUCCESSFUL in 24s
1 actionable task: 1 executed
```

Sin errores ni warnings de compilación. No se ejecutaron `test` ni `bootRun`.

## Puntos no aplicados

Ninguno. Los 7 hallazgos coincidieron con el código actual y se aplicaron sin necesidad de cambios
mayores ni improvisaciones.

## Observaciones y riesgos

- **H-07 — datos ya corruptos**: el arreglo solo corrige escrituras nuevas. Los bautismos ya
  registrados tienen `note = null` (la nota se descartó y no es recuperable) y `municipality = city`
  (el municipio real enviado por la PWA se perdió). Requiere revisión/backfill manual; no se puede
  reconstruir de forma fiable desde el esquema actual.
- **H-08 — efecto funcional**: antes, los no-admin no podían crear ni invalidar bautismos porque la
  comparación de `Branch` siempre daba `false`. Ahora operan sobre integrantes de su propia sede.
  Persiste el riesgo preexistente de NPE si `member.getBranch()` o la sede del usuario fueran null;
  no se añadió defensa para no ampliar el alcance.
- **H-08 — inconsistencia menor**: `create` sigue usando el literal `"ADMINISTRATOR"` y `invalid`
  usa `AppPermission.ADMINISTRATOR.name()`; ambos funcionan con `hasAuthority(String)` y se dejaron
  intactos para mantener el diff mínimo.
- **H-02 — orden de predicados**: el predicado de sede se evalúa ahora después de los filtros
  opcionales (antes estaba entre `invalid` y `category`). Al combinarse con `AND` la semántica es
  idéntica; solo puede variar el orden textual de las condiciones en el SQL generado. Un admin con
  `dto == null` sigue viendo todas las sedes; un no-admin con `branchId` ajeno sigue viendo solo la
  suya.
- **H-09 y H-48 — SpEL**: `compileJava` no valida expresiones SpEL; solo se comprobó la sintaxis por
  inspección. Conviene verificar en runtime que `DELETE /api/users/{id}` y
  `DELETE /api/services/{id}` arrancan y autorizan correctamente. En H-48, un admin inactivo ahora
  recibe 403 (antes eludía `isActive`), que es el comportamiento buscado.
- **H-31 — orden de borrado**: se mantuvo el orden existente (borrar fila → log → borrar foto en
  Cloudinary). `publicId` se captura antes del `delete`. Si Cloudinary falla, `FileErrorException`
  revierte la transacción; si Cloudinary tiene éxito y el commit de BD falla después, la foto queda
  eliminada y la fila permanece (efecto externo no reversible). Los registros antiguos con
  `pictureProfileUrl` pero sin `publicId` ya no intentan borrar y quedan huérfanos en Cloudinary.
- **H-46 — timeouts**: 60 s de lectura acota el bloqueo de hilos, pero consultas a Gemini
  excepcionalmente lentas podrían agotar el timeout antes que con el comportamiento anterior (sin
  límite). Conecta en 5 s, por lo que un fallo de red se reporta rápido.
