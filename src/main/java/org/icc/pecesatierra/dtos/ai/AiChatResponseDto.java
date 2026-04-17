package org.icc.pecesatierra.dtos.ai;

import lombok.*;
import java.util.List;
import java.util.Map;

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
        private String type; // "bar", "pie", "line"
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
