package org.icc.pecesatierra.web.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.configurations.SqlQueryValidator;
import org.icc.pecesatierra.dtos.ai.AiChatRequestDto;
import org.icc.pecesatierra.dtos.ai.AiChatResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final JdbcTemplate jdbcTemplate;
    private final SqlQueryValidator sqlQueryValidator;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.model.id}")
    private String modelId;

    private static final String HF_ROUTER_URL = "https://router.huggingface.co/v1/chat/completions";

    @Override
    @Transactional(readOnly = true)
    public AiChatResponseDto processChat(AiChatRequestDto request, User user) {
        try {
            // STEP 1: Generate SQL from Natural Language
            String sqlSystemPrompt = buildSqlSystemPrompt();
            String sqlUserPrompt = buildSqlUserPrompt(request.getMessage(), request.getHistory());
            String llmResponse = callHuggingFace(sqlSystemPrompt, sqlUserPrompt);

            log.info("LLM generated response for SQL: {}", llmResponse);

            // Extract SQL
            String sql = extractSql(llmResponse);

            if (sql != null && sqlQueryValidator.isSafeSelect(sql)) {
                // STEP 2: Execute SQL
                List<Map<String, Object>> resultData = jdbcTemplate.queryForList(sql);

                // STEP 3: Format result back to natural language and chart data
                String formattingSystemPrompt = "Eres un asistente inteligente para una iglesia. Debes transformar los datos de base de datos en una respuesta amable y natural para el usuario.";
                String formattingUserPrompt = buildFormattingUserPrompt(request.getMessage(), resultData);
                String finalLlmResponse = callHuggingFace(formattingSystemPrompt, formattingUserPrompt);

                log.info("LLM final response: {}", finalLlmResponse);

                return parseFinalResponse(finalLlmResponse, sql);
            } else {
                // If no SQL or unsafe, just treat as normal chat
                return AiChatResponseDto.builder()
                        .answer(llmResponse)
                        .query(sql)
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
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un experto senior en PostgreSQL. Tu misión es generar consultas SQL SELECT precisas para una base de datos de gestión de iglesia.\n\n");
        
        prompt.append("ESQUEMA TÉCNICO COMPLETO (TODOS LOS IDs SON 'TEXT/UUID'):\n");
        prompt.append("- members (m): id, complete_name, created_at, active, cc, cellphone, birthdate, gender, address, neighborhood, city, municipality, district, type_id (FK mt.id), category_id (FK mc.id), subcategory_id (FK msc.id), branch_id (FK br.id), registered_by\n");
        prompt.append("- attendance (a): id, member_id (FK m.id), service_event_id (FK se.id), attendance_date, member_category (FK mc.id), member_type (FK mt.id), sub_category_id (FK msc.id), branch_id, invalid\n");
        prompt.append("- baptism (b): id, member_baptized (FK m.id), date, created_at, invalid\n");
        prompt.append("- services (s): id, name, description, active\n");
        prompt.append("- service_event (se): id, service_id (FK s.id), branch_id (FK br.id), start_date_time, end_date_time\n");
        prompt.append("- branches (br): id, name, address, city\n");
        prompt.append("- member_types (mt): id, name\n");
        prompt.append("- member_categories (mc): id, name\n");
        prompt.append("- member_sub_categories (msc): id, name, category_id (FK mc.id)\n\n");

        prompt.append("REGLA DE ORO (LLAVES FORÁNEAS):\n");
        prompt.append("- Para filtrar por CATEGORÍA en asistencia, usa: `attendance.member_category = mc.id`. NUNCA uses `sub_category_id` para esto.\n");
        prompt.append("- Para filtrar por TIPO en asistencia, usa: `attendance.member_type = mt.id`.\n");
        prompt.append("- Si hay duda sobre los atributos del miembro, une siempre con la tabla `members`: `JOIN members m ON a.member_id = m.id`.\n\n");

        prompt.append("RESOLUCIÓN DE CONTEXTO:\n");
        prompt.append("1. Pregunta: 'Miembros con teléfono registrados este mes'\n");
        prompt.append("   SQL: ```sql\n");
        prompt.append("   SELECT m.complete_name, m.cellphone FROM members m \n");
        prompt.append("   WHERE EXTRACT(MONTH FROM m.created_at) = EXTRACT(MONTH FROM CURRENT_DATE) \n");
        prompt.append("   AND EXTRACT(YEAR FROM m.created_at) = EXTRACT(YEAR FROM CURRENT_DATE);\n");
        prompt.append("   ```\n\n");

        prompt.append("2. Pregunta: 'Miembros que NO asistieron a los últimos 2 servicios de Escuela Dominical'\n");
        prompt.append("   SQL: ```sql\n");
        prompt.append("   SELECT m.complete_name, m.cellphone FROM members m \n");
        prompt.append("   JOIN member_types mt ON m.type_id = mt.id \n");
        prompt.append("   WHERE mt.name ILIKE '%miembro%' \n");
        prompt.append("   AND NOT EXISTS ( \n");
        prompt.append("     SELECT 1 FROM attendance a \n");
        prompt.append("     WHERE a.member_id = m.id \n");
        prompt.append("     AND a.service_event_id IN ( \n");
        prompt.append("       SELECT se2.id FROM service_event se2 \n");
        prompt.append("       JOIN services s2 ON se2.service_id = s2.id \n");
        prompt.append("       WHERE s2.name ILIKE '%Escuela Dominical%' \n");
        prompt.append("       ORDER BY se2.start_date_time DESC LIMIT 2 \n");
        prompt.append("     ) \n");
        prompt.append("   );\n");
        prompt.append("   ```\n\n");

        prompt.append("RESOLUCIÓN DE CONTEXTO:\n");
        prompt.append("- Analiza siempre el HISTORIAL DE CONVERSACIÓN.\n");
        prompt.append("- Si el usuario usa pronombres (ese, ella, su, aquel) o frases como 'ese evento', identifica la entidad mencionada en la última respuesta de la IA y usa su nombre o ID en la nueva query.\n");
        prompt.append("- Ejemplo: Si la IA dijo 'Escuela Dominical tiene 24 niños' y el usuario pregunta '¿En qué fecha fue?', la nueva query debe filtrar por `name ILIKE '%Escuela Dominical%'`.\n\n");

        prompt.append("MAPEO SEMÁNTICO (INTENCIONES):\n");
        prompt.append("- 'Niños', 'Jóvenes', 'Adultos' -> Mapear siempre a la tabla `member_categories`.\n");
        prompt.append("- 'Miembros', 'Visitantes', 'Simpatizantes', 'Invitados' -> Mapear siempre a la tabla `member_types`.\n");
        prompt.append("- Si el usuario dice 'personas', no apliques filtros de tipo/categoría a menos que se especifique.\n\n");

        prompt.append("REGLAS CRÍTICAS (LEYES INFLEXIBLES):\n");
        prompt.append("- EXACTITUD DE SELECCIÓN: Si el usuario pide datos específicos (teléfono, documento, barrio, fecha registro), DEBES incluirlos en el `SELECT`.\n");
        prompt.append("- LÓGICA DE INASISTENCIA: Usa SIEMPRE `NOT EXISTS` para 'quién no hizo algo'.\n");
        prompt.append("- BÚSQUEDA FLEXIBLE: Usa siempre `ILIKE '%termino%'`.\n");
        prompt.append("- ALIAS ÚNICOS: En subconsultas anidadas, NUNCA repitas alias (`s`, `s2`, `se`, `se2`).\n");
        prompt.append("- SEGURIDAD: Solo genera sentencias SELECT.\n\n");
        
        prompt.append("RETORNO:\n");
        prompt.append("1. Devuelve estrictamente la query SQL en bloque ```sql ... ```.\n");
        prompt.append("2. Si faltan datos, devuelve Por favor deme mas información para completar su solicitud.\n");
        
        return prompt.toString();
    }

    private String buildSqlUserPrompt(String message, List<AiChatRequestDto.ChatMessage> history) {
        StringBuilder prompt = new StringBuilder();
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

    private String buildFormattingUserPrompt(String originalQuestion, List<Map<String, Object>> data) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("PREGUNTA ORIGINAL: ").append(originalQuestion).append("\n");
        prompt.append("DATOS RECUPERADOS: ").append(data.toString()).append("\n\n");

        prompt.append("INSTRUCCIONES DE FORMATO:\n");
        prompt.append("1. Responde de forma natural y AMABLE. Menciona nombres propios y fechas específicas encontradas en los datos para que el usuario pueda hacer preguntas de seguimiento.\n");
        prompt.append("2. Objeto JSON obligatorio:\n");
        prompt.append("{\n");
        prompt.append("  \"answer\": \"Tu respuesta amable...\",\n");
        prompt.append("  \"chartData\": { ... } o null\n");
        prompt.append("}\n");
        prompt.append("3. GENERACIÓN DE GRÁFICOS: Solo si el usuario lo pide explícitamente.\n");
        prompt.append("4. Si los datos están vacíos, responde amablemente que no encontraste información.\n");

        return prompt.toString();
    }

    private String callHuggingFace(String systemPrompt, String userMessage) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelId);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userMessage));

        body.put("messages", messages);
        body.put("parameters", Map.of("max_new_tokens", 800, "temperature", 0.1));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("Calling HF Router API: {} for model: {}", HF_ROUTER_URL, modelId);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(HF_ROUTER_URL, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List choices = (List) response.getBody().get("choices");
                if (!choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("Error in HF API call", e);
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
            response.setQuery(query);
            return response;
        } catch (Exception e) {
            log.error("Error parsing final LLM JSON", e);
            return AiChatResponseDto.builder()
                    .answer(llmJson) // Devuelve el texto bruto si falla el JSON
                    .query(query)
                    .requiresClarification(true)
                    .build();
        }
    }
}
