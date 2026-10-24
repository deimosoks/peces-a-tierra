<!-- estado-doc -->
> **Actualización post-corrección (2026-10-24).** Después de este informe se corrigieron M-02..M-24. H-11 quedó cerrado en servidor (M-04/M-19); M-03 es parcial (oráculo UUID 404/409); H-01 y M-01 siguen abiertos. Ver `audit/fixes-m02-m24.md` y `audit/TODO.md`.

# Verificación de las auditorías de implementaciones

- **Rama**: `audit/arquitectura-y-seguridad` (`7609417`)
- **Fecha**: 2026-10-24
- **Rol**: auditor 3 (meta-auditoría). No modifiqué código, tests, `build.gradle`, `application.properties` ni los informes existentes.
- **Método**: lectura del código real y de los tests, ejecución independiente de la suite, seis mutaciones y ocho sondas en una copia fuera del repositorio (`C:\Users\josef\AppData\Local\Temp\opencode\auditor3-mut`, restaurada y eliminada), y contraste de cada `archivo:línea` citado por los auditores 1 y 2. Las mutaciones de los auditores que no repetí se citan como reportadas; las mías están identificadas en la sección de mutaciones.
- **Restricciones**: sin Docker, sin Neon, sin credenciales reales, sin Gemini/Cloudinary, sin commits, stash ni cambio de rama.

## Veredicto consolidado

**La rama implementa correctamente 14 de los 17 hallazgos sometidos a revisión. Quedan parciales H-01, H-11 y H-22; no hay ningún hallazgo REFUTADO.**

| Estado | Hallazgos |
|---|---|
| **CONFIRMADO** (14) | H-02, H-03, H-04, H-05, H-07, H-08, H-09, H-10, H-12, H-20, H-21, H-31, H-46, H-48 |
| **PARCIAL** (3) | H-01 (abierto por diseño), H-11 (contrato 4xx incompleto), H-22 (fuga de nombre cerrada, oráculo de UUID abierto) |
| **REFUTADO** (0) | — |

Decisiones clave de la reconciliación:

1. **H-22 es PARCIAL, no CONFIRMADO.** El título original de H-22 en `INFORME-FINAL.md:202` incluye expresamente el «oráculo de UUID en `PUT /members/{id}`», no solo la fuga del nombre. El fix mueve la comprobación de sede antes del mapper (`MemberPersistenceService.java:144-149`), pero `MemberService.java:90-96` sigue haciendo `findById` y `existsByCc` antes de la comprobación de sede: un UUID inexistente da 404 y uno existente en otra sede da 409 genérico. La fuga de nombre está cerrada; el oráculo, no. Auditor 1 acierta; Auditor 2 se queda en la capa de persistencia y pierde el alcance del hallazgo.
2. **H-08 es CONFIRMADO, no PARCIAL.** El contrato de H-08 (`Q-02`) era sustituir `Branch.equals` por `getId()`. El diff de `9ab51f6` demuestra que la comprobación de duplicado ya estaba antes de la de sede: SB-02 no es una regresión del fix, es un defecto preexistente. SB-13 es una debilidad del test, no del código. Auditor 2 confunde «el fix no cubre todo lo que yo auditaría» con «el fix está parcial».
3. **H-11 es PARCIAL, y SA-03/SB-03 son parte de H-11, no un hallazgo nuevo.** La recomendación original de `Q-11` (`borrador-calidad.md:140`) pedía explícitamente un handler para `MissingServletRequestParameterException`. Ese caso sigue devolviendo 500 (reproducido). SA-03 y SB-03 son la misma brecha; se deduplican en M-04. SA-10/SB-04 es un bug distinto, preexistente en el mismo handler, y se conserva como M-19.
4. **H-01 sigue abierto y es el único P0 real.** El datasource dedicado y los límites están bien, pero el rol descrito por el runbook conserva `SELECT` sobre todo `public`, el validador acepta `UNION`, `information_schema`, `pg_sleep` y el bypass `;;`, el controlador sigue pasando `new User()` y no hay filtro por sede. `audit/TODO.md:37-62` lo declara riesgo aceptado; eso no lo convierte en cerrado.

**La suite está verde: 106 tests, 102 ejecutados, 4 saltados, 0 fallos, 21 suites** (`build/test-results/test`, ejecución propia con JDK 21). Los 4 saltados son `ApplicationContextIT` (1) y `AttendanceExportIT` (3), por ausencia de Docker.

**La rama no es apta para fusionar a producción tal cual**: H-01 sigue siendo crítico, el PWA cierra sesión ante cualquier 403 (M-01) y el alta de bautismo filtra el nombre de integrantes de otra sede (M-02). Como rama de auditoría y base de trabajo es apta para seguir; los 14 fixes confirmados están bien implementados y sus pruebas directas los protegen.

## Fiabilidad de cada informe

### Auditor 1 (seguridad y aislamiento)

**Qué confirmo.** Sus 13 veredictos son correctos, incluidos los tres que importan: H-11 PARCIAL, H-22 PARCIAL y H-01 PARCIAL/ABIERTO. La evidencia de H-22 (`MemberService.java:90-96` y `:175-180`), H-11 (`GlobalHandlerException.java:90-94`), H-01 (`SqlExecutor`/`AiDataSourceConfig`/`SqlQueryValidator`) y H-21 (`UserService.java:347-351`, `:380-384`) está bien localizada y abierta línea a línea. Los conteos 69/65, 106/102/4/0 son exactos. La prueba de que `GET /api/services` sin `onlyActive` devuelve 500 es correcta; yo la reproduje con `MockMvc` standalone.

**Qué exagera o matiza mal.**

