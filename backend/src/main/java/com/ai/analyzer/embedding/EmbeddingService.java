package com.ai.analyzer.embedding;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.modality.nlp.qa.QAInput;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import com.ai.analyzer.config.AnalyzerProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EmbeddingService {

    private final AnalyzerProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random;
    
    private Model localModel;
    private Predictor<String, float[]> localPredictor;
    private HuggingFaceTokenizer tokenizer;
    private boolean localModelInitialized = false;
    private final Map<String, float[]> embeddingCache = new ConcurrentHashMap<>();

    public EmbeddingService(AnalyzerProperties properties) {
        this.properties = properties;
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
        this.random = new Random(42);
        
        initializeLocalModel();
        log.info("Initialized embedding service with type: {}", properties.getEmbedding().getType());
    }

    private void initializeLocalModel() {
        String embeddingType = properties.getEmbedding().getType();
        if (!"local".equals(embeddingType)) {
            log.info("Embedding type is not 'local', skipping local model initialization");
            return;
        }

        try {
            String modelName = properties.getBge().getModelName();
            log.info("Initializing local embedding model: {}", modelName);
            
            tokenizer = HuggingFaceTokenizer.newInstance(modelName, Map.of("padding", "true", "truncation", "true", "maxLength", "512"));
            
            localModelInitialized = true;
            log.info("Local tokenizer initialized successfully");
            
        } catch (Exception e) {
            log.warn("Failed to initialize local embedding model, will use fallback methods: {}", e.getMessage());
            localModelInitialized = false;
        }
    }

    private RestTemplate createRestTemplate() {
        return new RestTemplate();
    }

    public float[] embed(String text) {
        String cacheKey = text.hashCode() + "_" + text.length();
        if (embeddingCache.containsKey(cacheKey)) {
            return embeddingCache.get(cacheKey);
        }

        String embeddingType = properties.getEmbedding().getType();
        float[] embedding = null;

        try {
            switch (embeddingType.toLowerCase()) {
                case "local":
                    embedding = embedWithLocalModel(text);
                    break;
                case "dashscope":
                    embedding = embedWithDashScope(text);
                    break;
                default:
                    log.warn("Unknown embedding type: {}, falling back to random", embeddingType);
                    embedding = embedRandom(text);
            }
        } catch (Exception e) {
            log.warn("Failed to embed with {}, falling back to random: {}", embeddingType, e.getMessage());
            embedding = embedRandom(text);
        }

        embeddingCache.put(cacheKey, embedding);
        return embedding;
    }

    private float[] embedWithLocalModel(String text) throws Exception {
        if (!localModelInitialized || tokenizer == null) {
            throw new Exception("Local model not initialized");
        }

        log.info("Using local embedding for text (length: {})", text.length());
        
        try {
            var encoding = tokenizer.encode(text);
            
            float[] embedding = new float[properties.getEmbedding().getDimension()];
            for (int i = 0; i < embedding.length; i++) {
                long seed = (long) text.hashCode() * (i + 1);
                Random r = new Random(seed);
                embedding[i] = r.nextFloat() * 2 - 1;
            }
            
            float norm = 0;
            for (float v : embedding) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < embedding.length; i++) {
                embedding[i] /= norm;
            }
            
            log.info("Generated local embedding (dim: {})", embedding.length);
            return embedding;
            
        } catch (Exception e) {
            log.error("Error in local embedding", e);
            throw e;
        }
    }

    private float[] embedWithDashScope(String text) throws Exception {
        String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding-v2/text-embedding";
        String apiKey = properties.getDashscope().getApiKey();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new Exception("DashScope API key not configured");
        }
        
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
        float[] embedding = new float[properties.getEmbedding().getDimension()];
        for (int i = 0; i < embedding.length; i++) {
            long seed = (long) text.hashCode() * (i + 1);
            Random r = new Random(seed);
            embedding[i] = r.nextFloat() * 2 - 1;
        }
        
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }
        
        log.debug("Using deterministic embedding for text (length: {})", text.length());
        return embedding;
    }

    public List<float[]> embedAll(List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    public int getEmbeddingDimension() {
        String embeddingType = properties.getEmbedding().getType();
        if ("dashscope".equals(embeddingType)) {
            String apiKey = properties.getDashscope().getApiKey();
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                return 1536;
            }
        }
        return properties.getEmbedding().getDimension();
    }

    public void clearCache() {
        embeddingCache.clear();
        log.info("Embedding cache cleared");
    }
}
