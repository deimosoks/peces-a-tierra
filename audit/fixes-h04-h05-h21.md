<!-- estado-doc -->
> **Estado posterior (2026-10-24).** Este informe es histórico. La suite final tiene 135 tests (131 ejecutados, 4 saltados, 0 fallos); los pendientes están en `audit/TODO.md` y el estado consolidado en `audit/INFORME-FINAL.md`.

# Corrección H-04, H-05 y H-21 — privilegios de usuario y rol

- **Rama**: `audit/arquitectura-y-seguridad`
- **Fecha**: 2026-10-24
- **Alcance**: `UserService`, `RoleService`, DTOs de rol y tres excepciones nuevas.
- **Regla aplicada**: `hasAuthority("ADMINISTRATOR")` sigue siendo bypass total. Para el resto de
  actores rige el principio de subconjunto: no pueden conceder ni gestionar permisos que no poseen.
- **Sin cambios** en controladores, firmas de endpoints, contrato HTTP, `role_permission`,
  `build.gradle` ni `application.properties`.

## 1. Cambios de producción

| Hallazgo | Archivo:línea | Cambio | Test | Mutación |
|---|---|---|---|---|
| H-04 (crear usuario) | `features/user/service/UserService.java:131-133` | Carga los roles de `rolesId` **antes** de `userRepository.save` y valida que la unión de sus permisos sea subconjunto de las autoridades efectivas del actor. Un rol con `ADMINISTRATOR` queda bloqueado para no-admin. | `UserServiceTest#nonAdminCannotCreateUserWithAdministratorRole_H04`, `#nonAdminCanCreateUserGrantingPermissionsItAlreadyHas_H04`, `#administratorCanCreateUserWithAdministratorRole_H04` | Falla al anular `validateCanGrantPermissions` (excepción esperada, `save` sí se ejecuta). |
| H-04 (editar usuario) | `features/user/service/UserService.java:250-252` | Misma validación en `update`, antes de `user.getRoles().clear()` y de cualquier mutación. | mismo bloque `UserServiceTest` | incluida en la mutación anterior |
| H-04 (crear rol) | `features/role/service/RoleService.java:92` | Valida el `Set<PermissionRequestDto>` contra las autoridades del actor antes de guardar. | `RoleServiceTest#nonAdminCannotCreateRoleWithAdministratorPermission_H04`, `#nonAdminCanCreateRoleWithPermissionsItAlreadyHas_H04`, `#administratorCanCreateRoleWithAdministratorPermission_H04` | Falla al anular `validateCanGrantPermissions`. |
| H-04 (editar rol) | `features/role/service/RoleService.java:142-144` | Valida primero que el rol objetivo no tenga permisos superiores y después los permisos solicitados, antes de `roleMapper.updateEntityFromDto` (que hace `clear()`). | `RoleServiceTest#nonAdminCannotAddAdministratorPermissionToExistingRole_H04`, `#nonAdminCannotUpdateRoleWithMorePrivileges_H04`, `#administratorCanUpdateRoleWithMorePrivileges_H04` | Fallan ambos tests de denegación al anular las dos comprobaciones. |
| H-04 (borrar rol) | `features/role/service/RoleService.java:191` | Aplica la comprobación de privilegios del rol objetivo antes de borrar (y antes de comprobar si está en uso). | `RoleServiceTest#nonAdminCannotDeleteRoleWithMorePrivileges_H04`, `#administratorCanDeleteRoleWithAdministratorPermission_H04` | Falla al anular `validateCanManageRole`. |
| H-21 (editar cuenta) | `features/user/service/UserService.java:244` | `validateCanManageUser` bloquea modificar cuentas con algún permiso que el actor no tenga (incluido `ADMINISTRATOR`). | `UserServiceTest#nonAdminCannotUpdateAdministrator_H21`, `#administratorCanUpdateAnotherAdministrator_H21` | Falla al anular `validateCanManageUser` (el servicio continúa y termina lanzando otra excepción o guardando). |
| H-21 / VS-03 (activar/desactivar) | `features/user/service/UserService.java:345` | Misma comprobación en `updateActive` antes de mutar; se conserva el bloqueo de auto-desactivación. | `UserServiceTest#nonAdminCannotDeactivateAdministrator_H21`, `#administratorCanDeactivateAnotherAdministrator_H21` | Falla al anular `validateCanManageUser`. |
| H-21 / VS-03 (borrar cuenta) | `features/user/service/UserService.java:378` | Misma comprobación en `delete` antes de borrar; se conserva el bloqueo de auto-borrado. | `UserServiceTest#nonAdminCannotDeleteAdministrator_H21`, `#administratorCanDeleteAnotherAdministrator_H21` | Falla al anular `validateCanManageUser`. |
| H-05 (sede del nuevo integrante) | `features/user/service/UserService.java:248` (`validateMemberBranch`, `:104-113`) | Valida la sede del **nuevo** `member`, no solo la actual del objetivo. Miembro nulo o de otra sede ⇒ 409 `CannotUpdateUserWithMemberOutSideYourBranchException` para no-admin; el admin conserva el bypass. | `UserServiceTest#nonAdminCannotMoveUserToMemberOfAnotherBranch_H05` | Falla al anular `validateMemberBranch` (la actualización prosigue). |
| H-05 (refresh tokens) | `features/user/service/UserService.java:272-282` | Si la contraseña cambia realmente, `user.getRefreshTokens().clear()` dentro de la transacción; `orphanRemoval = true` borra las filas. Si `matches` dice que es la misma, no se toca nada. | `UserServiceTest#passwordChangeRevokesRefreshTokens_H05`, `#updateWithoutPasswordChangeKeepsRefreshTokens_H05` | No cubierta por la mutación de privilegios (es revocación, no autorización); ambas pruebas verdes. |
| H-28 / H-04 (validación) | `features/role/dtos/PermissionRequestDto.java:14` | `@NotBlank` → `@NotNull` sobre el enum `AppPermission`, para que la validación no lance `UnexpectedTypeException`. | validación de contexto (no unitaria) | — |
| H-28 / H-04 (validación) | `features/role/dtos/RoleRequestDto.java:24-26` | `@Valid` en `permissions` para que se ejecute la validación anidada. No se añadió `@NotEmpty`: un rol sin permisos sigue siendo válido. | validación de contexto (no unitaria) | — |

