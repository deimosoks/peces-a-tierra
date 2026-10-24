# Auditoría de implementaciones — calidad, correctitud y pruebas

- **Rama**: `audit/arquitectura-y-seguridad` · **Fecha**: 2026-10-24
- **Base de comparación**: `master` (`f3d07cc`); el contrato dice `e3c0c83`, pero ese commit ya está dentro de la rama.
- **Alcance**: H-07, H-08, H-31, H-46; parte funcional de H-10/H-11/H-12/H-20/H-22; infraestructura de H-01; calidad de toda la suite; arranque, configuración y documentación.
- **Método**: lectura del código y de los tests reales (no de los informes), ejecución independiente de la suite, 16 mutaciones en una copia temporal fuera del repositorio, pruebas de contexto con `ApplicationContextRunner` y contraste de conteos. No se modificó código, tests, `build.gradle`, `application.properties` ni informes existentes.
- **Restricciones respetadas**: sin Docker, sin Neon, sin credenciales reales, sin commits/stash/cambio de rama. La copia temporal se eliminó; el árbol de trabajo solo conserva los archivos de auditoría no versionados.

## Veredicto global

**PARCIAL.** Los fixes asignados están implementados y las mutaciones directas de casi todos ellos se detectan. H-07, H-31, H-46, H-10, H-12, H-20 y H-22 son correctos para su contrato; H-08 y H-11 tienen residuos funcionales; H-01 está bien como infraestructura pero sigue abierto como hallazgo. La suite es una red **unitaria** útil para las líneas tocadas, pero **no es una red de seguridad end-to-end**: 4 IT se saltan, no hay contexto completo, no hay contrato de cliente y cuatro mutaciones estructurales no son detectadas por los tests que deberían cubrirlas (quitar el registro del filtro JWT deja la suite completa en verde). Además, el cambio 401→403 de H-11 rompe un flujo legítimo del PWA comprometido: el interceptor del bundle hace `logout()` ante cualquier 403.

## Resultado de ejecución

Comando ejecutado con JDK 21, sin variables de entorno reales:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

```text
BUILD SUCCESSFUL in 59s
EXIT=0
```

Agregado de `build/test-results/test/TEST-*.xml`:

| Métrica | Valor |
|---|---:|
| Suites | 21 |
| Tests | 106 |
| Ejecutados | 102 |
| Saltados | 4 |
| Fallos | 0 |
| Errores | 0 |

Saltados: `ApplicationContextIT` (1) y `AttendanceExportIT` (3), ambos por `@Testcontainers(disabledWithoutDocker = true)` al no haber Docker. Los conteos del `INFORME-FINAL.md:379` (106/102/4/0) son exactos. No hay `@Disabled` en la suite.

## Tabla por hallazgo

