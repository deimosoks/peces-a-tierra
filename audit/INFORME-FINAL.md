# Informe final consolidado — Arquitectura, calidad y seguridad

- **Repositorio**: `C:\dev\peces-a-tierra`
- **Rama**: `audit/arquitectura-y-seguridad` (creada desde `master` en `f3d07cc`)
- **Fecha**: 2026-10-23
- **Alcance**: 250 archivos Java al cierre de la auditoría (251 tras el fix M-03), 16 features,
  19 controllers, 69 handlers HTTP, 7 Specifications,
  `build.gradle`, `application.properties`, `logback.xml`, `Dockerfile`, wrapper y bundle de la PWA.
- **Método**: 2 agentes de análisis en paralelo (calidad y seguridad, sin acceso cruzado), 2 agentes
  de verificación adversarial con contexto limpio (cada uno solo vio su eje) y consolidación con
  muestreo manual del orquestador. Sin runtime ni base de datos: análisis estático, bytecode de los
  jars resueltos y `compileJava` (BUILD SUCCESSFUL con JDK 21).
- **Artefactos fuente**: `audit/borrador-calidad.md`, `audit/borrador-seguridad.md`,
  `audit/verificacion-calidad.md`, `audit/verificacion-seguridad.md`.

---

## 1. Resumen ejecutivo

La base del proyecto es razonable: separación controller → service → repository respetada en lo
esencial, 65 de 69 endpoints con `@PreAuthorize` (64 con `isActive(authentication)`), sin entidades
JPA expuestas, sin
secretos de servidor hardcodeados, sin `alg=none`, sin `/h2-console` ni actuator, y con BCrypt para
contraseñas. El problema no es la ausencia de controles, sino que **el aislamiento entre sedes se
aplica caso por caso y falla en varios caminos concretos**, y que **no hay red de pruebas** que
detecte regresiones.

**Resultado de la verificación**: de los 46 hallazgos originales, **42 quedaron confirmados, 4
parciales, 0 refutados y 0 no verificables**. La ola 2 añadió 12 hallazgos nuevos y el consolidado
fusionó 6 duplicados entre ejes: **51 hallazgos únicos**, de los que **1 se descartó por regla de
negocio** (H-06), quedando **50 vigentes** (1 crítica, 5 altas, 21 medias, 23 bajas).

Los cinco riesgos que justifican actuar primero:

1. **H-01 (Crítica)** — `/api/ai/chat` ejecuta SQL generado por el LLM sobre todo el esquema y todas
   las sedes. El validador es una lista negra de 11 palabras: permite `users.password_hash`,
   `members.cc`, `information_schema`, `UNION` y `pg_sleep`. Sin límite de filas ni timeout.
2. **H-02 (Alta, determinista)** — `POST /api/attendances/export` sin cuerpo devuelve las asistencias
   de **todas las sedes** con nombre, nota, sede y autor. No necesita LLM ni privilegios de admin.
3. **H-03 (Alta)** — cualquier usuario con `DISCIPULADO_PROGRESS` puede escribir el progreso de
   discipulado de integrantes de otra sede.
4. **H-04/H-05 (Alta, latentes)** — `UPDATE_USER`/`UPDATE_ROLE` permiten asignar `ADMINISTRATOR`,
   resetear la contraseña de un admin y mover cuentas entre sedes. Su explotabilidad depende de qué
   roles tengan esos permisos en `role_permission` (tabla externa al repo).
5. **H-07 (Alta funcional)** — registrar un bautismo **pierde la nota** y guarda `city` como
   `municipality`: pérdida silenciosa de datos que ya está ocurriendo.

---

## 2. Decisiones de consolidación

### 2.1 Duplicados fusionados

| Fusión | Resultado |
|---|---|
| `S-02` ≡ `VQ-01` | **H-02**, un único hallazgo (fuga entre sedes en export de asistencias) |
| `S-20` ≡ `VQ-02` | **H-12**, 500 en el filtro JWT |
| `Q-02` ≡ `S-17` | **H-08**, `Branch` sin `equals`; se reclasifica como bug funcional, no fuga |
| `Q-14` ≡ `VS-05` | **H-31**, la foto no se borra de Cloudinary |
| `Q-10` ⊂ `S-01` | Se absorbe en **H-01** como causa contribuyente (sin `maxRows` ni timeout) |
| `VS-04` ⊂ `Q-11` | Se absorbe en **H-11** como caso concreto (mensaje interno en login) |

