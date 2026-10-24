<!-- estado-doc -->
> **Estado actual (2026-10-24).** La suite tiene 25 clases y 135 tests (131 ejecutados, 4 saltados sin Docker, 0 fallos). T-01..T-05 y los gaps M-08..M-14 ya están cerrados; el estado vigente está en `audit/INFORME-FINAL.md` y `audit/TODO.md`.

# Verificación de la implementación de pruebas

**Rama:** `audit/arquitectura-y-seguridad` · **Fecha:** 2026-10-23

> **Actualización 2026-10-24**: H-06 queda descartado por regla de negocio (un integrante de otra
> sede puede asistir a un evento de otra sede). Se retira del backlog de pruebas; ver
> `audit/00-aclaraciones-negocio.md`.

**Alcance:** auditoría adversarial de `src/test`, `build.gradle` y de la infraestructura de pruebas descrita en `audit/implementacion-pruebas.md`. No se modificó `src/main`, `src/test`, `build.gradle` ni los informes existentes; las mutaciones se hicieron en una copia temporal.

## Veredicto global

**PARCIAL — la suite NO es todavía una red de seguridad fiable.**

La ejecución es reproducible y verde (32 tests, 28 ejecutados, 4 saltados, 0 fallos), y las pruebas exigidas por la tarea para H-09, H-31, H-07 y H-48 **sí detectan sus regresiones**. Pero:

- 2 de los 7 fixes de Fase 0 no tienen ninguna prueba: **H-02** (`AttendanceSpecification`) y **H-46** (`RestTemplateConfig`). Revertir H-02 deja la suite completa en verde (M6).
- **H-03** no está protegido: las dos pruebas correctas están `@Disabled` y una prueba verde (`currentBehaviorAllowsCrossBranchProgress_H03`) **codifica el comportamiento vulnerable**.
- **H-08** está cubierto solo en `create`; la rama `invalid` no se prueba y su reversión deja el test verde (M5).
- La “matriz” HTTP ejecuta **4 de 69 handlers** y el test de las 65 expresiones `@PreAuthorize` solo comprueba que un superusuario con todos los permisos pasa; no comprueba la autoridad exigida por endpoint. Cambiar `DELETE_MEMBER` por `VIEW_MEMBER_PANEL` en `MemberController.delete` deja la suite completa en verde (M7).
- La única prueba de contexto completo (`ApplicationContextIT`) está **NO VERIFICABLE** porque Docker no está disponible; el verde de P1 es parcialmente vacío.

Es útil como smoke test y como regresión de tres fixes concretos; no como red de seguridad de autorización, multi-sede ni integración.

## Resultado de ejecución (independiente)

Comando ejecutado con JDK 21 y sin variables de entorno reales:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
Remove-Item Env:DB_URL,Env:DB_USER,Env:DB_PASS,Env:JWT_SECRET,`
            Env:CLOUDINARY_CLOUD_NAME,Env:CLOUDINARY_API_KEY,`
            Env:CLOUDINARY_API_SECRET,Env:GEMINI_API_KEY -ErrorAction SilentlyContinue
