# Auditoría de implementaciones — seguridad y aislamiento

- **Rama**: `audit/arquitectura-y-seguridad` (`7609417`)
- **Fecha**: 2026-10-24
- **Eje**: seguridad, autorización, aislamiento entre sedes, contrato de errores y aislamiento del endpoint de IA.
- **Alcance**: H-02, H-03, H-04, H-05, H-09, H-10, H-11, H-12, H-20, H-21, H-22, H-48 y la parte de seguridad de H-01 (datasource/rol de IA).
- **Método**: lectura del código real (no de los informes), ejecución de la suite, pruebas temporales de contrato y mutaciones en una copia fuera del repositorio. No se usó Neon, Docker ni credenciales reales.

## Veredicto global

**APTA PARA SEGUIR, NO APTA PARA FUSIONAR NI PARA DECLARAR CERRADOS H-01, H-11 y H-22.**

Los diez arreglos de autorización/aislamiento revisados (H-02, H-03, H-04, H-05, H-09, H-10, H-12, H-20, H-21 y H-48) están realmente en el código, se ejecutan antes de las mutaciones y sus pruebas fallan al revertirlos. La suite está verde: 106 tests, 102 ejecutados, 4 saltados, 0 fallos.

Quedan tres brechas:

1. **H-01 sigue siendo crítica**: el cableado a `aiJdbcTemplate`, el rol separado y los límites de filas/tiempo son correctos, pero el rol, según el runbook, conserva `SELECT` sobre todo `public` —incluidas `users.password_hash`, `refresh_token` y PII de todas las sedes—, no hay filtro por sede, el controlador sigue pasando `new User()` y el validador sigue aceptando `UNION`, `information_schema` y `pg_sleep`. La pertenencia del rol a `neon_superuser` no es verificable desde el repositorio.
2. **H-11 es parcial**: los casos enumerados en el informe (403/401, cuerpo ilegible, binding, tipo de parámetro y `orderBy`) están corregidos, pero el `@ExceptionHandler(Exception.class)` sigue convirtiendo en 500 un parámetro requerido ausente, un método HTTP no soportado o un `Content-Type` no soportado. Verificado con una prueba temporal: `GET /api/services` sin `onlyActive` devuelve **500**, no 400.
3. **H-22 es parcial**: la fuga del **nombre** por el mapper está cerrada, pero el **oráculo de UUID** sigue vivo (`404` si no existe, `409` si existe en otra sede) y se suman oráculos de CC, de registros de asistencia y de existencia de maestros de discipulado.

Además, el cambio de contraseña self-service (`/api/auth/changge-password`) no revoca los refresh tokens, a diferencia del reset administrativo corregido en H-05 (nuevo SA-02).

## Resultado de ejecución

Comando exacto (JDK 21, Windows/PowerShell):

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

Resultado: **BUILD SUCCESSFUL in 19s**, exit 0.

| Métrica | Valor |
|---|---:|
| Suites | 21 |
| Tests totales | 106 |
| Ejecutados | 102 |
| Saltados | 4 |
| Fallos | 0 |
| Errores | 0 |

Los 4 saltados son `ApplicationContextIT` (1) y `AttendanceExportIT` (3), por `@Testcontainers(disabledWithoutDocker = true)` (`AbstractIntegrationTest.java:18`). No hay Docker en esta máquina. No se ejecutó `bootRun` ni se contactó Neon/Gemini.

## Tabla por hallazgo

