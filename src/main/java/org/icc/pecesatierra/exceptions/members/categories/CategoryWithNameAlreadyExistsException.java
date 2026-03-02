package org.icc.pecesatierra.exceptions.members.categories;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CategoryWithNameAlreadyExistsException extends ApiException {
    public CategoryWithNameAlreadyExistsException(String name) {
        super("Ya existe una categoria con el nombre " + name, HttpStatus.CONFLICT);
    }
}
