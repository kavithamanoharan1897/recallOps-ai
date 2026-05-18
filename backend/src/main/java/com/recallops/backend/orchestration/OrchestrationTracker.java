package com.recallops.backend.orchestration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class OrchestrationTracker {

    private final ThreadLocal<List<String>> stepLogs = ThreadLocal.withInitial(ArrayList::new);

    public void logStep(String step) {
        String formattedStep = "[Agent] " + step;
        log.info(formattedStep);
        stepLogs.get().add(formattedStep);
    }

    public List<String> getSteps() {
        return new ArrayList<>(stepLogs.get());
    }

    public void clear() {
        stepLogs.remove();
    }
}
