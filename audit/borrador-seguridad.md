# Borrador de auditoría — Seguridad

## Resumen ejecutivo

La API tiene una cobertura de guardas alta (69 handlers, 65 con `@PreAuthorize` y `isActive`, 0 endpoints públicos accidentales), pero la autorización multi-sede está implementada de forma ad hoc y falla en varios caminos concretos. El riesgo mayor es el endpoint de IA: ejecuta SQL generado por el LLM sobre cualquier tabla del esquema, sin filtro de sede y con un validador de lista negra trivialmente evadible, lo que permite leer `users.password_hash` y PII de todas las sedes (S-01, Crítica). Le siguen cinco hallazgos altos: exportación de asistencias sin filtro de sede cuando falta el cuerpo (S-02), IDOR de escritura en el progreso de discipulado (S-03), escalado a ADMINISTRATOR por asignación libre de roles/permisos (S-04), reseteo de la contraseña de un administrador por un usuario con `UPDATE_USER` (S-05) y registro de asistencias cruzando sedes (S-06). En el plano de sesión, el refresh token se guarda en claro en BD y en `localStorage`, vive 5 días y no se revoca al cambiar la contraseña; no hay rate limiting ni bloqueo en login y los errores permiten enumerar usuarios. También hay PII y notas pastorales en logs INFO, CORS comodín con credenciales y una guarda `@PreAuthorize` sintácticamente rota en `DELETE /api/users/{id}` que inutiliza el endpoint. No se encontraron secretos hardcodeados ni CVEs aplicables verificadas en las dependencias declaradas.

## Tabla de hallazgos

| ID | Severidad | Título | Archivo:línea | Tipo (real/latente/deuda) |
|---|---|---|---|---|
| S-01 | Crítica | SQL libre del LLM ejecutado sobre todo el esquema y todas las sedes | `features/ai/service/AiService.java:51-53` | real |
| S-02 | Alta | Exportación de asistencias sin filtro de sede si falta el cuerpo | `utils/specs/AttendanceSpecification.java:37` | real |
| S-03 | Alta | IDOR entre sedes en el progreso de discipulado | `features/discipulado/service/DiscipuladoService.java:132-158` | real |
| S-04 | Alta | Escalado de privilegios por asignación arbitraria de roles y permisos | `features/user/service/UserService.java:75-89` | latente |
| S-05 | Alta | `UPDATE_USER` permite resetear la contraseña de un admin y mover cuentas de sede | `features/user/service/UserService.java:183-194` | latente |
| S-06 | Alta | Registro de asistencias sin validar la sede del evento ni de los integrantes | `features/attendance/service/AttendanceService.java:58-68` | real |
| S-07 | Media | PII completa y notas pastorales expuestas a roles operativos | `features/member/controller/MemberController.java:57-68` | real |
| S-08 | Media | CORS con cualquier origen y `allowCredentials(true)` | `configurations/SecurityConfig.java:83-89` | real |
| S-09 | Media | Access y refresh token en `localStorage`; refresh de 5 días sin CSP | `src/main/resources/static/chunk-CJDLSKNK.js:5` | real |
| S-10 | Media | Ciclo de vida de tokens: refresh en claro, sin revocación, sin `iss/aud/jti` | `features/auth/RefreshToken.java:21-22` | real |
| S-11 | Media | Sin rate limiting ni bloqueo de cuenta en login | `features/auth/controller/AuthController.java:27-30` | real |
| S-12 | Media | Enumeración de usuarios por mensajes de error distintos en login | `features/auth/service/AuthService.java:37-50` | real |
| S-13 | Media | PII, notas y posible clave de Gemini en logs | `features/member/service/MemberPersistenceService.java:112-136` | real |
| S-14 | Media | Resultados con PII enviados a Gemini en cada consulta | `features/ai/service/AiService.java:253-258` | real |
| S-15 | Media | `@PreAuthorize` malformado en `DELETE /api/users/{id}` (500) | `features/user/controller/UserController.java:109-117` | real |
| S-16 | Media | `PUT/DELETE` de sedes sin comprobar la sede del usuario | `features/branch/service/BranchService.java:63-65` | latente |
| S-17 | Media | Comparación por identidad de `Branch` en bautismos (deniega a no-admin) | `features/baptism/service/BaptismService.java:54` | real |
| S-18 | Baja | Precedencia de operadores: admin elude `isActive` al borrar servicios | `features/service/controller/ServiceController.java:55-59` | real |
| S-19 | Baja | `logout` y cambio de contraseña sin `isActive` | `features/auth/controller/AuthController.java:38-49` | real |
| S-20 | Baja | Errores JWT no controlados: firma inválida devuelve 500 | `configurations/JwtAuthFilter.java:52-64` | real |
| S-21 | Baja | Catálogos y roles listables sin permiso específico | `features/type/controller/MemberTypeController.java:58-61` | real |
| S-22 | Baja | Higiene de dependencias y ausencia de SCA | `build.gradle:3` | deuda |

## Detalle por hallazgo

### S-01 — SQL libre del LLM ejecutado sobre todo el esquema y todas las sedes
- **Severidad**: Crítica
- **Ubicación**: `features/ai/controller/AiController.java:29`, `features/ai/service/AiService.java:51-53`, `configurations/SqlQueryValidator.java:12-55`, `utils/sql_executor/SqlExecutor.java:17-19`
- **Evidencia**:
  - `return ResponseEntity.ok(aiService.processChat(request, new User()));`
  - `if (sql != null && sqlQueryValidator.isSafeSelect(sql)) { List<Map<String, Object>> resultData = sqlExecutor.sqlExecutor(sql);`
  - `private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList("INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE"));`
  - `return jdbcTemplate.queryForList(sql);`