| H | Veredicto | Evidencia | ¿Test lo protege? | Riesgo residual |
|---|---|---|---|---|
| **H-07** | **CONFIRMADO** (código); datos históricos pendientes | `BaptismService.java:62` `.note(baptismRequestDto.getNote())`, `:68` `.municipality(baptismRequestDto.getMunicipality())`; entidad `Baptism.java:31,45`; el mapper real expone `note` (`BaptismMapper.java:18-21`). | **Sí.** `BaptismServiceTest#persistsNoteAndMunicipalityFromTheRequest_H07` (`:59-101`) captura la entidad y usa `@Spy BaptismMapper` real; mutar el builder produce 1/8 fallos. Los casos nulos no se prueban, pero no hay desreferencia: builder y mapper aceptan `null`. | Bautismos ya guardados conservan `note=null` y `municipality=city`; no hay backfill ni migración. `municipality` no está en `BaptismResponseDto`, así que el cliente no puede leerlo. `member.getBranch()`/`user.getMember().getBranch()` nulos siguen produciendo NPE. |
| **H-08** | **PARCIAL** | `BaptismService.java:54` y `:87` comparan `getBranch().getId().equals(...)`. `BaptismServiceTest:103-128` (create con dos instancias e igual id), `:215-236` (invalid con igual id), `:130-149` y `:192-213` (rechazo entre sedes), `:151-169` y `:238-259` (admin). | **Sí para `getId` vs `Branch.equals`.** Mutar ambas líneas a `Branch.equals` produce 2/8 fallos: `twoBranchInstancesWithTheSameIdAreTheSameBranch_H08` y `invalidAllowsTwoBranchInstancesWithTheSameId_H08`. **No distingue id de nombre**: mutar a `getName().equals(...)` deja 8/8 verdes porque ambos fixtures se llaman «Sede A» (SB-13). | La comprobación de bautismo duplicado se ejecuta **antes** de la de sede y su excepción incluye el nombre completo del integrante: fuga de PII y oráculo entre sedes para cualquier usuario con `BAPTISM_CREATE` (SB-02). `invalid` lanza `CannotCreateMembersOutsideYourBranch` con el mensaje de «crear integrantes» (`:89`). Sedes nulas → NPE (500), no 409. |
| **H-31** | **CONFIRMADO** | `MemberService.java:182-183` captura URL y `publicId` antes del borrado; `:206` `if (publicId != null) pictureUtils.delete(publicId);`; `@Transactional(rollbackFor = Exception.class)` en `:170`. | **Sí.** `MemberServiceTest#deleteSendsCloudinaryPublicIdInsteadOfUrl_H31` (`:54-70`) exige `delete("members/photos/abc123")` y prohíbe la URL; `#deleteWithoutPublicIdDoesNotCallCloudinary` (`:92-106`) cubre el registro legacy. Mutar a `delete(pictureUrl)` produce 1/3 fallos. | Registros con `pictureProfileUrl` y sin `publicId` quedan huérfanos en Cloudinary (aceptado). El orden es borrar fila → log → Cloudinary: si Cloudinary falla, la transacción revierte; si Cloudinary tiene éxito y el commit falla después, la foto se pierde y la fila queda. No hay prueba de rollback ni de fallo de Cloudinary. `MemberService.delete` comprueba asistencias **antes** de la sede (SB-09). |
| **H-46** | **CONFIRMADO** | `RestTemplateConfig.java:15-18`: `SimpleClientHttpRequestFactory`, `setConnectTimeout(Duration.ofSeconds(5))`, `setReadTimeout(Duration.ofSeconds(60))`, `new RestTemplate(requestFactory)`. Solo existe ese `RestTemplate` (`AiService.java:28,294`); no se aplicó a otros beans. | **Sí.** `RestTemplateConfigTest#restTemplateHasFiniteConnectAndReadTimeouts_H46` (`:23-36`) inspecciona la factory y exige 5 000/60 000 ms. Revertir a `new RestTemplate()` produce 1/1 fallo. | El test usa reflexión sobre campos privados de Spring; un cambio de implementación de `SimpleClientHttpRequestFactory` puede romperlo sin regresión real. No hay prueba de timeout efectivo contra Gemini ni de que 60 s sea suficiente para todas las consultas. El `RestTemplate` por defecto ya usaba `SimpleClientHttpRequestFactory`; el fix solo añade los timeouts, así que no cambia el cliente HTTP. |
| **H-10** | **CONFIRMADO** (funcional); prueba **PARCIAL** | Normalización en `MemberService.java:125`, `AttendanceService.java:169`, `UserService.java:193`, `ServiceEventService.java:138`; el calendario sin fechas lanza `InvalidStartAndEndDatesException` (400) antes del repositorio. `NullFilterBodyTest` cubre los cuatro servicios y la sede. | **Sí para la normalización.** Eliminar las cuatro líneas produce 4/6 fallos (member, attendance, user, calendar). **No para la aplicación del predicado**: los tests verifican `cb.equal(...)` pero no que el predicado esté en el `cb.and(...)` devuelto; mutar los cuatro specs a `return cb.conjunction()` deja 9/9 verdes (SB-10). | `MemberSpecification.java:28-30` sigue devolviendo `cb.conjunction()` si el DTO es nulo y `MemberService.export` (`:157-160`) no normaliza; hoy el controller exige cuerpo (`@RequestBody` requerido), pero es el mismo patrón de H-02 latente. La semántica de filtros vacíos es correcta: solo se aplica sede y no se añaden filtros. |
| **H-11** | **PARCIAL** | `GlobalHandlerException.java:33-106`: `AuthorizationDeniedException` → 403 (`:102-106`), `InternalAuthenticationServiceException` → 401 fijo (`:40-46`), `HttpMessageNotReadableException`/`BindException`/`MethodArgumentTypeMismatchException`/`PropertyReferenceException` → 400 (`:48-77`), `MethodArgumentNotValidException` → 400 (`:79-88`), `Exception` → 500 genérico (`:90-94`). No hay ambigüedad `BindException`/`MethodArgumentNotValidException`: en Spring 6 la segunda extiende la primera y el resolver elige el handler exacto. | **Parcial.** Mutar 403→401 produce 6/20 fallos (`GlobalHandlerExceptionTest` + `EndpointSecurityMatrixTest`). Los handlers de 400 se prueban **invocando el método directamente**, no a través de MVC: repuntar sus anotaciones `@ExceptionHandler` a otras excepciones deja 10/10 verdes (SB-12). El 500 residual y el `getFirst()` sin campo no se prueban. | Quedan 500 para `MissingServletRequestParameterException` (p. ej. `GET /api/services` sin `onlyActive`, `ServiceController.java:78`), método HTTP no soportado, `Content-Type` no soportado, `MaxUploadSizeExceededException` y `NoResourceFoundException` de `/api/**` (`MvcConfig.java:28-32`); comprobado con MockMvc en copia temporal (SB-03). Un error global de validación (sin `fieldErrors`) lanza `NoSuchElementException` desde `:83-86` (SB-04). **Regresión de cliente**: el PWA hace `logout()` ante 403 (SB-01). |
| **H-12** | **CONFIRMADO** | `JwtAuthFilter.java:55-88`: el `try` cubre `extractUsername`, `loadUserByUsername` e `isTokenValid`; `catch (JwtException \| IllegalArgumentException)` → 401 con mensaje fijo; `catch (AuthenticatedUserNotFoundException \| UsernameNotFoundException)` → 401. `writeUnauthorized` (`:92-107`) devuelve JSON UTF-8. El token válido y la petición sin cabecera siguen pasando. | **Sí para el filtro aislado.** `JwtAuthFilterTest` (7 tests) cubre firma, malformado, expirado, usuario borrado, firma en `isTokenValid`, token válido y sin cabecera. Estrechar el `catch` a `MalformedJwtException` produce 3/7 fallos. **No cubre el registro del filtro en la cadena**: quitar `.addFilterBefore(jwtAuthFilter, ...)` de `SecurityConfig.java:74` deja la suite completa en 106/102/4/0 (SB-11). | No hay prueba de la cadena real (`JwtAuthFilter` se mockea en la matriz). Un `DataAccessException` del `UserDetailsService` sigue produciendo 500. Un cliente que envíe un `Authorization` viejo a `/api/auth/login` o `/refresh` recibe 401 antes del endpoint; el PWA evita la cabecera en esas rutas (`chunk`/`main` interceptor), pero un cliente externo no. |
| **H-20** | **CONFIRMADO** | `BranchService.java:69` y `:115` llaman a `assertCanManageBranch` antes de mutar/consultar registros; helper `:141-146` compara `user.getMember().getBranch().getId()` con `branch.getId()` y exime a `ADMINISTRATOR`. `BranchServiceTest` cubre update/delete propios, ajenos y admin. | **Sí.** Quitar las dos llamadas produce 3/7 fallos: `updateFromAnotherBranchIsRejectedWithoutMutating_H20`, `deleteFromAnotherBranchIsRejectedBeforeCheckingRecords_H20` y `deleteAnotherBranchWithRecordsIsRejectedAsScopeViolation_H20`. Los flujos legítimos (no-admin en su sede, admin entre sedes) están verdes. | No se prueba el borrado de la sede propia con registros ni el de un admin con registros; el comportamiento es el preexistente (`CannotDeleteBranchWithRecords`). `user.getMember()`/`getBranch()` nulos → NPE. Si `role_permission` da `BRANCH_UPDATE`/`BRANCH_DELETE` a roles operativos, pierden el acceso cruzado (efecto buscado, documentado). |
| **H-22** | **CONFIRMADO** (persistencia); residuos en la capa superior | `MemberPersistenceService.java:147-149` comprueba la sede inmediatamente después de `findById` y antes de `beforeUpdate`/mapper; `:164-171` conserva la reasignación de sede solo para admin. `MemberPersistenceServiceTest` cubre rechazo antes del mapper, sede propia y admin. | **Sí en la capa de persistencia.** Quitar el bloque produce 1/3 fallos (`updateFromAnotherBranchIsRejectedBeforeMapper_H22`); el stub `lenient` del mapper reproduce la fuga del nombre si la comprobación se ejecuta tarde. No se prueba el HTTP ni `MemberService.update`. | `MemberService.update` (`:90-106`) comprueba `existsByCc` y sube la foto **antes** de delegar en la persistencia; un no-admin puede seguir usando el oráculo de CC y provoca una subida transitoria que luego se borra en el `catch` si la sede es ajena. La excepción sigue siendo `CannotCreateMembersOutsideYourBranch` («crear integrantes») para una actualización. El test no distingue id de nombre en los fixtures (misma debilidad que H-08). |
| **H-01 (infra)** | **PARCIAL**: infraestructura **CONFIRMADO**, H-01 global abierto | `AiDataSourceConfig.java:33-50` expone `aiJdbcTemplate` sobre `DriverManagerDataSource` interno, `setMaxRows(1000)` y `setQueryTimeout(10)`; `SqlExecutor.java:15,27` lo recibe con `@Qualifier("aiJdbcTemplate")` y no tiene `@Transactional`; `application.properties:24-28` define `ai.datasource.*` y `${AI_DB_URL:${DB_URL}}`; `AI_DB_USER`/`AI_DB_PASS` no tienen default. | **Parcial.** `AiDataSourceConfigTest` (3) y `SqlExecutorTest` (2) están verdes; mutar `AI_MAX_ROWS` a 0 produce 1/3 fallos. **No se prueba** el fallback `AI_DB_URL→DB_URL`, el arranque sin `AI_DB_USER`, ni la interacción real con `DataSourceAutoConfiguration`/`JdbcTemplateAutoConfiguration` (SB-06/07/08). `ApplicationContextIT` se salta. | `bootRun` falla si no existen `AI_DB_USER`/`AI_DB_PASS`; el test no lo detecta porque `application-test.properties` inyecta valores directos. `DriverManagerDataSource` no tiene pool ni límite de conexiones (una conexión por consulta; sin fuga por consulta, pero sin cota). El único límite de escritura es el rol externo: sin `@Transactional(readOnly=true)`, si `AI_DB_USER` se configura con un rol escribible, `SELECT ... INTO` (no bloqueado por `SqlQueryValidator`) podría escribir. El rol `peces_ai_readonly` y sus grants no son verificables sin Neon. |