| H | Veredicto | Evidencia | ¿Test lo protege? | Riesgo residual |
|---|---|---|---|---|
| **H-02** | **CONFIRMADO** | `AttendanceSpecification.java:80-84`: el predicado de sede del no-admin está fuera de `if (dto != null)`; `AttendanceService.java:201-204` exporta con el spec; `AttendanceController.java:79-83` mantiene el cuerpo opcional. | Sí: `AttendanceSpecificationTest` (3 tests; la mutación de mover el predicado dentro de `if (dto != null)` hace fallar `nullFiltersForNonAdminStillRestrictsToTheUsersBranch_H02`). `AttendanceExportIT` solo corre con Docker y está saltado. | La IT real no se ejecutó; el export no tiene límite de filas (H-32). El filtro es por sede del **evento**, de modo que un no-admin ve integrantes de otra sede que asistieron a un evento propio (coherente con H-06). Admin sin DTO sigue viendo todo por diseño. |
| **H-03** | **CONFIRMADO** | `DiscipuladoService.java:137-146` y `:161-170`: valida integrante y maestro antes de mutar; `:184-197` falla cerrado con nulos; `DiscipuladoController.java:93-95` pasa el principal real. | Sí: 4 tests de denegación + 2 de admin en `DiscipuladoServiceTest`; `DiscipuladoControllerTest` verifica el paso del usuario. Mutación de `isOutsideUserBranch` a `false`: 8 tests, 4 fallos. | Oráculo de existencia de maestro por UUID (`404` si no existe vs `409` si existe en otra sede) cuando el integrante sí es de la sede; el DTO no lleva `@Valid` en los endpoints `progress`. |
| **H-04** | **CONFIRMADO** | `UserService.java:76-89` (subconjunto de permisos de roles), `:131-133` (crear), `:252-254` (editar); `RoleService.java:62-70` (subconjunto), `:92` (crear), `:142-144` (editar), `:191` (borrar). Todas las comprobaciones preceden a `save`/`clear`/mapper. | Sí: `nonAdminCannotCreateUserWithAdministratorRole_H04` y los tests de `RoleServiceTest` (crear con ADMIN, añadir ADMIN a un rol existente, editar/borrar un rol superior). Mutación de `validateCanGrantPermissions`: 20 tests, 3 fallos (los tres que pasan por esa comprobación; los de `validateCanManageRole` no se ven afectados). Los tests solo usan `ADMINISTRATOR` como permiso “superior”; no prueban un permiso no-admin ajeno (ver SA-09). | `ADMINISTRATOR` sigue siendo un permiso, no un rol; el modelo de subconjunto es correcto. `GET /api/roles` sigue accesible con `UPDATE_USER` (`RoleController.java:71-81`), lo que permite reconocimiento de roles (H-50). La explotabilidad depende del sembrado externo de `role_permission`. |
| **H-05** | **CONFIRMADO** | `UserService.java:104-113` valida la sede del **nuevo** miembro; `:248-250` lo llama antes de mutar; `:282-285` vacía `refreshTokens` al cambiar la contraseña; `User.java:47-49` tiene `orphanRemoval=true`. | Sí: `nonAdminCannotMoveUserToMemberOfAnotherBranch_H05` y `passwordChangeRevokesRefreshTokens_H05`. Mutación de `validateMemberBranch` + eliminación de `clear()`: 12 tests, 2 fallos. | El test de revocación comprueba la lista en memoria, no el borrado real en PostgreSQL (no hay IT). El cambio self-service no revoca tokens (SA-02). Un no-admin con `UPDATE_USER` puede resetear la contraseña de un par de su misma sede y su mismo nivel (diseño del subconjunto; no es escalado vertical). |
| **H-09** | **CONFIRMADO** | `UserController.java:109-117`: `hasAuthority(T(...).DELETE_USER.name())` (SpEL válido) y `isActive` exterior. | Sí: `PreAuthorizeSpelTest#userDeleteRequiresDeleteUserAuthority_H09` (activo, inactivo, sin permiso) y `EndpointSecurityMatrixTest#deleteUserWithDeleteUserAuthoritySucceeds_H09`. Mutación al SpEL original: 13 tests, 8 fallos (incluye la matriz). | Ninguno funcional. La matriz de endpoint solo cubre este endpoint como representante de 69 (SA-09). |
| **H-10** | **CONFIRMADO** | `MemberService.java:125`, `AttendanceService.java:169`, `UserService.java:193`, `ServiceEventService.java:138`: `dto = dto == null ? new ...() : dto;`; el calendario lanza `InvalidStartAndEndDatesException` (400). | Sí: `NullFilterBodyTest` (6 tests). Mutación de las cuatro normalizaciones: 6 tests, 4 fallos (member, attendance, user, calendar). | `MemberSpecification.java:28-30` sigue devolviendo `cb.conjunction()` sin filtro de sede si recibe `null`; hoy no es alcanzable por `search` (normaliza) y `export` exige cuerpo (`MemberController.java:91`), pero es una trampa latente (SA-08). |
| **H-11** | **PARCIAL** | `GlobalHandlerException.java:29-46` (403 para `AuthorizationDeniedException`, 401 fijo para `InternalAuthenticationServiceException`), `:48-77` (400 para cuerpo ilegible, `BindException`, tipo y `PropertyReferenceException`), `:90-94` (500 genérico sin detalle). | Parcial: `GlobalHandlerExceptionTest` (10 tests) y `EndpointSecurityMatrixTest` cubren los casos corregidos. **No hay test** para `MissingServletRequestParameterException`, `HttpRequestMethodNotSupportedException` ni `HttpMediaTypeNotSupportedException`; una prueba temporal en la copia obtuvo **500** donde el contrato pide 400. | El `@ExceptionHandler(Exception.class)` tiene prioridad sobre los resolutores por defecto de Spring MVC: los parámetros requeridos ausentes (`ServiceController.java:78`, `UserController.java:104`, `MemberController.java:121`, `CategoryRulesController.java:53`, `BaptismController.java:66`) responden 500 (verificado), y los métodos/media types no soportados caen en el mismo catch-all (no se probó con una petición real). El handler de `MethodArgumentNotValidException` usa `.getFieldErrors().getFirst()` sin comprobar que haya errores de campo (`:83-86`), lo que puede producir 500 si solo hay errores globales (SA-10). El cuerpo del 500 no filtra stacktrace; el stacktrace se escribe en stderr/log (`:92,119-125`). El 401 del filtro sí es JSON válido. No se detectó ambigüedad entre `BindException` y `MethodArgumentNotValidException`: el segundo extiende al primero y Spring resuelve el handler más específico; ambos tienen prueba. |
| **H-12** | **CONFIRMADO** | `JwtAuthFilter.java:55-86`: `try` cubre `extractUsername`, `loadUserByUsername` e `isTokenValid`; captura `JwtException | IllegalArgumentException` y `AuthenticatedUserNotFoundException | UsernameNotFoundException`; `:92-107` escribe JSON 401 con mensajes fijos. | Sí: `JwtAuthFilterTest` (7 tests). Revertir el filtro a `e3c0c83`: 7 tests, 5 fallos (firma, malformado, expirado, usuario borrado y firma al validar). Los dos casos válidos siguen verdes. | Errores de base de datos distintos de “usuario no existe” siguen siendo 500; `logout` y `changge-password` no llaman a `isActive` (H-49 preexistente). El 401 no incluye `WWW-Authenticate`, pero no es un requisito del contrato. |
| **H-20** | **CONFIRMADO** | `BranchService.java:65-69` y `:111-115`: `assertCanManageBranch` antes de `branchMapper`, `save`, `existsMemberByBranch`, `existsAttendanceByBranch` y `delete`; helper `:141-146` compara `branch.getId()` con la sede del usuario. | Sí: 7 tests de `BranchServiceTest`. Mutación quitando las dos llamadas: 10 tests, 3 fallos (update ajeno, delete ajeno y delete ajeno con registros); el admin sigue permitido. | Un no-admin distingue `404` de sede inexistente y `409` de sede ajena; el informe ya lo acepta como metadato listable. `create` no se limita (no era el alcance de H-20). |
| **H-21** | **CONFIRMADO** | `UserService.java:94-102` compara las autoridades del objetivo; `:246` (update), `:347` (updateActive) y `:380` (delete) lo aplican antes de mutar/borrar. | Sí: 3 tests de denegación (`nonAdminCannotUpdate/Deactivate/DeleteAdministrator_H21`) y 3 de admin. Mutación de `validateCanManageUser`: 12 tests, 3 fallos. | El orden en `updateActive` y `delete` es privilegios → sede, así que un no-admin puede distinguir 403 (`CannotManageUserWithMorePrivileges`) de 409 (`CannotUpdateUser...`) para un UUID de otra sede y saber si esa cuenta tiene permisos que él no tiene (SA-07). Un no-admin con permiso igual puede desactivar/borrar a un par; es el modelo de subconjunto. |
| **H-22** | **PARCIAL** | `MemberPersistenceService.java:144-149` carga y comprueba la sede **antes** de `memberMapper.updateEntityFromDto` (`:164`); la excepción es `CannotCreateMembersOutsideYourBranch` (409) y no contiene el nombre. | La fuga del **nombre** sí: `MemberPersistenceServiceTest#updateFromAnotherBranchIsRejectedBeforeMapper_H22`; mutación devolviendo la comprobación después del mapper: 10 tests, 4 fallos (3 de H-20 + este). El **oráculo de UUID** no está probado ni cerrado. | `MemberService.java:90-96` hace `findById` y `existsByCc` antes de la comprobación de sede; un no-admin con `UPDATE_MEMBER` obtiene 404 si el UUID no existe y 409 si existe en otra sede, y puede sondear la existencia global de una CC. `MemberService.java:175-180` comprueba asistencias antes de la sede al borrar, y `DiscipuladoService.java:141-146`/`:165-170` hace lo mismo con el maestro. Es enumeración/oráculo, no lectura de PII (SA-01). |
| **H-48** | **CONFIRMADO** | `ServiceController.java:55-61`: paréntesis exteriores `(DELETE_SERVICE || ADMINISTRATOR) && @securityService.isActive(authentication)`. | Sí: `PreAuthorizeSpelTest#serviceDeleteWithDeleteServiceButInactiveUserIsDenied_H48` (DELETE_SERVICE inactivo → false, ADMIN inactivo → false) y `EndpointSecurityMatrixTest#deleteServiceWithDeleteServiceButInactiveAccountIsDenied_H48`. Mutación sin paréntesis: 13 tests, 8 fallos con H-09. | Ninguno funcional. El resto de las 65 expresiones también tiene paréntesis exterior; se verificó con el escaneo de controladores y con `PreAuthorizeSpelTest`. |
| **H-01** | **PARCIAL** | `AiDataSourceConfig.java:33-49` crea `aiJdbcTemplate` sobre `DriverManagerDataSource` con `maxRows=1000` y `queryTimeout=10`; `SqlExecutor.java:13-28` recibe `@Qualifier("aiJdbcTemplate")`; `application.properties:24-28` usa `${AI_DB_URL:${DB_URL}}`, `${AI_DB_USER}` y `${AI_DB_PASS}`. `AiDataSourceConfigTest` y `SqlExecutorTest` protegen límites, datasource y cableado. | Parcial: los límites y el cableado sí (mutar `maxRows`/`queryTimeout` a 0: 5 tests, 1 fallo). **No** hay test del mapeo `AI_DB_USER`/`AI_DB_PASS` ni del fallback `AI_DB_URL → DB_URL`: cambiar esas propiedades en una copia dejó toda la suite verde. **No** hay test de que el rol rechace escrituras ni de que no pueda leer `users`/`refresh_token`. | **Crítico abierto**: `SqlQueryValidator.java:12-16` solo bloquea 11 palabras; `SELECT password_hash FROM users`, `SELECT ... FROM information_schema...`, `SELECT pg_sleep(60)` y `UNION` pasan el validador (`:28-55`). `AiController.java:29` sigue pasando `new User()` y `AiService.java:39,51-53` no usa el usuario ni aplica sede: con el grant que describe el runbook, el SQL del LLM puede leer `users.password_hash`, `refresh_token` y PII de todas las sedes. El runbook concede `GRANT SELECT ON ALL TABLES IN SCHEMA public` (`audit/scripts/neon-ai-readonly.sql:38-40`) y `audit/TODO.md` lo da por aplicado; la pertenencia a `neon_superuser` y los grants reales **no son verificables** sin Neon. `queryTimeout` acota `pg_sleep` a 10 s y `maxRows` a 1000 filas, pero no acotan `string_agg` ni el coste de CPU/IO. El validador tampoco bloquea `INTO` (p. ej. `SELECT ... INTO`), aunque el rol sin `CREATE` en `public` debería frenar la escritura persistente; no hay prueba de ello. El fallback `AI_DB_URL → DB_URL` con credenciales de owner permitiría escritura si el entorno está mal configurado. No se encontró logueo de `AI_DB_PASS` ni de la cadena con credenciales; `AiService.java:46,62` sí loguea el SQL generado y la respuesta final (PII, H-13). |

