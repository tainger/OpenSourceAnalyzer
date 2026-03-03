package com.ai.analyzer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspectedLocation {
    private String filePath;
    private int lineNumber;
    private String className;
    private String methodName;
    private String description;
    private double confidence;
}
