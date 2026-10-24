# Plan de pruebas — Peces a Tierra

**Rama:** `audit/arquitectura-y-seguridad` · **Fecha:** 2026-10-23 · **Naturaleza:** diseño; este documento no modifica producción, `build.gradle` ni los informes de auditoría.

## 0. Punto de partida

- 19 controllers de features (20 archivos `*Controller`, incluido el `BaseController` abstracto), **69 handlers** y **65 `@PreAuthorize`**. Sin anotación quedan `POST /api/auth/login`, `POST /api/auth/logout`, `POST /api/auth/changge-password` y `GET /api/health`; `POST /api/auth/refresh` usa una expresión de bean (`@securityService.isActive(#refreshTokenRequestDto)`).
- El único test es `src/test/java/org/icc/pecesatierra/PecesATierraApplicationTests.java` (`@SpringBootTest` + `contextLoads`). Exige `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, Cloudinary y `GEMINI_API_KEY`. No existe `src/test/resources`.
- `spring.jpa.hibernate.ddl-auto=validate`, sin migraciones ni seeds; PostgreSQL requiere `unaccent`. `open-in-view=false`, IDs UUID `String`, escrituras UTC y negocio en `America/Bogota`.
- La auditoría consolidó 51 hallazgos. Este plan prioriza autorización, scoping por sede, fechas, validación e IA; no pretende cobertura total.
- Hay 7 correcciones en curso sobre `UserController`, `ServiceController`, `AttendanceSpecification`, `BaptismService`, `MemberService` y `RestTemplateConfig`. Las pruebas se escriben contra el **comportamiento esperado** posterior al merge; no se afirma que un defecto concreto siga presente.
- El build válido es con JDK 21 (`$env:JAVA_HOME="C:\Program Files\Java\jdk-21"`). El wrapper 8.14.3 no arranca con el JDK 25 del sistema.

## 1. Objetivo y principios

**Objetivo:** red de seguridad ejecutable que detecte regresiones de los hallazgos críticos y altos, y que obligue a mantener el aislamiento por sede, la autorización por permiso y la conversión de fechas.

**Principios**

1. Probar contrato observable (HTTP, persistencia, DTOs), no detalles de implementación.
2. Un test debe fallar por una sola razón y con un mensaje que identifique el hallazgo.
3. Determinismo: `Clock` fijo, IDs capturados de la BD, sin `Thread.sleep`, sin orden entre tests.
4. Aislamiento: cada test crea sus datos; nada depende de la BD de producción ni de variables de entorno reales.
5. Seguridad por defecto: los casos sin permiso y sin sede se prueban antes que el happy path.
6. No usar H2: el proyecto depende de `unaccent`, `timestamptz` y funciones de PostgreSQL.

**Qué no se prueba**

- Código generado por MapStruct (`*Impl`), Lombok (getters/setters/builders), DTOs triviales, `equals/hashCode` generados.
- El bundle Angular de `src/main/resources/static/`, APIs reales de Cloudinary/Gemini, internals de PostgreSQL, la feature `log` y `BaseController` mientras sigan sin uso.
- Textos exactos de logs o mensajes salvo que sean contrato de error del cliente.

**Pirámide propuesta (por casos, no por líneas)**

| Nivel | Proporción | Volumen estimado | Tiempo objetivo |
|---|---:|---:|---:|
| Unitarios puros (JUnit 5 + Mockito) | 50 % | ~120 | < 10 s |
| Slice web (`@WebMvcTest` + seguridad) | 30 % | ~70 casos (matriz parametrizada) | < 30 s |
| Integración (Testcontainers) | 20 % | ~45 | 60–120 s |

## 2. Infraestructura de test

### 2.1 Dependencias

| Dependencia | Estado actual | Uso |
|---|---|---|
| `org.springframework.boot:spring-boot-starter-test` | **Ya está** (`build.gradle:23`) | JUnit 5, Mockito, AssertJ, MockMvc, JSONPath |
| `org.junit.platform:junit-platform-launcher` | **Ya está** (`testRuntimeOnly`) | Ejecución JUnit |
| `org.springframework.security:spring-security-test` | **Falta** | `@WithMockUser`, `SecurityMockMvcRequestPostProcessors`, utilidades de seguridad |
| `org.springframework.boot:spring-boot-testcontainers` | **Falta** | `@ServiceConnection` |
| `org.testcontainers:junit-jupiter` | **Falta** | `@Testcontainers`, `@Container` |
| `org.testcontainers:postgresql` | **Falta** | `PostgreSQLContainer` |

Versiones gestionadas por el BOM de Boot 3.5.9: **no fijar versiones**. Bloque exacto a añadir cuando se implemente:

```gradle
testImplementation 'org.springframework.security:spring-security-test'
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
```

No añadir `com.h2database:h2` ni `spring-boot-starter-data-jpa` de test. `spring-boot-starter-test` ya incluye `mockito-junit-jupiter`; no duplicar.

### 2.2 Perfil `test`

Crear `src/test/resources/application-test.properties` con valores literales de prueba. Así ningún placeholder de `application.properties` consulta el entorno:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/peces_test
spring.datasource.username=test
spring.datasource.password=test
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.jdbc.time_zone=UTC
jwt.secret=test-secret-0123456789-0123456789-0123456789
cloudinary.cloud.name=test
cloudinary.api.key=test
cloudinary.api.secret=test
google.gemini.api.key=test
google.gemini.model.id=test-model
logging.level.org.hibernate.SQL=warn
```

