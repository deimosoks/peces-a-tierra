package org.icc.pecesatierra.utils.mappers;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.MemberNotes;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MemberNotesMapper {

    public MemberNoteResponseDto toDto(MemberNotes memberNotes){
        return MemberNoteResponseDto.builder()
                .id(memberNotes.getId())
                .note(memberNotes.getNote())
                .createdBy(memberNotes.getCreatedBy().getCompleteName())
                .createdAt(memberNotes.getCreatedAt())
                .build();
    }

}
