package com.ai.analyzer.service;

import com.ai.analyzer.config.AnalyzerProperties;
import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.CodeChunk;
import com.ai.analyzer.model.Repository;
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
import java.util.stream.Stream;

@Slf4j
@Service
public class ChatService {

    private final GitService gitService;
    private final VectorStoreService vectorStoreService;
    private final CodeParserService codeParserService;
    private final AnalyzerProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ChatService(GitService gitService, VectorStoreService vectorStoreService, CodeParserService codeParserService, AnalyzerProperties properties) {
        this.gitService = gitService;
        this.vectorStoreService = vectorStoreService;
        this.codeParserService = codeParserService;
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

        String apiKey = properties.getDashscope().getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.info("No API Key configured, using fallback response");
            return useFallbackResponse(repository, message);
        }

        log.info("API Key found, calling DashScope API...");
        try {
            return callDashscopeAPI(repository, message);
        } catch (Exception e) {
            log.error("Failed to call DashScope API: " + e.getMessage(), e);
            return "⚠️ 调用百炼 API 失败: " + e.getMessage() + "\n\n已自动切换到预设回答模式。\n\n" + useFallbackResponse(repository, message);
        }
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
        prompt.append("项目信息：\n");
        prompt.append("- 项目名称: ").append(repository.getName()).append("\n");
        prompt.append("- 项目地址: ").append(repository.getUrl()).append("\n");
        prompt.append("- 本地路径: ").append(repository.getLocalPath()).append("\n\n");
        
        try {
            List<String> allFiles = codeParserService.listFiles(repository.getLocalPath());
            
            String className = extractClassName(userMessage);
            if (className != null) {
                prompt.append("🔍 检测到类名查询: ").append(className).append("\n\n");
                List<String> matchedFiles = codeParserService.searchFilesByClassName(repository.getLocalPath(), className);
                if (!matchedFiles.isEmpty()) {
                    prompt.append("找到 ").append(matchedFiles.size()).append(" 个相关文件:\n");
                    for (int i = 0; i < matchedFiles.size(); i++) {
                        String matchedFile = matchedFiles.get(i);
                        prompt.append("  ").append(i + 1).append(". ").append(matchedFile).append("\n");
                        try {
                            String fileContent = codeParserService.readFile(repository.getLocalPath(), matchedFile);
                            prompt.append("文件内容:\n");
                            if (fileContent.length() > 8000) {
                                fileContent = fileContent.substring(0, 8000) + "\n... (文件内容已截断，如需查看完整内容请指定具体问题)";
                            }
                            prompt.append("```\n").append(fileContent).append("\n```\n\n");
                        } catch (IOException e) {
                            log.warn("Failed to read matched file: {}", matchedFile, e);
                        }
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
        } catch (IOException e) {
            log.warn("Failed to list repository files", e);
            prompt.append("无法读取项目文件信息。\n\n");
        }
        
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
        return prompt.toString();
    }

    private String extractClassName(String userMessage) {
        String[] patterns = {
            "分析下这个类的源码(.+)",
            "分析(.+)类的源码",
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
                candidate = candidate.replaceAll("^[的是下看]", "").trim();
                candidate = candidate.replaceAll("[的类源码]$", "").trim();
                if (!candidate.isEmpty() && candidate.length() > 1) {
                    return candidate;
                }
            }
        }
        
        String[] words = userMessage.split("[\\s，。、；：,.!！?？]+");
        for (String word : words) {
            if (word.length() >= 3 && Character.isUpperCase(word.charAt(0))) {
                boolean hasLower = false;
                for (int i = 1; i < word.length(); i++) {
                    if (Character.isLowerCase(word.charAt(i))) {
                        hasLower = true;
                        break;
                    }
                }
                if (hasLower) {
                    return word;
                }
            }
        }
        
        return null;
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
}