`jwt.secret` debe tener al menos 32 bytes: `JwtService` usa HS256 con `Keys.hmacShaKeyFor`. Los valores de Cloudinary/Gemini permiten construir los beans sin red.

### 2.3 Resolver `ddl-auto=validate` sin esquema

`src/main/resources/application.properties` **no se toca**. La comparación es solo para el perfil de test:

| Opción | Cómo | Coste | Riesgo de deriva | Veredicto |
|---|---|---|---|---|
| (a) Testcontainers + `create-drop` + `withInitScript` | Contenedor PostgreSQL; `CREATE EXTENSION IF NOT EXISTS unaccent`; Hibernate genera el esquema desde entidades | 0,5–1 día | **Alto**: el esquema de test lo genera Hibernate, no producción; puede ocultar diferencias de columnas, tipos y constraints | **Recomendada ahora** |
| (b) `schema.sql` versionado derivado de producción | `pg_dump --schema-only` de una BD real; `spring.sql.init.mode=always` + `ddl-auto=validate` en test | 1–2 días + mantenimiento | **Medio**: detecta deriva contra el dump, pero el dump se queda viejo | Complemento posterior, no base inicial |
| (c) Flyway | `flyway-core` + `flyway-database-postgresql`, baseline `V1__baseline.sql`, `ddl-auto=validate` | 3–5 días y cambio operativo | **Bajo** a largo plazo | Proyecto aparte; no bloquear la red mínima |

**Recomendación:** (a) en Fase 0. Es la única que permite arrancar hoy sin un dump ni migraciones y mantiene `validate` intacto en producción. Mitigar la deriva con un job nocturno que restaure un dump de producción en un contenedor y ejecute un `SchemaContractIT` con `ddl-auto=validate` (opción b). Flyway se decide cuando exista baseline de producción; no como requisito de estas pruebas.

`src/test/resources/db/testcontainers-init.sql`:

```sql
CREATE EXTENSION IF NOT EXISTS unaccent;
```

### 2.4 Testcontainers

```java
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Container @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("peces_test").withUsername("test")
                    .withPassword("test").withInitScript("db/testcontainers-init.sql");
}
```

| Criterio | `@ServiceConnection` | `@DynamicPropertySource` |
|---|---|---|
| Código | Mínimo; Boot deriva URL, usuario y driver | Hay que registrar `spring.datasource.*` a mano |
| Personalización | Limitada al contenedor | Permite un segundo datasource o un rol de solo lectura |
| Riesgo | Contenedor estático y contexto cacheado | Olvidar una propiedad produce un contexto distinto o fallo de conexión |

Usar `@ServiceConnection` para el PostgreSQL principal. Reservar `@DynamicPropertySource` para un contenedor secundario con rol restringido (pruebas de `SqlExecutor`) o para propiedades que no cubre la conexión. Si aparecen varios contextos y el contenedor se detiene entre clases, pasar al patrón singleton (`static { POSTGRES.start(); }` + `@DynamicPropertySource`).

**Tiempos y Docker:** primera descarga de `postgres:16-alpine` 5–15 s; arranque 2–5 s; contexto Spring 8–15 s; suite completa 60–120 s con un único contenedor estático y caché de contexto. Evitar `@DirtiesContext` salvo aislamiento real. En Windows se requiere Docker Desktop con backend WSL2; en GitHub Actions `ubuntu-latest` ya trae Docker y no hace falta declarar `services:`. No activar `withReuse(true)` en CI; el contenedor estático por JVM ya reutiliza.

