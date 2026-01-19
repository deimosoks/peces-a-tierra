package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface MemberMapper {
    MemberResponseDto toDto(Member member);

    void updateEntityFromDto(MemberRequestDto memberRequestDto, @MappingTarget Member member);

}