**Conteo de veredictos**: 7 CONFIRMADO (H-07, H-31, H-46, H-10, H-12, H-20, H-22), 3 PARCIAL (H-08, H-11, H-01), 0 REFUTADO, 0 NO VERIFICABLE como veredicto (los límites de Docker/Neon se detallan al final).

## Calidad de la suite

| Área | Fortaleza | Debilidad | Veredicto |
|---|---|---|---|
| Unitarios de fixes | Aserciones sobre entidad guardada, orden de llamadas y `never()`; `BaptismServiceTest` usa el mapper real; `MemberPersistenceServiceTest` simula la fuga del nombre. | `NullFilterBodyTest`/`AttendanceSpecificationTest` verifican la construcción del predicado, no su aplicación (SB-10). `MemberServiceTest` no prueba el orden registros/sede (SB-09). | **Buena para las líneas; incompleta para el contrato.** |
| Contrato HTTP | `EndpointSecurityMatrixTest` prueba 403 + cuerpo y 401 sin autenticar con `MockMvc`; `GlobalHandlerExceptionTest` cubre los cuerpos 400/500. | Solo 4/69 handlers; `JwtAuthFilter`/`SecurityService` mockeados; los handlers de 400 se invocan directamente, sin anotación MVC (SB-12). El CSV no valida `authority` ni `expectedWithoutPermission` (`EndpointInventoryTest.java:111-116`). | **Parcial.** |
| JWT | `JwtAuthFilterTest` verifica status, JSON, mensaje fijo, `SecurityContext` y que la cadena no continúa. | No verifica el registro del filtro en `SecurityConfig`; quitar `.addFilterBefore` deja 106/102/4/0 (SB-11). No hay prueba de la cadena completa. | **Parcial.** |
| Multi-sede | H-20/H-22 prueban denegación antes de mutar, sede propia y admin; H-02/H-10 verifican el predicado de sede. | La mayoría de los tests de specs son con `CriteriaBuilder` mockeado; no hay IT ejecutado. H-08 no distingue id de nombre (SB-13). | **Parcial.** |
| IA / datasource | `AiDataSourceConfigTest` comprueba maxRows/queryTimeout/URL/usuario; `SqlExecutorTest` comprueba delegación y propagación; no toca Neon. | Runner aislado, sin `DataSourceAutoConfiguration`/`JdbcTemplateAutoConfiguration`; sin fallback `AI_DB_URL`; sin arranque sin `AI_DB_USER`; sin contexto completo (SB-06/07/08). | **Parcial.** |
| Integración | `ApplicationContextIT` y `AttendanceExportIT` están bien diseñados para Testcontainers. | **No se ejecutan** sin Docker; el perfil de test apunta el datasource de IA a `localhost`, no al contenedor (SB-07); no hay CI ni JaCoCo. | **NO VERIFICABLE.** |
| Determinismo | Sin `Thread.sleep`, sin orden entre tests, fixtures con IDs; `JwtAuthFilterTest` limpia `SecurityContext`. | `DateTimeUtilsTest` usa reloj real con tolerancia de 1 s (SB-15); `PreAuthorizeSpelTest` tiene estado estático (`applicationContext`, `securityService`) y no soporta paralelismo. | **Aceptable con deuda.** |
| Cobertura | 106 tests / 21 suites; ningún `@Disabled`. | Sin JaCoCo; no se puede afirmar cobertura por capa. | **NO VERIFICABLE.** |

