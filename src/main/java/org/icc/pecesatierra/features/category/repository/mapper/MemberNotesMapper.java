package org.icc.pecesatierra.features.category.repository.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.features.member.MemberNotes;
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