- **SA-04** está etiquetado «Media» pero es **NO VERIFICABLE**: el `REVOKE neon_superuser` comentado en `audit/scripts/neon-ai-readonly.sql:24-28` solo importa si el rol preexistía creado desde consola/CLI. `audit/TODO.md:44-46` afirma que se creó por SQL; no hay evidencia en el repositorio de lo contrario. Es un riesgo operativo/condicional, no un bug confirmado.
- **SA-05** dice que «el fallback `AI_DB_URL → DB_URL` con credenciales de owner permitiría escritura». El código **nunca** cae a `DB_USER`/`DB_PASS`: `application.properties:26-27` usa siempre `${AI_DB_USER}`/`${AI_DB_PASS}`; solo la URL tiene default anidado (`:25`). El riesgo real es que un operador configure `AI_DB_USER=neondb_owner`, no que el fallback lo haga solo.
- **SA-06**: exagera el «sin timeout de conexión». El `DriverManagerDataSource` no configura un timeout explícito ni un pool, pero el driver PostgreSQL 42.7.8 aplica por defecto `connectTimeout=10 s` (verificado con `javap` sobre el jar). El bloqueo del socket no llega al timeout del sistema operativo; el riesgo real es de concurrencia/agotamiento de hilos, no de bloqueo indefinido.
- **H-22**: su veredicto es correcto, pero el informe no cita el título de H-22 que incluye el oráculo de UUID; la justificación se apoya solo en la recomendación del fix. La conclusión es acertada, la trazabilidad al alcance original es incompleta.

**Qué omite.**

- **SB-01 (PWA 403)**: es la regresión de cliente más importante de la rama y no aparece. El informe dice en «Límites» que no reauditó el bundle, pero el bundle ya estaba auditado en el informe original (`H-17`/`H-50`); la interacción 403→logout era conocida en `borrador-calidad.md:139`.
- **SB-02 (bautismo)**: no detecta que el duplicado se comprueba antes de la sede y filtra el nombre. Es un hallazgo Alto y está en una rama tocada por H-08.
- **SB-10/SB-11/SB-12/SB-13** y la debilidad de `DateTimeUtilsTest`: no evalúa la calidad de la red de pruebas más allá de decir «los tests fallan al revertir».
- **H-08**: no lo audita por reparto de alcance. Eso deja sin contraste el veredicto PARCIAL de Auditor 2 y el alcance real del fix (comparación por id, no la comprobación de duplicado).
- **`SqlQueryValidator` `;;`**: el original S-01 ya señalaba que solo se mira `parts[1]`; el auditor 1 menciona `UNION`, `information_schema`, `pg_sleep` e `INTO`, pero no el bypass `SELECT 1;;SELECT ...` que verifiqué con JShell.
- **Oráculo de `UserService.create`**: `existsByMemberId` y `findByUsername` se ejecutan antes de la comprobación de sede (`UserService.java:118-129`), lo que permite a un no-admin con `CREATE_USER` distinguir si un integrante ajeno ya tiene cuenta. No lo menciona.

**Fiabilidad global: alta en veredictos y evidencia de seguridad; incompleta en cliente y calidad de pruebas.**

### Auditor 2 (calidad, correctitud y pruebas)

**Qué confirmo.** Sus veredictos H-07, H-31, H-46, H-10, H-11, H-12, H-20 y H-01 son correctos. SB-01, SB-02, SB-09, SB-10, SB-11, SB-12 y SB-13 son sólidos; yo reproduje las mutaciones de specs, filtro JWT, handlers y H-08 (M2, M1, M3 y M4 de este informe). La cita del bundle es exacta: `main-2X72WS5U.js` contiene `if(i.status===403)return n.logout(),Cs(()=>i)` y el interceptor está registrado en `[z1,j1]`. La descripción de que el perfil de test apunta la IA a `localhost` es correcta (`application-test.properties:7-10`; `AbstractIntegrationTest.java:23-30` solo sustituye `spring.datasource.*`). Los conteos 106/102/4/0 y las 21 suites son exactos.

**Qué exagera o clasifica mal.**

- **H-08 PARCIAL**: SB-02 es preexistente (el diff de `9ab51f6` solo cambia `equals`→`getId`, `note` y `municipality`), y SB-13 es una debilidad del test. Ninguno de los dos es una regresión del fix. El veredicto correcto es CONFIRMADO.
- **H-22 CONFIRMADO**: la capa de persistencia está bien, pero el hallazgo original incluye el oráculo de UUID (`INFORME-FINAL.md:202`) y `MemberService.java:90-96` lo mantiene. El veredicto correcto es PARCIAL.
- **SB-05 «Media (despliegue)»**: `AI_DB_USER`/`AI_DB_PASS` son obligatorias por diseño y están documentadas en `AGENTS.md:26` y `fixes-h01-datasource.md:136-138`. Un `bootRun` sin variables falla con un placeholder sin resolver; es una nota de despliegue, no un defecto Medio.
- **SB-07 «Media»**: el desajuste del datasource de IA en tests solo afecta a un futuro `SqlExecutorIT`; hoy `ApplicationContextIT` no toca la conexión de IA y el `DriverManagerDataSource` es perezoso. Baja.
- **SB-14**: varias afirmaciones son correctas (`INFORME-FINAL.md:410` dice que no está commiteado y `git ls-files audit/` muestra 19 archivos; `AGENTS.md:11` cita una clase borrada; `AGENTS.md:56` está obsoleto), pero `AGENTS.md:52` no es falso: `aiJdbcTemplate` sigue siendo un `JdbcTemplate`; y «CI con Docker» es una aspiración, no una afirmación de que exista CI. Mezcla errores reales con nitpicks.

**Qué omite.**

- **SA-04**: no menciona el `REVOKE neon_superuser` comentado ni la ausencia de verificación de `pg_auth_members`. Su fila H-01 dice «grants no verificables», pero no concreta el riesgo del runbook.
- **SA-07**: no detecta el oráculo de privilegios de `updateActive`/`delete` (privilegios antes que sede).
- **SA-08**: no detecta la rama `MemberSpecification.java:28-30` que omite la sede con `dto == null`.
- **SA-09**: describe la debilidad de la matriz en la tabla de calidad, pero no la eleva a hallazgo; la matriz sigue cubriendo 4/69 y `PreAuthorizeSpelTest` usa un usuario con todos los permisos (`:145-150`).
- **`SqlQueryValidator` `;;`** y el oráculo de `UserService.create`; tampoco menciona el 409 latente de `UserService.update` con `memberId` nulo.
- **H-23**: el NPE del `catch` de `MemberService.create` (`:80-82`) sigue vivo. Es un hallazgo ya conocido y fuera del alcance de los 16 fixes, pero el informe toca `MemberService` por H-31 y no recuerda que sigue abierto.

