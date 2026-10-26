<!-- estado-doc -->
> **Informe de una ola concreta (2026-10-25).** Cubre H-14, H-19, H-18 y H-17 (lado servidor). El estado consolidado sigue en `audit/INFORME-FINAL.md` y los pendientes en `audit/TODO.md`.

# Correcciones H-14, H-19, H-18 y H-17 — datos personales, enumeración de usuarios, rate limiting y tokens

Fecha: 2026-10-25
Rama: `master`
Alcance: solo backend. No se toca `src/main/resources/static/`, ni el frontend, ni `build.gradle` (ninguna dependencia nueva), ni el esquema de la base de datos.

## Cambios

| Hallazgo | Archivo | Cambio | Test |
|---|---|---|---|
| H-14 | `features/ai/service/AiService.java`, `utils/ai/ResultRedactor.java` | El prompt de formateo ya no recibe `data.toString()`. Antes de construirlo se aplica `ResultRedactor.redact(...)`: enmascara a `[REDACTADO]` toda columna cuyo nombre contenga un fragmento de la lista conservadora (name, nombre, document, documento, cedula, cc, phone, celular, telefono, address, direccion, email, correo, note, nota, birth, nacimiento), envía como máximo 50 filas y, cuando trunca, añade un resumen numérico del resultado completo (min/max/suma/promedio/cantidad por columna no sensible). Las columnas numéricas y agregadas quedan intactas. | `unit/ai/ResultRedactorTest` (6) |
| H-19 | `features/auth/service/AuthService.java` | `login` ejecuta siempre `passwordEncoder.matches(...)` contra un hash BCrypt señuelo precomputado (coste 10, igual que el encoder real) cuando el usuario no existe o está inactivo. Mensaje y código HTTP sin cambios. | `unit/auth/AuthServiceTest`: `unknownUserStillPaysThePasswordHashCost_H19`, `inactiveUserStillPaysThePasswordHashCost_H19` |
| H-18 | `configurations/RateLimitFilter.java`, `utils/security/InMemoryRateLimiter.java`, `configurations/SecurityConfig.java` (bean `Clock`), `application.properties` | Filtro `OncePerRequestFilter` con ventana fija en memoria para `POST /api/auth/login` (IP remota + usuario del cuerpo) y `POST /api/ai/chat` (usuario autenticado, o IP si no hay principal). Límites y ventana por propiedad, con defaults 10 / 20 / 60 s. Al exceder: 429 con `ErrorResponseDto` y `Retry-After`. | `unit/security/InMemoryRateLimiterTest` (6), `unit/security/RateLimitFilterTest` (5), `web/security/RateLimitFilterRegistrationTest` (2) |
| H-17 (refresh) | `utils/security/TokenHasher.java`, `features/auth/service/RefreshTokenService.java`, `features/auth/IssuedRefreshToken.java`, `AuthService.java`, `configurations/SecurityService.java` | `refresh_token.token` guarda SHA-256 hex con pepper; toda búsqueda hashea la entrada (`findByRawToken`). El valor en claro solo viaja en `IssuedRefreshToken`. Se elimina `RefreshTokenMapper` (habría devuelto el hash al cliente). | `unit/auth/RefreshTokenServiceTest` (5) |
| H-17 (JWT) | `features/auth/service/JwtService.java` | El access token incorpora `iss`, `aud` y `jti`; el parser exige emisor y audiencia (`jwt.issuer` / `jwt.audience`). | `unit/auth/JwtServiceTest` (5) |

## Decisiones de diseño

### H-14 — redacción

- **Lista textual en una constante, sin reflexión.** El SQL lo genera un modelo: el alias de cada columna es arbitrario, así que no hay entidad que inspeccionar. Se compara el nombre normalizado (minúsculas y sin acentos) contra fragmentos; enmascarar de más solo cuesta un detalle cosmético, enmascarar de menos filtra PII a un tercero.
- **Fragmentos de dos caracteres (`cc`) por igualdad exacta** para no capturar `acceso` o `proceso`.
- **Truncado a 50 filas con resumen agregado**: el resumen se calcula sobre el resultado completo, de modo que "cuántos miembros hay" o "promedio de edades" siguen respondiéndose aunque la lista se recorte.
- El prompt de sistema del formateo explica que `[REDACTADO]` es deliberado, para que el modelo no invente nombres.
- La redacción ocurre en `buildFormattingUserPrompt`, es decir, en el único punto donde el resultado sale hacia el proveedor.

### H-19 — tiempos del login

- El hash señuelo es una constante pública a propósito: su resultado se descarta y solo aporta el coste de cómputo de BCrypt. No se genera al arrancar para no introducir una dependencia del estado del `PasswordEncoder` ni un coste de startup.
- Se cubren los dos caminos cortos: usuario inexistente/credenciales malas (excepción del `AuthenticationManager`) y usuario inactivo.

