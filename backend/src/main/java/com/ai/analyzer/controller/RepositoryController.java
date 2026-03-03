package com.ai.analyzer.controller;

import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.Repository;
import com.ai.analyzer.model.dto.CloneRepositoryRequest;
import com.ai.analyzer.service.RepositoryAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RepositoryController {

    private final GitService gitService;
    private final RepositoryAnalysisService analysisService;

    @PostMapping("/clone")
    public ResponseEntity<Repository> cloneRepository(@Valid @RequestBody CloneRepositoryRequest request) {
        Repository repository = analysisService.cloneAndAnalyze(request.getUrl(), request.getBranch());
        return ResponseEntity.ok(repository);
    }

    @GetMapping
    public ResponseEntity<Map<String, Repository>> getAllRepositories() {
        return ResponseEntity.ok(gitService.getAllRepositories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Repository> getRepository(@PathVariable String id) {
        Repository repository = gitService.getRepository(id);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(@PathVariable String id) {
        gitService.deleteRepository(id);
        return ResponseEntity.noContent().build();
    }
}
