<!-- estado-doc -->
> **Estado posterior (2026-10-24).** Este informe es histórico. La suite final tiene 135 tests (131 ejecutados, 4 saltados, 0 fallos); los pendientes están en `audit/TODO.md` y el estado consolidado en `audit/INFORME-FINAL.md`.

# H-03 — IDOR entre sedes en el progreso de discipulado

- **Rama**: `audit/arquitectura-y-seguridad`
- **Fecha**: 2026-10-23
- **Alcance**: cerrar H-03 en producción y habilitar los tests que estaban `@Disabled`.
- **Restricciones respetadas**: solo se tocaron los archivos de producción permitidos y los dos tests
  de `discipulado`; sin commits, sin cambio de rama, sin dependencias nuevas, sin tocar
  `build.gradle`, `application.properties` ni los informes de auditoría.

## Cambios de producción

| Archivo | Líneas | Qué cambió |
|---|---|---|
| `features/discipulado/service/DiscipuladoService.java` | 137-139 | `progress(...)`: valida el integrante del discipulado contra la sede del usuario **antes** de cargar el maestro. Si el integrante o su sede son nulos, se trata como fuera de sede. |
| `features/discipulado/service/DiscipuladoService.java` | 141-146 | `progress(...)`: carga el maestro y valida que pertenezca a la misma sede del usuario antes de mutar. |
| `features/discipulado/service/DiscipuladoService.java` | 161-170 | `updateProgress(...)`: mismas dos validaciones, en el mismo orden, antes de fijar `teacherId`/`dateCompleted`. |
| `features/discipulado/service/DiscipuladoService.java` | 178-197 | Helpers privados: `discipuladoMemberOf(...)` evita NPE con `discipulado` nulo e `isOutsideUserBranch(...)` centraliza la comparación de sede (admin exento; miembro/sede nulos ⇒ fuera de sede). No se modificó la lógica de fecha, `completed` ni el mapper. |
| `features/discipulado/controller/DiscipuladoController.java` | 93-95 | `progress(...)` recibe `@AuthenticationPrincipal User user` y lo propaga al servicio (antes pasaba `null`). `progressUpdate(...)` ya pasaba el usuario y quedó intacto. |

## Excepciones creadas

Ambas extienden `ApiException`, con mensaje en español y `HttpStatus.CONFLICT` (409):

- `features/discipulado/exceptions/CannotUpdateProgressWithMemberOutsideYourBranch.java` —
  «No puedes actualizar el progreso de un discipulado con un integrante fuera de tu rama sin
  permisos de administrador.»
- `features/discipulado/exceptions/CannotAssignTeacherOutsideYourBranch.java` —
  «No puedes asignar un maestro fuera de tu rama sin permisos de administrador.»

## Cambios de test

### `DiscipuladoServiceTest`

- Se eliminó `@Disabled` de `rejectsProgressForMemberFromAnotherBranch_H03`,
  `rejectsProgressWhenTeacherIsFromAnotherBranch_H03` y
  `rejectsUpdateProgressForMemberFromAnotherBranch_H03`.
- Se añadió `rejectsUpdateProgressWhenTeacherIsFromAnotherBranch_H03` (el caso equivalente de
  `updateProgress` con maestro de otra sede que faltaba).
- Los stubs se ajustaron para que no queden `UnnecessaryStubbingException`: si el integrante es
  ajeno, la validación corta antes de `memberRepository.findById`, así que ese stub ya no se declara.
- Las aserciones ahora exigen la excepción concreta (`CannotUpdateProgressWithMemberOutsideYourBranch`
  o `CannotAssignTeacherOutsideYourBranch`) y `HttpStatus.CONFLICT`, y verifican que el progreso no se
  mutó en el camino denegado (`completed=false`, `teacherId=null`, `dateCompleted=null`).
- Se conservaron los tests de admin y de camino feliz sin cambios de comportamiento.

### `DiscipuladoControllerTest`

- Se eliminó `@Disabled` y se adaptó la llamada a `progress(dto, user)`.
- Se usa un `User` real de `TestFixtures` y se verifica con `same(user)` que el servicio recibe
  exactamente ese principal.

## Resultado de `test`

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

```text
> Task :test

BUILD SUCCESSFUL in 15s
6 actionable tasks: 2 executed, 4 up-to-date
```

Agregado de `build/test-results/test`:

| Métrica | Valor |
|---|---|
| Total | 47 |
| Ejecutados | 42 |
| Saltados | 5 |
| Fallos | 0 |
| Errores | 0 |

Los 5 saltados son los de integración que dependen de Docker (`ApplicationContextIT`,
`AttendanceExportIT`) más 1 caso condicional de `EndpointSecurityMatrixTest`; ninguno pertenece a
H-03. Los 5 tests de H-03 se ejecutaron: los 4 de `DiscipuladoServiceTest` (los 3 habilitados más el
nuevo) y `progressPassesAuthenticatedUserToService_H03` en `DiscipuladoControllerTest`.

## Resultado de la mutación

En una copia temporal fuera del repo
(`%TEMP%\opencode\h03-mutation`), se reemplazó el cuerpo de `isOutsideUserBranch(...)` por
`return false;` (equivale a eliminar la validación de sede) y se ejecutó:

```powershell
.\gradlew.bat cleanTest test --tests "org.icc.pecesatierra.unit.discipulado.*" --console=plain
```

```text
8 tests completed, 4 failed

DiscipuladoServiceTest > rejectsProgressForMemberFromAnotherBranch_H03() FAILED
DiscipuladoServiceTest > rejectsUpdateProgressWhenTeacherIsFromAnotherBranch_H03() FAILED
DiscipuladoServiceTest > rejectsUpdateProgressForMemberFromAnotherBranch_H03() FAILED
DiscipuladoServiceTest > rejectsProgressWhenTeacherIsFromAnotherBranch_H03() FAILED

BUILD FAILED
```

Los 4 tests de denegación fallan sin la validación, luego protegen realmente H-03. La copia temporal
se eliminó y el árbol de trabajo conserva la corrección.

## Efectos colaterales

- `POST /api/discipulado/progress` y `PUT /api/discipulado/progress/{id}` ahora devuelven **409**
  (`ErrorResponseDto` vía `GlobalHandlerException`) cuando el integrante del discipulado o el maestro
  pertenecen a otra sede; antes devolvían **200** y aplicaban la escritura. Los administradores
  conservan el comportamiento anterior (pueden operar entre sedes).
- La respuesta ya no filtra la existencia del maestro cuando el integrante es ajeno: la validación
  del integrante corta antes de consultar `memberRepository`.
- No cambian rutas, permisos `@PreAuthorize`, DTOs ni el esquema de base de datos.
