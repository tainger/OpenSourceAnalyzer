package com.ai.analyzer;

import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.Repository;
import com.ai.analyzer.service.RepositoryAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SourceCodeAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SourceCodeAnalyzerApplication.class, args);
    }

    @Component
    @RequiredArgsConstructor
    @Slf4j
    public static class RepositoryInitializer implements ApplicationRunner {

        private final GitService gitService;
        private final RepositoryAnalysisService analysisService;

        @Override
        public void run(ApplicationArguments args) {
            log.info("Starting repository initialization...");
            for (Repository repository : gitService.getAllRepositories().values()) {
                analysisService.analyzeExistingRepository(repository);
            }
            log.info("Repository initialization completed");
        }
    }
}
