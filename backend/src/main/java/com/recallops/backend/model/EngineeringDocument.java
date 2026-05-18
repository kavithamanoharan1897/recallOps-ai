package com.recallops.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineeringDocument {
    private String id;
    private String title;
    private String content;
    private String type; // incident, deployment, kt-note, playbook
    private Set<String> tags;
    private String lastModified;
    private String author;
}