**Fiabilidad global: alta en calidad de pruebas y en el hallazgo del PWA; sus dos veredictos H-08/H-22 son incorrectos y sobrevalora tres notas de despliegue.**

## Tabla consolidada por H

| H | Veredicto final | Auditor 1 | Auditor 2 | Evidencia verificada | Riesgo residual |
|---|---|---|---|---|---|
| **H-02** | **CONFIRMADO** | CONFIRMADO | — (no emite veredicto; muta H-02) | `AttendanceSpecification.java:80-84`: el predicado de sede está fuera de `if (dto != null)`; `AttendanceService.java:201-204`; `AttendanceController.java:79-83`. Mutación de meterlo dentro: falla `nullFiltersForNonAdminStillRestrictsToTheUsersBranch_H02`. | Export sin límite de filas (H-32); IT de export saltada sin Docker. |
| **H-03** | **CONFIRMADO** | CONFIRMADO | — | `DiscipuladoService.java:137-146`, `:161-170`, `:184-197`; `DiscipuladoController.java:93-95`. Los cuatro tests de denegación y los dos de admin pasan; mutar `isOutsideUserBranch` a `false` produce 4 fallos. | Oráculo de existencia de maestro por UUID (M-03); endpoints `progress` sin `@Valid` (Q-12). |
| **H-04** | **CONFIRMADO** | CONFIRMADO | — | `UserService.java:76-89`, `:131-133`, `:252-254`; `RoleService.java:62-70`, `:92`, `:142-144`, `:191`. Todas las comprobaciones preceden a `save`/`clear`/mapper. | `ADMINISTRATOR` sigue siendo un permiso, no un rol; tests solo usan ese permiso como «superior» (M-18). |
| **H-05** | **CONFIRMADO** | CONFIRMADO | — | `UserService.java:104-113`, `:248-250`, `:282-285`; `User.java:47-49` con `orphanRemoval=true`. Mutación de `validateMemberBranch` + `clear()`: 2 fallos. | El test de revocación comprueba la lista en memoria, no el `DELETE` real (no hay IT). `UserService.update` con `memberId` nulo lanza 409 para no-admin (M-24, latente). |
| **H-07** | **CONFIRMADO** | — | CONFIRMADO | `BaptismService.java:62` `.note(...)`, `:68` `.municipality(...)`; `BaptismMapper.java:18-21`; test con mapper real. Mutar el builder: 1/8 fallo. | Sin backfill: bautismos antiguos conservan `note=null` y `municipality=city`; `municipality` no está en el response. |
| **H-08** | **CONFIRMADO** | — | **PARCIAL** (incorrecto) | `BaptismService.java:54` y `:87` usan `getId().equals(...)`; el alcance original es solo esa comparación (`INFORME-FINAL.md:188`). Los tests de dos instancias con igual id pasan; `git show 9ab51f6` confirma que el orden duplicado→sede ya existía. | SB-02 (M-02): el duplicado se comprueba antes de la sede y filtra el nombre; SB-13 (M-15): el test no distingue id de nombre. |
| **H-09** | **CONFIRMADO** | CONFIRMADO | — | `UserController.java:109-117`: `hasAuthority(T(...).DELETE_USER.name())` y `isActive` exterior. Tests `PreAuthorizeSpelTest` y matriz; mutación al SpEL original: 8 fallos. | Ninguno funcional; la matriz solo cubre este endpoint como representante (M-18). |
| **H-10** | **CONFIRMADO** | CONFIRMADO | CONFIRMADO | `MemberService.java:125`, `AttendanceService.java:169`, `UserService.java:193`, `ServiceEventService.java:138`; calendario lanza 400. `NullFilterBodyTest` 6 tests; mutación de las cuatro líneas: 4 fallos. | `MemberSpecification.java:28-30` conserva la rama `cb.conjunction()` con `dto == null` (M-17, latente). |
| **H-11** | **PARCIAL** | PARCIAL | PARCIAL | `GlobalHandlerException.java:29-106` cubre 403, 401, cuerpo ilegible, binding, tipo y `orderBy`; pero `:90-94` convierte en 500 `MissingServletRequestParameterException`, método y media type no soportados. Reproducido: `GET /api/services` sin `onlyActive` → **500**; `PUT /api/health` → **500**; `POST /api/services` con `text/plain` → **500**. | SA-03/SB-03 son esta misma brecha (M-04); SA-10/SB-04 es el `getFirst()` sin campos (M-19). El PWA hace logout ante 403 (M-01). |
| **H-12** | **CONFIRMADO** | CONFIRMADO | CONFIRMADO | `JwtAuthFilter.java:55-88` cubre `extractUsername`, `loadUserByUsername`, `isTokenValid`; `:92-107` escribe JSON 401. 7 tests; restaurar el filtro de `e3c0c83`: 5 fallos. | Un `DataAccessException` del `UserDetailsService` sigue siendo 500; el registro del filtro no está probado (M-13). |
| **H-20** | **CONFIRMADO** | CONFIRMADO | CONFIRMADO | `BranchService.java:65-69`, `:111-115`, helper `:141-146`. Se comprueba antes de mutar/consultar registros; 3 tests de denegación + admin. | 404 vs 409 para sede inexistente/ajena (metadato listable, aceptado); `create` no se limita (fuera de alcance). |
| **H-21** | **CONFIRMADO** | CONFIRMADO | — | `UserService.java:94-102`, `:246`, `:347`, `:380`; `containsAll` de autoridades. 3 tests de denegación + 3 de admin; mutación: 3 fallos. | Oráculo de privilegios en `updateActive`/`delete` (M-16); los tests solo usan `ADMINISTRATOR` como permiso superior. |
| **H-22** | **PARCIAL** | **PARCIAL** | **CONFIRMADO** (incorrecto) | `MemberPersistenceService.java:144-149` comprueba sede antes de `beforeUpdate` y del mapper (`:164`); la fuga de nombre está cerrada. Pero `MemberService.java:90-96` hace `findById` y `existsByCc` antes; además, `:175-180` (delete) comprueba asistencias antes de la sede, que es el subconjunto SB-09 de M-03. El título de H-22 incluye el oráculo de UUID (`INFORME-FINAL.md:202`). | M-03: UUID/CC/asistencias/maestro; M-17: `MemberSpecification`; M-23: oráculo de `UserService.create` (misma clase). |
| **H-31** | **CONFIRMADO** | — | CONFIRMADO | `MemberService.java:182-183` captura URL y `publicId`; `:206` `pictureUtils.delete(publicId)`; `@Transactional(rollbackFor = Exception.class)` en `:170`. Test exige el `publicId` y prohíbe la URL. | Registros legacy con URL y sin `publicId` quedan huérfanos; no hay prueba de rollback ni de fallo de Cloudinary. |
| **H-46** | **CONFIRMADO** | — | CONFIRMADO | `RestTemplateConfig.java:15-18`: `SimpleClientHttpRequestFactory`, 5 s connect / 60 s read, `new RestTemplate(requestFactory)`. Test por reflexión; revertir: 1 fallo. | El test depende de campos privados de Spring; no hay prueba de timeout efectivo contra Gemini. |
| **H-48** | **CONFIRMADO** | CONFIRMADO | — | `ServiceController.java:55-61`: paréntesis exteriores `(DELETE_SERVICE || ADMINISTRATOR) && isActive`. Tests de inactivo y admin; mutación sin paréntesis: 8 fallos con H-09. | Ninguno funcional; el resto de las 65 expresiones tiene paréntesis exterior. |
| **H-01** | **PARCIAL / ABIERTO** | PARCIAL | PARCIAL (infra confirmada; global abierto) | `AiDataSourceConfig.java:33-50`; `SqlExecutor.java:15,27`; `application.properties:24-28`; `SqlQueryValidator.java:12-16`, `:47-52`; `AiController.java:29` pasa `new User()`; `AiService.java:39-64` no usa el usuario ni aplica sede. Los límites y el cableado están probados; el rol y sus grants no. | P0: `SELECT` sobre `public` (password_hash, refresh_token, PII), validador evadible (`UNION`, `information_schema`, `pg_sleep`, `;;`), sin filtro por sede; `AI_DB_USER`/`AI_DB_PASS` pueden configurarse con un rol escribible por error (M-08); sin pool ni timeout explícito, aunque el driver aplica 10 s por defecto (M-06). |