### 2.2 Afirmaciones corregidas o retiradas

- **Retirada**: la clave de Gemini NO se filtra al log por `ResourceAccessException`; Spring 6.2.15
  elimina la query string del mensaje. Se mantiene el riesgo de PII en logs (H-13) y la clave en la
  URL hacia Google como buena práctica, no como hallazgo.
- **Retirada**: «no hay CVEs aplicables». Sin SCA ni árbol transitivo resuelto, eso es
  **NO VERIFICABLE**. Queda como deuda la ausencia de escaneo (H-51).
- **Corregido**: `@Transactional(readOnly=true)` de `SqlExecutor` mitiga DML/DDL, **no** lectura,
  `pg_sleep`, locks ni DoS. La afirmación del borrador era demasiado fuerte.
- **Rebajado**: `S-17` de Media a Baja (falla cerrado, no hay fuga) y **fusionado** con H-08.
- **Acotado**: `Q-07` (último día del reporte) es condicional: la PWA envía 23:59 por defecto; solo
  falla si el cliente manda `00:00`. Pasa a Baja/latente (H-29).
- **Acotado**: `Q-12` no es «no hay `@Valid`»; hay `@Valid` en 14 controllers. El defecto real son
  Discipulado, `gender` nulo y la cascada de DTOs anidados (H-28).

### 2.3 Recomendaciones que NO hay que aplicar literalmente

- **No prohibir `WITH`** en el validador de IA: el prompt del sistema lo usa para consultas
  complejas (`AiService.java:189-191`). La solución es lista blanca de tablas/columnas + rol de BD
  restringido + RLS, no otra keyword.
- **No llamar `setMaxRows`/`setQueryTimeout` sobre el `JdbcTemplate` inyectado**: es un singleton y
  mutaría todas las consultas concurrentes. Usar una instancia dedicada.
- **No añadir `@Valid` a `RoleRequestDto.permissions` sin antes cambiar `@NotBlank` por `@NotNull`**
  en `PermissionRequestDto.name` (un enum no admite `@NotBlank`): el alta de roles pasaría a 500.
- **No proponer H2** como base de pruebas: el proyecto usa `unaccent` y `ddl-auto=validate`. La vía
  es Testcontainers PostgreSQL.
- **No añadir `equals/hashCode` a entidades JPA** como arreglo genérico de H-08: comparar por
  `getId()` en el servicio.
- **No exigir `VIEW_MEMBER_PANEL` a los catálogos** (S-21): dejaría sin tipos/categorías a quien
  solo tiene `REGISTER_ATTENDANCE`. Elegir permiso por consumidor o crear uno de catálogo.

---

## 3. Hallazgos críticos y altos (detalle)

### H-01 — SQL libre del LLM ejecutado sobre todo el esquema y todas las sedes
- **Severidad**: Crítica · **Esfuerzo**: L · **Origen**: S-01 + Q-10 · **Tipo**: real, condicionado
- **Ubicación**: `features/ai/controller/AiController.java:29`, `features/ai/service/AiService.java:51-53,189-211,246-258`,
  `configurations/SqlQueryValidator.java:12-55`, `utils/sql_executor/SqlExecutor.java:17-19`
- **Evidencia**: `return ResponseEntity.ok(aiService.processChat(request, new User()));` ·
  `SELECT_PATTERN = "^\\s*(SELECT|WITH)\\s"` y lista negra de 11 palabras · `return jdbcTemplate.queryForList(sql);` ·
  `restTemplate.exchange(...)` sin timeout.
- **Impacto**: un usuario con `CHAT_AI` controla `message`/`history`, que se concatenan literalmente
  en el prompt. El SQL resultante pasa si empieza por `SELECT`/`WITH` y no contiene la lista negra:
  quedan permitidos `UNION`, `information_schema`, `current_user`, `pg_sleep` (DoS sin timeout),
  `SELECT ... FROM users` (incluido `password_hash`) y `SELECT ... FROM members` de cualquier sede.
  El resultado se reenvía a Gemini y vuelve al cliente; `string_agg` permite exfiltrar en una celda.
  `pg_read_file`/`dblink` dependen de privilegios del rol de BD (no verificable).
