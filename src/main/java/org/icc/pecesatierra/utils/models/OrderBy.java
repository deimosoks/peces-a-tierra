package org.icc.pecesatierra.utils.models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OrderBy {
    @NotBlank
    private String orderBy;
    @NotBlank
    private boolean asc;
}
