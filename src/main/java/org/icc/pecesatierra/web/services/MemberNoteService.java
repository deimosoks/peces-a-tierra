package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.User;

public interface MemberNoteService {
    MemberNoteResponseDto createNote(MemberNoteRequestDto memberNoteRequestDto, User user);

    void deleteNote(String noteId, User user);
}
