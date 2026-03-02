package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesRequestDto;
import org.icc.pecesatierra.dtos.member.category.rules.CategoryRulesResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.CategoryRulesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/category-rules")
public class CategoryRulesController {

    private final CategoryRulesService categoryRulesService;

    @PostMapping
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<CategoryRulesResponseDto> create(@RequestBody @Valid CategoryRulesRequestDto categoryRulesRequestDto,
                                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRulesService.create(categoryRulesRequestDto, user));
    }

    @PutMapping("/{categoryRuleId}")
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<CategoryRulesResponseDto> update(@RequestBody @Valid CategoryRulesRequestDto categoryRulesRequestDto,
                                                           @AuthenticationPrincipal User user,
                                                           @PathVariable String categoryRuleId) {
        return ResponseEntity.ok(categoryRulesService.update(categoryRulesRequestDto, user, categoryRuleId));
    }

    @PatchMapping("/{categoryRuleId}")
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Boolean> updateActive(@RequestParam boolean state,
                                                @PathVariable String categoryRuleId,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(categoryRulesService.updateActive(categoryRuleId, user, state));
    }

    @DeleteMapping("/{categoryRuleId}")
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<Void> delete(@PathVariable String categoryRuleId,
                                       @AuthenticationPrincipal User user) {
        categoryRulesService.delete(categoryRuleId, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("""
            hasAuthority('ADMINISTRATOR') 
            &&
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<List<CategoryRulesResponseDto>> findAll(){
        return ResponseEntity.ok(categoryRulesService.findAll());
    }

}
