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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LLMService implements EmailAnalysisPort {

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 2_000;

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

            String rawJson = callWithRetry(userPrompt);
            String cleanJson = sanitizeJson(rawJson);
            log.debug("LLM raw response length={}, cleaned length={}", rawJson.length(), cleanJson.length());

            AnalysisResult result = objectMapper.readValue(cleanJson, AnalysisResult.class);
            deduplicateHighlights(result);
            return result;

        } catch (Exception e) {
            log.error("LLM analysis failed: {}", e.getMessage(), e);
            return failureResult("LLM error: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String callWithRetry(String userPrompt) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
            } catch (Exception e) {
                last = e;
                log.warn("LLM call failed (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) Thread.sleep(RETRY_DELAY_MS);
            }
        }
        throw last;
    }

    private String buildUserPrompt(List<EmailMessage> emails) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        StringBuilder threads = new StringBuilder();
        for (int i = 0; i < emails.size(); i++) {
            EmailMessage email = emails.get(i);
            threads.append("THREAD [").append(i + 1).append("]:\n")
                    .append("  Thread-ID: ").append(email.getId()).append("\n")
                    .append("  Subject: ").append(email.getSubject()).append("\n")
                    .append("  From: ").append(email.getFrom()).append("\n")
                    .append("  ---\n")
                    .append(truncate(email.getBody(), 2000)).append("\n")
                    .append("  ---\n\n");
        }

        return userPromptTemplate
                .replace("{today_date}", today.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .replace("{today_day_of_week}", today.getDayOfWeek().toString())
                .replace("{since_datetime}", since.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .replace("{thread_count}", String.valueOf(emails.size()))
                .replace("{email_threads}", threads.toString());
    }

    /** Remove highlights whose thread already produced a task — enforces non-overlap deterministically. */
    private void deduplicateHighlights(AnalysisResult result) {
        if (result.getInboxHighlights() == null || result.getInboxHighlights().isEmpty()) return;
        if (result.getTasks() == null || result.getTasks().isEmpty()) return;

        Set<String> taskThreadIds = result.getTasks().stream()
                .map(AnalysisResult.TaskItem::getSourceThreadId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        result.setInboxHighlights(
                result.getInboxHighlights().stream()
                        .filter(h -> h.getThreadId() == null || !taskThreadIds.contains(h.getThreadId()))
                        .toList()
        );
    }

    private String sanitizeJson(String raw) {
        if (raw == null || raw.isBlank()) return "{}";
        String s = raw.strip()
                .replaceFirst("^```(?:json|JSON)?\\s*", "")
                .replaceFirst("\\s*```\\s*$", "");
        // Grab the outermost { ... } in case the model added preamble/postamble
        int first = s.indexOf('{');
        int last  = s.lastIndexOf('}');
        if (first >= 0 && last > first) s = s.substring(first, last + 1);
        return s.isBlank() ? "{}" : s;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "(no body)";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "... [truncated]";
    }

    private AnalysisResult emptyResult(String note) {
        AnalysisResult result = new AnalysisResult();
        result.setSummary("Quiet inbox — nothing to report.");
        result.setTasks(Collections.emptyList());
        result.setInboxHighlights(Collections.emptyList());
        result.setProcessingNotes(note);
        return result;
    }

    private AnalysisResult failureResult(String reason) {
        AnalysisResult result = new AnalysisResult();
        result.setSummary("Analysis failed — check processing notes.");
        result.setTasks(Collections.emptyList());
        result.setInboxHighlights(Collections.emptyList());
        result.setProcessingNotes(reason);
        return result;
    }

    private String loadResource(String path) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource(path).getInputStream(),
                StandardCharsets.UTF_8);
    }
}
