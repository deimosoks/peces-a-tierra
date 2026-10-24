# Borrador de auditoría — Arquitectura y calidad (Q)

**Cobertura**: inventario de los 243 archivos Java y de las 16 features (`ai`, `attendance`, `auth`, `baptism`, `branch`, `category`, `dashboard`, `discipulado`, `health`, `log`, `member`, `report`, `role`, `service`, `type`, `user`); 19/19 controllers y 7/7 Specifications revisados; lectura completa de 106 archivos Java (44 % aproximado del total), más `build.gradle`, `application.properties`, `logback.xml`, `Dockerfile`, wrapper y los bundles `main-2X72WS5U.js`, `chunk-NCMHTITX.js` y `chunk-CJDLSKNK.js` de la PWA. `compileJava` ejecutado con JDK 21 (BUILD SUCCESSFUL).

## Resumen ejecutivo

La separación controller → service → repository existe y es respetada en lo esencial, pero se rompe en los mappers: `MemberMapper` vive en `features/category/repository/mapper` y consulta tres repositorios, y `RoleMapper` ejecuta una consulta por rol. No se encontraron accesos lazy fuera de transacción ni entidades expuestas en respuestas; el N+1 está mayormente mitigado por `@EntityGraph` y `default_batch_fetch_size=50`.

Los tres riesgos principales son: (1) **pérdida silenciosa de datos** al registrar bautismos (`note` nunca se guarda y `municipality` se sobrescribe con `city`), más una comparación por identidad de `Branch` que bloquea a todos los usuarios no administradores; (2) **desalineación de contrato y zonas horarias** entre la PWA y la API: el reporte excluye el último día seleccionado, el calendario desplaza la ventana 5 horas y el filtro de discipulado usa UTC mientras la escritura usa Bogotá; (3) **errores de entrada que terminan en 500** (cuerpos opcionales desreferenciados, validaciones mal anotadas, `AuthorizationDeniedException` devuelto como 401).

El build es reproducible solo con el JDK 21 inyectado por variable de entorno (el wrapper falla con el JDK 25 del sistema) y la cobertura de pruebas es prácticamente nula (`contextLoads` únicamente), lo que explica que varios de estos bugs funcionales hayan llegado al bundle publicado.

## Tabla de hallazgos

| ID | Severidad | Título | Archivo:línea | Tipo (real/latente/deuda) |
|---|---|---|---|---|
| Q-01 | Alta | Bautismo pierde `note` y guarda `city` como `municipality` | `features/baptism/service/BaptismService.java:62-68` | real |
| Q-02 | Media | Comparación de sede por identidad bloquea bautismos de no administradores | `features/baptism/service/BaptismService.java:54` | real |
| Q-03 | Media | NPE en el `catch` de creación de integrante sin foto | `features/member/service/MemberService.java:80-82` | real |
| Q-04 | Media | `categoryLocked` se ignora al actualizar integrante | `features/category/repository/mapper/MemberMapper.java:153` | real |
| Q-05 | Media | Editar una subcategoría sin cambiar el nombre siempre devuelve 409 | `features/category/service/MemberSubCategoryService.java:74` | real |
| Q-06 | Media | Cuerpos de búsqueda opcionales desreferenciados → NPE/500 | `features/member/service/MemberService.java:129` | real |
| Q-07 | Media | El reporte excluye el último día del rango | `utils/specs/AttendanceReportSpecification.java:68` | real |
| Q-08 | Media | El calendario desplaza la ventana de fechas 5 horas | `features/service/dtos/event/ServiceEventsFilterRequestDto.java:16` | real |
| Q-09 | Media | El filtro de discipulado interpreta fechas en UTC y la escritura en Bogotá | `utils/specs/DiscipuladoSpecification.java:54` | real |
| Q-10 | Media | SQL de IA sin límite de filas ni timeout; resultado completo al prompt | `utils/sql_executor/SqlExecutor.java:19` | real |
| Q-11 | Media | Errores HTTP mal clasificados: 401 para permisos y 500 para entrada inválida | `advice/GlobalHandlerException.java:61-65` | real |
| Q-12 | Media | Validación de DTOs incorrecta o ausente | `features/discipulado/dtos/DiscipuladoRequestDto.java:18-19` | real |
| Q-13 | Baja | Doble carga del usuario (con grafo pesado) en cada petición | `configurations/SecurityService.java:25` | real |
| Q-14 | Baja | Borrado de foto usa la URL como `public_id`; I/O externo dentro de la transacción | `features/member/service/MemberService.java:203` | real |
| Q-15 | Baja | Exportaciones y listas del dashboard sin límite | `features/member/service/MemberService.java:158` | real |
| Q-16 | Baja | `BaseController` y la feature `log` son código muerto; tres formatos de respuesta | `utils/models/BaseController.java:11-35` | deuda |
| Q-17 | Baja | 65 anotaciones `@PreAuthorize` duplicadas y copias `beforeUpdate` repetidas | `features/member/controller/MemberController.java:27-35` | deuda |
| Q-18 | Baja | Log de borrado de usuario atribuye datos del actor y borra historial de roles | `features/user/service/UserService.java:298-313` | real |
| Q-19 | Baja | Excepciones de negocio reutilizadas fuera de contexto | `features/member/service/MemberNoteService.java:33-37` | real |
| Q-20 | Baja | Higiene de build: dependencia duplicada, ZIP versionado y sin pruebas | `build.gradle:22,32` | deuda |
| Q-21 | Baja | El wrapper 8.14.3 no arranca con el JDK 25 del sistema | `gradle/wrapper/gradle-wrapper.properties:3` | deuda |
| Q-22 | Baja | Mapper con acceso a repositorios y `RoleMapper` con N+1 | `features/category/repository/mapper/MemberMapper.java:36-38` | deuda |
| Q-23 | Baja | Edades y ventana del dashboard calculadas con la zona del servidor | `features/dashboard/service/DashboardService.java:44-45` | real |
| Q-24 | Baja | Lote de asistencias asigna todos los registros al primer evento | `features/attendance/service/AttendanceService.java:58` | latente |

