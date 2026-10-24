# Contrato de alcance — Auditoría de las implementaciones de la rama

- **Rama**: `audit/arquitectura-y-seguridad`
- **Fecha**: 2026-10-24
- **Base**: `master` (merge-base real `f3d07cc`; el trabajo de la rama empieza ahí)
- **Objetivo**: auditar las implementaciones hechas después de la auditoría inicial, no re-auditar
  todo el código base. Se incluyen los fixes de Fase 0 porque son la base de los cambios posteriores.
- **Método**: dos auditores en paralelo (seguridad y calidad/pruebas) y un tercer auditor que revisa
  los dos informes. Ningún auditor modifica código; cada uno escribe solo su informe en `audit/`.

> **Estado post-corrección (2026-10-24):** M-02..M-24 corregidos salvo los pendientes de
> `audit/TODO.md`; H-01 y M-01 abiertos. Suite: 135 tests (131 ejecutados, 4 saltados, 0 fallos).
> `master` publicado en `origin/master` (`fdd5ea6`). Ver `audit/fixes-m02-m24.md` y
> `audit/INFORME-FINAL.md`.

## Alcance funcional

Hallazgos implementados y sujetos a revisión:

| Hallazgo | Cambio | Commit |
|---|---|---|
| H-02 | `AttendanceSpecification` aplica sede aunque `dto` sea nulo | `9f1e7ae` |
| H-03 | `DiscipuladoService.progress/updateProgress` valida sede de integrante y maestro; controller pasa el usuario | `2ceb3f2` |
| H-04 | `UserService`/`RoleService` aplican el principio de subconjunto al conceder permisos | `7a73080` |
| H-05 | `UserService.update` valida la sede del nuevo miembro y revoca refresh tokens al cambiar contraseña | `7a73080` |
| H-07 | `BaptismService` persiste `note` y `municipality` correctamente | `9ab51f6` |
| H-08 | `BaptismService` compara sedes por `getId()` | `9ab51f6` |
| H-09 | `UserController.delete` corrige el SpEL de `DELETE_USER` | `7856d51` |
| H-10 | Servicios normalizan DTO nulo en búsquedas/calendario | `b9c3b83` |
| H-11 | `GlobalHandlerException` devuelve 403/400/401 correctamente | `b9c3b83` |
| H-12 | `JwtAuthFilter` captura JWT inválido y usuario borrado y devuelve 401 | `b9c3b83` |
| H-20 | `BranchService.update/delete` limita a la sede del usuario no-admin | `3dc9dfb` |
| H-21 | `UserService.update/updateActive/delete` protege cuentas con más privilegios | `7a73080` |
| H-22 | `MemberPersistenceService.update` comprueba sede antes del mapper | `3dc9dfb` |
| H-31 | `MemberService.delete` borra la foto por `publicId` | `0c1191a` |
| H-46 | `RestTemplateConfig` con timeouts finitos | `e4442f1` |
| H-48 | `ServiceController.delete` corrige la precedencia de `isActive` | `7856d51` |
| H-01 parcial | `SqlExecutor` usa `aiJdbcTemplate` con rol `peces_ai_readonly`, `maxRows=1000`, `queryTimeout=10`, `AI_DB_URL` opcional sobre `DB_URL` | `8d27d36`, `7609417` |

Fuera de alcance: H-06 (descartado por regla de negocio), H-41 (TODO aceptado), los hallazgos no
implementados y el código base que no fue tocado (salvo interacciones con estos cambios).

## División entre auditores

### Auditor 1 — Seguridad y aislamiento

- H-02, H-03, H-04, H-05, H-09, H-10, H-11, H-12, H-20, H-21, H-22, H-48.
- Aislamiento del endpoint de IA: `AiDataSourceConfig`, `SqlExecutor`, rol `peces_ai_readonly`,
  `AI_DB_URL`/`AI_DB_USER`/`AI_DB_PASS`, límites y `@Transactional`.
- Busca bypasses, escalado de privilegios, IDOR entre sedes, fugas de información, errores HTTP que
  revelen o permitan algo indebido, y regresiones de autorización.
- Evalúa si los tests de seguridad prueban de verdad el contrato (matriz de 4/69, SpEL con
  superusuario, etc.) y qué queda como riesgo residual.

### Auditor 2 — Calidad, correctitud y pruebas

- H-07, H-08, H-31, H-46 y la parte funcional de H-10/H-11/H-12/H-20/H-22.
- `AiDataSourceConfig` como infraestructura: `DriverManagerDataSource`, backoff de
  `JdbcTemplateAutoConfiguration`, `AI_DB_URL` opcional, arranque sin variables, perfil de test.
- Calidad de la suite: ¿los tests fallan si se revierte el fix? ¿Hay over-mocking, aserciones
  tautológicas, `@Disabled` injustificados o tests que codifican bugs? Ejecuta mutaciones.
- Regresiones: cambios de comportamiento legítimos, arranque, configuración, documentación
  (`AGENTS.md`, `audit/TODO.md`, `audit/INFORME-FINAL.md`) y conteos.

Si un hallazgo cae en ambos ejes, el auditor dueño del riesgo lo analiza y el otro lo referencia sin
duplicar el análisis. El auditor 3 reconciliará.

## Reglas de evidencia

1. Cada afirmación debe citar `archivo:línea` y una cita literal breve.
2. Nada de especulaciones: si no se puede verificar, `NO VERIFICABLE`.
3. Distinguir bug real, riesgo latente y deuda.
4. Los tests deben ejecutarse con:
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
   ```
   Docker no está disponible: 4 tests de integración se saltan. No consultes Neon ni uses credenciales
   reales; no imprimas secretos.
5. Para las mutaciones, trabajá en una copia temporal fuera del repo y restaurá los archivos.

## Rúbrica

- **Crítica**: bypass de autorización, escritura/lectura entre sedes, ejecución arbitraria, secreto real.
- **Alta**: escalado de privilegios, fuga de PII, corrupción de datos.
- **Media**: requiere condiciones, impacto acotado o deuda que ya causa bugs.
- **Baja**: mantenibilidad, consistencia, rendimiento menor.

## Formato de los informes (auditores 1 y 2)

```markdown
# Auditoría de implementaciones — <eje>
## Veredicto global
## Resultado de ejecución
## Tabla por hallazgo
| H | Veredicto (CONFIRMADO/PARCIAL/REFUTADO/NO VERIFICABLE) | Evidencia | ¿Test lo protege? | Riesgo residual |
## Hallazgos nuevos
### SA-01 / SB-01 — ...
## Regresiones y efectos colaterales
## Cobertura y pruebas
## Límites
```

Máximo 20 hallazgos nuevos por auditor, ordenados por severidad. Preferí 10 sólidos a 20 especulativos.

## Auditor 3 — meta-auditoría

Lee los dos informes, verifica cada afirmación contra el código, busca contradicciones y huecos que
ambos hayan pasado por alto, ejecuta la suite y al menos 4 mutaciones de comprobación, y escribe
`audit/verificacion-implementaciones.md` con:

- Veredicto consolidado y grado de fiabilidad de cada informe.
- Tabla de hallazgos verificados (CONFIRMADO/PARCIAL/REFUTADO) y discrepancias entre auditores.
- Riesgos residuales priorizados y plan de remediación.
- Qué no se pudo verificar (Docker, Neon, runtime).
