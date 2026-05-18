package com.recallops.backend.service;

import com.google.cloud.aiplatform.v1beta1.*;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class MemoryService {

    private final Map<String, String> localConversationHistory = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @org.springframework.beans.factory.annotation.Value("${spring.ai.vertex.ai.gemini.location:us-central1}")
    private String location;

    @org.springframework.beans.factory.annotation.Value("${kb.reasoning-engine.id:default}")
    private String reasoningEngineId;

    public String getSessionContext(String sessionId) {
        StringBuilder context = new StringBuilder();
        
        // 1. Check Local History
        String recentHistory = localConversationHistory.get(sessionId);
        if (recentHistory != null && !recentHistory.isBlank()) {
            context.append("RECENT INTERACTION HISTORY:\n").append(recentHistory).append("\n\n");
        }

        log.info("Vertex AI Memory Bank: Fetching persistent context for session: {}", sessionId);
        
        // 2. Check Cloud History
        try {
            String cloudMemories = fetchLongTermMemories(sessionId);
            if (cloudMemories != null && !cloudMemories.isBlank() && !cloudMemories.equals("- ")) {
                context.append("PERSISTENT ENGINEERING KNOWLEDGE (Retrieved from Vertex AI Memory Bank):\n")
                       .append(cloudMemories);
            }
        } catch (Exception e) {
            log.warn("Vertex AI Memory Bank: retrieval skipped. Reason: {}", e.getMessage());
            // Don't append "Cloud Memory initializing" to the prompt context if it's empty
        }

        String finalContext = context.toString().trim();
        return finalContext.isEmpty() ? null : finalContext;
    }

    public void updateSessionContext(String sessionId, String interaction) {
        if (sessionId == null) return;
        String current = localConversationHistory.getOrDefault(sessionId, "");
        localConversationHistory.put(sessionId, (current + "\n" + interaction).trim());

        log.info("Vertex AI Memory Bank: Persisting interaction for session: {}", sessionId);
        try {
            storeMemoryFact(sessionId, interaction);
        } catch (Exception e) {
            log.error("Vertex AI Memory Bank: Failed to persist memory: {}", e.getMessage());
        }
    }

    private String fetchLongTermMemories(String sessionId) throws IOException {
        String endpoint = location + "-aiplatform.googleapis.com:443";
        MemoryBankServiceSettings settings = MemoryBankServiceSettings.newBuilder()
                .setEndpoint(endpoint)
                .build();

        try (MemoryBankServiceClient client = MemoryBankServiceClient.create(settings)) {
            // Using the correct resource path for v1beta1 MemoryBank/ReasoningEngine
            String parent = constructParentPath();
            
            RetrieveMemoriesRequest request = RetrieveMemoriesRequest.newBuilder()
                    .setParent(parent)
                    .putScope("session_id", sessionId)
                    .build();

            RetrieveMemoriesResponse response = client.retrieveMemories(request);

            return response.getRetrievedMemoriesList().stream()
                    .map(rm -> rm.getMemory().getFact())
                    .collect(Collectors.joining("\n- ", "- ", ""));
        }
    }

    private void storeMemoryFact(String sessionId, String engineeringFact) throws IOException {
        String endpoint = location + "-aiplatform.googleapis.com:443";
        MemoryBankServiceSettings settings = MemoryBankServiceSettings.newBuilder()
                .setEndpoint(endpoint)
                .build();

        try (MemoryBankServiceClient client = MemoryBankServiceClient.create(settings)) {
            String parent = constructParentPath();
            
            Memory memory = Memory.newBuilder()
                    .setFact(engineeringFact)
                    .putScope("session_id", sessionId)
                    .build();

            CreateMemoryRequest request = CreateMemoryRequest.newBuilder()
                    .setParent(parent)
                    .setMemory(memory)
                    .build();

            client.createMemoryAsync(request);
        }
    }

    private String constructParentPath() {
        if ("default".equals(reasoningEngineId)) {
            // Fallback to location-based parent if no specific engine ID is provided
            return String.format("projects/%s/locations/%s", projectId, location);
        }
        return String.format("projects/%s/locations/%s/reasoningEngines/%s", projectId, location, reasoningEngineId);
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            localConversationHistory.remove(sessionId);
        }
    }
}
