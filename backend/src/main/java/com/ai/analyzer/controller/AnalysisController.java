package com.ai.analyzer.controller;

import com.ai.analyzer.git.GitService;
import com.ai.analyzer.model.Repository;
import com.ai.analyzer.model.dto.*;
import com.ai.analyzer.parser.CodeParserService;
import com.ai.analyzer.service.ChatMemoryService;
import com.ai.analyzer.service.ChatService;
import com.ai.analyzer.service.RepositoryAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalysisController {

    private final RepositoryAnalysisService analysisService;
    private final CodeParserService codeParserService;
    private final GitService gitService;
    private final ChatService chatService;
    private final ChatMemoryService chatMemoryService;

    @GetMapping("/architecture/{repoId}")
    public ResponseEntity<ArchitectureAnalysisResponse> analyzeArchitecture(@PathVariable String repoId) {
        return ResponseEntity.ok(analysisService.analyzeArchitecture(repoId));
    }

    @GetMapping("/walkthrough/{repoId}")
    public ResponseEntity<CodeWalkthroughResponse> walkthroughCode(
            @PathVariable String repoId,
            @RequestParam String filePath
    ) {
        return ResponseEntity.ok(analysisService.walkthroughCode(repoId, filePath));
    }

    @PostMapping("/error-stack")
    public ResponseEntity<ErrorStackAnalysisResponse> analyzeErrorStack(
            @Valid @RequestBody ErrorStackAnalysisRequest request
    ) {
        return ResponseEntity.ok(analysisService.analyzeErrorStack(
                request.getRepositoryId(),
                request.getErrorStack()
        ));
    }

    @GetMapping("/files/{repoId}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String repoId) throws IOException {
        Repository repository = gitService.getRepository(repoId);
        if (repository == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(codeParserService.listFiles(repository.getLocalPath()));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String response = chatService.chat(request.getRepositoryId(), request.getMessage());
        return ResponseEntity.ok(ChatResponse.builder()
                .message(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/chat/{repoId}/history")
    public ResponseEntity<Void> clearChatHistory(@PathVariable String repoId) {
        chatMemoryService.clearConversation(repoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/chat/history")
    public ResponseEntity<Void> clearAllChatHistory() {
        chatMemoryService.clearAllConversations();
        return ResponseEntity.noContent().build();
    }
}
