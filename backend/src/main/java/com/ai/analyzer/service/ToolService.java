package com.ai.analyzer.service;

import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.Repository;
import com.ai.analyzer.parser.CodeParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolService {

    private final GitService gitService;
    private final CodeParserService codeParserService;

    public Map<String, Object> executeTool(String toolName, Map<String, Object> params, String repoId) {
        try {
            return switch (toolName) {
                case "read_file" -> readFile(params, repoId);
                case "search_files" -> searchFiles(params, repoId);
                case "list_files" -> listFiles(params, repoId);
                case "search_code" -> searchCode(params, repoId);
                default -> Map.of(
                    "success", false,
                    "error", "Unknown tool: " + toolName
                );
            };
        } catch (Exception e) {
            log.error("Failed to execute tool: {}", toolName, e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    private Map<String, Object> readFile(Map<String, Object> params, String repoId) throws IOException {
        String filePath = (String) params.get("file_path");
        if (filePath == null) {
            return Map.of("success", false, "error", "file_path is required");
        }

        Repository repository = gitService.getRepository(repoId);
        if (repository == null) {
            return Map.of("success", false, "error", "Repository not found");
        }

        String content = codeParserService.readFile(repository.getLocalPath(), filePath);
        return Map.of(
            "success", true,
            "file_path", filePath,
            "content", content
        );
    }

    private Map<String, Object> searchFiles(Map<String, Object> params, String repoId) throws IOException {
        String className = (String) params.get("class_name");
        if (className == null) {
            return Map.of("success", false, "error", "class_name is required");
        }

        Repository repository = gitService.getRepository(repoId);
        if (repository == null) {
            return Map.of("success", false, "error", "Repository not found");
        }

        List<String> files = codeParserService.searchFilesByClassName(repository.getLocalPath(), className);
        return Map.of(
            "success", true,
            "files", files
        );
    }

    private Map<String, Object> listFiles(Map<String, Object> params, String repoId) throws IOException {
        Repository repository = gitService.getRepository(repoId);
        if (repository == null) {
            return Map.of("success", false, "error", "Repository not found");
        }

        List<String> files = codeParserService.listFiles(repository.getLocalPath());
        int limit = params.containsKey("limit") ? (int) params.get("limit") : 50;
        List<String> limitedFiles = files.stream().limit(limit).toList();
        
        return Map.of(
            "success", true,
            "files", limitedFiles,
            "total_count", files.size()
        );
    }

    private Map<String, Object> searchCode(Map<String, Object> params, String repoId) {
        String query = (String) params.get("query");
        if (query == null) {
            return Map.of("success", false, "error", "query is required");
        }

        return Map.of(
            "success", true,
            "message", "Code search functionality",
            "query", query
        );
    }

    public String getAvailableTools() {
        return """
            可用工具:
            1. read_file - 读取文件内容
               参数: file_path (文件路径)
            
            2. search_files - 搜索文件
               参数: class_name (类名)
            
            3. list_files - 列出仓库文件
               参数: limit (可选，返回文件数量限制)
            
            4. search_code - 搜索代码
               参数: query (搜索关键词)
            """;
    }
}