### Excepciones nuevas

| Clase | Ubicación | HTTP | Mensaje |
|---|---|---|---|
| `CannotGrantPermissionsYouDoNotHaveException` | `features/role/exceptions/` | 403 | "No puede otorgar permisos que usted no tiene." |
| `CannotManageRoleWithMorePrivilegesException` | `features/role/exceptions/` | 403 | "No puede gestionar un rol con más privilegios que los suyos." |
| `CannotManageUserWithMorePrivilegesException` | `features/user/exceptions/` | 403 | "No puede gestionar un usuario con más privilegios que los suyos." |

Las tres extienden `ApiException`, por lo que `GlobalHandlerException` las serializa como
`ErrorResponseDto` con el `status` correspondiente.

### Detalle de las comprobaciones

- **Subconjunto de permisos**: se comparan los nombres de permiso (`RolePermission.getId().getPermission()`)
  contra `User.getAuthorities()`, que ya es la unión de permisos de todos los roles del actor.
- **Autoridades del objetivo**: misma comparación contra el usuario a modificar/desactivar/borrar.
  Un objetivo con `ADMINISTRATOR` nunca es subconjunto de un actor no-admin.
- **Orden en `update`**: autoedición → colisión de integrante → sede actual del objetivo →
  privilegios del objetivo → sede del miembro nuevo → permisos de los roles solicitados → mutaciones.
  Cualquier rechazo ocurre antes de `clear()` y de `save`.
- **Orden en `delete`**: privilegios del objetivo → sede → auto-borrado. Se conserva el mensaje y el
  comportamiento previos para el resto de casos.

## 2. Pruebas añadidas

`src/test/java/org/icc/pecesatierra/unit/user/UserServiceTest.java` (12 tests):

