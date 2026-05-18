package com.recallops.backend.controller;

import com.recallops.backend.dto.ChatRequest;
import com.recallops.backend.dto.ChatResponse;
import com.recallops.backend.service.AgentOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentOrchestratorService orchestratorService;

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received engineering chat request: {}", request.getQuestion());
        return orchestratorService.orchestrate(request);
    }

    @GetMapping("/health")
    public String health() {
        return "RecallOps Backend is healthy";
    }
}
