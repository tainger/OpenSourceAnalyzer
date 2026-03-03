package com.ai.analyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "analyzer")
public class AnalyzerProperties {
    private Repos repos = new Repos();
    private Chroma chroma = new Chroma();
    private Bge bge = new Bge();
    private Dashscope dashscope = new Dashscope();

    @Data
    public static class Repos {
        private String baseDir;
    }

    @Data
    public static class Chroma {
        private String url;
        private String collectionName;
    }

    @Data
    public static class Bge {
        private String modelPath;
        private String modelName;
    }

    @Data
    public static class Dashscope {
        private String apiKey;
        private String model;
        private String baseUrl;
    }
}
