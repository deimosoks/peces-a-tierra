package org.icc.pecesatierra.dtos.notes;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberNoteResponseDto {

    private String id;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;

}
