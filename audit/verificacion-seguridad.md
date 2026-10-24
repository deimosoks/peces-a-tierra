<!-- estado-doc -->
> **Documento histórico (2026-10-24).** Refleja el estado en el momento de su creación; varios hallazgos ya fueron corregidos. El estado vigente está en `audit/INFORME-FINAL.md`, `audit/TODO.md` y `audit/fixes-m02-m24.md`.

# Verificación — eje de seguridad

Verificación estática (sin `bootRun`, sin base de datos, sin runtime) del `audit/borrador-seguridad.md`.
Se abrieron todas las ubicaciones citadas, se siguió el flujo controller → service → repository/specification
y se contrastaron las mitigaciones de Spring Security, Spring MVC, Hibernate y jjwt contra los jars que resuelve
el build (Boot 3.5.9, Spring 6.2.15, Security 6.5.7, Hibernate 6.6.39, jjwt 0.12.5).

> **Actualización 2026-10-24**: S-06 (H-06) queda **descartado por regla de negocio**: un integrante
> de una sede puede asistir a un evento de otra sede, así que la ausencia de validación de sede es
> intencional. Ver `audit/00-aclaraciones-negocio.md`.

## Veredicto global

El borrador es fiable en lo esencial: **20 de 22 hallazgos quedan CONFIRMADOS, 2 PARCIALES y 0 REFUTADOS**.
Las 22 ubicaciones existen, el código citado coincide y no hay duplicados ni rutas inventadas. No obstante, hay
dos afirmaciones que no se sostienen tal como están escritas (S-13, clave de Gemini en logs; S-22, CVEs concretas),
una severidad inflada (S-17), varios impactos imprecisos (S-06, S-18, S-04/S-05 como "Altas" sin marcar bien su
carácter latente) y dos "Descartados" incorrectos o incompletos (secretos hardcodeados y borrado de la foto).

De los tres riesgos principales: **S-02 es el más determinista y explotable hoy**; **S-01 es real y de impacto
total, pero es un SQLi mediado por LLM** (requiere `CHAT_AI` y que el modelo obedezca la inyección de prompt; no
existe un campo SQL directo en el DTO); **S-04 es real en código pero latente**, porque depende de qué roles
tengan `CREATE_USER`/`UPDATE_USER`/`CREATE_ROLE`/`UPDATE_ROLE` en `role_permission`, tabla que no está en el repo.

Se añaden cinco vectores que el borrador no cubre: clave de Google Maps en el bundle, fuga del nombre de un
integrante de otra sede en `PUT /api/members/{id}`, desactivación/degradación de un administrador por `UPDATE_USER`,
mensaje interno de autenticación devuelto en el login público y foto del integrante que no se borra de Cloudinary.
El informe es **apto para el consolidado con las correcciones de la sección de consistencia**; no exige rehacer el eje.

## Tabla de verificación

