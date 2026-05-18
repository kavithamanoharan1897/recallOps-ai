package com.recallops.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceMetadata {
    private String title;
    private String type;
    private String url;
    private String snippet;
    private double relevanceScore;
}
