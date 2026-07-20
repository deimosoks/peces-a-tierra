package org.icc.pecesatierra.features.member.dtos.notes;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberNoteRequestDto {

    @NotBlank
    private String note;
    @NotBlank
    private String memberId;

}
