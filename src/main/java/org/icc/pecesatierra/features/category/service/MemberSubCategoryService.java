package org.icc.pecesatierra.features.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.features.category.dtos.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.category.exceptions.sub.AlreadyExistsSubCategoryWithName;
import org.icc.pecesatierra.features.category.exceptions.CategoryNotFoundException;
import org.icc.pecesatierra.features.category.exceptions.sub.CannotDeleteSubCategoryInUseException;
import org.icc.pecesatierra.features.category.exceptions.sub.SubCategoryNotFoundException;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.mapper.MemberSubCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSubCategoryService {

    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberSubCategoryMapper memberSubCategoryMapper;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberSubCategoryResponseDto create(MemberSubCategoryRequestDto memberSubCategoryRequestDto, User user) {


        MemberCategory memberCategory = memberCategoryRepository.findById(memberSubCategoryRequestDto.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        if (memberSubCategoryRepository.existsByCategoryAndName(memberCategory, memberSubCategoryRequestDto.getName())) {
            log.warn("Usuario {} intento crear sub-categoria {} en la categoria {} pero ya existe una sub-categoria dentro de esa categoria con ese nombre.",
                    user.getMember().getId(), memberSubCategoryRequestDto.getName(), memberCategory.getId());
            throw new AlreadyExistsSubCategoryWithName(memberSubCategoryRequestDto.getName(), memberCategory.getName());
        }

        MemberSubCategory memberSubCategory = MemberSubCategory.builder()
                .color(memberSubCategoryRequestDto.getColor())
                .name(memberSubCategoryRequestDto.getName())
                .category(memberCategory)
                .build();

        memberSubCategoryRepository.save(memberSubCategory);

        log.info("""
                        Usuario {} creó una sub-categoria:
                        ID: {}
                        Nombre: {}
                        Color: {}
                        Categoria: {}
                        """,
                user.getMember().getId(),
                memberSubCategory.getId(),
                memberSubCategory.getName(),
                memberSubCategory.getColor(),
                memberSubCategory.getCategory().getName());

        return memberSubCategoryMapper.toDto(memberSubCategory);
    }

    @Transactional
    public MemberSubCategoryResponseDto update(MemberSubCategoryRequestDto memberSubCategoryRequestDto, String subCategoryId, User user) {

        MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(subCategoryId)
                .orElseThrow(SubCategoryNotFoundException::new);

        if (memberSubCategoryRepository.existsByCategoryAndName(memberSubCategory.getCategory(), memberSubCategoryRequestDto.getName())) {
            log.warn("Usuario {} intento actualizar sub-categoria {} en la categoria {} pero ya existe una sub-categoria dentro de esa categoria con ese nombre.",
                    user.getMember().getId(), memberSubCategoryRequestDto.getName(), memberSubCategory.getCategory().getId());
            throw new AlreadyExistsSubCategoryWithName(memberSubCategoryRequestDto.getName(), memberSubCategory.getCategory().getName());
        }

        MemberSubCategory beforeUpdate = MemberSubCategory.builder()
                .id(memberSubCategory.getId())
                .name(memberSubCategory.getName())
                .color(memberSubCategory.getColor())
                .category(memberSubCategory.getCategory())
                .build();

        memberSubCategoryMapper.updateEntityFromDto(memberSubCategoryRequestDto, memberSubCategory);

        log.info("""
                        Usuario {} actualizó la sub-categoria {}.
                        Estado anterior:
                        Nombre: {}
                        Color: {}
                        Categoria: {}
                        Nuevo estado:
                        Nombre: {}
                        Color: {}
                        Categoria: {}
                        """,
                user.getMember().getId(),
                memberSubCategory.getId(),
                beforeUpdate.getName(),
                beforeUpdate.getColor(),
                beforeUpdate.getCategory().getName(),
                memberSubCategory.getName(),
                memberSubCategory.getColor(),
                memberSubCategory.getCategory().getName());


        return memberSubCategoryMapper.toDto(memberSubCategory);
    }

    @Transactional
    public void delete(String subCategoryId, User user) {

        MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(subCategoryId)
                .orElseThrow(SubCategoryNotFoundException::new);

        if (memberRepository.existsMemberBySubcategoryId(memberSubCategory)) {
            log.warn("Usuario {} intento eliminar sub-categoria '{}' pero está en uso.", user.getMember().getId(), memberSubCategory.getName());
            throw new CannotDeleteSubCategoryInUseException(memberSubCategory.getName());
        }

        log.info("""
                        Usuario {} eliminó la sub-categoria:
                        ID: {}
                        Nombre: {}
                        Categoria: {}
                        """,
                user.getMember().getId(),
                memberSubCategory.getId(),
                memberSubCategory.getName(),
                memberSubCategory.getCategory().getName());

        memberSubCategoryRepository.delete(memberSubCategory);
    }
}
