package org.icc.pecesatierra.services;

import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;

public interface MemberService {
    MemberResponseDto create(MemberRequestDto memberRequestDto);

    MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId);

    MemberPagesResponseDto findAll(int page, boolean onlyActives);

    MemberPagesResponseDto findByQuery(String query, int page, boolean onlyActives);

    void delete(String memberId);

    boolean updateActive(String memberId, boolean active);
}