## Mutaciones

Todas en `C:\Users\josef\AppData\Local\Temp\opencode\audit2-mut` (copia sin `.git`, `build`, `.gradle`, `bin`), restaurada y eliminada al terminar. El repositorio no se tocó.

| # | Fix mutado | Mutación | Resultado | Veredicto |
|---|---|---|---|---|
| M1 | H-07 | `.note(...)`→`.neighborhood(getNote())`; `.municipality(getMunicipality())`→`getCity()` | 1/8 fallos: `persistsNoteAndMunicipalityFromTheRequest_H07` | **Detecta** |
| M2 | H-08 | `getId().equals`→`Branch.equals` en `create` e `invalid` | 2/8 fallos: los dos tests de dos instancias con igual id | **Detecta** |
| M3 | H-08 | `getId().equals`→`getName().equals` en ambas ramas | **0/8 fallos; 8 verdes** | **NO detecta** (SB-13) |
| M4 | H-31 | `delete(publicId)`→`delete(pictureUrl)` | 1/3 fallos: `deleteSendsCloudinaryPublicIdInsteadOfUrl_H31` | **Detecta** |
| M5 | H-46 | `new RestTemplate(requestFactory)`→`new RestTemplate()` | 1/1 fallo | **Detecta** |
| M6 | H-10 | Borrar las cuatro normalizaciones `dto == null ? new ...` | 4/6 fallos: member, attendance, user, calendar | **Detecta** |
| M7 | H-02 | Meter el predicado de sede dentro de `if (dto != null)` | 1/3 fallos: `nullFiltersForNonAdminStillRestrictsToTheUsersBranch_H02` | **Detecta** |
| M8 | H-20 | Quitar las dos llamadas a `assertCanManageBranch` | 3/7 fallos (update ajeno, delete ajeno, delete ajeno con registros) | **Detecta** |
| M9 | H-22 | Quitar la comprobación de sede de `MemberPersistenceService.update` | 1/3 fallos: `updateFromAnotherBranchIsRejectedBeforeMapper_H22` | **Detecta** |
| M10 | H-01 | `AI_MAX_ROWS=0` | 1/3 fallos: `aiJdbcTemplateHasDefensiveLimitsAndDedicatedDatasource_H01` | **Detecta** |
| M11 | H-11 | `AuthorizationDeniedException`→401/"Credenciales invalidas." | 6/20 fallos (1 handler + 5 matriz) | **Detecta** |
| M12 | H-12 | `catch (JwtException \| IllegalArgumentException)`→solo `MalformedJwtException` | 3/7 fallos (firma, expirado, firma en `isTokenValid`) | **Detecta** |
| M13 | H-04/H-05/H-21 | `return` temprano en las 5 validaciones de `UserService`/`RoleService` | 9/20 fallos (los 9 de denegación) | **Detecta** |
| M14 | H-02/H-10 | Los cuatro specs devuelven `cb.conjunction()` en vez de `cb.and(...)` | **0/9 fallos; 9 verdes** | **NO detecta** (SB-10) |
| M15 | H-12 | Quitar `.addFilterBefore(jwtAuthFilter, ...)` de `SecurityConfig` | **106/102/4/0; suite completa verde** | **NO detecta** (SB-11) |
| M16 | H-11 | Repuntar `@ExceptionHandler` de `HttpMessageNotReadable`, `BindException` y `PropertyReference` a otras excepciones | **0/10 fallos; verdes** | **NO detecta** (SB-12) |

Resumen: **12 mutaciones detectadas, 4 no detectadas**. Las cuatro no detectadas no son fallos de producción (el código actual es correcto), pero demuestran que la suite no protegería esas regresiones.

## Hallazgos nuevos

### SB-01 — El cambio 401→403 de H-11 hace que el PWA cierre la sesión ante cualquier permiso denegado
- **Severidad**: Alta (funcional; cliente comprometido).
- **Evidencia**: `src/main/resources/static/main-2X72WS5U.js:1` contiene el interceptor: `if(i.status===403)return n.logout(),Cs(()=>i);if(i.status===401)return Cs(()=>i)`. El servidor ahora devuelve 403 para `AuthorizationDeniedException` (`GlobalHandlerException.java:102-106`), mientras que antes devolvía 401. El bundle es el que se sirve en producción y no se puede editar a mano (`AGENTS.md:47`).
- **Impacto**: cualquier usuario autenticado que invoque un endpoint sin permiso (acción no visible, navegación directa, rol cambiado, `isActive=false`) recibe 403 y el cliente ejecuta `logout()`. Antes recibía 401 y el interceptor no ejecutaba `logout()` directo (intentaba refresh); ahora el cierre de sesión es inmediato. Es una regresión de flujo legítimo, no una mejora de seguridad.
- **Test**: ninguno; `EndpointSecurityMatrixTest` valida el 403 del servidor, no el contrato del cliente. El informe de H-11 asume que «el cliente ya no debería interpretar un 403 como sesión expirada», pero el cliente comprometido sí lo hace.
- **Remediación**: o se reconstruye el PWA para distinguir 403 de permiso (p. ej. código de error en el cuerpo) de 403 de sesión, o se mantiene 401 para denegaciones de permiso hasta que el cliente esté actualizado. No se puede cerrar H-11 solo con el cambio de servidor.