- **Impacto**: cualquier usuario con `CHAT_AI` controla el prompt y, por inyección de prompt o petición directa, consigue que Gemini emita SQL que el validador acepta porque solo exige que empiece por `SELECT|WITH` y no contenga una lista corta de palabras. No hay lista blanca de tablas ni de esquema: `SELECT username, password_hash FROM users` y `SELECT cc, cellphone, address FROM members` pasan; también `information_schema.tables`, `current_user` y `pg_sleep(60)` (DoS sin límite de filas ni `queryTimeout`); `pg_read_file` y `dblink` no están bloqueados, aunque su viabilidad depende de los privilegios del usuario de BD y de las extensiones instaladas (no verificable, ver Límites). El `user` recibido no se usa en ninguna parte, así que no hay filtro de sede: el resultado abarca todas las sedes y se devuelve al cliente tras pasar por Gemini. El historial del cliente se concatena literalmente en el prompt (`AiService.java:205-211`), lo que hace la inyección trivial. El chequeo de sentencias múltiples solo mira `parts[1]` (`SqlQueryValidator.java:47-52`), de modo que `SELECT 1;;SELECT pg_sleep(60)` supera la validación (la ejecución real del segundo statement depende del modo de statement del driver: latente). `@Transactional(readOnly = true)` en `SqlExecutor.java:17` mitiga escrituras, no exfiltración ni DoS.
- **Recomendación**: sustituir SQL libre por un conjunto de plantillas/consultas parametrizadas; si se mantiene el LLM, ejecutar con un rol de BD de solo lectura limitado a vistas y con RLS por `branch_id`, imponer lista blanca de tablas/funciones, `setMaxRows`/`setQueryTimeout`, y prohibir `;`, comentarios, `WITH`, `information_schema`, `pg_*` y `dblink`. Inyectar la sede desde el principal (no un `new User()`).
- **Esfuerzo**: L

### S-02 — Exportación de asistencias sin filtro de sede si falta el cuerpo
- **Severidad**: Alta
- **Ubicación**: `features/attendance/controller/AttendanceController.java:79-83`, `utils/specs/AttendanceSpecification.java:37`, `features/attendance/service/AttendanceService.java:199-202`
- **Evidencia**:
  - `public ResponseEntity<ExportResponseDto<AttendanceResponseDto>> export(@RequestBody(required = false) AttendanceFiltersRequestDto dto, ...`
  - `if (dto != null) { ... if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name())) { predicates.add(cb.equal(branch.get("id"), user.getMember().getBranch().getId())); } ... }`
  - `List<Attendance> members = attendanceRepository.findAll(spec);`
- **Impacto**: `POST /api/attendances/export` sin cuerpo entrega `dto == null`; el specification no añade ningún predicado (ni sede, ni fechas) y `findAll` devuelve todas las asistencias de todas las sedes, sin paginación. La respuesta incluye nombre completo del integrante, nota, servicio, fecha, sede y autor (`AttendanceMapper.java:26-38`). Alcanza a cualquier rol con `MANAGE_ATTENDANCE` o `MANAGE_DASHBOARD`, no solo a administradores.
- **Recomendación**: fallar con 400 si `dto == null` y mover el predicado de sede fuera del `if (dto != null)` para que sea incondicional. Añadir prueba de export sin cuerpo.
- **Esfuerzo**: S

### S-03 — IDOR entre sedes en el progreso de discipulado
- **Severidad**: Alta
- **Ubicación**: `features/discipulado/controller/DiscipuladoController.java:83-110`, `features/discipulado/service/DiscipuladoService.java:131-158`
- **Evidencia**:
  - `public ResponseEntity<DiscipuladoProgressResponseDto> progress(@RequestBody DiscipuladoProgressRequestDto dto) { return ResponseEntity.ok(discipuladoService.progress(dto, null)); }`
  - `public DiscipuladoProgressResponseDto progress(DiscipuladoProgressRequestDto dto, User user) { DiscipuladoProgress discipuladoProgress = discipuladoProgressRepository.findById(dto.getDiscipuladoProgressId()) ...`
  - `public DiscipuladoProgressResponseDto updateProgress(DiscipuladoProgressRequestDto dto, User user, String id) { DiscipuladoProgress discipuladoProgress = discipuladoProgressRepository.findById(id) ...`
- **Impacto**: ambos métodos reciben `user` pero no lo usan. Cualquier usuario con `DISCIPULADO_PROGRESS` puede marcar como completado el progreso de un discipulado de otra sede, fijar la fecha y asignar como maestro a un integrante de cualquier sede. Es escritura cruzada entre tenants y corrupción de datos. El `create`, `findById`, `search` y `delete` del mismo servicio sí comprueban sede, lo que hace el fallo más fácil de pasar por alto.
- **Recomendación**: pasar el principal, resolver `progress -> discipulado -> member.branch` y comparar con la sede del usuario (salvo admin); validar que `teacherId` pertenece a la misma sede.
- **Esfuerzo**: S

### S-04 — Escalado de privilegios por asignación arbitraria de roles y permisos
- **Severidad**: Alta
- **Ubicación**: `features/user/service/UserService.java:75-89` y `:198-212`, `features/role/service/RoleService.java:44-57`, `features/role/mapper/RoleMapper.java:47-59`, `features/role/dtos/PermissionRequestDto.java:15`
- **Evidencia**:
  - `user.getRoles().addAll(userRequestDto.getRolesId().stream().map(roleId -> { Role role = roleRepository.findById(roleId).orElseThrow(RoleNotFoundException::new); ...`
  - `role.getPermissions().clear(); role.getPermissions().addAll(roleRequestDto.getPermissions().stream().map(p -> { ... .permission(p.getName().name()) ...`
  - `private AppPermission name;` (el enum incluye `ADMINISTRATOR`)
- **Impacto**: no hay lista blanca de roles asignables. Un usuario no administrador con `CREATE_USER` o `UPDATE_USER` puede asignar el rol `ADMINISTRATOR` a otro usuario de su sede (o crear uno nuevo con ese rol) y usar esa cuenta. Con `CREATE_ROLE`/`UPDATE_ROLE` puede crear o modificar un rol con todos los permisos —incluido `ADMINISTRATOR`— y, si además tiene `CREATE_USER`/`UPDATE_USER`, asignarlo a una cuenta que controle. `UPDATE_ROLE` permite además vaciar los permisos del rol ADMINISTRATOR si conoce su id (por ejemplo, porque también tiene `VIEW_ROLE_PANEL`). Tipo latente: depende de qué roles tengan esos permisos en `role_permission`, tabla que no está en el repo.
- **Recomendación**: lista blanca en servidor de roles/permisos asignables; solo `ADMINISTRATOR` puede conceder `ADMINISTRATOR` o editar roles con ese permiso; impedir modificar el rol que uno mismo posee; pruebas de escalado.
- **Esfuerzo**: M

