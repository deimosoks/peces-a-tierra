package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.MemberNotes;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberNotesMapper {

    private final DateTimeUtils dateTimeUtils;

    public MemberNoteResponseDto toDto(MemberNotes memberNotes) {
        return MemberNoteResponseDto.builder()
                .id(memberNotes.getId())
                .note(memberNotes.getNote())
                .createdBy(memberNotes.getCreatedBy().getCompleteName())
                .createdAt(dateTimeUtils.toColombia(memberNotes.getCreatedAt()))
                .build();
    }

}
