package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.exceptions.branches.BranchNotFoundException;
import org.icc.pecesatierra.exceptions.members.CannotCreateMembersOutsideYourBranch;
import org.icc.pecesatierra.exceptions.members.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.exceptions.members.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.members.types.TypeNotFoundException;
import org.icc.pecesatierra.exceptions.members.categories.CategoryNotFoundException;
import org.icc.pecesatierra.exceptions.members.categories.sub.SubCategoryNotFoundException;
import org.icc.pecesatierra.repositories.*;
import org.icc.pecesatierra.utils.MemberCategoryDeterminer;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberPersistenceService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberTypeRepository memberTypeRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final BranchRepository branchRepository;
    private final MemberCategoryDeterminer memberCategoryDeterminer;

    @Transactional
    public MemberResponseDto save(MemberRequestDto dto, Map<String, String> pictureData, User user) {

        Member member = Member.builder()
                .completeName(dto.getCompleteName())
                .createdAt(LocalDateTime.now())
                .cc(dto.getCc())
                .cellphone(dto.getCellphone())
                .birthdate(dto.getBirthdate())
                .gender(dto.getGender().toString())
                .active(true)
                .build();
        member.setNeighborhood(dto.getNeighborhood());
        member.setCity(dto.getCity());
        member.setMunicipality(dto.getMunicipality());
        member.setDistrict(dto.getDistrict());
        member.setPostalCode(dto.getPostalCode());
        member.setLatitude(dto.getLatitude());
        member.setLongitude(dto.getLongitude());

        member.setCategoryLocked(dto.isCategoryLocked());

        if (user.hasAuthority("ADMINISTRATOR") && dto.getBranchId() != null) {
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(BranchNotFoundException::new);
            member.setBranch(branch);
        } else {
            member.setBranch(user.getMember().getBranch());
        }

        MemberCategory memberCategory = memberCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(CategoryNotFoundException::new);

        MemberType memberType = memberTypeRepository.findById(dto.getTypeId())
                .orElseThrow(TypeNotFoundException::new);

        member.setCategoryId(memberCategory);
        member.setTypeId(memberType);

        if (dto.getSubCategoryId() != null && !dto.getSubCategoryId().isEmpty()) {
            MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(dto.getSubCategoryId())
                    .orElseThrow(SubCategoryNotFoundException::new);

            if (!member.getCategoryId().equals(memberSubCategory.getCategory())) {
                log.warn("Usuario {} intento asignar sub-categoria {} al integrante {} pero no coincide con la categoria",
                        user.getMember().getId(),
                        memberSubCategory.getId(),
                        member.getCompleteName());
                throw new MemberNoHasCategoryForThisSubCategoryException(member.getCompleteName());
            }
            member.setSubcategoryId(memberSubCategory);
        }

        if (pictureData != null && pictureData.get("url") != null) {
            member.setPictureProfileUrl(pictureData.get("url"));
            member.setPublicId(pictureData.get("publicId"));
        }

        memberRepository.save(member);

        memberCategoryDeterminer.determineCategory(member);


        log.info("""
                        Usuario {} creó el integrante:
                        ID: {}
                        Nombre: {}
                        CC: {}
                        Celular: {}
                        Ciudad: {}
                        Barrio: {}
                        Branch: {}
                        Categoria: {}
                        Tipo: {}
                        SubCategoria: {}
                        """,
                user.getMember().getId(),
                member.getId(),
                member.getCompleteName(),
                member.getCc(),
                member.getCellphone(),
                member.getCity(),
                member.getNeighborhood(),
                member.getBranch().getName(),
                member.getCategoryId().getName(),
                member.getTypeId().getName(),
                member.getSubcategoryId() != null ? member.getSubcategoryId().getName() : "N/A"
        );

        return memberMapper.toDto(member, false);
    }

    @Transactional
    public MemberResponseDto update(String memberId, MemberRequestDto dto, Map<String, String> picData, User user) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        Member beforeUpdate = Member.builder()
                .id(member.getId())
                .completeName(member.getCompleteName())
                .cc(member.getCc())
                .cellphone(member.getCellphone())
                .city(member.getCity())
                .neighborhood(member.getNeighborhood())
                .branch(member.getBranch())
                .categoryId(member.getCategoryId())
                .typeId(member.getTypeId())
                .subcategoryId(member.getSubcategoryId())
                .build();

        memberMapper.updateEntityFromDto(dto, member);
        member.setUpdatedAt(LocalDateTime.now());

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(member.getBranch().getId())) {
            throw new CannotCreateMembersOutsideYourBranch();
        }

        if (user.hasAuthority("ADMINISTRATOR") && dto.getBranchId() != null) {
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(BranchNotFoundException::new);
            member.setBranch(branch);
        }

        if (picData != null && picData.get("url") != null) {
            member.setPictureProfileUrl(picData.get("url"));
            member.setPublicId(picData.get("publicId"));
        }

        memberRepository.save(member);

        log.info("""
                        Usuario {} actualizó el integrante {}.
                        Estado anterior:
                        Nombre: {}
                        CC: {}
                        Celular: {}
                        Ciudad: {}
                        Barrio: {}
                        Branch: {}
                        Categoria: {}
                        Tipo: {}
                        SubCategoria: {}
                        Nuevo estado:
                        Nombre: {}
                        CC: {}
                        Celular: {}
                        Ciudad: {}
                        Barrio: {}
                        Branch: {}
                        Categoria: {}
                        Tipo: {}
                        SubCategoria: {}
                        """,
                user.getMember().getId(),
                member.getId(),
                beforeUpdate.getCompleteName(),
                beforeUpdate.getCc(),
                beforeUpdate.getCellphone(),
                beforeUpdate.getCity(),
                beforeUpdate.getNeighborhood(),
                beforeUpdate.getBranch().getName(),
                beforeUpdate.getCategoryId().getName(),
                beforeUpdate.getTypeId().getName(),
                beforeUpdate.getSubcategoryId() != null ? beforeUpdate.getSubcategoryId().getName() : "N/A",
                member.getCompleteName(),
                member.getCc(),
                member.getCellphone(),
                member.getCity(),
                member.getNeighborhood(),
                member.getBranch().getName(),
                member.getCategoryId().getName(),
                member.getTypeId().getName(),
                member.getSubcategoryId() != null ? member.getSubcategoryId().getName() : "N/A"
        );

        return memberMapper.toDto(member, true);
    }

}