### S-05 — `UPDATE_USER` permite resetear la contraseña de un admin y mover cuentas de sede
- **Severidad**: Alta
- **Ubicación**: `features/user/service/UserService.java:165-171` y `:183-194`
- **Evidencia**:
  - `if (!givenBy.hasAuthority(AppPermission.ADMINISTRATOR.name()) && !user.getMember().getBranch().getId().equals(givenBy.getMember().getBranch().getId())) { throw new CannotUpdateUserWithMemberOutSideYourBranchException(); }`
  - `if (userRequestDto.getMemberId() != null) { user.setMember(member); }`
  - `if (userRequestDto.getPassword() != null && !passwordEncoder.matches(userRequestDto.getPassword(), user.getPasswordHash())) { user.setPasswordHash(passwordEncoder.encode(userRequestDto.getPassword())); }`
- **Impacto**: la comprobación de sede valida la sede **actual** del usuario, no la del miembro nuevo; después se reasigna `member` a cualquiera del DTO, sacando la cuenta de su sede. Además, un no-admin con `UPDATE_USER` puede fijar una contraseña nueva de cualquier usuario cuya ficha de miembro esté en su sede, sin conocer la actual, incluido un usuario con rol ADMINISTRATOR, y entrar como él. El bloqueo de autoedición (`givenBy.getId().equals(user.getId())`) no impide usar una segunda cuenta. Tipo latente por depender de los permisos sembrados.
- **Recomendación**: validar la sede del miembro nuevo; exigir `ADMINISTRATOR` para cambiar la contraseña de otro usuario o sus roles; revocar refresh tokens al resetear.
- **Esfuerzo**: S

### S-06 — Registro de asistencias sin validar la sede del evento ni de los integrantes
- **Severidad**: Alta
- **Ubicación**: `features/attendance/service/AttendanceService.java:58-68` y `:104-115`
- **Evidencia**:
  - `ServiceEvent event = serviceEventRepository.findById(attendances.getFirst().getServiceEventId()).orElseThrow(ServiceEventNotFoundException::new);`
  - `Map<String, Member> members = memberRepository.findAllById(memberIds).stream().collect(Collectors.toMap(Member::getId, m -> m));`
  - `.branch(event.getBranch())`
- **Impacto**: solo se valida que el evento exista, esté activo y la ventana horaria; no que pertenezca a la sede del usuario ni que los integrantes pertenezcan a esa sede. Cualquier rol con `REGISTER_ATTENDANCE` puede insertar asistencias de integrantes de otra sede en un evento de otra sede, y la fila queda etiquetada con la sede del evento. Contamina estadísticas y rompe el aislamiento entre sedes; también permite comprobar la existencia de UUIDs de integrantes ajenos.
- **Recomendación**: verificar `event.getBranch().getId()` contra la sede del usuario (salvo admin) y que cada `member.getBranch()` coincida con la del evento; rechazar el lote completo si algo no cuadra.
- **Esfuerzo**: S

### S-07 — PII completa y notas pastorales expuestas a roles operativos
- **Severidad**: Media
- **Ubicación**: `features/member/controller/MemberController.java:57-68`, `features/member/service/MemberService.java:145`, `features/category/repository/mapper/MemberMapper.java:77-79`
- **Evidencia**:
  - `hasAuthority(T(...).VIEW_MEMBER_PANEL.name()) || hasAuthority(T(...).VIEW_BAPTISM_PANEL.name()) || hasAuthority(T(...).MANAGE_ATTENDANCE.name()) || hasAuthority(T(...).REGISTER_ATTENDANCE.name()) || ... DISCIPULADO_PROGRESS ... DISCIPULADO_CREATE ...`
  - `memberResponseDto.notes(member.getNotes().stream().map(memberNotesMapper::toDto).collect(Collectors.toSet()));`
- **Impacto**: `POST /api/members/search` devuelve cédula, celular, dirección, fecha de nacimiento y notas pastorales (`MemberResponseDto.java:27-38`). Bastan permisos operativos de asistencia, bautismo o discipulado, pensados para tareas acotadas. Es una violación de mínimo privilegio dentro de la misma sede.
- **Recomendación**: exigir `VIEW_MEMBER_PANEL` para el DTO completo; devolver un DTO reducido (nombre, foto, categoría) a los roles operativos y excluir `notes` salvo permiso específico.
- **Esfuerzo**: M

### S-08 — CORS con cualquier origen y `allowCredentials(true)`
- **Severidad**: Media
- **Ubicación**: `configurations/SecurityConfig.java:83-89`
- **Evidencia**:
  - `configuration.setAllowedOriginPatterns(List.of("*"));`
  - `configuration.setAllowCredentials(true);`
- **Impacto**: la API refleja cualquier `Origin` y permite credenciales; `Authorization` está en `allowedHeaders`. Como la autenticación es Bearer en `localStorage` y no por cookie, el impacto es menor que en un esquema con cookies, pero elimina el aislamiento de origen: cualquier web puede invocar la API con un token filtrado y leer la respuesta, y no hay necesidad de CORS en un SPA servido desde el mismo origen.
- **Recomendación**: lista explícita de orígenes por configuración (sin comodín), `allowCredentials(false)` y métodos/cabeceras mínimos.
- **Esfuerzo**: S

### S-09 — Access y refresh token en `localStorage`; refresh de 5 días sin CSP
- **Severidad**: Media
- **Ubicación**: `src/main/resources/static/chunk-CJDLSKNK.js:5`, `src/main/java/org/icc/pecesatierra/features/auth/service/RefreshTokenService.java:26-31`
- **Evidencia**:
  - `getAccessToken(){return localStorage.getItem(this.ACCESS_TOKEN_KEY)}getRefreshToken(){return localStorage.getItem(this.REFRESH_TOKEN_KEY)}storeTokens(t){localStorage.setItem(...)}`
  - `.expiresAt(new Date(System.currentTimeMillis() + Duration.ofDays(5).toMillis()))`
- **Impacto**: cualquier XSS o dependencia comprometida del bundle puede exfiltrar ambos tokens; el refresh dura 5 días y permite obtener access tokens nuevos, por lo que el compromiso sobrevive a la pestaña. No hay cabecera CSP configurada (`SecurityConfig` no personaliza `headers`), lo que agrava el impacto de un XSS. Verificado sobre el bundle minificado commiteado; el código fuente Angular no está en el repo.
- **Recomendación**: refresh en cookie `HttpOnly; Secure; SameSite=Strict` y access token solo en memoria; añadir CSP estricta; limitar la vida absoluta de la sesión.
- **Esfuerzo**: M

