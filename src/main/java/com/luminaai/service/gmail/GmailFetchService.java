package com.luminaai.service.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.luminaai.domain.model.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Profile("!test")
public class GmailFetchService implements EmailFetcher {

    private static final Logger log = LoggerFactory.getLogger(GmailFetchService.class);
    private static final long TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L;

    private final Gmail gmail;

    public GmailFetchService(java.util.Optional<Gmail> gmailOptional) {
        this.gmail = gmailOptional.orElse(null);
    }

    @Override
    public List<EmailMessage> fetchEmailsFromLast24Hours() {
        if (gmail == null) {
            log.warn("Gmail client is null, skipping fetch.");
            return List.of();
        }

        try {
            long sinceSeconds = (System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS) / 1000;
            var response = gmail.users().messages().list("me")
                    .setQ("after:" + sinceSeconds)
                    .setMaxResults(50L)
                    .execute();

            List<Message> summaries = response.getMessages();
            if (summaries == null || summaries.isEmpty()) return List.of();

            List<EmailMessage> emails = new ArrayList<>();
            for (Message summary : summaries) {
                try {
                    Message full = gmail.users().messages().get("me", summary.getId())
                            .setFormat("full")
                            .execute();
                    emails.add(toEmailMessage(full));
                } catch (Exception e) {
                    log.warn("Skipping message {}: {}", summary.getId(), e.getMessage());
                }
            }

            log.info("Fetched {} emails from Gmail.", emails.size());
            return emails;

        } catch (Exception e) {
            log.error("Failed to fetch emails from Gmail.", e);
            return List.of();
        }
    }

    private EmailMessage toEmailMessage(Message msg) {
        return EmailMessage.builder()
                .id(msg.getId())
                .from(getHeader(msg, "From"))
                .subject(getHeader(msg, "Subject"))
                .body(getBody(msg))
                .build();
    }

    private String getHeader(Message msg, String name) {
        if (msg.getPayload() == null || msg.getPayload().getHeaders() == null) return "";
        return msg.getPayload().getHeaders().stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst().orElse("");
    }

    private String getBody(Message msg) {
        var payload = msg.getPayload();
        if (payload == null || payload.getParts() != null
                || payload.getBody() == null || payload.getBody().getData() == null) {
            return "(body unavailable)";
        }
        byte[] data = com.google.api.client.util.Base64.decodeBase64(payload.getBody().getData());
        return new String(data, StandardCharsets.UTF_8);
    }
}
