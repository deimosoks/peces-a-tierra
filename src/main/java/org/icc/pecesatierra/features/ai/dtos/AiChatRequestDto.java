package org.icc.pecesatierra.features.ai.dtos;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatRequestDto {
    private String message;
    private List<ChatMessage> history;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
