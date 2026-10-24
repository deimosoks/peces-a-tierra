# Corrección post-auditoría — M-02 a M-24

- **Rama**: `audit/arquitectura-y-seguridad`
- **Fecha**: 2026-10-24
- **Rol**: agente de corrección del bloque post-auditoría.
- **Alcance corregido**: M-02, M-03 (parcial: datos cerrados, oráculo UUID 404/409 residual), M-04,
  M-05, M-08, M-11, M-12, M-13, M-14, M-15, M-16, M-17, M-19, M-23 y M-24.
- **Fuera de alcance por instrucción**: H-01 (vistas `ai_read`, `REVOKE` sobre `public`, RLS,
  validador, `new User()`), M-01 (PWA y 403), M-07 (rol de IA en Neon, no verificable) y M-22
  (bypass del validador, parte de H-01).
- **No implementados** (documentados en `audit/TODO.md`): M-06, M-09, M-10, M-18 y M-21.
- **Método**: lectura del código real, cambio de producción + test de regresión por hallazgo, suite
  completa y 16 mutaciones en una copia temporal fuera del repositorio
  (`%TEMP%\opencode\fixer-mut`, eliminada al terminar; el árbol de trabajo no se mutó).
- **Restricciones respetadas**: sin Docker, sin Neon, sin credenciales reales, sin `bootRun`, sin
  commits, stash ni cambio de rama.

## Residuales

- **M-03 — oráculo de existencia de UUID (404 vs 409)**: se cerraron los oráculos de **datos**
  (CC, asistencias, maestro) comprobando la sede antes de esas consultas, pero un no-admin todavía
  distingue un UUID inexistente (404) de uno existente en otra sede (409) en `MemberService.update`,
  `delete` y `updateActive`, en `UserService.create` y en `BaptismService.create`/`invalid`. Cerrarlo
  exige devolver el mismo 404 para "no existe" y "fuera de sede", lo que cambia el contrato 409 actual;
  no se aplicó. Queda documentado en `audit/TODO.md`.

## Resultado de la suite

| Momento | Suites | Tests | Ejecutados | Saltados | Fallos | Errores |
|---|---:|---:|---:|---:|---:|---:|
| Antes (`HEAD` = `639df8f`) | 21 | 106 | 102 | 4 | 0 | 0 |
| Después | 25 | 135 | 131 | 4 | 0 | 0 |

Comando exacto:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

Salida: `BUILD SUCCESSFUL`, exit 0. Los 4 saltados siguen siendo `ApplicationContextIT` (1) y
`AttendanceExportIT` (3), por `@Testcontainers(disabledWithoutDocker = true)`. No hay Docker en esta
máquina, así que no se ejecutaron PostgreSQL, `unaccent`, el esquema `create-drop` ni el borrado real
de refresh tokens por SQL.

Hay 4 suites nuevas (`AuthServiceTest`, `AiDatasourcePropertiesTest`, `GlobalHandlerExceptionMvcTest`,
`JwtFilterRegistrationTest`) y 29 tests nuevos.

## Tabla de correcciones