- **Condición**: no hay parámetro SQL directo; el atacante necesita que Gemini obedezca la inyección
  de prompt. `readOnly` impide DML/DDL (probablemente; no probado contra PostgreSQL), no lectura ni DoS.
- **Recomendación**:
  1. Sustituir SQL libre por un catálogo de consultas parametrizadas con `branch_id` obligatorio.
  2. Si se mantiene el LLM: rol de BD de solo lectura, sin `pg_read_file`/`dblink`, con RLS por sede;
     lista blanca de tablas/columnas; `maxRows` y `queryTimeout` en una instancia dedicada de
     `JdbcTemplate`; prohibir comentarios y `;`.
  3. Inyectar el usuario real (no `new User()`) y aplicar el filtro de sede en el ejecutor.
- **Prueba**: caso con `message` que pida `SELECT password_hash FROM users` y caso con `pg_sleep`.

### H-02 — Exportación de asistencias sin cuerpo omite el filtro de sede
- **Severidad**: Alta · **Esfuerzo**: S · **Origen**: S-02 + VQ-01 · **Tipo**: real, explotable hoy
- **Ubicación**: `features/attendance/controller/AttendanceController.java:79-83`,
  `utils/specs/AttendanceSpecification.java:37,60-64`, `features/attendance/service/AttendanceService.java:199-202`
- **Evidencia**: `@RequestBody(required = false) AttendanceFiltersRequestDto dto` y todo el bloque de
  predicados, **incluido el de sede**, dentro de `if (dto != null) { ... }`; después
  `attendanceRepository.findAll(spec)`.
- **Impacto**: con `dto == null` el specification no añade ningún predicado y devuelve todas las
  asistencias de todas las sedes, sin paginar, con nombre completo, nota, servicio, fecha, sede y
  autor. Lo puede hacer cualquier rol con `MANAGE_ATTENDANCE` o `MANAGE_DASHBOARD`, no solo un admin.
- **Recomendación**: sacar el predicado de sede del `if`; si se mantiene el cuerpo opcional,
  normalizar `dto` a un DTO vacío antes de construir el spec. Prueba: export sin cuerpo con usuario
  de sede B y verificar que solo ve B.

### H-03 — IDOR entre sedes en el progreso de discipulado
- **Severidad**: Alta · **Esfuerzo**: S · **Origen**: S-03 · **Tipo**: real, explotable hoy
- **Ubicación**: `features/discipulado/service/DiscipuladoService.java:132-158`
- **Evidencia**: `progress(...)` recibe el usuario y lo ignora (`progress` pasa `null`); no compara
  `getId()` de sede antes de crear/actualizar el progreso.
- **Impacto**: un usuario con `DISCIPULADO_PROGRESS` puede completar el progreso de un integrante de
  otra sede, fijar la fecha y asignar un maestro de otra sede. El resto de operaciones del mismo
  servicio sí comprueban la sede.
- **Recomendación**: cargar el integrante y comparar `member.getBranch().getId()` con la del usuario
  (o `ADMINISTRATOR`), igual que `create/findById/search/delete`. Prueba cruzada de sede.

### H-04 — Escalado de privilegios por asignación arbitraria de roles y permisos
- **Severidad**: Alta (latente) · **Esfuerzo**: M · **Origen**: S-04 · **Tipo**: latente
- **Ubicación**: `features/user/service/UserService.java:75-89`, `features/role/service/RoleService.java`,
  `features/role/dtos/RoleRequestDto.java:23-24`, `features/role/dtos/PermissionRequestDto.java:14-15`
- **Evidencia**: `rolesId` se acepta sin lista blanca y `RoleRequestDto.permissions` no tiene `@Valid`,
  así que la validación anidada no se ejecuta; se pueden crear roles con `ADMINISTRATOR`.
