package com.ai.analyzer.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorStackAnalysisRequest {
    @NotBlank(message = "Error stack is required")
    private String errorStack;
    
    private String repositoryId;
}