### SB-02 — `BaptismService.create` comprueba el bautismo duplicado antes de la sede y filtra el nombre del integrante
- **Severidad**: Alta (PII / oráculo entre sedes; confirmar con auditor 1).
- **Evidencia**: `BaptismService.java:49-57`: primero `existsByBaptizedMemberAndInvalidFalse(member)` y `throw new MemberAlreadyHasRegisteredBaptismActiveException(member.getCompleteName())`; después, en `:54`, la comprobación de sede. La excepción incluye el nombre (`MemberAlreadyHasRegisteredBaptismActiveException.java:6-8`). El test `BaptismServiceTest#createRejectsMemberWithAnActiveBaptism_H08` (`:171-190`) solo cubre la misma sede; no hay caso cruzado con bautismo activo.
- **Impacto**: un usuario con `BAPTISM_CREATE` de la sede A puede enviar el `memberId` de un integrante de la sede B. Si tiene bautismo activo, recibe 409 con el nombre completo; si no, recibe otro 409; si el UUID no existe, 404. Es un oráculo de existencia/estado y una fuga de nombre entre sedes. La comprobación de sede debería ir antes.
- **Test**: no lo protege; la mutación de orden no está cubierta.

### SB-03 — H-11 deja 500 para excepciones MVC comunes no contempladas
- **Severidad**: Media.
- **Evidencia**: `GlobalHandlerException.java:90-94` captura `Exception` y devuelve 500; no se extiende `ResponseEntityExceptionHandler` ni hay handlers para `MissingServletRequestParameterException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `MaxUploadSizeExceededException` o `NoResourceFoundException`. Ejemplo real: `ServiceController.java:78` exige `@RequestParam boolean onlyActive`; `GET /api/services` sin el parámetro produce `MissingServletRequestParameterException`. Comprobado con `MockMvc` standalone en copia temporal: **500**. `MvcConfig.java:28-32` devuelve `null` para recursos `api/`, lo que termina en `NoResourceFoundException` y, por el catch-all, 500 en vez de 404 (este último se deduce del código; no se ejecutó con el contexto completo).
- **Impacto**: el contrato «entrada inválida → 400» no se cumple para parámetros faltantes, método/mediatype incorrectos, multipart demasiado grande o rutas `/api` inexistentes; los clientes y el monitoreo ven errores de servidor.
- **Test**: ninguno; el `GlobalHandlerExceptionTest` solo cubre los tipos que tienen handler.

### SB-04 — `MethodArgumentNotValidException` con errores globales lanza `NoSuchElementException` (500)
- **Severidad**: Media (latente hasta que exista una constraint de clase).
- **Evidencia**: `GlobalHandlerException.java:83-86`: `exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage()` sin comprobar que haya `fieldErrors`. Un `@AssertTrue`/`@ScriptAssert` o cualquier error de objeto produce una lista vacía. Comprobado con una prueba temporal: el handler lanza `java.util.NoSuchElementException`; el `@ExceptionHandler(Exception.class)` lo convertiría en 500.
- **Impacto**: validación de clase → 500 en vez de 400. Hoy no hay constraints globales (`@AssertTrue` no aparece en `src/main`), por eso es latente.
- **Test**: ninguno.

### SB-05 — `AI_DB_USER`/`AI_DB_PASS` son obligatorias y rompen `bootRun` sin un mensaje de configuración claro
- **Severidad**: Media (despliegue).
- **Evidencia**: `application.properties:26-27` usa `${AI_DB_USER}`/`${AI_DB_PASS}` sin default; `AiDataSourceConfig.java:36-37` las inyecta con `@Value`. Con `ApplicationContextRunner` y el mismo grafo, sin `AI_DB_USER` el contexto falla con un placeholder sin resolver (`AI_DB_USER`), antes de cualquier conexión. `application-test.properties:8-9` inyecta `test`/`test`, por lo que la suite no lo detecta. `AGENTS.md:26` y `fixes-h01-datasource.md:136-138` lo documentan, pero `INFORME-FINAL.md` no lo pone en el estado de H-01.
- **Impacto**: cualquier entorno existente que no defina las dos variables nuevas deja de arrancar; el error es un `IllegalArgumentException` de placeholder, no un mensaje de «configure el rol de IA». Es un fail-closed deliberado, pero es una ruptura de despliegue que debe coordinarse.
- **Test**: ninguno; el perfil de test lo enmascara.

### SB-06 — El fallback `AI_DB_URL → DB_URL` no tiene prueba y el perfil de test lo sustituye
- **Severidad**: Media (regresión de producción silenciosa).
- **Evidencia**: `application.properties:25` `${AI_DB_URL:${DB_URL}}`; `application-test.properties:7` fija `ai.datasource.url=jdbc:postgresql://localhost:5432/peces_test`, por lo que los tests nunca evalúan el placeholder anidado. Verifiqué con una prueba temporal que el fallback y el override funcionan hoy, pero no está versionada. Cambiar la propiedad a `${AI_DB_URL}` (sin default) dejaría la suite verde y rompería el arranque en producción si `AI_DB_URL` no está definida.
- **Impacto**: el contrato «AI_DB_URL es opcional» no está protegido.
- **Test**: ninguno.

### SB-07 — El datasource de IA del perfil de test apunta a `localhost`, no al contenedor de Testcontainers
- **Severidad**: Media (infraestructura de pruebas).
- **Evidencia**: `application-test.properties:7-10` fija `ai.datasource.url=jdbc:postgresql://localhost:5432/peces_test`; `AbstractIntegrationTest.java:23-30` solo declara `@ServiceConnection` para el `PostgreSQLContainer`. `@ServiceConnection` sustituye `spring.datasource.*`, no `ai.datasource.*`. El comentario de `application-test.properties:2` («La URL/usuario/password son sustituidos por @ServiceConnection») es falso para las líneas 7-10.
- **Impacto**: cuando Docker esté disponible, `ApplicationContextIT` carga porque `DriverManagerDataSource` es perezoso, pero cualquier futuro `SqlExecutorIT`/prueba de IA se conectará a `localhost:5432` (o fallará) en vez de al contenedor. El aislamiento de la prueba de IA queda roto.
- **Test**: ninguno; el único IT de contexto no toca el datasource de IA.