### 2.5 Aislamiento de `@SpringBootTest`

- `@ActiveProfiles("test")` en `AbstractIntegrationTest`; reemplazar `contextLoads` por `ApplicationContextIT extends AbstractIntegrationTest`.
- Sustituir `@MockBean` (deprecado) por `@MockitoBean`.
- En slices web: mockear `SecurityService`, `JwtAuthFilter`, `AuthenticationProvider` y todos los services que inyectan los controllers. En tests de IA: `@MockitoBean AiService`; en member: `@MockitoBean PictureUtils`/`CloudinaryService`; en JWT: `JwtService` real con secreto de test y `CustomUserDetailsServiceImpl` mockeado.
- No mockear globalmente `RestTemplate`: los tests de Gemini necesitan el bean real y `MockRestServiceServer`. Un test que no necesita contexto no debe arrancar Spring; `compileJava` no debe depender del perfil de test.

## 3. Niveles y estructura

### 3.1 Unitarios puros (sin Spring)

`@ExtendWith(MockitoExtension.class)`, constructor injection o `@InjectMocks`, AssertJ. Mockear repositorios, mappers y `DateTimeUtils`.

| Archivo propuesto | Qué cubre | Aserción representativa |
|---|---|---|
| `unit/time/DateTimeUtilsTest` | `toUTC`, `toColombia`, `nowUTC` con `Clock` fijo | `23:30` Bogotá → `04:30Z` del día siguiente |
| `unit/member/MemberServiceTest` | Borrado/activación por sede, admin, foto | Fuera de sede → `ApiException` 409; `delete` usa `publicId` |
| `unit/attendance/AttendanceServiceTest` | Sede del evento y del integrante, ventana, lote | Evento de sede B → 409/400; cada fila usa su `serviceEventId` |
| `unit/baptism/BaptismServiceTest` | Sede por `getId()`, builder `note`/`municipality`, duplicado | Dos `Branch` distintos con el mismo id → permitido; `note` no nulo |
| `unit/discipulado/DiscipuladoServiceTest` | `progress` y `updateProgress` con integrante/maestro de otra sede | `ApiException` 409; admin permitido |
| `unit/user/UserServiceTest` | Roles asignables, cambio de sede, admin, autodesactivación | No-admin no asigna `ADMINISTRATOR`; nuevo `memberId` de otra sede → 409 |
| `unit/auth/AuthServiceTest` | Login inactivo, refresh, logout ajeno, cambio de contraseña | Token de otro usuario → excepción de negocio |
| `unit/ai/SqlQueryValidatorTest` | Corpus de bypass | Ver 3.4 |
| `unit/ai/AiServiceTest` | Gemini simulado, SQL inválido, error del proveedor | No se ejecuta SQL si el validador rechaza |

```java
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {
    @Mock AttendanceRepository attendanceRepository;
    @Mock ServiceEventRepository serviceEventRepository;
    @Mock MemberRepository memberRepository;
    @Mock DateTimeUtils dateTimeUtils;
    @InjectMocks AttendanceService service;

    @Test
    void rejectsEventFromAnotherBranch() {
        // user de sede A, event de sede B
        assertThatThrownBy(() -> service.create(List.of(dto), userA))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
```

Los errores de sede son **409** (`CONFLICT`) en las excepciones actuales; no confundirlos con el **403** de autorización.

### 3.2 Slice web y matriz de autorización

**`web/security/EndpointSecurityMatrixTest`**: `@WebMvcTest` con todos los controllers, `@ActiveProfiles("test")`, `@Import(SecurityConfig.class)`, `GlobalHandlerException` incluido por el slice, y `@MockitoBean` para `SecurityService`, `AuthenticationProvider`, `JwtAuthFilter` y los servicios. Un solo contexto para los 69 handlers.

| Caso | Token | Autoridad | `isActive` | Esperado |
|---|---|---|---|---|
| Público | No | — | — | 200 en login (cuerpo válido) y health; 401 en el resto |
| Autenticado sin permiso | Sí | Ninguna | true | **403** |
| Permiso correcto | Sí | La del `@PreAuthorize` | true | 2xx (o 4xx de negocio, nunca 403) |
| Administrador | Sí | `ADMINISTRATOR` | true | 2xx |
| Usuario desactivado | Sí | La correcta | false | **403** |
| Token inválido/expirado | Sí | — | — | **401** (cubierto en `JwtAuthFilterTest`, no con filtro mockeado) |