## Detalle por hallazgo

### Q-01 — Bautismo pierde `note` y guarda `city` como `municipality`
- **Severidad**: Alta
- **Tipo**: real
- **Ubicación**: `features/baptism/service/BaptismService.java:62-68`
- **Evidencia**: el builder llama `.neighborhood(baptismRequestDto.getNote())` (línea 62) y luego `.neighborhood(baptismRequestDto.getNeighborhood())` (línea 66), con lo que el primer valor se descarta; además `.municipality(baptismRequestDto.getCity())` (línea 68). La entidad sí tiene el campo: `private String note;` (`features/baptism/Baptism.java:31`). La PWA envía ambos: `r.note&&e.append("note",r.note)` y `r.municipality&&e.append("municipality",r.municipality)` (`src/main/resources/static/main-2X72WS5U.js`, minificado).
- **Impacto**: cuando el usuario escribe una nota, el bautismo la guarda como `null`; además, `municipality` queda siempre con el valor de `city`. Los datos que el usuario captura se pierden sin error visible y no se pueden recuperar; los filtros por municipio quedan sesgados.
- **Recomendación**: reemplazar por `.note(baptismRequestDto.getNote())` y `.municipality(baptismRequestDto.getMunicipality())`; añadir una prueba de persistencia que compare request y response.
- **Esfuerzo**: S