- **Impacto**: un rol no-admin con `CREATE_ROLE` + `CREATE_USER` (o `UPDATE_USER`) puede crear un rol
  con `ADMINISTRATOR` y asignárselo, o asignarse el rol ADMINISTRATOR existente. `GET /api/roles`
  admite `UPDATE_USER`, así que el atacante puede descubrir el id del rol.
- **Condición**: depende de que esos permisos estén sembrados en `role_permission` para roles no
  admin. **Verificar en BD antes de priorizar.**
- **Recomendación**: lista blanca de permisos asignables según los privilegios del actor; impedir
  asignar `ADMINISTRATOR` salvo a un `ADMINISTRATOR`; validar el enum con `@NotNull`, no `@NotBlank`.

### H-05 — `UPDATE_USER` permite resetear la contraseña de un admin y mover cuentas de sede
- **Severidad**: Alta (latente) · **Esfuerzo**: S · **Origen**: S-05 · **Tipo**: latente
- **Ubicación**: `features/user/service/UserService.java:159,169,183-194`
- **Evidencia**: la comprobación de sede usa la sede **actual** del target (`:169`), pero el nuevo
  `memberId` se carga en `:159` y se asigna en `:183-185` sin comprobar su sede; la contraseña se
  resetea sin pedir la actual y el bloqueo de autoedición (`:162-163`) se evita con una segunda cuenta.
- **Impacto**: con `UPDATE_USER` se resetea la contraseña de un `ADMINISTRATOR` de la misma sede y se
  entra como él; también se puede mover una cuenta a otra sede. No revoca refresh tokens.
- **Recomendación**: exigir `ADMINISTRATOR` para modificar a un usuario con `ADMINISTRATOR`; validar
  la sede del `memberId` nuevo; revocar refresh tokens al cambiar contraseña.

### H-07 — El bautismo pierde la nota y guarda `city` como `municipality`
- **Severidad**: Alta (funcional) · **Esfuerzo**: S · **Origen**: Q-01 · **Tipo**: real, ocurriendo ya
- **Ubicación**: `features/baptism/service/BaptismService.java:62-68`, entidad `Baptism.java:31`
- **Evidencia**: `.neighborhood(baptismRequestDto.getNote())` seguido de
  `.neighborhood(baptismRequestDto.getNeighborhood())` (el primero se descarta) y
  `.municipality(baptismRequestDto.getCity())`.
- **Impacto**: la nota que escribe el usuario se guarda como `null`; `municipality` queda siempre con
  el valor de `city`. Pérdida silenciosa de datos y filtros por municipio sesgados. La PWA envía
  `note` y `municipality`.
- **Recomendación**: `.note(...)` y `.municipality(...)`, más una prueba de persistencia que compare
  request y response. Revisar si hay datos ya corruptos y planificar backfill.

---

## 4. Hallazgos medios

