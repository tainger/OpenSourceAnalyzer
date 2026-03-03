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
public class ErrorStackAnalysisResponse {
    private String errorType;
    private String rootCause;
    private String summary;
    private List<SuspectedLocation> suspectedLocations;
    private List<String> possibleFixes;
    private List<RelatedCode> relatedCode;
}