### SB-08 — La interacción real de H-01 con `DataSourceAutoConfiguration`/`JdbcTemplateAutoConfiguration` no está en la suite
- **Severidad**: Media (cobertura).
- **Evidencia**: `AiDataSourceConfigTest.java:28-37` usa un `ApplicationContextRunner` con solo `AiDataSourceConfig`, `SqlExecutor` y `PropertyPlaceholderAutoConfiguration`; no incluye las autoconfiguraciones de JDBC. `driverManagerDataSourceIsNotExposedAsBean` y `hasSingleBean(JdbcTemplate)` son triviales en ese contexto. `ApplicationContextIT` (el único contexto completo) se salta. Verifiqué en una prueba temporal que hoy `DataSourceAutoConfiguration` sigue creando Hikari y `JdbcTemplateAutoConfiguration` se retira, pero esa comprobación no está versionada.
- **Impacto**: una futura anotación `@Bean DataSource`, un segundo `JdbcTemplate` o un cambio de condición de Boot podría romper el datasource principal o el qualifier sin que la suite lo detecte. El `JdbcTemplate` de IA es además el único bean de ese tipo: una inyección futura por tipo recibiría el datasource de IA, no el principal.
- **Test**: no.

### SB-09 — `MemberService.delete` comprueba asistencias antes de la sede: oráculo para integrantes de otra sede
- **Severidad**: Media (información; no permite borrar).
- **Evidencia**: `MemberService.java:175-179`: `if (attendanceRepository.existsByMember(member)) throw new CannotDeleteMemberWithRecords();` y después la comprobación de sede. El test `MemberServiceTest#deleteOnMemberFromAnotherBranchIsRejected` (`:72-89`) hace `when(attendanceRepository.existsByMember(member)).thenReturn(false)`, así que no ejerce el orden. Es el mismo patrón que H-20 corrigió en `BranchService` (sede antes de registros) y que H-22 corrigió en la actualización de integrantes.
- **Impacto**: un no-admin que apunte al UUID de un integrante de otra sede recibe 409 «tiene registros» o 409 «fuera de su sede» según el estado, lo que revela la existencia de asistencias ajenas. No hay fuga de nombre en el mensaje.
- **Test**: no.

### SB-10 — Los tests de specs no verifican que el predicado de sede se aplique
- **Severidad**: Media (falsa confianza en H-02/H-10).
- **Evidencia**: `NullFilterBodyTest.java:194-197` y `AttendanceSpecificationTest.java:88-92` invocan `toPredicate(...)` y luego `verify(cb).equal(branchIdPath, ...)`, pero no comprueban que el `Predicate` devuelto incluya esa condición. En la copia temporal, cambiar los cuatro specs para que devuelvan `cb.conjunction()` dejó **9/9 tests verdes**; el `cb.equal` se sigue invocando, pero se descarta. Lo mismo ocurriría si se eliminara `predicates.add(...)` conservando la llamada.
- **Impacto**: la suite no detectaría una regresión que construya el filtro y no lo aplique; es exactamente el fallo que H-02/H-10 debían impedir. Los tests son de «construcción», no de «consulta».
- **Test**: no; mutación M14.

### SB-11 — La suite no verifica que `JwtAuthFilter` esté registrado en la cadena
- **Severidad**: Media (cobertura de H-12).
- **Evidencia**: `JwtAuthFilterTest` construye el filtro con `new JwtAuthFilter(...)` y llama `doFilter` directamente; `EndpointSecurityMatrixTest` lo sustituye por `@MockitoBean` y usa `authentication(...)`. `SecurityConfig.java:74` es la única línea que lo inserta. Quitarla deja la suite completa en **106/102/4/0** (M15). `ApplicationContextIT` solo comprueba que el contexto carga.
- **Impacto**: una regresión que desactive la autenticación JWT (todas las peticiones quedarían 401) no se detecta. El filtro funciona hoy; el problema es la red.
- **Test**: no.

### SB-12 — Los handlers de 400 de H-11 se prueban sin sus anotaciones MVC
- **Severidad**: Media (cobertura de H-11).
- **Evidencia**: `GlobalHandlerExceptionTest.java:75-139` llama directamente a `handler.handleHttpMessageNotReadableException(...)`, `handleBindException(...)`, etc. Cambiar las anotaciones `@ExceptionHandler` de esos métodos a `IllegalStateException`/`UnsupportedOperationException`/`ArithmeticException` deja **10/10 verdes** (M16), aunque en producción esas excepciones ya no tendrían handler y caerían en el 500. La matriz solo prueba 403, no 400.
- **Impacto**: el contrato 400 de H-11 no está protegido de extremo a extremo; una anotación mal puesta o una ambigüedad futura no se detecta.
- **Test**: no.

### SB-13 — H-08 no distingue id de nombre y `invalid` usa la excepción equivocada
- **Severidad**: Baja.
- **Evidencia**: `BaptismServiceTest` construye dos `Branch` con id `branch-a` **y nombre** «Sede A» (`:105-106`, `:217-218`); mutar la producción a `getName().equals(...)` deja 8/8 verdes (M3). Además, `BaptismService.java:89` lanza `CannotCreateMembersOutsideYourBranch` («No puede crear integrantes fuera de tu sede.») al invalidar un bautismo, y `BaptismServiceTest.java:203-205` fija esa excepción. Es el patrón de H-36 (excepciones reutilizadas), pero en una rama tocada por H-08.
- **Impacto**: el test no protege la comparación por id frente a una comparación por nombre (dos sedes homónimas en ciudades distintas se tratarían como la misma); el mensaje de invalidación es engañoso.
- **Test**: parcial.

