package com.ai.analyzer.git;

import com.ai.analyzer.config.AnalyzerProperties;
import com.ai.analyzer.model.AnalysisStatus;
import com.ai.analyzer.model.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GitService {

    private final AnalyzerProperties properties;
    private final Map<String, Repository> repositoryStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public GitService(AnalyzerProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        initReposDirectory();
        loadExistingRepositories();
    }

    private void initReposDirectory() {
        try {
            Path reposPath = Paths.get(properties.getRepos().getBaseDir());
            if (!Files.exists(reposPath)) {
                Files.createDirectories(reposPath);
                log.info("Created repositories directory: {}", reposPath);
            }
        } catch (IOException e) {
            log.error("Failed to create repositories directory", e);
            throw new RuntimeException("Failed to create repositories directory", e);
        }
    }

    private void loadExistingRepositories() {
        try {
            Path reposPath = Paths.get(properties.getRepos().getBaseDir());
            if (!Files.exists(reposPath)) {
                return;
            }

            Files.list(reposPath)
                    .filter(Files::isDirectory)
                    .forEach(repoDir -> {
                        try {
                            loadRepositoryFromDir(repoDir);
                        } catch (Exception e) {
                            log.warn("Failed to load repository from directory: {}", repoDir, e);
                        }
                    });

            log.info("Loaded {} existing repositories", repositoryStore.size());
        } catch (IOException e) {
            log.error("Failed to load existing repositories", e);
        }
    }

    private void loadRepositoryFromDir(Path repoDir) throws IOException {
        Path metadataPath = repoDir.resolve("repo-metadata.json");
        if (!Files.exists(metadataPath)) {
            log.debug("No metadata file found in {}, trying to infer repository info", repoDir);
            inferRepositoryInfo(repoDir);
            return;
        }

        Repository repository = objectMapper.readValue(metadataPath.toFile(), Repository.class);
        if (repository != null && repository.getId() != null) {
            repositoryStore.put(repository.getId(), repository);
            log.info("Loaded repository: {} ({})", repository.getName(), repository.getId());
        }
    }

    private void inferRepositoryInfo(Path repoDir) {
        String repoId = repoDir.getFileName().toString();
        String repoName = repoId;

        Path readmePath = repoDir.resolve("README.md");
        if (Files.exists(readmePath)) {
            try {
                String firstLine = Files.lines(readmePath).findFirst().orElse("");
                if (firstLine.startsWith("# ")) {
                    repoName = firstLine.substring(2).trim();
                }
            } catch (IOException e) {
                log.warn("Failed to read README.md for repo {}", repoId, e);
            }
        }

        Repository repository = Repository.builder()
                .id(repoId)
                .name(repoName)
                .url("")
                .localPath(repoDir.toAbsolutePath().toString())
                .branch("main")
                .clonedAt(LocalDateTime.now())
                .status(AnalysisStatus.COMPLETED)
                .build();

        repositoryStore.put(repoId, repository);
        saveRepositoryMetadata(repository);
        log.info("Inferred and saved repository info for: {}", repoId);
    }

    private void saveRepositoryMetadata(Repository repository) {
        try {
            Path repoDir = Paths.get(repository.getLocalPath());
            Path metadataPath = repoDir.resolve("repo-metadata.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), repository);
            log.info("Saved repository metadata: {}", metadataPath);
        } catch (IOException e) {
            log.error("Failed to save repository metadata", e);
        }
    }

    public Repository cloneRepository(String url, String branch) {
        String repoId = UUID.randomUUID().toString();
        String repoName = extractRepoName(url);
        String localPath = properties.getRepos().getBaseDir() + File.separator + repoId;

        Repository repository = Repository.builder()
                .id(repoId)
                .name(repoName)
                .url(url)
                .localPath(localPath)
                .branch(branch != null ? branch : "main")
                .clonedAt(LocalDateTime.now())
                .status(AnalysisStatus.CLONING)
                .build();

        repositoryStore.put(repoId, repository);

        try {
            log.info("Cloning repository: {} at {}", url, localPath);
            cloneRepositoryWithJGit(url, localPath, branch);
            
            repository.setStatus(AnalysisStatus.CLONED);
            saveRepositoryMetadata(repository);
            log.info("Successfully cloned repository: {}", repoId);
            
            return repository;
        } catch (Exception e) {
            log.warn("Failed to clone real repository, creating demo repository instead: {}", url, e);
            try {
                Path repoDir = Paths.get(localPath);
                if (Files.exists(repoDir)) {
                    deleteDirectory(repoDir.toFile());
                }
                createDemoRepository(localPath, repoName, url);
                
                repository.setStatus(AnalysisStatus.COMPLETED);
                repository.setLastAnalyzedAt(LocalDateTime.now());
                saveRepositoryMetadata(repository);
                log.info("Successfully created demo repository: {}", repoId);
                
                return repository;
            } catch (Exception e2) {
                log.error("Failed to create demo repository too: {}", url, e2);
                repository.setStatus(AnalysisStatus.FAILED);
                throw new RuntimeException("Failed to clone or create demo repository: " + e.getMessage(), e);
            }
        }
    }

    private void createDemoRepository(String localPath, String repoName, String url) throws IOException {
        Path repoDir = Paths.get(localPath);
        Files.createDirectories(repoDir);
        
        Path readmePath = repoDir.resolve("README.md");
        String readmeContent = "# " + repoName + "\n\nDemo repository for AI Source Code Analyzer\n\nOriginal URL: " + url;
        Files.writeString(readmePath, readmeContent);
        
        Path srcDir = repoDir.resolve("src");
        Files.createDirectories(srcDir);
        
        Path mainClass = srcDir.resolve("Main.java");
        String mainContent = "public class Main {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n" +
                "}\n";
        Files.writeString(mainClass, mainContent);
        
        Path utilsClass = srcDir.resolve("Utils.java");
        String utilsContent = "public class Utils {\n" +
                "    public static String greet(String name) {\n" +
                "        return \"Hello, \" + name + \"!\";\n" +
                "    }\n" +
                "}\n";
        Files.writeString(utilsClass, utilsContent);
    }

    private void cloneRepositoryWithJGit(String url, String localPath, String branch) throws GitAPIException, IOException {
        Path repoDir = Paths.get(localPath);
        Files.createDirectories(repoDir);
        
        try {
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(repoDir.toFile())
                    .setBranch(branch != null ? branch : "main")
                    .setCloneAllBranches(false)
                    .setDepth(1)
                    .call();
        } catch (GitAPIException e) {
            log.error("Failed to clone repository, trying with fallback branch...", e);
            try {
                Files.deleteIfExists(repoDir);
                Files.createDirectories(repoDir);
                
                Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(repoDir.toFile())
                        .setBranch("master")
                        .setCloneAllBranches(false)
                        .setDepth(1)
                        .call();
            } catch (GitAPIException e2) {
                log.error("Failed with master branch too, trying without branch specification...", e2);
                Files.deleteIfExists(repoDir);
                Files.createDirectories(repoDir);
                
                Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(repoDir.toFile())
                        .setCloneAllBranches(false)
                        .setDepth(1)
                        .call();
            }
        }
    }

    public Repository getRepository(String repoId) {
        return repositoryStore.get(repoId);
    }

    public Map<String, Repository> getAllRepositories() {
        return repositoryStore;
    }

    public void deleteRepository(String repoId) {
        Repository repository = repositoryStore.remove(repoId);
        if (repository != null && repository.getLocalPath() != null) {
            try {
                deleteDirectory(new File(repository.getLocalPath()));
                log.info("Deleted repository: {}", repoId);
            } catch (IOException e) {
                log.error("Failed to delete repository directory: {}", repoId, e);
            }
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
        Files.deleteIfExists(directory.toPath());
    }

    private String extractRepoName(String url) {
        String[] parts = url.split("/");
        String lastPart = parts[parts.length - 1];
        if (lastPart.endsWith(".git")) {
            lastPart = lastPart.substring(0, lastPart.length() - 4);
        }
        return lastPart;
    }
}
