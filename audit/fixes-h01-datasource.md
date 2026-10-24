<!-- estado-doc -->
> **Estado posterior (2026-10-24).** Este informe es histórico. La suite final tiene 135 tests (131 ejecutados, 4 saltados, 0 fallos); los pendientes están en `audit/TODO.md` y el estado consolidado en `audit/INFORME-FINAL.md`.

# H-01 (parcial) — Datasource de solo lectura para el endpoint de IA

- **Rama**: `audit/arquitectura-y-seguridad`
- **Fecha**: 2026-10-24
- **Alcance**: cablear `SqlExecutor` a una conexión dedicada con el rol `peces_ai_readonly`, con
  topes defensivos de filas y tiempo. **No cierra H-01**: sigue pendiente migrar a vistas seguras,
  revocar `SELECT` sobre `public` y aplicar scoping por sede.
- **Restricciones respetadas**: sin commits, sin cambio de rama, sin dependencias nuevas, sin tocar
  `AiService`, el validador SQL, `SecurityConfig`, `build.gradle` ni los informes de auditoría.

## Diseño elegido

`AiDataSourceConfig` expone **un único bean** `aiJdbcTemplate` (`JdbcTemplate`) construido sobre un
`DriverManagerDataSource` interno:

```java
JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
jdbcTemplate.setMaxRows(1000);
jdbcTemplate.setQueryTimeout(10);
```

`SqlExecutor` lo recibe con `@Qualifier("aiJdbcTemplate")` en una inyección por constructor
explícita.

### Por qué no un segundo bean `DataSource`

Declarar un `DataSource` adicional haría que `DataSourceAutoConfiguration` se retire
(`@ConditionalOnMissingBean(DataSource.class)`): el datasource principal de JPA dejaría de
configurarse y `@ServiceConnection` de Testcontainers perdería su punto de sustitución. El
`DriverManagerDataSource` queda como detalle interno, no como bean, y el ciclo de vida es trivial
porque no hay pool que cerrar.

**Efecto colateral asumido**: al existir un bean `JdbcOperations`, `JdbcTemplateAutoConfiguration`
tampoco crea su `jdbcTemplate` por defecto. Hoy `SqlExecutor` es el único consumidor de
`JdbcTemplate` en el repositorio (verificado con búsqueda en `src/`), así que no hay impacto. Si en
el futuro otro componente necesita el datasource principal, debe declarar su propio template con el
`DataSource` principal o añadir un `@Bean` explícito; no debe inyectar `JdbcTemplate` por tipo.

### Por qué se elimina `@Transactional(readOnly = true)`

Ese template no participa en el `PlatformTransactionManager` principal: su `DataSource` es
independiente, así que la anotación no abriría una transacción de solo lectura sobre esta conexión.
Quien garantiza la lectura es el rol de PostgreSQL `peces_ai_readonly`. Los topes de filas y tiempo
los impone el template dedicado.

## Archivos tocados

| Archivo | Cambio |
|---|---|
| `src/main/java/org/icc/pecesatierra/configurations/AiDataSourceConfig.java` (nuevo) | Bean `aiJdbcTemplate` sobre `DriverManagerDataSource` con `maxRows=1000` y `queryTimeout=10`. |
| `src/main/java/org/icc/pecesatierra/utils/sql_executor/SqlExecutor.java` | Constructor explícito con `@Qualifier("aiJdbcTemplate")`; se quita Lombok y `@Transactional(readOnly = true)`. Firma pública intacta. |
| `src/main/resources/application.properties` | Propiedades `ai.datasource.*` con placeholders de entorno, sin valores reales. |
| `src/test/resources/application-test.properties` | Valores locales ficticios para arrancar el contexto sin Neon. |
| `src/test/java/org/icc/pecesatierra/unit/ai/AiDataSourceConfigTest.java` (nuevo) | 3 tests del bean y del cableado. |
| `src/test/java/org/icc/pecesatierra/unit/ai/SqlExecutorTest.java` (nuevo) | 2 tests de delegación y propagación de errores. |

## Variables de entorno

```properties
ai.datasource.url=${AI_DB_URL:${DB_URL}}
ai.datasource.username=${AI_DB_USER}
ai.datasource.password=${AI_DB_PASS}
ai.datasource.driver-class-name=org.postgresql.Driver
```

`AI_DB_USER` y `AI_DB_PASS` **no tienen default**: son obligatorias para arrancar la aplicación
(incluido `bootRun`). `AI_DB_URL` es opcional y, si no se define, usa `DB_URL`: la IA apunta a la
misma base que la app, pero con un rol de solo lectura. En tests se sustituyen por los valores
ficticios de `application-test.properties`. No se commiteó host, usuario ni contraseña de Neon. El
rol y sus grants se crean con `audit/scripts/neon-ai-readonly.sql`.

## Límites defensivos

