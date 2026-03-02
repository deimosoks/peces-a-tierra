package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberCategoryResponseDto;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.members.categories.CategoryWithNameAlreadyExistsException;
import org.icc.pecesatierra.exceptions.members.categories.CategoryInUseException;
import org.icc.pecesatierra.exceptions.members.categories.CategoryNotFoundException;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.MemberCategoryMapper;
import org.icc.pecesatierra.web.services.MemberCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCategoryServiceImpl implements MemberCategoryService {

    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public MemberCategoryResponseDto create(MemberCategoryRequestDto memberCategoryRequestDto, User user) {
        if (memberCategoryRepository.existsByName(memberCategoryRequestDto.getName())) {
            log.warn("Usuario {} intento registrar la categoria con nombre {} pero ya se encuentra registrada una categoria con este nombre.", user.getMember().getId(), memberCategoryRequestDto.getName());
            throw new CategoryWithNameAlreadyExistsException("Ya existe una categoria con el nombre " + memberCategoryRequestDto.getName());
        }
        MemberCategory memberCategory = MemberCategory.builder().name(memberCategoryRequestDto.getName()).color(memberCategoryRequestDto.getColor()).build();

        return memberCategoryMapper.toDto(memberCategoryRepository.save(memberCategory));
    }

    @Transactional
    @Override
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
    @Override
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
    @Override
    public List<MemberCategoryResponseDto> findAll() {
        return memberCategoryRepository.findAll().stream().map(memberCategoryMapper::toDto).toList();
    }
}
