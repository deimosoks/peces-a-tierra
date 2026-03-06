package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberNotes;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.members.CannotDeleteMemberOutSideYourBranchException;
import org.icc.pecesatierra.exceptions.members.notes.NoteNotFoundException;
import org.icc.pecesatierra.repositories.MemberNotesRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.MemberNotesMapper;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.icc.pecesatierra.web.services.MemberNoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberNoteServiceImpl implements MemberNoteService {

    private final DateTimeUtils dateTimeUtils;
    private final MemberNotesMapper memberNotesMapper;
    private final MemberNotesRepository memberNotesRepositor;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public MemberNoteResponseDto createNote(MemberNoteRequestDto memberNoteRequestDto, User user) {

        Member member = memberRepository.findById(memberNoteRequestDto.getMemberId())
                .orElseThrow(NoteNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException();
        }

        MemberNotes memberNotes = MemberNotes.builder()
                .note(memberNoteRequestDto.getNote())
                .createdAt(dateTimeUtils.nowUTC())
                .createdBy(user.getMember())
                .member(member)
                .build();

        memberNotesRepositor.save(memberNotes);

        log.info("""
                        Se creó una nota de integrante:
                        Nota ID: {}
                        Integrante: {} ({})
                        Contenido: {}
                        Creada por: {} ({})
                        Fecha creación: {}
                        """,
                memberNotes.getId(),
                member.getCompleteName(),
                member.getId(),
                memberNotes.getNote(),
                user.getUsername(),
                user.getId(),
                memberNotes.getCreatedAt()
        );

        return memberNotesMapper.toDto(memberNotes);
    }

    @Transactional
    @Override
    public void deleteNote(String noteId, User user) {
        MemberNotes memberNotes = memberNotesRepositor.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(memberNotes.getMember().getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException();
        }

        log.info("""
                        Se eliminó una nota de integrante:
                        Nota ID: {}
                        Integrante: {} ({})
                        Contenido: {}
                        Eliminada por: {} ({})
                        Fecha eliminación: {}
                        """,
                memberNotes.getId(),
                memberNotes.getMember().getCompleteName(),
                memberNotes.getMember().getId(),
                memberNotes.getNote(),
                user.getUsername(),
                user.getId(),
                dateTimeUtils.nowUTC()
        );

        memberNotesRepositor.delete(memberNotes);
    }
}
