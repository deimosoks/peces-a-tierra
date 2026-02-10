package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberExportDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.utils.enums.CategoryMember;
import org.icc.pecesatierra.utils.enums.TypeMember;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

//@Mapper(componentModel = "spring")
@Component
@RequiredArgsConstructor
public class MemberMapper {
//    MemberResponseDto toDto(Member member);

//    MemberExportDto toExportDto(Member member);

    //    void updateEntityFromDto(MemberRequestDto memberRequestDto, @MappingTarget Member member);

    private final MemberNotesMapper memberNotesMapper;

    public MemberResponseDto toDto(Member member, boolean withNotes) {
        if (member == null) {
            return null;
        }

        MemberResponseDto.MemberResponseDtoBuilder memberResponseDto = MemberResponseDto.builder();

        memberResponseDto.id(member.getId());
        memberResponseDto.completeName(member.getCompleteName());
        if (member.getType() != null) {
            memberResponseDto.type(Enum.valueOf(TypeMember.class, member.getType()));
        }
        if (member.getCategory() != null) {
            memberResponseDto.category(Enum.valueOf(CategoryMember.class, member.getCategory()));
        }
        memberResponseDto.cellphone(member.getCellphone());
        memberResponseDto.address(member.getAddress());
        memberResponseDto.birthdate(member.getBirthdate());
        memberResponseDto.cc(member.getCc());
        memberResponseDto.createdAt(member.getCreatedAt());
        memberResponseDto.updatedAt(member.getUpdatedAt());
        memberResponseDto.pictureProfileUrl(member.getPictureProfileUrl());
        memberResponseDto.active(member.isActive());

        if (member.getBirthdate() != null){
            memberResponseDto.age(LocalDate.now().getYear() - member.getBirthdate().getYear());
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
        if (member.getType() != null) {
            memberExportDto.type(Enum.valueOf(TypeMember.class, member.getType()));
        }
        if (member.getCategory() != null) {
            memberExportDto.category(Enum.valueOf(CategoryMember.class, member.getCategory()));
        }
        memberExportDto.cellphone(member.getCellphone());
        memberExportDto.address(member.getAddress());
        memberExportDto.birthdate(member.getBirthdate());
        memberExportDto.cc(member.getCc());

        return memberExportDto.build();
    }

    public void updateEntityFromDto(MemberRequestDto memberRequestDto, Member member) {
        if (memberRequestDto == null) {
            return;
        }

        if (memberRequestDto.getType() != null) {
            member.setType(memberRequestDto.getType().name());
        } else {
            member.setType(null);
        }
        if (memberRequestDto.getCategory() != null) {
            member.setCategory(memberRequestDto.getCategory().name());
        } else {
            member.setCategory(null);
        }
        member.setCompleteName(memberRequestDto.getCompleteName());
        member.setCc(memberRequestDto.getCc());
        member.setCellphone(memberRequestDto.getCellphone());
        member.setAddress(memberRequestDto.getAddress());
        member.setBirthdate(memberRequestDto.getBirthdate());
    }
}
