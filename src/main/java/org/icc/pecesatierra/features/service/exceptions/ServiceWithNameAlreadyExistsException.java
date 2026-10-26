package org.icc.pecesatierra.features.service.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

/**
 * H-42b: el nombre del servicio es la clave natural que ve el usuario. Sin esta
 * validacion el alta y la edicion dependian de un indice que puede no existir y,
 * si existia, el choque acababa en el 409 generico del handler de integridad.
 */
public class ServiceWithNameAlreadyExistsException extends ApiException {

    public ServiceWithNameAlreadyExistsException(String name) {
        super("Ya existe un servicio con el nombre " + name + ".", HttpStatus.CONFLICT);
    }
}