| M | Archivo:línea | Cambio | Test de regresión | Mutación | Resultado |
|---|---|---|---|---|---|
| **M-02** | `BaptismService.java:49-58` | La comprobación de sede pasa **antes** de `existsByBaptizedMemberAndInvalidFalse` y del log de duplicado; el 409 de sede no contiene el nombre. | `BaptismServiceTest#createForMemberOfAnotherBranchWithActiveBaptismDoesNotLeakTheName_M02` (bautismo activo + sede ajena: 409 sin nombre y `existsBy…` nunca invocado) y `#createRejectsMemberWithAnActiveBaptism_H08` (duplicado propio conserva `MemberAlreadyHasRegisteredBaptismActiveException` con el nombre). | Restaurar duplicado→sede. | `BaptismServiceTest`: 9 tests, **1 fallo**. |
| **M-03** | `MemberService.java:93-96` | `assertCanManageMember` justo después de `findById`, antes de `existsByCc` y antes de subir la foto. Excepción dedicada `CannotUpdateMemberOutsideYourBranchException` (409, «No puede actualizar integrantes fuera de su sede.»). | `MemberServiceTest#updateOfForeignMemberChecksBranchBeforeCcUniquenessAndPhoto_M03`. | Mover la sede detrás de `existsByCc`. | `unit.member.*`: 8 tests, **1 fallo**. |
| **M-03** | `MemberService.java:179-184` | La sede se comprueba antes de `attendanceRepository.existsByMember`. | `MemberServiceTest#deleteOnMemberFromAnotherBranchIsRejectedBeforeAttendanceLookup_M03` (verifica `existsByMember` nunca). | Mover la sede detrás de asistencias. | `unit.member.*`: 8 tests, **1 fallo**. |
| **M-03** | `MemberService.java:218` | `updateActive` ya comprobaba la sede antes de mutar; ahora reutiliza el helper y una excepción con mensaje correcto. | `MemberServiceTest#updateActiveOnMemberFromAnotherBranchIsRejected_M03`. | — (orden ya correcto). | — |
| **M-03** | `MemberPersistenceService.java:147-149` | Defensa en profundidad: conserva la comprobación y pasa a la excepción de actualización (mismo 409, mensaje correcto). | `MemberPersistenceServiceTest#updateFromAnotherBranchIsRejectedBeforeMapper_H22`. | Mover la comprobación tras el mapper (ya cubierto por H-22). | Suite verde. |
| **M-03** | `DiscipuladoService.java:141,164,187-199` | `findTeacherForActor`: si el maestro no existe y el actor no es admin, devuelve `CannotAssignTeacherOutsideYourBranch` (409); el admin conserva `MemberNotFoundException` (404). | `DiscipuladoServiceTest#missingTeacherLooksLikeForeignTeacherForNonAdminInProgress_M03`, `#…InUpdateProgress_M03` (mismo mensaje que el caso ajeno) y `#administratorStillGetsNotFoundForAMissingTeacher_M03`. | Volver a `orElseThrow(MemberNotFoundException::new)`. | `unit.discipulado.*`: 11 tests, **2 fallos**. |
| **M-23** | `UserService.java:118-133` | `create` carga el integrante y comprueba su sede antes de `existsByMemberId` y `findByUsername`. | `UserServiceTest#createForMemberOfAnotherBranchDoesNotRevealWhetherItHasAnAccount_M23` (409 de sede y unicidades nunca consultadas). | Restaurar unicidades→sede. | `unit.user.*`: 16 tests, **1 fallo**. |
| **M-04** | `GlobalHandlerException.java:102-146` | Handlers nuevos: `MissingServletRequestParameterException` → 400, `HttpRequestMethodNotSupportedException` → 405, `HttpMediaTypeNotSupportedException` → 415, `MaxUploadSizeExceededException` → 413 y `NoResourceFoundException` → 404. | `GlobalHandlerExceptionMvcTest` con `MockMvc` real (handler mapping + adaptador + resolvers): parámetro ausente, `PUT` sobre ruta solo `POST`, `text/plain` sobre `@RequestBody`, recurso inexistente y subida sobredimensionada. | Repuntar el handler de parámetro ausente a `IllegalStateException`. | `unit.advice.*`: 20 tests (8 MVC en ese momento), **1 fallo**; después, `GlobalHandlerExceptionMvcTest` 9/9 verde con el `bind` añadido. |
| **M-19** | `GlobalHandlerException.java:87-99` | `getAllErrors()` + primer `defaultMessage` no nulo + fallback «La petición contiene datos inválidos.»; ya no usa `getFieldErrors().getFirst()`. | `GlobalHandlerExceptionTest#beanValidationWithOnlyGlobalErrorsIs400_M19` (solo `ObjectError`) y `#beanValidationWithoutAnyErrorFallsBackToAGenericMessage_M19`. | Volver a `getFieldErrors().getFirst().getDefaultMessage()`. | `GlobalHandlerExceptionTest`: 12 tests, **2 fallos** (`NoSuchElementException`). |
| **M-05** | `RefreshTokenRepository.java:39-44`; `AuthService.java:133-147` | `deleteByUserId` (`@Modifying` + JPQL) y llamada dentro de la transacción de `changgePassword`; se registra el número de sesiones revocadas. | `AuthServiceTest#passwordChangeRevokesEveryRefreshTokenOfTheUser_M05` y dos negativos (contraseñas no coinciden / contraseña actual incorrecta no revocan). | Sustituir la llamada por `int revokedSessions = 0;`. | `unit.auth.*`: 10 tests, **1 fallo**. |
| **M-15** | `BaptismService.java:91`; `BaptismServiceTest.java:105-106,218-219` | `invalid` lanza `CannotInvalidBaptismOutsideYourBranch` (409, mensaje de invalidación) en vez de `CannotCreateMembersOutsideYourBranch`. Los dos fixtures de H-08 usan el mismo id con **nombres distintos**. | `invalidRejectsBaptismForMemberFromAnotherBranch_H08` y los dos tests de «mismo id, distinto nombre». | Cambiar `getId().equals` por `getName().equals` en `create` e `invalid`. | `BaptismServiceTest`: 9 tests, **2 fallos**. |
| **M-16** | `UserService.java:353-361` y `:389-397` | En `updateActive` y `delete` la sede se comprueba antes de `validateCanManageUser`: un objetivo de otra sede da 409, no 403. | `UserServiceTest#nonAdminCannotLearnPrivilegesOfAnAccountInAnotherBranch_M16` y `#…OnDelete_M16`. Los tests de H-21 (misma sede, más privilegios → 403) siguen verdes. | Poner `validateCanManageUser` antes de la sede en `updateActive`. | `unit.user.*`: 16 tests, **1 fallo**. |
| **M-17** | `MemberSpecification.java:24-27` | `dto == null` se normaliza a `new MemberFilterRequestDto()`; se elimina la rama `return cb.conjunction()` y el predicado de sede se aplica siempre. | `NullFilterBodyTest#memberSpecificationWithNullDtoStillRestrictsToTheUsersBranch_M17`. | Reintroducir `if (dto == null) return cb.conjunction();`. | `unit.filters.*`: 7 tests, **1 fallo**. |
| **M-24** | `UserService.java:254-256` | `validateMemberBranch` solo se llama si `memberId != null`. | `UserServiceTest#updateWithoutMemberIdDoesNotValidateMemberBranch_M24`. | Quitar el `if (member != null)`. | `unit.user.*`: 16 tests, **1 fallo**. |
| **M-08** | `AiDatasourcePropertiesTest.java` (nuevo) | Test que carga el `application.properties` real y exige `ai.datasource.username=${AI_DB_USER}`, `password=${AI_DB_PASS}` y `url=${AI_DB_URL:${DB_URL}}`, incluidos fallback y override. | El propio test. | Cambiar las credenciales a `${DB_USER}`/`${DB_PASS}`. | `AiDatasourcePropertiesTest`: 2 tests, **1 fallo**. |
| **M-11** | `AbstractIntegrationTest.java:40-44`; `application-test.properties:1-5` | `@DynamicPropertySource` apunta `ai.datasource.url/username/password` al contenedor PostgreSQL; la URL fija del perfil queda como fallback documentado. | No ejecutable sin Docker (IT saltadas); el contrato se cubre con `AiDatasourcePropertiesTest` para producción. | — | — |
| **M-12** | `AttendanceSpecificationTest.java:84-121`; `NullFilterBodyTest.java:180-256` | Se captura el array de `Predicate` que recibe `cb.and(...)` y se comprueba que contiene el predicado de sede. | Los tests existentes de H-02/H-10 endurecidos. | Devolver `cb.conjunction()` en `MemberSpecification` y `AttendanceSpecification`. | `unit.filters.*` + `unit.attendance.*`: 10 tests, **5 fallos**. |
| **M-13** | `JwtFilterRegistrationTest.java` (nuevo) | `@WebMvcTest` con `SecurityConfig` y el `JwtAuthFilter` real, `JwtService` y `CustomUserDetailsServiceImpl` mockeados: token inválido → 401 con el cuerpo JSON del filtro; token válido → 200 del controlador. | El propio test. | Quitar `.addFilterBefore(jwtAuthFilter, …)` de `SecurityConfig`. | `JwtFilterRegistrationTest`: 2 tests, **2 fallos**. |
| **M-14** | `GlobalHandlerExceptionMvcTest.java` (nuevo) | Los 400 de `HttpMessageNotReadableException`, `BindException` y `PropertyReferenceException` se prueban a través de `MockMvc`, no llamando al handler. | El propio test. | Repuntar las tres anotaciones a otras excepciones. | `GlobalHandlerExceptionMvcTest`: 9 tests, **3 fallos**. |
| **M-20** | `AGENTS.md:8-13,55-62`; `INFORME-FINAL.md:6,392-421`; `00-alcance-implementaciones.md:5` | Comando de test real (la clase `PecesATierraApplicationTests` ya no existe); «Testing reality» describe 25 suites, 4 IT saltadas, sin CI/JaCoCo; conteo 250→251 archivos Java; artefactos ya commiteados (`23`); merge-base `f3d07cc`. | No aplica. | — | — |