## Hallazgos consolidados (SA/SB → M-xx)

| M | Origen | Severidad | Evidencia | ¿Confirmado? | Remedio |
|---|---|---|---|---|---|
| **M-01** | SB-01 | **Alta (funcional)** | `main-2X72WS5U.js` (línea 1, offset ~2288): `if(i.status===403)return n.logout(),Cs(()=>i)`; el interceptor está registrado en `[z1,j1]` (`main-2X72WS5U.js:1`). El servidor devuelve 403 en `GlobalHandlerException.java:102-106`. | **Sí**, estático; no se ejecutó el bundle. | Recompilar el PWA para distinguir 403 de permiso de 403 de sesión (código de error en el cuerpo), o mantener 401 para denegaciones hasta migrar el cliente. |
| **M-02** | SB-02 | **Alta (PII; requiere UUID ajeno y BAPTISM_CREATE)** | `BaptismService.java:49-57`: `existsByBaptizedMemberAndInvalidFalse(member)` y `throw new MemberAlreadyHasRegisteredBaptismActiveException(member.getCompleteName())` antes de la comprobación de sede en `:54`. Sonda: un no-admin de la sede A con un `memberId` de la sede B recibe el nombre «Ana Perez B». | **Sí**, sonda propia. | Mover la comprobación de sede antes del duplicado y no incluir el nombre en el 409; añadir test cruzado con bautismo activo. |
| **M-03** | SA-01 + SB-09 (+ H-22) | **Media** | `MemberService.java:90-96` (`findById`/`existsByCc` antes de sede), `:175-180` (asistencias antes de sede), `:211-216` (`updateActive`); `DiscipuladoService.java:141-146` y `:165-170` (maestro); `BaptismService.java:84-89` (invalidación). La comprobación de sede de H-22 está en la capa de persistencia, no en la de servicio. | **Sí**; sonda de CC: `MemberService.update` de un integrante ajeno con CC existente lanza `AlreadyExistsMemberWithCc` antes del 409 de sede. | Comprobar sede inmediatamente después de cargar el recurso, antes de unicidad/registros; devolver el mismo 404 para «no existe» y «fuera de sede»; tests de los oráculos. |
| **M-04** | SA-03 + SB-03 (parte de H-11) | **Media** | `GlobalHandlerException.java:90-94` captura `Exception` y responde 500; `ServiceController.java:78`, `:92`, `UserController.java:104`, `MemberController.java:121`, `BaptismController.java:66`, `CategoryRulesController.java:53`. Sondas: `GET /api/services` sin `onlyActive` → 500; `PUT /api/health` → 500; `POST /api/services` con `text/plain` → 500. `MvcConfig.java:28-32` deja `NoResourceFoundException` para `/api/...` inexistente, que también cae en el catch-all (deducido; no ejecutado con el contexto completo). | **Sí**, tres sondas MockMvc. | Añadir handlers para `MissingServletRequestParameterException`, `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException`, `MaxUploadSizeExceededException` y `NoResourceFoundException`, o extender `ResponseEntityExceptionHandler`; tests MVC. |
| **M-05** | SA-02 | **Media** | `AuthService.java:123-142`: `changgePassword` hace `setPasswordHash` y `save`, pero no vacía `getRefreshTokens()`; `User.java:47-49` tiene `orphanRemoval=true` y `UserService.java:282-285` sí lo hace en el reset administrativo. Sonda: tras el cambio, el refresh token sigue en la lista. | **Sí**. | Borrar los tokens por repositorio o recargar el usuario gestionado y vaciar la colección en la misma transacción (el principal del filtro está desacoplado y la colección es LAZY); añadir test; el access token actual caduca en 10 min, el refresh en 5 días. |
| **M-06** | SA-06 | **Baja / Media** | `AiDataSourceConfig.java:40-49` usa `DriverManagerDataSource` sin pool y sin timeout explícito; el driver PostgreSQL 42.7.8 aplica por defecto `connectTimeout=10 s` (verificado con `javap`), así que el socket no queda indefinido; el DNS puede no estar cubierto y no hay `loginTimeout`. `JdbcTemplate.setQueryTimeout(10)` (`:48`) se aplica al `Statement`, no a la conexión. | **Parcial**: el riesgo de concurrencia/agotamiento de hilos es real; el «timeout del sistema operativo» del informe es exagerado. | `HikariDataSource` interno (sin exponerlo como bean `DataSource`) con `connectionTimeout` corto y pool pequeño, o `DriverManager.setLoginTimeout`; prueba de fallo rápido. |
| **M-07** | SA-04 | **Media / NO VERIFICABLE** | `audit/scripts/neon-ai-readonly.sql:24-28`: el `ALTER ROLE`/`REVOKE neon_superuser` está comentado; `:43-50` solo consulta `pg_roles`, no `pg_auth_members`; `:38-40` concede `SELECT` sobre todo `public`. `TODO.md:44-48` dice que el rol se creó por SQL, no por consola. | **NO VERIFICABLE** sin Neon. Si el rol preexistía creado por consola/CLI, es un riesgo real de escritura heredada. | Ejecutar `REVOKE neon_superuser` sin condicionarlo; verificar `pg_auth_members` y `information_schema.role_table_grants`; prueba de `permission denied` en `INSERT`/`UPDATE`/`DELETE` y `SELECT password_hash`. |
| **M-08** | SA-05 + SB-06 | **Media (cobertura/config)** | `application.properties:25-27`: URL con `${AI_DB_URL:${DB_URL}}`, pero usuario/contraseña siempre `AI_DB_USER`/`AI_DB_PASS`. `AiDataSourceConfigTest.java:32-36` inyecta valores inline, no carga `application.properties`; `application-test.properties:7-10` los sustituye. Mutación a `DB_USER`/`DB_PASS`: **suite completa verde**. | **Sí**: no hay test que proteja el mapeo ni el fallback; el código no cae a credenciales de owner, solo a la URL. | Test de propiedades reales (contexto con `application.properties` o `ApplicationContextRunner` con `PropertySources`), y comprobación en arranque de `current_user`/`has_table_privilege`; documentar que `AI_DB_USER` nunca debe ser el owner. |
| **M-09** | SB-05 | **Baja (despliegue)** | `application.properties:26-27` sin default; `fixes-h01-datasource.md:136-138` y `AGENTS.md:26` lo documentan; `application-test.properties:8-9` inyecta `test`. | **Sí**, pero es una ruptura de despliegue documentada, no un bug de código. | Añadir un `@ConfigurationProperties`/validación con mensaje accionable, o un default explícito solo en perfiles locales; nota de release. |
| **M-10** | SB-08 | **Baja (trampa)** | `AiDataSourceConfig.java:33-50` expone un `JdbcTemplate`; `JdbcTemplateConfiguration` de Boot 3.5.9 está anotado `@ConditionalOnMissingBean(JdbcOperations.class)` (verificado con `javap` sobre el jar). Hoy `SqlExecutor` es el único consumidor; una inyección futura por tipo recibiría el datasource de IA. | **Sí**, estructural; el fix lo documenta en `fixes-h01-datasource.md:33-37`. | No inyectar `JdbcTemplate` por tipo; declarar un `@Bean("appJdbcTemplate")` explícito para el datasource principal; test de contexto completo. |
| **M-11** | SB-07 | **Baja (infra de tests)** | `application-test.properties:7-10` fija `ai.datasource.url=jdbc:postgresql://localhost:5432/peces_test`; `AbstractIntegrationTest.java:23-30` solo declara `@ServiceConnection` para `spring.datasource.*`. El comentario de `application-test.properties:2` es falso para esas líneas. | **Sí**. | Sustituir la URL de IA con `@DynamicPropertySource` a partir del contenedor, o un `@ServiceConnection` específico; añadir `SqlExecutorIT`. |
| **M-12** | SB-10 | **Media (falsa confianza)** | `NullFilterBodyTest.java:194-197` y `AttendanceSpecificationTest.java:88-92` verifican `cb.equal(...)`, no que el `Predicate` devuelto lo incluya; mutar los cuatro specs a `return cb.conjunction()` deja **9/9 verdes**. | **Sí**, mutación propia. | Devolver un predicado centinela desde `cb.equal` y verificar que `cb.and` lo recibe; o un IT con PostgreSQL. |
| **M-13** | SB-11 | **Media (cobertura de H-12)** | `SecurityConfig.java:74` es la única línea que registra el filtro; `JwtAuthFilterTest` lo construye a mano y `EndpointSecurityMatrixTest` lo mockea. Quitar `.addFilterBefore(...)`: **suite completa 106/102/4/0 verde**. | **Sí**, mutación propia. | Test de la cadena real con un JWT firmado y `MockMvc`, o aserción sobre los filtros de `SecurityFilterChain`; ejecutarlo sin Docker. |
| **M-14** | SB-12 | **Media (cobertura de H-11)** | `GlobalHandlerExceptionTest.java:75-139` invoca los métodos directamente; repuntar las anotaciones de `HttpMessageNotReadable`, `BindException` y `PropertyReference` a otras excepciones deja **10/10 verdes**. | **Sí**, mutación propia. | Probar los 400 a través de `MockMvc`/`@WebMvcTest` con la excepción real; no llamar al handler a mano. |
| **M-15** | SB-13 | **Baja** | `BaptismServiceTest.java:105-106` y `:217-218` crean dos `Branch` con id `branch-a` **y nombre** «Sede A»; mutar `getId` a `getName` deja **8/8 verdes**. Además, `BaptismService.java:89` lanza `CannotCreateMembersOutsideYourBranch` («crear integrantes») al invalidar. | **Sí**, mutación propia. | Usar nombres distintos con el mismo id en los tests; cambiar la excepción de `invalid` por una de bautismo. |
| **M-16** | SA-07 | **Baja** | `UserService.java:347-351` y `:380-384`: `validateCanManageUser` (403) antes de la comprobación de sede (409); un no-admin distingue si una cuenta ajena tiene permisos que él no posee. | **Sí**, por código; no hay test. | Sede antes que privilegios y mismo error para ambos casos; test. |
| **M-17** | SA-08 | **Baja (latente)** | `MemberSpecification.java:28-30`: `if (dto == null) return cb.conjunction();` antes del predicado de sede; `MemberService.java:158` pasa el DTO crudo en `export`, y `MemberController.java:91` lo exige. | **Sí**, latente; no alcanzable por HTTP hoy. | Mover el predicado de sede antes del `if` o eliminar la rama; test con `build(null, userNoAdmin)`. |
| **M-18** | SA-09 | **Baja / Media (riesgo de regresión)** | `PreAuthorizeSpelTest.java:145-150` evalúa las 65 expresiones con un usuario que tiene **todos** los permisos; `EndpointSecurityMatrixTest` cubre 4/69; `EndpointInventoryTest.java:111-116` lee solo 3 columnas y `endpoint-matrix.csv` tiene 69 `pending`. | **Sí**. | Parametrizar la matriz con permiso esperado y usuario que solo tenga ese permiso; completar las 69 filas; probar un permiso no-admin ajeno en H-04/H-21. |
| **M-19** | SA-10 + SB-04 | **Baja / Media (latente)** | `GlobalHandlerException.java:83-86`: `getFieldErrors().getFirst()` sin comprobar que haya `FieldError`; sonda con un `ObjectError` lanza `NoSuchElementException`. No hay constraints de clase hoy (`@AssertTrue`/`@ScriptAssert`: 0 en `src/main`). | **Sí**, sonda propia. | Usar `getAllErrors()` con `Optional`/fallback; test con `BindingResult` sin `FieldError`. |
| **M-20** | SB-14 | **Baja** | `AGENTS.md:11` cita `PecesATierraApplicationTests` (borrada; `Test-Path` false); `AGENTS.md:56` dice «There is only contextLoads» con 106 tests; `INFORME-FINAL.md:410` dice que los artefactos no están commiteados y `git ls-files audit/` lista 19; `INFORME-FINAL.md:6` habla de 243 archivos Java y hoy hay 250 en `src/main/java`; el merge-base real es `f3d07cc`, no `e3c0c83`. `AGENTS.md:52` sí sigue siendo cierto en lo esencial. | **Sí**, salvo el nitpick de `AGENTS.md:52`. | Actualizar `AGENTS.md`, `INFORME-FINAL.md` y el alcance; no mezclar con el código. |
| **M-21** | SB-15 | **Baja (flakiness)** | `DateTimeUtilsTest.java:52-61` compara dos lecturas del reloj real con tolerancia de 1 s; no usa `Clock`. | **Sí**. | Inyectar un `Clock` fijo y verificar el instante, no solo la coherencia entre dos llamadas. |
| **M-22** | **Omitido por ambos** (H-01 residual) | **Alta (dentro de H-01)** | `SqlQueryValidator.java:47-52`: el chequeo de `;` solo mira `parts[1]`; `SELECT 1;;SELECT pg_sleep(60)` devuelve `true` (JShell), igual que `UNION`, `information_schema` y `pg_sleep`. No hay test del validador en `src/test`. | **Sí**, JShell; la ejecución real del segundo statement depende del driver/rol, pero el contrato del validador está roto. | Lista blanca de tablas/vistas y de funciones; rechazar `;`, comentarios, `UNION`, `information_schema`, `pg_catalog`, `pg_*`; test unitario de cada bypass. |
| **M-23** | **Omitido por ambos** | **Media (oráculo)** | `UserService.java:118-129`: `existsByMemberId` y `findByUsername` se ejecutan antes de cargar el integrante y comprobar su sede; un no-admin con `CREATE_USER` distingue si un integrante ajeno ya tiene cuenta (409) de uno que no (409 de sede) y de un UUID inexistente (404). | **Sí**, por código; no hay test. | Cargar el integrante y comprobar sede antes de las unicidades; mismo error para los casos cruzados. |
| **M-24** | **Omitido por ambos** | **Baja (latente)** | `UserService.java:229-232` deja `member = null` si `memberId` es nulo; `:250` llama a `validateMemberBranch(givenBy, member)` y `:109-111` lanza 409 para no-admin. El `@NotBlank` de `UserRequestDto.java:17` y el `@Valid` de `UserController.java:88` lo impiden por HTTP, pero el método de servicio acepta el caso. | **Sí**, latente; no alcanzable por el contrato actual. | Solo validar la sede del nuevo miembro si `memberId != null` (o exigir `memberId` también en el servicio); test. |

