# Corrección H-20 y H-22 — aislamiento multi-sede en sedes e integrantes

Fecha: 2026-10-24
Rama: `audit/arquitectura-y-seguridad`
Alcance: cierre de las dos fugas de scoping por sede del eje multi-sede. Sin cambios en controladores, DTOs, `SecurityConfig`, `build.gradle` ni otros servicios.

## Cambios

| Hallazgo | Archivo:línea | Cambio | Test | Mutación |
|---|---|---|---|---|
| H-20 (`PUT /api/branches/{id}`) | `features/branch/service/BranchService.java:69` (helper `:141-146`) | `update` llama a `assertCanManageBranch(branch, user)` tras cargar la sede y antes de `beforeUpdate`, `branchMapper.updateEntityFromDto` y `save`. Un no-admin solo puede actualizar la sede de `user.getMember().getBranch()`; `ADMINISTRATOR` queda exento. | `unit/branch/BranchServiceTest`: `updateFromAnotherBranchIsRejectedWithoutMutating_H20`, `updateOwnBranchIsAllowedForNonAdmin_H20`, `updateAnotherBranchIsAllowedForAdministrator_H20` | Sin la comprobación, el caso de sede ajena deja de lanzar excepción y muta la entidad: el test falla. |
| H-20 (`DELETE /api/branches/{id}`) | `features/branch/service/BranchService.java:115` (helper `:141-146`) | `delete` llama a `assertCanManageBranch(branch, user)` antes de `existsMemberByBranch`/`existsAttendanceByBranch` y de `delete`, de modo que un no-admin recibe 409 sin saber si la sede ajena tiene registros. | `unit/branch/BranchServiceTest`: `deleteFromAnotherBranchIsRejectedBeforeCheckingRecords_H20`, `deleteAnotherBranchWithRecordsIsRejectedAsScopeViolation_H20`, `deleteOwnBranchIsAllowedForNonAdmin_H20`, `deleteAnotherBranchIsAllowedForAdministrator_H20` | Sin la comprobación, la sede ajena sin registros se borra y la que tiene registros responde `CannotDeleteBranchWithRecords`: ambos tests fallan. |
| H-20 (excepción) | `features/branch/exceptions/CannotManageBranchOutsideYourBranchException.java:6-9` | Nueva excepción de negocio que extiende `ApiException`, HTTP 409 y mensaje en español: «No puede gestionar una sede fuera de tu sede.». | Cubierta por los tests de denegación de `BranchServiceTest`, que verifican `getStatus() == CONFLICT`. | — |
| H-22 (`PUT /api/members/{id}`) | `features/member/service/MemberPersistenceService.java:147-149` | La comprobación de sede se mueve al inicio de `update`, inmediatamente después de `findById` y antes de `beforeUpdate`, de `memberMapper.updateEntityFromDto` y de cualquier mutación. El camino cruzado lanza `CannotCreateMembersOutsideYourBranch` (409) y ya no puede llegar al mapper que expone `member.getCompleteName()`. Se elimina la comprobación tardía que estaba después del mapper. La reasignación de sede para admin (`dto.getBranchId()`) se mantiene. | `unit/member/MemberPersistenceServiceTest`: `updateFromAnotherBranchIsRejectedBeforeMapper_H22`, `updateFromSameBranchReachesTheMapper_H22`, `administratorCanUpdateMemberFromAnotherBranch_H22` | Con la comprobación devuelta después del mapper, el test cruzado recibe `MemberNoHasCategoryForThisSubCategoryException` con el nombre «Ana Perez B» y falla, reproduciendo la fuga. |

## Detalle de los tests

Los tests de denegación no debilitan aserciones:

- H-20 `update`: verifican que no se invoca `branchMapper.updateEntityFromDto` ni `branchRepository.save`, y que nombre, dirección, ciudad y celular de la sede objetivo siguen intactos.
- H-20 `delete`: verifican que no se invoca `existsMemberByBranch`, ni `existsAttendanceByBranch`, ni `branchRepository.delete`; en el caso con registros se comprueba además que la excepción es la de sede y no `CannotDeleteBranchWithRecords`.
- H-22: verifica que no se invoca `memberMapper.updateEntityFromDto`, que no hay `save`, que no se consulta `branchRepository`/`memberSubCategoryRepository` y que el integrante conserva nombre, CC, sede y `updatedAt == null`. El stub del mapper lanza la excepción filtrada de forma `lenient` para que, si la comprobación se ejecuta tarde, el fallo muestre exactamente la fuga.

Casos de permitido: no-admin sobre su propia sede llega al mapper y a `save`; admin sobre sede ajena también, incluida la reasignación de sede del integrante.

## Verificación

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

Resultado: **BUILD SUCCESSFUL** (`exit 0`), 27 s.

| Métrica | Valor |
|---|---|
| Suites | 19 |
| Tests | 101 |
| Ejecutados | 97 |
| Saltados | 4 |
| Fallos | 0 |
| Errores | 0 |

Saltados: `ApplicationContextIT` (1) y `AttendanceExportIT` (3), por `@Testcontainers(disabledWithoutDocker = true)` al no haber Docker disponible.

### Autoverificación de mutación

Copia temporal de `src`, `gradle`, `build.gradle`, `settings.gradle` y el wrapper en `%TEMP%\opencode\h20-h22-mutation`. En la copia se quitaron las dos llamadas a `assertCanManageBranch` y se devolvió la comprobación de H-22 a después de `memberMapper.updateEntityFromDto`. Ejecutados solo los dos test nuevos:

```
10 tests completed, 4 failed
```

Fallos esperados:

1. `BranchServiceTest.updateFromAnotherBranchIsRejectedWithoutMutating_H20`: «Expecting code to raise a throwable» (la sede ajena se mutaba).
2. `BranchServiceTest.deleteFromAnotherBranchIsRejectedBeforeCheckingRecords_H20`: «Expecting code to raise a throwable» (la sede ajena se borraba).
3. `BranchServiceTest.deleteAnotherBranchWithRecordsIsRejectedAsScopeViolation_H20`: recibió `CannotDeleteBranchWithRecords` en vez de `CannotManageBranchOutsideYourBranchException` (oráculo de registros).
4. `MemberPersistenceServiceTest.updateFromAnotherBranchIsRejectedBeforeMapper_H22`: recibió `MemberNoHasCategoryForThisSubCategoryException: El integrante Ana Perez B no cumple los requisitos...` en vez de la excepción de sede (fuga del nombre confirmada).

La copia temporal se eliminó y el árbol de trabajo conserva únicamente los cambios definitivos.

## Efectos colaterales

- **Roles no-admin con `BRANCH_UPDATE` o `BRANCH_DELETE`**: pierden la capacidad de actualizar o borrar sedes distintas de la suya. Si en `role_permission` esos permisos están asignados a roles operativos, esos roles quedan limitados a su propia sede. `ADMINISTRATOR` no cambia.
- **Creación de sedes**: `create` no se modifica; sigue dependiendo de `BRANCH_CREATE || ADMINISTRATOR`.
- **Integrantes**: para no-admin, el `PUT` de un integrante de otra sede ahora responde 409 genérico antes de resolver categoría/subcategoría, tipo o foto. El comportamiento intra-sede y el de admin (incluida la reasignación de sede) no cambia.
- **404 vs 409 en sedes**: un no-admin que apunte a un `branchId` inexistente sigue recibiendo 404 (`BranchNotFoundException`) porque la carga precede a la comprobación de sede. No se filtra información de integrantes; solo la existencia de una sede, metadato ya listable con `VIEW_BRANCH_PANEL`.
- **Foto en `PUT /members/{id}` ajeno**: `MemberService.update` sube la imagen a Cloudinary antes de delegar en `MemberPersistenceService.update`; al lanzarse ahora la excepción de sede, el `catch (ApiException)` existente borra el `publicId` recién subido. No queda foto huérfana, aunque persiste una subida transitoria que no se aborda aquí porque `MemberService` está fuera del alcance de esta corrección.
