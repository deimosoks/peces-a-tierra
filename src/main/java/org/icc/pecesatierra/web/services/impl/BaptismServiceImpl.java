package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.baptism.*;
import org.icc.pecesatierra.entities.Baptism;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.baptisms.BaptismNotFoundException;
import org.icc.pecesatierra.exceptions.baptisms.CannotRegisterBaptismWithMemberOutsideYourBranch;
import org.icc.pecesatierra.exceptions.members.CannotCreateMembersOutsideYourBranch;
import org.icc.pecesatierra.exceptions.members.MemberAlreadyHasRegisteredBaptismActiveException;
import org.icc.pecesatierra.exceptions.members.notes.NoteNotFoundException;
import org.icc.pecesatierra.repositories.BaptismRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.BaptismMapper;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.BaptismSpecification;
import org.icc.pecesatierra.web.services.BaptismService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaptismServiceImpl implements BaptismService {

    private final BaptismRepository baptismRepository;
    private final MemberRepository memberRepository;
    private final BaptismMapper baptismMapper;
    private final BaptismSpecification baptismSpecification;

    @Transactional
    @Override
    public BaptismResponseDto create(BaptismRequestDto baptismRequestDto, User user) {

        Member member = memberRepository.findById(baptismRequestDto.getMemberId())
                .orElseThrow(NoteNotFoundException::new);

        if (baptismRepository.existsByBaptizedMemberAndInvalidFalse(member)) {
            log.info("Usuario {} intento registrar un bautismo al integrante {} el cual ya tiene un bautismo registrado", user.getMember().getId(), member.getId());
            throw new MemberAlreadyHasRegisteredBaptismActiveException(member.getCompleteName());
        }

        if (!user.hasAuthority("ADMINISTRATOR") && !member.getBranch().equals(user.getMember().getBranch())) {
            log.warn("Usuario {} intento registrar un bautismo con el integrante {} que esta fuera de su sede.", user.getMember().getId(), member.getId());
            throw new CannotRegisterBaptismWithMemberOutsideYourBranch();
        }

        Baptism baptism = Baptism.builder()
                .baptizedMember(member)
                .date(baptismRequestDto.getDate())
                .neighborhood(baptismRequestDto.getNote())
                .createdAt(LocalDateTime.now())
                .registeredBy(user.getMember())
                .address(baptismRequestDto.getAddress())
                .neighborhood(baptismRequestDto.getNeighborhood())
                .city(baptismRequestDto.getCity())
                .municipality(baptismRequestDto.getCity())
                .district(baptismRequestDto.getDistrict())
                .postalCode(baptismRequestDto.getPostalCode())
                .latitude(baptismRequestDto.getLatitude())
                .longitude(baptismRequestDto.getLongitude())
                .invalid(false)
                .build();

        log.info("Usuario {} registro una bautismo al integrante {}", user.getMember().getId(), member.getId());

        return baptismMapper.toDto(baptismRepository.save(baptism));
    }

    @Transactional
    @Override
    public BaptismResponseDto invalid(BaptismInvalidRequestDto baptismInvalidRequestDto, User user) {

        Baptism baptism = baptismRepository.findById(baptismInvalidRequestDto.getBaptismId())
                .orElseThrow(BaptismNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !baptism.getBaptizedMember().getBranch().equals(user.getMember().getBranch())) {
            log.warn("Usuario {} intento invalidar un bautismo al integrante {} el cual esta fuera de su sede.", user.getMember().getId(), baptism.getBaptizedMember().getId());
            throw new CannotCreateMembersOutsideYourBranch();
        }

        baptism.setInvalid(true);
        baptism.setInvalidatorId(user.getMember());
        baptism.setInvalidReason(baptismInvalidRequestDto.getInvalidReason());
        baptism.setInvalidAt(LocalDateTime.now());

        log.info("Usuario {} invalido el bautismo {}", user.getMember().getId(), baptism.getId());

        return baptismMapper.toDto(baptism);
    }

    @Transactional(readOnly = true)
    @Override
    public PagesResponseDto<BaptismResponseDto> search(int page, BaptismFilterRequestDto dto, User user) {

        Specification<Baptism> spec = baptismSpecification.build(dto, user);

        Page<Baptism> pageResult = baptismRepository.findAll(
                spec,
                PageRequest.of(page, 20, Sort.by("baptizedMember.completeName"))
        );

        return PagesResponseDto.<BaptismResponseDto>builder()
                .data(pageResult.stream().map(baptismMapper::toDto).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();
    }

}
