package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.exceptions.CategoryWithNameAlreadyExistsException;
import org.icc.pecesatierra.exceptions.MemberCategoryInUseException;
import org.icc.pecesatierra.exceptions.MemberCategoryNotFoundException;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.icc.pecesatierra.utils.mappers.MemberCategoryMapper;
import org.icc.pecesatierra.web.services.MemberCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberCategoryServiceImpl implements MemberCategoryService {

    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberRepository memberRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;

    @Transactional
    @Override
    public MemberCategoryResponseDto create(MemberCategoryRequestDto memberCategoryRequestDto) {
        if (memberCategoryRepository.existsByName(memberCategoryRequestDto.getName())){
            throw new CategoryWithNameAlreadyExistsException("Ya existe una categoria con el nombre " + memberCategoryRequestDto.getName());
        }
        MemberCategory memberCategory = MemberCategory.builder().name(memberCategoryRequestDto.getName()).color(memberCategoryRequestDto.getColor()).build();

        return memberCategoryMapper.toDto(memberCategoryRepository.save(memberCategory));
    }

    @Transactional
    @Override
    public MemberCategoryResponseDto update(MemberCategoryRequestDto memberCategoryRequestDto, String categoryId) {
        MemberCategory memberCategory = memberCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new MemberCategoryNotFoundException("Categoria no encontrada."));

        memberCategoryMapper.updateEntityFromDto(memberCategoryRequestDto, memberCategory);

        return memberCategoryMapper.toDto(memberCategoryRepository.save(memberCategory));
    }

    @Transactional
    @Override
    public void delete(String categoryId) {

        MemberCategory memberCategory = memberCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new MemberCategoryNotFoundException("Categoria no encontrada."));

        if (memberRepository.existsMemberByCategoryId(memberCategory)){
            throw new MemberCategoryInUseException("Esta categoria esta en uso asi que no puede ser eliminada.");
        }

//        if (memberSubCategoryRepository.existsByCategory(memberCategory)){}

        memberCategoryRepository.delete(memberCategory);

    }

    @Transactional(readOnly = true)
    @Override
    public List<MemberCategoryResponseDto> findAll() {
        return memberCategoryRepository.findAll().stream().map(memberCategoryMapper::toDto).toList();
    }
}
