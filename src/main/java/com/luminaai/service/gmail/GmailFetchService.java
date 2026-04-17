package com.luminaai.service.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GmailFetchService {

    private static final Logger log = LoggerFactory.getLogger(GmailFetchService.class);
    private final Gmail gmail;

    public GmailFetchService(java.util.Optional<Gmail> gmailOptional) {
        this.gmail = gmailOptional.orElse(null);
    }

    public void fetchRecentEmails() {
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
    }

    public String fetchLatestEmailSubject() {
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
    }
}