Casos especiales: `logout` y `changge-password` no tienen `@PreAuthorize`; deben exigir autenticación y, tras el arreglo, `isActive` (inactivo → 403). `refresh` se prueba con token válido, inválido e inactivo. En `@BeforeEach`, `when(securityService.isActive(any(Authentication.class))).thenReturn(true)`; para `refresh`, stube también `isActive(any(RefreshTokenRequestDto.class))`. El caso inactivo cambia el valor a `false`. `web/discipulado/DiscipuladoProgressUserTest` captura el `User` que el controller pasa a `progress` y exige que no sea `null`.

**Trampa del principal:** `@WithMockUser` inyecta un `org.springframework.security.core.userdetails.User`, pero los controllers reciben `@AuthenticationPrincipal org.icc.pecesatierra.features.user.User`; el argumento sería `null`. Crear `support/WithDomainUser.java` + `DomainUserSecurityContextFactory` que construyan un `User` de dominio con `Role`/`RolePermission` y lo pongan como principal. Para la matriz, el factory debe permitir `branch` A/B y un conjunto de `AppPermission`.

**Cuerpos:** `MemberController.create/update` y `BaptismController.create` usan `@ModelAttribute`, no JSON: enviar `multipart()` o `param()`. Los `@RequestBody` usan un fixture JSON válido; un cuerpo inválido puede dar 400 **antes** de evaluar `@PreAuthorize`, así que la matriz debe usar cuerpos válidos para que el 403 sea determinista.

**Completitud:** `web/security/EndpointMatrixCompletenessTest` obtiene `RequestMappingHandlerMapping` y compara los handlers con `src/test/resources/security/endpoint-matrix.csv`. Si aparece un endpoint nuevo sin fila, falla. Columnas: `controller,httpMethod,path,authority,bodyFixture,expectedWithoutPermission`.

**SpEL inválido:** `web/security/PreAuthorizeSpelTest` recorre los 65 métodos anotados y parsea cada expresión; también evalúa las expresiones representativas con `DefaultMethodSecurityExpressionHandler` y `BeanFactoryResolver` para detectar beans inexistentes y variables mal nombradas.

```java
@Test
void allPreAuthorizeExpressionsParse() {
    var parser = new SpelExpressionParser();
    for (Method m : controllerMethods()) {
        var ann = m.getAnnotation(PreAuthorize.class);
        if (ann != null) {
            assertThatNoException().isThrownBy(() -> parser.parseExpression(ann.value()));
        }
    }
}
```

La evaluación debe usar `handler.createEvaluationContext(authentication, methodInvocation)`; un `StandardEvaluationContext` plano no conoce `hasAuthority` ni `#param`. Para `refresh`, aportar la variable `#refreshTokenRequestDto`.

**Validación y errores**

- `web/validation/ValidationContractTest`: nombre vacío, tipo/categoría nulos, `gender` inválido, `dateStarted` nulo (`@NotBlank` sobre `LocalDateTime`), `PermissionRequestDto.name` nulo, `RoleRequestDto.permissions` anidado. Esperado 400, nunca 500.
- `web/validation/NullableBodyTest`: `POST /api/members/search`, `/api/attendances/search`, `/api/users/search` y `/api/services-events/calendar` sin cuerpo. Esperado 200 con lista vacía o 400 controlado; nunca NPE/500.
- `web/error/GlobalHandlerExceptionTest`: `ApiException` 409, `MethodArgumentNotValidException` 400, `BadCredentialsException` 401, `AuthorizationDeniedException` **403**, `Exception` 500 con mensaje genérico y sin stack trace en el cuerpo.
- `web/security/JwtAuthFilterTest`: firma inválida, token expirado, malformado y usuario borrado → **401**, no 500. Usar `MockMvc` standalone con el filtro real.

### 3.3 Integración con Testcontainers

