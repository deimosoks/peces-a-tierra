<!-- estado-doc -->
> **Documento histórico (2026-10-24).** Refleja el estado en el momento de su creación; varios hallazgos ya fueron corregidos. El estado vigente está en `audit/INFORME-FINAL.md`, `audit/TODO.md` y `audit/fixes-m02-m24.md`.

# Verificación — eje de calidad

## Veredicto global

El borrador es fiable: **22 de 24 hallazgos quedan CONFIRMADOS (91,7 %)**, 2 son PARCIALES (Q-07 y Q-12), 0 REFUTADOS y 0 NO VERIFICABLES. Las rutas y las líneas citadas existen y el código reproduce el comportamiento descrito salvo en los dos casos parciales. La única imprecisión menor de ubicación es `SecurityService.java:25` (la llamada a `findByUsername` empieza en la 24; la 25 es el `orElseThrow`).

Los tres riesgos del resumen ejecutivo son reales, con un matiz: el riesgo (2) —contrato y zonas horarias— se sostiene por Q-08 y Q-09, no por Q-07, que es condicional. El riesgo (1) —pérdida de datos y bloqueo por identidad de `Branch`— es el más sólido y el riesgo (3) —errores de entrada convertidos en 500— se confirma en Q-03, Q-06 y Q-11, además del 500 de `Discipulado` sin `@Valid` y del NPE de `gender`.

Las afirmaciones de ausencia resisten el barrido: no encontré mapeos DTO con asociaciones lazy fuera de `@Transactional` ni entidades JPA en respuestas. El “N+1 mayormente mitigado” es correcto por `default_batch_fetch_size=50`, pero no universal: `UserService.search` no sobrescribe `findAll` con `@EntityGraph` y depende del batch fetch.

El borrador **es apto para el consolidado**, con dos correcciones (bajar Q-07 a Baja/latente y acotar Q-12 a los DTOs realmente no validados) y una adición obligatoria: **VQ-01**, que es una fuga de datos entre sedes que el borrador no lista.

## Tabla de verificación

| ID | Veredicto | Severidad (orig → propuesta) | Esfuerzo | Bug real / latente / deuda | Nota |
|---|---|---|---|---|---|
| Q-01 | CONFIRMADO | Alta → Alta | S | real | `note` se pisa y `municipality` recibe `city`; la PWA envía ambos. |
| Q-02 | CONFIRMADO | Media → Media | S | real | `Branch` sin `equals`; el principal está desacoplado; mismo patrón en `:87`. |
| Q-03 | CONFIRMADO | Media → Media | S | real | Solo en la ruta de error; si no es `ApiException`, la foto queda huérfana. |
| Q-04 | CONFIRMADO | Media → Media | S | real | `setCategoryLocked(member.isCategoryLocked())`; la PWA sí envía el campo. |
| Q-05 | CONFIRMADO | Media → Media | S | real | La consulta incluye la propia fila; el `update` de categoría tampoco comprueba. |
| Q-06 | CONFIRMADO | Media → Media | S | real | Los cuatro cuerpos opcionales desreferenciados; `page < 0` también da 500. |
| Q-07 | PARCIAL | Media → Baja | S | latente | La UI envía 23:59 por defecto; solo falla si el fin llega a 00:00. |
| Q-08 | CONFIRMADO | Media → Media | M | real | Verificado con Jackson 2.19.4: el `Z` se descarta y `toUTC` suma 5 h. |
| Q-09 | CONFIRMADO | Media → Media | S | real | Escritura `toUTC` frente a filtro `atOffset(UTC)`; la PWA envía 23:59:59. |
| Q-10 | CONFIRMADO | Media → Media | M | real | Sin `maxRows` ni timeout; el `setMaxRows` propuesto mutaría el template singleton. |
| Q-11 | CONFIRMADO | Media → Media | S | real | 401 para permisos y 500 para entrada inválida; el interceptor 403 hace logout. |
| Q-12 | PARCIAL | Media → Media | S | real/latente | `@Valid` sí existe en 14 controllers; el fallo es de estos DTOs y de Discipulado. |
| Q-13 | CONFIRMADO | Baja → Baja | S | real | Dos cargas con el mismo `@EntityGraph` por petición; `login` también carga dos veces. |
| Q-14 | CONFIRMADO | Baja → Baja | S | real | Se borra con la URL; `publicId` está disponible; el destroy va dentro de la transacción. |
| Q-15 | CONFIRMADO | Baja → Baja | M | real | `findAll(spec)` y listas del dashboard sin límite. |
| Q-16 | CONFIRMADO | Baja → Baja | S/M | deuda | 9 controllers extienden `BaseController`; 0 llamadas; `log` solo tiene la entidad. |
| Q-17 | CONFIRMADO | Baja → Baja | L | deuda | 65 `@PreAuthorize` en 18 controllers y 8 `beforeUpdate`; conteos exactos. |
| Q-18 | CONFIRMADO | Baja → Baja | S | real | El log usa `user` (actor) en username/member/active; roles se recrean con nueva metadata. |
| Q-19 | CONFIRMADO | Baja → Baja | S | real | `NoteNotFoundException` para integrante y excepciones de borrado/creación fuera de contexto. |
| Q-20 | CONFIRMADO | Baja → Baja | M | deuda | Dependencia duplicada en 22/32, `src (2).zip` versionado y un solo `contextLoads`. |
| Q-21 | CONFIRMADO | Baja → Baja | S | deuda | Wrapper 8.14.3; el fallo con JDK 25 está en el baseline del contrato. |
| Q-22 | CONFIRMADO | Baja → Baja | M | deuda | `MemberMapper` en `category.repository.mapper` con tres repositorios; `RoleMapper` hace un count por rol. |
| Q-23 | CONFIRMADO | Baja → Baja | S | real | `LocalDateTime.now()` y `LocalDate.now()`; el Dockerfile no fija TZ. |
| Q-24 | CONFIRMADO | Baja → Baja | S | latente | El lote se asigna al evento del primer elemento; la PWA envía uno solo. |