### S-10 — Ciclo de vida de tokens: refresh en claro, sin revocación, sin `iss/aud/jti`
- **Severidad**: Media
- **Ubicación**: `features/auth/RefreshToken.java:21-22`, `features/auth/service/AuthService.java:122-143`, `features/auth/service/JwtService.java:22-25` y `:53-59`
- **Evidencia**:
  - `@Column(unique = true, nullable = false) private String token;`
  - `user.setPasswordHash(passwordEncoder.encode(dto.getPassword())); ... userRepository.save(user);` (no borra refresh tokens)
  - `subject(userDetails.getUsername()).issuedAt(new Date(System.currentTimeMillis()))` y `.expiration(new Date(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis()))`
  - `byte[] keyBytes = this.SECRET_KEY.getBytes(StandardCharsets.UTF_8); return Keys.hmacShaKeyFor(keyBytes);`
- **Impacto**: si se lee la tabla `refresh_token`, los tokens son utilizables directamente (no hay hash); tras un cambio de contraseña —el escenario típico tras un compromiso— los refresh tokens del atacante siguen vivos hasta 5 días. El access token no tiene `jti`, `iss` ni `aud` y no se puede revocar; el secreto no se valida al arrancar (si mide menos de 32 bytes, falla en el primer login, no al desplegar). La rotación en sí está bien implementada (`deleteIfValid` + token nuevo, `RefreshTokenService.java:49-62`).
- **Recomendación**: guardar hash del refresh, borrar todos los tokens del usuario al cambiar contraseña o desactivarlo, añadir `jti`/`iss`/`aud` y validarlos, y comprobar la longitud del secreto en el arranque.
- **Esfuerzo**: M

### S-11 — Sin rate limiting ni bloqueo de cuenta en login
- **Severidad**: Media
- **Ubicación**: `features/auth/controller/AuthController.java:27-30`, `configurations/SecurityConfig.java:34-74`, `configurations/AuthenticationConfig.java:17-20`
- **Evidencia**:
  - `@PostMapping("/login") public ResponseEntity<AuthResponseDto> login(@RequestBody @Valid AuthRequestDto authRequestDto) { return ResponseEntity.ok(authService.login(authRequestDto)); }`
  - `DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService); authenticationProvider.setPasswordEncoder(passwordEncoder());`
- **Impacto**: no hay filtro, dependencia ni contador de intentos (búsqueda de `RateLimit|Bucket|attempts|lockout` sin resultados). El login admite fuerza bruta ilimitada por IP/usuario; BCrypt ralentiza pero no impide el ataque distribuido. Tampoco hay límite para `/api/ai/chat`, que consume cuota de Gemini, ni para las exportaciones sin paginación.
- **Recomendación**: limitador por IP+usuario con backoff y bloqueo temporal, alertas de fallos y límite de cuota para IA/exportaciones.
- **Esfuerzo**: M

### S-12 — Enumeración de usuarios por mensajes de error distintos en login
- **Severidad**: Media
- **Ubicación**: `features/auth/service/AuthService.java:37-50`, `features/auth/exceptions/AuthenticatedUserNotFoundException.java:8`, `advice/GlobalHandlerException.java:55-58`
- **Evidencia**:
  - `super("No autorizado.", HttpStatus.UNAUTHORIZED);`
  - `return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.", request.getRequestURI());`
- **Impacto**: si el usuario no existe, `CustomUserDetailsServiceImpl` lanza `AuthenticatedUserNotFoundException` y la respuesta es 401 «No autorizado.»; si existe pero la contraseña es incorrecta, `BadCredentialsException` produce 401 «Credenciales invalidas.». Ambos cuerpos son distinguibles y permiten enumerar cuentas. Además, el camino sin usuario no ejecuta BCrypt, lo que añade una diferencia de tiempos.
- **Recomendación**: un único mensaje/estado para todos los fallos y un hash ficticio para usuarios inexistentes.
- **Esfuerzo**: S

### S-13 — PII, notas y posible clave de Gemini en logs
- **Severidad**: Media
- **Ubicación**: `features/member/service/MemberPersistenceService.java:112-136`, `features/member/service/MemberNoteService.java:49-64`, `features/attendance/service/AttendanceService.java:119-139`, `features/ai/service/AiService.java:46` y `:62`, `:290-310`
- **Evidencia**:
  - `member.getCompleteName(), member.getCc(), member.getCellphone(), member.getCity(), member.getNeighborhood()`
  - `memberNotes.getNote()` (contenido literal de la nota)
  - `log.info("Gemini generated response for SQL: {}", llmResponse);` / `log.info("Gemini final response: {}", finalLlmResponse);`
  - `String url = GEMINI_API_URL + modelId + ":generateContent?key=" + apiKey; ... log.error("Error calling Gemini API", e);`
- **Impacto**: los logs (root INFO a consola, `logback.xml:11-13`) acumulan cédulas, celulares, direcciones y notas pastorales. `AiService` registra el SQL generado y la respuesta final, que puede contener nombres y teléfonos. Si falla la red, el mensaje de `ResourceAccessException` de `RestTemplate` puede incluir la URL completa con `?key=`, filtrando la clave de Gemini al log. `GlobalHandlerException` además imprime stack traces a `System.err`.
- **Recomendación**: logging estructurado con redacción, registrar solo ids; no registrar notas ni respuestas de IA; enviar la clave en cabecera (`x-goog-api-key`) en lugar de query string; desactivar stack traces en producción.
- **Esfuerzo**: M

### S-14 — Resultados con PII enviados a Gemini en cada consulta
- **Severidad**: Media
- **Ubicación**: `features/ai/service/AiService.java:253-258` (y `:56-60`)
- **Evidencia**:
  - `prompt.append("DATOS DE LA BASE DE DATOS: ").append(data.toString()).append("\n\n");`
  - `String formattingUserPrompt = buildFormattingUserPrompt(request.getMessage(), resultData); String finalLlmResponse = callGemini(formattingSystemPrompt, formattingUserPrompt, true);`