| Archivo propuesto | Foco |
|---|---|
| `integration/member/MemberSpecificationIT` | `unaccent` (`José`/`jose`, `Muñoz`/`munoz`), dos sedes, admin vs operador, paginación 20, orden, `onlyActive`, edad |
| `integration/attendance/AttendanceExportIT` | Export sin cuerpo con usuario de sede B: solo B; admin ve A+B; `dto` con `branchId` no permite escapar de la sede |
| `integration/attendance/AttendanceServiceIT` | Evento/integrante de otra sede, lote con dos eventos, duplicados, ventana horaria, `saveAll` |
| `integration/attendance/AttendanceSpecificationIT` | Predicados con `dto` nulo, sede, fechas y paginación; regresión de H-02 |
| `integration/time/TimezoneContractIT` | `ServiceEventsFilterRequestDto`, `DiscipuladoSpecification`, reporte y dashboard en UTC/Bogotá (H-26/H-27/H-40) |
| `integration/baptism/BaptismPersistenceIT` | `note` en el DTO; `municipality` en la entidad/repositorio; `existsByBaptizedMemberAndInvalidFalse`; dos sedes |
| `integration/user/UserRoleIT` | `role_permission` sembrado, `User.getAuthorities()`, `user_role` único, conteos por sede |
| `integration/lazy/LazyLoadingContractIT` | Llamar al service y mapear DTO fuera de transacción; sin `LazyInitializationException` (`open-in-view=false`) |
| `integration/tx/TransactionRollbackIT` | `MemberService.delete` con foto que falla: la fila sigue; `UserService.update` con rol inexistente: roles intactos |
| `integration/report/ReportServiceIT` | Rango de fechas, `invalid=false`, agrupaciones, evento 02:00Z = 21:00 Bogotá del día anterior y dos eventos de la misma fecha Bogotá con distinta fecha UTC en un solo grupo |
| `integration/dashboard/DashboardServiceIT` | Totales admin vs sede, cumpleaños, últimos 30 días, con `Clock` fijo |
| `integration/ai/SqlExecutorIT` | `SELECT` válido, `maxRows`, `queryTimeout`, intento de DML en transacción read-only |

Notas:

- `@DataJpaTest` es transaccional por defecto y puede ocultar problemas de lazy/flush. Si se usa, añadir `@AutoConfigureTestDatabase(replace = NONE)`, extender `AbstractIntegrationTest` e importar las Specifications/`DateTimeUtils`; para servicios y lazy usar `@SpringBootTest` sin `@Transactional` y limpiar datos con un `DatabaseCleaner` o `@Sql` en orden de FKs.
- No afirmar unicidad de `cc`: solo hay índice `idx_cc`, no constraint único. Los únicos reales son `users.username`, `users.member_id`, `member_categories.name`, `member_types.name`, `refresh_token.token`, `user_role(role_id,user_id)` y la clave compuesta de `category_rules`.
- El `id` se captura tras `saveAndFlush`; no se hardcodea. Para `role_permission`, guardar primero el `Role`, luego construir `RolePermissionId` con `role.getId()`.
- `ddl-auto=create-drop` crea `role_permission` desde la entidad; en producción esa tabla debe existir y estar sembrada. El test no sustituye esa verificación.

### 3.4 IA

`unit/ai/SqlQueryValidatorTest` con tabla de casos. El contrato esperado es lista blanca de tablas/columnas, no otra lista negra:

| Entrada | Esperado |
|---|---|
| `SELECT * FROM members` | aceptar |
| `WITH x AS (SELECT 1) SELECT * FROM x` | aceptar (el prompt usa `WITH`) |
| `SELECT * FROM users` / `SELECT password_hash FROM users` | rechazar |
| `SELECT * FROM information_schema.tables` | rechazar |
| `SELECT pg_sleep(10)` | rechazar |
| `SELECT * FROM members UNION SELECT password_hash FROM users` | rechazar |
| `SELECT 1 -- comentario` / `/* x */ SELECT 1` | rechazar |
| `SELECT * FROM members; DROP TABLE members` | rechazar |
| `SeLeCt * FROM members` con espacios/saltos | aceptar (lista blanca insensible a mayúsculas); `SeLeCt * FROM users` rechazar |

`unit/ai/AiServiceTest` con `MockRestServiceServer`:

```java
var server = MockRestServiceServer.bindTo(restTemplate).build();
server.expect(requestTo(containsString(":generateContent")))
      .andRespond(withSuccess(geminiJson, MediaType.APPLICATION_JSON));
```

Casos: SQL válido + JSON final; SQL rechazado no llama a `SqlExecutor`; Gemini 500 no devuelve 500 al cliente (contrato a decidir: 502 o respuesta amable); JSON final inválido cae al fallback sin excepción. En unitario, inyectar `apiKey`/`modelId` con `ReflectionTestUtils.setField`. En `web/ai/AiControllerSecurityTest`, capturar con `ArgumentCaptor` el `User` que recibe `processChat`: debe ser el autenticado, no `new User()`.