## Hallazgos nuevos

### SA-01 — El arreglo de H-22 no cierra los oráculos de existencia entre sedes (Media)

- **Ubicación**: `features/member/service/MemberService.java:90-96`, `features/member/service/MemberService.java:175-180`, `features/member/service/MemberPersistenceService.java:144-149`, `features/discipulado/service/DiscipuladoService.java:141-146` y `:165-170`.
- **Evidencia**: `MemberService.update` hace `memberRepository.findById(memberId)` y `memberRepository.existsByCc(...)` antes de delegar en la comprobación de sede; `MemberService.delete` comprueba `attendanceRepository.existsByMember(member)` antes de la sede; `DiscipuladoService` carga el maestro y solo después compara su sede.
- **Impacto**: un usuario con `UPDATE_MEMBER`/`DELETE_MEMBER`/`DISCIPULADO_PROGRESS` puede distinguir un UUID inexistente (404) de uno existente en otra sede (409), y en el caso de `MemberService.update` también puede saber si una CC existe globalmente (409 `AlreadyExistsMemberWithCc`) aunque el integrante sea de otra sede. No se obtiene el nombre ni otros datos, pero se enumera la existencia de integrantes y se confirma actividad.
- **Recomendación**: comprobar la sede inmediatamente después de cargar el recurso y antes de cualquier comprobación de unicidad/registros, o devolver el mismo 404 para “no existe” y “fuera de sede”; añadir tests de los tres oráculos.
- **Test**: no hay test; los tests actuales solo comprueban que el mapper no se invoca.

