package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.exceptions.MemberHasHistoricalRecordException;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.ServerErrorException;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.web.services.MemberService;
import org.icc.pecesatierra.utils.PictureUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;


@Service
@AllArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final AttendanceRepository attendanceRepository;
    private final MemberPersistenceService memberPersistenceService;
    private final PictureUtils pictureUtils;

    @Override
    public MemberResponseDto create(MemberRequestDto memberRequestDto) {

        Map<String, String> pictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile());

        try {
            return memberPersistenceService.save(memberRequestDto, pictureData);
        } catch (Exception e) {
            if (pictureData.get("publicId") != null) {
                pictureUtils.delete(pictureData.get("publicId"));
            }
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde.");
        }
    }

    @Override
    public MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        String oldPublicId = member.getPublicId();
        Map<String, String> newPictureData = null;

        if (memberRequestDto.getPictureProfile() != null) {
            newPictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile());
        }

        try {
            MemberResponseDto response = memberPersistenceService.update(member, memberRequestDto, newPictureData);

            if (newPictureData != null && oldPublicId != null) {
                pictureUtils.delete(oldPublicId);
            }
            return response;

        } catch (Exception e) {
            if (newPictureData != null && newPictureData.get("publicId") != null) {
                pictureUtils.delete(newPictureData.get("publicId"));
            }
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde.");
        }
    }

    @Override
    public MemberPagesResponseDto findAll(int page, boolean onlyActives, String query) {

        Pageable pageable = PageRequest.of(page, 20, Sort.by("id").ascending());

        Page<Member> members = onlyActives ? memberRepository.findAllByOptionalQueryAndActiveTrue(query, pageable) : memberRepository.findAllByQuery(query, pageable);

        int totalPages = members.getTotalPages();

        return new MemberPagesResponseDto(members.stream().map(memberMapper::toDto).toList(), totalPages);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        if (attendanceRepository.existsByMember(member))
            throw new MemberHasHistoricalRecordException("Este integrante tiene historial de asistencias registrado asi que no puede ser eliminado del sistema, considere desactivarlo.");

        String pictureUrl = member.getPictureProfileUrl();

        memberRepository.delete(member);

        if (pictureUrl != null) pictureUtils.delete(pictureUrl);
    }

    @Override
    public boolean updateActive(String memberId, boolean active) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        member.setActive(active);

        memberRepository.save(member);

        return member.isActive();
    }


}