### Notas de implementación

- **M-03**: la excepción nueva es `CannotUpdateMemberOutsideYourBranchException` (409). Sustituye a
  `CannotDeleteMemberOutSideYourBranchException` en `updateActive` (mensaje de «eliminar» en una
  actualización) y a `CannotCreateMembersOutsideYourBranch` en la actualización de persistencia. Las
  respuestas siguen siendo 409, así que el cliente no cambia de flujo.
- **M-04**: `NoResourceFoundException` se añade solo porque el resource resolver de `MvcConfig` sigue
  devolviendo `index.html` para las rutas no-API; el handler únicamente ve los casos en que el resolver
  devuelve `null` (rutas `/api/**` inexistentes). No se tocó `MvcConfig` ni el fallback SPA.
- **M-05**: se usa un `DELETE` por repositorio en vez de vaciar la colección porque el principal del
  filtro está desacoplado y `refreshTokens` es LAZY. La operación va en la misma transacción que el
  `save` del hash.
- **M-11**: los `DynamicPropertySource` usan suppliers perezosos (`POSTGRES::getJdbcUrl`) para no
  forzar el arranque del contenedor al registrar las propiedades.

## Mutaciones de comprobación

Todas en `C:\Users\josef\AppData\Local\Temp\opencode\fixer-mut` (copia sin `.git`, `build`, `.gradle`
ni `bin`), con una línea base verde antes de mutar. Cada archivo mutado se restauró desde el
repositorio antes de la siguiente mutación; al terminar la copia se eliminó.

