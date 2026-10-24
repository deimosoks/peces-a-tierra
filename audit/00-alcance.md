# Contrato de alcance — Auditoría técnica (rama `audit/arquitectura-y-seguridad`)

Este documento es la referencia común para la ola 1 (análisis) y la ola 2 (verificación).
Ningún agente modifica código de producción. Solo se escriben archivos dentro de `audit/`.

## Contexto del repositorio

- Ruta: `C:\dev\peces-a-tierra`
- API REST Spring Boot 3.5.9 / Java 21, PWA Angular precompilada en `src/main/resources/static/`.
- 243 archivos Java bajo `src/main/java/org/icc/pecesatierra`.
- Features: `category`, `member`, `service`, `user`, `auth`, `discipulado`, `role`, `attendance`,
  `baptism`, `branch`, `type`, `ai`, `report`, `dashboard`, `log`, `health`.
- Documento de convenciones del repo: `AGENTS.md` (léelo, pero verifica cada afirmación contra el código).

## Baseline de build (ya comprobado por el orquestador)

- Con el JDK del sistema (25) el wrapper falla: `Unsupported class file major version 69`.
- Comando canónico que funciona (exit 0):

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; .\gradlew.bat compileJava
```

- NO ejecutar `bootRun` ni `test`: requieren variables de entorno (`DB_URL`, `DB_USER`, `DB_PASS`,
  `JWT_SECRET`, credenciales de Cloudinary, `GEMINI_API_KEY`) que no están configuradas.
- No ejecutar comandos git que cambien el estado (checkout, commit, stash).

## Reglas de evidencia

1. Todo hallazgo debe citar `ruta/archivo.java:línea` y una cita literal breve del código.
2. Prohibido especular: si no se puede verificar leyendo el código, se marca `NO VERIFICABLE`.
3. Nada de recomendaciones genéricas ("usar DTOs", "mejorar tests") sin señalar el archivo y el impacto concreto.
4. Cada hallazgo indica si es un problema real en producción, un riesgo latente o deuda técnica.
5. Si algo parece grave pero el código lo mitiga en otra capa, se documenta en la sección
   "Descartados" con el motivo (esto alimenta la ola 2).

## Rúbrica de severidad

- **Crítica**: explotable sin autenticación o con impacto total (bypass de autorización, SQLi/RCE,
  secretos reales en el repo, pérdida masiva de datos).
- **Alta**: escalado de privilegios, IDOR entre sucursales, fuga de datos personales, corrupción de datos.
- **Media**: requiere condiciones o privilegios, impacto acotado, o deuda que ya provoca bugs funcionales.
- **Baja**: mantenibilidad, consistencia, rendimiento menor, estilo.

## Formato obligatorio de los borradores (ola 1)

```markdown
# Borrador de auditoría — <eje>

## Resumen ejecutivo
(5-10 líneas: los 3 riesgos principales y el estado general)

## Tabla de hallazgos
| ID | Severidad | Título | Archivo:línea | Tipo (real/latente/deuda) |

## Detalle por hallazgo
### Q-01 — <título>
- **Severidad**: Alta
- **Ubicación**: `ruta:línea`
- **Evidencia**: (cita literal del código)
- **Impacto**: (qué pasa en producción, para quién)
- **Recomendación**: (cambio concreto)
- **Esfuerzo**: S / M / L

## Descartados (falsos positivos o mitigados)
| Qué parecía | Por qué no es hallazgo | Evidencia |

## Límites de la revisión
(qué no se pudo verificar y por qué)
```

Máximo 25 hallazgos por eje, ordenados por severidad. Preferible 15 sólidos que 25 especulativos.

## Frontera entre ejes (para no duplicar)

- **Calidad (Q)**: capas y dependencias entre paquetes, transacciones y lazy loading, N+1 y consultas,
  mappers MapStruct, manejo de errores y códigos HTTP, duplicación, consistencia de contratos,
  paginación, testabilidad, build/reproducibilidad.
- **Seguridad (S)**: autenticación, JWT, autorización y permisos, IDOR y branch scoping, inyección
  (incluido SQL del endpoint de IA), uploads, CORS/CSRF/headers, secretos, PII en logs, dependencias.
- Un hallazgo que cruza ambos ejes se reporta en el eje dueño del riesgo y el otro eje lo menciona
  como referencia, sin duplicar el análisis.
