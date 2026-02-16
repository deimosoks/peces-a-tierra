package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryRequestDto;
import org.icc.pecesatierra.dtos.member.category.MemberSubCategoryResponseDto;
import org.icc.pecesatierra.web.services.MemberSubCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/member-sub-category")
public class MemberSubCategoryController {

    private final MemberSubCategoryService memberSubCategoryService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    public ResponseEntity<MemberSubCategoryResponseDto> create(@Valid @RequestBody MemberSubCategoryRequestDto memberSubCategoryRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(memberSubCategoryService.create(memberSubCategoryRequestDto));
    }

    @PutMapping("/{subCategoryId}")
    @PreAuthorize("hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    public ResponseEntity<MemberSubCategoryResponseDto> update(@Valid @RequestBody MemberSubCategoryRequestDto memberSubCategoryRequestDto,
                                                              @PathVariable String subCategoryId){
        return ResponseEntity.ok(memberSubCategoryService.update(memberSubCategoryRequestDto, subCategoryId));
    }

    @DeleteMapping("/{subCategoryId}")
    @PreAuthorize("hasAuthority('ADMINISTRATOR') && @securityService.isActive(authentication)")
    public ResponseEntity<Void> delete(@PathVariable String subCategoryId){
        memberSubCategoryService.delete(subCategoryId);
        return ResponseEntity.noContent().build();
    }

}
