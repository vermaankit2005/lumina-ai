package com.luminaai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.domain.model.LLMAnalysisResult;
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

@Slf4j
@Service
public class LLMService {

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

    public LLMAnalysisResult analyzeEmails(List<EmailMessage> emails) {
        if (emails == null || emails.isEmpty()) {
            log.info("No emails to analyze.");
            return emptyResult("No emails to analyze.");
        }

        try {
            String userPrompt = buildUserPrompt(emails);
            log.info("Sending {} email(s) to LLM for analysis...", emails.size());

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            log.debug("LLM raw response: {}", response);
            return objectMapper.readValue(response, LLMAnalysisResult.class);

        } catch (Exception e) {
            log.error("LLM analysis failed: {}", e.getMessage(), e);
            return emptyResult("LLM error: " + e.getMessage());
        }
    }

    private String buildUserPrompt(List<EmailMessage> emails) {
        LocalDate today = LocalDate.now();
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        StringBuilder threadsBuilder = new StringBuilder();
        for (int i = 0; i < emails.size(); i++) {
            EmailMessage email = emails.get(i);
            threadsBuilder.append("THREAD [").append(i + 1).append("]:\n")
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
                .replace("{email_threads}", threadsBuilder.toString())
                .replace("{open_tasks_context}", "None");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "(no body)";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "... [truncated]";
    }

    private LLMAnalysisResult emptyResult(String note) {
        LLMAnalysisResult result = new LLMAnalysisResult();
        result.setSummary("No significant emails to report.");
        result.setTasks(Collections.emptyList());
        result.setProcessingNotes(note);
        return result;
    }

    private String loadResource(String path) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource(path).getInputStream(),
                StandardCharsets.UTF_8);
    }
}
