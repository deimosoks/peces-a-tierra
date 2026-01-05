package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.springframework.http.ResponseEntity;

public interface MemberController {
    ResponseEntity<MemberResponseDto> create(MemberRequestDto memberRequestDto);
    ResponseEntity<MemberResponseDto> update(MemberRequestDto memberRequestDto, String memberId);
    ResponseEntity<MemberPagesResponseDto> findAll(int page, boolean onlyActive);
    ResponseEntity<MemberPagesResponseDto> findByQuery(String query, int page, boolean onlyActive);
    ResponseEntity<Void> delete(String memberId);
    ResponseEntity<Boolean> updateActive(String memberId, boolean active);
}
