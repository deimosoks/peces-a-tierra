package org.icc.pecesatierra.utils.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

/**
 * H-32: la exportacion pedida supera {@code export.max-rows}. Se responde 400 y
 * no 409 porque el usuario puede resolverlo acotando fechas o filtros; no hay
 * conflicto con el estado de la base.
 */
public class ExportLimitExceededException extends ApiException {

    public ExportLimitExceededException(long rows, int maxRows) {
        super("La exportación devuelve " + rows + " registros y el máximo permitido es " + maxRows
                        + ". Acote el rango de fechas o los filtros e inténtelo de nuevo.",
                HttpStatus.BAD_REQUEST);
    }
}
