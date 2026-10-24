# Fixes H-10, H-11 y H-12 — contrato HTTP y filtro JWT

- Rama: `audit/arquitectura-y-seguridad`
- Fecha: 2026-10-24
- Alcance: `GlobalHandlerException`, `JwtAuthFilter`, los cuatro servicios con
  `@RequestBody(required = false)` (`MemberService.search`, `AttendanceService.search`,
  `UserService.search`, `ServiceEventService.findForCalendar`) y `src/test/**`.
- No se modificaron `SecurityConfig`, el contrato de login/refresh, el handler de
  `ApiException` ni el de `BadCredentialsException`. No se añadieron dependencias.

## Resumen

Los tres hallazgos quedan cerrados:

- **H-10:** un cuerpo nulo en `/api/members/search`, `/api/attendances/search`,
  `/api/users/search` y `/api/services-events/calendar` ya no produce `NullPointerException`
  (500). El DTO se normaliza a una instancia vacía al inicio del servicio y el filtro de sede
  del usuario no administrador se sigue aplicando. El calendario, que exige fechas, responde
  400 de negocio (`InvalidStartAndEndDatesException`) en vez de 500.
- **H-11:** una denegación de permiso o de cuenta inactiva responde **403** (antes 401 con
  "Credenciales invalidas."); el login público ya no devuelve `ex.getMessage()`; el cuerpo
  ilegible, los errores de binding, los parámetros con tipo inválido y el ordenamiento
  inexistente responden **400** (antes 500 por el `catch (Exception.class)`).
- **H-12:** el filtro JWT captura toda `JwtException` y `IllegalArgumentException` alrededor de
  `extractUsername`, `loadUserByUsername` e `isTokenValid`, y también
  `AuthenticatedUserNotFoundException`/`UsernameNotFoundException`. Responde **401** con
  mensaje fijo y no propaga la excepción a `/error`.

## Tabla de cambios