- **Impacto**: cada respuesta de IA envía a Google el conjunto de resultados completo (nombres, cédulas, celulares, direcciones, notas) sin minimización ni seudonimización, además del historial del usuario. Es una transferencia de datos personales a un tercero fuera del control de la iglesia, sin base técnica de minimización.
- **Recomendación**: agregar/seudonimizar antes de enviar, enviar solo las columnas necesarias para el gráfico, revisar el acuerdo de tratamiento con el proveedor y documentar la base legal.
- **Esfuerzo**: M

### S-15 — `@PreAuthorize` malformado en `DELETE /api/users/{id}` (500)
- **Severidad**: Media
- **Ubicación**: `features/user/controller/UserController.java:109-117`
- **Evidencia**:
  - `hasAuthority(org.icc.pecesatierra.utils.enums.AppPermission).DELETE_USER.name())`
- **Impacto**: la expresión referencia el tipo sin `T(...)`; SpEL intenta leer la propiedad `org` sobre el root y falla antes de evaluar la rama `|| ADMINISTRATOR`. Verificado en aislamiento con `spring-expression 6.2.15` (la versión que resuelve Boot 3.5.9): `SpelEvaluationException: EL1008E: Property or field 'org' cannot be found`. El endpoint devuelve error (500 por el handler genérico) para todos, incluido ADMINISTRATOR: no hay bypass, pero la función de borrado queda inutilizable y la guarda no protege nada.
- **Recomendación**: `hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).DELETE_USER.name())`; añadir una prueba que evalúe todas las expresiones `@PreAuthorize`.
- **Esfuerzo**: S

### S-16 — `PUT/DELETE` de sedes sin comprobar la sede del usuario
- **Severidad**: Media
- **Ubicación**: `features/branch/service/BranchService.java:63-65` y `:107-109`
- **Evidencia**:
  - `Branch branch = branchRepository.findById(branchId).orElseThrow(BranchNotFoundException::new);` (sin comparar con `user.getMember().getBranch()`)
  - `Branch branch = branchRepository.findById(branchId).orElseThrow(BranchNotFoundException::new);` (idem en delete)
- **Impacto**: cualquier usuario no administrador al que se le conceda `BRANCH_UPDATE` o `BRANCH_DELETE` puede renombrar, cambiar dirección/celular o borrar cualquier sede por id. El borrado solo se bloquea si la sede tiene integrantes o asistencias. Es una escritura cruzada entre tenants; tipo latente porque depende de que esos permisos se asignen a roles no-admin (no verificable sin `role_permission`).
- **Recomendación**: restringir ambas operaciones a `ADMINISTRATOR` o limitar a la sede propia.
- **Esfuerzo**: S

### S-17 — Comparación por identidad de `Branch` en bautismos (deniega a no-admin)
- **Severidad**: Media
- **Ubicación**: `features/baptism/service/BaptismService.java:54` y `:87`, `features/branch/Branch.java:15-36`
- **Evidencia**:
  - `if (!user.hasAuthority("ADMINISTRATOR") && !member.getBranch().equals(user.getMember().getBranch())) { throw new CannotRegisterBaptismWithMemberOutsideYourBranch(); }`
  - `@Entity @Table(name = "branches") public class Branch { ... }` (sin `equals`/`hashCode`)
- **Impacto**: el principal se carga en el filtro JWT (otra transacción/persistence context) y el integrante se carga en el servicio; son dos instancias distintas de `Branch`, y `Object.equals` compara identidad, así que la condición es siempre verdadera para no-administradores. Ningún usuario no-admin puede crear ni invalidar bautismos. Falla cerrado (no hay fuga), pero rompe la funcionalidad y demuestra que la comprobación no está probada.
- **Recomendación**: comparar `getId()` como hace el resto del código (`MemberService.java:176`, `AttendanceService.java:152`).
- **Esfuerzo**: S

### S-18 — Precedencia de operadores: admin elude `isActive` al borrar servicios
- **Severidad**: Baja
- **Ubicación**: `features/service/controller/ServiceController.java:55-59`
- **Evidencia**:
  - `hasAuthority(T(...).DELETE_SERVICE.name()) || hasAuthority(T(...).ADMINISTRATOR.name()) && @securityService.isActive(authentication)`
- **Impacto**: `&&` liga más fuerte que `||`, así que la expresión equivale a `DELETE_SERVICE || (ADMINISTRATOR && isActive)`. Un administrador desactivado con un access token aún vivo (hasta 10 minutos) puede borrar servicios. El resto de controladores agrupa con paréntesis.
- **Recomendación**: envolver la disyunción en paréntesis antes de `&&`.
- **Esfuerzo**: S

### S-19 — `logout` y cambio de contraseña sin `isActive`
- **Severidad**: Baja
- **Ubicación**: `features/auth/controller/AuthController.java:38-49`
- **Evidencia**:
  - `@PostMapping("/logout") public ResponseEntity<Void> logout(@AuthenticationPrincipal User user, ...`
  - `@PostMapping("/changge-password") public ResponseEntity<Void> changgePassword(...`
- **Impacto**: son los dos únicos endpoints autenticados sin `@PreAuthorize` ni `isActive`; solo los cubre `.requestMatchers("/api/**").authenticated()` (`SecurityConfig.java:47`). Un usuario desactivado con token vigente puede cambiar su contraseña y cerrar sesión durante la ventana del access token. No es escalado, pero rompe la política de desactivación inmediata.
- **Recomendación**: añadir `@PreAuthorize("@securityService.isActive(authentication)")` a ambos.
- **Esfuerzo**: S

### S-20 — Errores JWT no controlados: firma inválida devuelve 500
- **Severidad**: Baja
- **Ubicación**: `configurations/JwtAuthFilter.java:49-65`
- **Evidencia**:
  - `} catch (ExpiredJwtException | MalformedJwtException e) { response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); ... .message(e.getMessage()) ...`
- **Impacto**: `SignatureException`, `UnsupportedJwtException` e `IllegalArgumentException` no se capturan; una petición con token manipulado o algoritmo no soportado produce 500 (o la respuesta por defecto del contenedor) en vez de 401, y se devuelve `e.getMessage()` con detalles del parser. No hay fail-open: la petición no continúa autenticada.
- **Recomendación**: capturar `JwtException | IllegalArgumentException` y devolver un 401 con mensaje fijo.
- **Esfuerzo**: S

