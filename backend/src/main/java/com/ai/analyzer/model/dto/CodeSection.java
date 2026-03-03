package com.ai.analyzer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSection {
    private String sectionName;
    private int startLine;
    private int endLine;
    private String explanation;
    private String codeSnippet;
}