## Refutaciones y matices

### Q-07 — PARCIAL: el reporte no excluye el último día en el flujo normal de la PWA

La evidencia citada no sostiene el “siempre”. El filtro del reporte usa `datetime-local` y la función `e(n)` solo añade `T00:00:00` cuando el valor **no contiene** `T`; un `datetime-local` siempre lo contiene. Además, la página inicializa el rango con `r.setHours(23,59,59,999)` y `toLocalISOString` devuelve `YYYY-MM-DDTHH:mm`, es decir, el fin por defecto es `23:59` (`main-2X72WS5U.js`, minificado). Con ese valor el `<=` del spec incluye el último día completo.

El mecanismo sí existe si el cliente envía un fin a las `00:00:00` (por ejemplo, un consumidor de la API o un usuario que borre la hora). En ese caso se pierde el día, y la recomendación de normalizar el fin de día es válida. Por eso: **PARCIAL, Baja, latente**, no “real/Media”. La recomendación literal `endDate.plusDays(1)` exclusiva rompería el caso contrario: con un fin explícito a las `23:59:59` incluiría casi dos días más. Debe ser condicional (solo si la hora es `00:00`).

### Q-12 — PARCIAL: la validación no está “nunca activa”

`@Valid` aparece en 14 controllers (`MemberController:37,51`, `UserController:47,88`, `AttendanceController:38,96`, `BaptismController:35,50,65`, `RoleController:35,50`, `ServiceController:35,49`, `MemberTypeController:30,41`, `MemberCategoryController:30,41`, `MemberSubCategoryController:28,39`, `BranchController:34,62`, `CategoryRulesController:30,41`, `ServiceEventController:35`, `MemberNoteController:31`, `AuthController:28,34,40,47`). La frase “no hay `@Valid` ni cascada en los DTOs anidados” es cierta solo para los DTOs citados: `DiscipuladoController` no lo usa en **ninguno** de sus cuerpos (crear, buscar, progress y progressUpdate), `ReportController` y `AiController` tampoco, y los DTOs anidados (`OrderBy`, `PermissionRequestDto`) no cascadean.

