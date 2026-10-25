package org.icc.pecesatierra.features.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.features.member.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.MemberNotes;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.member.exceptions.CannotDeleteMemberOutSideYourBranchException;
import org.icc.pecesatierra.features.member.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.features.member.exceptions.notes.CannotManageNotesOutsideYourBranchException;
import org.icc.pecesatierra.features.member.exceptions.notes.NoteNotFoundException;
import org.icc.pecesatierra.features.member.repository.MemberNotesRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberNotesMapper;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberNoteService {

    private final DateTimeUtils dateTimeUtils;
    private final MemberNotesMapper memberNotesMapper;
    private final MemberNotesRepository memberNotesRepositor;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberNoteResponseDto createNote(MemberNoteRequestDto memberNoteRequestDto, User user) {

        Member member = memberRepository.findById(memberNoteRequestDto.getMemberId())
                .orElseThrow(NoteNotFoundException::new);

        if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name()) && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
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
    public void deleteNote(String noteId, User user) {
        MemberNotes memberNotes = memberNotesRepositor.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);

        if (!user.hasAuthority(AppPermission.ADMINISTRATOR.name()) && !user.getMember().getBranch().getId().equals(memberNotes.getMember().getBranch().getId())) {
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

    /**
     * Notas de un integrante, de la mas reciente a la mas antigua. La sede se
     * comprueba antes de leer las notas: un no-admin solo ve notas de
     * integrantes de su propia sede.
     */
    @Transactional(readOnly = true)
    public List<MemberNoteResponseDto> findByMember(String memberId, User user) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        assertCanManageNotes(user, member);

        return memberNotesRepositor.findByMember_IdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(memberNotesMapper::toDto)
                .toList();
    }

    /**
     * Actualiza el contenido de una nota existente. El {@code memberId} del DTO
     * no mueve la nota de integrante: solo se cambia el texto.
     */
    @Transactional
    public MemberNoteResponseDto updateNote(String noteId, MemberNoteRequestDto memberNoteRequestDto, User user) {

        MemberNotes memberNotes = memberNotesRepositor.findById(noteId)
                .orElseThrow(NoteNotFoundException::new);

        assertCanManageNotes(user, memberNotes.getMember());

        String previousNote = memberNotes.getNote();
        memberNotes.setNote(memberNoteRequestDto.getNote());

        memberNotesRepositor.save(memberNotes);

        log.info("""
                        Se actualizó una nota de integrante:
                        Nota ID: {}
                        Integrante: {} ({})
                        Contenido anterior: {}
                        Contenido nuevo: {}
                        Actualizada por: {} ({})
                        Fecha actualización: {}
                        """,
                memberNotes.getId(),
                memberNotes.getMember().getCompleteName(),
                memberNotes.getMember().getId(),
                previousNote,
                memberNotes.getNote(),
                user.getUsername(),
                user.getId(),
                dateTimeUtils.nowUTC()
        );

        return memberNotesMapper.toDto(memberNotes);
    }

    /**
     * Mismo criterio de sede que {@link #deleteNote(String, User)}: el
     * administrador conserva el bypass y cualquier otro usuario solo opera sobre
     * integrantes de su sede.
     */
    private void assertCanManageNotes(User user, Member member) {
        if (user.hasAuthority(AppPermission.ADMINISTRATOR.name())) {
            return;
        }

        if (member.getBranch() == null
                || !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
            throw new CannotManageNotesOutsideYourBranchException();
        }
    }
}