- `nonAdminCannotCreateUserWithAdministratorRole_H04` — 403 y `save` no invocado.
- `nonAdminCanCreateUserGrantingPermissionsItAlreadyHas_H04` — 201 lógico, rol asignado.
- `nonAdminCannotUpdateAdministrator_H21` — 403, usuario y roles intactos, `roleRepository` sin uso.
- `nonAdminCannotDeactivateAdministrator_H21` — 403, `active` intacto.
- `nonAdminCannotDeleteAdministrator_H21` — 403, `delete` no invocado.
- `nonAdminCannotMoveUserToMemberOfAnotherBranch_H05` — 409, `member` intacto.
- `passwordChangeRevokesRefreshTokens_H05` — lista de refresh tokens vacía.
- `updateWithoutPasswordChangeKeepsRefreshTokens_H05` — la lista se conserva.
- `administratorCanCreateUserWithAdministratorRole_H04`
- `administratorCanUpdateAnotherAdministrator_H21`
- `administratorCanDeactivateAnotherAdministrator_H21`
- `administratorCanDeleteAnotherAdministrator_H21`

`src/test/java/org/icc/pecesatierra/unit/role/RoleServiceTest.java` (8 tests):

- `nonAdminCannotCreateRoleWithAdministratorPermission_H04` — 403, `save` no invocado.
- `nonAdminCanCreateRoleWithPermissionsItAlreadyHas_H04` — rol creado con su permiso.
- `nonAdminCannotAddAdministratorPermissionToExistingRole_H04` — 403, mapper y `save` no invocados.
- `nonAdminCannotUpdateRoleWithMorePrivileges_H04` — 403, rol intacto.
- `nonAdminCannotDeleteRoleWithMorePrivileges_H04` — 403, `delete` no invocado.
- `administratorCanCreateRoleWithAdministratorPermission_H04`
- `administratorCanUpdateRoleWithMorePrivileges_H04`
- `administratorCanDeleteRoleWithAdministratorPermission_H04`

Los tests usan `TestFixtures.user(...)` y un helper local `role(...)` que construye `Role` y
`RolePermission` reales. No arrancan Spring ni tocan base de datos.

## 3. Resultado de la verificación

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

```
BUILD SUCCESSFUL
```

- **Suites**: 14
- **Tests totales**: 67
- **Ejecutados**: 62
- **Saltados**: 5 (4 IT con Testcontainers sin Docker: `ApplicationContextIT` y 3 de `AttendanceExportIT`; más el caso 403 de H-11, que sigue `@Disabled`)
- **Fallos**: 0
- **Errores**: 0

Los 20 tests nuevos (12 + 8) pasan en verde.

## 4. Autoverificación de mutación

En una copia temporal (`%TEMP%\opencode\mutacion-h04-h05-h21`, sin `build/`, `.gradle/` ni `bin/`)
se anularon las cinco comprobaciones nuevas con un `return` temprano:

- `UserService.validateCanGrantPermissions`
- `UserService.validateCanManageUser`
- `UserService.validateMemberBranch`
- `RoleService.validateCanGrantPermissions`
- `RoleService.validateCanManageRole`

Resultado de `gradlew test --tests "*UserServiceTest" --tests "*RoleServiceTest"` en la copia:

```
20 tests completed, 9 failed
```

Fallaron **los 9 tests de denegación** y siguieron pasando los 11 positivos/de administrador:

| Test mutado | Resultado |
|---|---|
| `UserServiceTest#nonAdminCannotCreateUserWithAdministratorRole_H04` | FAILED |
| `UserServiceTest#nonAdminCannotUpdateAdministrator_H21` | FAILED |
| `UserServiceTest#nonAdminCannotDeactivateAdministrator_H21` | FAILED |
| `UserServiceTest#nonAdminCannotDeleteAdministrator_H21` | FAILED |
| `UserServiceTest#nonAdminCannotMoveUserToMemberOfAnotherBranch_H05` | FAILED |
| `RoleServiceTest#nonAdminCannotCreateRoleWithAdministratorPermission_H04` | FAILED |
| `RoleServiceTest#nonAdminCannotAddAdministratorPermissionToExistingRole_H04` | FAILED |
| `RoleServiceTest#nonAdminCannotUpdateRoleWithMorePrivileges_H04` | FAILED |
| `RoleServiceTest#nonAdminCannotDeleteRoleWithMorePrivileges_H04` | FAILED |