`integration/ai/SqlExecutorIT`: `maxRows` y `queryTimeout` sobre una instancia dedicada de `JdbcTemplate` (no mutar el singleton); `SELECT generate_series(1,10000)` devuelve como máximo el límite; `SELECT pg_sleep(5)` lanza timeout; el DML en transacción read-only se rechaza o se documenta como no verificable.

## 4. Datos de prueba

`support/TestDataFactory` con builders; dos sedes, permisos y fechas fijas.

```java
public final class TestDataFactory {
    public static Branch branch(String name) { /* address, city, cellphone, createdAt */ }
    public static Member member(Branch b, String name, boolean active) { /* completeName, createdAt */ }
    public static Role role(String name, AppPermission... perms) { /* color, description, RolePermission */ }
    public static User user(Member m, String username, Role... roles) { /* passwordHash, active, createdAt */ }
    public static Services service(String name) { /* createdAt, description, active */ }
    public static ServiceEvent event(Branch b, Member createdBy, Services s,
                                     OffsetDateTime start, OffsetDateTime end) { }
}
```

**Conjunto base**

| Dato | Valores |
|---|---|
| Sedes | `Sede A`, `Sede B` (UUID capturados) |
| Usuarios | `admin-a` (`ADMINISTRATOR`), `operador-a` (permisos de negocio), `operador-b`, `inactivo-a` |
| Roles | `ADMINISTRADOR`, `OPERADOR`; permisos `CREATE_MEMBER`, `MANAGE_ATTENDANCE`, `DISCIPULADO_PROGRESS`, etc. |
| Integrantes | A1/A2 activos en A, B1 activo en B, A3 inactivo |
| Catálogos | categoría/subcategoría/tipo mínimos; `MemberCategory` y `MemberType` con nombre único |
| Eventos y asistencias | Eventos A pasado/activo/futuro y B activo; asistencias A válida, A inválida, B válida |
| Bautismo y discipulado | Bautismo A y B; discipulado A con 13 `DiscipuladoProgress` |

**`role_permission`:** no está en el repo como datos. Sembrar con JPA en este orden: `branchRepository.saveAndFlush` → `memberRepository.saveAndFlush` → `roleRepository.saveAndFlush` → construir `RolePermission` con `new RolePermissionId(permission.name(), role.getId())` y añadirlo a `role.getPermissions()` → `saveAndFlush` → `userRepository.saveAndFlush` (sin roles) → construir `UserRole` con `user.getId()` y `role.getId()`, añadirlo a `user.getRoles()` → `saveAndFlush`. Si se prefiere SQL, usar `JdbcTemplate` después de los roles; no depender del orden de `@Sql` respecto a JPA.

**Reloj determinista (cambio de producción necesario):** añadir un bean `Clock` (`Clock.systemUTC()` en producción) e inyectarlo en `DateTimeUtils`; reemplazar `LocalDateTime.now(zone)`/`OffsetDateTime.now(UTC)` por `now(clock)`. `MemberSpecification` usa `LocalDate.now()` y `DashboardService`/`AiService` usan `now()` directo: deben pasar por `DateTimeUtils`/`Clock`. `JwtService` usa `System.currentTimeMillis`; para probar expiración, inyectar `Clock` o generar un token de vida corta. En test, `@TestConfiguration @Primary Clock fixed = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), UTC)`. Es el único cambio de `src/main` que este plan requiere; no se implementa aquí.

**IDs y fechas:** capturar IDs de `saveAndFlush` para entidades; usar UUID fijos solo en fixtures de path que no consultan BD. Fechas canónicas: `2026-01-15T02:00:00Z` (14 de enero 21:00 en Bogotá), `2026-06-15T15:00:00Z`, `2026-01-15` para `LocalDate`. Evitar `LocalDate.now()` en aserciones.

## 5. Trazabilidad