### SA-02 — El cambio de contraseña self-service no revoca refresh tokens (Media)

- **Ubicación**: `features/auth/service/AuthService.java:123-142`, `features/user/User.java:47-49`, `features/user/service/UserService.java:282-285`.
- **Evidencia**: `ChanggePassword` hace `user.setPasswordHash(passwordEncoder.encode(dto.getPassword()))` y `userRepository.save(user)`, pero no vacía `user.getRefreshTokens()`; el reset administrativo de `UserService.update` sí lo hace.
- **Impacto**: si un refresh token fue robado, la víctima puede cambiar su contraseña y el atacante conserva la sesión hasta 5 días (el refresh token no se invalida). El access token actual sigue vigente hasta 10 minutos, lo que es aceptable, pero el refresh no.
- **Recomendación**: en `changgePassword`, tras persistir el nuevo hash, limpiar/borrar los refresh tokens del usuario dentro de la misma transacción y añadir un test que lo verifique.
- **Test**: no hay test; el test de H-05 solo cubre `UserService.update`.

### SA-03 — H-11 deja en 500 los parámetros requeridos ausentes y otros 4xx de Spring MVC (Media)

- **Ubicación**: `advice/GlobalHandlerException.java:90-94`; parámetros afectados en `features/service/controller/ServiceController.java:78` y `:92`, `features/user/controller/UserController.java:104`, `features/member/controller/MemberController.java:121`, `features/category/controller/CategoryRulesController.java:53` y `features/baptism/controller/BaptismController.java:66`.
- **Evidencia**: no existe `@ExceptionHandler(MissingServletRequestParameterException.class)` ni para `HttpRequestMethodNotSupportedException`/`HttpMediaTypeNotSupportedException`; el `@ExceptionHandler(Exception.class)` captura todo y responde `HttpStatus.INTERNAL_SERVER_ERROR`.
- **Impacto**: un cliente que omite `?onlyActive=`/`?active=`/`?page=` recibe 500 en vez de 400. Verificado en la copia temporal con una prueba `MockMvc` equivalente a la matriz: `Status expected:<400> but was:<500>` para `GET /api/services` sin `onlyActive`. Por el mismo catch-all, un verbo o `Content-Type` no soportado también quedaría en 500 en vez de 405/415 (no se probó con una petición real). No hay fuga de internals en el cuerpo, pero se incumple el contrato de H-11 y se ensucia el canal de errores.
- **Recomendación**: añadir handlers para `MissingServletRequestParameterException`, `MissingServletRequestPartException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException` y `HandlerMethodValidationException`; o extender `ResponseEntityExceptionHandler` en lugar del catch-all. Añadir los casos a `GlobalHandlerExceptionTest` y a la matriz.