Por tanto, el núcleo es correcto: `@NotBlank` sobre `LocalDateTime`/`boolean`/enum es inválido y, si se activara, Hibernate Validator lanzaría `UnexpectedTypeException`; hoy no se ejecuta. Los 500 reales son el `dateStarted` nulo de Discipulado (columna `nullable = false`) y el `gender` nulo en `MemberPersistenceService:57` (y en `MemberMapper:144`, que el borrador no cita). Mantengo **Media** por esos dos 500, pero el texto debe acotarse.

### Matices de hallazgos confirmados

- **Q-02**: el diagnóstico es exacto, pero conviene subrayar que el mismo `equals` de identidad aparece en `BaptismService:87` (invalidar), no solo en `:54`. La corrección por `getId()` debe cubrir ambas líneas. En cambio, la comparación de entidades dentro de un `Criteria` (`BaptismSpecification:32`) sí se traduce a FK y no tiene este problema.
- **Q-03**: el NPE solo ocurre si `memberPersistenceService.save` lanza una `ApiException`; si lanza una `DataIntegrityViolationException` o similar, el 500 es directo y la foto queda huérfana. La recomendación de limpiar también en el `catch` genérico es correcta.
- **Q-06**: además de los cuatro puntos citados, el mismo patrón afecta a `BaptismService:109` (`dto.getOrderBy()`), `DiscipuladoService:94` y `ReportService:82`; el cuerpo requerido de `Baptism`/`Discipulado`/`Report` lo hace latente (un `null` literal se rechaza con 400, comprobado con `MockMvc`), mientras que los `@RequestBody(required = false)` de `Member`, `Attendance`, `User` y `ServiceEvent` sí dejan pasar `dto == null`. La recomendación de normalizar a un DTO vacío es la adecuada; debe hacerse **antes** de tocar el spec para no perder el filtro de sede (ver VQ-01).
- **Q-10**: `jdbcTemplate.setMaxRows(...)` y `setQueryTimeout(...)` sobre el `JdbcTemplate` inyectado mutan la configuración del bean singleton y afectan a cualquier otra consulta concurrente. La corrección debe usar una instancia dedicada (`new JdbcTemplate(dataSource)`) o un `PreparedStatementCreator`/`ResultSetExtractor` con límite. A esto se suma VQ-07: el `RestTemplate` de Gemini no tiene timeouts.
- **Q-11**: el diagnóstico es correcto, pero la lista de entradas que terminan en 500 es más amplia: un `BindException` de `@ModelAttribute` (por ejemplo `birthdate=abc` en `MemberController.create`), un `NoResourceFoundException` de una ruta `/api` inexistente y un `PropertyReferenceException` de un `orderBy` inválido también caen en `@ExceptionHandler(Exception.class)`. No cambia la severidad; amplía el alcance de la prueba.
- **Q-14**: el `delete` está dentro de `@Transactional(rollbackFor = Exception.class)`, pero el `update` borra la foto anterior **después** de que `memberPersistenceService.update` haya confirmado. Si Cloudinary falla en ese punto, el cliente recibe 500 aunque la actualización ya se aplicó; el `TransactionSynchronization` propuesto no resuelve ese caso por sí solo.
- **Q-16**: confirmado que ninguno de los 9 controllers llama a `success`, `created`, `noContent` ni `error`; borrar `BaseController` exige quitar el `extends` en 9 archivos, sin cambio funcional. La entidad `Log` no tiene repositorio, servicio ni controller.
- **Q-20**: la recomendación de “perfil H2” no es aplicable tal cual: las consultas usan `unaccent` (extensión de PostgreSQL), el esquema es externo y `ddl-auto=validate`. La vía realista es Testcontainers PostgreSQL; H2 exigiría alias de funciones y un esquema paralelo.
- **Q-22**: `UserService.search` también pagina sin `@EntityGraph`; el `default_batch_fetch_size=50` evita el N+1 por fila, pero el “mayormente mitigado” no cubre ese caso. `RoleMapper` sí es N+1 puro (un count por rol).
- **Q-23**: la premisa “el contenedor corre en UTC” no se puede verificar desde el repo (el Dockerfile no fija `TZ`); si el despliegue inyecta `TZ=America/Bogota`, el impacto desaparece. El defecto de fondo —depender de la zona del servidor— es real.
- **Q-24**: el `HashSet<Attendance>` no colapsa registros porque `Attendance` no define `equals/hashCode`; el único defecto es asignar todos al primer evento.

