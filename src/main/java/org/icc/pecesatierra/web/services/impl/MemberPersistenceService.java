package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.icc.pecesatierra.entities.MemberType;
import org.icc.pecesatierra.exceptions.MemberCategoryNotFoundException;
import org.icc.pecesatierra.exceptions.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.exceptions.MemberSubCategoryNotFoundException;
import org.icc.pecesatierra.exceptions.MemberTypeNotFoundException;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.icc.pecesatierra.repositories.MemberTypeRepository;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
@Service
public class MemberPersistenceService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberTypeRepository memberTypeRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;

    @Transactional
    public MemberResponseDto save(MemberRequestDto dto, Map<String, String> pictureData) {
        Member member = Member.builder()
//                .type(dto.getTypeId().toString())
//                .category(dto.getCategoryId().toString())
                .completeName(dto.getCompleteName())
                .createdAt(LocalDateTime.now())
                .cc(dto.getCc())
                .cellphone(dto.getCellphone())
//                .address(dto.getAddress())
                .birthdate(dto.getBirthdate())
                .active(true)
                .build();
        member.setNeighborhood(dto.getNeighborhood());
        member.setCity(dto.getCity());
        member.setMunicipality(dto.getMunicipality());
        member.setDistrict(dto.getDistrict());
        member.setPostalCode(dto.getPostalCode());
        member.setLatitude(dto.getLatitude());
        member.setLongitude(dto.getLongitude());

        MemberCategory memberCategory = memberCategoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new MemberCategoryNotFoundException("Categoria invalida."));

        MemberType memberType = memberTypeRepository.findById(dto.getTypeId())
                .orElseThrow(() -> new MemberTypeNotFoundException("Tipo invalido."));

        member.setCategoryId(memberCategory);
        member.setTypeId(memberType);

        if (dto.getSubCategoryId() != null) {
            MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(dto.getSubCategoryId())
                    .orElseThrow(() -> new MemberSubCategoryNotFoundException("Sub categoria no encontrada."));

            if (!member.getCategoryId().equals(memberSubCategory.getCategory())) {
                throw new MemberNoHasCategoryForThisSubCategoryException("Este miembro necesita la categoria " + memberSubCategory.getCategory().getName() + " para poder poder recibir esta sub categoria.");
            }
            member.setSubcategoryId(memberSubCategory);
        }

        if (pictureData.get("url") != null) {
            member.setPictureProfileUrl(pictureData.get("url"));
            member.setPublicId(pictureData.get("publicId"));
        }
        return memberMapper.toDto(memberRepository.save(member), false);
    }

    @Transactional
    public MemberResponseDto update(Member member, MemberRequestDto dto, Map<String, String> picData) {
        memberMapper.updateEntityFromDto(dto, member);
        member.setUpdatedAt(LocalDateTime.now());

        if (picData != null && picData.get("url") != null) {
            member.setPictureProfileUrl(picData.get("url"));
            member.setPublicId(picData.get("publicId"));
        }
        return memberMapper.toDto(memberRepository.save(member), true);
    }

}
