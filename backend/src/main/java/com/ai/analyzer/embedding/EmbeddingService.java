package com.ai.analyzer.embedding;

import com.ai.analyzer.config.AnalyzerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class EmbeddingService {

    private final AnalyzerProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random;

    public EmbeddingService(AnalyzerProperties properties) {
        this.properties = properties;
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
        this.random = new Random(42);
        log.info("Initialized embedding service");
    }

    private RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    public float[] embed(String text) {
        String apiKey = properties.getDashscope().getApiKey();
        
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                return embedWithDashScope(text);
            } catch (Exception e) {
                log.warn("Failed to use DashScope embedding, falling back to random: {}", e.getMessage());
            }
        }
        
        log.debug("Using random embedding for text (length: {})", text.length());
        return embedRandom(text);
    }

    private float[] embedWithDashScope(String text) throws Exception {
        String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding-v2/text-embedding";
        String apiKey = properties.getDashscope().getApiKey();
        
        log.info("Calling DashScope embedding API for text (length: {})", text.length());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "text-embedding-v2");
        requestBody.put("input", Map.of("texts", Arrays.asList(text)));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);
        
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode embeddings = root.path("output").path("embeddings");
        
        if (embeddings.isArray() && embeddings.size() > 0) {
            JsonNode embeddingNode = embeddings.get(0).path("embedding");
            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = embeddingNode.get(i).floatValue();
            }
            log.info("Successfully got embedding from DashScope (dim: {})", embedding.length);
            return embedding;
        }
        
        throw new Exception("No embedding returned from DashScope");
    }

    private float[] embedRandom(String text) {
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
        String apiKey = properties.getDashscope().getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return 1536;
        }
        return 384;
    }
}
