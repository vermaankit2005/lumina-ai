package com.luminaai.service.gmail;

import com.google.api.services.gmail.Gmail;
import com.luminaai.domain.model.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class GmailFetchService {

    private static final Logger log = LoggerFactory.getLogger(GmailFetchService.class);
    private final Gmail gmail;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${lumina.mailpit.api-url}")
    private String mailpitApiUrl;

    public GmailFetchService(java.util.Optional<Gmail> gmailOptional) {
        this.gmail = gmailOptional.orElse(null);
    }

    public void fetchRecentEmails() {
        // TODO: Re-enable real Gmail API integration once API access is configured.
        // To re-enable: Uncomment the block below and comment out the Mailpit mock logic.
        /*
        if (gmail == null) {
            log.warn("Gmail client is null, skipping fetch.");
            return;
        }

        try {
            log.info("Fetching recent unread emails...");
            ListMessagesResponse response = gmail.users().messages().list("me")
                    .setQ("is:unread")
                    .setMaxResults(5L)
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No new messages found.");
                return;
            }

            for (Message msg : messages) {
                Message fullMsg = gmail.users().messages().get("me", msg.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(List.of("From", "Subject"))
                        .execute();

                String from = "";
                String subject = "";

                if (fullMsg.getPayload() != null && fullMsg.getPayload().getHeaders() != null) {
                    for (MessagePartHeader header : fullMsg.getPayload().getHeaders()) {
                        if ("From".equalsIgnoreCase(header.getName())) from = header.getValue();
                        if ("Subject".equalsIgnoreCase(header.getName())) subject = header.getValue();
                    }
                }

                log.info("Message ID: {} | From: {} | Subject: {}", fullMsg.getId(), from, subject);
            }
        } catch (Exception e) {
            log.error("Failed to fetch emails", e);
        }
        */

        // Local testing logic using Mailpit mock
        try {
            log.info("Fetching recent emails from Mailpit (Local Mock)...");
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(mailpitApiUrl, Map.class);
            if (response != null && response.containsKey("messages")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
                if (messages == null || messages.isEmpty()) {
                    log.info("No new messages found in Mailpit.");
                    return;
                }
                for (Map<String, Object> msg : messages) {
                    String from = (String) msg.get("From");
                    String subject = (String) msg.get("Subject");
                    String id = (String) msg.get("ID");
                    log.info("Mailpit Message ID: {} | From: {} | Subject: {}", id, from, subject);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch emails from Mailpit (is the container running?): {}", e.getMessage());
        }
    }

    public String fetchLatestEmailSubject() {
        // TODO: Re-enable real Gmail API integration once API access is configured.
        // To re-enable: Uncomment the block below and comment out the Mailpit mock logic.
        /*
        if (gmail == null) {
            log.warn("Gmail client is null, skipping fetch.");
            return null;
        }
        try {
            ListMessagesResponse response = gmail.users().messages().list("me")
                    .setQ("is:unread")
                    .setMaxResults(1L)
                    .execute();

            List<Message> messages = response.getMessages();
            if (messages == null || messages.isEmpty()) {
                log.info("No unread messages found.");
                return null;
            }

            Message fullMsg = gmail.users().messages().get("me", messages.get(0).getId())
                    .setFormat("metadata")
                    .setMetadataHeaders(List.of("Subject"))
                    .execute();

            if (fullMsg.getPayload() != null && fullMsg.getPayload().getHeaders() != null) {
                for (MessagePartHeader header : fullMsg.getPayload().getHeaders()) {
                    if ("Subject".equalsIgnoreCase(header.getName())) {
                        return header.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch latest email subject", e);
        }
        return null;
        */

        // Local testing logic using Mailpit mock
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(mailpitApiUrl + "?limit=1", Map.class);
            if (response != null && response.containsKey("messages")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
                if (messages != null && !messages.isEmpty()) {
                    return (String) messages.get(0).get("Subject");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch latest email from Mailpit: {}", e.getMessage());
        }
        return null;
    }

    public EmailMessage fetchLatestEmailMessage() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(mailpitApiUrl + "?limit=1", Map.class);
            if (response == null || !response.containsKey("messages")) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> messages = (List<Map<String, Object>>) response.get("messages");
            if (messages == null || messages.isEmpty()) return null;

            Map<String, Object> msg = messages.get(0);
            String id = (String) msg.get("ID");
            String subject = (String) msg.get("Subject");

            // From is an object: {"Name": "...", "Address": "..."}
            String from = "";
            Object fromObj = msg.get("From");
            if (fromObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> fromMap = (Map<String, String>) fromObj;
                String name = fromMap.getOrDefault("Name", "");
                String address = fromMap.getOrDefault("Address", "");
                from = name.isBlank() ? address : name + " <" + address + ">";
            } else if (fromObj instanceof String) {
                from = (String) fromObj;
            }

            // Fetch body from detail endpoint
            String body = fetchMessageBody(id);

            return EmailMessage.builder()
                    .id(id)
                    .subject(subject)
                    .from(from)
                    .body(body)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to fetch latest email from Mailpit: {}", e.getMessage());
            return null;
        }
    }

    private String fetchMessageBody(String messageId) {
        try {
            // Derive detail URL from mailpitApiUrl: replace /messages with /message/{id}
            String baseUrl = mailpitApiUrl.replace("/messages", "");
            String detailUrl = baseUrl + "/message/" + messageId;
            @SuppressWarnings("unchecked")
            Map<String, Object> detail = restTemplate.getForObject(detailUrl, Map.class);
            if (detail != null && detail.containsKey("Text")) {
                return (String) detail.get("Text");
            }
        } catch (Exception e) {
            log.warn("Failed to fetch body for message {}: {}", messageId, e.getMessage());
        }
        return "(body unavailable)";
    }
}