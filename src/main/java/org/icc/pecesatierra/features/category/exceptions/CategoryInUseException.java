package org.icc.pecesatierra.features.category.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CategoryInUseException extends ApiException {
    public CategoryInUseException() {
        super("No puede eliminar una categoria que esta en uso.", HttpStatus.CONFLICT);
    }
}