| ID | Hallazgo | Evidencia | Origen | Esf. |
|---|---|---|---|---|
| H-08 | `Branch` sin `equals` bloquea bautismos de todos los no-admin (falla cerrado) | `BaptismService.java:54,87` | Q-02 + S-17 | S |
| H-09 | `@PreAuthorize` con SpEL inválido inutiliza `DELETE /api/users/{id}` (500) | `UserController.java:109-117` | S-15 | S |
| H-10 | Cuerpos `@RequestBody(required=false)` desreferenciados → NPE/500 | `MemberService.java:129`, `AttendanceController.search`, `UserController.search`, `ServiceEventController.calendar` | Q-06 | S |
| H-11 | Errores HTTP mal clasificados: 401 para permisos, 500 para entrada inválida y mensaje interno en login | `GlobalHandlerException.java:31-35,49-65` | Q-11 + VS-04 | S |
| H-12 | El filtro JWT no captura `SignatureException` ni usuario borrado: 500 en vez de 401 | `JwtAuthFilter.java:49-65` | S-20 + VQ-02 | S |
| H-13 | PII, notas pastorales y SQL en logs INFO | `MemberPersistenceService.java:112-136`, `MemberNoteService.java:49-64`, `AiService.java:46,62` | S-13 | M |
| H-14 | Resultados con PII de todas las sedes se envían a Gemini en cada consulta | `AiService.java:246-258` | S-14 | M |
| H-15 | Roles operativos reciben CC, celular, dirección y notas (mínimo privilegio intra-sede) | `MemberController.java:57-68` | S-07 | M |
| H-16 | CORS con `allowedOriginPatterns("*")` + `allowCredentials(true)` | `SecurityConfig.java:83-89` | S-08 | S |
| H-17 | Tokens en `localStorage`; refresh en claro 5 días sin revocación ni `iss/aud/jti` | `chunk-CJDLSKNK.js:5`, `RefreshToken.java:21-22` | S-09 + S-10 | M |
| H-18 | Sin rate limiting ni bloqueo de cuenta en login (ni en IA/export) | `AuthController.java:27-30` | S-11 | M |
| H-19 | Enumeración de usuarios por mensaje y tiempo distintos en login | `AuthService.java:37-50` | S-12 | S |
| H-20 | `PUT/DELETE` de sedes no comprueban la sede del usuario (latente) | `BranchService.java:63-65` | S-16 | S |
| H-21 | `UPDATE_USER` permite desactivar o degradar a un administrador (latente) | `UserService.java:196-212,249-269` | VS-03 | S |
| H-22 | Fuga del nombre de un integrante de otra sede y oráculo de UUID en `PUT /members/{id}` | `MemberPersistenceService.java:160,163`, `MemberMapper.java:133-134` | VS-02 | S |
| H-23 | NPE en el `catch` del alta de integrante sin foto; foto huérfana en errores no `ApiException` | `MemberService.java:80-82` | Q-03 | S |
| H-24 | `categoryLocked` se ignora al actualizar integrante | `MemberMapper.java:153` | Q-04 | S |
| H-25 | Editar una subcategoría sin cambiar el nombre siempre devuelve 409 | `MemberSubCategoryService.java:74` | Q-05 | S |
| H-26 | El calendario desplaza la ventana de fechas 5 h (`Z` descartado + `toUTC`) | `ServiceEventsFilterRequestDto.java:16` | Q-08 | M |
| H-27 | El filtro de discipulado interpreta en UTC lo que se escribe en Bogotá | `DiscipuladoSpecification.java:54` | Q-09 | S |
| H-28 | Validación de DTOs incorrecta o ausente (`@NotBlank` sobre enum/fecha/boolean, sin cascada) | `DiscipuladoRequestDto.java:18-19`, `PermissionRequestDto.java:14-15` | Q-12 | S |

---

## 5. Hallazgos bajos

| ID | Hallazgo | Origen | Esf. |
|---|---|---|---|
| H-29 | El reporte excluye el último día solo si el cliente envía `00:00` (latente) | Q-07 | S |
| H-30 | Doble carga del usuario con `@EntityGraph` pesado por petición | Q-13 | S |
| H-31 | La foto no se borra de Cloudinary: se pasa la URL como `public_id` (retención de PII) | Q-14 + VS-05 | S |
| H-32 | Exportaciones y listas del dashboard sin límite | Q-15 | M |
| H-33 | `BaseController` y la feature `log` son código muerto; tres formatos de respuesta | Q-16 | S/M |
| H-34 | 65 `@PreAuthorize` duplicados y 8 `beforeUpdate` repetidos | Q-17 | L |
| H-35 | El log de borrado de usuario atribuye datos del actor y borra historial de roles | Q-18 | S |
| H-36 | Excepciones de negocio reutilizadas fuera de contexto | Q-19 | S |
| H-37 | Higiene de build: dependencia duplicada, `src (2).zip` versionado, sin tests | Q-20 | M |
| H-38 | El wrapper 8.14.3 no arranca con el JDK 25 del sistema (`major version 69`) | Q-21 | S |
| H-39 | `MemberMapper` con tres repositorios y `RoleMapper` con N+1 | Q-22 | M |
| H-40 | Edades y ventana del dashboard con la zona del servidor | Q-23 | S |
| H-41 | El lote de asistencias asigna todos los registros al primer evento | Q-24 | S |
| H-42 | Renombrar username o tipo a un valor existente devuelve 500 en vez de 409 | VQ-03 | S |
| H-43 | El filtro de edad pierde a quienes están en el último año de cada edad | VQ-04 | S |
| H-44 | La IA convierte cualquier fallo interno en 200 con mensaje amable | VQ-05 | S |
| H-45 | Permisos duplicados en un rol: el `Set` no deduplica y el alta falla | VQ-06 | S |
| H-46 | `RestTemplate` de Gemini sin timeouts: agota hilos de Tomcat | VQ-07 | S |
| H-47 | Clave de Google Maps hardcodeada en el bundle público | VS-01 | S |
| H-48 | Precedencia `OR/AND`: admin elude `isActive` al borrar servicios | S-18 | S |
| H-49 | `logout` y cambio de contraseña sin `isActive` | S-19 | S |
| H-50 | Catálogos y roles listables con permisos amplios (reconocimiento) | S-21 | S |
| H-51 | Sin SCA ni escaneo de dependencias; CVEs no verificables | S-22 | M |

