package com.recallops.backend.service;

import com.recallops.backend.orchestration.OrchestrationTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIClientService {

    private final ChatClient chatClient;
    private final OrchestrationTracker tracker;

    public AIClientService(ChatClient.Builder builder, SyncMcpToolCallbackProvider mcpToolProvider, OrchestrationTracker tracker) {
        this.tracker = tracker;
        
        // Wrap MCP tools to intercept calls and handle them in parallel via CompletableFuture
        List<ToolCallback> wrappedCallbacks = Arrays.stream(mcpToolProvider.getToolCallbacks())
                .map(this::wrapToolCallback)
                .collect(Collectors.toList());

        this.chatClient = builder
                .defaultToolCallbacks(wrappedCallbacks)
                .build();
    }

    private ToolCallback wrapToolCallback(ToolCallback original) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return original.getToolDefinition();
            }
            
            @Override
            public String call(String input) {
                String toolName = original.getToolDefinition().name();
                tracker.logStep("[GitHub] Initiating parallel retrieval for tool [" + toolName + "]...");

                try {
                    // Using CompletableFuture with virtual threads for parallel execution
                    return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        tracker.logStep("[GitHub] Execution started (Virtual Thread) for: " + input);
                        return original.call(input);
                    }, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()).get();
                } catch (Exception e) {
                    tracker.logStep("[GitHub] Tool execution failed: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Retryable(
        value = { Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public String generateResponse(String systemPrompt, String userPrompt) {
        log.info("AI Client: Sending request to ChatClient...");
        
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI Client: Error during AI generation", e);
            throw new RuntimeException("AI generation failed", e);
        }
    }
}