| Hallazgo | Archivo:línea | Cambio | Test | Mutación |
|---|---|---|---|---|
| H-10 | `features/member/service/MemberService.java:125` | `dto == null` → `new MemberFilterRequestDto()` antes de `memberSpecification.build` y de `dto.getOrderBy()`. Con el DTO vacío el specification deja de tomar su rama `dto == null` (que devolvía `cb.conjunction()` y omitía la sede) y aplica el predicado de sede del no-admin. | `unit/filters/NullFilterBodyTest#memberSearchWithNullBodyUsesEmptyFiltersAndKeepsBranchScoping_H10` | M1: 4/6 fallos en `NullFilterBodyTest` (NPE / rama de conjunción). |
| H-10 | `features/attendance/service/AttendanceService.java:169` | `dto == null` → `new AttendanceFiltersRequestDto()` antes de `attendanceSpecification.build` y de `dto.getOrderBy()`. El predicado de sede ya estaba fuera del `if (dto != null)`; se conserva. | `NullFilterBodyTest#attendanceSearchWithNullBodyUsesEmptyFiltersAndKeepsBranchScoping_H10` | M1: falla con NPE. |
| H-10 | `features/user/service/UserService.java:193` | `dto == null` → `new UserFilterRequestDto()` antes de `userSpecification.build`. `UserSpecification` desreferenciaba el DTO en `dto.getQuery()`; el DTO vacío mantiene el filtro de sede del no-admin. | `NullFilterBodyTest#userSearchWithNullBodyUsesEmptyFiltersAndKeepsBranchScoping_H10` y `#userFilterDtoFromAForeignBranchStillCannotBeUsedByNonAdmin_H10` | M1: falla con NPE. |
| H-10 | `features/service/service/ServiceEventService.java:138` | `dto == null` → `new ServiceEventsFilterRequestDto()` antes de leer las fechas. Sin fechas se lanza `InvalidStartAndEndDatesException` (400); no se construye el specification porque el contrato exige el rango. La prueba adicional con fechas verifica que el specification real sigue filtrando por la sede del no-admin. | `NullFilterBodyTest#calendarWithNullBodyReturnsTheBusiness400InsteadOfNpe_H10` y `#calendarKeepsBranchScopingForNonAdminWhenDatesArePresent_H10` | M1: falla con NPE en `dto.getStartDate()`. |
| H-11 | `advice/GlobalHandlerException.java:29-31, 102-106` | Constantes de mensaje y `AuthorizationDeniedException` → **403** `"No tiene permisos para realizar esta acción."`. | `unit/advice/GlobalHandlerExceptionTest#authorizationDeniedIs403AndNot401_H11`; `EndpointSecurityMatrixTest#deniedRequestShouldReturn403_H11` y `assertDenied`. | M2: 11/20 fallos entre `GlobalHandlerExceptionTest` y `EndpointSecurityMatrixTest`. |
| H-11 | `advice/GlobalHandlerException.java:40-46` | `InternalAuthenticationServiceException` → 401 con mensaje fijo `"No autorizado."`; el detalle queda solo en el log del servidor. | `GlobalHandlerExceptionTest#internalAuthenticationFailureDoesNotLeakDetails_H11` | M2: falla al volver a `ex.getMessage()`. |
| H-11 | `advice/GlobalHandlerException.java:48-77` | Handlers nuevos para `HttpMessageNotReadableException`, `BindException`, `MethodArgumentTypeMismatchException` y `PropertyReferenceException` → **400** con mensajes en español. | `GlobalHandlerExceptionTest#unreadableBodyIs400`, `#bindingFailureIs400`, `#typeMismatchOnARequestParamIs400`, `#invalidSortPropertyIs400` | M2: los cuatro fallan si se revierten a 500. |
| H-11 | `src/test/.../web/security/EndpointSecurityMatrixTest.java:167-183, 201-209` | Se elimina `@Disabled` del caso 403 y `assertDenied` exige 403 + cuerpo (`status`, `error`, `message`, `path`); se añade un caso explícito de 401 solo para petición sin autenticar. | `EndpointSecurityMatrixTest` (10 tests) | M2: fallan los 5 casos de denegación. |
| H-12 | `configurations/JwtAuthFilter.java:55-88` | `try` cubre `extractUsername`, `loadUserByUsername` e `isTokenValid`; `catch (JwtException \| IllegalArgumentException)` → 401 `"Token inválido o expirado."`; `catch (AuthenticatedUserNotFoundException \| UsernameNotFoundException)` → 401 `"No autorizado."`. No se devuelve `e.getMessage()` ni se propaga la excepción. | `unit/auth/JwtAuthFilterTest` (7 tests) | M3: 5/7 fallos (firma, malformado, expirado, usuario borrado y firma en `isTokenValid`). |
| H-12 | `configurations/JwtAuthFilter.java:92-106` | Helper `writeUnauthorized` centraliza el JSON 401 (`ErrorResponseDto`) con UTF-8; se conserva el paso de la cadena para tokens válidos y peticiones sin `Authorization`. | `JwtAuthFilterTest#validTokenSetsAuthenticationAndContinuesTheChain`, `#requestWithoutAuthorizationHeaderContinuesWithoutAuthenticating` | Los dos casos válidos siguen verdes en M3, como debe ser. |

Los endpoints con `@RequestBody` requerido (Baptism, Discipulado, Report) no se tocaron.

## Tests añadidos/habilitados

| Suite | Tests | Detalle |
|---|---:|---|
| `unit/advice/GlobalHandlerExceptionTest` | 10 | 403, 401 interno sin fuga, 401 de credenciales, 400 de cuerpo ilegible, validación, binding, tipo de parámetro y ordenamiento, `ApiException` con su status y 500 genérico sin detalles. |
| `unit/auth/JwtAuthFilterTest` | 7 | Firma inválida, token malformado, expirado, usuario borrado, firma al validar, token válido y petición sin `Authorization`. Usa `MockHttpServletRequest`, `MockHttpServletResponse` y `MockFilterChain`; comprueba status, JSON, mensaje fijo y que la cadena no continúa. |
| `unit/filters/NullFilterBodyTest` | 6 | Los cuatro servicios con `dto == null`; se captura el `Specification` real y se verifica el predicado de sede con un árbol de criterios mockeado. Dos pruebas adicionales cubren una sede ajena explícita y el calendario con fechas. |
| `web/security/EndpointSecurityMatrixTest` | 10 | Caso 403 habilitado, `assertDenied` endurecido a 403 + cuerpo y nuevo caso 401 solo para falta de autenticación. |

## Resultado de `test`

Comando ejecutado (JDK 21):

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

