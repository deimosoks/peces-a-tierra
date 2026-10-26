package org.icc.pecesatierra.configurations;

import lombok.Getter;
import org.icc.pecesatierra.utils.exceptions.ExportLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Techo comun de las exportaciones (H-32).
 *
 * <p>Las exportaciones de integrantes y de asistencias materializan todas las
 * filas que devuelve el filtro en memoria, en el JSON de respuesta y en el
 * PDF/Excel del cliente. Una sede con anos de historia puede superar con holgura
 * las decenas de miles de filas, asi que se fija un maximo configurable con
 * {@code export.max-rows} (10000 por defecto, variable de entorno
 * {@code EXPORT_MAX_ROWS}).</p>
 *
 * <p>Al superarlo no se trunca en silencio: se rechaza la exportacion para que
 * el usuario acote el rango de fechas o los filtros.</p>
 */
@Component
@Getter
public class ExportLimits {

    private final int maxRows;

    public ExportLimits(@Value("${export.max-rows:10000}") int maxRows) {
        // Un valor <= 0 dejaria todas las exportaciones inutilizables; se
        // normaliza a 1 en lugar de fallar al arrancar.
        this.maxRows = Math.max(1, maxRows);
    }

    /**
     * Comprueba el tamano antes de traer las filas. Los servicios cuentan
     * primero con la misma especificacion y solo consultan las entidades si el
     * resultado cabe en el limite.
     */
    public void assertAllowed(long rows) {
        if (rows > maxRows) {
            throw new ExportLimitExceededException(rows, maxRows);
        }
    }
}
