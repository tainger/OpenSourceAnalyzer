package com.ai.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChunk {
    private String id;
    private String repositoryId;
    private String filePath;
    private String fileName;
    private String fileType;
    private int startLine;
    private int endLine;
    private String content;
    private String summary;
    private List<String> tags;
    private float[] embedding;
}