---

## 6. Roadmap de corrección propuesto

### Fase 0 — Quick wins (menos de 1 día, riesgo bajo)
1. **H-02**: mover el predicado de sede fuera de `if (dto != null)` en `AttendanceSpecification`.
2. **H-07**: corregir el builder de `BaptismService` (`note`, `municipality`).
3. **H-08**: comparar `getId()` en `BaptismService:54` y `:87`.
4. **H-09**: corregir la expresión SpEL de `UserController:109-117`.
5. **H-48**: poner paréntesis en `ServiceController:55-59`.
6. **H-31**: usar `member.getPublicId()` al borrar la foto.
7. **H-46**: timeouts en el `RestTemplate` de Gemini.

### Fase 1 — Aislamiento multi-sede (2–3 días)
H-03, H-04, H-05, H-21, H-22, H-20, H-15. Requiere **verificar antes** el sembrado de
`role_permission` y decidir la jerarquía de roles. Añadir pruebas de acceso cruzado de sede para cada
caso (dos usuarios, dos sedes, el mismo UUID).

### Fase 2 — IA y sesión (3–5 días)
H-01 (decisión de producto: plantillas parametrizadas vs. SQL libre con rol restringido + RLS),
H-13, H-14, H-16, H-17, H-18, H-19, H-12. H-01 es el único que exige rediseño y no debe abordarse
como un parche del validador.

### Fase 3 — Correctitud funcional (2–3 días)
H-10, H-11, H-23, H-24, H-25, H-26, H-27, H-28, H-42, H-43, H-44, H-45.

### Fase 4 — Deuda y red de seguridad (1 semana)
H-29, H-30, H-32–H-41, H-47, H-49–H-51. Incluye:
- Testcontainers PostgreSQL con la extensión `unaccent` y un esquema de prueba; sustituir el único
  `contextLoads`.
- Fijar el JDK 21 en el wrapper (`toolchain`) o subir Gradle para que el build no dependa de `JAVA_HOME`.
- SCA en el build (OWASP Dependency-Check o Renovate).
- Restringir por referrer la clave de Google Maps (o rotarla si se confirma uso indebido).

---

## 7. Lo que se verificó y NO es un problema

Para evitar retrabajo, estos puntos se revisaron y se descartaron con evidencia:

- CSRF: la API usa Bearer, no cookies de sesión.
- `alg=none` / confusión de algoritmo: jjwt 0.12.5 valida la firma con la clave configurada.
- Secretos de servidor hardcodeados: no hay en `src/main` ni en `application.properties`.
- `/h2-console`, actuator y endpoints públicos accidentales: no existen.
- Contraseñas: BCrypt; no se almacenan en claro.
- Path traversal y MIME en uploads: Tika valida el contenido y `MvcConfig` bloquea `../`.
- Borrado de archivos ajenos en Cloudinary: no es posible con el flujo actual.
- Entidades JPA expuestas en respuestas: todas las respuestas son DTOs, `Boolean`, `Void` o `String`.
- Accesos lazy fuera de transacción: no se encontraron; `default_batch_fetch_size=50` mitiga el N+1
  salvo en `UserService.search` y en `RoleMapper` (H-39).
