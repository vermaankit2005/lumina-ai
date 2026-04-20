package com.luminaai.service.gmail;

import com.luminaai.domain.model.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("test")
public class MailPitFetchService implements EmailFetcher {

    private static final Logger log = LoggerFactory.getLogger(MailPitFetchService.class);
    private static final long TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${lumina.mailpit.api-url}")
    private String apiUrl;

    @Override
    @SuppressWarnings("unchecked")
    public List<EmailMessage> fetchEmailsFromLast24Hours() {
        try {
            Map<String, Object> response = restTemplate.getForObject(apiUrl + "?limit=50", Map.class);
            if (response == null) return List.of();

            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
            if (messages == null || messages.isEmpty()) return List.of();

            long cutoff = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS;
            List<EmailMessage> emails = new ArrayList<>();

            for (Map<String, Object> msg : messages) {
                if (isOlderThan(msg, cutoff)) break; // Mailpit returns newest-first
                emails.add(toEmailMessage(msg));
            }

            log.info("Fetched {} emails from MailPit.", emails.size());
            return emails;

        } catch (Exception e) {
            log.warn("Failed to fetch emails from MailPit: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isOlderThan(Map<String, Object> msg, long cutoff) {
        Object created = msg.get("Created");
        if (!(created instanceof String)) return false;
        return Instant.parse((String) created).toEpochMilli() < cutoff;
    }

    @SuppressWarnings("unchecked")
    private EmailMessage toEmailMessage(Map<String, Object> msg) {
        String id = (String) msg.get("ID");
        String subject = (String) msg.get("Subject");
        String from = extractFrom((Map<String, String>) msg.get("From"));
        String body = fetchBody(id);
        return EmailMessage.builder().id(id).subject(subject).from(from).body(body).build();
    }

    private String extractFrom(Map<String, String> fromMap) {
        if (fromMap == null) return "";
        String name = fromMap.getOrDefault("Name", "");
        String address = fromMap.getOrDefault("Address", "");
        return name.isBlank() ? address : name + " <" + address + ">";
    }

    private String fetchBody(String messageId) {
        try {
            String detailUrl = apiUrl.replace("/messages", "") + "/message/" + messageId;
            Map<String, Object> detail = restTemplate.getForObject(detailUrl, Map.class);
            if (detail != null) return (String) detail.getOrDefault("Text", "(body unavailable)");
        } catch (Exception e) {
            log.warn("Failed to fetch body for message {}: {}", messageId, e.getMessage());
        }
        return "(body unavailable)";
    }
}
