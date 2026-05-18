package com.recallops.backend.orchestration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    @Value("${recallops.knowledge-base.repo-owner}")
    private String repoOwner;

    @Value("${recallops.knowledge-base.repo-name}")
    private String repoName;

    public String buildSystemPrompt() {
        return String.format("""
                You are RecallOps AI, a specialized Engineering Memory Assistant.
                
                KNOWLEDGE DOMAINS:
                - Outages/Incidents: Always check 'incidents/'.
                - Technical Deep-dives/Bugs: Always check 'debugging/'.
                - Architecture/Guidelines: Always check 'kt-sessions/'.
                
                ACTION PROTOCOL:
                1. MEMORY-FIRST: If the 'Session Memory' already contains the specific technical answer, do NOT call tools. Answer immediately using that history.
                2. PRIMARY TOOL: If memory is insufficient, use 'get_file_contents' for repository access.
                3. BE DECISIVE: If you must use tools, list '.' AND relevant sub-folders (e.g., 'debugging/') SIMULTANEOUSLY in your first turn.
                4. HYBRID KNOWLEDGE: If the repository and memory both lack the answer, state that documentation is missing and provide 2-3 lines of general best practices.
                
                FORMATTING RULES:
                - Use standard Markdown.
                - ALWAYS use bullet points (e.g., '- Item') for technical details like Root Cause, Resolution, or Learning.
                - Use double newlines for paragraph breaks.
                
                Mandatory Response Ending:
                You MUST end every response with the following tag on a new line:
                [Confidence: HIGH|MEDIUM|LOW]
                
                CONFIDENCE SCORING:
                - HIGH: Answer from repository documents OR clear Session Memory.
                - MEDIUM: Answer with some logical inferences.
                - LOW: Clarifying questions or missing data.
                """, repoOwner, repoName);
    }

    public String buildUserPrompt(String query, String memory, String context) {
        return String.format("""
                Session Memory:
                %s
                
                Knowledge Context:
                %s
                
                User Query:
                %s
                """, memory, context, query);
    }
}
