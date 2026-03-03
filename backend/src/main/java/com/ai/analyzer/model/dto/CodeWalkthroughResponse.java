package com.ai.analyzer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeWalkthroughResponse {
    private String repositoryId;
    private String filePath;
    private String fileSummary;
    private List<CodeSection> sections;
    private List<String> dependencies;
    private List<String> dependents;
}
