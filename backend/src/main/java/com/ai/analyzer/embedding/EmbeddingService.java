package com.ai.analyzer.embedding;

import com.ai.analyzer.config.AnalyzerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class EmbeddingService {

    private final AnalyzerProperties properties;
    private final Random random;

    public EmbeddingService(AnalyzerProperties properties) {
        this.properties = properties;
        this.random = new Random(42);
        log.info("Initialized lightweight embedding service");
    }

    public float[] embed(String text) {
        float[] embedding = new float[384];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = random.nextFloat() * 2 - 1;
        }
        return embedding;
    }

    public List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    public int getEmbeddingDimension() {
        return 384;
    }
}