| # | Guard revertido | Filtro de test | Resultado |
|---|---|---|---|
| 1 | M-02: duplicado antes que sede | `unit.baptism.*` | 9 tests, **1 fallo** |
| 2 | M-03: sede después de asistencias (`delete`) | `unit.member.*` | 8 tests, **1 fallo** |
| 3 | M-03: sede después de `existsByCc` (`update`) | `unit.member.*` | 8 tests, **1 fallo** |
| 4 | M-03: maestro inexistente → 404 para no-admin | `unit.discipulado.*` | 11 tests, **2 fallos** |
| 5 | M-05: sin `deleteByUserId` | `unit.auth.*` | 10 tests, **1 fallo** |
| 6 | M-16: privilegios antes que sede | `unit.user.*` | 16 tests, **1 fallo** |
| 7 | M-17: `if (dto == null) return cb.conjunction()` | `unit.filters.*` | 7 tests, **1 fallo** |
| 8 | M-12: specs devuelven `cb.conjunction()` | `unit.filters.*` + `unit.attendance.*` | 10 tests, **5 fallos** |
| 9 | M-13: sin `addFilterBefore` | `JwtFilterRegistrationTest` | 2 tests, **2 fallos** |
| 10 | M-04: handler de parámetro ausente repuntado | `unit.advice.*` | 20 tests, **1 fallo** |
| 11 | M-14: tres handlers 400 repuntados | `GlobalHandlerExceptionMvcTest` | 9 tests, **3 fallos** |
| 12 | M-19: `getFieldErrors().getFirst()` | `GlobalHandlerExceptionTest` | 12 tests, **2 fallos** |
| 13 | M-15: `getId()` → `getName()` | `unit.baptism.*` | 9 tests, **2 fallos** |
| 14 | M-24: sin guarda de `member` nulo | `unit.user.*` | 16 tests, **1 fallo** |
| 15 | M-23: unicidades antes que sede | `unit.user.*` | 16 tests, **1 fallo** |
| 16 | M-08: `AI_DB_USER`/`AI_DB_PASS` → `DB_USER`/`DB_PASS` | `AiDatasourcePropertiesTest` | 2 tests, **1 fallo** |

