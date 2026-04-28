package com.luminaai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.port.EmailAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * {@link EmailAnalysisPort} implementation backed by a locally-running or remote LLM
 * (Ollama in production, OpenAI-compatible in test).
 *
 * <p>Prompts are loaded from {@code classpath:prompts/} so they can be tuned without
 * recompiling the service.
 */
@Slf4j
@Service
public class LLMService implements EmailAnalysisPort {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;
    private final String userPromptTemplate;

    public LLMService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) throws Exception {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
        this.systemPrompt = loadResource("prompts/email-analysis-system.txt");
        this.userPromptTemplate = loadResource("prompts/email-analysis-user.txt");
    }

    @Override
    public AnalysisResult analyze(List<EmailMessage> emails) {
        if (emails == null || emails.isEmpty()) {
            log.info("No emails to analyse — returning empty result.");
            return emptyResult("No emails to analyse.");
        }

        try {
            String userPrompt = buildUserPrompt(emails);
            log.info("Sending {} email(s) to LLM for analysis...", emails.size());

            String rawJson = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            String cleanJson = sanitizeJson(rawJson);
            log.debug("LLM raw response length={}, cleaned length={}", rawJson.length(), cleanJson.length());
            return objectMapper.readValue(cleanJson, AnalysisResult.class);

        } catch (Exception e) {
            log.error("LLM analysis failed: {}", e.getMessage(), e);
            return failureResult("LLM error: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildUserPrompt(List<EmailMessage> emails) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        StringBuilder threads = new StringBuilder();
        for (int i = 0; i < emails.size(); i++) {
            EmailMessage email = emails.get(i);
            threads.append("THREAD [").append(i + 1).append("]:\n")
                    .append("  Thread-ID: ").append(email.getId()).append("\n")
                    .append("  Subject: ").append(email.getSubject()).append("\n")
                    .append("  Participants: From ").append(email.getFrom()).append("\n")
                    .append("  Messages in thread: 1\n")
                    .append("  ---\n")
                    .append(truncate(email.getBody(), 2000)).append("\n")
                    .append("  ---\n\n");
        }

        return userPromptTemplate
                .replace("{today_date}", today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .replace("{today_day_of_week}", today.getDayOfWeek().toString())
                .replace("{since_datetime}", since.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .replace("{thread_count}", String.valueOf(emails.size()))
                .replace("{email_threads}", threads.toString())
                .replace("{open_tasks_context}", "None");
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";
        String s = raw.strip();
        // strip ```json ... ``` or ``` ... ``` fences that smaller models emit despite instructions
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            int lastFence = s.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                s = s.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return s;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "(no body)";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "... [truncated]";
    }

    private AnalysisResult emptyResult(String note) {
        AnalysisResult result = new AnalysisResult();
        result.setSummary("No significant emails to report.");
        result.setTasks(Collections.emptyList());
        result.setProcessingNotes(note);
        return result;
    }

    private AnalysisResult failureResult(String reason) {
        AnalysisResult result = new AnalysisResult();
        result.setSummary("⚠️ Email analysis failed — see notes.");
        result.setTasks(Collections.emptyList());
        result.setProcessingNotes(reason);
        return result;
    }

    private String loadResource(String path) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource(path).getInputStream(),
                StandardCharsets.UTF_8);
    }
}