| Hallazgo | Tipo | Archivo propuesto | Aserción |
|---|---|---|---|
| H-01 | Integración/unit IA | `unit/ai/SqlQueryValidatorTest`, `integration/ai/SqlExecutorIT`, `web/ai/AiControllerSecurityTest` | Rechaza `users.password_hash`, `information_schema`, `pg_sleep`, `UNION` con tablas no permitidas, comentarios y `;`; límite de filas/timeout; recibe el usuario real |
| H-02 | Integración | `integration/attendance/AttendanceExportIT` | Export sin cuerpo como sede B devuelve solo B; admin ve A+B |
| H-03 | Unitario/web | `unit/discipulado/DiscipuladoServiceTest`, `web/discipulado/DiscipuladoProgressUserTest` | `progress` recibe el usuario autenticado (no `null`) y rechaza integrante/maestro de otra sede → 409; admin permitido |
| H-04 | Unitario | `unit/user/UserServiceTest` | No-admin no asigna rol `ADMINISTRATOR` ni crea rol con ese permiso |
| H-05 | Unitario/integración | `unit/user/UserServiceTest`, `integration/user/UserRoleIT` | No-admin no modifica admin; `memberId` nuevo de otra sede → 409; reset de contraseña revoca refresh tokens |
| ~~H-06~~ | **Descartado** | — | Regla de negocio (2026-10-24): un integrante de otra sede puede asistir a un evento de otra sede; no debe probarse el rechazo. Ver `00-aclaraciones-negocio.md`. |
| H-07 | Integración | `integration/baptism/BaptismPersistenceIT` | `note` vuelve en el DTO; `municipality` se persiste en la entidad (no está en `BaptismResponseDto`) |
| H-08 | Unitario | `unit/baptism/BaptismServiceTest` | Dos `Branch` con el mismo id permiten el bautismo; no se compara con `equals` |
| H-09 | Web/SpEL | `web/security/PreAuthorizeSpelTest` | Las 65 expresiones parsean; `DELETE /api/users/{id}` evalúa sin excepción |
| H-10 | Web | `web/validation/NullableBodyTest` | Cuerpo nulo → 200/400 controlado, nunca 500 |
| H-11 | Web | `web/error/GlobalHandlerExceptionTest` | Permiso denegado → 403; entrada inválida → 400; error interno → 500 genérico |
| H-12 | Web | `web/security/JwtAuthFilterTest` | Firma inválida/usuario borrado → 401, no 500 |
| H-26/H-27 | Integración | `integration/time/TimezoneContractIT`, `integration/report/ReportServiceIT` | Evento 02:00Z aparece el día anterior en Bogotá; calendario y filtro de discipulado interpretan hora local |
| H-28 | Web | `web/validation/ValidationContractTest` | Enum/fecha/boolean inválidos y DTOs anidados → 400 |
| H-31 | Unitario | `unit/member/MemberServiceTest` | `delete` llama a `pictureUtils.delete(publicId)`, nunca con la URL |
| H-41 | Unitario | `unit/attendance/AttendanceServiceTest` | Lote con dos eventos: cada fila queda en su `serviceEventId` |
| H-43 | Integración | `integration/member/MemberSpecificationIT` | Edad en el último año del rango no se pierde |
| H-46 | Unitario | `unit/ai/RestTemplateConfigTest` | El `RestTemplate` tiene timeouts de conexión/lectura finitos |
| H-48 | Web | `web/security/EndpointSecurityMatrixTest` | `DELETE /api/services/{id}` con admin inactivo → 403, no 204 |
| H-49 | Web | `web/security/AuthEndpointSecurityTest` | `logout`/`changge-password` con usuario inactivo → 403 |

## 6. Fases de implementación

| Fase | Días | Contenido | Depende de | Criterio de aceptación |
|---|---:|---|---|---|
| **0. Red mínima** | 2–3 | Dependencias, `application-test.properties`, `AbstractIntegrationTest`, init script, `ApplicationContextIT`, 3 unitarios, `PreAuthorizeSpelTest`, CI esqueleto | — | `.\gradlew.bat test` pasa con JDK 21 y Docker, **sin** variables de entorno; `compileJava` sigue pasando |
| **1. Seguridad y multi-sede** | 5–6 | Matriz de 69 endpoints, principal de dominio, JWT filter, error mapping, suites cruzadas H-02/H-03/H-04/H-05/H-08/H-09/H-48/H-49 | Fase 0 | 100 % de handlers con fila en la matriz; cada hallazgo alto tiene al menos un test; los `@Tag("hallazgo")` pendientes están listados |
| **2. Datos, fechas y transacciones** | 4–5 | Specs/repos con `unaccent`, paginación, constraints, lazy, rollback, `Clock`, report/dashboard, edad | Fase 0; puede ir en paralelo con 1 | Specs y repos cubiertos; sin `LazyInitializationException`; tests de fecha deterministas y repetibles |
| **3. IA, funcional y CI** | 3–4 | Corpus del validador, `MockRestServiceServer`, límites del ejecutor, bautismo/validación, JaCoCo, CI completo, documentación | Fases 0–2 | CI verde en `push`/`PR`; umbral de cobertura; no quedan hallazgos altos sin prueba o justificación |