### Q-02 — Comparación de sede por identidad bloquea bautismos de no administradores
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/baptism/service/BaptismService.java:54` (mismo patrón en `:87`)
- **Evidencia**: `if (!user.hasAuthority("ADMINISTRATOR") && !member.getBranch().equals(user.getMember().getBranch())) {`
- **Impacto**: `Branch` no define `equals`/`hashCode` (`features/branch/Branch.java`), por lo que se compara identidad de objeto. El usuario autenticado se carga en la transacción del filtro JWT y queda desacoplado (`CustomUserDetailsServiceImpl` es `@Transactional(readOnly = true)`), mientras que `memberRepository.findById` carga otra instancia en la transacción del servicio. El resultado es `false` incluso para la sede propia: **ningún usuario no administrador puede crear ni invalidar bautismos**; recibe 409 "fuera de su sede".
- **Recomendación**: comparar identificadores (`member.getBranch().getId().equals(user.getMember().getBranch().getId())`) o normalizar la comparación en un helper; no depender de `equals` de entidades.
- **Esfuerzo**: S

### Q-03 — NPE en el `catch` de creación de integrante sin foto
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/member/service/MemberService.java:80-82`
- **Evidencia**: `Map<String, String> pictureData = null;` (línea 54) y luego `} catch (ApiException e) { if (pictureData.get("publicId") != null) {` (líneas 80-81).
- **Impacto**: si se crea un integrante sin foto y el servicio de persistencia lanza una excepción de negocio (categoría/tipo/subcategoría inexistente, regla de subcategoría), el `catch` lanza `NullPointerException` y `GlobalHandlerException` responde 500 "Ha ocurrido un error inesperado" en lugar del 404/409 original. El cliente pierde el mensaje accionable y el equipo no distingue el error real.
- **Recomendación**: `if (pictureData != null && pictureData.get("publicId") != null)`; limpiar también ante excepciones no `ApiException` para no dejar fotos huérfanas.
- **Esfuerzo**: S

### Q-04 — `categoryLocked` se ignora al actualizar integrante
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/category/repository/mapper/MemberMapper.java:153`
- **Evidencia**: `member.setCategoryLocked(member.isCategoryLocked());` (autoasignación; el DTO sí tiene el campo `private boolean categoryLocked;` en `features/member/dtos/MemberRequestDto.java:27`).
- **Impacto**: el formulario de edición de la PWA envía `categoryLocked` (`integranteForm.patchValue({...,categoryLocked:r.categoryLocked||!1})`), pero el backend nunca aplica el cambio. El usuario no puede bloquear ni desbloquear la categoría desde la edición; el job nocturno de reglas seguirá reasignándola.
- **Recomendación**: `member.setCategoryLocked(memberRequestDto.isCategoryLocked());` y cubrirlo con prueba de actualización.
- **Esfuerzo**: S

### Q-05 — Editar una subcategoría sin cambiar el nombre siempre devuelve 409
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/category/service/MemberSubCategoryService.java:74`
- **Evidencia**: `if (memberSubCategoryRepository.existsByCategoryAndName(memberSubCategory.getCategory(), memberSubCategoryRequestDto.getName())) {` — la consulta derivada incluye la propia fila. La PWA envía `{name:e.name,color:e.color,categoryId:r}` al editar (`main-2X72WS5U.js`), es decir, el mismo nombre si solo se cambia el color.
- **Impacto**: cualquier edición de subcategoría que no cambie el nombre responde 409 "ya existe una sub-categoría con ese nombre"; la UI muestra "Error al guardar sub-categoría". En el mismo eje, `MemberCategoryService.update` (`:41-69`) no verifica duplicados: si la base aplica el `unique = true` declarado en `MemberCategory.name`, el intento termina en una violación de constraint (500 genérico) en lugar de un 409 explicable.
- **Recomendación**: usar `existsByCategoryAndNameAndIdNot(...)` en subcategorías y añadir la comprobación equivalente en `MemberCategoryService.update`.
- **Esfuerzo**: S

### Q-06 — Cuerpos de búsqueda opcionales desreferenciados → NPE/500
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/member/service/MemberService.java:129` (también `features/attendance/service/AttendanceService.java:173`, `utils/specs/UserSpecification.java:22`, `features/service/service/ServiceEventService.java:138`)
- **Evidencia**: el controller declara `@RequestBody(required = false) MemberFilterRequestDto memberFilterRequestDto` (`features/member/controller/MemberController.java:70-72`) y el servicio hace `if (dto.getOrderBy() != null) {`. En `ServiceEventService` el controller también permite cuerpo nulo (`ServiceEventController.java:67-69`) y el servicio abre con `if (dto.getStartDate() == null || dto.getEndDate() == null) {`.
- **Impacto**: `POST /api/members/search`, `POST /api/attendances/search`, `POST /api/users/search` y `POST /api/services-events/calendar` sin cuerpo devuelven 500 por `NullPointerException` en vez de una lista vacía/filtros por defecto o un 400. El contrato declara el cuerpo opcional, así que es una entrada válida.
- **Recomendación**: normalizar el DTO nulo en el servicio (`dto == null ? new XFilterRequestDto() : dto`) y conservar el filtro de sede; o marcar el cuerpo como obligatorio (`required = true`). Validar también `page >= 0` (`PageRequest.of` con página negativa lanza `IllegalArgumentException` → 500).
- **Esfuerzo**: S

### Q-07 — El reporte excluye el último día del rango
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `utils/specs/AttendanceReportSpecification.java:68`
- **Evidencia**: `cb.lessThanOrEqualTo(serviceDateTime, dateTimeUtils.toUTC(dto.getEndDate()))`. La PWA usa un input `type="datetime-local"` y normaliza el valor con la función `e(n)`: si no trae hora le añade `T00:00:00` (`main-2X72WS5U.js`), por lo que un día seleccionado llega como medianoche.
- **Impacto**: al filtrar, por ejemplo, del 1 al 23 de octubre, solo entran los eventos del 23 que empiecen exactamente a las 00:00. El servicio del domingo (o del día seleccionado como fin) no aparece; los totales del reporte quedan cortos.
- **Recomendación**: interpretar `endDate` como fin de día (`endDate.plusDays(1)` exclusivo) o normalizar en el DTO; alinear el contrato con la UI y documentarlo.
- **Esfuerzo**: S

### Q-08 — El calendario desplaza la ventana de fechas 5 horas
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/service/dtos/event/ServiceEventsFilterRequestDto.java:16-17` y `utils/specs/ServiceEventSpecification.java:34-40`
- **Evidencia**: el DTO declara `private LocalDateTime startDate;` / `private LocalDateTime endDate;`; el spec aplica `dateTimeUtils.toUTC(dto.getStartDate())`. La PWA envía `{startDate:e.toISOString(),endDate:t.toISOString()}` (`static/chunk-NCMHTITX.js`, minificado), es decir, un instante UTC con `Z`. Se verificó con Jackson 2.19.4 (el que resuelve Boot 3.5) que `2026-10-01T05:00:00.000Z` se deserializa como `LocalDateTime` `2026-10-01T05:00` descartando el offset; después `toUTC` vuelve a sumar el offset de Bogotá.
- **Impacto**: la ventana consultada queda desplazada +5 h. Se pierden los eventos entre 00:00 y 05:00 del primer día visible del calendario y se incluyen eventos hasta 5 h después del último día (que la UI no mapea). El calendario muestra huecos o eventos fuera de rango sin error.
- **Recomendación**: usar `OffsetDateTime`/`Instant` en el DTO y convertir una sola vez, o enviar desde la PWA fecha local sin offset (`YYYY-MM-DDTHH:mm:ss`) y mantener `LocalDateTime` + `toUTC`.
- **Esfuerzo**: M

### Q-09 — El filtro de discipulado interpreta fechas en UTC y la escritura en Bogotá
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `utils/specs/DiscipuladoSpecification.java:51-66` frente a `features/discipulado/service/DiscipuladoService.java:51`
- **Evidencia**: al escribir, `dateStarted(dateTimeUtils.toUTC(dto.getDateStarted()))` (interpreta la entrada como `America/Bogota`); al filtrar, `dto.getDateStartedFrom().atOffset(java.time.ZoneOffset.UTC)` y `dto.getDateStartedTo().atOffset(java.time.ZoneOffset.UTC)`. La PWA envía `dateStartedFrom` con `T00:00:00` y `dateStartedTo` con `T23:59:59` sobre la fecha local.
- **Impacto**: el límite superior se compara 5 h antes de lo que se guardó. Un discipulado creado a las 20:00 de Bogotá del último día del filtro se almacena como 01:00 UTC del día siguiente y desaparece de la búsqueda; el usuario ve menos registros de los que hay.
- **Recomendación**: usar `dateTimeUtils.toUTC(...)` también en el spec (o `OffsetDateTime` de extremo a extremo); añadir prueba con un registro creado después de las 19:00.
- **Esfuerzo**: S

### Q-10 — SQL de IA sin límite de filas ni timeout; resultado completo al prompt
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `utils/sql_executor/SqlExecutor.java:19` y `features/ai/service/AiService.java:57,256`
- **Evidencia**: `return jdbcTemplate.queryForList(sql);` sin `setMaxRows` ni `setQueryTimeout`; después `buildFormattingUserPrompt(...)` hace `prompt.append("DATOS DE LA BASE DE DATOS: ").append(data.toString())`.
- **Impacto**: una consulta generada sobre `attendance` o `members` puede traer toda la tabla a heap, convertirla a `String` y enviarla a Gemini. Con el crecimiento de la congregación esto produce latencia, errores 429 del proveedor u `OutOfMemoryError`; el hilo queda ocupado sin límite. (La validación de la SQL pertenece al eje S; aquí se reporta el consumo de recursos.)
- **Recomendación**: `jdbcTemplate.setMaxRows(...)` y `setQueryTimeout(...)`; truncar el resultado antes de construir el prompt y devolver al usuario una advertencia de "resultado parcial".
- **Esfuerzo**: M

### Q-11 — Errores HTTP mal clasificados: 401 para permisos y 500 para entrada inválida
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `advice/GlobalHandlerException.java:61-65` (y `:49-52`)
- **Evidencia**: `@ExceptionHandler(AuthorizationDeniedException.class)` devuelve `buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.", request.getRequestURI())`. Cualquier otra excepción, incluidas `HttpMessageNotReadableException` (enum/fecha inválida) y `MethodArgumentTypeMismatchException` (`?active=abc`), cae en `@ExceptionHandler(Exception.class)` con 500.
- **Impacto**: un usuario autenticado sin permiso recibe "Credenciales invalidas" y 401; el interceptor de la PWA trata el 401 como token expirado e intenta refrescar (y el 403 como logout), con lo que el síntoma visible es un cierre de sesión o un mensaje engañoso. Una petición mal formada se reporta como fallo del servidor, dificultando el soporte.
- **Recomendación**: devolver 403 con mensaje de permiso; añadir handlers para `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` y `MissingServletRequestParameterException` con 400 y el detalle del campo.
- **Esfuerzo**: S

### Q-12 — Validación de DTOs incorrecta o ausente
- **Severidad**: Media
- **Tipo**: real
- **Ubicación**: `features/discipulado/dtos/DiscipuladoRequestDto.java:18-19`, `utils/models/OrderBy.java:14-15`, `features/role/dtos/PermissionRequestDto.java:14-15`, `features/discipulado/controller/DiscipuladoController.java:31`
- **Evidencia**: `@NotBlank private LocalDateTime dateStarted;`, `@NotBlank private boolean asc;`, `@NotBlank private AppPermission name;` y el controller `create(@RequestBody DiscipuladoRequestDto dto, ...)` sin `@Valid`. Además, `MemberPersistenceService.java:57` hace `gender(dto.getGender().toString())` sin validar `gender`.
- **Impacto**: las restricciones nunca se ejecutan (no hay `@Valid` ni cascada en los DTOs anidados); si se activaran, Hibernate Validator lanzaría `UnexpectedTypeException` por aplicar `@NotBlank` a tipos no textuales. `POST /api/discipulado` sin `dateStarted` llega a una columna `nullable = false` y responde 500; crear un integrante sin `gender` produce NPE en la línea 57.
- **Recomendación**: `@NotNull` para fechas y enums, eliminar `@NotBlank` de `boolean`, añadir `@Valid` en los controllers y `@Valid`/`@NotNull` en los DTOs anidados; validar `gender` explícitamente.
- **Esfuerzo**: S/M

### Q-13 — Doble carga del usuario (con grafo pesado) en cada petición
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `configurations/SecurityService.java:25`
- **Evidencia**: `User user = userRepository.findByUsername(authentication.getName())` dentro de `@Transactional(readOnly = true)`, ejecutado por cada `@PreAuthorize`; el filtro ya hizo `UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);` (`configurations/JwtAuthFilter.java:69`). La consulta usa `@EntityGraph(attributePaths = {"member","member.branch","roles","roles.role","roles.role.permissions.id.permission"})` (`features/user/repository/UserRepository.java:11-17`).
- **Impacto**: cada endpoint autenticado ejecuta dos veces la misma consulta pesada (asociaciones y colecciones), además de la consulta del caso de uso. Es carga evitable que crece con usuarios y roles; `AuthService.login` también carga el usuario dos veces (`AuthService.java:37` y `:44`).
- **Recomendación**: en `SecurityService.isActive`, leer `((User) authentication.getPrincipal()).isActive()` (el principal ya está cargado) o usar una consulta `exists`/proyección ligera; reutilizar el `UserDetails` del login.
- **Esfuerzo**: S

### Q-14 — Borrado de foto usa la URL como `public_id`; I/O externo dentro de la transacción
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `features/member/service/MemberService.java:203`
- **Evidencia**: `String pictureUrl = member.getPictureProfileUrl();` (línea 180) y luego `if (pictureUrl != null) pictureUtils.delete(pictureUrl);` (línea 203), mientras la firma es `public void delete(String publicId)` (`utils/cloudinary/PictureUtils.java:47`) y termina en `cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap())` (`utils/cloudinary/CloudinaryService.java:43`). El `public_id` correcto está disponible en `member.getPublicId()` (se guarda en `MemberPersistenceService.java:104`).
- **Impacto**: Cloudinary recibe una URL donde espera un `public_id`, responde "not found" y el resultado se ignora; la imagen queda huérfana y sigue consumiendo cuota. Además, el `destroy` HTTP se ejecuta dentro de `@Transactional` (`MemberService.java:168`), manteniendo la conexión y el bloqueo mientras dura la llamada externa.
- **Recomendación**: borrar con `member.getPublicId()` y hacerlo después del commit (`TransactionSynchronization` o evento), no dentro de la transacción; registrar el resultado de Cloudinary.
- **Esfuerzo**: S

### Q-15 — Exportaciones y listas del dashboard sin límite
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `features/member/service/MemberService.java:158`
- **Evidencia**: `List<Member> members = memberRepository.findAll(spec);` (sin paginación) y el mismo patrón en `AttendanceService.java:202`; en el dashboard, `memberRepository.findMembersWithBirthdayInMonth(...)` y `findLatestMembers(...)` (`DashboardService.java:53-58`) devuelven listas completas que se serializan con `MemberResponseDto`.
- **Impacto**: el tiempo y la memoria de exportación/dashboard crecen linealmente con el histórico. Con miles de integrantes y años de asistencias, el endpoint de export puede agotar heap o superar el timeout del proxy; el dashboard se vuelve lento para todos.
- **Recomendación**: exportar por streaming o lotes con proyección mínima; limitar las listas del dashboard con `Pageable`/`LIMIT` (por ejemplo, últimos 20) y paginar el resto.
- **Esfuerzo**: M

### Q-16 — `BaseController` y la feature `log` son código muerto; tres formatos de respuesta
- **Severidad**: Baja
- **Tipo**: deuda
- **Ubicación**: `utils/models/BaseController.java:11-35`
- **Evidencia**: `public abstract class BaseController {` con `success`, `created`, `error` y `noContent`; 9 controllers lo extienden, pero ninguno de esos métodos tiene llamadas (búsqueda global: 0 usos). La feature `log` solo contiene la entidad `features/log/Log.java:16` (con `Timestamp timesTamp`), sin repositorio, servicio ni controller. Coexisten respuestas DTO crudas, `PagesResponseDto`/`ExportResponseDto` y `ErrorResponseDto`.
- **Impacto**: el envelope `ApiResponse` no se aplica nunca, pero su existencia sugiere lo contrario; el cliente debe manejar tres formas. La tabla/feature de log no registra nada, así que la auditoría depende de `log.info`, y su entidad usa `java.sql.Timestamp` en vez del `OffsetDateTime` del resto.
- **Recomendación**: eliminar `BaseController` y la feature `log` no usada, o completar el log de auditoría y adoptar un único contrato de respuesta en un cambio coordinado con el frontend.
- **Esfuerzo**: S/M

### Q-17 — 65 anotaciones `@PreAuthorize` duplicadas y copias `beforeUpdate` repetidas
- **Severidad**: Baja
- **Tipo**: deuda
- **Ubicación**: `features/member/controller/MemberController.java:27-35` (patrón repetido)
- **Evidencia**: el mismo text block con `hasAuthority(T(...AppPermission).X.name()) || hasAuthority(...ADMINISTRATOR...) && @securityService.isActive(authentication)` aparece 65 veces repartido entre 18 controllers; por ejemplo `MemberController.java:27-35`, `:42-49`, `:57-68`, `:81-89`. Además, 8 servicios reconstruyen una entidad `beforeUpdate` casi idéntica (`MemberPersistenceService.java:147-158`, `BranchService.java:67-74`, `ServiceService.java:64-71`, etc.).
- **Impacto**: cualquier cambio de permisos exige editar decenas de cadenas; la duplicación ya produjo al menos una expresión inválida (ver referencia cruzada a S). Los logs de "estado anterior" duplican código y son un punto de divergencia.
- **Recomendación**: extraer meta-anotaciones o un `PermissionEvaluator` central, generar el `beforeUpdate` con un mapper/`copy` y cubrirlo con pruebas de autorización.
- **Esfuerzo**: L

### Q-18 — Log de borrado de usuario atribuye datos del actor y borra historial de roles
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `features/user/service/UserService.java:298-313` (y `:196-212`)
- **Evidencia**: el log etiqueta `Username`, `Integrante` y `Activo` del usuario eliminado pero recibe `user.getUsername()` (línea 308), `user.getMember().getId()` (309) y `user.isActive()` (310), que son del actor; solo `userToDelete.getId()` y `userToDelete.getRoles()` son del objetivo. En `update`, `user.getRoles().clear();` y se recrean todos los `UserRole` con `givenDate(dateTimeUtils.nowUTC())` (196-212).
- **Impacto**: la traza de auditoría dice que se eliminó al usuario equivocado (nombre/integrante/estado del administrador). Al editar un usuario sin cambiar roles, se pierde `giverId`/`givenDate` originales de cada rol, destruyendo el historial de quién otorgó qué y cuándo.
- **Recomendación**: usar `userToDelete.getUsername()`, `userToDelete.getMember().getId()` y `userToDelete.isActive()`; comparar roles y solo reemplazar los que cambian, conservando la metadata de los existentes.
- **Esfuerzo**: S

### Q-19 — Excepciones de negocio reutilizadas fuera de contexto
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `features/member/service/MemberNoteService.java:33-37` (también `MemberService.java:211-212`, `BaptismService.java:87-89`)
- **Evidencia**: `memberRepository.findById(memberNoteRequestDto.getMemberId()).orElseThrow(NoteNotFoundException::new)` (debería ser `MemberNotFoundException`); en `updateActive`, `throw new CannotDeleteMemberOutSideYourBranchException();`; en `invalid` de bautismo, `throw new CannotCreateMembersOutsideYourBranch();`.
- **Impacto**: mensajes y códigos incorrectos para el usuario: "Nota no encontrada" cuando falta el integrante, "No puede eliminar integrantes..." al desactivar/actualizar, y "No puede crear..." al invalidar. El cliente no puede distinguir la causa y el soporte se complica.
- **Recomendación**: crear/ usar una excepción por operación (`CannotUpdateMemberOutsideYourBranch`, `CannotInvalidateBaptismOutsideYourBranch`, etc.), con mensajes en español coherentes.
- **Esfuerzo**: S

### Q-20 — Higiene de build: dependencia duplicada, ZIP versionado y sin pruebas
- **Severidad**: Baja
- **Tipo**: deuda
- **Ubicación**: `build.gradle:22,32`; `src (2).zip`; `src/test/java/org/icc/pecesatierra/PecesATierraApplicationTests.java:6-11`
- **Evidencia**: `implementation 'org.springframework.boot:spring-boot-starter-web'` aparece dos veces (líneas 22 y 32); `git ls-files` incluye `src (2).zip` en la raíz; el único test es `@SpringBootTest class PecesATierraApplicationTests { @Test void contextLoads() {} }`.
- **Impacto**: la duplicación no rompe el build pero confunde el mantenimiento; el ZIP versionado duplica la fuente de verdad y engorda el clon. Sin pruebas, los bugs de Q-01 a Q-09 no tienen red de seguridad, y `contextLoads` ni siquiera puede ejecutarse sin una base de datos y todas las variables de entorno.
- **Recomendación**: eliminar la dependencia duplicada y el ZIP; añadir pruebas unitarias de mappers/servicios y de integración con Testcontainers (o perfil H2) para que `test` sea ejecutable en CI.
- **Esfuerzo**: M

### Q-21 — El wrapper 8.14.3 no arranca con el JDK 25 del sistema
- **Severidad**: Baja
- **Tipo**: deuda
- **Ubicación**: `gradle/wrapper/gradle-wrapper.properties:3`
- **Evidencia**: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip`. Con el JDK 25 del sistema, `.\gradlew.bat compileJava` falla con `BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' Unsupported class file major version 69` (reproducido); con `JAVA_HOME` apuntando a JDK 21 el build es `BUILD SUCCESSFUL`. El bloque `java { toolchain { languageVersion = 21 } }` no aplica al JVM que ejecuta Gradle.
- **Impacto**: un desarrollador nuevo no puede compilar sin descubrir y exportar `JAVA_HOME`; no hay `.java-version`, `org.gradle.java.home` ni CI que fije la versión. El README/AGENTS lo documenta, pero el fallo no es autoexplicativo.
- **Recomendación**: actualizar Gradle a una versión compatible con JDK 25 (o fijar el JVM de Gradle en `gradle.properties`), y añadir la comprobación a CI/README.
- **Esfuerzo**: S

### Q-22 — Mapper con acceso a repositorios y `RoleMapper` con N+1
- **Severidad**: Baja
- **Tipo**: deuda
- **Ubicación**: `features/category/repository/mapper/MemberMapper.java:36-38` (y `:1`)
- **Evidencia**: el paquete es `org.icc.pecesatierra.features.category.repository.mapper` y el mapper inyecta `MemberCategoryRepository`, `MemberTypeRepository` y `MemberSubCategoryRepository`, consultándolos en `updateEntityFromDto` (`:117-137`); además `RoleMapper.toDto` ejecuta `.totalUsers(userRoleRepository.countByRoleId(role.getId()))` (`features/role/mapper/RoleMapper.java:33`).
- **Impacto**: la capa de mapeo depende de persistencia y del feature `category`, así que no es reutilizable ni testeable sin contexto; `RoleService.findAll` dispara una consulta `count` por cada rol (N+1 que no cubre el batch fetch). Cualquier cambio de esquema obliga a tocar mappers de otro dominio.
- **Recomendación**: mover `MemberMapper`/`MemberNotesMapper`/`MemberTypeMapper` a `features/member/mapper` y `features/type/mapper`; resolver las entidades en el servicio antes de mapear y calcular `totalUsers` con una consulta agregada.
- **Esfuerzo**: M

### Q-23 — Edades y ventana del dashboard calculadas con la zona del servidor
- **Severidad**: Baja
- **Tipo**: real
- **Ubicación**: `features/dashboard/service/DashboardService.java:44-45`
- **Evidencia**: `.startDate(LocalDateTime.now().minusDays(7))` y `.endDate(LocalDateTime.now())`; después `AttendanceReportSpecification` aplica `dateTimeUtils.toUTC(...)`, que asume que esa hora es de Bogotá. El mismo patrón aparece en `MemberMapper.java:74` (`ChronoUnit.YEARS.between(member.getBirthdate(), LocalDateTime.now())`), `MemberSpecification.java:102` (`LocalDate today = LocalDate.now();`) y `MemberCategoryDeterminer.java:34`.
- **Impacto**: el contenedor `eclipse-temurin:21-jre` corre por defecto en UTC, así que la ventana "última semana" se interpreta como si `now()` fuera hora de Bogotá: se desplaza 5 h y su extremo superior cae 5 h en el futuro. Las edades y el filtro de edad cambian de día a las 19:00 de Bogotá, produciendo resultados distintos a los que ve el usuario en su calendario.
- **Recomendación**: usar `dateTimeUtils.nowColombia()` para calcular la ventana y `nowColombia().toLocalDate()` para edades/filtros; reservar `nowUTC()` para escrituras.
- **Esfuerzo**: S

### Q-24 — Lote de asistencias asigna todos los registros al primer evento
- **Severidad**: Baja
- **Tipo**: latente
- **Ubicación**: `features/attendance/service/AttendanceService.java:58` (y `:106`)
- **Evidencia**: `ServiceEvent event = serviceEventRepository.findById(attendances.getFirst().getServiceEventId())` y dentro del bucle `.serviceEvent(event)` para cada `AttendanceRequestDto`, sin comprobar que los demás elementos tengan el mismo `serviceEventId`.
- **Impacto**: la API acepta una lista y asigna todas las asistencias al evento del primer elemento; si un cliente o una importación envía eventos mezclados, las asistencias quedan registradas en el evento equivocado (corrupción del historial y de los reportes). La PWA actual envía un solo `selectedEventId`, por lo que hoy es latente.
- **Recomendación**: agrupar por `serviceEventId` y procesar cada grupo, o rechazar con 400 los lotes con más de un evento; documentar la restricción en el contrato.
- **Esfuerzo**: S

## Descartados (falsos positivos o mitigados)

| Qué parecía | Por qué no es hallazgo | Evidencia |
|---|---|---|
| N+1 general en búsquedas y mapeos de integrantes/asistencias/bautismos | Las consultas paginadas sobreescriben `findAll` con `@EntityGraph` y Hibernate tiene `default_batch_fetch_size=50`; las colecciones lazy se cargan por lotes dentro de la transacción. Queda el caso puntual de `RoleMapper` (Q-22) y las exportaciones sin límite (Q-15). | `MemberRepository.java:21-29`, `AttendanceRepository.java:21-32`, `BaptismRepository.java:18-25`, `application.properties:18` |
| `LazyInitializationException` por `open-in-view=false` | No se encontró ningún mapeo DTO fuera de `@Transactional`: las lecturas declaran `readOnly = true` y los mappers se invocan dentro de ellas; el principal trae `member`/`branch` EAGER o en el grafo. | `MemberService.java:122,154`, `DashboardService.java:35`, `UserRepository.java:11-17`, `application.properties:10` |
| El rango `registeredTo` de integrantes excluye el último día | La PWA compensa el fin de día y envía `T23:59:59`; el `toUTC` del backend lo interpreta correctamente como Bogotá. | `main-2X72WS5U.js` (`registeredTo:...T23:59:59`) frente a `MemberSpecification.java:108-110` |
| El cron `0 0 3 ? * 2#2` de limpieza de tokens es inválido | Se verificó con Spring 6.2.15: `CronExpression.parse` acepta `2#2` (segundo martes). | `utils/schedulings/Scheduling.java:33`; prueba local con `spring-context-6.2.15` |
| Entidades JPA expuestas en respuestas | Todos los controllers devuelven DTOs (`MemberResponseDto`, `AttendanceResponseDto`, etc.); no se encontró ninguna entidad en un `ResponseEntity`. | `MemberController.java:37-123`, `AttendanceController.java:38-99` |
| Stacktrace filtrado al cliente | `handleAllExceptions` responde el texto genérico "Ha ocurrido un error inesperado"; el stacktrace va a `System.err` (logging mejorable, ver Q-11). | `GlobalHandlerException.java:49-53` |
| `findAll()` sin paginar en catálogos | `branches`, `services`, `member-types`, `member-categories` y `roles` son catálogos acotados; el costo es aceptable y no se reporta como bug. | `BranchService.java:135-141`, `ServiceService.java:133-139`, `RoleService.java:153-156` |
| El reporte agrupa por fecha UTC y podría duplicar días | `ReportService` agrupa además por `serviceDateTime` exacto y reescribe `date` con la fecha de Bogotá (`toLocalDate`), por lo que la etiqueta final es correcta; el problema real es el límite superior (Q-07). | `ReportService.java:90-98,148-156` |
| La SQL generada por IA es inyectable | Pertenece al eje de seguridad (validación de SQL); en este eje solo se reporta el consumo de recursos (Q-10). No se analiza aquí. | `configurations/SqlQueryValidator.java`, `features/ai/service/AiService.java` |
| `SecurityService` anotado con `@Configuration` en vez de `@Service` | Es un bean válido y `@Transactional` funciona a través del proxy; no se observó fallo. Es estilo, no bug. | `configurations/SecurityService.java:20-22` |

## Límites de la revisión

- **Sin ejecución contra base de datos**: `bootRun` y `test` requieren `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, credenciales de Cloudinary y `GEMINI_API_KEY`, que no están disponibles. La verificación fue estática más `compileJava` con JDK 21; no se observó el comportamiento real de Hibernate/JPA.
- **Esquema no versionado**: no hay migraciones, `schema.sql` ni datos semilla; `ddl-auto=validate`. No se pudieron verificar índices, constraints ni la extensión `unaccent` contra la base real. Las afirmaciones sobre columnas (`nullable`, tipos) se basan en las anotaciones de las entidades.
- **Frontend minificado**: `src/main/resources/static/` es build de Angular sin fuentes; las rutas y payloads se citan del bundle (`main-2X72WS5U.js`, `chunk-NCMHTITX.js`, línea 1 por estar minificados). No se puede garantizar que el bundle corresponda exactamente al backend desplegado.
- **Comportamiento externo no ejecutado**: la semántica de Cloudinary (`destroy` con URL) y los tiempos de Gemini se infieren de su API; no se hicieron llamadas reales.
- **Flush de Hibernate no verificado**: el posible conflicto al limpiar y recrear `UserRole`/`RolePermission` con la misma clave compuesta (`UserService.java:196-212`) no se pudo reproducir sin base de datos; por eso se reporta solo la pérdida de metadata verificable (Q-18), no un fallo de flush.
- **Referencias cruzadas al eje S (no analizadas)**: (1) `features/user/controller/UserController.java:111` contiene una expresión `@PreAuthorize` con sintaxis inválida (`hasAuthority(org.icc...AppPermission).DELETE_USER.name()`), lo que afecta al endpoint de borrado de usuarios; (2) `utils/specs/MemberSpecification.java:28-30` retorna `cb.conjunction()` antes del filtro de sede cuando `dto == null`; hoy el NPE de `MemberService.java:129` lo enmascara, pero una corrección ingenua de Q-06 expondría datos de otras sedes. Ambos corresponden a Seguridad.
- **Frontera de ejes**: no se analizaron autenticación/JWT, autorización/permisos, IDOR, inyección SQL, secretos, CORS/CSRF, uploads ni PII en logs; solo se referencian cuando el síntoma es de calidad (por ejemplo, el código HTTP de Q-11).
