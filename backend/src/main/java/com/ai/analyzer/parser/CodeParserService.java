package com.ai.analyzer.parser;

import com.ai.analyzer.model.CodeChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Service
public class CodeParserService {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "py", "js", "ts", "go", "rs", "cpp", "c", "h", "hpp",
            "cs", "php", "rb", "swift", "kt", "scala", "sh", "sql", "xml",
            "json", "yaml", "yml", "html", "css", "md"
    );

    private static final Set<String> IGNORE_DIRS = Set.of(
            ".git", "node_modules", "target", "build", "dist", "vendor",
            "__pycache__", ".idea", ".vscode", ".next", ".nuxt", "out"
    );

    public List<CodeChunk> parseRepository(String repoId, String localPath) {
        List<CodeChunk> chunks = new ArrayList<>();
        Path repoPath = Paths.get(localPath);

        if (!Files.exists(repoPath)) {
            log.error("Repository path does not exist: {}", localPath);
            return chunks;
        }

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .forEach(path -> {
                        try {
                            chunks.addAll(parseFile(repoId, path, repoPath));
                        } catch (IOException e) {
                            log.error("Failed to parse file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to walk repository: {}", localPath, e);
        }

        log.info("Parsed {} code chunks from repository: {}", chunks.size(), repoId);
        return chunks;
    }

    private boolean isSupportedFile(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return false;

        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    private boolean shouldIgnoreDir(Path path, Path repoPath) {
        Path relativePath = repoPath.relativize(path);
        for (Path component : relativePath) {
            if (IGNORE_DIRS.contains(component.toString())) {
                return true;
            }
        }
        return false;
    }

    private List<CodeChunk> parseFile(String repoId, Path filePath, Path repoPath) throws IOException {
        List<CodeChunk> chunks = new ArrayList<>();
        String relativePath = repoPath.relativize(filePath).toString().replace(File.separator, "/");
        String fileName = filePath.getFileName().toString();
        String extension = getFileExtension(fileName);

        List<String> lines = Files.readAllLines(filePath);
        if (lines.isEmpty()) {
            return chunks;
        }

        int chunkSize = getChunkSize(extension);
        int overlap = 5;
        int currentLine = 0;

        while (currentLine < lines.size()) {
            int endLine = Math.min(currentLine + chunkSize - 1, lines.size());
            String content = String.join("\n", lines.subList(currentLine, endLine));

            CodeChunk chunk = CodeChunk.builder()
                    .id(repoId + "_" + relativePath + "_" + (currentLine + 1))
                    .repositoryId(repoId)
                    .filePath(relativePath)
                    .fileName(fileName)
                    .fileType(extension)
                    .startLine(currentLine + 1)
                    .endLine(endLine)
                    .content(content)
                    .build();

            chunks.add(chunk);
            currentLine = currentLine + chunkSize - overlap;
        }

        return chunks;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return "";
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    private int getChunkSize(String extension) {
        return switch (extension) {
            case "java", "cs", "kt", "scala" -> 100;
            case "py", "rb", "php" -> 80;
            case "js", "ts", "jsx", "tsx" -> 120;
            default -> 100;
        };
    }

    public String readFile(String localPath, String relativePath) throws IOException {
        Path filePath = Paths.get(localPath, relativePath);
        return Files.readString(filePath);
    }

    public List<String> listFiles(String localPath) throws IOException {
        List<String> files = new ArrayList<>();
        Path repoPath = Paths.get(localPath);

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .filter(path -> !shouldIgnoreDir(path, repoPath))
                    .forEach(path -> files.add(repoPath.relativize(path).toString().replace(File.separator, "/")));
        }

        return files;
    }

    public List<String> searchFilesByClassName(String localPath, String className) throws IOException {
        List<String> matchedFiles = new ArrayList<>();
        Path repoPath = Paths.get(localPath);
        String lowerClassName = className.toLowerCase();

        try (Stream<Path> paths = Files.walk(repoPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isSupportedFile)
                    .filter(path -> !shouldIgnoreDir(path, repoPath))
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String fileNameWithoutExt = fileName;
                        int dotIndex = fileName.lastIndexOf('.');
                        if (dotIndex != -1) {
                            fileNameWithoutExt = fileName.substring(0, dotIndex);
                        }

                        if (fileNameWithoutExt.equalsIgnoreCase(className) || 
                            fileNameWithoutExt.toLowerCase().contains(lowerClassName)) {
                            matchedFiles.add(repoPath.relativize(path).toString().replace(File.separator, "/"));
                        }
                    });
        }

        if (matchedFiles.isEmpty()) {
            try (Stream<Path> paths = Files.walk(repoPath)) {
                paths.filter(Files::isRegularFile)
                        .filter(this::isSupportedFile)
                        .filter(path -> !shouldIgnoreDir(path, repoPath))
                        .filter(path -> fileNameMatchesClass(path, className))
                        .forEach(path -> {
                            String relativePath = repoPath.relativize(path).toString().replace(File.separator, "/");
                            if (!matchedFiles.contains(relativePath)) {
                                matchedFiles.add(relativePath);
                            }
                        });
            }
        }

        return matchedFiles;
    }

    private boolean fileNameMatchesClass(Path path, String className) {
        String fileName = path.getFileName().toString().toLowerCase();
        String lowerClassName = className.toLowerCase();
        
        String[] parts = lowerClassName.split("(?=[A-Z])");
        boolean matches = true;
        for (String part : parts) {
            if (!part.isEmpty() && !fileName.contains(part)) {
                matches = false;
                break;
            }
        }
        return matches;
    }
}
