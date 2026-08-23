package org.icc.pecesatierra.features.ai.controller;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.utils.enums.AppPermission;
import org.icc.pecesatierra.utils.models.BaseController;
import org.icc.pecesatierra.features.ai.dtos.AiChatRequestDto;
import org.icc.pecesatierra.features.ai.dtos.AiChatResponseDto;
import org.icc.pecesatierra.features.ai.service.AiService;
import org.icc.pecesatierra.features.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController extends BaseController {

    private final AiService aiService;

    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).CHAT_AI.name()) || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
            ) && @securityService.isActive(authentication)
            """)
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@RequestBody AiChatRequestDto request) {
        return ResponseEntity.ok(aiService.processChat(request, new User()));
    }
}