## Hallazgos nuevos

### VQ-01 — La exportación de asistencias sin cuerpo omite el filtro de sede
- **Severidad**: Alta
- **Tipo**: real (con dependencia del eje S: el IDOR pertenece a Seguridad; aquí se documenta la causa de calidad y el impacto)
- **Ubicación**: `features/attendance/controller/AttendanceController.java:79-81`, `utils/specs/AttendanceSpecification.java:37,60-64`, `features/attendance/service/AttendanceService.java:199-202`
- **Evidencia**: el controller declara `@RequestBody( required = false ) AttendanceFiltersRequestDto dto`; el servicio hace `Specification<Attendance> spec = attendanceSpecification.build(dto, user); List<Attendance> members = attendanceRepository.findAll(spec);`; y el spec envuelve **todo**, incluido el filtro de sede, en `if (dto != null) { ... if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name())) { predicates.add(cb.equal(branch.get("id"), user.getMember().getBranch().getId())); } ... }`.
- **Impacto**: un usuario autenticado no administrador con `MANAGE_ATTENDANCE` (o `MANAGE_DASHBOARD`) puede hacer `POST /api/attendances/export` sin cuerpo; `dto` llega nulo, el spec no añade ninguna predicado de sede y la respuesta incluye asistencias de todas las sedes, con nombre del integrante, categoría, tipo, subcategoría, sede y nota. Es una fuga de datos personales entre sedes, no un 500. El `search` equivalente no la expone porque revienta antes en `dto.getOrderBy()`; el `export` no desreferencia el DTO y por eso sí responde.
- **Recomendación**: sacar el predicado de sede del `if (dto != null)` (o normalizar `dto` a un DTO vacío antes de construir el spec) y, si se mantiene el cuerpo opcional, aplicar siempre la sede del usuario para no administradores. Añadir una prueba de `export` sin cuerpo con un usuario de sede B.
- **Esfuerzo**: S

### VQ-02 — El filtro JWT no captura `JwtException` ni usuario inexistente y responde 500
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `configurations/JwtAuthFilter.java:49-65`, `features/auth/service/JwtService.java:27-33`, `features/user/service/CustomUserDetailsServiceImpl.java:22-23`
- **Evidencia**: el filtro solo captura `ExpiredJwtException | MalformedJwtException`; `jwtService.extractUsername(jwt)` termina en `parseSignedClaims(token)`, que puede lanzar `SignatureException` (firma inválida o clave distinta), `UnsupportedJwtException` o `IllegalArgumentException`. Además `loadUserByUsername` lanza `AuthenticatedUserNotFoundException` (una `ApiException`) si el usuario fue borrado, y esa excepción nace **dentro del filtro**, antes del `DispatcherServlet`, por lo que el `@RestControllerAdvice` no la ve.
- **Impacto**: un token manipulado, firmado con otra clave o de un usuario eliminado (durante los 10 minutos de vida del access token) produce un 500 con el cuerpo genérico de error, no un 401. El interceptor de la PWA solo refresca en 401 y hace logout en 403; ante un 500 muestra el error genérico y no recupera la sesión. Además se llenan los logs con stacktraces de peticiones no autenticadas.
- **Recomendación**: capturar `JwtException | IllegalArgumentException` y escribir un 401 con `ErrorResponseDto`; capturar también `AuthenticatedUserNotFoundException`/`UsernameNotFoundException` en el filtro y devolver 401. No dejar que una excepción del filtro llegue al `/error` por defecto.
- **Esfuerzo**: S