### S-21 — Catálogos y roles listables sin permiso específico
- **Severidad**: Baja
- **Ubicación**: `features/type/controller/MemberTypeController.java:58-61`, `features/category/controller/MemberCategoryController.java:59-62`, `features/role/controller/RoleController.java:71-84`
- **Evidencia**:
  - `@PreAuthorize("@securityService.isActive(authentication)")` en ambos `GET`
  - `hasAuthority(...VIEW_ROLE_PANEL.name()) || ... ADMINISTRATOR ... || hasAuthority(...UPDATE_USER.name())`
- **Impacto**: cualquier usuario activo puede listar tipos y categorías; cualquier usuario con `UPDATE_USER` puede listar todos los roles con sus permisos, útil para reconocimiento previo a un escalado. La sensibilidad es baja, pero no hay motivo para no exigir el permiso del panel correspondiente.
- **Recomendación**: exigir `VIEW_MEMBER_PANEL`/`VIEW_ROLE_PANEL` (o el permiso del panel que consume el catálogo).
- **Esfuerzo**: S

### S-22 — Higiene de dependencias y ausencia de SCA
- **Severidad**: Baja
- **Ubicación**: `build.gradle:3`, `:39`, `:42-44`, `:48`
- **Evidencia**:
  - `id 'org.springframework.boot' version '3.5.9'`
  - `implementation 'io.jsonwebtoken:jjwt-api:0.12.5'`
  - `implementation 'com.cloudinary:cloudinary-http44:1.32.0'`
  - `implementation 'org.apache.tika:tika-core:3.2.2'`
- **Impacto**: no se encontró ningún CVE aplicable explotable en el código revisado: Tika 3.2.2 es precisamente la versión que corrige CVE-2025-66516/CVE-2025-54988 (afecta hasta 3.2.1) y aquí solo se usa `Tika.detect()`; jjwt 0.12.5 aparece en CVE-2024-31033 (disputada, corregida en 0.12.6), que afecta a `setSigningKey(String)`, API que este código no usa; Spring Framework 6.2.15 acumula CVEs 2026 (CVE-2026-41843/41852/59283) pero requieren recursos estáticos desde filesystem con versionado o SpEL de entrada no confiable, que no es el caso; Spring Boot 3.5.9 está por debajo de 3.5.14/3.5.15 (CVE-2026-40972/40973/40992/41001), ninguna aplicable por features no usadas (sin DevTools, JavaMail, Artemis ni sesiones persistentes). El riesgo es de deriva: no hay SCA ni actualización automatizada y hay artefactos de 2022 (Cloudinary) y 2024 (jjwt) sin actualizar.
- **Recomendación**: subir a la última 3.5.x (≥3.5.15), jjwt ≥0.12.7, Cloudinary 1.39.x; añadir OWASP Dependency-Check o Renovate al build.
- **Esfuerzo**: M

## Descartados (falsos positivos o mitigados)

| Qué parecía | Por qué no es hallazgo | Evidencia |
|---|---|---|
| Inyección SQL en búsquedas y specifications | Todas usan Criteria API/JPQL con parámetros; el único SQL crudo es el del endpoint de IA (S-01) | `MemberSpecification.java:79-96` (`cb.like`, `cb.function`), `AttendanceSpecification.java:38-82` |
| `alg=none` o clave pública usada como HMAC | `parseSignedClaims` exige token firmado y `verifyWith(SecretKey)` restringe a HMAC; no se usa `setSigningKey(String)` ni `parse()` sin verificar | `JwtService.java:27-33` |
| CSRF | Sesión `STATELESS`, sin cookies de autenticación; el token va en `Authorization` | `SecurityConfig.java:67-69` |
| `/h2-console` o Actuator expuestos | No son dependencias del build; `/h2-console` cae en el catch-all del SPA y devuelve `index.html` | `build.gradle:20-49`, `MvcConfig.java:28-30` |
| Secretos hardcodeados en el repo | `application.properties` usa `${...}` para DB/JWT/Cloudinary/Gemini; grep de literales `password|secret|api_key|token =` sin resultados en `src/main`, `build.gradle` y `logback.xml` | `application.properties:20-33` |
| Path traversal o MIME falso en subida de imágenes | Tika detecta por magic bytes y limita a JPEG/PNG/WEBP; el límite global es 5 MB; el nombre del archivo no se usa en el filesystem del servidor y las credenciales de Cloudinary no se devuelven | `PictureUtils.java:27-39`, `application.properties:7-9`, `CloudinaryService.java:22-34` |
| Borrado de archivos ajenos en Cloudinary | El `publicId` sale de la BD (`member.getPublicId()`), no del cliente; en `delete(memberId)` se pasa por error la URL en vez del publicId, lo que falla, no borra archivos ajenos | `MemberService.java:98-109` y `:203`, `PictureUtils.java:47-60` |
| Endpoints públicos por accidente | Solo `POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/health` y estáticos; `/api/**` se evalúa antes del catch-all y exige autenticación | `SecurityConfig.java:40-64` |
| Contraseñas en claro o hashing débil | BCrypt con coste por defecto (10) y el cambio verifica la contraseña actual antes de escribir | `AuthenticationConfig.java:28-31`, `AuthService.java:125-131` |
| Replay del refresh token rotado | La rotación borra el token solo si sigue vigente (`deleteIfValid`); el token viejo deja de servir | `RefreshTokenService.java:49-62`, `RefreshTokenRepository.java:23-32` |
| Escrituras vía IA (`DELETE`/`UPDATE`/`SELECT INTO`) | El validador bloquea esas palabras y `@Transactional(readOnly = true)` impide escribir; el riesgo real es lectura/DoS (S-01) | `SqlQueryValidator.java:14-16`, `SqlExecutor.java:17` |
| Fuga de refresh token en la respuesta de login | Es el diseño del flujo; el problema es dónde lo guarda el cliente (S-09) y cómo lo almacena el servidor (S-10) | `AuthService.java:68-71` |

## Límites de la revisión

