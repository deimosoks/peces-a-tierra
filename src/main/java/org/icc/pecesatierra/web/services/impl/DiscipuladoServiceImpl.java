package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.discipulado.*;
import org.icc.pecesatierra.entities.Discipulado;
import org.icc.pecesatierra.entities.DiscipuladoProgress;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.discipulado.DiscipuladoNotFoundException;
import org.icc.pecesatierra.exceptions.discipulado.DiscipuladoProgressNotFoundException;
import org.icc.pecesatierra.exceptions.members.MemberNotFoundException;
import org.icc.pecesatierra.repositories.DiscipuladoProgressRepository;
import org.icc.pecesatierra.repositories.DiscipuladoRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.DiscipuladoMapper;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.DiscipuladoSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.icc.pecesatierra.web.services.DiscipuladoService;
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
public class DiscipuladoServiceImpl implements DiscipuladoService {

    private final DiscipuladoRepository discipuladoRepository;
    private final DiscipuladoMapper discipuladoMapper;
    private final DateTimeUtils dateTimeUtils;
    private final MemberRepository memberRepository;
    private final DiscipuladoSpecification discipuladoSpecification;
    private final DiscipuladoProgressRepository discipuladoProgressRepository;

    @Override
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

        return discipuladoMapper.toDto(discipulado);
    }

    @Override
    @Transactional(readOnly = true)
    public DiscipuladoResponseDto findById(String id) {
        return discipuladoMapper.toDto(discipuladoRepository.findById(id)
                .orElseThrow(DiscipuladoNotFoundException::new));
    }

    @Override
    @Transactional(readOnly = true)
    public PagesResponseDto<DiscipuladoResponseDto> search(int page, DiscipuladoFilterRequestDto filters, User user) {
        Specification<Discipulado> spec = discipuladoSpecification.build(filters, user);

        Sort sort = Sort.by("member.completeName").ascending();

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

    @Override
    @Transactional
    public void delete(String id) {
        Discipulado discipulado = discipuladoRepository.findById(id)
                .orElseThrow(DiscipuladoNotFoundException::new);
        discipulado.getProgress().clear();
        discipuladoRepository.delete(discipulado);
    }

    @Override
    @Transactional
    public DiscipuladoProgressResponseDto progress(DiscipuladoProgressRequestDto dto, User user) {

        DiscipuladoProgress discipuladoProgress = discipuladoProgressRepository.findById(dto.getDiscipuladoProgressId())
                .orElseThrow(DiscipuladoProgressNotFoundException::new);

        Member teacher = memberRepository.findById(dto.getTeacherId())
                .orElseThrow(MemberNotFoundException::new);

        discipuladoProgress.setCompleted(true);
        discipuladoProgress.setDateCompleted(dateTimeUtils.toUTC(dto.getDateCompleted()));
        discipuladoProgress.setTeacherId(teacher);

        return null;
    }
}
