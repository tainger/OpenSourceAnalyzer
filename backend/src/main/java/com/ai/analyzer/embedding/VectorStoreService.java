package com.ai.analyzer.embedding;

import com.ai.analyzer.config.AnalyzerProperties;
import com.ai.analyzer.model.CodeChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VectorStoreService {

    private final Map<String, List<CodeChunk>> chunkStore;
    private final EmbeddingService embeddingService;

    public VectorStoreService(EmbeddingService embeddingService, AnalyzerProperties properties) {
        this.embeddingService = embeddingService;
        this.chunkStore = new ConcurrentHashMap<>();
        log.info("Initialized in-memory vector store");
    }

    public void addCodeChunk(CodeChunk chunk) {
        if (chunk.getEmbedding() == null) {
            chunk.setEmbedding(embeddingService.embed(chunk.getContent()));
        }
        chunkStore.computeIfAbsent(chunk.getRepositoryId(), k -> new ArrayList<>()).add(chunk);
        log.debug("Added code chunk to in-memory store: {}", chunk.getFilePath());
    }

    public void addCodeChunks(List<CodeChunk> chunks) {
        for (CodeChunk chunk : chunks) {
            addCodeChunk(chunk);
        }
    }

    public List<CodeChunk> search(String query, String repositoryId, int limit) {
        List<CodeChunk> results = new ArrayList<>();
        
        if (repositoryId == null || !chunkStore.containsKey(repositoryId)) {
            return results;
        }

        List<CodeChunk> chunks = chunkStore.get(repositoryId);
        if (chunks.isEmpty()) {
            return results;
        }

        float[] queryEmbedding = embeddingService.embed(query);

        List<ScoredChunk> scoredChunks = new ArrayList<>();
        for (CodeChunk chunk : chunks) {
            float[] chunkEmbedding = chunk.getEmbedding();
            if (chunkEmbedding != null) {
                double similarity = cosineSimilarity(queryEmbedding, chunkEmbedding);
                double keywordScore = calculateKeywordScore(query, chunk);
                double combinedScore = 0.7 * similarity + 0.3 * keywordScore;
                scoredChunks.add(new ScoredChunk(chunk, combinedScore));
            } else {
                double keywordScore = calculateKeywordScore(query, chunk);
                scoredChunks.add(new ScoredChunk(chunk, keywordScore * 0.5));
            }
        }

        scoredChunks.sort((a, b) -> Double.compare(b.score, a.score));

        for (int i = 0; i < Math.min(limit, scoredChunks.size()); i++) {
            results.add(scoredChunks.get(i).chunk);
        }

        log.info("Vector search completed, found {} results (limit: {})", results.size(), limit);
        return results;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += Math.pow(a[i], 2);
            normB += Math.pow(b[i], 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double calculateKeywordScore(String query, CodeChunk chunk) {
        String lowerQuery = query.toLowerCase();
        String lowerContent = chunk.getContent() != null ? chunk.getContent().toLowerCase() : "";
        String lowerFilePath = chunk.getFilePath() != null ? chunk.getFilePath().toLowerCase() : "";

        double score = 0.0;
        String[] keywords = lowerQuery.split("\\s+");

        for (String keyword : keywords) {
            if (keyword.isEmpty()) continue;
            
            if (lowerFilePath.contains(keyword)) {
                score += 0.5;
            }
            
            if (lowerContent.contains(keyword)) {
                score += 0.3;
            }
        }

        return Math.min(score, 1.0);
    }

    public void deleteByRepositoryId(String repositoryId) {
        chunkStore.remove(repositoryId);
        log.info("Deleted all chunks for repository: {}", repositoryId);
    }

    private static class ScoredChunk {
        final CodeChunk chunk;
        final double score;

        ScoredChunk(CodeChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