### SA-04 — El runbook del rol de IA no demuestra que `peces_ai_readonly` no herede `neon_superuser` (Media; NO VERIFICABLE en Neon)

- **Ubicación**: `audit/scripts/neon-ai-readonly.sql:24-28`, `:43-50`, `:52-58`; `audit/TODO.md:44-48`.
- **Evidencia**: el `REVOKE neon_superuser FROM peces_ai_readonly;` está **comentado** en la sección 2; la verificación consulta `pg_roles` (atributos `rolsuper`, `rolcreatedb`, `rolcreaterole`, `rolbypassrls`) pero no `pg_auth_members` ni los privilegios efectivos de `pg_read_all_data`/`pg_write_all_data`. La sección 3 concede `SELECT` sobre todas las tablas de `public`.
- **Impacto**: si el rol se creó previamente desde la consola/CLI/API y es miembro de `neon_superuser`, puede heredar `pg_write_all_data` y escribir aunque `rolsuper`/`rolbypassrls` aparezcan en `false`; el “rol de solo lectura” no sería tal. No se puede comprobar sin conectarse a Neon.
- **Recomendación**: ejecutar y verificar `REVOKE neon_superuser`, consultar `pg_auth_members`/`information_schema.role_table_grants`, y añadir una prueba de integración que espere `permission denied` en `INSERT/UPDATE/DELETE` y en `SELECT password_hash FROM users`.
- **Test**: no hay prueba de integración del rol; el runbook no es ejecutable por la suite.

### SA-05 — El mapeo de credenciales de IA no está probado y el fallback puede usar credenciales de owner (Media)

- **Ubicación**: `src/main/resources/application.properties:25-27`, `configurations/AiDataSourceConfig.java:35-44`, `src/test/java/.../unit/ai/AiDataSourceConfigTest.java:32-36`.
- **Evidencia**: `ai.datasource.username=${AI_DB_USER}` y `ai.datasource.password=${AI_DB_PASS}`, con `ai.datasource.url=${AI_DB_URL:${DB_URL}}`. El test construye el contexto con `withPropertyValues("ai.datasource.username=test", ...)`, sin cargar `application.properties`; el perfil de test sobrescribe las tres propiedades (`application-test.properties:7-10`). En una copia temporal se sustituyeron `AI_DB_USER`/`AI_DB_PASS` por `DB_USER`/`DB_PASS` y **toda la suite siguió verde**.
- **Impacto**: un error de despliegue (usar el rol owner para `AI_DB_USER`, o dejar el fallback a `DB_URL` con esas credenciales) convierte el endpoint de IA en lectura/escritura total, sin que ningún test lo detecte. El código no comprueba el rol efectivo ni el atributo `default_transaction_read_only`.
- **Recomendación**: probar el mapeo desde `application.properties` (no solo desde propiedades inline), verificar en arranque que `current_user` no es owner/superusuario y que `has_table_privilege(current_user, 'users', 'INSERT')` es falso; documentar que `AI_DB_URL` debe apuntar al mismo clúster con el rol restringido.

### SA-06 — El datasource de IA no tiene timeout de conexión ni pool (Media)

- **Ubicación**: `configurations/AiDataSourceConfig.java:40-49`.
- **Evidencia**: `new DriverManagerDataSource()` solo recibe driver, URL, usuario y contraseña; no se configura `DriverManager.setLoginTimeout`, ni un pool, ni un `connectionTimeout`. `queryTimeout=10` se aplica al `Statement`, no al handshake TCP/TLS con Neon.
- **Impacto**: si Neon no responde o la URL es incorrecta, cada petición `/api/ai/chat` puede bloquear un hilo de Tomcat durante el timeout del sistema operativo; sin rate limiting (H-18), unas pocas peticiones pueden agotar el pool de hilos. No es una escritura, pero es un DoS del endpoint.
- **Recomendación**: usar un `HikariDataSource` interno (sin exponerlo como bean `DataSource`) con `connectionTimeout` corto y pool pequeño, o fijar `DriverManager.setLoginTimeout`; añadir una prueba de fallo rápido con una URL inalcanzable.