- Cron del reporte (`0 0 3 ? * 2#2`) y `registeredTo` con `T23:59:59`: correctos.
- El `null` literal en `POST /api/members/export` no produce fuga: `required=true` devuelve 400.
- **H-06 (descartado por regla de negocio, 2026-10-24)**: un integrante de una sede puede asistir a
  un evento de otra sede, así que la ausencia de validación de sede en `AttendanceService` es
  intencional. Se mantiene H-41 (el lote se asigna al primer evento) como defecto independiente.
  Ver `audit/00-aclaraciones-negocio.md`.

---

## 8. Límites del informe

- **Sin runtime ni BD**: no se explotó ningún endpoint ni se ejecutó `bootRun`/`test`. Todo es
  análisis estático, bytecode de los jars resueltos y `compileJava`.
- **`role_permission` no está en el repo**: H-04, H-05, H-20 y H-21 quedan condicionados al sembrado
  real. Si esos permisos son exclusivos de `ADMINISTRATOR`, su severidad baja.
- **Privilegios de PostgreSQL**: no se verificó si `DB_USER` es superusuario, si `dblink`/`pg_read_file`
  son invocables ni si el driver ejecuta el segundo statement. `pg_sleep`, `information_schema`,
  `current_user` y `UNION` sí son viables con un rol normal.
- **Cloudinary y Google**: no se llamó a sus APIs; se infiere del contrato de los SDK.
- **PWA**: solo se auditó el bundle minificado; no hay fuentes Angular ni `package.json` en el repo.

---

## Anexo A — Trazabilidad

| Consolidado | Orígenes |
|---|---|
| H-01 | S-01, Q-10 |
| H-02 | S-02, VQ-01 |
| H-03 | S-03 |
| H-04 | S-04 |
| H-05 | S-05 |
| H-06 | S-06 — descartado por regla de negocio (2026-10-24) |
| H-07 | Q-01 |
| H-08 | Q-02, S-17 |
| H-09 | S-15 |
| H-10 | Q-06 |
| H-11 | Q-11, VS-04 |
| H-12 | S-20, VQ-02 |
| H-13 | S-13 |
| H-14 | S-14 |
| H-15 | S-07 |
| H-16 | S-08 |
| H-17 | S-09, S-10 |
| H-18 | S-11 |
| H-19 | S-12 |
| H-20 | S-16 |
| H-21 | VS-03 |
| H-22 | VS-02 |
| H-23 | Q-03 |
| H-24 | Q-04 |
| H-25 | Q-05 |
| H-26 | Q-08 |
| H-27 | Q-09 |
| H-28 | Q-12 |
| H-29 | Q-07 |
| H-30 | Q-13 |
| H-31 | Q-14, VS-05 |
| H-32 | Q-15 |
| H-33 | Q-16 |
| H-34 | Q-17 |
| H-35 | Q-18 |
| H-36 | Q-19 |
| H-37 | Q-20 |
| H-38 | Q-21 |
| H-39 | Q-22 |
| H-40 | Q-23 |
| H-41 | Q-24 |
| H-42 | VQ-03 |
| H-43 | VQ-04 |
| H-44 | VQ-05 |
| H-45 | VQ-06 |
| H-46 | VQ-07 |
| H-47 | VS-01 |
| H-48 | S-18 |
| H-49 | S-19 |
| H-50 | S-21 |
| H-51 | S-22 |

## Estado de implementación (actualizado 2026-10-24)

| Estado | Hallazgos |
|---|---|
| **Corregidos y con prueba de regresión (servidor)** | H-02, H-03, H-04, H-05, H-07, H-08, H-09, H-10, H-11, H-12, H-20, H-21, H-31, H-46, H-48 |
| **Pendientes críticos/altos** | H-01 (IA, parcial) y H-22 (oráculo UUID, parcial) |
| **Descartados por regla de negocio** | H-06 (asistencia inter-sede permitida) |
| **Aceptados temporalmente (TODO)** | H-41 (lote de asistencias) y M-01 (el PWA hace logout con 403); ver `audit/TODO.md` |
| **Pendientes medios/bajos** | H-13 a H-19, H-23 a H-30, H-32 a H-45, H-47, H-49 a H-51 |

