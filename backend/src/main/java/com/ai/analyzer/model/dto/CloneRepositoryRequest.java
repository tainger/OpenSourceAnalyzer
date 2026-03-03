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
public class CloneRepositoryRequest {
    @NotBlank(message = "Repository URL is required")
    private String url;
    
    private String branch;
}