| Límite | Valor | Qué hace |
|---|---|---|
| `maxRows` | 1000 | `Statement.setMaxRows(1000)`: corta las filas devueltas. No evita que una agregación (`string_agg`, `array_agg`) devuelva mucho contenido en una sola fila. |
| `queryTimeout` | 10 s | `Statement.setQueryTimeout(10)`: el driver PostgreSQL cancela la sentencia al superar el tiempo. Acota `pg_sleep` y consultas costosas. |

## Tests añadidos

### `AiDataSourceConfigTest` (3)

Usa `ApplicationContextRunner` con `AiDataSourceConfig`, `SqlExecutor` y `PropertyPlaceholderAutoConfiguration`;
no abre conexiones (el `DriverManagerDataSource` conecta de forma perezosa).

- `aiJdbcTemplateHasDefensiveLimitsAndDedicatedDatasource_H01`: `getMaxRows()==1000`,
  `getQueryTimeout()==10`, el `DataSource` es `DriverManagerDataSource`, URL esperada y
  `getUsername()=="test"`.
- `driverManagerDataSourceIsNotExposedAsBean_H01`: no hay ningún bean `DataSource`.
- `sqlExecutorIsWiredToTheAiJdbcTemplate_H01`: el campo `jdbcTemplate` de `SqlExecutor` es el bean
  `aiJdbcTemplate` (verifica el qualifier).

### `SqlExecutorTest` (2)

- `delegatesQueryToTheInjectedJdbcTemplate_H01`: con `JdbcTemplate` mockeado, `sqlExecutor("SELECT 1")`
  llama a `queryForList("SELECT 1")` y devuelve el mismo valor.
- `propagatesJdbcErrorsInsteadOfSwallowingThem_H01`: una `QueryTimeoutException` del template se
  propaga (no se silencia el fallo ni se devuelve una lista vacía).

Ningún test se conecta a Neon ni usa la contraseña real.

## Resultado de `test`

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat cleanTest test --console=plain
```

```text
> Task :test

BUILD SUCCESSFUL in 1m 7s
6 actionable tasks: 3 executed, 3 up-to-date
```

Agregado de `build/test-results/test`:

| Métrica | Valor |
|---|---|
| Total | 106 |
| Ejecutados | 102 |
| Saltados | 4 |
| Fallos | 0 |
| Errores | 0 |

Los 4 saltados son los de integración que dependen de Docker (`ApplicationContextIT`, 1;
`AttendanceExportIT`, 3). Los 5 tests nuevos se ejecutaron y pasaron:
`AiDataSourceConfigTest` 3/3 y `SqlExecutorTest` 2/2.

## Advertencias

- **Sin pool**: `DriverManagerDataSource` abre y cierra una conexión por consulta. Cada consulta de
  IA paga handshake y TLS contra Neon; es aceptable para un endpoint de baja frecuencia, pero no
  debe reutilizarse para tráfico alto. Si hiciera falta, la alternativa es un `HikariDataSource`
  creado dentro del bean (sin exponerlo como bean `DataSource`), con pool pequeño y timeout de
  conexión.
- **`AI_DB_USER`/`AI_DB_PASS` ahora son obligatorias** para arrancar en producción y en `bootRun`.
  `AI_DB_URL` es opcional y cae a `DB_URL`. Un despliegue sin las credenciales de IA falla al resolver
  placeholders, no al conectar.
- **La confidencialidad sigue abierta**: el rol tiene `SELECT` sobre todas las tablas de `public`
  (riesgo aceptado en `audit/TODO.md`), de modo que un SQL generado por el LLM todavía puede leer
  `users.password_hash`, `refresh_token` y PII de cualquier sede. Este cambio solo limita escritura,
  filas y tiempo.
- **`maxRows` no acota el coste**: limita filas devueltas, no CPU/IO del servidor; `queryTimeout` es
  el que acota el tiempo.
- No se rota la contraseña ni se configura el gestor de secretos en este cambio; eso queda en el
  runbook de H-01.
- El aviso de compilación sobre API deprecada en `AiService` es preexistente y no proviene de este
  cambio.

## Pendiente de H-01

1. Crear el esquema `ai_read` con vistas que excluyan columnas sensibles y PII.
2. Revocar `SELECT` sobre `public`; dejar `USAGE` + `SELECT` solo sobre `ai_read`.
3. Configurar la conexión de IA con `currentSchema=ai_read` (o cualificar las vistas).
4. Añadir filtro por sede en las vistas (`branch_id`) con RLS o `SET LOCAL app.branch_id`, e inyectar
   el usuario real en `AiService`/`SqlExecutor` en lugar de `new User()`.
5. Endurecer `SqlQueryValidator`: lista blanca de tablas/columnas, prohibir comentarios y `;`,
   bloquear `information_schema`/`pg_catalog`; el rol de BD no sustituye al validador.
6. Rotar la contraseña, guardarla en el gestor de secretos y verificar con
   `information_schema.role_table_grants` que el rol no puede leer `users`/`refresh_token`.
7. Añadir una prueba de integración que espere `permission denied` al consultar `password_hash`
   (requiere el esquema `ai_read` y un PostgreSQL con el rol aplicado).
