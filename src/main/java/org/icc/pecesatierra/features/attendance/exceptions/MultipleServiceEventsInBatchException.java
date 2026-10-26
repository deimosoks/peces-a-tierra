package org.icc.pecesatierra.features.attendance.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

/**
 * H-41: un lote de asistencias se registra siempre contra un unico evento de
 * servicio. Antes se tomaba el evento del primer item y se descartaba en
 * silencio el de los demas; ahora el lote mixto se rechaza.
 */
public class MultipleServiceEventsInBatchException extends ApiException {
    public MultipleServiceEventsInBatchException() {
        super("No se puede registrar un lote de asistencias con eventos de servicio distintos. Envie un solo evento por lote.",
                HttpStatus.BAD_REQUEST);
    }
}
