package org.icc.pecesatierra.features.category.repository.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.branch.mapper.BranchMapper;
import org.icc.pecesatierra.features.category.mapper.MemberCategoryMapper;
import org.icc.pecesatierra.features.category.mapper.MemberSubCategoryMapper;
import org.icc.pecesatierra.features.member.dtos.MemberExportDto;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.category.exceptions.CategoryNotFoundException;
import org.icc.pecesatierra.features.member.exceptions.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.features.category.exceptions.sub.SubCategoryNotFoundException;
import org.icc.pecesatierra.features.type.exceptions.TypeNotFoundException;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
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
    private final DateTimeUtils dateTimeUtils;

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
        memberResponseDto.createdAt(dateTimeUtils.toColombia(member.getCreatedAt()));
        memberResponseDto.updatedAt(dateTimeUtils.toColombia(member.getUpdatedAt()));
        memberResponseDto.pictureProfileUrl(member.getPictureProfileUrl());
        memberResponseDto.active(member.isActive());
        memberResponseDto.gender(member.getGender());
        memberResponseDto.branch(branchMapper.toDto(member.getBranch()));
        memberResponseDto.categoryLocked(member.isCategoryLocked());
        memberResponseDto.registeredBy(member.getRegisteredBy() != null ? member.getRegisteredBy().getCompleteName() : "desconocido");

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