.\gradlew.bat cleanTest test --console=plain
```

Resultado:

```text
EXIT=0
BUILD SUCCESSFUL in 19s
TOTAL tests=32 failures=0 errors=0 skipped=4
```

- `.\gradlew.bat -version` confirmó **Launcher JVM 21.0.8** y daemon `C:\Program Files\Java\jdk-21`. El `java` del `PATH` es 25, pero Gradle usó 21.
- `.\gradlew.bat compileJava --offline` terminó con `BUILD SUCCESSFUL` (exit 0).
- `docker info` falla: `failed to connect to the docker API ... dockerDesktopLinuxEngine`. Por eso `ApplicationContextIT` se saltó.
- El estado reportado (**32/28/4/0**) coincide exactamente con `build/test-results/test/TEST-*.xml`.

Detalle por suite desde los XML:

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

## Tabla por suite

| Suite | Tests (total/ejec/skip) | ¿Prueba lo que dice? | ¿Falla si se revierte? | Veredicto |
|---|---:|---|---|---|
| `ApplicationContextIT` | 1 / 0 / 1 | Solo `contextLoads` (`ApplicationContextIT.java:12-15`); no hay aserciones de negocio. Depende de Docker y el contenedor no arrancó. | **NO VERIFICABLE** sin Docker. No cubre ninguno de los 7 fixes. | **NO VERIFICABLE** (smoke de contexto) |
| `BaptismServiceTest` | 4 / 4 / 0 | H-07: captura el `Baptism` guardado y comprueba `note`, `municipality`, `city` y `neighborhood` (`BaptismServiceTest.java:85-95`). H-08: dos `Branch` distintas con el mismo id (`:99-124`), rechazo entre sedes (`:127-145`) y admin (`:147-166`). El `note` de la respuesta se prueba contra un mapper **mockeado** (`:78-81`), no contra `BaptismMapper` real. | H-07: **sí** (M3). H-08 `create`: **sí** (M5b, falla en `:122`). H-08 `invalid` (`BaptismService.java:87`): **no**; su reversión deja 4/4 en verde (M5). | **PARCIAL** |
| `DiscipuladoServiceTest` | 4 / 2 / 2 | El único test ejecutado de camino feliz (`:67-86`) muta el `DiscipuladoProgress` y verifica fecha/maestro, pero no usa el usuario ni prueba la sede. Las dos pruebas correctas de H-03 están `@Disabled` (`:89-90`, `:127-128`). `currentBehaviorAllowsCrossBranchProgress_H03` (`:108-124`) **afirma que el cruce de sede está permitido**, es decir, convierte el bug en contrato verde. | H-03: **no**. Si se corrige el servicio, la prueba `deuda-h03` fallará, pero hoy no protege nada. | **NO CUMPLE / RIESGO** |
| `MemberServiceTest` | 3 / 3 / 0 | H-31: verifica `pictureUtils.delete("members/photos/abc123")` y **nunca** la URL (`MemberServiceTest.java:64-69`). Rechazo entre sedes (`:72-89`) y foto heredada sin `publicId` (`:91-106`). Es unitario, con repositorio y Cloudinary mockeados. | H-31: **sí**; M4 produce 2 fallos (`:67` y `:105`). | **CONFIRMADO** para H-31; alcance limitado a `delete`. |
| `DateTimeUtilsTest` | 6 / 6 / 0 | Conversiones exactas con cambio de día (`:17-24`), UTC→Bogotá (`:26-32`), ida y vuelta (`:34-39`) y nulos (`:41-45`). `nowUtcUsesUtcOffset` (`:47-50`) solo mira el offset; `nowColombiaUsesBusinessZone` (`:52-61`) compara dos lecturas de reloj real con tolerancia de 1 s. | Las conversiones puras **sí**. `nowUtcUsesUtcOffset` pasaría con un valor fijo de offset UTC; `nowColombiaUsesBusinessZone` no prueba el instante y es sensible al reloj real. No hay `Clock`. | **PARCIAL** |
| `EndpointInventoryTest` | 2 / 2 / 0 | Compara `controller+verbo+ruta` de los 69 handlers contra el CSV (`EndpointInventoryTest.java:38-52`, `:67-92`). Verificado: borrar la fila de `HealthController` hace fallar el test en `:49`. **No compara la columna `authority` ni `bodyFixture`**: solo lee las tres primeras columnas (`:111-116`). | La completitud estructural **sí**. Una autoridad incorrecta en el CSV o en el controller **no** se detecta. El CSV ya tiene filas `PENDING` (`endpoint-matrix.csv:31,38,65`) mientras los controllers exigen `isActive` (`MemberCategoryController.java:59-62`, `MemberTypeController.java:58-61`, `UserController.java:52-55`). | **CONFIRMADO (estructura) / PARCIAL (seguridad)** |
| `EndpointSecurityMatrixTest` | 9 / 8 / 1 | Cubre solo 4 handlers: `DELETE /api/users/{id}`, `DELETE /api/services/{id}`, `GET /api/services` y `GET /api/health` (`:102-181`). H-09 y H-48 se ejercitan de verdad. H-11 está `@Disabled` (`:169`) y `assertDenied` acepta **401 o 403** (`:194-200`), por lo que el contrato HTTP de denegación sigue sin verificarse. `JwtAuthFilter`, `SecurityService` y `AuthenticationProvider` están mockeados (`:74-99`); el `@ContextConfiguration` reducido (`:61-67`) excluye `MvcConfig` y los otros 16 controllers. | H-09/H-48: **sí** (la matriz también fallaría). H-11: **no**. Los otros 65 handlers: **no**. | **PARCIAL** |
| `PreAuthorizeSpelTest` | 3 / 3 / 0 | Parsea y evalúa las 65 expresiones (`:79-99`), y prueba específicamente H-09 (`:101-115`) y H-48 (`:117-127`). El test global usa un `Authentication` con **todos** los permisos y `isActive=true`, así que solo demuestra “un superusuario activo pasa”; no demuestra que cada endpoint exija su permiso ni que `isActive` sea obligatorio. | H-09: **sí** (M1). H-48: **sí** (M2). Cambiar `DELETE_MEMBER` por `VIEW_MEMBER_PANEL` en `MemberController.delete` deja **32/0/4 y 3/3 en verde** (M7). | **PARCIAL** |

## Pruebas de mutación

Todas las mutaciones se ejecutaron en `C:\Users\josef\AppData\Local\Temp\opencode\mutation-audit` (copia sin `build/`, `.gradle/`, `bin/`, `.git/`). El repo real no se tocó. Se restauró el archivo antes de la siguiente mutación.

| Mutación | Fix revertido | Suite | Resultado esperado | Resultado real | Veredicto |
|---|---|---|---|---|---|
| **M1** | `UserController.java:111`: quitar `T(...)` y volver a `hasAuthority(org...AppPermission).DELETE_USER.name()` | `PreAuthorizeSpelTest` | Fallar | **EXIT=1**; 2 fallos: `userDeleteRequiresDeleteUserAuthority_H09` en `:108` y `allPreAuthorizeExpressions...` en `:93`/`:141` (`SpelParseException`) | **BUENA** |
| **M2** | `ServiceController.java:55-61`: quitar los paréntesis de `DELETE_SERVICE \|\| ADMINISTRATOR && isActive` | `PreAuthorizeSpelTest` | Fallar | **EXIT=1**; `serviceDeleteWithDeleteServiceButInactiveUserIsDenied_H48` falla en `:123` | **BUENA** |
| **M3** | `BaptismService.java:66,68`: volver a `.neighborhood(getNote())` y `.municipality(getCity())` | `BaptismServiceTest` | Fallar | **EXIT=1**; falla `persistsNoteAndMunicipalityFromTheRequest_H07` en `:90` (municipality). La aserción de `neighborhood` (`:92`) no llegó a ejecutarse por el primer fallo, pero también habría fallado | **BUENA** |
| **M4** | `MemberService.java:204`: volver a `pictureUtils.delete(pictureUrl)` | `MemberServiceTest` | Fallar | **EXIT=1**; fallan `deleteSendsCloudinaryPublicIdInsteadOfUrl_H31` (`:67`) y `deleteWithoutPublicIdDoesNotCallCloudinary` (`:105`) | **BUENA** |
| **M5** (extra) | `BaptismService.java:87`: volver a `Branch.equals(...)` solo en `invalid` | `BaptismServiceTest` | Fallar | **EXIT=0**, 4/4 en verde | **MALA / HUECO** |
| **M5b** (extra) | `BaptismService.java:54`: volver a `Branch.equals(...)` en `create` | `BaptismServiceTest` | Fallar | **EXIT=1**; falla `twoBranchInstancesWithTheSameIdAreTheSameBranch_H08` en `:122` | **BUENA** |
| **M6** (extra) | `AttendanceSpecification.java:80-84`: devolver el predicado de sede dentro de `if (dto != null)` (H-02) | Suite completa | Fallar | **EXIT=0**, **32/0/4** | **MALA / HUECO** |
| **M7** (extra) | `MemberController.java:98`: cambiar `DELETE_MEMBER` por `VIEW_MEMBER_PANEL` | Suite completa | Fallar | **EXIT=0**, **32/0/4**; `PreAuthorizeSpelTest` 3/3 en verde | **MALA / HUECO** |
| **M8** (extra) | `RestTemplateConfig.java:13-18`: volver a `new RestTemplate()` sin timeouts (H-46) | Suite completa | Fallar | **EXIT=0**, **32/0/4** | **MALA / HUECO** |
| **CSV** (comprobación P4) | Borrar la fila `HealthController,GET,/api/health` de `endpoint-matrix.csv` | `EndpointInventoryTest` | Fallar | **EXIT=1**; falla `everyHandlerHasARowInTheEndpointMatrix` en `:49` (68 filas frente a 69 esperadas) | **BUENA** |

Conclusión de mutaciones: las cuatro exigidas son buenas; las cuatro adicionales revelan que H-02, H-46, la rama `invalid` de H-08 y la autorización por permiso de la mayoría de endpoints **no están protegidas**.

## Infraestructura

- **`application-test.properties`** (`src/test/resources/application-test.properties:1-21`): valores literales de prueba, sin secretos reales. `spring.jpa.hibernate.ddl-auto=create-drop` está **solo** en el perfil de test (`:7`); `application.properties` conserva `validate` (`src/main/resources/application.properties:6`) y no fue modificado (`git diff -- src/main/resources` vacío). `jwt.secret` de test supera los 32 bytes; Cloudinary y Gemini son valores falsos. La URL `localhost:5432/peces_test` es de test, no de producción.
- **`build.gradle`** (`build.gradle:23-28`): añade exactamente cuatro dependencias de test, sin versión y sin H2: `spring-security-test`, `spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:postgresql`. No añade JaCoCo ni workflow de CI. El `spring-boot-starter-web` duplicado de las líneas 22 y 36 es preexistente y ajeno a esta tarea.
- **`AbstractIntegrationTest`** (`support/AbstractIntegrationTest.java:18-30`): `@Testcontainers(disabledWithoutDocker = true)`, `@SpringBootTest`, `@ActiveProfiles("test")`, PostgreSQL 16-alpine, `@ServiceConnection` y `withInitScript("db/testcontainers-init.sql")` con `CREATE EXTENSION IF NOT EXISTS unaccent;`. El diseño es correcto para CI. **NO VERIFICABLE** aquí: Docker está apagado y el único test que lo usa se salta limpiamente. No se probó que el init script funcione en `postgres:16-alpine` ni que el esquema `create-drop` cubra el esquema de producción.
- **Salto limpio de ITs:** sí. La ejecución termina con exit 0 y un `<skipped/>` para `ApplicationContextIT`; no hay fallo de conexión. El costo es que el contexto completo nunca se carga en esta máquina y el verde de P1 no demuestra que el contexto arranque.
- **`EndpointInventoryTest`:** detecta de verdad una fila faltante (probado). También detecta un endpoint nuevo por el tamaño y por `missingInCsv`/`staleRows`. No valida la columna `authority`, `bodyFixture` ni `expectedWithoutPermission`; además, si dos handlers comparten controller+verbo+ruta, el `Set` los colapsa. Es un inventario estructural, no una matriz de autorización.
- **Matriz HTTP:** cubre `UserController`, `ServiceController` y `HealthController`; quedan fuera 16 controllers y 65 handlers, entre ellos `AuthController`, `MemberController`, `AttendanceController`, `BaptismController`, `DiscipuladoController`, `RoleController`, `ReportController` y `AiController`. El mocking de `JwtAuthFilter`, `SecurityService` y `AuthenticationProvider` evita probar JWT real, `isActive` real y la cadena completa. El `@ContextConfiguration` reducido no incluye `MvcConfig` ni el resto de beans; para los 4 handlers probados la seguridad de método y el `@RestControllerAdvice` son reales, pero no representa el contexto de producción.
- **Aislamiento y determinismo:** los unitarios no tocan BD ni red; usan `TestFixtures` y mocks. No hay `Thread.sleep` ni orden entre tests. `PreAuthorizeSpelTest` usa estado estático (`applicationContext`, `securityService`) y muta el stub de `isActive` en cada evaluación; no hay paralelismo, pero es un riesgo si se habilita. `DateTimeUtilsTest` sí usa reloj real en `nowUtcUsesUtcOffset`/`nowColombiaUsesBusinessZone` (`:47-61`), contra el principio de `Clock` fijo del plan.
- **Cobertura:** no hay JaCoCo ni informe de cobertura. **NO VERIFICABLE**.

## Cobertura del plan (P1–P4)

| Prioridad | Estado | Evidencia |
|---|---|---|
| **P1 — red mínima sin variables de entorno** | **PARCIAL** | `test` sale 0 sin `DB_URL`/`JWT_SECRET`/Cloudinary/Gemini, y `application-test.properties` + `AbstractIntegrationTest` existen. Pero `ApplicationContextIT` está saltado por Docker y no hay workflow de CI (`.github` no existe). El verde demuestra que los tests que corren no necesitan entorno, no que el contexto completo arranque. |
| **P2 — `PreAuthorizeSpelTest`** | **PARCIAL** | Evalúa las 65 expresiones y detecta H-09/H-48 (M1/M2). No valida la autoridad esperada por endpoint ni `isActive` en los otros 63: M7 deja la suite verde con `DELETE_MEMBER` convertido en `VIEW_MEMBER_PANEL`. La aserción textual de H-09 (`PreAuthorizeSpelTest.java:108`) acopla el test a `T(org...AppPermission)` y fallaría ante un refactor equivalente a `hasAuthority('DELETE_USER')`. |
| **P3 — unitarios de fixes** | **PARCIAL** | H-07 (entidad) y H-31 están bien; H-08 solo `create`; H-08 `invalid` no (M5); H-03 no está entregado: dos `@Disabled` y un test que afirma el bug (`DiscipuladoServiceTest.java:108-124`). `DateTimeUtilsTest` cubre conversiones, pero no hay `Clock` ni pruebas de H-26/H-27. |
| **P4 — inventario/matriz** | **PARCIAL** (como reconoce el informe) | CSV de 69 handlers y detección de fila faltante confirmados. La matriz ejecuta 4/69 handlers, H-11 sigue `@Disabled` y la columna `authority` del CSV no se usa. El propio `implementacion-pruebas.md` ya lo marca parcial; la realidad es aún más limitada que “subconjunto representativo” porque solo hay 4 handlers. |

Contraste con las fases del plan:

| Fase | Estado | Motivo |
|---|---|---|
| **Fase 0 — red mínima** | **PARCIAL** | Dependencias, perfil, base Testcontainers, `ApplicationContextIT` y SpEL existen; el contexto no se ejecutó y no hay CI. |
| **Fase 1 — seguridad y multi-sede** | **NO ENTREGADO / PARCIAL** | Solo 4 endpoints en la matriz; H-03, H-04, H-05, H-49 y H-12 sin pruebas activas; no hay `JwtAuthFilterTest`, `AuthEndpointSecurityTest`, `ValidationContractTest` ni `GlobalHandlerExceptionTest`. |
| **Fase 2 — datos, fechas y transacciones** | **NO ENTREGADO** | No hay `@DataJpaTest`, specs/repos con `unaccent`, lazy, rollback, `Clock`, `ReportServiceIT` ni `DashboardServiceIT`. |
| **Fase 3 — IA, funcional y CI** | **NO ENTREGADO** | No hay `SqlQueryValidatorTest`, `AiServiceTest`, `SqlExecutorIT`, JaCoCo ni CI. H-01 queda sin red. |

Cobertura de los 7 fixes de Fase 0:

| Fix | Prueba | Resultado |
|---|---|---|
| H-02 | Ninguna | **NO PROTEGIDO** (M6 verde) |
| H-07 | `BaptismServiceTest` (entidad) | **PARCIAL**; el DTO se prueba con mapper mockeado |
| H-08 | `BaptismServiceTest` (`create`) | **PARCIAL**; `invalid` no protegido (M5) |
| H-09 | `PreAuthorizeSpelTest` + matriz | **CONFIRMADO** |
| H-31 | `MemberServiceTest` | **CONFIRMADO** |
| H-46 | Ninguna | **NO PROTEGIDO** |
| H-48 | `PreAuthorizeSpelTest` + matriz | **CONFIRMADO** |

## Riesgos de falsa confianza

1. **H-03 convertido en test verde.** `DiscipuladoServiceTest.java:108-124` afirma que un operador de la sede A puede completar el progreso de un integrante de la sede B. La suite está verde con una vulnerabilidad de autorización/multi-sede. Las pruebas que sí la detectarían están `@Disabled` (`:89-90`, `:127-128`). Un lector del informe puede creer que H-03 está “documentado” cuando en realidad está **codificado como comportamiento esperado**.
2. **H-02 sin ninguna prueba.** `AttendanceSpecification.java:80-84` no aparece en `src/test`. Revertir el fix deja **32/0/4** (M6). Es el hallazgo de scoping por sede más grave sin red.
3. **H-46 sin ninguna prueba.** `RestTemplateConfig` no se referencia en `src/test`; con Docker apagado el contexto ni se carga. Revertir a `new RestTemplate()` deja **32/0/4** (M8). Una regresión a timeouts infinitos no se detecta.
4. **La “evaluación de las 65 expresiones” no valida permisos.** `PreAuthorizeSpelTest.java:80-99` usa un `Authentication` con todos los permisos y `isActive=true`. M7 demuestra que `MemberController.delete` puede aceptar `VIEW_MEMBER_PANEL` en lugar de `DELETE_MEMBER` y la suite sigue verde. Lo mismo ocurriría si se elimina `isActive` de cualquiera de los 63 endpoints sin prueba específica.
5. **H-11 aceptado como 401.** `EndpointSecurityMatrixTest.java:194-200` acepta 401/403 y la prueba de 403 está `@Disabled` (`:169`). `GlobalHandlerException.java:61-65` mapea `AuthorizationDeniedException` a 401; el contrato de autorización sigue roto y la suite lo tolera.
6. **H-07 DTO probado con un mock.** `BaptismServiceTest.java:78-81` construye un `BaptismResponseDto` a mano con `baptism.getNote()`; la aserción `response.getNote()` (`:95`) prueba el stub, no `BaptismMapper`. Un fallo real de mapeo de `note` no se detectaría.
7. **H-08 `invalid` sin prueba.** M5 deja 4/4 verde; solo `create` está protegido.
8. **Matriz de 4/69.** El nombre “matriz” sugiere cobertura total, pero solo se ejecutan 4 handlers. `AuthController`, `MemberController`, `AttendanceController`, `DiscipuladoController`, `AiController`, etc. no tienen ningún caso de autorización.
9. **Inventario con autoridad no validada.** `EndpointInventoryTest.java:111-116` ignora `authority`; el CSV ya tiene `PENDING` en `endpoint-matrix.csv:31,38,65` frente a `isActive` real. El inventario puede estar “completo” y la autorización mal documentada.
10. **`ApplicationContextIT` saltado.** El único test de contexto completo no corre; el verde de P1 no prueba que el contexto arranque con Testcontainers. Es un **NO VERIFICABLE**, no un aprobado.
11. **`DateTimeUtilsTest` con reloj real.** `:47-61` no usa `Clock`; la tolerancia de 1 s puede ser flaky bajo carga y no verifica el instante. El plan exige determinismo con `Clock` fijo.
12. **H-09 acoplado al texto.** `PreAuthorizeSpelTest.java:108` exige literalmente `T(org.icc...AppPermission).DELETE_USER`; un refactor correcto a `hasAuthority('DELETE_USER')` haría fallar la prueba aunque el comportamiento sea correcto.

## Huecos prioritarios (T-01…)

| ID | Hueco | Prioridad | Evidencia / acción |
|---|---|---|---|
| **T-01** | H-03: `DiscipuladoService.progress` y `updateProgress` ignoran el usuario; no comparan sede del integrante ni del maestro | **Crítica** | `DiscipuladoService.java:131-158`; habilitar `:89-90`/`:127-128`, eliminar o invertir `currentBehavior...`, añadir caso de controller con principal real y admin. |
| **T-02** | H-02: `AttendanceSpecification` no tiene prueba; el scoping por sede puede volver a quedar dentro de `dto != null` | **Crítica** | `AttendanceSpecification.java:80-84`; M6 verde. Añadir IT con dos sedes, `dto=null`, admin vs operador, y `branchId` ajeno. |
| **T-03** | H-46: timeouts de `RestTemplate` sin prueba | **Alta** | `RestTemplateConfig.java:13-18`; test unitario que inspeccione `SimpleClientHttpRequestFactory` o un `MockRestServiceServer` con timeout corto. |
| **T-04** | H-08 `invalid` y duplicado de bautismo sin prueba | **Alta** | `BaptismService.java:87`; M5 verde. Añadir `invalid` entre sedes, admin y `existsBy...=true`. |
| **T-05** | H-07 DTO real y persistencia; el test usa mapper mockeado | **Alta** | `BaptismServiceTest.java:78-81`; usar `BaptismMapper` real o IT de persistencia; `municipality` no está en `BaptismResponseDto`, verificar entidad/repositorio. |
| **T-06** | Matriz de autorización real para 69 handlers, con `authority` y `bodyFixture` del CSV | **Crítica** | `EndpointSecurityMatrixTest.java:102-181` solo cubre 4; `EndpointInventoryTest.java:111-116` ignora columnas 4-6. Poblar el CSV y ejecutar cada fila con 401/403/2xx/inactivo. |
| **T-07** | H-11: `AuthorizationDeniedException` debe ser 403, no 401 | **Alta** | `GlobalHandlerException.java:61-65`; habilitar `EndpointSecurityMatrixTest.java:169` y afirmar 403 + cuerpo de error. |
| **T-08** | H-12/H-49: JWT inválido/expirado y `logout`/`changge-password` con usuario inactivo | **Alta** | No existe `JwtAuthFilterTest` ni `AuthEndpointSecurityTest`; `JwtAuthFilter.java:49-65` solo se mockea. |
| **T-09** | H-01: validador SQL y ejecutor de IA sin pruebas | **Crítica** | No hay `SqlQueryValidatorTest`, `AiServiceTest` ni `SqlExecutorIT`; `SqlQueryValidator`/`SqlExecutor` quedan fuera de toda red. |
| **T-10** | CI con Docker, `ApplicationContextIT` real, JaCoCo y `Clock` fijo | **Alta** | No hay `.github`; `ApplicationContextIT` se salta; `DateTimeUtils` no inyecta `Clock`. |
| **T-11** | H-04, H-05, H-10, H-28, H-41, H-43… de fases 1–2 | **Media/Alta** | Sin `UserServiceTest`, `AttendanceServiceTest`, `NullableBodyTest`, `ValidationContractTest` ni ITs de specs/repos; el plan los exige. |

## Límites de la auditoría

- **Docker no disponible:** `ApplicationContextIT` y cualquier prueba de integración con PostgreSQL real son **NO VERIFICABLES** en esta máquina. No se afirma que el contexto completo arranque ni que `unaccent`/`create-drop` funcionen en CI.
- **Sin JaCoCo:** no hay métricas de cobertura; no se puede afirmar el 45–55 % global ni los umbrales por capa del plan. **NO VERIFICABLE.**
- **Mutaciones limitadas:** se probaron las cuatro exigidas, cuatro adicionales (H-02, H-46, H-08 `invalid` y autoridad de `MemberController`) y la fila del CSV. No es un análisis exhaustivo de todos los fixes ni de todas las ramas.
- **Sin APIs reales:** no se llamó a Cloudinary, Gemini ni a una BD de producción. Los unitarios usan mocks; la matriz usa `MockMvc` y mocks de seguridad. Los problemas de integración, lazy loading, transacciones y mapeo real pueden quedar ocultos.
- **JDK:** Gradle se ejecutó con JDK 21.0.8; el `java` del `PATH` es 25, pero no se usó para el build. El resultado de `test` es con 21.
- **Alcance de escritura:** solo se creó este archivo; no se modificaron `src/main`, `src/test`, `build.gradle` ni los informes existentes. Las mutaciones viven en la copia temporal y no en el repo.