### SA-07 — Oráculo de privilegios entre sedes en `updateActive` y `delete` de usuarios (Baja)

- **Ubicación**: `features/user/service/UserService.java:347-351` y `:380-384`.
- **Evidencia**: en `updateActive` y `delete`, `validateCanManageUser(user, target)` se ejecuta antes de comparar la sede; si el objetivo tiene un permiso que el actor no tiene, responde 403 `CannotManageUserWithMorePrivileges`; si no lo tiene, responde 409 `CannotUpdateUserWithMemberOutSideYourBranch`.
- **Impacto**: con un UUID de otra sede, un usuario con `UPDATE_USER` o `DELETE_USER` puede inferir si esa cuenta tiene permisos que él no posee. Es un oráculo de privilegios, no una modificación; `update` ya hace la comprobación de sede primero y no tiene este problema.
- **Recomendación**: unificar el orden (sede antes que privilegios) y devolver el mismo error para “fuera de sede” y “sin privilegios”, o al menos no distinguir por el cuerpo.

### SA-08 — `MemberSpecification` conserva una rama que omite el filtro de sede (Baja, latente)

- **Ubicación**: `utils/specs/MemberSpecification.java:28-30`; `features/member/service/MemberService.java:158`.
- **Evidencia**: `if (dto == null) { return cb.conjunction(); }` se ejecuta antes de la comprobación de sede. `MemberService.search` normaliza el DTO (`:125`), por lo que hoy no se alcanza; `MemberService.export` pasa el DTO crudo (`:158`) y el controlador lo declara requerido (`MemberController.java:91`), así que hoy Spring rechaza el cuerpo nulo. Es una trampa para un futuro llamador o si cambia `required`.
- **Impacto**: si cualquier camino nuevo llama a `build(null, user)`, el no-admin vería todos los integrantes de todas las sedes, reproduciendo H-02/H-10 en integrantes.
- **Recomendación**: eliminar la rama `dto == null` o mover el predicado de sede antes de ella y añadir una prueba que llame a `build(null, ...)` con un no-admin.

### SA-09 — La cobertura de seguridad no valida la autoridad por endpoint (Baja, riesgo de regresión)

- **Ubicación**: `src/test/java/.../web/security/PreAuthorizeSpelTest.java:80-99,145-150`; `src/test/java/.../web/security/EndpointSecurityMatrixTest.java:100-188`; `src/test/resources/security/endpoint-matrix.csv:1-70`.
- **Evidencia**: `PreAuthorizeSpelTest` evalúa las 65 expresiones con un `Authentication` que tiene **todos** los permisos (`authenticationOfAllPermissions`); solo `userDelete` y `serviceDelete` se prueban con permisos concretos. La matriz solo ejecuta 4 de los 69 handlers (`DELETE /api/users/{id}`, `DELETE /api/services/{id}`, `GET /api/services`, `GET /api/health`). `EndpointInventoryTest` compara solo controller/verbo/ruta; `readMatrix` ignora las columnas de autoridad y `expectedWithoutPermission` vale `pending` en las 69 filas.
- **Impacto**: si un endpoint queda con el permiso equivocado (por ejemplo, `VIEW_MEMBER_PANEL` en lugar de `VIEW_USER_PANEL`), o si se añade un endpoint sin `@PreAuthorize`, las pruebas actuales no lo detectan; el superusuario hace pasar todas las expresiones. La matriz no es una matriz de autorización, es un inventario.
- **Recomendación**: parametrizar la matriz con permiso esperado y un usuario que solo tenga ese permiso; probar cada handler con un usuario sin permiso (403) y con el permiso correcto; completar las 69 filas. Añadir un caso de permiso no-admin ajeno (distinto de `ADMINISTRATOR`) a H-04/H-21.

### SA-10 — `MethodArgumentNotValidException` asume que siempre hay errores de campo (Baja)

- **Ubicación**: `advice/GlobalHandlerException.java:79-86`.
- **Evidencia**: `exception.getBindingResult().getFieldErrors().getFirst().getDefaultMessage()` se ejecuta sin comprobar que la lista no esté vacía. `List.getFirst()` lanza `NoSuchElementException` si solo hay errores globales o de objeto.
- **Impacto**: una validación con un constraint de clase (o un futuro DTO con errores globales) produce 500 en el handler de errores en vez de 400. Hoy los DTOs revisados no tienen constraints de clase, por lo que es latente.
- **Recomendación**: usar `getAllErrors()` con `Optional`/fallback y añadir un test con un `BindingResult` sin `FieldError`.

