package org.icc.pecesatierra.utils.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * H-14: prepara un resultado de la base de datos para enviarlo a un proveedor
 * externo de IA (Gemini) sin datos personales.
 *
 * <p>El asistente solo necesita cifras, conteos y distribuciones para responder
 * ("cuantos miembros hay", "promedio de edades", "asistencia por sede"). Por eso
 * el redactor:</p>
 * <ol>
 *   <li>enmascara el valor de toda columna cuyo nombre parezca identificativa;</li>
 *   <li>limita el numero de filas enviadas a {@link #MAX_ROWS};</li>
 *   <li>cuando trunca, agrega un resumen estadistico del total de filas.</li>
 * </ol>
 *
 * <p>La lista {@link #PII_COLUMN_FRAGMENTS} es deliberadamente conservadora y se
 * aplica por coincidencia de texto, no por reflexion sobre la entidad: el SQL que
 * llega aqui lo genero un modelo y puede devolver cualquier alias, con lo que
 * enmascarar de mas (perder un detalle cosmetico) es infinitamente mas barato
 * que enmascarar de menos (filtrar nombre, cedula o telefono de un miembro a un
 * tercero). Los fragmentos cortos se comparan por igualdad exacta para no
 * confundir, por ejemplo, {@code cc} con {@code acceso} o {@code proceso}.</p>
 *
 * <p>Las columnas numericas y las agregadas no se tocan: son las que sostienen
 * las respuestas cuantitativas del asistente.</p>
 */
public final class ResultRedactor {

    /** Marcador que sustituye el valor de una columna sensible. */
    public static final String MASK = "[REDACTADO]";

    /** Maximo de filas que se envian al modelo en una sola consulta. */
    public static final int MAX_ROWS = 50;

    /**
     * Fragmentos (ya normalizados: minusculas y sin acentos) que marcan una
     * columna como dato personal. Los de dos caracteres o menos se comparan por
     * igualdad exacta del nombre de columna.
     */
    public static final List<String> PII_COLUMN_FRAGMENTS = List.of(
            "name",
            "nombre",
            "document",
            "documento",
            "cedula",
            "cc",
            "phone",
            "celular",
            "telefono",
            "address",
            "direccion",
            "email",
            "correo",
            "note",
            "nota",
            "birth",
            "nacimiento"
    );

    private static final String AGGREGATE_TOTAL = "total_filas";

    private ResultRedactor() {
    }

    /**
     * Redacta y trunca un resultado de consulta.
     *
     * @param data filas tal como las devuelve el ejecutor SQL; puede ser nulo o vacio
     * @return filas enmascaradas y acotadas, mas el resumen del total
     */
    public static RedactedResult redact(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return new RedactedResult(List.of(), 0, false, Map.of(AGGREGATE_TOTAL, 0));
        }

        List<Map<String, Object>> maskedRows = new ArrayList<>(Math.min(data.size(), MAX_ROWS));
        for (Map<String, Object> row : data) {
            maskedRows.add(maskRow(row));
        }

        boolean truncated = maskedRows.size() > MAX_ROWS;
        List<Map<String, Object>> rows = truncated
                ? List.copyOf(maskedRows.subList(0, MAX_ROWS))
                : List.copyOf(maskedRows);

        return new RedactedResult(rows, data.size(), truncated, summarize(data));
    }

    private static Map<String, Object> maskRow(Map<String, Object> row) {
        if (row == null) {
            return Map.of();
        }

        Map<String, Object> masked = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String column = entry.getKey();
            Object value = entry.getValue();
            if (value != null && isSensitiveColumn(column)) {
                masked.put(column, MASK);
            } else {
                masked.put(column, value);
            }
        }
        return masked;
    }

    /**
     * Indica si el nombre de columna describe un dato personal.
     *
     * @param column nombre o alias de la columna devuelta por el SQL
     */
    public static boolean isSensitiveColumn(String column) {
        if (column == null || column.isBlank()) {
            return false;
        }

        String normalized = normalize(column);
        for (String fragment : PII_COLUMN_FRAGMENTS) {
            if (fragment.length() <= 2) {
                if (normalized.equals(fragment)) {
                    return true;
                }
            } else if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resumen de todas las columnas numericas no sensibles. Se calcula sobre el
     * resultado completo, incluso si luego se trunca, y no contiene texto libre.
     */
    private static Map<String, Object> summarize(List<Map<String, Object>> data) {
        Map<String, Map<String, Object>> stats = new LinkedHashMap<>();

        for (Map<String, Object> row : data) {
            if (row == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue();
                if (!(value instanceof Number number)) {
                    continue;
                }
                String column = entry.getKey();
                if (isSensitiveColumn(column)) {
                    continue;
                }

                double numeric = number.doubleValue();
                Map<String, Object> columnStats = stats.computeIfAbsent(column, key -> {
                    Map<String, Object> created = new LinkedHashMap<>();
                    created.put("min", numeric);
                    created.put("max", numeric);
                    created.put("suma", 0d);
                    created.put("cantidad", 0L);
                    return created;
                });
                columnStats.put("min", Math.min((Double) columnStats.get("min"), numeric));
                columnStats.put("max", Math.max((Double) columnStats.get("max"), numeric));
                columnStats.put("suma", (Double) columnStats.get("suma") + numeric);
                columnStats.put("cantidad", (Long) columnStats.get("cantidad") + 1L);
            }
        }

        Map<String, Object> aggregates = new LinkedHashMap<>();
        aggregates.put(AGGREGATE_TOTAL, data.size());
        for (Map.Entry<String, Map<String, Object>> entry : stats.entrySet()) {
            Map<String, Object> columnStats = entry.getValue();
            double sum = (Double) columnStats.get("suma");
            long count = (Long) columnStats.get("cantidad");
            columnStats.put("promedio", count == 0 ? 0d : sum / count);
            aggregates.put(entry.getKey(), Collections.unmodifiableMap(columnStats));
        }
        return Collections.unmodifiableMap(aggregates);
    }

    private static String normalize(String column) {
        String withoutAccents = Normalizer.normalize(column, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Resultado listo para el prompt: filas enmascaradas, tamano original y, si
     * hubo truncado, el resumen agregado de todas las filas.
     *
     * @param rows      filas enmascaradas y acotadas a {@link #MAX_ROWS}
     * @param totalRows filas que devolvio la consulta antes de truncar
     * @param truncated si se descartaron filas
     * @param aggregates resumen numerico del resultado completo
     */
    public record RedactedResult(List<Map<String, Object>> rows,
                                 int totalRows,
                                 boolean truncated,
                                 Map<String, Object> aggregates) {

        /** Texto que se inserta en el prompt enviado al modelo. */
        public String toPromptText() {
            StringBuilder text = new StringBuilder();
            text.append("FILAS (").append(rows.size()).append(" de ").append(totalRows);
            if (truncated) {
                text.append(", lista truncada");
            }
            text.append("): ").append(rows).append('\n');
            if (truncated) {
                text.append("RESUMEN AGREGADO DE TODAS LAS FILAS (").append(totalRows)
                        .append("): ").append(aggregates).append('\n');
            }
            return text.toString();
        }
    }
}
