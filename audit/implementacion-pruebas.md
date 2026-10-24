# Implementación de pruebas — Fase 0

**Rama:** `audit/arquitectura-y-seguridad` · **Fecha:** 2026-10-23 ·
**Alcance:** convertir `audit/plan-pruebas.md` en una red ejecutable, sin tocar código de producción
y sin depender de variables de entorno reales ni de Docker.

## 1. Resumen

| Prioridad | Estado | Resultado |
|---|---|---|
| P1 — red mínima sin variables de entorno | **Completo** | `compileJava` y `test` salen con exit 0 sin `DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, Cloudinary ni `GEMINI_API_KEY`. |
| P2 — `PreAuthorizeSpelTest` | **Completo** | Evalúa las **65** expresiones con un `GenericApplicationContext`; detecta H-09 y H-48. |
| P3 — unitarios de fixes | **Completo** | `BaptismServiceTest` (H-07/H-08), `MemberServiceTest` (H-31), `DiscipuladoServiceTest` (H-03 documentado), `DateTimeUtilsTest`. |
| P4 — inventario/matriz | **Parcial entregado** | CSV con los **69** handlers y test estático de completitud; matriz ejecutada sobre un subconjunto representativo (usuarios, servicios y health). |
| P5 — documentación | **Completo** | Este documento. |

Resultado final de la suite:

```text
32 tests, 28 ejecutados, 4 saltados, 0 fallos, 0 errores
```

Los 4 saltados son: `ApplicationContextIT` (Docker apagado), dos pruebas `@Disabled` de H-03 y una
prueba `@Disabled` de H-11.

## 2. Archivos

### 2.1 Nuevos

| Archivo | Propósito |
|---|---|
| `src/test/resources/application-test.properties` | Perfil `test` con valores literales; `ddl-auto=create-drop`; secreto JWT de más de 32 bytes; Cloudinary/Gemini falsos. |
| `src/test/resources/db/testcontainers-init.sql` | `CREATE EXTENSION IF NOT EXISTS unaccent;` antes de que Hibernate cree el esquema. |
| `src/test/resources/security/endpoint-matrix.csv` | Inventario de los 69 handlers con columnas `controller,httpMethod,path,authority,bodyFixture,expectedWithoutPermission`. |
| `src/test/java/org/icc/pecesatierra/support/AbstractIntegrationTest.java` | `@Testcontainers(disabledWithoutDocker = true)`, `@SpringBootTest`, `@ActiveProfiles("test")`, PostgreSQL 16 con `@ServiceConnection` e init script. |
| `src/test/java/org/icc/pecesatierra/ApplicationContextIT.java` | `contextLoads` real contra PostgreSQL cuando hay Docker; se salta solo si no lo hay. |
| `src/test/java/org/icc/pecesatierra/support/TestFixtures.java` | Builders de `Branch`, `Member`, `User`, roles y permisos para unitarios; sin Spring y sin BD. |
| `src/test/java/org/icc/pecesatierra/unit/time/DateTimeUtilsTest.java` | Conversiones UTC ↔ `America/Bogota`, cambio de día y nulos (6 pruebas). |
| `src/test/java/org/icc/pecesatierra/unit/baptism/BaptismServiceTest.java` | H-07 (nota y municipio), H-08 (dos `Branch` con el mismo id), rechazo entre sedes y admin (4 pruebas). |
| `src/test/java/org/icc/pecesatierra/unit/member/MemberServiceTest.java` | H-31 (`delete(publicId)`, nunca la URL), rechazo entre sedes y foto heredada sin `publicId` (3 pruebas). |
| `src/test/java/org/icc/pecesatierra/unit/discipulado/DiscipuladoServiceTest.java` | Camino feliz de `progress`, H-03 pendiente y comportamiento actual documentado (4 pruebas, 2 `@Disabled`). |
| `src/test/java/org/icc/pecesatierra/web/security/PreAuthorizeSpelTest.java` | Evaluación real de las 65 expresiones; H-09; precedencia de H-48 (3 pruebas). |
| `src/test/java/org/icc/pecesatierra/web/security/EndpointInventoryTest.java` | Reflexión + `@RequestMapping` contra el CSV; falla si falta o sobra una fila (2 pruebas). |
| `src/test/java/org/icc/pecesatierra/web/security/EndpointSecurityMatrixTest.java` | Matriz representativa con principal de dominio, `SecurityConfig` y `@MockitoBean` (9 pruebas, 1 `@Disabled` por H-11). |

### 2.2 Modificados

| Archivo | Cambio |
|---|---|
| `build.gradle` | Cuatro `testImplementation` sin versión, gestionadas por el BOM de Boot 3.5.9: `spring-security-test`, `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`. |
| `src/test/java/org/icc/pecesatierra/PecesATierraApplicationTests.java` | **Eliminado**; su función la cubre `ApplicationContextIT` con perfil `test`. |

No se modificó `src/main/**`, `src/main/resources/application.properties`, los informes de auditoría,
ni se hizo commit, stash o cambio de rama. Los cambios de `src/main` que aparecen en `git status`
son los siete arreglos de la Fase 0 ya existentes, ajenos a esta tarea.

## 3. Cómo ejecutarlo

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
Remove-Item Env:DB_URL,Env:DB_USER,Env:DB_PASS,Env:JWT_SECRET,`
            Env:CLOUDINARY_CLOUD_NAME,Env:CLOUDINARY_API_KEY,`
            Env:CLOUDINARY_API_SECRET,Env:GEMINI_API_KEY -ErrorAction SilentlyContinue

.\gradlew.bat compileJava
.\gradlew.bat test
```

No hace falta PostgreSQL local ni Docker para que `test` termine en verde: `ApplicationContextIT`
queda saltado por `disabledWithoutDocker = true`. Para ejecutar la integración real, arrancar Docker
Desktop y correr:

```powershell
.\gradlew.bat test --tests "org.icc.pecesatierra.ApplicationContextIT"
```

El perfil `test` sustituye las propiedades obligatorias de `application.properties`; el contenedor
aporta URL, usuario y contraseña mediante `@ServiceConnection`.

## 4. Verificación ejecutada

Comandos exactos, con las variables de entorno de base de datos, JWT, Cloudinary y Gemini
**eliminadas** del proceso y sin daemon reutilizado:

```text
> Task :compileJava UP-TO-DATE
BUILD SUCCESSFUL in 17s
COMPILE_EXIT=0

> Task :test
BUILD SUCCESSFUL in 25s
TEST_EXIT=0
```

Detalle por suite:

| Suite | Tests | Ejecutados | Saltados | Fallos |
|---|---:|---:|---:|---:|
| `ApplicationContextIT` | 1 | 0 | 1 | 0 |
| `BaptismServiceTest` | 4 | 4 | 0 | 0 |
| `DiscipuladoServiceTest` | 4 | 2 | 2 | 0 |
| `MemberServiceTest` | 3 | 3 | 0 | 0 |
| `DateTimeUtilsTest` | 6 | 6 | 0 | 0 |
| `EndpointInventoryTest` | 2 | 2 | 0 | 0 |
| `EndpointSecurityMatrixTest` | 9 | 8 | 1 | 0 |
| `PreAuthorizeSpelTest` | 3 | 3 | 0 | 0 |
| **Total** | **32** | **28** | **4** | **0** |

## 5. Cobertura del plan

### P1 — Red mínima

- Dependencias de test añadidas sin versión explícita.
- Perfil `test` completo y `unaccent` creado por `withInitScript`.
- `AbstractIntegrationTest` reutilizable; contexto cacheado por una única clase de contenedor.
- `ApplicationContextIT` sustituye el antiguo `contextLoads` que exigía el entorno real.
- `@MockBean` no se usa en ninguna prueba; se usa `@MockitoBean`.
- Sin H2 y sin cambios en `application.properties`.

### P2 — SpEL de seguridad

`PreAuthorizeSpelTest` no arranca Spring Boot:

1. Escanea los `@RestController` de `org.icc.pecesatierra.features`.
2. Cuenta **65** métodos con `@PreAuthorize` (falla si el inventario cambia).
3. Crea un `GenericApplicationContext`, registra un mock de `SecurityService` como bean
   `securityService`, refresca y configura `DefaultMethodSecurityExpressionHandler`.
4. Para cada método crea un `MethodInvocation` Mockito, obtiene el contexto con
   `createEvaluationContext(authentication, invocation)`, registra `#refreshTokenRequestDto` y
   **evalúa** la expresión.
5. Con `ADMINISTRATOR` y todos los permisos, todas las expresiones deben devolver `true`.
6. H-09: `UserController.delete` autoriza con `DELETE_USER`, autoriza con `ADMINISTRATOR`, deniega
   sin permiso y deniega con la cuenta inactiva.
7. H-48: `ServiceController.delete` deniega con `DELETE_SERVICE` e `isActive=false`; con la expresión
   original (`DELETE_SERVICE || ADMINISTRATOR && isActive`) esa combinación devolvía `true` porque
   SpEL evalúa `&&` antes que `||`. La prueba distingue el defecto de la corrección.

### P3 — Unitarios de los fixes

- **H-07:** `ArgumentCaptor<Baptism>` sobre `baptismRepository.save`; verifica `note`, `municipality`,
  `city`, `neighborhood` y `createdAt`. Con el código anterior `note` habría sido `null`.
- **H-08:** dos instancias distintas de `Branch` con el mismo `id` permiten crear el bautismo;
  además se prueba el rechazo entre sedes y el camino de administrador.
- **H-31:** `MemberService.delete` llama a `pictureUtils.delete("members/photos/abc123")` y nunca a
  la URL de Cloudinary; también se cubre el caso de foto heredada sin `publicId`.
- **H-03:** el servicio actual sigue ignorando el usuario en `progress` y `updateProgress`; por eso
  hay dos pruebas `@Disabled` con la expectativa correcta (409) y una prueba etiquetada
  `deuda-h03` que documenta el comportamiento actual. Cuando H-03 se corrija, esa prueba fallará
  y habrá que habilitar las dos `@Disabled`.
- **`DateTimeUtils`:** conversiones con cambio de día (23:30 Bogotá → 04:30Z), 02:00Z → 21:00 del día
  anterior, ida y vuelta, nulos y zona de `nowColombia`/`nowUTC`.

### P4 — Inventario y matriz

- `endpoint-matrix.csv` contiene los **69** handlers (65 con `@PreAuthorize`, 4 públicos/autenticados
  sin anotación: login, logout, change-password y health).
- `EndpointInventoryTest` obtiene los handlers por reflexión sobre `@RequestMapping`/`@*Mapping` y
  compara controller + verbo + ruta contra el CSV. Falla si falta una fila o si sobra una.
- `EndpointSecurityMatrixTest` ejecuta un subconjunto representativo:
  - `DELETE /api/users/{userId}`: sin permiso → denegado; con `DELETE_USER` → 204; con la cuenta
    inactiva → denegado.
  - `DELETE /api/services/{serviceId}`: con `DELETE_SERVICE` e inactivo → denegado (H-48); activo →
    204.
  - `GET /api/services?onlyActive=true`: sin permiso → denegado; con `VIEW_SERVICE_PANEL` → 200.
  - `GET /api/health`: público → 200.
- La matriz usa un `User` de dominio como principal (no `@WithMockUser`), `SecurityConfig` importado,
  `SecurityService`, `AuthenticationProvider` y `JwtAuthFilter` mockeados con `@MockitoBean`. El mock
  del filtro deja pasar la cadena; si no, MockMvc devolvería respuestas vacías con 200.
- El resto de filas del CSV queda con `expectedWithoutPermission=pending`; no se simula un resultado
  que no se haya ejecutado.

## 6. Hallazgos, decisiones y huecos

### Confirmados durante la implementación

1. **H-03 sigue abierto.** `DiscipuladoService.progress` y `updateProgress` reciben el usuario pero no
   lo usan: no comparan `discipulado.getMember().getBranch().getId()` con la sede del usuario. Un
   operador de la sede A puede completar el progreso de un integrante de la sede B y asignar un
   maestro de B. Las pruebas `rejectsProgressForMemberFromAnotherBranch_H03` y
   `rejectsUpdateProgressForMemberFromAnotherBranch_H03` quedan `@Disabled`.
2. **H-11 sigue abierto.** `GlobalHandlerException` mapea `AuthorizationDeniedException` a **401**,
   no a 403. La matriz acepta 401/403 y comprueba que el servicio no se invoca; hay una prueba
   `@Disabled` con la expectativa correcta (403). No se debilitó la aserción de autorización: la
   operación sigue sin ejecutarse.
3. **H-48: matiz sobre el informe.** La expresión original era
   `DELETE_SERVICE || ADMINISTRATOR && isActive`. Con la precedencia de SpEL (`&&` antes que `||`),
   el bypass lo sufría un usuario con `DELETE_SERVICE` e `isActive=false`; un administrador sin
   `DELETE_SERVICE` sí quedaba sujeto a `isActive`. La prueba de H-48 usa precisamente
   `DELETE_SERVICE` + inactivo, que es la combinación que distingue la corrección.
4. **Infraestructura de `@WebMvcTest`.** El `@ComponentScan(basePackages = ...)` explícito de
   `PecesATierraApplication` anula el filtro de tipos de `@WebMvcTest(controllers = ...)`: el slice
   intenta crear los 19 controllers y falla por dependencias como `JdbcTemplate`. La solución fue
   `@ContextConfiguration(classes = {UserController, ServiceController, HealthController,
   SecurityConfig, GlobalHandlerException})`.
5. **Nombre del mock de `SecurityService`.** Para que `@securityService` resuelva en la expresión,
   el `@MockitoBean` debe declarar `name = "securityService"`; con el nombre inferido del campo, el
   `BeanFactoryResolver` no encontraba el bean.

### Pendiente (no implementado aquí)

| Hueco | Motivo / siguiente paso |
|---|---|
| Matriz completa de 69 endpoints | Solo se ejecutó el subconjunto de usuarios, servicios y health. El CSV está completo y con estados `pending`; falta poblar `bodyFixture` y ejecutar cada fila con `@WebMvcTest` o integración. |
| H-02 (export sin cuerpo) | Requiere integración con dos sedes y `AttendanceSpecification`; no se ejecutó por no haber Docker. |
| H-01 (IA) | No se escribieron pruebas que codifiquen la vulnerabilidad. Falta el corpus de `SqlQueryValidator` y `SqlExecutor` con límites. |
| H-04, H-05, H-10, H-12, H-26/H-27, H-28, H-41, H-43, H-46, H-49 | Fuera del alcance P1–P4; quedan como deuda de las fases 1–3 del plan. |
| H-06 | Descartado por regla de negocio el 2026-10-24: la asistencia inter-sede está permitida, así que no debe probarse el rechazo. Ver `00-aclaraciones-negocio.md`. |
| `Clock` determinista | `DateTimeUtils` no inyecta `Clock`; cambiarlo es una modificación de producción que esta tarea no permite. Por eso no se hacen aserciones exactas sobre `now()`. |
| Integración real | `ApplicationContextIT` está listo, pero no se pudo ejecutar: Docker no está corriendo en esta máquina. Queda verificarlo en CI o en una máquina con Docker. |
| CI y cobertura | No se añadió workflow ni JaCoCo; el plan lo reserva para la fase 3. |

### Nota sobre datos ya corruptos (H-07)

El arreglo de H-07 solo corrige escrituras nuevas. Los bautismos existentes pueden tener `note=null`
y `municipality=city`; hace falta un backfill manual. Ninguna prueba de esta entrega puede
reconstruir esos datos.
