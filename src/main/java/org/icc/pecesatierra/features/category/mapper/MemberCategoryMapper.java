package org.icc.pecesatierra.features.category.mapper;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryRequestDto;
import org.icc.pecesatierra.features.category.dtos.MemberCategoryResponseDto;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MemberCategoryMapper {

    private final MemberSubCategoryMapper memberSubCategoryMapper;

    public MemberCategoryResponseDto toDto(MemberCategory memberCategory) {
        if (memberCategory == null) {
            return null;
        }

        MemberCategoryResponseDto.MemberCategoryResponseDtoBuilder memberCategoryResponseDto = MemberCategoryResponseDto.builder();

        memberCategoryResponseDto.id(memberCategory.getId());
        memberCategoryResponseDto.name(memberCategory.getName());
        memberCategoryResponseDto.color(memberCategory.getColor());

        if (memberCategory.getSubCategories() != null) {
            memberCategoryResponseDto.subCategories(memberCategory.getSubCategories().stream().map(memberSubCategoryMapper::toDto).collect(Collectors.toSet()));
        }

        return memberCategoryResponseDto.build();
    }

    public void updateEntityFromDto(MemberCategoryRequestDto memberCategoryRequestDto, MemberCategory memberCategory) {
        if (memberCategoryRequestDto == null) {
            return;
        }

        memberCategory.setName(memberCategoryRequestDto.getName());
        memberCategory.setColor(memberCategoryRequestDto.getColor());
    }
}
