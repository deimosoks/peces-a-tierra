package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberExportDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.icc.pecesatierra.entities.MemberType;
import org.icc.pecesatierra.exceptions.members.categories.CategoryNotFoundException;
import org.icc.pecesatierra.exceptions.members.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.exceptions.members.categories.sub.SubCategoryNotFoundException;
import org.icc.pecesatierra.exceptions.members.types.TypeNotFoundException;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.icc.pecesatierra.repositories.MemberTypeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberMapper {

    private final MemberNotesMapper memberNotesMapper;
    private final MemberCategoryMapper memberCategoryMapper;
    private final MemberTypeMapper memberTypeMapper;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberTypeRepository memberTypeRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;
    private final MemberSubCategoryMapper memberSubCategoryMapper;
    private final BranchMapper branchMapper;

    public MemberResponseDto toDto(Member member, boolean withNotes) {
        if (member == null) {
            return null;
        }

        MemberResponseDto.MemberResponseDtoBuilder memberResponseDto = MemberResponseDto.builder();

        memberResponseDto.id(member.getId());
        memberResponseDto.completeName(member.getCompleteName());

        memberResponseDto.category(memberCategoryMapper.toDto(member.getCategoryId()));
        memberResponseDto.type(memberTypeMapper.toDto(member.getTypeId()));

        memberResponseDto.cellphone(member.getCellphone());
        memberResponseDto.address(member.getAddress());
        memberResponseDto.birthdate(member.getBirthdate());
        memberResponseDto.cc(member.getCc());
        memberResponseDto.createdAt(member.getCreatedAt());
        memberResponseDto.updatedAt(member.getUpdatedAt());
        memberResponseDto.pictureProfileUrl(member.getPictureProfileUrl());
        memberResponseDto.active(member.isActive());
        memberResponseDto.gender(member.getGender());
        memberResponseDto.branch(branchMapper.toDto(member.getBranch()));
        memberResponseDto.categoryLocked(member.isCategoryLocked());

        if (member.getSubcategoryId() != null) {
            memberResponseDto.subCategory(memberSubCategoryMapper.toDto(member.getSubcategoryId()));
        }

        if (member.getBirthdate() != null) {
            memberResponseDto.age((int) ChronoUnit.YEARS.between(member.getBirthdate(), LocalDateTime.now()));
        }

        if (withNotes) {
            memberResponseDto.notes(member.getNotes().stream().map(memberNotesMapper::toDto).collect(Collectors.toSet()));
        }

        return memberResponseDto.build();
    }

    public MemberExportDto toExportDto(Member member) {
        if (member == null) {
            return null;
        }

        MemberExportDto.MemberExportDtoBuilder memberExportDto = MemberExportDto.builder();

        memberExportDto.completeName(member.getCompleteName());

        memberExportDto.type(member.getTypeId().getName());
        memberExportDto.category(member.getCategoryId().getName());
        memberExportDto.gender(member.getGender());
        memberExportDto.cellphone(member.getCellphone());
        memberExportDto.address(member.getAddress());
        memberExportDto.birthdate(member.getBirthdate());
        memberExportDto.cc(member.getCc());

        if (member.getBirthdate() != null) {
            memberExportDto.age((int) ChronoUnit.YEARS.between(member.getBirthdate(), LocalDateTime.now()));
        }

        if (member.getSubcategoryId() != null) {
            memberExportDto.subCategory(member.getSubcategoryId().getName());
        }

        return memberExportDto.build();
    }

    public void updateEntityFromDto(MemberRequestDto memberRequestDto, Member member) {
        if (memberRequestDto == null) {
            return;
        }

        if (!Objects.equals(memberRequestDto.getTypeId(), member.getTypeId().getId())) {
            MemberType memberType = memberTypeRepository.findById(memberRequestDto.getTypeId())
                    .orElseThrow(TypeNotFoundException::new);
            member.setTypeId(memberType);
        }

        if (!Objects.equals(memberRequestDto.getCategoryId(), member.getCategoryId().getId())) {
            MemberCategory memberCategory = memberCategoryRepository.findById(memberRequestDto.getCategoryId())
                    .orElseThrow(CategoryNotFoundException::new);
            member.setCategoryId(memberCategory);
        }
        member.setSubcategoryId(null);
        if (memberRequestDto.getSubCategoryId() != null && !memberRequestDto.getSubCategoryId().isEmpty()) {
            MemberSubCategory memberSubCategory = memberSubCategoryRepository.findById(memberRequestDto.getSubCategoryId())
                    .orElseThrow(SubCategoryNotFoundException::new);

            if (!member.getCategoryId().getId().equals(memberSubCategory.getCategory().getId())) {
                throw new MemberNoHasCategoryForThisSubCategoryException(member.getCompleteName());
            }
            member.setSubcategoryId(memberSubCategory);
        }

        member.setCompleteName(memberRequestDto.getCompleteName());
        member.setCc(memberRequestDto.getCc());
        member.setCellphone(memberRequestDto.getCellphone());
        member.setAddress(memberRequestDto.getAddress());
        member.setBirthdate(memberRequestDto.getBirthdate());
        member.setGender(memberRequestDto.getGender().toString());

        member.setNeighborhood(memberRequestDto.getNeighborhood());
        member.setCity(memberRequestDto.getCity());
        member.setMunicipality(memberRequestDto.getMunicipality());
        member.setDistrict(memberRequestDto.getDistrict());
        member.setPostalCode(memberRequestDto.getPostalCode());
        member.setLatitude(memberRequestDto.getLatitude());
        member.setLongitude(memberRequestDto.getLongitude());
        member.setCategoryLocked(member.isCategoryLocked());

    }
}
