package com.recallops.backend.service;

import com.recallops.backend.dto.ChatRequest;
import com.recallops.backend.dto.ChatResponse;
import com.recallops.backend.dto.SourceMetadata;
import com.recallops.backend.orchestration.OrchestrationTracker;
import com.recallops.backend.orchestration.PromptBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AgentOrchestratorService {

    private final AIClientService aiClient;
    private final MemoryService memoryService;
    private final OrchestrationTracker tracker;
    private final PromptBuilder promptBuilder;
    private final String repoOwner;
    private final String repoName;

    public AgentOrchestratorService(AIClientService aiClient, MemoryService memoryService, 
                                    OrchestrationTracker tracker, PromptBuilder promptBuilder,
                                    @Value("${kb.repo.owner}") String repoOwner,
                                    @Value("${kb.repo.name}") String repoName) {
        this.aiClient = aiClient;
        this.memoryService = memoryService;
        this.tracker = tracker;
        this.promptBuilder = promptBuilder;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public ChatResponse orchestrate(ChatRequest request) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        
        tracker.clear();
        
        try {
            tracker.logStep("Analyzing engineering question: " + request.getQuestion());

            tracker.logStep("Retrieving session context...");
            String sessionMemory = memoryService.getSessionContext(sessionId);
            boolean hasHistory = sessionMemory != null && !sessionMemory.isBlank();
            tracker.logStep(hasHistory ? "Resuming conversation context." : "Initializing new engineering session.");

            tracker.logStep("Orchestrating agentic reasoning flow...");
            
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(request.getQuestion(), sessionMemory, 
                    "Target Repository: " + repoOwner + "/" + repoName);

            String rawAiResponse = aiClient.generateResponse(systemPrompt, userPrompt);
            
            tracker.logStep("Reasoning flow complete. Synthesizing final response...");

            // Extract confidence and answer using more robust regex
            String confidence = "MEDIUM";
            String answer = rawAiResponse;
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\[Confidence:\\s*(HIGH|MEDIUM|LOW)\\]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(rawAiResponse);
            
            if (matcher.find()) {
                confidence = matcher.group(1).toUpperCase();
                answer = rawAiResponse.substring(0, matcher.start()).trim();
            }

            tracker.logStep("Updating session memory for future recall...");
            memoryService.updateSessionContext(sessionId, request.getQuestion() + "\nAI: " + answer);

            // Sanitize answer to normalize excessive newlines (fix for huge gaps in UI)
            String sanitizedAnswer = answer.replaceAll("\\n{3,}", "\n\n").trim();

            String githubUrl = String.format("https://github.com/%s/%s", repoOwner, repoName);

            return ChatResponse.builder()
                    .answer(sanitizedAnswer)
                    .sources(List.of(SourceMetadata.builder()
                            .title(repoName)
                            .type("GitHub Repository")
                            .url(githubUrl)
                            .build()))
                    .reasoningFlow(tracker.getSteps())
                    .memoryTimeline(List.of("Interaction at " + LocalDateTime.now()))
                    .confidence(confidence)
                    .sessionId(sessionId)
                    .traceId(traceId)
                    .build();

        } finally {
            log.info("Orchestration flow completed for session: {} [Trace: {}]", sessionId, traceId);
        }
    }
}