### SB-14 — Documentación desactualizada o falsa
- **Severidad**: Baja.
- **Evidencia**: `AGENTS.md:11` propone `test --tests "org.icc.pecesatierra.PecesATierraApplicationTests"`, clase eliminada en `396a934`; `AGENTS.md:56` afirma «There is only `contextLoads`; no unit, integration, fixture, or Testcontainers setup», falso con 106 tests y 21 suites; `AGENTS.md:52` dice que la IA ejecuta «directly via `JdbcTemplate`», ahora es `aiJdbcTemplate`. `INFORME-FINAL.md:410` afirma «Ninguno de estos archivos está commiteado todavía», pero los 19 artefactos del Anexo B están versionados (`git ls-files audit/`); `INFORME-FINAL.md:393` habla de ejecutar en «CI con Docker» y no existe `.github` ni CI. El alcance de 243 archivos Java es una foto histórica: hoy hay 250. El contrato dice base `e3c0c83`, pero el merge-base real es `f3d07cc`.
- **Impacto**: un agente o desarrollador que siga `AGENTS.md` ejecuta un test inexistente y cree que no hay suite; el informe declara no versionado lo que sí lo está.
- **Test**: no aplica.

### SB-15 — `DateTimeUtilsTest` depende del reloj real
- **Severidad**: Baja (flakiness).
- **Evidencia**: `DateTimeUtilsTest.java:52-61` compara dos lecturas consecutivas del reloj con tolerancia de 1 s: `assertThat(Math.abs(Duration.between(colombia, utcAsColombia).toMillis())).isLessThan(1_000L)`. No usa `Clock`; el plan de pruebas lo exige (`plan-pruebas.md:303`).
- **Impacto**: bajo carga o con un reloj ajustado, el test puede fallar sin regresión; además no verifica el instante, solo la coherencia de dos llamadas.
- **Test**: es el propio test; no cubre conversiones de negocio (H-26/H-27).

## Regresiones y efectos colaterales

1. **H-11 / PWA (SB-01)**: el paso de 401 a 403 para permisos denegados provoca `logout()` en el cliente comprometido. Es la regresión funcional más importante de la rama; H-11 no puede considerarse cerrado sin actualizar el PWA o cambiar el contrato de error.
2. **H-11 / contrato HTTP (SB-03)**: siguen apareciendo 500 para parámetros faltantes, método/mediatype no soportados, multipart grande y rutas `/api` inexistentes. El cambio mejora los casos tocados, no el contrato completo.
3. **H-11 / validación (SB-04)**: un error de validación de clase produce 500; latente mientras no haya constraints globales.
4. **H-11 / logs**: `ApiException` sigue haciendo `printStackTrace(System.err)` (`GlobalHandlerException.java:36`), igual que `BadCredentialsException` y el 500. El cuerpo no filtra el stacktrace, pero los logs se ensucian y un error de negocio genera traza completa.
5. **H-12 / cliente**: un `Authorization` viejo en `/api/auth/login` o `/refresh` devuelve 401 antes del endpoint; el PWA evita la cabecera en esas rutas, pero clientes externos pueden quedar bloqueados. No se probó.
6. **H-10 / calendario**: sin cuerpo o sin fechas ahora devuelve 400 de negocio; antes era 500. Es una mejora, pero el cliente debe manejar el 400.
7. **H-20/H-22 / roles operativos**: los no-admin pierden la gestión de sedes/integrantes de otras sedes. Es el comportamiento buscado; si `role_permission` daba `BRANCH_UPDATE`/`BRANCH_DELETE`/`UPDATE_MEMBER` a roles operativos, esos flujos cambian. No hay datos de `role_permission` en el repo.
8. **H-31 / Cloudinary**: los registros legacy con URL y sin `publicId` ya no intentan borrar y quedan huérfanos; los borrados nuevos dependen de que `publicId` exista. Si Cloudinary falla, la transacción revierte; si tiene éxito y el commit falla, la foto se pierde. No hay prueba de ninguno de los dos caminos.
9. **H-07 / datos**: las escrituras nuevas son correctas; los bautismos antiguos conservan `note=null` y `municipality=city`. No hay backfill; la pérdida de la nota original es irreversible.
10. **H-01 / despliegue**: `bootRun` y producción requieren `AI_DB_USER`/`AI_DB_PASS`; el perfil de test las enmascara. `AI_DB_URL` es opcional y funciona (verificado con prueba temporal). No hay pool: una conexión por consulta, sin cota; el rol externo es el único límite de escritura y `SELECT ... INTO` no está bloqueado por el validador.
11. **H-01 / tipo `JdbcTemplate`**: al retirarse `JdbcTemplateAutoConfiguration`, el único `JdbcTemplate` es el de IA. Hoy `SqlExecutor` es el único consumidor, pero una inyección futura por tipo recibiría el datasource de IA. Está documentado, no probado.
12. **H-08 / orden**: el no-admin ya puede operar en su sede, pero el duplicado se comprueba antes de la sede y filtra el nombre (SB-02); `invalid` devuelve el mensaje de creación de integrantes.
13. **Suite**: 4 IT saltados, sin contexto completo, sin CI, sin JaCoCo y con cuatro puntos ciegos estructurales (SB-10/11/12/13). El verde de 106 no equivale a «sin regresiones».

## Estado de H-41, H-06 y H-01

- **H-41**: aceptado temporalmente; el código sigue leyendo el evento del primer elemento (`AttendanceService.java:58`) y construyendo todas las filas con él (`:106-107`). `TODO.md:6-35` describe el problema, el diseño objetivo y el bloqueo de la PWA; es exacto. No hay test que lo proteja porque se decidió no corregirlo.
- **H-06**: descartado por regla de negocio; `AttendanceService.create` no valida sede del evento/integrante y no debe añadirse una prueba de rechazo. `00-aclaraciones-negocio.md:9` y `INFORME-FINAL.md:292-295` son coherentes. La autorización por permiso y las validaciones de evento/integrante activo siguen vigentes.
- **H-01**: la infraestructura de solo lectura está cableada, pero el hallazgo sigue abierto. `TODO.md:37-62` documenta el rol, el `DriverManagerDataSource`, los límites, el riesgo de `SELECT` sobre `public` y los pasos pendientes (vistas `ai_read`, revocación, RLS, validador, rotación, verificación de grants). No pude verificar la existencia del rol ni sus privilegios sin Neon; el código no aplica scoping por sede y `AiController` sigue pasando `new User()`.