### VQ-03 — Actualizar username o nombre de tipo a un valor existente devuelve 500
- **Severidad**: Baja
- **Tipo**: latente (real para un administrador que edite)
- **Ubicación**: `features/user/service/UserService.java:54-55,187-189`, `features/user/User.java:33-34`; `features/type/service/MemberTypeService.java:33-35,59-70`, `features/type/MemberType.java:19-20`
- **Evidencia**: en creación hay comprobación (`if (userRepository.findByUsername(...).isPresent()) throw new UsernameAlreadyRegister(...)` y `if (memberTypeRepository.existsByName(...))`), pero en actualización no: `if (userRequestDto.getUsername() != null) { user.setUsername(userRequestDto.getUsername()); }` y `memberTypeMapper.updateEntityFromDto(...)` sin comprobar el nombre. Ambas columnas son `@Column(nullable = false, unique = true)`.
- **Impacto**: si un administrador renombra un usuario al username de otro, o un tipo al nombre de otro, la transacción llega al `INSERT/UPDATE` y la constraint única lanza `DataIntegrityViolationException`, que el handler genérico convierte en 500 “Ha ocurrido un error inesperado”. El cliente no recibe el 409 explicable y no sabe qué corregir. `MemberCategoryService.update` tiene el mismo hueco (ya mencionado en Q-05).
- **Recomendación**: añadir `existsByUsernameAndIdNot(username, id)` y `existsByNameAndIdNot(name, id)` antes de mutar, y lanzar las excepciones de conflicto existentes (`UsernameAlreadyRegister`, `TypeWithNameAlreadyExistsException`, `CategoryWithNameAlreadyExistsException`).
- **Esfuerzo**: S

### VQ-04 — El filtro de edad por rango pierde a los integrantes que cumplen años
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `utils/specs/MemberSpecification.java:99-105`
- **Evidencia**: `int minAge = Math.min(...); int maxAge = Math.max(...); LocalDate today = LocalDate.now(); LocalDate maxBirthdate = today.minusYears(minAge); LocalDate minBirthdate = today.minusYears(maxAge); predicates.add(cb.between(root.get("birthdate"), minBirthdate, maxBirthdate));`
- **Impacto**: para el rango 20–30, `minBirthdate` es `today - 30 años` y `maxBirthdate` es `today - 20 años`. Un integrante nacido el 1 de junio de 1996, que hoy tiene 30 años y cumple 31 en 2027, tiene birthdate anterior a `today - 30 años` y queda **fuera** del rango, aunque la UI lo muestra como “30 años” (`ChronoUnit.YEARS`). El filtro devuelve edades exactas entre 20,0 y 30,0, no los enteros 20–30; se pierden aproximadamente la mitad de los que están en el último año de cada edad.
- **Recomendación**: calcular el límite inferior como `today.minusYears(maxAge + 1).plusDays(1)` (el día en que la persona cumple `maxAge + 1`) y cubrirlo con una prueba de frontera (nacido justo antes y justo después del cumpleaños). Mantener `LocalDate.now()` en la zona de negocio es Q-23.
- **Esfuerzo**: S

### VQ-05 — La IA convierte cualquier fallo interno en un 200 con mensaje amable
- **Severidad**: Baja
- **Tipo**: real / deuda
- **Ubicación**: `features/ai/service/AiService.java:80-86`
- **Evidencia**: el método `processChat` envuelve todo el flujo en `try { ... } catch (Exception e) { log.error("Error processing AI chat", e); return AiChatResponseDto.builder().answer("Lo siento, hubo un error procesando tu solicitud. Por favor intenta de nuevo.").requiresClarification(true).build(); }`.
- **Impacto**: un 429/timeout de Gemini, un error de SQL o un JSON irrecuperable se presentan al cliente como una respuesta válida (HTTP 200, `requiresClarification=true`), indistinguible de una pregunta ambigua. El cliente no reintenta, el monitoreo HTTP no detecta la caída y el soporte solo ve el log. El endpoint de IA es el único que no usa el contrato de errores del resto de la API.
- **Recomendación**: dejar propagar los fallos de infraestructura a `GlobalHandlerException` con 502/503 (o añadir un campo `error`/`retryable` al DTO) y reservar el mensaje amable para el caso “no se pudo generar SQL” o “faltan datos”. Mantener el `log.error`.
- **Esfuerzo**: S

