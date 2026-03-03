package com.ai.analyzer.service;

import com.ai.analyzer.config.AnalyzerProperties;
import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.CodeChunk;
import com.ai.analyzer.model.Repository;
import com.ai.analyzer.model.dto.ErrorStackAnalysisResponse;
import com.ai.analyzer.model.dto.SuspectedLocation;
import com.ai.analyzer.embedding.VectorStoreService;
import com.ai.analyzer.parser.CodeParserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class ChatService {

    private final GitService gitService;
    private final VectorStoreService vectorStoreService;
    private final CodeParserService codeParserService;
    private final RepositoryAnalysisService analysisService;
    private final ChatMemoryService chatMemoryService;
    private final ToolService toolService;
    private final AnalyzerProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ChatService(GitService gitService, VectorStoreService vectorStoreService, CodeParserService codeParserService, RepositoryAnalysisService analysisService, ChatMemoryService chatMemoryService, ToolService toolService, AnalyzerProperties properties) {
        this.gitService = gitService;
        this.vectorStoreService = vectorStoreService;
        this.codeParserService = codeParserService;
        this.analysisService = analysisService;
        this.chatMemoryService = chatMemoryService;
        this.toolService = toolService;
        this.properties = properties;
        this.restTemplate = createRestTemplateWithTimeout();
        this.objectMapper = new ObjectMapper();
    }

    private RestTemplate createRestTemplateWithTimeout() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }

    public String chat(String repositoryId, String message) {
        Repository repository = gitService.getRepository(repositoryId);
        if (repository == null) {
            return "未找到仓库，请先选择一个仓库。";
        }

        chatMemoryService.addMessage(repositoryId, "user", message);

        String apiKey = properties.getDashscope().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("No API Key configured, using fallback response");
            
            String response;
            if (isErrorStack(message)) {
                response = analyzeErrorStack(repositoryId, message);
            } else {
                response = useFallbackResponse(repository, message);
            }
            
            chatMemoryService.addMessage(repositoryId, "assistant", response);
            return response;
        }

        log.info("API Key found, calling DashScope API...");
        try {
            String response = callDashscopeAPI(repository, message);
            chatMemoryService.addMessage(repositoryId, "assistant", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to call DashScope API: " + e.getMessage(), e);
            
            String response;
            if (isErrorStack(message)) {
                response = "⚠️ 调用百炼 API 失败: " + e.getMessage() + "\n\n已自动切换到基础分析模式。\n\n" + analyzeErrorStack(repositoryId, message);
            } else {
                response = "⚠️ 调用百炼 API 失败: " + e.getMessage() + "\n\n已自动切换到预设回答模式。\n\n" + useFallbackResponse(repository, message);
            }
            
            chatMemoryService.addMessage(repositoryId, "assistant", response);
            return response;
        }
    }
    
    private boolean isErrorStack(String message) {
        if (message.length() < 50) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("exception") || lowerMessage.contains("error") || 
            lowerMessage.contains("stack trace") || lowerMessage.contains("at ")) {
            return true;
        }
        
        Pattern stackPattern = Pattern.compile("at\\s+[\\w.$]+\\.([\\w$]+)\\(");
        Matcher matcher = stackPattern.matcher(message);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
            if (matchCount >= 2) {
                return true;
            }
        }
        
        return false;
    }
    
    private String analyzeErrorStack(String repositoryId, String errorStack) {
        ErrorStackAnalysisResponse analysis = analysisService.analyzeErrorStack(repositoryId, errorStack);
        
        StringBuilder response = new StringBuilder();
        response.append("## 🐛 错误堆栈分析结果\n\n");
        
        response.append("### 错误类型\n");
        response.append("`").append(analysis.getErrorType()).append("`\n\n");
        
        response.append("### 根本原因\n");
        response.append(analysis.getRootCause()).append("\n\n");
        
        response.append("### 摘要\n");
        response.append(analysis.getSummary()).append("\n\n");
        
        if (!analysis.getSuspectedLocations().isEmpty()) {
            response.append("### 可疑位置\n");
            for (int i = 0; i < analysis.getSuspectedLocations().size(); i++) {
                SuspectedLocation location = analysis.getSuspectedLocations().get(i);
                response.append(String.format("%d. **%s.%s**\n", i + 1, location.getClassName(), location.getMethodName()));
                
                if (location.getFilePath() != null && location.getFilePath().contains("/")) {
                    response.append(String.format("   - 📄 文件: `%s:%d`\n", 
                            location.getFilePath(), 
                            location.getLineNumber()));
                } else {
                    response.append(String.format("   - 📄 文件: `%s:%d`\n", location.getFilePath(), location.getLineNumber()));
                }
                
                response.append(String.format("   - 🎯 置信度: %.0f%%\n", location.getConfidence() * 100));
                if (location.getDescription() != null && !location.getDescription().isEmpty()) {
                    response.append(String.format("   - %s\n", location.getDescription()));
                }
                response.append("\n");
            }
        }
        
        if (!analysis.getPossibleFixes().isEmpty()) {
            response.append("### 可能的修复方案\n");
            for (int i = 0; i < analysis.getPossibleFixes().size(); i++) {
                response.append(String.format("%d. %s\n", i + 1, analysis.getPossibleFixes().get(i)));
            }
            response.append("\n");
        }
        
        if (!analysis.getRelatedCode().isEmpty()) {
            response.append("### 相关代码\n");
            for (int i = 0; i < analysis.getRelatedCode().size() && i < 3; i++) {
                com.ai.analyzer.model.dto.RelatedCode code = analysis.getRelatedCode().get(i);
                response.append(String.format("#### %d. %s\n", i + 1, code.getFilePath()));
                response.append("```\n").append(code.getCodeSnippet()).append("\n```\n\n");
            }
            if (analysis.getRelatedCode().size() > 3) {
                response.append(String.format("... 还有 %d 个相关代码片段\n\n", analysis.getRelatedCode().size() - 3));
            }
        }
        
        response.append("💡 提示：你可以在\"错误分析\"页面查看更详细的分析结果和相关代码！");
        
        return response.toString();
    }

    private String callDashscopeAPI(Repository repository, String message) throws Exception {
        String baseUrl = properties.getDashscope().getBaseUrl();
        String model = properties.getDashscope().getModel();
        String apiKey = properties.getDashscope().getApiKey();

        log.info("Calling DashScope API at: {}", baseUrl);
        log.info("Using model: {}", model);

        String prompt = buildPrompt(repository, message);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的代码分析助手。请用中文回答用户关于代码和项目架构的问题。");
        messages.add(systemMessage);
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/chat/completions",
                entity,
                String.class
        );

        log.info("DashScope API response received successfully");

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode messageNode = choices.get(0).path("message");
            return messageNode.path("content").asText();
        }

        return "未能获取到 AI 回复。";
    }

    private String buildPrompt(Repository repository, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的代码分析助手，擅长理解和分析代码库。\n\n");
        
        String conversationHistory = chatMemoryService.buildConversationContext(repository.getId());
        if (!conversationHistory.isEmpty()) {
            prompt.append(conversationHistory).append("\n\n");
        }
        
        prompt.append("## 可用工具\n\n");
        prompt.append("如果需要，你可以要求使用以下工具。请用 JSON 格式描述工具调用：\n");
        prompt.append(toolService.getAvailableTools()).append("\n\n");
        
        prompt.append("项目信息：\n");
        prompt.append("- 项目名称: ").append(repository.getName()).append("\n");
        prompt.append("- 项目地址: ").append(repository.getUrl()).append("\n");
        prompt.append("- 本地路径: ").append(repository.getLocalPath()).append("\n\n");
        
        prompt.append("## 回答要求\n\n");
        prompt.append("1. 用中文回答\n");
        prompt.append("2. 回答要专业、详细\n");
        prompt.append("3. 如果需要查看更多代码或文件，可以要求使用工具\n");
        prompt.append("4. 结合项目上下文进行分析\n\n");
        
        boolean isErrorStack = isErrorStack(userMessage);
        
        try {
            List<String> allFiles = codeParserService.listFiles(repository.getLocalPath());
            
            if (isErrorStack) {
                prompt.append("🔍 检测到错误堆栈，正在进行深度分析...\n\n");
                
                ErrorStackAnalysisResponse analysis = analysisService.analyzeErrorStack(repository.getId(), userMessage);
                
                prompt.append("## 错误分析结果\n\n");
                prompt.append("### 错误类型\n");
                prompt.append(analysis.getErrorType()).append("\n\n");
                prompt.append("### 根本原因\n");
                prompt.append(analysis.getRootCause()).append("\n\n");
                prompt.append("### 摘要\n");
                prompt.append(analysis.getSummary()).append("\n\n");
                
                if (!analysis.getSuspectedLocations().isEmpty()) {
                    prompt.append("### 可疑位置\n");
                    for (int i = 0; i < analysis.getSuspectedLocations().size(); i++) {
                        SuspectedLocation location = analysis.getSuspectedLocations().get(i);
                        prompt.append(String.format("%d. **%s.%s**\n", i + 1, location.getClassName(), location.getMethodName()));
                        prompt.append(String.format("   - 文件: `%s:%d`\n", location.getFilePath(), location.getLineNumber()));
                        prompt.append(String.format("   - 置信度: %.0f%%\n", location.getConfidence() * 100));
                        prompt.append("\n");
                        
                        if (location.getFilePath() != null && location.getFilePath().contains("/")) {
                            try {
                                String fileContent = codeParserService.readFile(repository.getLocalPath(), location.getFilePath());
                                if (fileContent.length() > 15000) {
                                    fileContent = fileContent.substring(0, 15000) + "\n... (文件内容已截断)";
                                }
                                prompt.append(String.format("   - 文件内容:\n```\n%s\n```\n\n", fileContent));
                            } catch (Exception e) {
                                log.warn("Failed to read full file for location: {}", location.getFilePath(), e);
                            }
                        }
                    }
                }
                
                if (!analysis.getRelatedCode().isEmpty()) {
                    prompt.append("### 相关代码\n");
                    for (int i = 0; i < analysis.getRelatedCode().size() && i < 5; i++) {
                        com.ai.analyzer.model.dto.RelatedCode code = analysis.getRelatedCode().get(i);
                        prompt.append(String.format("#### %d. %s\n", i + 1, code.getFilePath()));
                        prompt.append("```\n").append(code.getCodeSnippet()).append("\n```\n\n");
                    }
                }
                
                prompt.append("## 你的任务\n\n");
                prompt.append("请基于以上错误分析结果，进行深度分析并提供：\n");
                prompt.append("1. 详细解释错误发生的原因\n");
                prompt.append("2. 分析相关代码，指出问题所在\n");
                prompt.append("3. 提供具体的修复建议和代码示例\n");
                prompt.append("4. 说明如何预防类似问题\n");
                prompt.append("\n请用中文回答，保持专业和详细。\n\n");
                
            } else {
                String className = extractClassName(userMessage);
                if (className != null) {
                    prompt.append("🔍 检测到类名查询: ").append(className).append("\n\n");
                    
                    List<String> matchedFiles = new ArrayList<>();
                    try {
                        matchedFiles = codeParserService.searchFilesByClassName(
                                repository.getLocalPath(),
                                className
                        );
                        
                        if (matchedFiles.isEmpty()) {
                            log.info("No match by simple class name, trying full class path match");
                            matchedFiles = findFilesByFullClassName(allFiles, className);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to search files by class name", e);
                    }
                    
                    if (!matchedFiles.isEmpty()) {
                        prompt.append("找到 ").append(matchedFiles.size()).append(" 个相关文件:\n");
                        for (int i = 0; i < matchedFiles.size() && i < 3; i++) {
                            String matchedFile = matchedFiles.get(i);
                            prompt.append("  ").append(i + 1).append(". ").append(matchedFile).append("\n");
                            try {
                                String fileContent = codeParserService.readFile(repository.getLocalPath(), matchedFile);
                                prompt.append("文件内容:\n");
                                if (fileContent.length() > 15000) {
                                    fileContent = fileContent.substring(0, 15000) + "\n... (文件内容已截断)";
                                }
                                prompt.append("```\n").append(fileContent).append("\n```\n\n");
                            } catch (IOException e) {
                                log.warn("Failed to read matched file: {}", matchedFile, e);
                            }
                        }
                        if (matchedFiles.size() > 3) {
                            prompt.append("... 还有 ").append(matchedFiles.size() - 3).append(" 个文件\n\n");
                        }
                    } else {
                        prompt.append("⚠️ 未找到名为 \"").append(className).append("\" 的类文件。\n\n");
                        prompt.append("项目文件列表 (共 ").append(allFiles.size()).append(" 个文件):\n");
                        List<String> keyFiles = findKeyFiles(allFiles);
                        for (String file : keyFiles) {
                            prompt.append("  - ").append(file).append("\n");
                        }
                        if (allFiles.size() > keyFiles.size()) {
                            prompt.append("  ... 还有 ").append(allFiles.size() - keyFiles.size()).append(" 个文件\n");
                        }
                    }
                } else {
                    prompt.append("项目文件列表 (共 ").append(allFiles.size()).append(" 个文件):\n");
                    
                    List<String> keyFiles = findKeyFiles(allFiles);
                    for (String file : keyFiles) {
                        prompt.append("  - ").append(file).append("\n");
                    }
                    
                    if (allFiles.size() > keyFiles.size()) {
                        prompt.append("  ... 还有 ").append(allFiles.size() - keyFiles.size()).append(" 个文件\n");
                    }
                    prompt.append("\n");
                    
                    String readmeContent = readReadmeFile(repository.getLocalPath());
                    if (readmeContent != null) {
                        prompt.append("README 内容:\n");
                        prompt.append(readmeContent).append("\n\n");
                    }
                    
                    List<String> sampleFiles = selectSampleFiles(allFiles, userMessage);
                    for (String sampleFile : sampleFiles) {
                        try {
                            String fileContent = codeParserService.readFile(repository.getLocalPath(), sampleFile);
                            prompt.append("文件: ").append(sampleFile).append("\n");
                            if (fileContent.length() > 1000) {
                                fileContent = fileContent.substring(0, 1000) + "\n... (文件内容已截断)";
                            }
                            prompt.append("```\n").append(fileContent).append("\n```\n\n");
                        } catch (IOException e) {
                            log.warn("Failed to read sample file: {}", sampleFile, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to list repository files", e);
            prompt.append("无法读取项目文件信息。\n\n");
        }
        
        if (!isErrorStack) {
            prompt.append("用户问题: ").append(userMessage).append("\n\n");
            
            List<CodeChunk> relatedChunks = vectorStoreService.search(userMessage, repository.getId(), 3);
            if (!relatedChunks.isEmpty()) {
                prompt.append("相关代码片段:\n");
                for (int i = 0; i < relatedChunks.size(); i++) {
                    CodeChunk chunk = relatedChunks.get(i);
                    prompt.append("\n--- 片段 ").append(i + 1).append(":\n");
                    if (chunk.getFilePath() != null) {
                        prompt.append("文件: ").append(chunk.getFilePath()).append("\n");
                    }
                    if (chunk.getContent() != null) {
                        String content = chunk.getContent();
                        if (content.length() > 500) {
                            content = content.substring(0, 500) + "...";
                        }
                        prompt.append("内容:\n").append(content).append("\n");
                    }
                }
            }
            
            prompt.append("\n请根据以上信息回答用户的问题。");
        }
        
        return prompt.toString();
    }

    private String extractClassName(String userMessage) {
        log.info("Extracting class name from message: {}", userMessage);
        
        String[] patterns = {
            "分析下这个类的源码(.+)",
            "分析(.+)类的源码",
            "分析(.+)的源码",
            "分析(.+)类",
            "查看(.+)类",
            "查找(.+)类",
            "(.+)的源码",
            "类(.+)"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(userMessage);
            if (matcher.find()) {
                String candidate = matcher.group(1).trim();
                log.info("Pattern '{}' matched, candidate: {}", pattern, candidate);
                candidate = cleanClassName(candidate);
                if (!candidate.isEmpty() && candidate.length() > 1) {
                    log.info("Extracted class name from pattern: {}", candidate);
                    return candidate;
                }
            }
        }
        
        String[] words = userMessage.split("[\\s，。、；：,.!！?？]+");
        for (String word : words) {
            log.info("Checking word: {}", word);
            String cleaned = cleanClassName(word);
            log.info("Cleaned word: {}", cleaned);
            if (cleaned.length() >= 3 && Character.isUpperCase(cleaned.charAt(0))) {
                boolean hasLower = false;
                for (int i = 1; i < cleaned.length(); i++) {
                    if (Character.isLowerCase(cleaned.charAt(i))) {
                        hasLower = true;
                        break;
                    }
                }
                if (hasLower) {
                    log.info("Extracted class name from word: {}", cleaned);
                    return cleaned;
                }
            }
        }
        
        log.info("No class name extracted from message");
        return null;
    }
    
    private String cleanClassName(String candidate) {
        candidate = candidate.trim();
        candidate = candidate.replaceAll("^[的是下看]", "").trim();
        candidate = candidate.replaceAll("[的类源码]$", "").trim();
        
        if (candidate.toLowerCase().endsWith(".java")) {
            candidate = candidate.substring(0, candidate.length() - 5);
        } else if (candidate.toLowerCase().endsWith(".py")) {
            candidate = candidate.substring(0, candidate.length() - 3);
        } else if (candidate.toLowerCase().endsWith(".js") || candidate.toLowerCase().endsWith(".ts")) {
            candidate = candidate.substring(0, candidate.length() - 3);
        }
        
        return candidate.trim();
    }
    
    private List<String> findKeyFiles(List<String> allFiles) {
        List<String> keyFiles = new ArrayList<>();
        Set<String> keyPatterns = Set.of(
                "README", "readme", "pom.xml", "build.gradle", "package.json", 
                "requirements.txt", "go.mod", "Cargo.toml", "setup.py", 
                "main", "app", "application", "index"
        );
        
        for (String file : allFiles) {
            String fileName = file.toLowerCase();
            for (String pattern : keyPatterns) {
                if (fileName.contains(pattern)) {
                    keyFiles.add(file);
                    break;
                }
            }
            if (keyFiles.size() >= 10) break;
        }
        
        return keyFiles;
    }
    
    private String readReadmeFile(String localPath) {
        Path repoPath = Paths.get(localPath);
        List<String> possibleReadmes = Arrays.asList(
                "README.md", "README.txt", "README", 
                "readme.md", "readme.txt", "readme"
        );
        
        for (String readmeName : possibleReadmes) {
            Path readmePath = repoPath.resolve(readmeName);
            if (Files.exists(readmePath)) {
                try {
                    String content = Files.readString(readmePath);
                    if (content.length() > 2000) {
                        content = content.substring(0, 2000) + "\n... (README 已截断)";
                    }
                    return content;
                } catch (IOException e) {
                    log.warn("Failed to read README file: {}", readmePath, e);
                }
            }
        }
        return null;
    }
    
    private List<String> selectSampleFiles(List<String> allFiles, String userMessage) {
        List<String> samples = new ArrayList<>();
        String lowerMessage = userMessage.toLowerCase();
        
        for (String file : allFiles) {
            String fileName = file.toLowerCase();
            
            if (lowerMessage.contains("架构") || lowerMessage.contains("architecture") || lowerMessage.contains("结构")) {
                if (fileName.contains("pom") || fileName.contains("gradle") || 
                    fileName.contains("package") || fileName.contains("requirement")) {
                    samples.add(file);
                }
            } else if (lowerMessage.contains("代码") || lowerMessage.contains("code")) {
                if (fileName.endsWith(".java") || fileName.endsWith(".py") || 
                    fileName.endsWith(".js") || fileName.endsWith(".ts")) {
                    samples.add(file);
                }
            } else if (lowerMessage.contains("文件") || lowerMessage.contains("file")) {
                samples.add(file);
            }
            
            if (samples.size() >= 3) break;
        }
        
        if (samples.isEmpty()) {
            for (String file : allFiles) {
                if (file.endsWith(".java") || file.endsWith(".py") || 
                    file.endsWith(".js") || file.endsWith(".ts") || 
                    file.endsWith(".md")) {
                    samples.add(file);
                    if (samples.size() >= 3) break;
                }
            }
        }
        
        return samples;
    }

    private String useFallbackResponse(Repository repository, String message) {
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("架构") || lowerMessage.contains("architecture") || lowerMessage.contains("结构")) {
            return generateArchitectureResponse(repository);
        } else if (lowerMessage.contains("文件") || lowerMessage.contains("file")) {
            return generateFileListResponse(repository);
        } else if (lowerMessage.contains("代码") || lowerMessage.contains("code")) {
            return generateCodeSearchResponse(repository, message);
        } else if (lowerMessage.contains("你好") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "你好！我是 AI 代码分析助手。我可以帮你分析项目架构、查找代码文件、解释代码功能。\n\n注意：当前未配置百炼 API Key，使用的是预设回答。如需真实的 LLM 回答，请配置 DASHSCOPE_API_KEY 环境变量。";
        } else {
            return "我理解你的问题了！这是一个演示版本，完整的大模型集成需要配置百炼 API Key。\n\n你可以尝试问我：\n- 这个项目的架构是什么？\n- 有哪些文件？\n- 帮我查找相关代码\n\n如需真实的 LLM 回答，请配置 DASHSCOPE_API_KEY 环境变量。";
        }
    }

    private String generateArchitectureResponse(Repository repository) {
        return String.format("""
            ## %s 项目架构分析
            
            📁 **项目概览**
            - 项目名称: %s
            - 仓库地址: %s
            - 克隆时间: %s
            
            🏗️ **推测架构**
            基于仓库信息，这可能是一个 %s 类型的项目。
            
            💡 **提示**
            在完整版本中（配置百炼 API Key 后），我会：
            1. 分析项目的目录结构
            2. 识别主要模块和依赖
            3. 绘制架构图
            4. 分析设计模式
            
            你可以点击"架构分析"页面查看更详细的分析！
            """, 
            repository.getName(),
            repository.getName(),
            repository.getUrl(),
            repository.getClonedAt().toString(),
            detectProjectType(repository)
        );
    }

    private String generateFileListResponse(Repository repository) {
        return String.format("""
            ## %s 文件列表
            
            📂 **项目文件**
            仓库已克隆到: %s
            
            💡 **提示**
            在完整版本中（配置百炼 API Key 后），我会：
            1. 列出所有源代码文件
            2. 按类型分类（Java、Python、JS 等）
            3. 显示文件大小和修改时间
            4. 识别关键文件
            
            你可以在"源码走读"页面浏览和分析具体文件！
            """,
            repository.getName(),
            repository.getLocalPath()
        );
    }

    private String generateCodeSearchResponse(Repository repository, String query) {
        List<CodeChunk> chunks = vectorStoreService.search(query, repository.getId(), 3);
        
        if (chunks.isEmpty()) {
            return String.format("""
                ## 代码搜索结果
                
                🔍 搜索: "%s"
                
                未找到相关代码片段。
                
                💡 **提示**
                在完整版本中（配置百炼 API Key 后），我会：
                1. 使用语义搜索找到相关代码
                2. 显示代码上下文
                3. 解释代码功能
                4. 提供使用示例
                """,
                query
            );
        }
        
        StringBuilder response = new StringBuilder();
        response.append(String.format("## 代码搜索结果\n\n🔍 搜索: \"%s\"\n\n", query));
        
        for (int i = 0; i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);
            response.append(String.format("### %d. %s\n", i + 1, chunk.getFilePath() != null ? chunk.getFilePath() : "未知文件"));
            if (chunk.getContent() != null) {
                String contentPreview = chunk.getContent().length() > 200 
                    ? chunk.getContent().substring(0, 200) + "..." 
                    : chunk.getContent();
                response.append("```\n").append(contentPreview).append("\n```\n\n");
            }
        }
        
        response.append("\n💡 **提示**\n配置百炼 API Key 后，我会提供更智能的代码分析！");
        
        return response.toString();
    }

    private String detectProjectType(Repository repository) {
        String url = repository.getUrl().toLowerCase();
        if (url.contains("spring") || url.contains("java")) {
            return "Java/Spring Boot";
        } else if (url.contains("python") || url.contains("django") || url.contains("flask")) {
            return "Python Web";
        } else if (url.contains("react") || url.contains("vue") || url.contains("javascript")) {
            return "Frontend";
        } else {
            return "通用";
        }
    }
    
    private List<String> findFilesByFullClassName(List<String> allFiles, String className) {
        List<String> matchedFiles = new ArrayList<>();
        
        String packagePath = className.replace('.', '/') + ".java";
        log.info("Checking package path: {}", packagePath);
        
        for (String file : allFiles) {
            if (file.endsWith(packagePath)) {
                matchedFiles.add(file);
            }
        }
        
        if (matchedFiles.isEmpty()) {
            String simpleFileName = className + ".java";
            for (String file : allFiles) {
                if (file.endsWith("/" + simpleFileName) || file.equals(simpleFileName)) {
                    matchedFiles.add(file);
                }
            }
        }
        
        if (matchedFiles.isEmpty()) {
            String lowerClassName = className.toLowerCase();
            for (String file : allFiles) {
                String fileName = file.substring(file.lastIndexOf('/') + 1).toLowerCase();
                if (fileName.contains(lowerClassName) && fileName.endsWith(".java")) {
                    matchedFiles.add(file);
                }
            }
        }
        
        log.info("Found {} files for class {} via full class name match", matchedFiles.size(), className);
        return matchedFiles;
    }
}
