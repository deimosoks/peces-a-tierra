package org.icc.pecesatierra.features.ai.dtos;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiChatResponseDto {
    private String answer;
    private ChartData chartData;
    private boolean requiresClarification;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartData {
        private String type;
        private List<String> labels;
        private List<Series> series;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Series {
        private String name;
        private List<Number> data;
    }
}
