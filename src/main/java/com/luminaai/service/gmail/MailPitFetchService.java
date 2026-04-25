package com.luminaai.service.gmail;

import com.luminaai.domain.model.EmailMessage;
import com.luminaai.port.EmailFetcherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test-profile {@link EmailFetcherPort} implementation that reads messages from a
 * locally-running <a href="https://mailpit.axllent.org/">Mailpit</a> SMTP stub via
 * its REST API. Mailpit receives emails sent by the test script in {@code docs/}.
 *
 * <p>Active only on the {@code test} Spring profile.
 */
@Slf4j
@Service
@Profile("test")
public class MailPitFetchService implements EmailFetcherPort {

    private static final long TWENTY_FOUR_HOURS_MS = 24L * 60 * 60 * 1000;

    private final RestClient restClient = RestClient.create();

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_REF =
            new ParameterizedTypeReference<>() {};

    @Value("${lumina.mailpit.api-url}")
    private String apiUrl;

    @Override
    @SuppressWarnings("unchecked")
    public List<EmailMessage> fetchEmailsFromLast24Hours() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(apiUrl + "?limit=50")
                    .retrieve()
                    .body(MAP_REF);
            if (response == null) return List.of();

            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
            if (messages == null || messages.isEmpty()) return List.of();

            long cutoffMs = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS;
            List<EmailMessage> emails = new ArrayList<>();

            for (Map<String, Object> msg : messages) {
                if (isOlderThan(msg, cutoffMs)) break; // Mailpit returns newest-first
                emails.add(toEmailMessage(msg));
            }

            log.info("Fetched {} email(s) from Mailpit.", emails.size());
            return emails;

        } catch (Exception e) {
            log.warn("Failed to fetch emails from Mailpit: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isOlderThan(Map<String, Object> msg, long cutoffMs) {
        Object created = msg.get("Created");
        if (!(created instanceof String createdStr)) return false;
        return Instant.parse(createdStr).toEpochMilli() < cutoffMs;
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
            Map<String, Object> detail = restClient.get().uri(detailUrl).retrieve().body(MAP_REF);
            if (detail == null) return "(body unavailable)";
            String text = (String) detail.get("Text");
            if (text != null && !text.isBlank()) return text;
            String html = (String) detail.get("HTML");
            return html != null ? html : "(body unavailable)";
        } catch (Exception e) {
            log.warn("Failed to fetch body for message {}: {}", messageId, e.getMessage());
        }
        return "(body unavailable)";
    }
}
