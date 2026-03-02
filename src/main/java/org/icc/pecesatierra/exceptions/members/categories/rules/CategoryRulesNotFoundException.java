package org.icc.pecesatierra.exceptions.members.categories.rules;

import org.icc.pecesatierra.utils.models.ApiException;
import org.springframework.http.HttpStatus;

public class CategoryRulesNotFoundException extends ApiException {
    public CategoryRulesNotFoundException() {
        super("Regla de categoria no encontrada.", HttpStatus.NOT_FOUND);
    }
}
