<!-- estado-doc -->
> **Estado posterior (2026-10-24).** Este informe es histórico. La suite final tiene 135 tests (131 ejecutados, 4 saltados, 0 fallos); los pendientes están en `audit/TODO.md` y el estado consolidado en `audit/INFORME-FINAL.md`.

# Corrección de pruebas — T-01 a T-05

**Rama:** `audit/arquitectura-y-seguridad` · **Fecha:** 2026-10-23
**Alcance:** cierre de los huecos T-01 a T-05 de `audit/verificacion-pruebas.md`. Solo se modificó `src/test/**` y se creó este informe. No se tocó `src/main`, `build.gradle`, `application.properties`, los informes existentes ni el historial de Git (sin commit, sin stash, sin cambio de rama).

## Resumen

| Métrica | Antes | Después |
|---|---:|---:|
| Tests totales | 32 | **46** |
| Ejecutados | 28 | **37** |
| Saltados | 4 | **9** |
| Fallos / errores | 0 / 0 | **0 / 0** |
| Exit de `test` | 0 | **0** |

Los 5 saltos nuevos son deliberados: 3 de `AttendanceExportIT` (Docker apagado), 1 del test de controller de H-03 (`@Disabled`) y 1 del caso de maestro de otra sede en `DiscipuladoServiceTest` (`@Disabled`). No hay ningún test marcado como pendiente que se haya disfrazado de verde.

## Trabajos

| T-ID | Archivo | Qué se añadió / eliminó | Verificación de mutación | Resultado |
|---|---|---|---|---|
| **T-01** | `src/test/java/org/icc/pecesatierra/unit/discipulado/DiscipuladoServiceTest.java` | **Eliminado** `currentBehaviorAllowsCrossBranchProgress_H03` y su `@Tag("deuda-h03")`, junto con las importaciones `Tag` y `assertThatCode` que quedaron sin uso. Se mantienen los dos tests correctos `@Disabled`: rechazo de integrante de otra sede en `progress` y en `updateProgress`. Se añadió un `@Disabled` para el **maestro** de otra sede (la otra mitad de H-03) y dos tests verdes de administrador (`progress` y `updateProgress`), que sí son el contrato esperado. | No aplica (H-03 sigue abierto; habilitar los tests haría fallar la suite contra producción). | 6 tests: 3 ejecutados, 3 saltados, 0 fallos. La suite ya **no afirma** que el cruce de sede sea correcto. |
| **T-01** | `src/test/java/org/icc/pecesatierra/unit/discipulado/DiscipuladoControllerTest.java` (nuevo) | Test `@Disabled` que llama a `DiscipuladoController.progress(dto)` y captura el `User` que recibe el servicio; exige que no sea `null`. Hoy el controller pasa `null` literal (`DiscipuladoController.java:94`), por eso queda deshabilitado. | No aplica. | 1 test saltado, 0 fallos. No requirió Spring ni andamiaje web: se instancia el controller con Mockito. |
| **T-02** | `src/test/java/org/icc/pecesatierra/unit/attendance/AttendanceSpecificationTest.java` (nuevo) | 3 tests sin Docker con `RETURNS_DEEP_STUBS` para `Root<Attendance>`, `CriteriaQuery<?>` y `CriteriaBuilder`, y mocks explícitos de los `Join` `serviceEvent`, `services`, `member` y `branch`. Casos: (1) `dto == null` + operador de A → `branch.id == "branch-a"`; (2) `dto.branchId = B` + operador de A → sigue filtrando por A y nunca consulta B; (3) `dto == null` + admin → no se pide `branch.id`. Usa `TestFixtures.user/branch/member` reales. | **M-A:** en la copia temporal se movió el bloque de sede dentro de `if (dto != null)`. `AttendanceSpecificationTest` **falla** en `:92` (`WantedButNotInvoked`): el caso 1 deja de añadir el predicado. Copia restaurada (hash idéntico al repo). | 3 ejecutados, 0 fallos. Detecta la regresión de H-02 (M6 del informe anterior). |
| **T-02** | `src/test/java/org/icc/pecesatierra/integration/attendance/AttendanceExportIT.java` (nuevo) | IT con `AbstractIntegrationTest`: dos sedes, integrantes y eventos por sede, una asistencia por sede, operador de A y admin. `export(null, operadorA)` → solo A; `export(null, admin)` → A+B; `export(dto con branchId de B, operadorA)` → sigue solo A. | No ejecutada aquí: Docker no está disponible. La mutación H-02 se verificó con el test unitario, que sí corre sin Docker. | 3 tests **saltados** por `@Testcontainers(disabledWithoutDocker = true)`. Compila y queda listo para CI. **NO VERIFICABLE en esta máquina contra PostgreSQL real.** |
| **T-03** | `src/test/java/org/icc/pecesatierra/unit/config/RestTemplateConfigTest.java` (nuevo) | Instancia `new RestTemplateConfig().restTemplate()`, exige `SimpleClientHttpRequestFactory` y timeouts de 5000 ms / 60000 ms. Spring Framework 6.2.15 **no expone getters** en `SimpleClientHttpRequestFactory` (`javap` solo muestra setters), así que se leen los campos privados `connectTimeout` y `readTimeout` con `ReflectionTestUtils`; queda explicado en el javadoc. | No aplica. | 1 ejecutado, 0 fallos. Revertir a `new RestTemplate()` o cambiar los timeouts haría fallar el test (el campo sería 0/-1 o distinto). |
| **T-04** | `src/test/java/org/icc/pecesatierra/unit/baptism/BaptismServiceTest.java` | **Añadido** `invalid` con integrante de sede B y operador de A → `CannotCreateMembersOutsideYourBranch` (409) y sin mutar `invalid`, `invalidatorId`, `invalidReason` ni `invalidAt`; **añadido** `invalid` con admin → permitido; **añadido** `create` con `existsByBaptizedMemberAndInvalidFalse == true` → `MemberAlreadyHasRegisteredBaptismActiveException` (409) sin `save`; **añadido** `invalid` con dos instancias de `Branch` distintas y el mismo id, operador no-admin → permitido (este es el caso que detecta la reversión de H-08). | **M-B:** en la copia temporal se revirtió `BaptismService.java:87` a `!...getBranch().equals(...)`. `invalidAllowsTwoBranchInstancesWithTheSameId_H08` **falla** en `:232` (`AssertionError`, se lanza la excepción de sede). Copia restaurada (hash idéntico). | 8 ejecutados, 0 fallos. La rama `invalid` de H-08 queda protegida; el caso de sede ajena y el duplicado también. |
| **T-05** | `src/test/java/org/icc/pecesatierra/unit/baptism/BaptismServiceTest.java` | Se sustituyó `@Mock BaptismMapper` por `@Spy BaptismMapper baptismMapper = new BaptismMapper(new DateTimeUtils())`; `@InjectMocks` inyecta el mapper real. Se eliminaron todos los stubs de `toDto` (incluido el `thenAnswer` que copiaba `note`) y la aserción tautológica sobre el stub. `municipality` se valida en la entidad capturada (no existe en `BaptismResponseDto`) y `note`, `memberName`, `branchName` y `registeredBy` en el DTO real. | **M-C (extra):** en la copia temporal se quitó `.note(baptism.getNote())` de `BaptismMapper`. `persistsNoteAndMunicipalityFromTheRequest_H07` **falla** en `:97` (`AssertionFailedError`). Copia restaurada (hash idéntico). | 8 ejecutados, 0 fallos. El DTO ya se prueba contra el mapper real, no contra un stub. |

