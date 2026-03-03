package com.ai.analyzer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchitectureAnalysisResponse {
    private String repositoryId;
    private String overallStructure;
    private List<String> mainModules;
    private Map<String, String> moduleDescriptions;
    private List<String> designPatterns;
    private List<String> keyFiles;
    private String techStack;
    private List<String> recommendations;
}
