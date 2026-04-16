package org.icc.pecesatierra.configurations;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

@Component
public class SqlQueryValidator {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s", Pattern.CASE_INSENSITIVE);
    
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList(
        "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE"
    ));

    /**
     * Valida que la query sea únicamente de lectura (SELECT) y no contenga palabras clave peligrosas.
     * @param sql La consulta SQL a validar.
     * @return true si la consulta es segura (SELECT), false de lo contrario.
     */
    public boolean isSafeSelect(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String normalizedSql = sql.trim().toUpperCase();

        // Debe empezar con SELECT
        if (!SELECT_PATTERN.matcher(normalizedSql).find()) {
            return false;
        }

        // No debe contener palabras clave prohibidas como tokens independientes
        for (String keyword : FORBIDDEN_KEYWORDS) {
            // Usamos regex para asegurar que la palabra esté aislada (no parte de un nombre de columna ej: "UpdatedBy")
            String regex = "\\b" + keyword + "\\b";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(normalizedSql).find()) {
                // Excepción: SELECT puede estar en la lista de prohibidos si no es al inicio, 
                // pero aquí ya validamos que inicia con SELECT. Solo bloqueamos los demás.
                return false;
            }
        }

        // Bloquear múltiples sentencias (;) para evitar SQL Injection por apilamiento
        if (normalizedSql.contains(";")) {
            // Solo se permite el punto y coma al final de la sentencia
            String[] parts = normalizedSql.split(";");
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