| ID | Veredicto | Severidad (orig → propuesta) | Esfuerzo | Explotable hoy / condicionado / teórico | Nota |
|---|---|---|---|---|---|
| S-01 | CONFIRMADO | Crítica → Crítica | L | Condicionado: usuario con `CHAT_AI` + obediencia del LLM | `AiController.java:29` pasa `new User()`; el validador solo exige prefijo `SELECT`/`WITH` y bloquea 11 palabras; `SqlExecutor` ejecuta sin `maxRows` ni timeout y el resultado se reenvía a Gemini. `;`, `pg_*`, `dblink`, `WITH`, `UNION` e `information_schema` no están bloqueados. `readOnly` frena DML/DDL, no lectura ni DoS. |
| S-02 | CONFIRMADO | Alta → Alta | S | Explotable hoy: `MANAGE_ATTENDANCE` o `MANAGE_DASHBOARD` | `@RequestBody(required=false)` + `AttendanceSpecification.java:37` meten el filtro de sede dentro de `if (dto != null)`; sin cuerpo, `findAll(spec)` devuelve todas las sedes y el mapper incluye nombre, nota, sede y autor. `search` NPEa en `dto.getOrderBy()`; `export` no. |
| S-03 | CONFIRMADO | Alta → Alta | S | Explotable hoy: `DISCIPULADO_PROGRESS` | `progress` pasa `null` y ambos métodos ignoran `user`; se puede completar un progreso ajeno, fijar fecha y asignar un maestro de otra sede. El `create`/`findById`/`search`/`delete` del mismo servicio sí comparan `getId()`. |
| S-04 | CONFIRMADO | Alta → Alta (latente) | M | Condicionado a `role_permission` | No hay lista blanca de roles ni de permisos; `RoleMapper` y `UserService` aceptan `ADMINISTRATOR`. `PermissionRequestDto.@NotBlank` sobre un enum no se valida porque el `Set` no lleva `@Valid`, así que el alta de rol funciona. Si esos permisos son solo de ADMIN, baja a Media/Baja. |
| S-05 | CONFIRMADO | Alta → Alta (latente) | S | Condicionado a `role_permission` | La comprobación de sede usa la sede actual del target, no la del `memberId` nuevo; después reasigna `member` sin comprobar la sede. Resetea la contraseña sin la actual y el bloqueo de autoedición se evita con una segunda cuenta. No revoca refresh tokens. |
| S-06 | **DESCARTADO** (regla de negocio, 2026-10-24) | Alta → N/A | — | No aplica: la asistencia inter-sede está permitida | Se conserva la evidencia histórica. El defecto independiente del lote (todos los registros al primer `serviceEventId`) sigue en H-41. Ver `00-aclaraciones-negocio.md`. |
| S-07 | CONFIRMADO | Media → Media | M | Explotable hoy: roles operativos, misma sede | `MemberService.search` usa `toDto(member, true)` y devuelve cc, celular, dirección, nacimiento y notas; la specification sí limita a la sede. Es mínimo privilegio intra-sede, no una fuga entre sedes. |
| S-08 | CONFIRMADO | Media → Media (Baja si se confirma que no hay cookies) | S | Condicionado a token robado/XSS | `allowedOriginPatterns("*")` + `allowCredentials(true)` refleja cualquier origen. Con Bearer en `localStorage` no es explotable por sí solo; es defensa en profundidad y sube si se migra a cookies. |
| S-09 | CONFIRMADO | Media → Media | M | Condicionado a XSS o dependencia comprometida | `chunk-CJDLSKNK.js:5`: `storeTokens` guarda access y refresh en `localStorage`; el refresh dura 5 días. No hay CSP en `SecurityConfig` ni en `index.html`. |
| S-10 | CONFIRMADO | Media → Media | M | Requiere leer `refresh_token` o robar el token | Token en claro, 5 días, sin revocación al cambiar contraseña; access sin `jti`/`iss`/`aud`; secreto no validado al arrancar. La rotación (`deleteIfValid`) sí es correcta. |
| S-11 | CONFIRMADO | Media → Media | M | Explotable hoy: fuerza bruta | No hay filtro, contador ni dependencia de rate limiting; login público, sin límite en `/api/ai/chat` ni en exportaciones. BCrypt ralentiza, no bloquea. |
| S-12 | CONFIRMADO | Media → Media | S | Explotable hoy: enumeración sin autenticación | Usuario inexistente → `ApiException` envuelta en `InternalAuthenticationServiceException` → "No autorizado."; contraseña mala → "Credenciales invalidas.". El camino inexistente no ejecuta BCrypt (diferencia de tiempos real). |
| S-13 | PARCIAL | Media → Media | M | PII: acceso a logs; clave: no | PII y notas en logs INFO: confirmado. La clave de Gemini en `ResourceAccessException`: **refutada**; `RestTemplate` 6.2.15 elimina la query del mensaje (`substring(0, indexOf('?'))`). La clave sigue viajando en la URL a Google, pero no por ese log. |
| S-14 | CONFIRMADO | Media → Media | M | Ocurre en cada consulta con `CHAT_AI` | `buildFormattingUserPrompt` envía `data.toString()` completo a Gemini, sin minimización; el SQL puede seleccionar PII de todas las sedes y el historial del usuario también se envía. |
| S-15 | CONFIRMADO | Media → Media | S | No explotable: falla cerrado | `hasAuthority(org...AppPermission).DELETE_USER.name()`: `hasAuthority` cierra antes de `.DELETE_USER`; SpEL intenta leer `org` sobre el root y lanza `SpelEvaluationException`; 500 para todos, sin bypass. |
| S-16 | CONFIRMADO | Media → Media (latente) | S | Condicionado a `BRANCH_UPDATE`/`BRANCH_DELETE` no-admin | `update`/`delete` no comparan la sede del usuario; `delete` solo bloquea si hay integrantes o asistencias. Si esos permisos son solo de ADMIN, baja a Baja. |
| S-17 | CONFIRMADO | Media → **Baja** | S | No explotable: falla cerrado | `Branch` no tiene `equals`/`hashCode`; el principal y el integrante viven en contextos distintos, así que `equals` es siempre false para no-admin y deniega creación/invalidación. Es un bug funcional, no una fuga. |
| S-18 | CONFIRMADO | Baja → Baja | S | Explotable con `DELETE_SERVICE` y token vivo | `A` OR `B` AND `C` equivale a `A` OR (`B` AND `C`): cualquier tenedor de `DELETE_SERVICE` omite `isActive`; un ADMINISTRATOR sin `DELETE_SERVICE` no. La ventana del access token es de hasta 10 minutos. |
| S-19 | CONFIRMADO | Baja → Baja | S | Explotable con token vivo de un desactivado | `logout` y `changge-password` solo están cubiertos por `.authenticated()`; un desactivado puede cambiar su contraseña (aunque no podrá loguearse) y cerrar sesión. Impacto acotado. |
| S-20 | CONFIRMADO | Baja → Baja | S | Explotable hoy con token manipulado | `SignatureException`, `UnsupportedJwtException` e `IllegalArgumentException` no se capturan; en jjwt 0.12.5 `security.SignatureException` → `SignatureException` → `security.SecurityException` → `JwtException`, así que propaga y devuelve 500. Un usuario borrado también produce 500 en el filtro. |
| S-21 | CONFIRMADO | Baja → Baja | S | Explotable hoy: reconocimiento | `GET` de tipos y categorías solo exige `isActive`; `GET /api/roles` admite `UPDATE_USER`, lo que permite enumerar roles/permisos antes de S-04. La recomendación de exigir `VIEW_MEMBER_PANEL` puede romper formularios operativos. |
| S-22 | PARCIAL | Baja → Baja | M | Deuda / teórico | Versiones y ausencia de SCA confirmadas (`build.gradle:3,39,42-44,48`); las CVE concretas no son verificables sin un SCA ni base de datos de vulnerabilidades, como el propio informe reconoce en sus límites. |

