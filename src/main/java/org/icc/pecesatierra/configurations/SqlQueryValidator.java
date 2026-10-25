package org.icc.pecesatierra.configurations;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Validacion lexica de las consultas SQL que genera el asistente de IA antes de
 * ejecutarlas con el rol de solo lectura.
 *
 * <p>Es una lista negra, no un parser: no conoce el esquema real. Por eso se
 * mantiene deliberadamente conservadora y rechaza todo lo que no sea una unica
 * sentencia que empiece por {@code SELECT} o {@code WITH} y no contenga verbos
 * de escritura, catalogos del sistema, funciones peligrosas, columnas sensibles
 * ni {@code UNION}.</p>
 *
 * <p>Decisiones conservadoras, documentadas para quien amplie la lista:</p>
 * <ul>
 *   <li><b>Punto y coma.</b> Solo se admite uno y como ultimo caracter de la
 *       consulta. Un {@code ;} intermedio (incluso dentro de un literal) hace
 *       que la consulta se rechace: es la unica forma de garantizar que no haya
 *       una segunda sentencia, que era el bypass de H-01
 *       ({@code "SELECT 1;;SET statement_timeout=0"}).</li>
 *   <li><b>UNION.</b> Se bloquea siempre. Sin un catalogo de tablas permitidas no
 *       se puede demostrar que ambos lados del {@code UNION} solo lean tablas
 *       del negocio; permitirlo abriria la lectura de {@code users} o
 *       {@code refresh_token}. Las consultas legitimas del asistente usan una
 *       unica {@code SELECT} con {@code JOIN}, {@code WHERE}, {@code GROUP BY},
 *       {@code ORDER BY} y {@code LIMIT}.</li>
 *   <li><b>INTO.</b> Se bloquea porque {@code SELECT ... INTO} escribe una tabla
 *       nueva; no forma parte del contrato de lectura del asistente.</li>
 *   <li><b>Funciones {@code pg_}.</b> Cualquier llamada {@code pg_*(...)} se
 *       rechaza; cubre {@code pg_sleep} y {@code pg_advisory_lock} sin depender
 *       de enumerarlas todas.</li>
 * </ul>
 */
@Component
public class SqlQueryValidator {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*(SELECT|WITH)\\s", Pattern.CASE_INSENSITIVE);

    /**
     * Verbos y palabras clave que no pueden aparecer como token independiente en
     * una consulta de solo lectura.
     */
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList(
            "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "ALTER", "CREATE", "GRANT", "REVOKE",
            "EXEC", "EXECUTE", "DO", "CALL", "COPY", "SET", "COMMENT", "VACUUM", "ANALYZE",
            "REINDEX", "DISCARD", "LOCK", "PREPARE", "UNION", "INTO"
    ));

    /**
     * Catalogos del sistema, funciones peligrosas y columnas sensibles. Se buscan
     * como subcadenas delimitadas (o llamadas a funcion) sobre la consulta en
     * mayusculas.
     */
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            Pattern.compile("\\binformation_schema\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpg_catalog\\b", Pattern.CASE_INSENSITIVE),
            // Cubre pg_sleep(...), pg_advisory_lock(...) y cualquier otra pg_*(...)
            Pattern.compile("\\bpg_\\w+\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bset_config\\s*\\(", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcurrent_user\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsession_user\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpassword_hash\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\brefresh_token\\b", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Valida que la query sea unicamente de lectura (SELECT) y no contenga
     * palabras clave, catalogos, funciones ni columnas peligrosas.
     * @param sql La consulta SQL a validar.
     * @return true si la consulta es segura (un unico SELECT), false de lo contrario.
     */
    public boolean isSafeSelect(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        String normalizedSql = sql.trim().toUpperCase();

        // Debe empezar con SELECT (o un WITH que introduce una SELECT)
        if (!SELECT_PATTERN.matcher(normalizedSql).find()) {
            return false;
        }

        // H-01: una sola sentencia. El unico ';' admitido es el final.
        if (!isSingleStatement(normalizedSql)) {
            return false;
        }

        // No debe contener palabras clave prohibidas como tokens independientes
        for (String keyword : FORBIDDEN_KEYWORDS) {
            // Usamos regex para asegurar que la palabra esté aislada (no parte de un nombre de columna ej: "UpdatedBy")
            String regex = "\\b" + keyword + "\\b";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(normalizedSql).find()) {
                return false;
            }
        }

        for (Pattern forbiddenPattern : FORBIDDEN_PATTERNS) {
            if (forbiddenPattern.matcher(normalizedSql).find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Rechaza cualquier punto y coma que no sea el ultimo caracter de la
     * consulta. Antes se partia por {@code ;} y solo se miraba el segundo
     * segmento, de modo que {@code "SELECT 1;;SET ..."} o
     * {@code "SELECT 1; SELECT 2"} pasaban la validacion.
     */
    private boolean isSingleStatement(String normalizedSql) {
        int firstSemicolon = normalizedSql.indexOf(';');
        return firstSemicolon < 0 || firstSemicolon == normalizedSql.length() - 1;
    }
}