## Resultado exacto de la verificación final

Comando ejecutado en el repo real, con JDK 21 y sin variables de entorno reales:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
Remove-Item Env:DB_URL,Env:DB_USER,Env:DB_PASS,Env:JWT_SECRET,`
            Env:CLOUDINARY_CLOUD_NAME,Env:CLOUDINARY_API_KEY,`
            Env:CLOUDINARY_API_SECRET,Env:GEMINI_API_KEY -ErrorAction SilentlyContinue
.\gradlew.bat cleanTest test --console=plain
```

Salida:

```text
> Task :cleanTest
> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE
> Task :test

BUILD SUCCESSFUL in 13s
6 actionable tasks: 2 executed, 4 up-to-date
EXIT=0
```

Detalle por suite desde `build/test-results/test/TEST-*.xml`:

| Suite | Tests | Ejecutados | Saltados | Fallos | Errores |
|---|---:|---:|---:|---:|---:|
| `ApplicationContextIT` | 1 | 0 | 1 | 0 | 0 |
| `integration.attendance.AttendanceExportIT` | 3 | 0 | 3 | 0 | 0 |
| `unit.attendance.AttendanceSpecificationTest` | 3 | 3 | 0 | 0 | 0 |
| `unit.baptism.BaptismServiceTest` | 8 | 8 | 0 | 0 | 0 |
| `unit.config.RestTemplateConfigTest` | 1 | 1 | 0 | 0 | 0 |
| `unit.discipulado.DiscipuladoControllerTest` | 1 | 0 | 1 | 0 | 0 |
| `unit.discipulado.DiscipuladoServiceTest` | 6 | 3 | 3 | 0 | 0 |
| `unit.member.MemberServiceTest` | 3 | 3 | 0 | 0 | 0 |
| `unit.time.DateTimeUtilsTest` | 6 | 6 | 0 | 0 | 0 |
| `web.security.EndpointInventoryTest` | 2 | 2 | 0 | 0 | 0 |
| `web.security.EndpointSecurityMatrixTest` | 9 | 8 | 1 | 0 | 0 |
| `web.security.PreAuthorizeSpelTest` | 3 | 3 | 0 | 0 | 0 |
| **Total** | **46** | **37** | **9** | **0** | **0** |

## Mutaciones ejecutadas (copia temporal)

Directorio: `C:\Users\josef\AppData\Local\Temp\opencode\mutation-tests` (copia sin `build/`, `.gradle/`, `.git/`, `bin/`). El repo real no se tocó. Los tres archivos mutados se restauraron desde el repo y se comprobó que el hash SHA-256 coincide (`match=True` en los tres).

| Mutación | Cambio en la copia | Test ejecutado | Resultado esperado | Resultado real | Veredicto |
|---|---|---|---|---|---|
| **M-A (H-02)** | Mover el bloque `if (!user.hasAuthority(ADMINISTRATOR)) ...` dentro de `if (dto != null)` en `AttendanceSpecification.java` | `AttendanceSpecificationTest` | Fallar | **EXIT=1**; 1 fallo de 3 en `:92` (`WantedButNotInvoked`). Los casos 2 y 3 siguen pasando, como se esperaba: el mutante solo elimina el filtro cuando `dto == null`. | **BUENA** |
| **M-B (H-08 `invalid`)** | `BaptismService.java:87`: volver a `!baptism.getBaptizedMember().getBranch().equals(user.getMember().getBranch())` | `BaptismServiceTest` | Fallar | **EXIT=1**; 1 fallo de 8: `invalidAllowsTwoBranchInstancesWithTheSameId_H08` en `:232` | **BUENA** |
| **M-C (H-07, extra)** | Quitar `.note(baptism.getNote())` de `BaptismMapper.toDto` | `BaptismServiceTest.persistsNoteAndMunicipalityFromTheRequest_H07` | Fallar | **EXIT=1**; `AssertionFailedError` en `:97` | **BUENA** |

Nota: la mutación M-B solo puede detectarse con dos instancias de `Branch` distintas y el mismo id. Un test de “sede B contra sede A” con ids distintos pasa también con `Branch.equals` (ambas ramas rechazan), por eso se añadió explícitamente `invalidAllowsTwoBranchInstancesWithTheSameId_H08`. Es el mismo patrón que ya usaba `create` para H-08.

## Huecos que no se pudieron cerrar

1. **H-03 sigue abierto en producción.** Los tests que exigen rechazo entre sedes y el que exige que el controller pase el `User` autenticado quedan `@Disabled`. No se habilitaron a propósito. Hasta que se corrija `DiscipuladoService.progress/updateProgress` y `DiscipuladoController.progress`, la suite no protege H-03: solo documenta el contrato esperado.
2. **`AttendanceExportIT` no se ejecutó.** Docker no está disponible en esta máquina (`docker info` falla), así que los 3 tests se saltan. El IT compila y está listo para CI, pero la verificación real contra PostgreSQL, `unaccent` y el mapeo perezoso de `AttendanceMapper` queda **NO VERIFICABLE** aquí. La parte de especificación sí quedó protegida sin Docker por `AttendanceSpecificationTest`.
3. **T-02 unitario mockea `CriteriaBuilder`.** El propio `plan-pruebas.md` (§8) desaconseja mockear criterios para “probar” una Specification por el riesgo de probar la implementación y no el comportamiento. Se hizo así porque la tarea lo exige y porque no hay Docker; el IT es la contraparte de comportamiento real. El unitario verifica la construcción de predicados, no la SQL generada.
4. **T-03 usa reflexión.** `SimpleClientHttpRequestFactory` de Spring 6.2.15 no tiene getters de timeout; el test lee campos privados. Si Spring cambia esos nombres en una versión futura, el test fallará por `IllegalArgumentException` de `ReflectionTestUtils`, no por un timeout incorrecto. Es una dependencia aceptada y documentada.
5. **H-04 no estaba en el encargo.** La petición final menciona “H-02 y H-04/H-08”; H-04 (escalada de rol en `UserService`) pertenece a T-11 y no se tocó. Las autoverificaciones realizadas son H-02, H-08 (`invalid`) y, como extra, H-07 (mapper real).
6. **No se añadieron tests de controller HTTP para T-02/T-04.** El encargo pedía IT de servicio para H-02 y unitarios de servicio para H-08/H-07; la matriz HTTP de 69 handlers sigue siendo deuda de T-06.