### H-18 — rate limiting

- **Almacenamiento**: `ConcurrentHashMap<clave, ventana>` con reloj inyectable. El contador se incrementa dentro de `compute`, que serializa por clave; no hay carrera entre peticiones simultáneas.
- **Acotado**: máximo 10 000 claves vivas por limitador. Al llegar al tope se purgan las ventanas expiradas y, si no basta, se descarta la más antigua. Un atacante que rote usuarios o IPs no puede hacer crecer el mapa.
- **Clave**: login = `login|IP|usuario`; IA = `ai|usuario`. La IP sale de `getRemoteAddr()` y **no** de `X-Forwarded-For`, que el cliente puede falsificar y usaría para saltarse el límite.
- **Cuerpo del login**: para discriminar por usuario sin agotar el `InputStream`, el filtro cachea el JSON y lo reproduce al controller. Solo lo hace si `Content-Length` es conocido y ≤ 8 KB; si no, la clave se queda sin usuario y el cuerpo sigue intacto.
- **Limitaciones asumidas**: el estado es por instancia y se pierde al reiniciar. Con varias instancias el límite efectivo se multiplica; si eso importa, hay que mover el contador a un almacén compartido. El filtro corre antes de `@PreAuthorize`, así que un usuario sin `CHAT_AI` también consume cuota de IA.
- Los tests fijan topes altos en `application-test.properties`; el test de registro del filtro usa `@TestPropertySource` con topes pequeños.

### H-17 — tokens

- **SHA-256 con pepper y no BCrypt**: el refresh token tiene 128 bytes de `SecureRandom`, no es adivinable y no se verifica contra un diccionario; BCrypt solo añadiría latencia a cada refresco.
- **`findByRawToken` centraliza el hasheo**, de modo que `AuthService.logout`, `SecurityService.isActive(RefreshTokenRequestDto)` y la rotación no pueden olvidarse de hashear.
- **Se borra `RefreshTokenMapper`**: mapeaba `token -> token` y, al pasar la entidad a guardar el hash, habría enviado el hash como si fuera el token. La respuesta se construye desde `IssuedRefreshToken`.
- **`iss`/`aud` obligatorios en el parser**: un token firmado con la misma clave para otro propósito deja de ser aceptado. `jti` abre la puerta a una lista de revocación de access tokens sin tocar el formato.

## Impacto en datos y despliegue

- **Refresh tokens**: no hay migraciones en el repo y no hace falta ninguna de esquema. `refresh_token.token` debe admitir 64 caracteres (SHA-256 hex); si la columna fuese más corta, hay que ampliarla antes de desplegar. Las filas escritas antes del cambio contienen el token en claro, ya no coinciden con ningún hash y se comportan como revocadas: **todos los usuarios inician sesión una vez más**.
- **Access tokens**: los emitidos antes del cambio no tienen `iss`/`aud` y se rechazan. Como duran 10 minutos, el impacto es un refresco de sesión tras el despliegue (que además reemite el refresh token hasheado).
- **Pepper**: `REFRESH_TOKEN_PEPPER` tiene valor por defecto. Definirlo en producción es recomendable; cambiarlo después invalida todas las sesiones.
- **Opcional, limpieza manual**: las filas antiguas en texto plano pueden borrarse con `DELETE FROM refresh_token WHERE length(token) <> 64;` una vez desplegado, o dejarse caducar (5 días).
- **Rate limiting**: sin cambios de despliegue. Si hay proxy inverso y se quiere limitar por IP real habría que confiar en una cabecera del proxy y ajustar el filtro de forma explícita (hoy se prefiere no confiar en cabeceras del cliente).

## Verificación

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat test --console=plain
```

| Métrica | Antes | Después |
|---|---|---|
| Clases | 40 | 46 |
| Tests | 217 | 248 |
| Fallos | 0 | 0 |
| Errores | 0 | 0 |
| Saltados | 4 | 4 |

Los 4 saltados son `ApplicationContextIT` y `AttendanceExportIT` (`@Testcontainers(disabledWithoutDocker = true)`, no hay Docker en la máquina). Consecuencia: el cableado de beans de H-17 (`TokenHasher`, `RefreshTokenService` en `SecurityService`) solo se comprueba con Docker; el de H-18 sí está cubierto sin Docker por `RateLimitFilterRegistrationTest`, que arranca `SecurityConfig` + `RateLimitFilter` reales.

## Riesgos residuales

- H-01 sigue abierto: el validador de SQL es una lista negra.
- El asistente sigue aplicando datos de todas las sedes (sin scoping por sede): H-14 reduce la PII enviada, no el alcance de la consulta.
- El rate limiter no distingue usuarios no autorizados de autorizados y no es distribuido.