**Nota sobre H-23:** el NPE del `catch` de `MemberService.java:80-82` (Q-03/H-23) sigue presente. No es una regresión de H-31 ni de H-22 y ya figura como pendiente en `INFORME-FINAL.md:203`; los dos informes de implementaciones lo omiten, pero no lo agrego como M nuevo para no duplicar un hallazgo conocido y fuera de alcance.

## Discrepancias resueltas

### 1. H-22: «fuga de nombre cerrada» no equivale a «H-22 cerrado»

El alcance original de H-22 no es solo el mapper. `INFORME-FINAL.md:202` lo titula «Fuga del nombre de un integrante de otra sede **y oráculo de UUID** en `PUT /members/{id}`». El fix de `3dc9dfb` cierra exactamente la fuga del nombre: la comprobación de sede está en `MemberPersistenceService.java:147` y el mapper se llama en `:164`. Pero la capa de servicio sigue cargando el integrante (`MemberService.java:90`) y comprobando la CC (`:93`) antes de delegar; el 404 vs 409 sigue vivo. Por tanto:

- H-22 = **PARCIAL**.
- El oráculo de UUID es **parte de H-22** (no un SA nuevo).
- El oráculo de CC, asistencias y maestro es la **extensión** SA-01; SB-09 es el subconjunto de asistencias. Se consolidan en M-03.
- El oráculo de `UserService.create` (M-23) es de la misma familia, pero en otro servicio; no estaba en H-22.

