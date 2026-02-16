package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.icc.pecesatierra.exceptions.MemberCategoryNotFoundException;
import org.icc.pecesatierra.exceptions.MemberSubCategoryInUseException;
import org.icc.pecesatierra.exceptions.MemberSubCategoryNotFoundException;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.icc.pecesatierra.utils.mappers.MemberSubCategoryMapper;
import org.icc.pecesatierra.web.services.MemberSubCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MemberSubCategoryServiceImpl implements MemberSubCategoryService {

    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberSubCategoryMapper memberSubCategoryMapper;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public MemberSubCategoryResponseDto create(MemberSubCategoryRequestDto memberSubCategoryRequestDto) {
        MemberCategory memberCategory = memberCategoryRepository.findById(memberSubCategoryRequestDto.getCategoryId())
                .orElseThrow(() -> new MemberCategoryNotFoundException("Categoria no encontrada."));

        MemberSubCategory memberSubCategory = MemberSubCategory.builder()
                .color(memberSubCategoryRequestDto.getColor())
                .name(memberSubCategoryRequestDto.getName())
                .category(memberCategory)
                .build();

        return memberSubCategoryMapper.toDto(memberSubCategoryRepository.save(memberSubCategory));
    }

    @Transactional
    @Override
    public MemberSubCategoryResponseDto update(MemberSubCategoryRequestDto memberSubCategoryRequestDto, String subCategoryId) {
        MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new MemberSubCategoryNotFoundException("Sub categoria no encontrada."));

        memberSubCategoryMapper.updateEntityFromDto(memberSubCategoryRequestDto, memberSubCategory);

        return memberSubCategoryMapper.toDto(memberSubCategoryRepository.save(memberSubCategory));
    }

    @Transactional
    @Override
    public void delete(String subCategoryId) {

        MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new MemberSubCategoryNotFoundException("Sub categoria no encontrada."));

        if (memberRepository.existsMemberBySubcategoryId(memberSubCategory)) {
            throw new MemberSubCategoryInUseException("Esta sub categoria esta en uso asi que no puede ser eliminada.");
        }

        memberSubCategoryRepository.delete(memberSubCategory);

    }
}
