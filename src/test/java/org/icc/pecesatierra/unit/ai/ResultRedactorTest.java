package org.icc.pecesatierra.unit.ai;

import org.icc.pecesatierra.utils.ai.ResultRedactor;
import org.icc.pecesatierra.utils.ai.ResultRedactor.RedactedResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H-14: el resultado de una consulta no puede llegar a Gemini con datos
 * personales, con mas filas de las necesarias ni sin un resumen que permita
 * responder preguntas cuantitativas cuando la lista se trunca.
 */
class ResultRedactorTest {

    @Test
    void masksNameColumnBeforeSendingItToTheModel_H14() {
        List<Map<String, Object>> rows = List.of(row("complete_name", "Ana Confidencial", "total", 3));

        RedactedResult redacted = ResultRedactor.redact(rows);

        assertThat(redacted.rows()).hasSize(1);
        assertThat(redacted.rows().get(0))
                .containsEntry("complete_name", ResultRedactor.MASK)
                .containsEntry("total", 3);
        assertThat(redacted.toPromptText())
                .contains(ResultRedactor.MASK)
                .doesNotContain("Ana Confidencial");
    }

    @Test
    void masksIdentityDocumentColumns_H14() {
        List<Map<String, Object>> rows = List.of(
                row("cc", "1000000001", "documento", "1000000001", "cedula", "1000000001"));

        RedactedResult redacted = ResultRedactor.redact(rows);

        assertThat(redacted.rows()).hasSize(1);
        Map<String, Object> row = redacted.rows().get(0);
        assertThat(row.get("cc")).isEqualTo(ResultRedactor.MASK);
        assertThat(row.get("documento")).isEqualTo(ResultRedactor.MASK);
        assertThat(row.get("cedula")).isEqualTo(ResultRedactor.MASK);
        assertThat(redacted.toPromptText()).doesNotContain("1000000001");
    }

    @Test
    void keepsAggregatedColumnsIntact_H14() {
        List<Map<String, Object>> rows = List.of(
                row("promedio_edad", 34.5, "total_miembros", 120L),
                row("promedio_edad", 35.5, "total_miembros", 80L));

        RedactedResult redacted = ResultRedactor.redact(rows);

        assertThat(redacted.rows()).hasSize(2);
        assertThat(redacted.rows().get(0))
                .containsEntry("promedio_edad", 34.5)
                .containsEntry("total_miembros", 120L);
        assertThat(redacted.aggregates())
                .containsEntry("total_filas", 2);
        assertThat(redacted.toPromptText())
                .contains("promedio_edad=34.5")
                .contains("total_miembros=120");
    }

    @Test
    void truncatesRowsAndSummarisesTheWholeResult_H14() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            rows.add(row("sede", "Sede " + index, "asistentes", index + 1));
        }

        RedactedResult redacted = ResultRedactor.redact(rows);

        assertThat(redacted.rows()).hasSize(ResultRedactor.MAX_ROWS);
        assertThat(redacted.totalRows()).isEqualTo(60);
        assertThat(redacted.truncated()).isTrue();

        @SuppressWarnings("unchecked")
        Map<String, Object> asistentes =
                (Map<String, Object>) redacted.aggregates().get("asistentes");
        assertThat(asistentes)
                .containsEntry("min", 1.0)
                .containsEntry("max", 60.0)
                .containsEntry("promedio", 30.5);

        assertThat(redacted.toPromptText())
                .contains("lista truncada")
                .contains("RESUMEN AGREGADO DE TODAS LAS FILAS (60)")
                .contains("total_filas=60");
    }

    @Test
    void emptyResultIsSentAsAnEmptySummary_H14() {
        RedactedResult redacted = ResultRedactor.redact(List.of());

        assertThat(redacted.rows()).isEmpty();
        assertThat(redacted.totalRows()).isZero();
        assertThat(redacted.truncated()).isFalse();
        assertThat(redacted.toPromptText()).contains("FILAS (0 de 0)");
    }

    @Test
    void shortFragmentsOnlyMatchWholeColumnNames_H14() {
        assertThat(ResultRedactor.isSensitiveColumn("cc")).isTrue();
        assertThat(ResultRedactor.isSensitiveColumn("acceso")).isFalse();
        assertThat(ResultRedactor.isSensitiveColumn("proceso")).isFalse();
        assertThat(ResultRedactor.isSensitiveColumn("Teléfono")).isTrue();
        assertThat(ResultRedactor.isSensitiveColumn("direccion")).isTrue();
        assertThat(ResultRedactor.isSensitiveColumn("promedio_edad")).isFalse();
    }

    private Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            row.put((String) keyValues[index], keyValues[index + 1]);
        }
        return row;
    }
}
