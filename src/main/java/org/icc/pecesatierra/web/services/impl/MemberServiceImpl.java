package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.member.*;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.member.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberNotes;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.members.*;
import org.icc.pecesatierra.exceptions.members.notes.NoteNotFoundException;
import org.icc.pecesatierra.repositories.MemberNotesRepository;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.MemberNotesMapper;
import org.icc.pecesatierra.utils.models.ApiException;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.MemberSpecification;
import org.icc.pecesatierra.web.services.MemberService;
import org.icc.pecesatierra.utils.cloudinary.PictureUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final AttendanceRepository attendanceRepository;
    private final MemberPersistenceService memberPersistenceService;
    private final PictureUtils pictureUtils;
    private final MemberNotesMapper memberNotesMapper;
    private final MemberNotesRepository memberNotesRepositor;
    private final MemberSpecification memberSpecification;

    @Override
    public MemberResponseDto create(MemberRequestDto memberRequestDto, User user) {

        if (memberRequestDto.getCc() != null && memberRepository.existsByCc(memberRequestDto.getCc())) {
            log.warn("Usuario {} intento crear integrante con CC {} pero ya existe.", user.getMember().getId(), memberRequestDto.getCc());
            throw new AlreadyExistsMemberWithCc(memberRequestDto.getCc());
        }

        Map<String, String> pictureData = null;

        if (memberRequestDto.getPictureProfile() != null && !memberRequestDto.getPictureProfile().isEmpty()) {
            pictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile(), "members/photos");
        }

        try {
            MemberResponseDto response = memberPersistenceService.save(memberRequestDto, pictureData, user);

            log.info("""
                            Usuario {} creó integrante:
                            ID: {}
                            Nombre: {}
                            CC: {}
                            Celular: {}
                            Branch: {}
                            """,
                    user.getMember().getId(),
                    response.getId(),
                    response.getCompleteName(),
                    response.getCc(),
                    response.getCellphone(),
                    response.getBranch().getId()
            );

            return response;
        } catch (ApiException e) {
            if (pictureData.get("publicId") != null) {
                pictureUtils.delete(pictureData.get("publicId"));
            }
            throw e;
        }
    }

    @Override
    public MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId, User user) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (memberRequestDto.getCc() != null && !memberRequestDto.getCc().equals(member.getCc()) && memberRepository.existsByCc(memberRequestDto.getCc())) {
            log.warn("Usuario {} intento actualizar integrante {} con CC {} pero ya existe.", user.getMember().getId(), memberId, memberRequestDto.getCc());
            throw new AlreadyExistsMemberWithCc(memberRequestDto.getCc());
        }

        String oldPublicId = member.getPublicId();
        Map<String, String> newPictureData = null;

        if (memberRequestDto.getPictureProfile() != null) {
            newPictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile(), "members/photos");
        }

        try {
            MemberResponseDto response = memberPersistenceService.update(member.getId(), memberRequestDto, newPictureData, user);

            if (newPictureData != null && oldPublicId != null) {
                pictureUtils.delete(oldPublicId);
            }

            return response;

        } catch (ApiException e) {
            if (newPictureData != null && newPictureData.get("publicId") != null) {
                pictureUtils.delete(newPictureData.get("publicId"));
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public PagesResponseDto<MemberResponseDto> search(int page, MemberFilterRequestDto dto, User user) {

        Specification<Member> spec = memberSpecification.build(dto, user);

        Page<Member> pageResult = memberRepository.findAll(
                spec,
                PageRequest.of(page, 20, Sort.by("completeName"))
        );

        return PagesResponseDto.<MemberResponseDto>builder()
                .data(pageResult.stream().map(member -> memberMapper.toDto(member, true)).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();

    }

    @Transactional(readOnly = true)
    @Override
    public ExportResponseDto<MemberExportDto> export(MemberFilterRequestDto dto, User user) {
        Specification<Member> spec = memberSpecification.build(dto, user);

        List<Member> members = memberRepository.findAll(spec);

        log.info("Usuario {} exporto una lista de integrantes.", user.getMember().getId());

        return ExportResponseDto.<MemberExportDto>builder()
                .data(members.stream().map(memberMapper::toExportDto).toList())
                .totalElements(members.size())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String memberId, User user) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (attendanceRepository.existsByMember(member))
            throw new CannotDeleteMemberWithRecords();

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException();
        }

        String pictureUrl = member.getPictureProfileUrl();

        memberRepository.delete(member);

        log.info("""
                        Usuario {} eliminó el integrante:
                        ID: {}
                        Nombre completo: {}
                        Sede: {} ({})
                        Eliminado por: {} ({})
                        Tenía foto: {}
                        """,
                user.getMember().getId(),
                member.getId(),
                member.getCompleteName(),
                member.getBranch().getName(),
                member.getBranch().getId(),
                user.getUsername(),
                user.getId(),
                pictureUrl != null
        );


        if (pictureUrl != null) pictureUtils.delete(pictureUrl);
    }

    @Transactional
    @Override
    public boolean updateActive(String memberId, boolean active, User user) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
            throw new CannotDeleteMemberOutSideYourBranchException();
        }

        boolean previousState = member.isActive();
        member.setActive(active);

        memberRepository.save(member);

        log.info("""
                        Se actualizó el estado activo de un integrante:
                        ID: {}
                        Nombre completo: {}
                        Sede: {} ({})
                        Estado anterior: {}
                        Nuevo estado: {}
                        Modificado por: {} ({})
                        """,
                member.getId(),
                member.getCompleteName(),
                member.getBranch().getName(),
                member.getBranch().getId(),
                previousState,
                member.isActive(),
                user.getUsername(),
                user.getId()
        );

        return member.isActive();
    }

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
                .createdAt(LocalDateTime.now())
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
                LocalDateTime.now()
        );

        memberNotesRepositor.delete(memberNotes);
    }


}