- Suite actual: **135 tests, 131 ejecutados, 4 saltados, 0 fallos** (25 suites). Saltan 3 IT por Docker
  apagado y `ApplicationContextIT`.
- Las regresiones de H-02, H-03, H-04, H-05, H-07, H-08, H-09, H-10, H-11, H-12, H-20, H-21, H-22,
  H-31 y H-48 se comprobaron con mutaciones: revertir cada fix hace fallar su prueba.
- H-03 valida la sede del integrante y del maestro en `progress`/`updateProgress`.
- H-04/H-05/H-21 impiden que un no-admin conceda permisos que no tiene, gestione cuentas/roles con
  más privilegios o mueva usuarios de sede; el cambio de contraseña revoca refresh tokens.
- H-10/H-12 normalizan cuerpos nulos y el filtro JWT. H-11 queda corregido en servidor
  (403/400/405/413/415 y `getAllErrors`); el 403 sigue provocando `logout()` en el PWA precompilado
  (M-01, aceptado).
- H-20 limita `update`/`delete` de sedes a la propia sede para no-admin; H-22 cierra la fuga del
  nombre, pero el oráculo UUID 404 vs 409 sigue abierto (M-03 residual).
- H-01 parcial: `SqlExecutor` usa un `JdbcTemplate` dedicado (`aiJdbcTemplate`) contra el rol
  `peces_ai_readonly`, con `maxRows=1000` y `queryTimeout=10`. Sigue abierto porque el rol conserva
  `SELECT` sobre todo `public`; faltan las vistas `ai_read`, el filtrado por sede y el validador SQL.
- **Auditoría de implementaciones (2026-10-24)**: 14 CONFIRMADO, 3 PARCIAL (H-01, H-11, H-22),
  0 REFUTADO. Ver `audit/verificacion-implementaciones.md`. Antes de fusionar: M-01 (el PWA hace
  `logout()` ante cualquier 403), M-02 (fuga de nombre en bautismos), M-05 (refresh tokens en cambio
  self-service), M-04 (500 residuales) y H-01.
- **Corrección post-auditoría (2026-10-24)**: M-02, M-04, M-05, M-08, M-11, M-12, M-13, M-14, M-15,
  M-16, M-17, M-19, M-23 y M-24 corregidos con test de regresión; M-03 parcial (datos cerrados,
  oráculo UUID residual). Siguen abiertos H-01/M-07/M-22, M-01 (bundle del PWA), M-06/M-09/M-10/M-18/
  M-21 y el residual de M-03. Ver `audit/fixes-m02-m24.md` y `audit/TODO.md`.
- Las IT contra PostgreSQL (`AttendanceExportIT`, `ApplicationContextIT`) quedan pendientes de
  ejecutarse en un entorno con Docker; hoy no hay CI en el repositorio.

Todo el trabajo está commiteado en la rama; no se ha hecho push.

## Anexo B — Artefactos

| Archivo | Contenido |
|---|---|
| `audit/00-alcance.md` | Contrato, rúbrica, baseline de build |
| `audit/00-aclaraciones-negocio.md` | Reglas de negocio que descartan hallazgos (H-06) |
| `audit/TODO.md` | Riesgos aceptados y cambios de diseño aplazados (H-41) |
| `audit/borrador-calidad.md` | 24 hallazgos Q + descartados + límites |
| `audit/borrador-seguridad.md` | 22 hallazgos S + anexo de 69 endpoints |
| `audit/verificacion-calidad.md` | 22 CONFIRMADO / 2 PARCIAL + 7 nuevos (VQ) |
| `audit/verificacion-seguridad.md` | 20 CONFIRMADO / 2 PARCIAL + 5 nuevos (VS) + cobertura reconstruida |
| `audit/verificacion-implementaciones.md` | Meta-auditoría de las implementaciones (M-01..M-24) |
| `audit/fixes-m02-m24.md` | Corrección post-auditoría de M-02 a M-24 |
| `audit/INFORME-FINAL.md` | Este documento |

Los artefactos de auditoría están commiteados en la rama desde `3b18ce2` (`git ls-files audit/`
lista 23 archivos).