## Refutaciones y matices

### S-13 — PARCIAL: la PII está confirmada, la clave de Gemini en logs no

- **Confirmado**: `MemberPersistenceService.java:112-136` registra nombre, CC, celular, ciudad y barrio;
  `MemberNoteService.java:49-64` registra el contenido literal de la nota; `AttendanceService.java:119-139`
  registra el nombre del integrante; `AiService.java:46` y `:62` registran el SQL generado y la respuesta final.
  `logback.xml:11-13` deja el root en INFO a consola. Todo eso es real.
- **Refutado**: la afirmación de que un fallo de red puede volcar `?key=<GEMINI_API_KEY>` al log. En Spring 6.2.15,
  `RestTemplate.createResourceAccessException` construye el mensaje con la URL **sin query**:
  `url.toString()`, y si `url.getRawQuery() != null` hace `resource.substring(0, resource.indexOf('?'))`
  (bytecode de `spring-web-6.2.15.jar`, `RestTemplate.createResourceAccessException`, líneas 923-926 de la fuente).
  `AiService.java:308-310` hace `log.error("Error calling Gemini API", e)`, pero el mensaje de la excepción ya no
  contiene la clave. El `IOException` estándar (timeout, conexión rechazada, host desconocido) tampoco incluye la
  query; no se probaron clientes HTTP alternativos. La clave sigue en la URL que viaja a Google (y podría aparecer
  en un proxy/access log externo), pero no en este log de la aplicación por el camino citado.
- **Consecuencia**: el hallazgo sigue siendo Media por la PII y las notas, pero hay que eliminar la evidencia de la
  clave o rebajarla a "riesgo de proxy/access log, no del código". La recomendación de usar `x-goog-api-key` sigue
  siendo buena práctica, no una mitigación de este log.

### S-22 — PARCIAL: versiones confirmadas, CVE no verificables

- **Confirmado**: `build.gradle:3` declara Boot 3.5.9; `:39` Tika 3.2.2; `:42-44` jjwt 0.12.5; `:48`
  `cloudinary-http44:1.32.0`. No hay OWASP Dependency-Check, Renovate ni SCA en el build. La deriva de versiones
  y la ausencia de escaneo son reales.
- **No verificable**: las CVE concretas (CVE-2025-66516/54988, CVE-2024-31033, CVE-2026-41843/41852/59283,
  CVE-2026-40972/40973/40992/41001) y la afirmación de que ninguna es aplicable requieren una base de datos de
  vulnerabilidades y el árbol transitivo resuelto. El borrador lo reconoce en "Límites"; no se puede elevar a
  "sin CVEs aplicables" como conclusión firme. El veredicto correcto es deuda/incertidumbre, no un *clean bill*.

### Matices en hallazgos confirmados

- **S-01 (matiz de explotación)**: el validador es evitable, pero no hay un parámetro SQL directo: el atacante
  controla `message`/`history` y necesita que Gemini emita el `SELECT`. El bypass `SELECT 1;;SELECT pg_sleep(60)`
  supera `isValid` porque `parts[1]` queda vacío (`SqlQueryValidator.java:47-52`), pero la ejecución del segundo
  statement depende del modo de pgjdbc; con el modo extendido (por defecto) PostgreSQL rechaza varios comandos en un
  `Parse`. El vector fiable es un **único** `SELECT` con `pg_sleep`, `information_schema`, `current_user`,
  `pg_read_file` o `dblink`. `pg_read_file`/`dblink`/`COPY ... TO PROGRAM` dependen de privilegios del rol de BD y
  de extensiones; `pg_sleep` es ejecutable por PUBLIC y no necesita privilegios. El resultado no se devuelve crudo:
  se reenvía a Gemini y vuelve como `answer`/`chartData`, pero el prompt de formateo pide nombres y fechas
  (`AiService.java:246`), así que la exfiltración es viable (y `string_agg` permite meter todo en una celda).
- **S-01 (`readOnly`)**: `HibernateJpaDialect.beginTransaction` llama a `DataSourceUtils.prepareConnectionForTransaction`
  cuando `prepareConnection=true` y el modo de conexión es `ON_CLOSE`; `HibernateJpaVendorAdapter` configura
  `DELAYED_ACQUISITION_AND_HOLD`, que usa `ON_CLOSE`. Por tanto, la conexión se marca read-only y PostgreSQL debería
  rechazar DML/DDL. Es una mitigación real para escrituras, **no** para lectura, `pg_sleep`, locks ni DoS. Además, el
  validador no bloquea `SELECT ... INTO` ni `lo_import`; si el flag read-only no llegara a aplicarse, serían escrituras.
  No se probó contra PostgreSQL.
