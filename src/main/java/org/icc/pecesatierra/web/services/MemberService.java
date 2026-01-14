package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;

public interface MemberService {
    MemberResponseDto create(MemberRequestDto memberRequestDto);

    MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId);

    MemberPagesResponseDto findAll(int page, boolean onlyActives, String query);

    void delete(String memberId);

    boolean updateActive(String memberId, boolean active);
}
