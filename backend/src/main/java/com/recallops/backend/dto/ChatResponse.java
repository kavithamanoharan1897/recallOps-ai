package com.recallops.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String answer;
    private String confidence; // HIGH, MEDIUM, LOW
    private List<SourceMetadata> sources;
    private List<String> relatedIncidents;
    private List<String> memoryTimeline;
    private List<String> reasoningFlow;
    private String sessionId;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private String traceId;
}