### 2. H-08: el orden duplicado→sede es preexistente, no una regresión

`git show 9ab51f6 -- BaptismService.java` muestra que el commit de H-07/H-08 solo cambió:

- `member.getBranch().equals(...)` → `member.getBranch().getId().equals(...)` (línea 54),
- `baptism.getBaptizedMember().getBranch().equals(...)` → `getId().equals(...)` (línea 87),
- `.neighborhood(getNote())` → `.note(getNote())`,
- `.municipality(getCity())` → `.municipality(getMunicipality())`.

La comprobación de duplicado ya estaba antes de la de sede; no la introdujo el fix. SB-02 es un hallazgo **Alto preexistente**, no una regresión de H-08. SB-13 es una debilidad del test (los dos fixtures se llaman «Sede A»), no del código. Por tanto, H-08 = **CONFIRMADO** y SB-02/SB-13 se reclasifican como M-02/M-15. Auditor 2 acierta al detectarlos, pero se equivoca al convertirlos en «H-08 parcial».

### 3. H-11: SA-03/SB-03 son H-11, no un hallazgo nuevo

`borrador-calidad.md:140` (recomendación de Q-11) pedía «añadir handlers para `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` y `MissingServletRequestParameterException` con 400». El fix añadió los dos primeros y `BindException`/`PropertyReference`, pero no el tercero. `GlobalHandlerException.java:90-94` sigue capturando `MissingServletRequestParameterException` con 500. Reproducido con tres sondas. Por tanto:

