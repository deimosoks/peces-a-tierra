package org.icc.pecesatierra.web.controllers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.ai.AiChatRequestDto;
import org.icc.pecesatierra.dtos.ai.AiChatResponseDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController extends BaseController {

    private final AiService aiService;

    @PreAuthorize("isAuthenticated() && @securityService.isActive(authentication)")
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@RequestBody AiChatRequestDto request) {
        return ResponseEntity.ok(aiService.processChat(request, new User()));
    }
}
