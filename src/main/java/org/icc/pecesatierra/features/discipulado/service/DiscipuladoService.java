package org.icc.pecesatierra.features.discipulado.service;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.discipulado.dtos.*;
import org.icc.pecesatierra.features.discipulado.Discipulado;
import org.icc.pecesatierra.features.discipulado.exceptions.*;
import org.icc.pecesatierra.features.discipulado.DiscipuladoProgress;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.member.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.features.discipulado.repository.DiscipuladoProgressRepository;
import org.icc.pecesatierra.features.discipulado.repository.DiscipuladoRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.discipulado.mapper.DiscipuladoMapper;
import org.icc.pecesatierra.features.discipulado.mapper.DiscipuladoProgressMapper;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.DiscipuladoSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DiscipuladoService {

    private final DiscipuladoRepository discipuladoRepository;
    private final DiscipuladoMapper discipuladoMapper;
    private final DateTimeUtils dateTimeUtils;
    private final MemberRepository memberRepository;
    private final DiscipuladoSpecification discipuladoSpecification;
    private final DiscipuladoProgressRepository discipuladoProgressRepository;
    private final DiscipuladoProgressMapper discipuladoProgressMapper;

    @Transactional
    public DiscipuladoResponseDto create(DiscipuladoRequestDto dto, User user) {

        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        Discipulado discipulado = Discipulado.builder()
                .member(member)
                .dateStarted(dateTimeUtils.toUTC(dto.getDateStarted()))
                .registeredBy(user.getMember())
                .createdAt(dateTimeUtils.nowUTC())
                .completed(false)
                .build();

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
            throw new CannotCreateDiscipuladoWithMemberOutsideYourBranch();
        }

        Set<DiscipuladoProgress> progresses = IntStream.range(0, 13).mapToObj(n -> {
            return DiscipuladoProgress.builder()
                    .discipulado(discipulado)
                    .step(n)
                    .completed(false)
                    .registeredBy(user.getMember())
                    .createdAt(dateTimeUtils.nowUTC())
                    .build();
        }).collect(Collectors.toSet());

        discipulado.getProgress().addAll(progresses);

        return discipuladoMapper.toDto(discipuladoRepository.save(discipulado));
    }

    @Transactional(readOnly = true)
    public DiscipuladoResponseDto findById(String id, User user) {
        Discipulado discipulado = discipuladoRepository.findById(id)
                .orElseThrow(DiscipuladoNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(discipulado.getMember().getBranch().getId())) {
            throw new CannotFindDiscipuladoWithMemberOutsideYourBranch();
        }

        return discipuladoMapper.toDto(discipulado);
    }

    @Transactional(readOnly = true)
    public PagesResponseDto<DiscipuladoResponseDto> search(int page, DiscipuladoFilterRequestDto filters, User user) {
        Specification<Discipulado> spec = discipuladoSpecification.build(filters, user);

        Sort sort = Sort.by("member.completeName").ascending();

        if (filters.getOrderBy() != null) {
            sort = filters.getOrderBy().isAsc()
                    ? Sort.by(filters.getOrderBy().getOrderBy()).ascending()
                    : Sort.by(filters.getOrderBy().getOrderBy()).descending();
        }

        Page<Discipulado> pageResult = discipuladoRepository.findAll(
                spec,
                PageRequest.of(
                        page,
                        20,
                        sort
                )
        );

        return PagesResponseDto.<DiscipuladoResponseDto>builder()
                .data(pageResult.stream().map(discipuladoMapper::toDto).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();
    }

    @Transactional
    public void delete(String id, User user) {
        Discipulado discipulado = discipuladoRepository.findById(id)
                .orElseThrow(DiscipuladoNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(discipulado.getMember().getBranch().getId())) {
            throw new CannotDeleteDiscipuladoWithMemberOutsideYourBranch();
        }
        discipulado.getProgress().clear();

        discipuladoRepository.delete(discipulado);
    }

    @Transactional
    public DiscipuladoProgressResponseDto progress(DiscipuladoProgressRequestDto dto, User user) {

        DiscipuladoProgress discipuladoProgress = discipuladoProgressRepository.findById(dto.getDiscipuladoProgressId())
                .orElseThrow(DiscipuladoProgressNotFoundException::new);

        Member teacher = memberRepository.findById(dto.getTeacherId())
                .orElseThrow(MemberNotFoundException::new);

        discipuladoProgress.setCompleted(true);
        discipuladoProgress.setDateCompleted(dateTimeUtils.toUTC(dto.getDateCompleted()));
        discipuladoProgress.setTeacherId(teacher);

        return discipuladoProgressMapper.toDto(discipuladoProgress);
    }

    @Transactional
    public DiscipuladoProgressResponseDto updateProgress(DiscipuladoProgressRequestDto dto, User user, String id) {

        DiscipuladoProgress discipuladoProgress = discipuladoProgressRepository.findById(id)
                .orElseThrow(DiscipuladoProgressNotFoundException::new);

        discipuladoProgress.setTeacherId(memberRepository.findById(dto.getTeacherId())
                .orElseThrow(MemberNotFoundException::new));
        discipuladoProgress.setDateCompleted(dateTimeUtils.toUTC(dto.getDateCompleted()));

        return discipuladoProgressMapper.toDto(discipuladoProgress);
    }
}