- **S-02 (matiz)**: la variante equivalente en `POST /api/members/export` **no** es explotable con un cuerpo JSON
  `null`: `RequestResponseBodyMethodProcessor.readWithMessageConverters` comprueba `arg == null && checkRequired(parameter)`
  y lanza `HttpMessageNotReadableException` (400) cuando `required=true` (verificado en `spring-webmvc-6.2.15.jar`).
  En los endpoints con `required=false`, el `null` llega al servicio y produce NPE en `dto.getOrderBy()`; el único
  camino que llega a `findAll` sin filtro es el `export` de asistencias sin cuerpo. La decisión del borrador de no
  incluir la variante de integrantes es correcta.
- **S-04 (matiz)**: el alta de rol con `ADMINISTRATOR` funciona porque `RoleRequestDto.permissions` no tiene `@Valid`
  y la validación anidada no se cascadea (`RoleRequestDto.java:23-24`, `PermissionRequestDto.java:14-15`). Si se añade
  `@Valid` sin cambiar `@NotBlank` por `@NotNull`, Hibernate Validator lanzará `UnexpectedTypeException` (HV000030)
  porque `@NotBlank` no valida enums y **romperá** el alta de roles. La ruta más fiable es `CREATE_ROLE` + `CREATE_USER`
  (o `UPDATE_USER`); `CREATE_USER` solo no basta si no se conoce el id del rol, pero `UPDATE_USER` sí puede listar
  roles por `GET /api/roles` (`RoleController.java:71-84`). La variante `UPDATE_ROLE` para vaciar el rol ADMINISTRATOR
  funciona con un `Set` vacío; la modificación con permisos solapados puede chocar con el orden `clear()` + `addAll`
  de `RoleMapper.java:47-59` y la PK compuesta de `RolePermission`, pero eso no se pudo probar sin BD.
- **S-05 (matiz)**: la comprobación de `UserService.java:169` usa la sede del miembro **actual** del target; el
  `memberId` nuevo se carga en `:159` y se asigna en `:183-185` sin comprobar su sede. El bloqueo de autoedición
  (`:162-163`) solo impide editarse a uno mismo; con una segunda cuenta de la misma sede se puede resetear la
  contraseña de un ADMINISTRATOR y entrar como él. El mismo `UPDATE_USER` permite además desactivar o degradar al
  admin (ver VS-03). Si el admin objetivo no tiene su ficha de miembro en la sede del atacante, el ataque no aplica.
- **S-06 (matiz)**: la escritura cruzada es real, pero el "oráculo de existencia de UUIDs" es más débil de lo que
  sugiere el borrador: `AttendanceService.java:79-82` hace `continue` si el integrante no existe y `:84-87` si está
  inactivo; el endpoint devuelve `void`/200 (`AttendanceController.java:38-41`). Para observar el resultado hace falta
  un permiso de lectura posterior (`MANAGE_ATTENDANCE`/`MANAGE_DASHBOARD`) o el log. Además, el lote usa solo el
  `serviceEventId` del primer DTO (`:58`) y aplica ese evento a todas las filas: un lote con eventos mezclados se
  etiqueta con el primero (integridad de datos).
- **S-08 (matiz)**: el CORS es real, pero con Bearer en `localStorage` no es explotable por sí solo: el navegador no
  adjunta credenciales automáticamente. Es defensa en profundidad; su impacto sube si se migra el refresh a cookies.
  La recomendación de lista explícita y `allowCredentials(false)` es correcta para el esquema actual.
- **S-12 (matiz)**: el camino de usuario inexistente lanza `AuthenticatedUserNotFoundException` (extiende `ApiException`,
  no `UsernameNotFoundException`); `DaoAuthenticationProvider.retrieveUser` lo captura en el `catch (Exception)` y lo
  envuelve en `InternalAuthenticationServiceException`, sin `mitigateAgainstTimingAttack` (verificado en
  `spring-security-core-6.5.7.jar`). El handler `GlobalHandlerException.java:31-35` devuelve el mensaje de la causa
  ("No autorizado.") y `:55-58` devuelve "Credenciales invalidas." para `BadCredentialsException`; la diferencia de
  mensaje y de tiempo es real. El mismo handler devuelve el mensaje interno si el fallo es de BD (ver VS-04).
- **S-15 (matiz)**: la expresión falla cerrado. No hay bypass ni elevación; el impacto es que `DELETE /api/users/{id}`
  queda inutilizable para todos, incluido ADMINISTRATOR, y devuelve 500 por `GlobalHandlerException.java:49-53`.
  Como riesgo de seguridad es Baja; como bug funcional, Media según la rúbrica. Se mantiene Media por la segunda
  lectura, pero el consolidado debería decir "sin impacto de confidencialidad/integridad".
- **S-17 (matiz y rebaja)**: el código y el impacto descrito son exactos, pero la comparación falla **cerrado**:
  ningún no-admin puede crear ni invalidar bautismos, no hay fuga ni escritura cruzada. Es un defecto funcional de
  la comprobación, no un riesgo de seguridad; se propone **Baja** y se remite al eje de calidad.
- **S-18 (matiz)**: la precedencia es correcta, pero el sujeto del bypass no es solo el admin: cualquier usuario con
  `DELETE_SERVICE` (aunque esté desactivado y tenga un access token vivo) puede borrar servicios; un ADMINISTRATOR
  que no tenga `DELETE_SERVICE` sí queda sujeto a `isActive`. La ventana es de hasta 10 minutos.