## Documentación y conteos

- **Confirmado**: 106 tests, 102 ejecutados, 4 saltados, 0 fallos; 16 hallazgos con fix y prueba (H-02, H-03, H-04, H-05, H-07, H-08, H-09, H-10, H-11, H-12, H-20, H-21, H-22, H-31, H-46, H-48); 69 handlers y 65 `@PreAuthorize`; 21 suites; 4 tests de integración saltados.
- **Matizado**: «16 findings corregidos» es cierto para el código, pero H-08 y H-11 tienen residuos (SB-02/SB-01/SB-03/SB-04) y H-11 no debería figurar como cerrado end-to-end mientras el PWA cierre sesión ante 403. H-01 es parcial. H-07 no incluye backfill.
- **Incorrecto**: `INFORME-FINAL.md:410` («ninguno commiteado») y `INFORME-FINAL.md:393` («CI con Docker», sin CI); `AGENTS.md:11,56`; el alcance de 243 archivos es histórico (hoy 250); la base del contrato (`e3c0c83`) no es el merge-base (`f3d07cc`).
- **Coherente**: `fixes-h01-datasource.md` con el código y los tests; `fixes-h20-h22.md` con las líneas actuales; `fixes-fase0.md` con H-07/H-08/H-31/H-46; `fixes-h10-h11-h12.md` con los handlers, salvo la suposición sobre el cliente 403; `TODO.md` con H-41/H-01; `00-aclaraciones-negocio.md` con H-06.

## Cobertura y pruebas

- **Ejecución**: 21 suites, 106/102/4/0, JDK 21, sin variables de entorno. `BUILD SUCCESSFUL`, `EXIT=0`.
- **Unitarios**: 19 suites con tests ejecutados (16 unitarias y 3 web); los fixes asignados tienen al menos un test directo. Los tests de H-07 usan el mapper real; H-20/H-22 comprueban el orden de las llamadas; H-31 prohíbe la URL; H-46 inspecciona la factory; H-01 comprueba los getters del template.
- **Integración**: `ApplicationContextIT` (1) y `AttendanceExportIT` (3) se saltan. No se ejecutó PostgreSQL, `unaccent`, `create-drop`, Testcontainers ni el esquema de producción. La única prueba de contexto completo es, por tanto, **NO VERIFICABLE**.
- **Web**: `EndpointSecurityMatrixTest` cubre 4/69 handlers (users delete, services delete/list, health) y mockea `JwtAuthFilter`/`SecurityService`; `EndpointInventoryTest` solo compara controller+verbo+ruta y no lee las columnas `authority`/`bodyFixture`/`expectedWithoutPermission`. El CSV conserva tres filas `PENDING` (`endpoint-matrix.csv:31,38,65`) y el valor `pending` en la última columna de todas las filas.
- **Errores**: `GlobalHandlerExceptionTest` prueba los cuerpos, pero no el wiring MVC de los 400 (SB-12); el 403 sí está cubierto por la matriz; el 500 residual no.
- **Tokens de refresco**: `UserServiceTest#passwordChangeRevokesRefreshTokens_H05` solo comprueba que la colección en memoria queda vacía después de `clear()`; no verifica el borrado en base de datos. La entidad `User` tiene `orphanRemoval = true` (`User.java:47-49`), pero ningún test ejecuta el flush/delete real.
- **JWT**: 7 tests del filtro aislado; no hay prueba de la cadena (SB-11).
- **IA**: 5 tests unitarios; no hay prueba de `AI_DB_URL`, `AI_DB_USER`, autoconfiguración ni base real (SB-06/07/08).
- **Determinismo**: sin `Thread.sleep` ni orden entre tests; `JwtAuthFilterTest` limpia el contexto; `DateTimeUtilsTest` usa reloj real (SB-15); `PreAuthorizeSpelTest` usa estado estático y no soporta paralelismo.
- **Cobertura de línea**: no hay JaCoCo ni informe; **NO VERIFICABLE**.
- **CI**: no existe `.github`; la suite no se ejecuta en un pipeline. `AGENTS.md:14` lo dice, pero `INFORME-FINAL.md:393` sugiere lo contrario.
- **Cliente**: no hay tests del PWA; el hallazgo SB-01 es evidencia estática del bundle.

## Límites

- **Docker no disponible**: los 4 tests de integración se saltan; no se ejecutó el contexto completo, ni PostgreSQL, ni `unaccent`, ni el esquema `create-drop`, ni `JpaTransactionManager`. Todo lo relativo a JPA/Testcontainers es **NO VERIFICABLE** en esta máquina.
- **Neon no consultado**: no se verificó la existencia del rol `peces_ai_readonly`, sus grants, `AI_DB_URL`, la rotación de contraseña ni que `SELECT` sobre `public` esté limitado. El script `audit/scripts/neon-ai-readonly.sql` contiene placeholders, no credenciales.
- **Cloudinary/Gemini**: no se llamaron APIs reales; H-31 se probó con mocks y H-46 con reflexión sobre la factory. No se midió el timeout efectivo ni el borrado real.
- **PWA**: no se ejecutó el bundle; SB-01 se basa en el interceptor minificado. No hay fuentes Angular ni `package.json` en el repo.
- **Sin `bootRun` end-to-end**: el comportamiento de `AI_DB_USER`/`AI_DB_URL` se comprobó con `ApplicationContextRunner`, no con el servidor y una base real.
- **Sin JaCoCo/CI**: no hay métricas de cobertura ni ejecución reproducible en pipeline.
- **`role_permission`**: no está en el repo; los flujos de H-04/H-05/H-20/H-21 dependen de datos externos.
- **Mutaciones**: 16 escenarios sobre una copia temporal; no es un análisis exhaustivo de todas las ramas. La copia se restauró y eliminó; el repo no se modificó.
- **Contrato**: la base declarada (`e3c0c83`) no coincide con el merge-base real (`f3d07cc`); se usó `master..HEAD` como rango efectivo.
