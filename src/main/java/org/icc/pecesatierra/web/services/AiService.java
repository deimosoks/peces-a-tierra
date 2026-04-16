package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.ai.AiChatRequestDto;
import org.icc.pecesatierra.dtos.ai.AiChatResponseDto;
import org.icc.pecesatierra.entities.User;

public interface AiService {
    AiChatResponseDto processChat(AiChatRequestDto request, User user);
}