- H-11 = **PARCIAL**.
- SA-03 y SB-03 son **la misma brecha** y forman parte de H-11; se deduplican en M-04.
- SA-10/SB-04 (`getFirst()` sin comprobar `FieldError`) es un bug **preexistente y distinto**: el diff de `b9c3b83` no toca esa línea y Q-11 no lo pedía; se conserva como M-19.
- El 500 de `NoResourceFoundException` y `MaxUploadSizeExceededException` es del mismo catch-all, pero no se ejecutó con el contexto completo; queda como «deducido» dentro de M-04.

### 4. H-01: el código no usa credenciales de owner por el fallback

`application.properties:25` es `${AI_DB_URL:${DB_URL}}`; `:26-27` son `${AI_DB_USER}`/`${AI_DB_PASS}`. El fallback solo afecta a la URL; las credenciales son siempre las de IA. La afirmación de SA-05 «el fallback puede usar credenciales de owner» es incorrecta como descripción del código: solo ocurre si un operador configura `AI_DB_USER` con el rol owner. El riesgo real es de configuración y de ausencia de verificación en arranque, no un fallback automático. SA-04 es **NO VERIFICABLE** (rol/neon_superuser en Neon), no un bug confirmado. SA-06 sí tiene un riesgo de diseño real (sin pool ni timeout explícito), aunque el driver aplica 10 s por defecto y el «timeout del SO» del informe es exagerado.

### 5. SB-01: la omisión más grave del Auditor 1

El bundle servido es `main-2X72WS5U.js` (`index.html` lo referencia). En la línea 1, offset ~2288, está `if(i.status===403)return n.logout(),Cs(()=>i)`; el interceptor se registra en `[z1,j1]` (`main-2X72WS5U.js:1`). El servidor cambió las denegaciones de permiso/inactividad a 403 (`GlobalHandlerException.java:102-106`). Cualquier 403 de un endpoint autenticado dispara `logout()` en el cliente. Es una regresión funcional real; Auditor 2 la detecta y Auditor 1 la omite. No es una fuga, pero rompe flujos legítimos y no se arregla solo con el servidor.

### 6. Severidad de SB-05/SB-07/SB-08

- **SB-05**: `AI_DB_USER`/`AI_DB_PASS` obligatorias es una decisión de despliegue documentada (`AGENTS.md:26`, `fixes-h01-datasource.md:136-138`). El fallo al arrancar es claro (`Could not resolve placeholder`), no silencioso. Baja, no Media.
- **SB-07**: el datasource de IA del perfil de test apunta a `localhost`; hoy no hay ningún IT de IA y `ApplicationContextIT` no lo toca. Afecta a un futuro `SqlExecutorIT`. Baja.
- **SB-08**: la interacción con `JdbcTemplateAutoConfiguration` es una trampa real, pero está documentada en el fix y hoy no hay consumidor por tipo. Baja, no Media.

## Mutaciones de comprobación

Todas en `C:\Users\josef\AppData\Local\Temp\opencode\auditor3-mut` (copia sin `.git`, `build`, `.gradle` ni `bin`). Se ejecutó una línea base verde (106/102/4/0) antes de mutar. Al terminar se restauraron los archivos desde el repositorio y se eliminó la copia; el árbol de trabajo no se tocó.

| # | Mutación | Comando | Resultado | Conclusión |
|---|---|---|---|---|
| **M1** | Quitar `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)` de `SecurityConfig.java:74` | `cleanTest test` (suite completa) | **106 tests, 102 ejecutados, 4 saltados, 0 fallos: verde** | SB-11 confirmado: la suite no detecta que se desactive la autenticación JWT. |
| **M2** | En `MemberSpecification`, `AttendanceSpecification`, `UserSpecification` y `ServiceEventSpecification`, devolver `cb.conjunction()` en vez de `cb.and(predicates...)` | `--tests NullFilterBodyTest --tests AttendanceSpecificationTest` | **9 tests, 0 fallos: verde** | SB-10 confirmado: los tests verifican que se llama a `cb.equal`, no que el predicado se aplique. |
| **M3** | Repuntar `@ExceptionHandler` de `HttpMessageNotReadableException`, `BindException` y `PropertyReferenceException` a `IllegalStateException`, `UnsupportedOperationException` y `ArithmeticException` | `--tests GlobalHandlerExceptionTest` | **10 tests, 0 fallos: verde** | SB-12 confirmado: los handlers de 400 se prueban sin sus anotaciones MVC. |
| **M4** | En `BaptismService.java:54,87`, cambiar `getId().equals(...)` por `getName().equals(...)` | `--tests BaptismServiceTest` | **8 tests, 0 fallos: verde** | SB-13 confirmado: los fixtures usan el mismo nombre; el test no distingue id de nombre. |
| **M5** | En `application.properties:26-27`, cambiar `AI_DB_USER`/`AI_DB_PASS` por `DB_USER`/`DB_PASS` | `cleanTest test` (suite completa) | **106 tests, 102 ejecutados, 4 saltados, 0 fallos: verde** | SA-05/SB-06 confirmados: no hay test del mapeo de credenciales ni del fallback. |
| **M6** | Quitar `user.getRefreshTokens().clear()` de `UserService.java:284` | `--tests UserServiceTest` | **12 tests, 1 fallo:** `passwordChangeRevokesRefreshTokens_H05` | Control positivo: el test de H-05 sí detecta la pérdida de la revocación administrativa. |