Resultado: **BUILD SUCCESSFUL in 38s**, exit 0.

| Métrica | Valor |
|---|---:|
| Suites | 17 |
| Tests totales | 91 |
| Ejecutados | 87 |
| Saltados | 4 |
| Fallos | 0 |
| Errores | 0 |

Los 4 saltados son los que requieren Docker y ya estaban saltados antes del cambio:
`ApplicationContextIT` (1) y `AttendanceExportIT` (3). Línea base previa: 67 tests,
62 ejecutados, 5 saltados (el caso 403 estaba `@Disabled`), 0 fallos.

## Verificación de mutación

Copia temporal: `C:\Users\josef\AppData\Local\Temp\opencode\mutation-h10-h11-h12`
(sin `.git`, `build`, `.gradle` ni `bin`). Se guardó una copia de los archivos originales
y se restauró al terminar; los seis archivos de producción de la copia quedaron con SHA-256
idéntico a los del repositorio.

| Mutación | Reversión aplicada | Comando | Resultado |
|---|---|---|---|
| **M1 — normalización de DTO** | Se eliminaron las cuatro líneas `dto = dto == null ? ... : dto;`. | `gradlew test --tests "org.icc.pecesatierra.unit.filters.NullFilterBodyTest"` | **6 tests, 4 fallos**: member, attendance, user y calendar. Los dos casos con DTO no nulo siguen verdes. |
| **M2 — mapeo 403/400** | `AuthorizationDeniedException` vuelve a 401/"Credenciales invalidas."; el login vuelve a `ex.getMessage()`; los cuatro handlers nuevos devuelven 500. Se conservaron las firmas para que la suite compile (eliminarlas rompería la compilación del test, que también es detección). | `gradlew test --tests "org.icc.pecesatierra.unit.advice.GlobalHandlerExceptionTest" --tests "org.icc.pecesatierra.web.security.EndpointSecurityMatrixTest"` | **20 tests, 11 fallos**: 6 en `GlobalHandlerExceptionTest` y 5 en `EndpointSecurityMatrixTest`. El caso 401 sin autenticación sigue verde. |
| **M3 — captura del filtro** | Se restauró el `JwtAuthFilter` original (`catch (ExpiredJwtException \| MalformedJwtException)` y `loadUserByUsername` fuera del `try`). | `gradlew test --tests "org.icc.pecesatierra.unit.auth.JwtAuthFilterTest"` | **7 tests, 5 fallos**: firma inválida, malformado, expirado, usuario borrado y firma en `isTokenValid`. Token válido y petición sin cabecera siguen verdes (comportamiento que debía conservarse). |

Tras restaurar la copia se volvieron a ejecutar las cuatro suites nuevas y pasaron
(`BUILD SUCCESSFUL`).

## Efectos colaterales observados

- Las denegaciones por permiso o por `isActive=false` pasan de **401 a 403**. El cliente ya no
  debería interpretar un 403 como sesión expirada; el 401 queda para falta de autenticación o
  token inválido.
- La ausencia de cuerpo o un JSON ilegible en cualquier endpoint pasa de **500 a 400**
  (incluidos los endpoints con `@RequestBody` requerido). También pasan a 400 un `page` no
  numérico, un error de binding y un `orderBy` inexistente.
- El login con un fallo interno de `UserDetailsService` sigue devolviendo 401, pero ahora con
  `"No autorizado."` en lugar del mensaje interno. `BadCredentialsException` conserva su
  mensaje y su 401.
- Un token con firma inválida, malformado, expirado o de un usuario borrado responde 401 con
  JSON y mensaje fijo, en vez de dejar que la excepción llegue a `/error` y produzca 500.
- Un cuerpo nulo en los cuatro endpoints de búsqueda/calendario responde 200 con filtros
  vacíos y sede aplicada; en el calendario responde 400 porque las fechas son obligatorias.

## Límites de la verificación

- La verificación es unitaria con árbol de criterios mockeado; no hay Docker en esta máquina,
  así que las 4 pruebas de integración siguen saltadas y no se ejecutó contra PostgreSQL real.
- `POST /api/attendances/export` también acepta cuerpo nulo, pero su servicio no desreferencia
  el DTO y su specification ya aplica el filtro de sede fuera del `if (dto != null)`; se revisó
  y no requiere cambio.