### VQ-06 — Permisos duplicados en un rol: el `Set` no deduplica y el alta falla
- **Severidad**: Baja
- **Tipo**: latente
- **Ubicación**: `features/role/dtos/RoleRequestDto.java:23-24`, `features/role/dtos/PermissionRequestDto.java:12-15`, `features/role/service/RoleService.java:44-57`, `features/role/mapper/RoleMapper.java:49-59`
- **Evidencia**: `@NotNull private Set<PermissionRequestDto> permissions;` y `PermissionRequestDto` no define `equals/hashCode`; `RoleService.create` recorre el set y crea un `RolePermission` por elemento con `RolePermissionId(permission, roleId)`. `RolePermission` tampoco define `equals/hashCode` (solo su `@Embeddable RolePermissionId` lo hace), así que dos objetos con el mismo `RolePermissionId` caben en el `HashSet` de `Role.permissions` y se persisten dos veces.
- **Impacto**: un cliente que envíe `[{name:"CREATE_ROLE"},{name:"CREATE_ROLE"}]` (o el mismo permiso repetido en el formulario) provoca dos inserts con la misma clave compuesta: Hibernate lanza `EntityExistsException`/`PersistentObjectException` o la base viola la PK, y el usuario recibe un 500. El `Set` del DTO da una falsa sensación de deduplicación.
- **Recomendación**: deduplicar por `name` en el servicio (`Collectors.toMap(PermissionRequestDto::getName, p -> p, (a,b) -> a)`) o cambiar el DTO a `Set<AppPermission>`. No implementar `equals/hashCode` en `RolePermission` sobre campos mutables de la entidad; si se necesita identidad, usar `RolePermissionId`.
- **Esfuerzo**: S

### VQ-07 — `RestTemplate` sin timeouts en la llamada a Gemini
- **Severidad**: Baja
- **Tipo**: latente
- **Ubicación**: `utils/RestTemplateConfig.java:11`, `features/ai/service/AiService.java:294`
- **Evidencia**: `@Bean public RestTemplate restTemplate() { return new RestTemplate(); };` y `ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);`.
- **Impacto**: el `RestTemplate` por defecto usa `SimpleClientHttpRequestFactory` con timeouts infinitos. Si Gemini no responde (conexión colgada, proveedor degradado), el hilo de Tomcat queda bloqueado indefinidamente; el `catch` de `processChat` no se ejecuta hasta que falle el socket. Combinado con Q-10 (consulta sin límite), unas pocas peticiones pueden agotar el pool de 200 hilos y tumbar el resto de la API.
- **Recomendación**: construir el bean con `new RestTemplateBuilder().connectTimeout(Duration.ofSeconds(5)).readTimeout(Duration.ofSeconds(60)).build()` (o un `SimpleClientHttpRequestFactory` configurado) y propagar el timeout como 504/503.
- **Esfuerzo**: S

## Consistencia del informe

