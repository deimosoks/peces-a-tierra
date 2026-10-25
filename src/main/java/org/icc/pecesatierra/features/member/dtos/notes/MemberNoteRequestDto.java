package org.icc.pecesatierra.features.member.dtos.notes;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberNoteRequestDto {

    /**
     * Grupo de validacion del alta. El frontend edita una nota enviando solo
     * {@code note} a {@code PUT /api/members/notes/{id}}, asi que
     * {@code memberId} solo es obligatorio al crear.
     */
    public interface CreateValidation {
    }

    @NotBlank
    private String note;

    @NotBlank(groups = CreateValidation.class)
    private String memberId;

}
