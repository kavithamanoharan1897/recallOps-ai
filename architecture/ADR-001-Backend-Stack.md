# ADR 001: Backend Tech Stack Selection

## Status
Accepted

## Context
We need a robust, scalable backend to handle AI orchestration and data retrieval for the RecallOps AI prototype.

## Decision
We will use Spring Boot (3.4.x) with Java 21 (targeting Java 25 features as available).

## Rationale
- **Strong Typing:** Java provides excellent support for complex domain models needed for agentic orchestration.
- **Ecosystem:** Spring AI offers emerging support for LLM integrations, including Gemini.
- **Cloud Compatibility:** Spring Boot is well-supported on Google Cloud Run.
- **Maintainability:** Familiarity within enterprise engineering teams.

## Consequences
- Requires a JVM runtime.
- Slightly higher memory footprint compared to Go or Python, but manageable on Cloud Run.
