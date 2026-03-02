package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.*;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;

import java.util.List;

public interface MemberService {
    MemberResponseDto create(MemberRequestDto memberRequestDto, User user);

    MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId, User user);

    PagesResponseDto<MemberResponseDto> search(int page, MemberFilterRequestDto memberFilterRequestDto, User user);

    void delete(String memberId, User user);

    ExportResponseDto<MemberExportDto> export(MemberFilterRequestDto dto, User user);

    boolean updateActive(String memberId, boolean active, User user);

    MemberNoteResponseDto createNote(MemberNoteRequestDto memberNoteRequestDto, User user);

    void deleteNote(String noteId, User user);

}
