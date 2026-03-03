package com.ai.analyzer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedCode {
    private String filePath;
    private String codeSnippet;
    private String relevance;
}