La copia temporal se eliminó; el árbol de trabajo no se mutó en ningún momento. Repetir la suite
en el repositorio real volvió a dar 67 tests / 0 fallos.

## 5. Efectos colaterales y riesgos residuales

- **Flujos no-admin que dejan de funcionar** (comportamiento buscado):
  - Con `CREATE_USER`/`UPDATE_USER`, un no-admin ya no puede asignar roles cuyos permisos no tenga,
    ni tocar cuentas con permisos superiores (incluido cualquier `ADMINISTRATOR`), ni mover usuarios a
    otra sede.
  - Con `CREATE_ROLE`/`UPDATE_ROLE`/`DELETE_ROLE`, un no-admin ya no puede crear roles con
    `ADMINISTRATOR` ni con permisos ajenos, ni editar/borrar roles que ya los tengan (el rol
    ADMINISTRATOR queda fuera de su alcance).
- **Dependencia de `role_permission`**: si en la BD real un rol operativo tiene `CREATE_ROLE`,
  `UPDATE_ROLE` o `UPDATE_USER`, sus usuarios conservarán el endpoint pero verán 403 en los casos
  anteriores. No hay que sembrar nada nuevo; el comportamiento es más restrictivo, no menos.
- **Cuentas con más privilegios**: un coordinador de sede que antes gestionaba la cuenta de un
  administrador de su sede ahora recibe 403. Es el objetivo de H-21/VS-03; si el negocio necesita
  esa gestión, debe hacerse con una cuenta `ADMINISTRATOR`.
- **Cambio de contraseña**: el objetivo pierde sus refresh tokens (cierre de sesión en otros
  dispositivos) solo cuando la contraseña cambia de verdad. El access token vigente sigue siendo
  válido hasta su expiración; H-17 (tokens en `localStorage`, sin `jti`) no se aborda aquí.
- **Validación de roles**: `permissions: []` sigue siendo válido y `permissions: null` sigue dando
  400 por `@NotNull`. Un elemento nulo dentro del `Set` no se valida con `@Valid` (limitación de
  Bean Validation sobre elementos de colección); no se cambió la semántica.
- **H-45** (permisos duplicados por falta de `equals` en `PermissionRequestDto`) no se toca: sigue
  siendo un defecto independiente.
- **H-09** (`DELETE /api/users/{id}`) ya estaba corregido en la rama; esta corrección no lo altera.
- **Sin cambios de contrato HTTP**: los endpoints, sus firmas y sus códigos de éxito se mantienen;
  solo aparecen 403/409 en rutas que antes no los devolvían.

## 6. Archivos tocados

Producción:

- `src/main/java/org/icc/pecesatierra/features/user/service/UserService.java`
- `src/main/java/org/icc/pecesatierra/features/role/service/RoleService.java`
- `src/main/java/org/icc/pecesatierra/features/user/exceptions/CannotManageUserWithMorePrivilegesException.java` (nuevo)
- `src/main/java/org/icc/pecesatierra/features/role/exceptions/CannotGrantPermissionsYouDoNotHaveException.java` (nuevo)
- `src/main/java/org/icc/pecesatierra/features/role/exceptions/CannotManageRoleWithMorePrivilegesException.java` (nuevo)
- `src/main/java/org/icc/pecesatierra/features/role/dtos/RoleRequestDto.java`
- `src/main/java/org/icc/pecesatierra/features/role/dtos/PermissionRequestDto.java`

Tests:

- `src/test/java/org/icc/pecesatierra/unit/user/UserServiceTest.java` (nuevo)
- `src/test/java/org/icc/pecesatierra/unit/role/RoleServiceTest.java` (nuevo)