## Regresiones y efectos colaterales

1. **Arranque con nuevas variables obligatorias**: `AI_DB_USER` y `AI_DB_PASS` no tienen default (`application.properties:26-27`); un despliegue o `bootRun` sin ellas falla al resolver placeholders. `AI_DB_URL` es opcional y cae a `DB_URL`. El perfil de test sigue funcionando (`application-test.properties:7-10`) y la suite pasa, pero `ApplicationContextIT` está saltado, así que el arranque completo con el nuevo bean no se verificó en esta máquina.
2. **`JdbcTemplateAutoConfiguration` se retira**: al existir un bean `JdbcOperations` (`aiJdbcTemplate`), Boot no crea el `JdbcTemplate` por defecto. Hoy `SqlExecutor` es el único consumidor (búsqueda en `src/main`), por lo que no hay rotura; un futuro componente que inyecte `JdbcTemplate` por tipo recibirá silenciosamente el template de IA. El informe de H-01 lo advierte.
3. **Contrato 401→403**: las denegaciones por permiso o cuenta inactiva pasan de 401 a 403. Es intencional (H-11/H-12), pero el cliente debe dejar de interpretar todo 403 como sesión expirada. `logout` y `changge-password` no llevan `isActive` (H-49 preexistente).
4. **400 en cuerpos ilegibles**: antes 500; ahora 400. Intencional. La contrapartida es SA-03: los 4xx que no tienen handler siguen siendo 500.
5. **409 en operaciones entre sedes**: H-03, H-20 y H-22 cambian respuestas de 200/500 a 409. Intencional.
6. **Restricciones H-04/H-21**: un no-admin ya no puede gestionar cuentas/roles con permisos que no tiene ni mover usuarios de sede. Es el objetivo; puede romper flujos operativos que dependían de `UPDATE_USER`/`UPDATE_ROLE` con roles superiores.
7. **`@Transactional(readOnly=true)` eliminado de `SqlExecutor`**: correcto para el datasource independiente, pero elimina una defensa en profundidad; la única garantía de lectura es el rol de PostgreSQL. Si el rol está mal configurado, no hay segunda barrera.
8. **Foto en `PUT /members/{id}` ajeno**: `MemberService.update` sube la imagen a Cloudinary antes de que `MemberPersistenceService.update` rechace por sede; el `catch (ApiException)` borra el `publicId`, pero se paga la subida y se depende de que Cloudinary no falle. Ya estaba anotado en `fixes-h20-h22.md`; no hay fuga de datos.
9. **Pruebas**: se eliminó `PecesATierraApplicationTests` (el antiguo `contextLoads`) y se sustituyó por `ApplicationContextIT`, que se salta sin Docker. La suite ya no verifica el arranque completo del contexto en esta máquina.

## Cobertura y pruebas

### Mutaciones ejecutadas (copia fuera del repositorio, restaurada)

La copia temporal se creó en `%TEMP%\opencode\auditor1-mut`, se mutaron los guards y se restauró; al final se comparó SHA-256 de los 283 archivos de `src/main` y eran idénticos al repositorio. La copia se eliminó.

| Mutación | Guard revertido | Resultado |
|---|---|---|
| M1 — H-04 | `validateCanGrantPermissions` con `return` temprano en `UserService` y `RoleService` | **20 tests, 3 fallos**: `nonAdminCannotCreateUserWithAdministratorRole_H04`, `nonAdminCannotCreateRoleWithAdministratorPermission_H04`, `nonAdminCannotAddAdministratorPermissionToExistingRole_H04`. Los tests positivos y de admin siguen verdes. |
| M2 — H-05 | `validateMemberBranch` con `return` temprano y eliminación de `refreshTokens.clear()` | **12 tests, 2 fallos**: `nonAdminCannotMoveUserToMemberOfAnotherBranch_H05`, `passwordChangeRevokesRefreshTokens_H05`. |
| M3 — H-21 | `validateCanManageUser` con `return` temprano | **12 tests, 3 fallos**: `nonAdminCannotUpdateAdministrator_H21`, `nonAdminCannotDeactivateAdministrator_H21`, `nonAdminCannotDeleteAdministrator_H21`. |
| M4 — H-03 | `isOutsideUserBranch` siempre `false` | **8 tests, 4 fallos**: los cuatro tests de denegación de `DiscipuladoServiceTest`. |
| M5 — H-09/H-48 | SpEL de `DELETE_USER` sin `T(...)` y `DELETE /services` sin paréntesis | **13 tests, 8 fallos**: 5 de `EndpointSecurityMatrixTest` y 3 de `PreAuthorizeSpelTest`. |
| M6 — H-20/H-22 | Quitar las dos llamadas a `assertCanManageBranch` y mover la comprobación de H-22 después del mapper | **10 tests, 4 fallos**: 3 de `BranchServiceTest` y `updateFromAnotherBranchIsRejectedBeforeMapper_H22`. |
| M7 — H-02 | Mover el predicado de sede dentro de `if (dto != null)` | **9 tests, 1 fallo**: `nullFiltersForNonAdminStillRestrictsToTheUsersBranch_H02`. El test de `NullFilterBodyTest` de attendance sigue verde porque H-10 normaliza el DTO; el export sigue dependiendo del spec y el test de H-02 lo cubre. |
| M8 — H-10 | Eliminar las cuatro normalizaciones `dto == null` | **6 tests, 4 fallos**: member, attendance, user y calendar de `NullFilterBodyTest`. |
| M9 — H-12 | Restaurar el `JwtAuthFilter` de `e3c0c83` | **7 tests, 5 fallos**: firma, malformado, expirado, usuario borrado y firma al validar. |
| M10 — H-01 | `maxRows=0` y `queryTimeout=0` | **5 tests, 1 fallo**: `aiJdbcTemplateHasDefensiveLimitsAndDedicatedDatasource_H01`. Los tests de delegación no detectan la pérdida de límites porque usan un mock. |
| M11 — H-01 (mapeo) | Cambiar `ai.datasource.username/password` de `AI_DB_USER`/`AI_DB_PASS` a `DB_USER`/`DB_PASS` | **Suite completa verde**: no hay test que proteja el mapeo de credenciales. |
| Sonda — H-11 | Añadir en la copia un test `GET /api/services` sin `onlyActive` esperando 400 | **Falla: `Status expected:<400> but was:<500>`**. Confirma SA-03. |