- **Conteos**: la tabla tiene 24 hallazgos y el resumen dice 1 Alta / 11 Media / 12 Baja; coinciden exactamente. Con mi recalibración (Q-07 a Baja) el consolidado quedaría 1 Alta / 10 Media / 13 Baja; Q-12 sigue Media por los 500 de Discipulado y `gender`.
- **Rutas y líneas**: todas existen. Única imprecisión menor: `configurations/SecurityService.java:25` — la llamada `findByUsername` está en la 24 y la 25 es `.orElseThrow(...)`; la cita literal abarca 24-25.
- **Duplicados**: no hay hallazgos duplicados. Q-02 y Q-19 comparten `BaptismService:87-89`, pero uno es la comparación por identidad y el otro la excepción equivocada; Q-03 y Q-12 comparten el alta de integrante, pero uno es la foto y el otro `gender`/validación.
- **Cobertura declarada**: 243 archivos Java (contados), 19 controllers (contados), 7 Specifications y 16 features; correcto. “106 archivos / 44 %” no es verificable sin el registro de lectura del agente A, pero es plausible.
- **Afirmaciones de ausencia**: “no hay lazy fuera de transacción” se sostiene tras revisar todos los usos de mappers; “no hay entidades expuestas” se sostiene (todos los controllers devuelven DTOs, `Boolean`, `Void` o `String`); “N+1 mayormente mitigado” es correcto con el matiz de `UserService.search` (sin `@EntityGraph`) y de las exportaciones sin límite (Q-15).
- **Descartados**: `registeredTo` con `T23:59:59` (confirmado en el bundle), cron `0 0 3 ? * 2#2` (probado con `CronExpression.parse` de Spring 6.2.15), entidades no expuestas, stacktrace genérico, catálogos acotados y agrupación del reporte: todos correctos.
- **Recomendaciones que hay que ajustar**: (1) Q-07 no debe aplicar `plusDays(1)` a un fin explícito a las 23:59; (2) Q-10 no debe llamar `setMaxRows` sobre el `JdbcTemplate` singleton; (3) Q-20 no debe proponer H2 sin resolver `unaccent` y el `ddl-auto=validate`; (4) Q-12 debe corregir las anotaciones **antes** de añadir `@Valid`, o el 500 pasa a ser `UnexpectedTypeException`; (5) Q-02 debe incluir `BaptismService:87`; (6) Q-16 exige tocar 9 controllers para eliminar `BaseController`.
- **Límite incompleto del borrador**: la referencia cruzada a S sobre `MemberSpecification:28-30` dice que el NPE de `MemberService:129` enmascara la fuga. Es cierto para `search`, pero **no** para `AttendanceService.export`: allí no hay NPE y el spec deja pasar todas las sedes (VQ-01). El borrador debió listarlo.
- **Referencias cruzadas que también son bugs funcionales**: la expresión inválida de `UserController:111` (`hasAuthority(org.icc.pecesatierra.utils.enums.AppPermission).DELETE_USER.name()`) no solo es de S: rompe `DELETE /api/users/{id}` para todos los usuarios, así que el consolidado debería reflejarlo como fallo funcional además de hallazgo de autorización. En la misma familia de Q-11, `BaptismController:66` declara `@RequestParam int page` sin `defaultValue` y los `updateActive` usan `@RequestParam boolean` sin valor por defecto: omitirlos o enviarlos mal termina en 500 (o en el 400 nativo si el handler no los intercepta).

## Límites de la verificación

- No se ejecutó `bootRun` ni `test` (sin `DB_URL`, `JWT_SECRET`, Cloudinary ni `GEMINI_API_KEY`). La verificación es estática más `compileJava` (`BUILD SUCCESSFUL`, tarea `UP-TO-DATE`) y pruebas aisladas en el directorio temporal, fuera del repo: deserialización de `LocalDateTime` con Jackson 2.19.4 (Q-08), `CronExpression.parse` (descartado del borrador), `MockMvc` para comprobar que un cuerpo `null` con `@RequestBody` requerido devuelve 400 (por eso `MemberController.export` no es la fuga; la de `AttendanceController.export` sí, porque es `required = false`) y un contexto Spring mínimo con H2 para comprobar que `ReportService` **sí** arranca pese al `@PersistenceContext` sobre un campo final (falso positivo potencial descartado).
- No se observó el comportamiento real de Hibernate/JPA ni de PostgreSQL: la fuga de VQ-01 se deduce del flujo `required=false → dto null → spec sin predicado`, no de una petición ejecutada; la duplicación de VQ-06 se deduce de la ausencia de `equals/hashCode` y del contrato de `persist` de Hibernate, no de un flush real; el borrado de integrantes con bautismos/usuarios/eventos (posible 500 por FK) no se pudo verificar y **no** se reporta como hallazgo.
- No se verificaron índices, constraints ni la extensión `unaccent` contra la base; las afirmaciones sobre `unique`/`nullable` se basan en las anotaciones de las entidades, como ya advierte el borrador.
- El bundle de la PWA es minificado y puede no corresponder al backend desplegado; las citas de Q-01, Q-04, Q-05, Q-07, Q-08 y Q-09 se comprobaron en los archivos presentes, pero no se puede garantizar la correspondencia con producción.
- No se leyó `audit/borrador-seguridad.md` (independencia deliberada). VQ-01 y VQ-02 se evalúan por su parte de calidad/contrato (scoping y códigos HTTP); la explotabilidad y el IDOR pertenecen al eje S.
