package org.icc.pecesatierra.controllers.imp;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.MemberController;
import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.services.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/members")
public class MemberControllerImp implements MemberController {

    private final MemberService memberService;

    @Override
    @PostMapping
    public ResponseEntity<MemberResponseDto> create(@Valid @ModelAttribute MemberRequestDto memberRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.create(memberRequestDto));
    }

    @Override
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponseDto> update(@Valid @ModelAttribute MemberRequestDto memberRequestDto,
                                                    @PathVariable String memberId) {
        return ResponseEntity.ok(memberService.update(memberRequestDto, memberId));
    }

    @Override
    @GetMapping
    public ResponseEntity<MemberPagesResponseDto> findAll(@RequestParam int page,
                                                          @RequestParam boolean onlyActive) {
        return ResponseEntity.ok(memberService.findAll(page, onlyActive));
    }

    @Override
    @GetMapping("/query")
    public ResponseEntity<MemberPagesResponseDto> findByQuery(@RequestParam String query,
                                                              @RequestParam int page,
                                                              @RequestParam boolean onlyActive) {
        return ResponseEntity.ok(memberService.findByQuery(query, page, onlyActive));
    }

    @Override
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> delete(@PathVariable String memberId) {
        memberService.delete(memberId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{memberId}")
    public ResponseEntity<Boolean> updateActive(@PathVariable String memberId,
                                                @RequestParam boolean active) {
        return ResponseEntity.ok(memberService.updateActive(memberId, active));
    }


}
