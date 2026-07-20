package org.icc.pecesatierra.features.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryRequestDto;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryResponseDto;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.features.category.exceptions.CategoryWithNameAlreadyExistsException;
import org.icc.pecesatierra.features.category.exceptions.CategoryInUseException;
import org.icc.pecesatierra.features.category.exceptions.CategoryNotFoundException;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.category.mapper.MemberCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCategoryService {

    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberRepository memberRepository;

    @Transactional
    public MemberCategoryResponseDto create(MemberCategoryRequestDto memberCategoryRequestDto, User user) {
        if (memberCategoryRepository.existsByName(memberCategoryRequestDto.getName())) {
            log.warn("Usuario {} intento registrar la categoria con nombre {} pero ya se encuentra registrada una categoria con este nombre.", user.getMember().getId(), memberCategoryRequestDto.getName());
            throw new CategoryWithNameAlreadyExistsException("Ya existe una categoria con el nombre " + memberCategoryRequestDto.getName());
        }
        MemberCategory memberCategory = MemberCategory.builder().name(memberCategoryRequestDto.getName()).color(memberCategoryRequestDto.getColor()).build();

        return memberCategoryMapper.toDto(memberCategoryRepository.save(memberCategory));
    }

    @Transactional
    public MemberCategoryResponseDto update(MemberCategoryRequestDto memberCategoryRequestDto, String categoryId, User user) {
        MemberCategory memberCategory = memberCategoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);

        MemberCategory beforeUpdate = MemberCategory.builder()
                .id(memberCategory.getId())
                .name(memberCategory.getName())
                .color(memberCategory.getColor())
                .build();

        memberCategoryMapper.updateEntityFromDto(memberCategoryRequestDto, memberCategory);

        log.info("""
                        Usuario {} hizo cambios a la categoria {}.
                        Estado anterior:
                        Nombre: {}
                        Color: {}
                        Nuevo estado:
                        Nombre: {}
                        Color: {}
                        """
                , user.getMember().getId()
                , memberCategory.getId()
                , beforeUpdate.getName()
                , beforeUpdate.getColor()
                , memberCategory.getName()
                , memberCategory.getColor());

        return memberCategoryMapper.toDto(memberCategory);
    }

    @Transactional
    public void delete(String categoryId, User user) {

        MemberCategory memberCategory = memberCategoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);

        if (memberRepository.existsMemberByCategoryId(memberCategory)) {
            log.warn("Usuario {} intento eliminar la categoria {} pero esta se encuentra en uso asi que no puede ser eliminado.", user.getMember().getId(), memberCategory.getId());
            throw new CategoryInUseException();
        }

        log.info("""
                        Usuario {} elimino la categoria:
                        Id: {}
                        Nombre: {}
                        Color: {}
                        """
                , user.getMember().getId()
                , memberCategory.getId()
                , memberCategory.getName()
                , memberCategory.getColor());

        memberCategoryRepository.delete(memberCategory);

    }

    @Transactional(readOnly = true)
    public List<MemberCategoryResponseDto> findAll() {
        return memberCategoryRepository.findAll().stream().map(memberCategoryMapper::toDto).toList();
    }
}
