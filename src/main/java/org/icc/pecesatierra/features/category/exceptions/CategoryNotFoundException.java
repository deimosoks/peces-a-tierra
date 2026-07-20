package org.icc.pecesatierra.features.category.exceptions;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends ApiException {
    public CategoryNotFoundException() {
        super("Categoria no encontrada.", HttpStatus.NOT_FOUND);
    }
}