## Cobertura de endpoints (reconstruida)

Reconstrucción propia con `@(Get|Post|Put|Patch|Delete)Mapping` sobre `features/**/*Controller.java`:
**88 coincidencias = 19 `@RequestMapping` de clase + 69 handlers**. El anexo del borrador tiene 69 filas y coincide
handler por handler; no falta ninguno ni hay rutas nuevas sin documentar.

| Controlador | Handlers | Guarda | ¿Coincide con el anexo? |
|---|---:|---|---|
| `AiController` | 1 | `CHAT_AI` o ADMIN + `isActive` | Sí (#1) |
| `AttendanceController` | 4 | `REGISTER_ATTENDANCE`; `MANAGE_ATTENDANCE`/`MANAGE_DASHBOARD`; `MANAGE_ATTENDANCE` | Sí (#2-#5) |
| `AuthController` | 4 | login público; refresh `isActive(#refreshTokenRequestDto)`; logout y change-password solo `.authenticated()` | Sí (#6-#9) |
| `BaptismController` | 3 | `BAPTISM_CREATE`; `BAPTISM_INVALIDATE`; `VIEW_BAPTISM_PANEL`, todos + ADMIN/`isActive` | Sí (#10-#12) |
| `BranchController` | 4 | `BRANCH_CREATE`; `VIEW_BRANCH_PANEL`; `BRANCH_UPDATE`; `BRANCH_DELETE` | Sí (#13-#16) |
| `CategoryRulesController` | 5 | solo ADMIN + `isActive` | Sí (#17-#21) |
| `DashboardController` | 1 | `MANAGE_DASHBOARD` + ADMIN/`isActive` | Sí (#29) |
| `DiscipuladoController` | 6 | `DISCIPULADO_CREATE`; `VIEW_DISCIPULADO_PANEL`; `DISCIPULADO_DELETE`; `DISCIPULADO_PROGRESS` | Sí (#30-#35) |
| `HealthController` | 1 | público | Sí (#36) |
| `MemberCategoryController` | 4 | 3 ADMIN + `isActive`; GET solo `isActive` | Sí (#22-#25) |
| `MemberController` | 6 | `CREATE_MEMBER`; `UPDATE_MEMBER`; `VIEW_MEMBER_PANEL`/`VIEW_BAPTISM_PANEL`/`MANAGE_ATTENDANCE`/`REGISTER_ATTENDANCE`/`DISCIPULADO_PROGRESS`/`DISCIPULADO_CREATE`; `DELETE_MEMBER` | Sí (#37-#42) |
| `MemberNoteController` | 2 | `CREATE_NOTE`; `DELETE_NOTE` + ADMIN/`isActive` | Sí (#43-#44) |
| `MemberSubCategoryController` | 3 | solo ADMIN + `isActive` | Sí (#26-#28) |
| `MemberTypeController` | 4 | 3 ADMIN + `isActive`; GET solo `isActive` | Sí (#59-#62) |
| `ReportController` | 1 | `MANAGE_REPORT`/`MANAGE_DASHBOARD` + ADMIN/`isActive` | Sí (#45) |
| `RoleController` | 4 | `CREATE_ROLE`; `UPDATE_ROLE`; `DELETE_ROLE`; `VIEW_ROLE_PANEL`/`UPDATE_USER` + ADMIN/`isActive` | Sí (#46-#49) |
| `ServiceController` | 5 | `CREATE_SERVICE`; `UPDATE_SERVICE`; `DELETE_SERVICE` (precedencia rota); `VIEW_SERVICE_PANEL`/`REGISTER_ATTENDANCE`; `UPDATE_SERVICE` | Sí (#50-#54) |
| `ServiceEventController` | 4 | `REGISTER_EVENTS`; `CANCEL_EVENTS`; `MANAGE_ATTENDANCE`/`VIEW_EVENTS_PANEL`; `REGISTER_ATTENDANCE` | Sí (#55-#58) |
| `UserController` | 7 | `VIEW_USER_PANEL`; `CREATE_USER`; GET `@me` solo `isActive`; `VIEW_USER_PANEL`; `UPDATE_USER`; `UPDATE_USER`; `DELETE_USER` (roto) | Sí (#63-#69) |

**Recuento**: 69 handlers; **65 con `@PreAuthorize`** (64 con `isActive(authentication)` + 1 con
`isActive(#refreshTokenRequestDto)`); **4 sin `@PreAuthorize`** (login y health públicos; logout y change-password
solo `.authenticated()`); 0 endpoints `/api/**` públicos por accidente. El anexo acierta también en los matices
`#52` (isActive parcial por precedencia), `#69` (isActive presente pero inalcanzable) y `#40` (scoping parcial).
La única diferencia es de forma: `MemberNoteController` es una clase aparte, pero sus dos rutas están incluidas
como #43/#44; no hay handlers fuera del anexo.

También se comprobó el potencial `POST /api/members/export` con cuerpo `null`: **no** produce la fuga global porque
`@RequestBody` es `required=true` y Spring MVC rechaza el argumento nulo con 400. En `MemberController.search`,
`AttendanceController.search`, `UserController.search` y `ServiceEventController.calendar` (`required=false`), el
`null` llega al servicio y produce NPE/500, no una lectura global. El caso de S-02 es el único que llega a
`findAll` sin filtro porque `export` no desreferencia el DTO antes de la specification.

## Hallazgos nuevos

### VS-01 — Clave de Google Maps hardcodeada en el bundle

- **Severidad**: Baja (Media si la clave no tiene restricciones de referrer/IP en Google Cloud; no verificable)
- **Ubicación**: `src/main/resources/static/chunk-CJDLSKNK.js:5`
- **Evidencia**: `EE={googleMapsApiKey:"AIzaSyBdLCeyYmOSGHt9Ct5Jn3U8Kt-Lai065ZE"};var Wi={baseUrl:"/api",googleMapsApiKey:EE.googleMapsApiKey};`
- **Impacto**: la clave viaja en el bundle público de la PWA; cualquiera puede extraerla. Una clave de Google Maps
  de navegador no es un secreto de servidor, pero si no está restringida por HTTP referrer (y, si aplica, por API)
  puede usarse desde otro sitio o desde un script para consumir cuota y generar facturación. El borrador afirma en
  "Descartados" que no hay secretos hardcodeados; esa afirmación es incorrecta para esta clave. No se puede
  comprobar desde el repo si la clave está activa o restringida.
- **Recomendación**: restringir la clave por referrer y por las APIs de Maps realmente usadas; rotarla si se confirma
  uso no autorizado; no tratarla como secreto de servidor, pero sí inventariarla y monitorizar su cuota.
- **Esfuerzo**: S

### VS-02 — Fuga del nombre de un integrante de otra sede en `PUT /api/members/{id}`

- **Severidad**: Baja (Media si se encadenan UUIDs filtrados y se quiere confirmar identidad)
- **Ubicación**: `features/member/service/MemberPersistenceService.java:160` y `:163`,
  `features/category/repository/mapper/MemberMapper.java:133-134`,
  `features/member/exceptions/MemberNoHasCategoryForThisSubCategoryException.java:8`
- **Evidencia**: `memberMapper.updateEntityFromDto(dto, member);` se ejecuta antes de
  `if (!user.hasAuthority(...) && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) { throw ... }`;
  el mapper lanza `new MemberNoHasCategoryForThisSubCategoryException(member.getCompleteName())`, cuyo mensaje es
  `"El integrante " + memberName + " no cumple los requisitos..."`, y `GlobalHandlerException.java:24-29` lo devuelve
  al cliente.
- **Impacto**: un usuario con `UPDATE_MEMBER` de la sede A puede enviar un `PUT` contra un `memberId` de la sede B
  con una categoría/subcategoría incoherente; el mapper falla **antes** de la comprobación de sede y el error 409
  incluye el nombre completo del integrante ajeno y confirma que el UUID existe. La transacción revierte, así que no
  hay escritura, pero sí una fuga de PII y un oráculo de existencia entre sedes. Requiere conocer el UUID (no es
  enumerable por fuerza bruta) y `UPDATE_MEMBER`.
- **Recomendación**: mover la comprobación de sede al inicio de `MemberPersistenceService.update`, antes de
  `updateEntityFromDto` y antes de la subida a Cloudinary; devolver un error genérico sin el nombre del integrante.
- **Esfuerzo**: S

### VS-03 — `UPDATE_USER` permite desactivar o degradar a un administrador de la misma sede

- **Severidad**: Media (latente: depende de que un rol no-admin tenga `UPDATE_USER`)
- **Ubicación**: `features/user/controller/UserController.java:87-91` y `:102-106`,
  `features/user/service/UserService.java:196-212` y `:249-269`
- **Evidencia**: `updateActive` solo comprueba `user.getId().equals(userToUpdate.getId())` y la sede
  (`if (!user.hasAuthority(ADMINISTRATOR) && !user.getMember().getBranch().getId().equals(userToUpdate.getMember().getBranch().getId()))`);
  no comprueba si el target tiene `ADMINISTRATOR`. `update` hace `user.getRoles().clear(); user.getRoles().addAll(...)`
  sin jerarquía. El `PATCH` recibe `@RequestParam boolean active`.
- **Impacto**: un usuario no-admin con `UPDATE_USER` puede desactivar (`PATCH ...?active=false`) a un administrador
  cuya ficha de miembro esté en su sede, o vaciar/rebajar sus roles con `PUT /api/users/{id}` (`rolesId` vacío o sin
  `ADMINISTRATOR`). El bloqueo de autoedición no impide usar una segunda cuenta. Es un bloqueo/denegación de servicio
  de una cuenta privilegiada y una degradación de privilegios, no una elevación. Si `UPDATE_USER` es solo de ADMIN,
  el riesgo desaparece.
- **Recomendación**: exigir `ADMINISTRATOR` para modificar usuarios que tengan `ADMINISTRATOR`, o impedir que un
  no-admin cambie `active`/`roles` de un usuario con más privilegios; añadir una prueba de jerarquía.
- **Esfuerzo**: S

### VS-04 — Mensaje interno de autenticación devuelto en el login público

- **Severidad**: Baja
- **Ubicación**: `advice/GlobalHandlerException.java:31-35`, `features/auth/controller/AuthController.java:27-30`,
  `configurations/AuthenticationConfig.java:17-20`
- **Evidencia**: `public ResponseEntity<ErrorResponseDto> handleAInternalAuthenticationServiceException(...) { ... return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), ...); }`
  y el login es público (`@PostMapping("/login")`).
- **Impacto**: `DaoAuthenticationProvider` envuelve en `InternalAuthenticationServiceException` cualquier excepción
  del `UserDetailsService` que no sea `UsernameNotFoundException`, incluidas las de base de datos. El handler devuelve
  el mensaje interno al cliente; si el fallo es de infraestructura, un atacante no autenticado puede obtener detalles
  del backend (tipo de error, a veces nombre de la excepción/JPA, estado de la conexión). No es explotable por sí solo,
  pero es un canal de reconocimiento en el endpoint más expuesto.
- **Recomendación**: devolver un mensaje fijo ("No autorizado.") para `InternalAuthenticationServiceException` y
  registrar el detalle solo en el log; no usar `ex.getMessage()` en respuestas HTTP.
- **Esfuerzo**: S

### VS-05 — La foto del integrante no se borra de Cloudinary al eliminar el miembro

- **Severidad**: Baja (retención de PII)
- **Ubicación**: `features/member/service/MemberService.java:180` y `:203`, `utils/cloudinary/CloudinaryService.java:41-43`,
  `features/member/Member.java:43-44`
- **Evidencia**: `String pictureUrl = member.getPictureProfileUrl();` ... `if (pictureUrl != null) pictureUtils.delete(pictureUrl);`
  y `deletePhoto(String publicId) { ... cloudinary.uploader().destroy(publicId, ...); }`; el `Member` tiene un campo
  `publicId` que no se usa en el borrado.
- **Impacto**: `destroy` espera un `public_id`, no una URL; al pasarle `pictureProfileUrl` la imagen no se elimina de
  Cloudinary y queda accesible en su URL pública después de borrar al integrante. El borrador descarta correctamente
  que se puedan borrar archivos ajenos, pero no cubre esta retención de la foto propia. No se pudo comprobar si la
  cuenta de Cloudinary restringe el acceso a las URLs.
- **Recomendación**: usar `member.getPublicId()` en `MemberService.delete` (y validar que no sea nulo) antes de
  borrar la entidad; si se quiere ser estricto, borrar la imagen primero y abortar el borrado de la fila si falla.
- **Esfuerzo**: S

## Consistencia del informe

- **Resumen vs. tabla**: 22 hallazgos = 1 Crítica + 5 Altas + 11 Medias + 5 Bajas. La cuenta es correcta. Tras la
  recalibración de S-17 a Baja, el consolidado debe pasar a **1 Crítica + 5 Altas + 10 Medias + 6 Bajas**. Las
  "5 Altas" incluyen S-04 y S-05, que son latentes; conviene decir "3 Altas explotables hoy + 2 latentes" para no
  confundir impacto con probabilidad.
- **Anexo**: 69 handlers, 65 con `@PreAuthorize` (64 + 1) y 4 sin él; coincide con la reconstrucción y con el
  código. No hay endpoints faltantes, duplicados ni rutas inexistentes. El typo `/api/auth/changge-password` es real
  en el controlador y en el bundle; no es un error del anexo.
- **Citas**: todas las rutas y líneas citadas existen y el código coincide. S-20 tiene una diferencia menor entre la
  tabla (`:52-64`) y el detalle (`:49-65`); ambas contienen el `catch`, no afecta. No hay hallazgos duplicados: S-04
  (roles) y S-05 (password/member) son distintos; S-07 (PII a roles) y S-14 (PII a Gemini) también.
- **"Descartados" incorrectos o incompletos**:
  - "Secretos hardcodeados": **falso**; la clave `AIza...` de Google Maps está en `chunk-CJDLSKNK.js:5` (VS-01).
    No es un secreto de servidor, pero la frase "sin resultados" no es correcta.
  - "Escrituras vía IA": el validador no bloquea `SELECT ... INTO` ni `lo_import`; la protección real es el
    `@Transactional(readOnly=true)`, que es un flag de Spring/Hibernate y no se probó contra PostgreSQL. La frase
    "impide escribir" es demasiado fuerte; debe decir "mitiga DML/DDL, no lectura/DoS".
  - "Borrado de archivos ajenos": correcto en que no borra archivos de otros, pero incompleto: no borra la foto
    propia (VS-05).
  - El resto de descartes (CSRF, `alg=none`, `/h2-console`, endpoints públicos, contraseñas BCrypt, replay del
    refresh, subida con Tika) se sostiene por lectura.
- **Recomendaciones que pueden romper funcionamiento**:
  - S-01 propone "prohibir `WITH`", pero el `buildSqlSystemPrompt` ordena usar `WITH` para consultas complejas
    (`AiService.java:189-191`). Si se prohíbe, se rompe el caso de uso previsto; mejor lista blanca de tablas/
    columnas y rol de BD restringido.
  - S-21 propone exigir `VIEW_MEMBER_PANEL`/`VIEW_ROLE_PANEL` a los catálogos. Los formularios de asistencia y
    bautismo usan tipos/categorías con permisos operativos; exigir `VIEW_MEMBER_PANEL` puede dejar sin catálogos a
    un usuario con solo `REGISTER_ATTENDANCE`. Hay que elegir el permiso por consumidor o crear uno de catálogo.
  - S-04: si se añade `@Valid` a `RoleRequestDto.permissions`, hay que cambiar `@NotBlank` por `@NotNull` en
    `PermissionRequestDto.name`; de lo contrario el alta de roles falla con `UnexpectedTypeException`.
  - S-10: guardar el refresh token hasheado obliga a hashear también en `findByToken`/`deleteIfValid`; no es solo
    un cambio de columna.
- **Coherencia de severidad**: S-01 Crítica es defendible por el impacto total (PII, `password_hash`, `refresh_token`),
  pero debe explicitar que es un SQLi mediado por LLM y que `pg_read_file`/`dblink` dependen de privilegios. S-08 y
  S-15 son Media/Baja según se priorice exposición vs. disponibilidad; S-17 es Baja. S-22 no es un "sin CVEs"
  demostrado. Con esas matizaciones, las cifras del resumen son internamente consistentes.
- **Los 3 riesgos principales**: S-01 es real y explotable en impacto, con la condición del LLM; S-02 es real y
  determinista; S-04 es real en código pero latente por `role_permission`. El borrador no sobrevende S-02 ni S-04;
  sí conviene rebajar la certeza de S-04/S-05/S-16 a "latente" en el resumen, como ya hace en el detalle.

## Límites de la verificación

- **Sin runtime ni BD**: no se ejecutó `bootRun` ni `test`; no se explotó ningún endpoint. Todo es análisis estático
  y lectura de bytecode de los jars resueltos por el build.
- **`role_permission`**: no está en el repo; no se pudo verificar qué roles tienen `CREATE_USER`, `UPDATE_USER`,
  `CREATE_ROLE`, `UPDATE_ROLE`, `BRANCH_UPDATE` o `BRANCH_DELETE`. S-04, S-05, S-16 y VS-03 quedan condicionados a
  ese sembrado. Si esos permisos son exclusivos de ADMINISTRATOR, su severidad baja.
- **Privilegios de PostgreSQL**: no se pudo comprobar si `DB_USER` es superusuario, si `dblink` está instalado, si
  `pg_read_file`/`COPY ... TO PROGRAM` son invocables ni si el driver ejecuta el segundo statement de
  `SELECT 1;;SELECT ...`. `pg_sleep`, `information_schema`, `current_user` y `UNION` sí son viables con un rol normal.
- **`@Transactional(readOnly=true)`**: se verificó la cadena Spring/Hibernate (`HibernateJpaDialect`,
  `DataSourceUtils.prepareConnectionForTransaction`, `DELAYED_ACQUISITION_AND_HOLD`), pero no se ejecutó contra
  PostgreSQL; no se puede afirmar que toda escritura falle, solo que la conexión se marca read-only.
- **Frontend**: solo se auditó el bundle minificado; no hay fuentes Angular ni `package.json`. La CSP del despliegue
  real (proxy/CDN) no se puede verificar; en el repo no hay CSP. La clave de Google Maps no se probó contra Google.
- **Cloudinary**: no se llamó a la API; se infiere del contrato del SDK que `destroy` espera un `public_id` y que una
  URL no borra la imagen. No se pudo comprobar si las URLs de la cuenta son públicas o firmadas.
- **SpEL**: no se ejecutó la expresión dentro de un contexto Spring Security completo; el fallo se deduce de la
  sintaxis (`hasAuthority(...)` cierra antes de `.DELETE_USER`) y del `GlobalHandlerException` genérico. El jar
  `spring-expression 6.2.15` está en la caché del build.
- **Dependencias**: no se ejecutó SCA ni se resolvió el árbol transitivo; las CVE de S-22 no son verificables aquí.
- **Path traversal en `MvcConfig`**: no se reporta como hallazgo. El `PathResourceResolver` se sobrescribe sin
  `checkResource`, pero `ResourceHttpRequestHandler.isInvalidPath` bloquea `../` y `..\` literales; los encodings
  dependen de Tomcat y no se probaron. Queda como riesgo residual, no confirmado.
- **`@RequestBody` nulo**: se verificó en `spring-webmvc-6.2.15` que `required=true` rechaza el argumento nulo con
  400; para `required=false` el nulo llega al servicio y produce NPE en los `search`, salvo el `export` de S-02.
  No se probó con peticiones reales.

---

**Resumen de conteo**: 20 CONFIRMADO, 2 PARCIAL (S-13, S-22), 0 REFUTADO, 0 NO VERIFICABLE.
**Nuevos**: 5 (VS-01…VS-05). **Inventario**: 69/69 handlers, 65 con `@PreAuthorize`, 4 sin él, sin diferencias.
**Consolidado**: apto con las correcciones de S-13, S-17, S-18, S-06, S-04/S-05, los descartes de secretos/readOnly/
foto y las recomendaciones de S-01/S-21.
**Actualización 2026-10-24**: S-06 pasa a **DESCARTADO** por regla de negocio; quedan 19 CONFIRMADO y 2 PARCIAL.
