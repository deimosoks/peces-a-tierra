package org.icc.pecesatierra.dtos.member.notes;

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