**Orden:** 0 es bloqueante. 1 y 2 pueden solaparse si se comparten fixtures. 3 cierra la deuda y activa el gate de cobertura. Total estimado: **14–18 días**.

## 7. CI

Hoy **no hay CI** en el repo. Pipeline mínimo, `.github/workflows/ci.yml`:

```yaml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: gradle
      - uses: gradle/actions/setup-gradle@v4
      - run: docker info
      - run: ./gradlew test --no-daemon
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: build/reports/tests/test
```

- `ubuntu-latest` ya tiene Docker; Testcontainers lo usa sin `services:`. Caché de Gradle con `setup-gradle`; no cachear imágenes Docker en el runner.
- JDK 21 fijo en CI. En local, documentar `JAVA_HOME`; no commitear rutas de máquina.
- Cobertura: añadir plugin `jacoco`, `jacocoTestReport` y `jacocoTestCoverageVerification`. Umbral inicial: línea 45 % global, 70 % en `features.*.service`, 75 % en `configurations`/`utils.specs`, 80 % en `SqlQueryValidator`; excluir `**/mapper/**`, `**/dtos/**`, `**/*Application*` y `**/*Impl` de MapStruct. Subir el umbral por trimestre, no fijarlo en 80 % global desde el día uno.
- Opcional fase 3: `dependencyCheckAnalyze` (H-51) y Renovate; no bloquear el primer pipeline.

## 8. Métricas y riesgos

| Capa | Objetivo realista | Motivo |
|---|---:|---|
| Global (línea) | 45–55 % | Hay mucho DTO/mapper generado y código de arranque |
| Services con lógica | 70–80 % | Sede, fechas, estados y excepciones |
| Specs y repositorios | 65–75 % | `unaccent`, paginación y joins |
| Controllers | 60–70 % | La matriz cubre seguridad; no cada rama de negocio |
| Seguridad/IA | 80 % en ramas del validador y del filtro | Es la superficie de mayor riesgo |

**Trampas conocidas**

| Riesgo | Mitigación |
|---|---|
| `unaccent` ausente | `withInitScript` antes de Hibernate; test `José`/`jose` que falle si falta |
| Deriva `create-drop` vs producción | `SchemaContractIT` nocturno con dump real y `ddl-auto=validate` |
| UUID generados | Capturar `getId()` tras `saveAndFlush`; no hardcodear IDs de entidades |
| UTC vs Bogotá | `Clock` fijo; probar evento 02:00Z y `DATE()`/`timestamptz`; revisar zona de sesión PostgreSQL |
| `open-in-view=false` | Test de lazy sin `@Transactional`; mapear DTO fuera del service |
| `SecurityService.isActive` | Mockear en slices; integration con usuario inactivo real |
| MapStruct/Lombok | Excluir de cobertura; no testear `*Impl` |
| `@Transactional` en test | Rollback oculta flush; usar limpieza explícita en tests de transacción/lazy |
| Windows/Docker | Docker Desktop/WSL2; JDK 21; no usar el JDK 25 del sistema |
| `role_permission` externa | Sembrar en test; verificar en BD real antes de concluir H-04/H-05 |
| `@ModelAttribute` | Usar `multipart()`/`param()`, no JSON |
| SpEL `T(...)` | Parsear y evaluar con `BeanFactoryResolver`; no confiar en el arranque |
| `CURRENT_DATE` en SQL nativo | Fijar zona del contenedor o calcular el esperado con la fecha de la BD |
| Gemini/Cloudinary reales | `MockRestServiceServer`, mocks; nunca llamar a APIs externas en `test` |

**Pruebas frágiles o de bajo valor (evitar)**

- Snapshot completo de JSON o comparación de mensajes exactos en español.
- Tests de getters/setters, builders de Lombok o clases `*Impl` de MapStruct.
- Mocks de `CriteriaBuilder`/`CriteriaQuery` para “probar” una Specification; mejor integración real.
- `Thread.sleep` para ventanas horarias o timeouts; usar `Clock` y timeouts cortos configurados.
- Afirmar unicidad de `cc` o de campos sin constraint en la BD.
- Un `@SpringBootTest` por controller para probar solo el 200.
- Tests que dependan del orden de ejecución o de datos dejados por otra clase.


