package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.*;
import org.icc.pecesatierra.dtos.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.User;

import java.util.List;

public interface MemberService {
    MemberResponseDto create(MemberRequestDto memberRequestDto);

    MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId);

    MemberPagesResponseDto findAll(int page, MemberFilterRequestDto memberFilterRequestDto);

    void delete(String memberId);

    List<MemberExportDto> findAllData(MemberFilterRequestDto dto);

    boolean updateActive(String memberId, boolean active);

    MemberNoteResponseDto createNote(MemberNoteRequestDto memberNoteRequestDto, User user);

    void deleteNote(String noteId);

}