Sondas adicionales (tests temporales, no forman parte del repo). Las sondas MVC son `standaloneSetup`, sin la cadena de seguridad: en la aplicación real hace falta un usuario autenticado para llegar al handler, pero el 500 del `@ExceptionHandler(Exception.class)` es el mismo.

| Sonda | Resultado | Hallazgo |
|---|---|---|
| `GET /api/services` sin `onlyActive`, `MockMvc` standalone con `GlobalHandlerException` | **500** (esperado 400) | SA-03/SB-03 |
| `PUT /api/health` | **500** (esperado 405) | SA-03/SB-03 |
| `POST /api/services` con `Content-Type: text/plain` | **500** (esperado 415) | SA-03/SB-03 |
| `MethodArgumentNotValidException` con un `ObjectError` y sin `FieldError` | `NoSuchElementException` | SA-10/SB-04 |
| `BaptismService.create` con integrante ajeno y bautismo activo | mensaje con «Ana Perez B» | SB-02 |
| `MemberService.update` con integrante ajeno y CC existente | `AlreadyExistsMemberWithCc` antes del 409 de sede | SA-01 (CC) |
| `AuthService.changgePassword` con un refresh token | el token sigue en la lista | SA-02 |
| `SqlQueryValidator.isSafeSelect("SELECT 1;;SELECT pg_sleep(60)")` (JShell) | `true`; también `UNION`, `information_schema`, `pg_sleep` | M-22 |

## Riesgos residuales priorizados (P0/P1/P2)

### P0 — bloquea producción

- **H-01 / M-22**: `/api/ai/chat` puede leer `users.password_hash`, `refresh_token` y PII de todas las sedes con el rol descrito por el runbook; el validador es evadible y no hay filtro por sede. `audit/TODO.md:37-62` lo acepta temporalmente, pero mientras siga abierto no debe desplegarse el endpoint sin `ai_read`/revocaciones o sin deshabilitarlo. Verificar además **M-07** (`neon_superuser`): si el rol lo hereda, el P0 es aún mayor.

### P1 — corregir antes de fusionar

- **M-01**: el PWA hace `logout()` ante cualquier 403. Recompilar el cliente o mantener 401 hasta la migración.
- **M-02**: el alta de bautismo filtra el nombre de un integrante de otra sede. Mover la comprobación de sede antes del duplicado.
- **M-05**: el cambio de contraseña self-service no revoca refresh tokens. Limpiar la colección en la misma transacción.
- **M-04**: los 500 de parámetros/método/media type/no resource incumplen H-11. Añadir handlers y pruebas MVC.
- **M-07** (condicional): verificar `pg_auth_members`; si hay membresía en `neon_superuser`, tratarlo como P0.

### P2 — deuda y cobertura

- **M-03** (oráculos de existencia), **M-06** (pool/timeout explícito de IA), **M-08/M-09/M-10/M-11** (configuración y tests de IA), **M-12/M-13/M-14** (falsas confianzas en specs, filtro JWT y handlers), **M-15/M-16/M-17/M-18/M-19** (tests y robustez), **M-20/M-21** (documentación y reloj), **M-23/M-24** (oráculo de `UserService.create` y 409 latente).

## Límites

- **Sin Docker**: los 4 IT (`ApplicationContextIT`, `AttendanceExportIT`) se saltan; no se ejecutó PostgreSQL real, `unaccent`, el esquema `create-drop`, el borrado por `orphanRemoval` ni el filtro de sede contra la base. Todo lo que depende de JPA/Testcontainers es **NO VERIFICABLE** en esta máquina.
- **Sin Neon**: no se verificaron la existencia de `peces_ai_readonly`, su pertenencia a `neon_superuser`, sus grants efectivos, ni el comportamiento del `DriverManagerDataSource` contra una URL real. No se usaron credenciales reales.
- **Sin PWA en ejecución**: M-01 es evidencia estática del bundle minificado; no se abrió un navegador ni se reprodujo el logout.
- **Sin Gemini/Cloudinary**: no se llamaron APIs reales; H-46 se verificó por reflexión sobre la factory, H-31 con mocks y M-06 por código.
- **Sin `bootRun` completo**: el arranque con las nuevas variables y el contexto completo no se ejecutó; `ApplicationContextIT` está saltado. Las pruebas de MVC fueron standalone, no con el contexto de producción.
- **`role_permission` no está en el repositorio**: la explotabilidad de H-04/H-05/H-20/H-21 depende del sembrado externo. Los controles de código son correctos, pero no se puede afirmar qué roles operativos tienen esos permisos.
- **Sin concurrencia/TOCTOU, sin SCA/CVE, sin JaCoCo/CI**: no se midió cobertura de línea ni se probaron carreras entre lectura y mutación; H-51 y la cobertura real siguen pendientes.
- **Secretos**: no encontré secretos reales en el árbol de trabajo ni los imprimí; el runbook usa placeholders. La contraseña de IA que el `TODO` dice expuesta en el chat debe rotarse. No repetí la búsqueda en todo el historial de Git; la afirmación de que no está commiteada es del Auditor 1 y no la doy por verificada aquí.