### Cobertura real

- **69 handlers, 65 `@PreAuthorize`**. Los 4 sin anotación son `POST /api/auth/login` (público), `POST /api/auth/logout`, `POST /api/auth/changge-password` y `GET /api/health` (público); los tres primeros se cubren solo con `authenticated()` en `SecurityConfig`, no con `isActive`.
- **La matriz cubre 4/69** handlers. El CSV tiene 69 filas, pero la columna `authority` no se compara con el código y `expectedWithoutPermission` es `pending` en todas.
- **`PreAuthorizeSpelTest` usa un superusuario con todos los permisos** para las 65 expresiones; solo dos endpoints se prueban con permisos concretos. Un permiso equivocado en los otros 63 no se detecta.
- **Los tests de H-04/H-21 solo usan `ADMINISTRATOR` como permiso superior**. No hay un caso de un rol con, por ejemplo, `DELETE_USER` que el actor no posee; el código lo maneja con `containsAll`, pero la prueba no lo demuestra.
- **Los tests de H-22 solo prueban el nombre**, no el oráculo de UUID/CC/registros.
- **No hay tests** de: H-01 con el rol real (permiso denegado), fallback `AI_DB_URL`, `AI_DB_USER`/`AI_DB_PASS`, `SqlQueryValidator`, autoservicio de contraseña con refresh tokens, parámetros requeridos ausentes, ni del orden sede/privilegios de `updateActive`/`delete`.
- **Integración**: `AttendanceExportIT` es la única prueba real de H-02 contra PostgreSQL y está saltada; `ApplicationContextIT` también. El resto son unitarias con Mockito o `@WebMvcTest` con mocks; no hay Testcontainers ejecutado en este entorno.

## Límites

- **Sin Docker/PostgreSQL real**: 4 IT saltados; no se ejecutó SQL real, no se verificó el borrado por `orphanRemoval` de refresh tokens, ni el comportamiento del predicado de sede contra la base.
- **Sin Neon**: no se verificaron los grants efectivos de `peces_ai_readonly`, su pertenencia a `neon_superuser`, la denegación de `SELECT password_hash`/`INSERT`, ni el endpoint de IA contra Gemini.
- **Sin `bootRun`**: no se verificó el arranque completo con las nuevas variables de entorno ni el comportamiento del `DriverManagerDataSource` contra una URL real.
- **`role_permission` no está en el repositorio**: la explotabilidad de H-04/H-05/H-20/H-21 sigue condicionada al sembrado real; los controles de código son correctos, pero no se puede afirmar qué roles operativos tienen esos permisos.
- **Sin pruebas de concurrencia/TOCTOU**: no se probaron carreras entre la lectura del objetivo y su modificación, ni entre la actualización de un rol y la asignación de ese rol.
- **PWA**: no se reauditó el bundle minificado; H-47 (clave de Google Maps) y H-50 (catálogos/roles listables) siguen fuera de este alcance.
- **Sin SCA/CVE**: no se verificaron dependencias; H-51 sigue pendiente.
- **Secretos**: no se encontró la contraseña indicada en el encargo, un valor de `AI_DB_PASS`, un host de Neon ni un token `npg_...` en el árbol de trabajo ni en el historial (`git grep` y `git log --all -S` sobre la cadena exacta). El único `neondb_owner` es el nombre de rol del runbook; el host no aparece. La contraseña de IA que el `TODO` dice expuesta en el chat debe rotarse; **no está commiteada**.
