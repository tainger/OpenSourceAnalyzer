package com.ai.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Repository {
    private String id;
    private String name;
    private String url;
    private String localPath;
    private String branch;
    private LocalDateTime clonedAt;
    private LocalDateTime lastAnalyzedAt;
    private AnalysisStatus status;
}
