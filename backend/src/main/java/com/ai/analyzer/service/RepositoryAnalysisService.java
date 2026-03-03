package com.ai.analyzer.service;

import com.ai.analyzer.embedding.VectorStoreService;
import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.AnalysisStatus;
import com.ai.analyzer.model.CodeChunk;
import com.ai.analyzer.model.Repository;
import com.ai.analyzer.model.dto.*;
import com.ai.analyzer.parser.CodeParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final GitService gitService;
    private final CodeParserService codeParserService;
    private final VectorStoreService vectorStoreService;

    public Repository cloneAndAnalyze(String url, String branch) {
        Repository repository = gitService.cloneRepository(url, branch);
        
        try {
            repository.setStatus(AnalysisStatus.PARSING);
            List<CodeChunk> chunks = codeParserService.parseRepository(repository.getId(), repository.getLocalPath());
            
            repository.setStatus(AnalysisStatus.EMBEDDING);
            vectorStoreService.addCodeChunks(chunks);
            
            repository.setStatus(AnalysisStatus.COMPLETED);
            repository.setLastAnalyzedAt(LocalDateTime.now());
            
            log.info("Successfully analyzed repository: {}", repository.getId());
            return repository;
        } catch (Exception e) {
            log.error("Failed to analyze repository: {}", repository.getId(), e);
            repository.setStatus(AnalysisStatus.FAILED);
            throw new RuntimeException("Failed to analyze repository", e);
        }
    }

    public void analyzeExistingRepository(Repository repository) {
        if (repository.getStatus() == AnalysisStatus.COMPLETED && 
            repository.getLastAnalyzedAt() != null) {
            log.info("Repository already analyzed: {}", repository.getId());
            return;
        }
        
        try {
            log.info("Analyzing existing repository: {} ({})", repository.getName(), repository.getId());
            repository.setStatus(AnalysisStatus.PARSING);
            List<CodeChunk> chunks = codeParserService.parseRepository(repository.getId(), repository.getLocalPath());
            
            repository.setStatus(AnalysisStatus.EMBEDDING);
            vectorStoreService.addCodeChunks(chunks);
            
            repository.setStatus(AnalysisStatus.COMPLETED);
            repository.setLastAnalyzedAt(LocalDateTime.now());
            
            log.info("Successfully analyzed existing repository: {}, added {} chunks", repository.getId(), chunks.size());
        } catch (Exception e) {
            log.error("Failed to analyze existing repository: {}", repository.getId(), e);
            repository.setStatus(AnalysisStatus.FAILED);
        }
    }

    public ArchitectureAnalysisResponse analyzeArchitecture(String repoId) {
        Repository repository = gitService.getRepository(repoId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repoId);
        }

        List<String> files;
        try {
            files = codeParserService.listFiles(repository.getLocalPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list repository files", e);
        }

        Map<String, List<String>> moduleFiles = groupFilesByModule(files);
        List<String> mainModules = new ArrayList<>(moduleFiles.keySet());
        Map<String, String> moduleDescriptions = generateModuleDescriptions(moduleFiles);
        List<String> keyFiles = findKeyFiles(files);
        List<String> designPatterns = detectDesignPatterns(repoId);
        String techStack = detectTechStack(files);

        return ArchitectureAnalysisResponse.builder()
                .repositoryId(repoId)
                .overallStructure(generateOverallStructure(mainModules))
                .mainModules(mainModules)
                .moduleDescriptions(moduleDescriptions)
                .designPatterns(designPatterns)
                .keyFiles(keyFiles)
                .techStack(techStack)
                .recommendations(generateRecommendations(mainModules))
                .build();
    }

    public CodeWalkthroughResponse walkthroughCode(String repoId, String filePath) {
        Repository repository = gitService.getRepository(repoId);
        if (repository == null) {
            throw new IllegalArgumentException("Repository not found: " + repoId);
        }

        String content;
        try {
            content = codeParserService.readFile(repository.getLocalPath(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }

        List<CodeSection> sections = splitIntoSections(content, filePath);
        List<String> dependencies = extractDependencies(content, filePath);
        List<String> dependents = findDependents(repoId, filePath);

        return CodeWalkthroughResponse.builder()
                .repositoryId(repoId)
                .filePath(filePath)
                .fileSummary(generateFileSummary(content, filePath))
                .sections(sections)
                .dependencies(dependencies)
                .dependents(dependents)
                .build();
    }

    public ErrorStackAnalysisResponse analyzeErrorStack(String repoId, String errorStack) {
        log.info("Analyzing error stack, repoId: {}", repoId);
        List<SuspectedLocation> locations = parseErrorStack(errorStack);
        log.info("Found {} suspected locations", locations.size());
        
        List<RelatedCode> relatedCode = new ArrayList<>();
        if (repoId != null) {
            Repository repository = gitService.getRepository(repoId);
            if (repository != null) {
                log.info("Repository found: {}", repository.getName());
                
                try {
                    List<String> allFiles = codeParserService.listFiles(repository.getLocalPath());
                    log.info("Total files in repository: {}", allFiles.size());
                    
                    for (SuspectedLocation location : locations) {
                        try {
                            String fullClassName = location.getClassName();
                            String simpleClassName = extractSimpleClassName(fullClassName);
                            log.info("Searching for class: {} (full: {})", simpleClassName, fullClassName);
                            
                            String matchedFile = findFileByFullClassName(allFiles, fullClassName, simpleClassName);
                            
                            if (matchedFile != null) {
                                location.setFilePath(matchedFile);
                                log.info("Found exact match for {}: {}", fullClassName, matchedFile);
                                
                                try {
                                    String fileContent = codeParserService.readFile(repository.getLocalPath(), matchedFile);
                                    String relevantSnippet = extractRelevantSnippet(fileContent, location.getMethodName(), location.getLineNumber());
                                    relatedCode.add(RelatedCode.builder()
                                            .filePath(matchedFile)
                                            .codeSnippet(relevantSnippet)
                                            .relevance("High")
                                            .build());
                                    location.setConfidence(0.95);
                                    log.info("Added related code for: {}", matchedFile);
                                } catch (Exception e) {
                                    log.warn("Failed to read file: {}", matchedFile, e);
                                }
                            } else {
                                log.info("No exact match, falling back to search by simple class name");
                                List<String> matchedFiles = codeParserService.searchFilesByClassName(
                                        repository.getLocalPath(),
                                        simpleClassName
                                );
                                if (!matchedFiles.isEmpty()) {
                                    String matchedFile2 = matchedFiles.get(0);
                                    location.setFilePath(matchedFile2);
                                    log.info("Found match by simple name: {}", matchedFile2);
                                    
                                    try {
                                        String fileContent = codeParserService.readFile(repository.getLocalPath(), matchedFile2);
                                        String relevantSnippet = extractRelevantSnippet(fileContent, location.getMethodName(), location.getLineNumber());
                                        relatedCode.add(RelatedCode.builder()
                                                .filePath(matchedFile2)
                                                .codeSnippet(relevantSnippet)
                                                .relevance("High")
                                                .build());
                                        location.setConfidence(0.85);
                                    } catch (Exception e) {
                                        log.warn("Failed to read file: {}", matchedFile2, e);
                                    }
                                } else {
                                    List<CodeChunk> relatedChunks = vectorStoreService.search(
                                            location.getClassName() + " " + location.getMethodName(),
                                            repoId,
                                            3
                                    );
                                    if (!relatedChunks.isEmpty()) {
                                        location.setConfidence(0.7);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Failed to process location: {}", location.getClassName(), e);
                        }
                    }

                if (relatedCode.isEmpty()) {
                    log.info("No direct matches found, searching vector store...");
                    List<CodeChunk> chunks = vectorStoreService.search(errorStack, repoId, 10);
                    for (CodeChunk chunk : chunks) {
                        relatedCode.add(RelatedCode.builder()
                                .filePath(chunk.getFilePath())
                                .codeSnippet(chunk.getContent())
                                .relevance("Medium")
                                .build());
                    }
                    log.info("Found {} related chunks from vector store", chunks.size());
                }
            } else {
                log.warn("Repository not found for id: {}", repoId);
            }
        }

        return ErrorStackAnalysisResponse.builder()
                .errorType(extractErrorType(errorStack))
                .rootCause(extractRootCause(errorStack))
                .summary(generateErrorSummary(errorStack))
                .suspectedLocations(locations)
                .possibleFixes(generatePossibleFixes(errorStack))
                .relatedCode(relatedCode)
                .build();
    }

    private String extractRelevantSnippet(String fileContent, String methodName, int lineNumber) {
        String[] lines = fileContent.split("\n");
        int startLine = Math.max(0, lineNumber - 1 - 15);
        int endLine = Math.min(lines.length, lineNumber - 1 + 20);
        
        StringBuilder snippet = new StringBuilder();
        for (int i = startLine; i < endLine; i++) {
            snippet.append(String.format("%4d: %s\n", i + 1, lines[i]));
        }
        return snippet.toString();
    }

    private Map<String, List<String>> groupFilesByModule(List<String> files) {
        Map<String, List<String>> modules = new LinkedHashMap<>();
        
        for (String file : files) {
            String[] parts = file.split("/");
            if (parts.length > 1) {
                String module = parts[0];
                modules.computeIfAbsent(module, k -> new ArrayList<>()).add(file);
            }
        }
        
        if (modules.isEmpty()) {
            modules.put("root", files);
        }
        
        return modules;
    }

    private Map<String, String> generateModuleDescriptions(Map<String, List<String>> moduleFiles) {
        Map<String, String> descriptions = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : moduleFiles.entrySet()) {
            descriptions.put(entry.getKey(), "Contains " + entry.getValue().size() + " files");
        }
        return descriptions;
    }

    private List<String> findKeyFiles(List<String> files) {
        List<String> keyFiles = new ArrayList<>();
        Set<String> keyPatterns = Set.of(
                "README", "pom.xml", "build.gradle", "package.json", "requirements.txt",
                "go.mod", "Cargo.toml", "setup.py", "main", "app", "application"
        );
        
        for (String file : files) {
            String fileName = file.toLowerCase();
            for (String pattern : keyPatterns) {
                if (fileName.contains(pattern.toLowerCase())) {
                    keyFiles.add(file);
                    break;
                }
            }
        }
        
        return keyFiles.stream().limit(20).toList();
    }

    private List<String> detectDesignPatterns(String repoId) {
        List<String> patterns = new ArrayList<>();
        patterns.add("MVC (Potential)");
        patterns.add("Repository Pattern (Potential)");
        patterns.add("Service Layer (Potential)");
        return patterns;
    }

    private String detectTechStack(List<String> files) {
        Set<String> tech = new LinkedHashSet<>();
        
        for (String file : files) {
            if (file.endsWith(".java")) tech.add("Java");
            if (file.endsWith(".py")) tech.add("Python");
            if (file.endsWith(".js") || file.endsWith(".ts")) tech.add("JavaScript/TypeScript");
            if (file.endsWith(".go")) tech.add("Go");
            if (file.endsWith(".rs")) tech.add("Rust");
            if (file.contains("pom.xml")) tech.add("Maven");
            if (file.contains("build.gradle")) tech.add("Gradle");
            if (file.contains("package.json")) tech.add("Node.js");
            if (file.contains("requirements.txt")) tech.add("Python (pip)");
        }
        
        return String.join(", ", tech);
    }

    private String generateOverallStructure(List<String> mainModules) {
        return "The project consists of " + mainModules.size() + " main modules: " + 
               String.join(", ", mainModules);
    }

    private List<String> generateRecommendations(List<String> mainModules) {
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Start with the main entry point to understand the application flow");
        recommendations.add("Review configuration files to understand setup and dependencies");
        if (mainModules.size() > 3) {
            recommendations.add("Focus on one module at a time to avoid overwhelm");
        }
        return recommendations;
    }

    private List<CodeSection> splitIntoSections(String content, String filePath) {
        List<CodeSection> sections = new ArrayList<>();
        String[] lines = content.split("\n");
        
        sections.add(CodeSection.builder()
                .sectionName("File Overview")
                .startLine(1)
                .endLine(Math.min(50, lines.length))
                .explanation("This is the beginning of the file showing imports and initial setup")
                .codeSnippet(String.join("\n", Arrays.copyOfRange(lines, 0, Math.min(50, lines.length))))
                .build());
        
        if (lines.length > 50) {
            sections.add(CodeSection.builder()
                    .sectionName("Main Content")
                    .startLine(51)
                    .endLine(lines.length)
                    .explanation("Main implementation of the file")
                    .codeSnippet(String.join("\n", Arrays.copyOfRange(lines, 50, lines.length)))
                    .build());
        }
        
        return sections;
    }

    private List<String> extractDependencies(String content, String filePath) {
        List<String> deps = new ArrayList<>();
        Pattern importPattern = Pattern.compile("^import\\s+([\\w.]+);?");
        Pattern requirePattern = Pattern.compile("require\\s*\\([\"']([^\"']+)[\"']\\)");
        Pattern fromPattern = Pattern.compile("from\\s+[\"']([^\"']+)[\"']");
        
        for (String line : content.split("\n")) {
            Matcher matcher;
            if ((matcher = importPattern.matcher(line.trim())).find()) {
                deps.add(matcher.group(1));
            } else if ((matcher = requirePattern.matcher(line)).find()) {
                deps.add(matcher.group(1));
            } else if ((matcher = fromPattern.matcher(line)).find()) {
                deps.add(matcher.group(1));
            }
        }
        
        return deps.stream().distinct().limit(20).toList();
    }

    private List<String> findDependents(String repoId, String filePath) {
        return new ArrayList<>();
    }

    private String generateFileSummary(String content, String filePath) {
        return "This file contains the implementation for " + filePath + 
               ". It has approximately " + content.split("\n").length + " lines of code.";
    }

    private List<SuspectedLocation> parseErrorStack(String errorStack) {
        List<SuspectedLocation> locations = new ArrayList<>();
        
        List<Pattern> patterns = new ArrayList<>();
        patterns.add(Pattern.compile(
                "at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([\\w.$-]+\\.java):?(\\d+)?\\)"
        ));
        patterns.add(Pattern.compile(
                "at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(([^:]+):(\\d+)\\)"
        ));
        patterns.add(Pattern.compile(
                "at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(Unknown Source\\)"
        ));
        patterns.add(Pattern.compile(
                "at\\s+([\\w.$]+)\\.([\\w$<>]+)\\(Native Method\\)"
        ));
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(errorStack);
            while (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                String fileName = matcher.groupCount() >= 3 ? matcher.group(3) : "Unknown";
                int lineNumber = matcher.groupCount() >= 4 && matcher.group(4) != null 
                    ? Integer.parseInt(matcher.group(4)) 
                    : 0;
                
                if (fileName.equals("Unknown Source") || fileName.equals("Native Method")) {
                    fileName = extractSimpleClassName(className) + ".java";
                }
                
                locations.add(SuspectedLocation.builder()
                        .className(className)
                        .methodName(methodName)
                        .filePath(fileName)
                        .lineNumber(lineNumber)
                        .description("Found in stack trace")
                        .confidence(0.7)
                        .build());
            }
        }
        
        return locations.stream().distinct().toList();
    }
    
    private String extractSimpleClassName(String fullClassName) {
        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot != -1) {
            return fullClassName.substring(lastDot + 1);
        }
        return fullClassName;
    }
    
    private String findFileByFullClassName(List<String> allFiles, String fullClassName, String simpleClassName) {
        log.info("Trying to find file for full class name: {}", fullClassName);
        
        String packagePath = fullClassName.replace('.', '/') + ".java";
        log.info("Checking package path: {}", packagePath);
        
        for (String file : allFiles) {
            if (file.endsWith(packagePath)) {
                log.info("Found exact package path match: {}", file);
                return file;
            }
        }
        
        String simpleFileName = simpleClassName + ".java";
        for (String file : allFiles) {
            if (file.endsWith("/" + simpleFileName) || file.equals(simpleFileName)) {
                log.info("Found simple file name match: {}", file);
                return file;
            }
        }
        
        String lowerSimpleClassName = simpleClassName.toLowerCase();
        for (String file : allFiles) {
            String fileName = file.substring(file.lastIndexOf('/') + 1).toLowerCase();
            if (fileName.contains(lowerSimpleClassName) && fileName.endsWith(".java")) {
                log.info("Found fuzzy match: {}", file);
                return file;
            }
        }
        
        log.info("No match found for class: {}", fullClassName);
        return null;
    }

    private String extractErrorType(String errorStack) {
        String[] lines = errorStack.split("\n");
        if (lines.length > 0) {
            String firstLine = lines[0].trim();
            int colonIndex = firstLine.indexOf(':');
            if (colonIndex > 0) {
                return firstLine.substring(0, colonIndex);
            }
            return firstLine;
        }
        return "Unknown Error";
    }

    private String extractRootCause(String errorStack) {
        String[] lines = errorStack.split("\n");
        if (lines.length > 0) {
            return lines[0].trim();
        }
        return "Could not determine root cause";
    }

    private String generateErrorSummary(String errorStack) {
        return "The error occurred at the location shown in the stack trace. " +
               "Review the suspected locations and related code to identify the issue.";
    }

    private List<String> generatePossibleFixes(String errorStack) {
        List<String> fixes = new ArrayList<>();
        fixes.add("Check the suspected location in the source code");
        fixes.add("Verify input parameters at the error location");
        fixes.add("Add null checks if NullPointerException is present");
        fixes.add("Review recent changes to the affected files");
        return fixes;
    }
}
