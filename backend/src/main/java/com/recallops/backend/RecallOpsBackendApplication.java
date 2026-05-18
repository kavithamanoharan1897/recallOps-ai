package com.recallops.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(excludeName = {"org.springframework.ai.model.vertexai.autoconfigure.embedding.VertexAiEmbeddingConnectionAutoConfiguration"})
@EnableRetry
public class RecallOpsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecallOpsBackendApplication.class, args);
	}

}
