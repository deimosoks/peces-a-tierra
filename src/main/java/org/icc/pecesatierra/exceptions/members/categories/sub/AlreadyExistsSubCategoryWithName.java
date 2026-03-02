package org.icc.pecesatierra.exceptions.members.categories.sub;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class AlreadyExistsSubCategoryWithName extends ApiException {
    public AlreadyExistsSubCategoryWithName(String name, String category) {
        super("Ya existe una sun-categoria con llamada " + name + " en la categoria " + category, HttpStatus.CONFLICT);
    }
}