- **Sin base de datos ni runtime**: no se ejecutó `bootRun` ni `test` (faltan `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, credenciales de Cloudinary y `GEMINI_API_KEY`). No se pudo verificar qué roles tienen `CREATE_USER`, `UPDATE_USER`, `CREATE_ROLE`, `UPDATE_ROLE`, `BRANCH_UPDATE` o `BRANCH_DELETE` en `role_permission`; por eso S-04, S-05 y S-16 quedan como latentes.
- **Comportamiento de PostgreSQL en el endpoint de IA**: no se pudo comprobar si el `DB_USER` es superusuario, si `dblink` está instalado, si `pg_read_file` es invocable ni si el driver ejecuta el segundo statement de `SELECT 1;;SELECT pg_sleep(60)`. La lógica del validador sí se verificó por lectura.
- **`@RequestBody` con JSON `null`**: no se verificó empíricamente si Spring MVC entrega `null` a `MemberController.export` (`MemberSpecification` también devuelve `cb.conjunction()` con DTO nulo). Por eso la variante de exportación de integrantes no se incluyó como hallazgo; el caso de asistencias (S-02) es inequívoco porque el cuerpo es `required = false`.
- **Verificación del SpEL**: se hizo en aislamiento con `spring-expression 6.2.15` (el jar que resuelve el build), no dentro del contexto Spring Security completo; el fallo de evaluación es reproducible, el código HTTP final (500) es la consecuencia esperada del handler genérico.
- **Frontend**: solo se auditó el bundle minificado commiteado; no hay fuentes Angular ni `package.json` en el repo. No se pudo evaluar si las respuestas de la IA se renderizan con `innerHTML` (posible XSS por inyección de prompt) ni la política CSP real del despliegue.
- **Dependencias**: se contrastaron avisos públicos de Spring Boot, Spring Framework, Spring Security, jjwt, Tika y Cloudinary, pero no se ejecutó un SCA ni se resolvió el árbol transitivo; las versiones de `cloudinary-http44` y su HttpClient no se verificaron más allá de la ficha del artefacto. Cualquier CVE no listada en S-22 queda como NO VERIFICABLE.
- **Despliegue**: se desconoce si hay TLS/HSTS terminado en un proxy, retención y destino de logs, configuración de la cuenta de Cloudinary (acceso público a las URLs) y permisos del rol de BD. HSTS y la exposición de las imágenes subidas no se pudieron verificar.
- **Pruebas de autorización**: no hay fixtures ni tests de integración; la cobertura de guardas se midió por lectura estática de las 69 rutas (anexo), no con peticiones reales.
- **`MvcConfig`**: el `PathResourceResolver` se sobrescribe con `location.createRelative(resourcePath)` sin `checkResource`; no se pudo confirmar si Tomcat normaliza todos los `..` codificados en este despliegue, así que no se reporta como hallazgo.

## Anexo — Inventario de endpoints (69 rutas)

`Permiso` resume el `@PreAuthorize`; `isActive` indica si incluye `@securityService.isActive(authentication)` (o `isActive(#refreshTokenRequestDto)` en refresh). `Scoping` indica si el servicio compara la sede del usuario con la del recurso.

| # | Endpoint | Permiso | isActive | Scoping de sede | Nota |
|---|---|---|---|---|---|
| 1 | POST `/api/ai/chat` | CHAT_AI \|\| ADMIN | Sí | No | S-01; recibe `new User()` |
| 2 | POST `/api/attendances` | REGISTER_ATTENDANCE \|\| ADMIN | Sí | No | S-06 |
| 3 | POST `/api/attendances/search` | MANAGE_ATTENDANCE \|\| MANAGE_DASHBOARD \|\| ADMIN | Sí | Sí | NPE si falta el cuerpo |
| 4 | POST `/api/attendances/export` | MANAGE_ATTENDANCE \|\| MANAGE_DASHBOARD \|\| ADMIN | Sí | **No si `dto == null`** | S-02 |
| 5 | PATCH `/api/attendances/invalidate` | MANAGE_ATTENDANCE \|\| ADMIN | Sí | Sí | — |
| 6 | POST `/api/auth/login` | — (público) | N/A | N/A | S-11, S-12 |
| 7 | POST `/api/auth/refresh` | `isActive(#refreshTokenRequestDto)` | Sí (token) | N/A | Ruta pública con token |
| 8 | POST `/api/auth/logout` | — | No | N/A (propio) | S-19; solo `.authenticated()` |
| 9 | POST `/api/auth/changge-password` | — | No | N/A (propio) | S-19; solo `.authenticated()` |
| 10 | POST `/api/baptisms` | BAPTISM_CREATE \|\| ADMIN | Sí | Parcial | S-17 |
| 11 | POST `/api/baptisms/invalidate` | BAPTISM_INVALIDATE \|\| ADMIN | Sí | Parcial | S-17 |
| 12 | POST `/api/baptisms/search` | VIEW_BAPTISM_PANEL \|\| ADMIN | Sí | Sí | — |
| 13 | POST `/api/branches` | BRANCH_CREATE \|\| ADMIN | Sí | N/A (global) | — |
| 14 | GET `/api/branches` | VIEW_BRANCH_PANEL \|\| ADMIN | Sí | N/A (todas) | — |
| 15 | PUT `/api/branches/{branchId}` | BRANCH_UPDATE \|\| ADMIN | Sí | No | S-16 |
| 16 | DELETE `/api/branches/{branchId}` | BRANCH_DELETE \|\| ADMIN | Sí | No | S-16 |
| 17 | POST `/api/category-rules` | ADMIN | Sí | N/A (global) | — |
| 18 | PUT `/api/category-rules/{categoryRuleId}` | ADMIN | Sí | N/A | — |
| 19 | PATCH `/api/category-rules/{categoryRuleId}` | ADMIN | Sí | N/A | — |
| 20 | DELETE `/api/category-rules/{categoryRuleId}` | ADMIN | Sí | N/A | — |
| 21 | GET `/api/category-rules` | ADMIN | Sí | N/A | — |
| 22 | POST `/api/member-category` | ADMIN | Sí | N/A | — |
| 23 | PUT `/api/member-category/{categoryId}` | ADMIN | Sí | N/A | — |
| 24 | DELETE `/api/member-category/{categoryId}` | ADMIN | Sí | N/A | — |
| 25 | GET `/api/member-category` | — (solo activo) | Sí | N/A | S-21 |
| 26 | POST `/api/member-sub-category` | ADMIN | Sí | N/A | — |
| 27 | PUT `/api/member-sub-category/{subCategoryId}` | ADMIN | Sí | N/A | — |
| 28 | DELETE `/api/member-sub-category/{subCategoryId}` | ADMIN | Sí | N/A | — |
| 29 | POST `/api/dashboards` | MANAGE_DASHBOARD \|\| ADMIN | Sí | Sí | — |
| 30 | POST `/api/discipulado` | DISCIPULADO_CREATE \|\| ADMIN | Sí | Sí | — |
| 31 | GET `/api/discipulado/{id}` | VIEW_DISCIPULADO_PANEL \|\| ADMIN | Sí | Sí | — |
| 32 | POST `/api/discipulado/search` | VIEW_DISCIPULADO_PANEL \|\| ADMIN | Sí | Sí | — |
| 33 | DELETE `/api/discipulado/{id}` | DISCIPULADO_DELETE \|\| ADMIN | Sí | Sí | — |
| 34 | POST `/api/discipulado/progress` | DISCIPULADO_PROGRESS \|\| ADMIN | Sí | **No** | S-03 |
| 35 | PUT `/api/discipulado/progress/{id}` | DISCIPULADO_PROGRESS \|\| ADMIN | Sí | **No** | S-03 |
| 36 | GET `/api/health` | — (público) | N/A | N/A | Por diseño |
| 37 | POST `/api/members` | CREATE_MEMBER \|\| ADMIN | Sí | Sí | — |
| 38 | PUT `/api/members/{memberId}` | UPDATE_MEMBER \|\| ADMIN | Sí | Sí | — |
| 39 | POST `/api/members/search` | VIEW_MEMBER_PANEL \|\| VIEW_BAPTISM_PANEL \|\| MANAGE_ATTENDANCE \|\| REGISTER_ATTENDANCE \|\| DISCIPULADO_PROGRESS \|\| DISCIPULADO_CREATE \|\| ADMIN | Sí | Sí | S-07; NPE si falta el cuerpo |
| 40 | POST `/api/members/export` | VIEW_MEMBER_PANEL \|\| ADMIN | Sí | Parcial | `null` de cuerpo no verificado (Límites) |
| 41 | DELETE `/api/members/{memberId}` | DELETE_MEMBER \|\| ADMIN | Sí | Sí | — |
| 42 | PATCH `/api/members/{memberId}` | UPDATE_MEMBER \|\| ADMIN | Sí | Sí | — |
| 43 | POST `/api/members/notes` | CREATE_NOTE \|\| ADMIN | Sí | Sí | — |
| 44 | DELETE `/api/members/notes/{noteId}` | DELETE_NOTE \|\| ADMIN | Sí | Sí | — |
| 45 | POST `/api/reports` | MANAGE_REPORT \|\| MANAGE_DASHBOARD \|\| ADMIN | Sí | Sí | — |
| 46 | POST `/api/roles` | CREATE_ROLE \|\| ADMIN | Sí | N/A (global) | S-04 |
| 47 | PUT `/api/roles/{roleId}` | UPDATE_ROLE \|\| ADMIN | Sí | N/A | S-04 |
| 48 | DELETE `/api/roles/{roleId}` | DELETE_ROLE \|\| ADMIN | Sí | N/A | — |
| 49 | GET `/api/roles` | VIEW_ROLE_PANEL \|\| UPDATE_USER \|\| ADMIN | Sí | N/A | S-21 |
| 50 | POST `/api/services` | CREATE_SERVICE \|\| ADMIN | Sí | N/A (global) | — |
| 51 | PUT `/api/services/{serviceId}` | UPDATE_SERVICE \|\| ADMIN | Sí | N/A | — |
| 52 | DELETE `/api/services/{serviceId}` | DELETE_SERVICE \|\| ADMIN | **Parcial** | N/A | S-18 (precedencia) |
| 53 | GET `/api/services` | VIEW_SERVICE_PANEL \|\| REGISTER_ATTENDANCE \|\| ADMIN | Sí | N/A | — |
| 54 | PATCH `/api/services/{serviceId}` | UPDATE_SERVICE \|\| ADMIN | Sí | N/A | — |
| 55 | POST `/api/services-events` | REGISTER_EVENTS \|\| ADMIN | Sí | Sí | — |
| 56 | DELETE `/api/services-events/{serviceEventId}` | CANCEL_EVENTS \|\| ADMIN | Sí | Sí | — |
| 57 | POST `/api/services-events/calendar` | MANAGE_ATTENDANCE \|\| VIEW_EVENTS_PANEL \|\| ADMIN | Sí | Sí | NPE si falta el cuerpo |
| 58 | GET `/api/services-events/active` | REGISTER_ATTENDANCE \|\| ADMIN | Sí | Sí | — |
| 59 | POST `/api/member-type` | ADMIN | Sí | N/A | — |
| 60 | PUT `/api/member-type/{typeId}` | ADMIN | Sí | N/A | — |
| 61 | DELETE `/api/member-type/{typeId}` | ADMIN | Sí | N/A | — |
| 62 | GET `/api/member-type` | — (solo activo) | Sí | N/A | S-21 |
| 63 | POST `/api/users/report` | VIEW_USER_PANEL \|\| ADMIN | Sí | Sí | — |
| 64 | POST `/api/users` | CREATE_USER \|\| ADMIN | Sí | Sí (miembro) | S-04 (roles) |
| 65 | GET `/api/users/@me` | — (solo activo) | Sí | N/A (propio) | — |
| 66 | POST `/api/users/search` | VIEW_USER_PANEL \|\| ADMIN | Sí | Sí | NPE si falta el cuerpo |
| 67 | PUT `/api/users/{userId}` | UPDATE_USER \|\| ADMIN | Sí | Parcial | S-04, S-05 |
| 68 | PATCH `/api/users/{userId}` | UPDATE_USER \|\| ADMIN | Sí | Sí | — |
| 69 | DELETE `/api/users/{userId}` | DELETE_USER \|\| ADMIN (roto) | Sí | Sí (inalcanzable) | S-15 |

**Recuento de cobertura**: 69 handlers; 65 con `@PreAuthorize` (64 con `isActive(authentication)` + 1 con `isActive(#refreshTokenRequestDto)`); 4 sin `@PreAuthorize` (login y health públicos por diseño; logout y cambio de contraseña solo con `.authenticated()`); 0 endpoints públicos accidentales. De los 65, todos admiten `ADMINISTRATOR` y todos contienen `isActive`, aunque en S-18 solo cubre a los no-admin y en S-15 la expresión falla antes de evaluarla.