Conclusión: los 16 guards probados tienen al menos un test que falla al revertirlos. No se debilitó
ninguna aserción existente; los cambios en tests existentes endurecen (`never()` sobre la consulta que
filtraba, excepciones más específicas, predicados capturados).

## Lo que no se hizo y por qué

| M | Estado | Motivo |
|---|---|---|
| **M-06** | No implementado | Requiere elegir pool/timeout y probarlo contra una base real; el driver PostgreSQL ya aplica `connectTimeout=10 s`. Riesgo de agotamiento de hilos documentado. |
| **M-07** | No verificado | **NO VERIFICABLE** sin Neon (rol, `neon_superuser`, grants). No se tocó por instrucción. |
| **M-09** | No implementado | El fallo al arrancar ya es fail-closed y está documentado; un mensaje accionable exige validación de configuración y una decisión de despliegue. |
| **M-10** | No implementado | No hay consumidores de `JdbcTemplate` por tipo; declarar el bean principal retira la autoconfiguración y añade riesgo sin beneficio actual. |
| **M-18** | No implementado | Completar las 69 filas de la matriz con permiso esperado y usuario restringido es un cambio grande y separado; se deja documentado como deuda. |
| **M-21** | No implementado | Inyectar `Clock` en `DateTimeUtils` obliga a tocar todos los specs; el test actual solo es potencialmente flaky por la tolerancia de 1 s. |
| **M-01 / H-01 / M-22** | No tocados | Excluidos explícitamente. |

## Efectos colaterales

- **PWA y 403 (M-01)**: no se tocó el bundle ni el contrato de 403. Al contrario, M-03/M-16 convierten
  varios 403/404 en 409 de sede, lo que reduce los casos que hoy provocan `logout()` en el cliente. El
  riesgo de M-01 sigue igual para los 403 legítimos de permiso.
- **Contrato de errores**: cinco respuestas que antes eran 500 pasan a 400/404/405/413/415. Un cliente
  que esperase 500 (no documentado) verá el código correcto. `NoResourceFoundException` solo afecta a
  `/api/**` inexistentes; las rutas no-API siguen sirviendo el SPA.
- **Mensajes 409**: el alta de bautismo ya no incluye el nombre cuando el integrante es de otra sede;
  dentro de la sede se conserva. La invalidación de bautismo y la actualización de integrantes usan
  mensajes propios en vez de los de «crear integrantes».
- **`updateActive` de integrantes**: cambia el texto del 409 («actualizar» en vez de «eliminar»); el
  código HTTP no cambia.
- **Tests**: 4 suites y 29 tests nuevos; la suite completa sube de 21/106 a 25/135, sin cambios en los
  4 saltados. No se añadieron `Thread.sleep`, red real ni Docker.
- **Documentación**: `AGENTS.md` y `INFORME-FINAL.md` describen la suite real y los artefactos
  commiteados; `audit/TODO.md` lista los M pendientes con su motivo.
