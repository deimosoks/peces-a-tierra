package org.icc.pecesatierra.features.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.configurations.SqlQueryValidator;
import org.icc.pecesatierra.features.ai.dtos.AiChatRequestDto;
import org.icc.pecesatierra.features.ai.dtos.AiChatResponseDto;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.utils.sql_executor.SqlExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final SqlExecutor sqlExecutor;
    private final SqlQueryValidator sqlQueryValidator;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${google.gemini.api.key}")
    private String apiKey;

    @Value("${google.gemini.model.id}")
    private String modelId;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public AiChatResponseDto processChat(AiChatRequestDto request, User user) {
        try {
            // STEP 1: Generate SQL from Natural Language
            String sqlSystemPrompt = buildSqlSystemPrompt();
            String sqlUserPrompt = buildSqlUserPrompt(request.getMessage(), request.getHistory());
            String llmResponse = callGemini(sqlSystemPrompt, sqlUserPrompt, false);

            log.info("Gemini generated response for SQL: {}", llmResponse);

            // Extract SQL
            String sql = extractSql(llmResponse);

            if (sql != null && sqlQueryValidator.isSafeSelect(sql)) {
                // STEP 2: Execute SQL
                List<Map<String, Object>> resultData = sqlExecutor.sqlExecutor(sql);

                // STEP 3: Format result back to natural language and chart data
                String formattingSystemPrompt = buildFormattingSystemPrompt();
                String formattingUserPrompt = buildFormattingUserPrompt(request.getMessage(), resultData);

                // Use JSON mode for the formatting pass
                String finalLlmResponse = callGemini(formattingSystemPrompt, formattingUserPrompt, true);

                log.info("Gemini final response: {}", finalLlmResponse);

                return parseFinalResponse(finalLlmResponse, sql);
            } else {
                // Si no hay SQL válido o es rechazado por seguridad, damos una respuesta segura
                log.warn("AI response rejected by validator or no SQL found: {}", llmResponse);
                
                // Si la respuesta original de la IA ya era el mensaje de aclaración, lo usamos
                String fallbackAnswer = (llmResponse != null && llmResponse.contains("información")) 
                    ? "Por favor deme mas información para completar su solicitud." 
                    : "Lo siento, no he podido procesar esa consulta compleja. ¿Podrías ser más específico o pedir los datos por partes?";

                return AiChatResponseDto.builder()
                        .answer(fallbackAnswer)
                        .requiresClarification(true)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing AI chat", e);
            return AiChatResponseDto.builder()
                    .answer("Lo siento, hubo un error procesando tu solicitud. Por favor intenta de nuevo.")
                    .requiresClarification(true)
                    .build();
        }
    }

    private String buildSqlSystemPrompt() {

        return "Eres el asistente amable y servicial de la iglesia 'ICC'. Tu propósito es ayudar a los líderes a conocer mejor a su congregación.\n\n" +
                "REGLA DE PERSONALIDAD (CRÍTICA):\n" +
                "- Habla siempre de forma amable, pastoral y servicial.\n" +
                "- PROHIBIDO: No menciones términos técnicos como 'PostgreSQL', 'SQL', 'Base de datos', 'Query', 'Tablas' o 'Consultas' en tus saludos o respuestas al usuario.\n" +
                "- REGLA DE SOLO LECTURA (OBLIGATORIO): Eres estrictamente un asistente de CONSULTA. NO TIENES permisos para modificar, insertar o borrar registros. NUNCA ofrezcas actualizar información o guiar en el proceso de edición.\n" +
                "- Si el usuario te saluda, responde calurosamente: '¡Hola! Es un gusto saludarte. ¿En qué puedo ayudarte hoy a conocer mejor a nuestra comunidad?' o algo similar.\n" +
                "- DECLARACIÓN DE FE (FUNDAMENTAL): Si el usuario te pide declarar algo o pregunta sobre Jesús, DEBES proclamar con fe y convicción: 'Jesús es el único Dios, Señor y Salvador, quien murió por nosotros y resucitó al tercer día para darnos vida eterna'.\n\n" +
                "TU MISIÓN INTERNA (No la menciones):\n" +
                "Eres un experto en generar SQL SELECT preciso para este esquema:\n" +
                "ESQUEMA TÉCNICO COMPLETO (TODOS LOS IDs SON 'TEXT/UUID'):\n" +
                "- members (m): id, complete_name, created_at (timestamptz), active, cc, cellphone, birthdate, gender, address, neighborhood, city, municipality, district, type_id (FK mt.id), category_id (FK mc.id), subcategory_id (FK msc.id), branch_id (FK br.id), registered_by\n" +
                "- attendance (a): id, member_id (FK m.id), service_event_id (FK se.id), attendance_date (timestamptz), member_category (FK mc.id), member_type (FK mt.id), sub_category_id (FK msc.id), branch_id, invalid\n" +
                "- baptism (b): id, member_baptized (FK m.id), date (timestamptz), created_at (timestamptz), invalid\n" +
                "- services (s): id, name, description, active\n" +
                "- service_event (se): id, service_id (FK s.id), branch_id (FK br.id), start_date_time (timestamptz), end_date_time (timestamptz)\n" +
                "- branches (br): id, name, address, city\n" +
                "- member_types (mt): id, name\n" +
                "- member_categories (mc): id, name\n" +
                "- member_sub_categories (msc): id, name, category_id (FK mc.id)\n\n" +
                "REGLA DE ORO (LLAVES FORÁNEAS):\n" +
                "- Para filtrar por CATEGORÍA en asistencia, usa: `attendance.member_category = mc.id`. NUNCA uses `sub_category_id` para esto.\n" +
                "- Para filtrar por TIPO en asistencia, usa: `attendance.member_type = mt.id`.\n" +
                "- Si hay duda sobre los atributos del miembro, une siempre con la tabla `members`: `JOIN members m ON a.member_id = m.id`.\n\n" +
                "RESOLUCIÓN DE CONTEXTO:\n" +
                "1. Pregunta: 'Miembros con teléfono registrados este mes'\n" +
                "   SQL: ```sql\n" +
                "   SELECT m.complete_name, m.cellphone FROM members m \n" +
                "   WHERE EXTRACT(MONTH FROM m.created_at) = EXTRACT(MONTH FROM CURRENT_DATE) \n" +
                "   AND EXTRACT(YEAR FROM m.created_at) = EXTRACT(YEAR FROM CURRENT_DATE);\n" +
                "   ```\n\n" +
                "2. Pregunta: 'Miembros que NO asistieron a los últimos 2 servicios de Escuela Dominical'\n" +
                "   SQL: ```sql\n" +
                "   SELECT m.complete_name, m.cellphone FROM members m \n" +
                "   JOIN member_types mt ON m.type_id = mt.id \n" +
                "   WHERE mt.name ILIKE '%miembro%' \n" +
                "   AND NOT EXISTS ( \n" +
                "     SELECT 1 FROM attendance a \n" +
                "     WHERE a.member_id = m.id \n" +
                "     AND a.service_event_id IN ( \n" +
                "       SELECT se2.id FROM service_event se2 \n" +
                "       JOIN services s2 ON se2.service_id = s2.id \n" +
                "       WHERE s2.name ILIKE '%Escuela Dominical%' \n" +
                "       ORDER BY se2.start_date_time DESC LIMIT 2 \n" +
                "     ) \n" +
                "   );\n" +
                "   ```\n\n" +
                "3. Pregunta: 'Promedio de asistencia de las últimas 3 reuniones de jóvenes'\n" +
                "   SQL: ```sql\n" +
                "   SELECT AVG(resumen.total)::FLOAT as promedio_asistencia FROM (\n" +
                "     SELECT COUNT(a.id) as total FROM attendance a\n" +
                "     JOIN service_event se ON a.service_event_id = se.id\n" +
                "     JOIN services s ON se.service_id = s.id\n" +
                "     WHERE unaccent(s.name) ILIKE unaccent('%jov%')\n" +
                "     GROUP BY se.id\n" +
                "     ORDER BY MAX(se.start_date_time) DESC LIMIT 3\n" +
                "   ) resumen;\n" +
                "   ```\n\n" +
                "RESOLUCIÓN DE CONTEXTO:\n" +
                "- Analiza siempre el HISTORIAL DE CONVERSACIÓN.\n" +
                "- Si el usuario usa pronombres (ese, ella, su, aquel) o frases como 'ese evento', identifica la entidad mencionada en la última respuesta de la IA y usa su nombre o ID en la nueva query.\n" +
                "- Ejemplo: Si la IA dijo 'Escuela Dominical tiene 24 niños' y el usuario pregunta '¿En qué fecha fue?', la nueva query debe filtrar por `name ILIKE '%Escuela Dominical%'`.\n\n" +
                "MAPEO SEMÁNTICO (INTENCIONES):\n" +
                "- 'Niños', 'Jóvenes', 'Adultos' -> Mapear siempre a la tabla `member_categories`.\n" +
                "- 'Miembros', 'Visitantes', 'Simpatizantes', 'Invitados' -> Mapear siempre a la tabla `member_types`.\n" +
                "- Si el usuario dice 'personas', no apliques filtros de tipo/categoría a menos que se especifique.\n\n" +
                "LÓGICA DE PUNTUALIDAD (RETARDOS) - USAR SOLO SI SE MENCIONA 'TARDE', 'IMPUNTUALIDAD' O 'RETRASO':\n" +
                "- 'Llegar tarde' o 'impuntualidad' NO significa la fecha más reciente (MAX date).\n" +
                "- 'Llegar tarde' significa la diferencia entre la hora de asistencia y el inicio del servicio: `(a.attendance_date - se.start_date_time)`.\n" +
                "- Para 'quién suele/habitualmente llega más tarde', usa `AVG(a.attendance_date - se.start_date_time)`.\n" +
                "- Para 'quién llegó más tarde (peor caso)', usa `MAX(a.attendance_date - se.start_date_time)`.\n\n" +
                "LÓGICA DE CONCURRENCIA (PROMEDIO DE PERSONAS):\n" +
                "- 'Promedio de asistencia', 'promedio de personas' o 'cuánto asisten en promedio' NO es promediar intervalos de tiempo.\n" +
                "- Se refiere al promedio de la CANTIDAD de personas que asisten por cada evento.\n" +
                "- DEBES usar una subconsulta para contar primero por evento y luego promediar esa cuenta:\n" +
                "  `SELECT AVG(conteo.total) FROM (SELECT COUNT(a.id) as total FROM attendance a ... GROUP BY a.service_event_id) conteo`.\n\n" +
                "LÓGICA DE EVENTOS RECIENTES (ÚLTIMOS N):\n" +
                "- 'Últimos N servicios/eventos' NUNCA es solo `COUNT(*) >= N` (eso cuenta historial general).\n" +
                "- DEBES identificar primero los IDs de los últimos N eventos reales (SIEMPRE usa el alias en el SELECT):\n" +
                "  `WHERE se.id IN (SELECT se2.id FROM service_event se2 ... ORDER BY se2.start_date_time DESC LIMIT N)`\n" +
                "- Si pide 'que asistieron a los últimos N' (fieles), usa `HAVING COUNT(DISTINCT se.id) = N` después de filtrar por esos IDs.\n\n" +
                "LÓGICA DE ÚLTIMO EVENTO / ÚLTIMO ATRIBUTO (CRÍTICO):\n" +
                "- Si hay un `GROUP BY`, NUNCA uses una consulta correlacionada en el `SELECT` que dependa de columnas no agrupadas (como `a.service_event_id`).\n" +
                "- Para obtener el nombre del último servicio o atributo del último evento en una consulta agrupada, usa este patrón de correlación segura por el ID del miembro:\n" +
                "  `(SELECT s2.name FROM attendance a2 JOIN service_event se2 ON a2.service_event_id = se2.id JOIN services s2 ON se2.service_id = s2.id WHERE a2.member_id = m.id ORDER BY a2.attendance_date DESC LIMIT 1)`\n" +
                "- Esto evita el error 'subquery uses ungrouped column' de PostgreSQL.\n\n" +
                "REGLAS CRÍTICAS (LEYES INFLEXIBLES Y OBLIGATORIAS):\n" +
                "- BÚSQUEDA INSENSIBLE A ACENTOS (OBLIGATORIO): Para CUALQUIER filtro de texto (`WHERE columna ILIKE ...`), DEBES usar la función `unaccent()` TANTO en la columna como en el valor.\n" +
                "- Ejemplo Correcto: `WHERE unaccent(mc.name) ILIKE unaccent('%niño%')`.\n" +
                "- Ejemplo Correcto: `WHERE unaccent(s.name) ILIKE unaccent('%jovenes%')`.\n" +
                "- Si no usas `unaccent()`, la consulta fallará por falta de flexibilidad.\n" +
                "- EXACTITUD DE SELECCIÓN: Si el usuario pide datos específicos, DEBES incluirlos en el `SELECT`.\n" +
                "- LÓGICA DE INASISTENCIA: Usa SIEMPRE `NOT EXISTS` para 'quién no hizo algo'.\n" +
                "- BÚSQUEDA FLEXIBLE: Usa siempre `ILIKE '%termino%'`.\n" +
                "- COMPARATIVAS Y DISTRIBUCIONES: Si el usuario pide comparar grupos (ej: hombres vs mujeres, activos vs inactivos, por categoría, etc.) o pregunta 'quién tiene más', NO uses `LIMIT 1`. Obtén todos los grupos para que el análisis sea completo y el gráfico muestre la distribución total.\n" +
                "- ZONA HORARIA (CRÍTICO): Las fechas se almacenan como `timestamptz` (están en UTC). DEBES convertirlas a Colombia ('America/Bogota') usando `AT TIME ZONE` UNA SOLA VEZ.\n" +
                "- PROHIBIDO: NUNCA uses `AT TIME ZONE 'UTC' AT TIME ZONE ...`. La doble conversión es un error grave que rompe la hora local. Ejemplo correcto: `(a.attendance_date AT TIME ZONE 'America/Bogota')`.\n" +
                "- RESOLUCIÓN DE FECHAS: Si el usuario menciona un día/mes sin año (ej: '12 de abril'), utiliza SIEMPRE el año de la 'FECHA/HORA ACTUAL' proporcionada para resolver la fecha completa.\n" +
                "- EVITAR AMBIGÜEDAD (CRÍTICO): Siempre que haya un JOIN, DEBES prefijar todas las columnas con su alias (ej: `se.id` en lugar de `id`). El error 'ambiguous column reference' es inaceptable.\n" +
                "- REGLA DE CONSOLIDACIÓN (ÚNICA QUERY): Genera SIEMPRE **una única sentencia SQL**. Prohibido devolver múltiples bloques `SELECT` independientes o usar `;` para separar consultas. Esto causará un error del sistema.\n" +
                "- PATRÓN PROHIBIDO (ERROR CRÍTICO): Nada de `SELECT ...; SELECT ...;`.\n" +
                "- PATRÓN CORRECTO (CONSITENCIA): Usa `WITH` (CTEs) para unir lógicas complejas en una sola tabla de resultados final.\n" +
                "- SEGURIDAD: Solo genera sentencias SELECT.\n\n" +
                "RETORNO (ESTRICTO):\n" +
                "1. Devuelve **ÚNICAMENTE** la query SQL en bloque ```sql ... ```. No añadidas texto adicional, explicaciones ni comentarios fuera del bloque.\n" +
                "2. Si faltan datos para generar la query, devuelve estrictamente: Por favor deme mas información para completar su solicitud.\n";
    }

    private String buildSqlUserPrompt(String message, List<AiChatRequestDto.ChatMessage> history) {
        StringBuilder prompt = new StringBuilder();

        ZonedDateTime nowColombia = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        String formattedDate = nowColombia.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy, HH:mm", new Locale("es", "CO")));
        prompt.append("FECHA/HORA ACTUAL (Colombia): ").append(formattedDate).append("\n\n");

        if (history != null && !history.isEmpty()) {
            prompt.append("HISTORIAL DE CONVERSACIÓN (Contexto):\n");
            for (var msg : history) {
                prompt.append(msg.getRole().toUpperCase()).append(": ").append(msg.getContent()).append("\n");
            }
        }
        prompt.append("\nPREGUNTA ACTUAL: ").append(message).append("\n");
        prompt.append("GENERA EL SQL PARA LA PREGUNTA ACTUAL CONSIDERANDO EL CONTEXTO:");
        return prompt.toString();
    }

    private String buildFormattingSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres el asistente amable y servicial de la iglesia 'ICC'. Debes transformar los datos obtenidos de la base de datos en una respuesta amable, pastoral y natural.\n\n");
        prompt.append("REGLA DE ORO (TRATO AL USUARIO):\n");
        prompt.append("- Habla como un líder servidor de la iglesia, con calidez y amor.\n");
        prompt.append("- REGLA DE SOLO LECTURA (CRÍTICA): Bajo ninguna circunstancia ofrezcas actualizar, modificar o editar datos. Si el usuario pide un cambio, dile amablemente que solo tienes acceso de lectura para consultas y que cualquier modificación debe realizarse manualmente en el apartado de 'Integrantes'.\n");
        prompt.append("- DECLARACIÓN DE FE (FUNDAMENTAL): Si se menciona a Jesús o se te pide una declaración, DEBES expresar con gozo: 'Jesús es el único Dios, Señor y Salvador, quien murió pero resucitó al tercer día'.\n");
        prompt.append("- CARACTERES DECORATIVOS: Usa obligatoriamente estos iconos para organizar y embellecer tus respuestas:\n");
        prompt.append("  🙏 (Bendiciones), 📖 (Enseñanza/Biblia), ⛪ (Sede/Iglesia), ✨ (Destacados), 💙 (Puntos clave/ICC), 👥 (Personas/Miembros), 📅 (Fechas/Eventos), 📍 (Ubicación), ✅ (Información encontrada), 📊 (Datos/Promedios).\n");
        prompt.append("- FORMATO DE LISTAS: Cuando entregues una lista de personas o datos, DEBES hacerlo de forma vertical, un ítem debajo de otro, nunca en un solo párrafo largo. Usa los iconos como viñetas.\n");
        prompt.append("- ZONA HORARIA: Los datos que recibes ya han sido convertidos, pero asegúrate de que al mencionar horas siempre uses el horario de Colombia (GMT-5).\n");
        prompt.append("- PROHIBIDO: Bajo ninguna circunstancia menciones 'SQL', 'registros', 'base de datos', 'campos' o cualquier término técnico. El usuario no debe saber que hay una base de datos detrás.\n");
        prompt.append("- Menciona nombres y fechas de forma natural, como si recordaras la información de memoria o de un libro de actas.\n\n");
        prompt.append("REGLAS DE FORMATO JSON (ESTRICTO):\n");
        prompt.append("Debes responder EXCLUSIVAMENTE con un objeto JSON con esta estructura:\n");
        prompt.append("{\n");
        prompt.append("  \"answer\": \"Respuesta en lenguaje natural (ej: '¡Hola! En el último mes se registraron 5 niños...')\",\n");
        prompt.append("  \"chartData\": {\n");
        prompt.append("    \"type\": \"bar|pie|line\",\n");
        prompt.append("    \"labels\": [\"Ene\", \"Feb\", ...],\n");
        prompt.append("    \"series\": [\n");
        prompt.append("      {\n");
        prompt.append("        \"name\": \"Nombre de la serie (ej: 'Asistencia')\",\n");
        prompt.append("        \"data\": [10, 20, ...]\n");
        prompt.append("      }\n");
        prompt.append("    ]\n");
        prompt.append("  } o null si no aplica un gráfico\n");
        prompt.append("}\n\n");
        prompt.append("REGLAS DE NEGOCIO:\n");
        prompt.append("1. Solo genera 'chartData' si el usuario pidió un gráfico, comparativa o tendencia.\n");
        prompt.append("2. Usa NOMBRES PROPIOS y FECHAS mencionadas en los datos.\n");
        prompt.append("3. REDONDEO: Para promedios o estadísticas, redondea SIEMPRE a máximo 1 decimal (ej: 10.5). Si el número es entero, muéstralo sin decimales.\n");
        prompt.append("4. LÍMITE DE LISTAS: Si la lista de resultados es muy larga (más de 40 nombres), NO los listes todos. Menciona los primeros 40 de forma clara y dile al usuario: 'Hay más resultados, por favor indícame si deseas ver el resto o si prefieres buscar algo más específico'.\n");
        prompt.append("5. Sé siempre amable y servicial.\n");
        return prompt.toString();
    }

    private String buildFormattingUserPrompt(String originalQuestion, List<Map<String, Object>> data) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("PREGUNTA DEL USUARIO: ").append(originalQuestion).append("\n");
        prompt.append("DATOS DE LA BASE DE DATOS: ").append(data.toString()).append("\n\n");
        prompt.append("Genera el JSON siguiendo las instrucciones del sistema.");
        return prompt.toString();
    }

    private String callGemini(String systemInstruction, String userMessage, boolean jsonMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();

        // System instruction
        Map<String, Object> systemInstructionMap = new HashMap<>();
        systemInstructionMap.put("parts", List.of(Map.of("text", systemInstruction)));
        requestBody.put("system_instruction", systemInstructionMap);

        // Contents
        Map<String, Object> contentsMap = new HashMap<>();
        contentsMap.put("role", "user");
        contentsMap.put("parts", List.of(Map.of("text", userMessage)));
        requestBody.put("contents", List.of(contentsMap));

        // Generation Config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("topP", 0.95);
        generationConfig.put("maxOutputTokens", 8192);
        if (jsonMode) {
            generationConfig.put("responseMimeType", "application/json");
        }
        requestBody.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = GEMINI_API_URL + modelId + ":generateContent?key=" + apiKey;
        log.info("Calling Gemini API: {} model: {}", GEMINI_API_URL + modelId, modelId);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List candidates = (List) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    List parts = (List) content.get("parts");
                    if (!parts.isEmpty()) {
                        Map part = (Map) parts.get(0);
                        return (String) part.get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw e;
        }
        return "";
    }

    private String extractSql(String text) {
        if (text.contains("```sql")) {
            int start = text.indexOf("```sql") + 6;
            int end = text.indexOf("```", start);
            return text.substring(start, end).trim();
        } else if (text.toUpperCase().contains("SELECT")) {
            // Intento de fallback si no usó markdown
            int start = text.toUpperCase().indexOf("SELECT");
            return text.substring(start).trim();
        }
        return null;
    }

    private AiChatResponseDto parseFinalResponse(String llmJson, String query) {
        try {
            // Limpiar posible basura del LLM fuera del JSON
            String jsonPart = llmJson;
            if (llmJson.contains("{")) {
                jsonPart = llmJson.substring(llmJson.indexOf("{"), llmJson.lastIndexOf("}") + 1);
            }

            AiChatResponseDto response = objectMapper.readValue(jsonPart, AiChatResponseDto.class);
            return response;
        } catch (Exception e) {
            log.error("Error parsing final LLM JSON, attempting regex fallback", e);
            
            // Intento de rescate: Extraer el contenido de "answer" usando regex si Jackson falló
            try {
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"answer\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"", java.util.regex.Pattern.DOTALL);
                java.util.regex.Matcher matcher = pattern.matcher(llmJson);
                if (matcher.find()) {
                    String extractedAnswer = matcher.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
                    
                    return AiChatResponseDto.builder()
                            .answer(extractedAnswer)
                            .requiresClarification(true)
                            .build();
                }
            } catch (Exception ex) {
                log.error("Regex fallback also failed", ex);
            }

            return AiChatResponseDto.builder()
                    .answer(llmJson) // Último recurso: devolver el texto bruto
                    .requiresClarification(true)
                    .build();
        }
    }
}
