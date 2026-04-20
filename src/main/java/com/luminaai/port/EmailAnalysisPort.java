package com.luminaai.port;

import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.domain.model.EmailMessage;

import java.util.List;

/**
 * Port that abstracts the AI-powered email analysis backend.
 * Implementations may delegate to Ollama, OpenAI, or any compatible LLM.
 */
public interface EmailAnalysisPort {
    AnalysisResult analyze(List<EmailMessage> emails);
}
