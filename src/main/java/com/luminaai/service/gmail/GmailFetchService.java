package com.luminaai.service.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.luminaai.domain.model.EmailMessage;
import com.luminaai.port.EmailFetcherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Production {@link EmailFetcherPort} implementation that reads messages from Gmail
 * via the official Google API, authenticating with OAuth 2.0.
 *
 * <p>Active on all profiles except {@code test}. If the Gmail client could not be
 * initialised (missing credentials), the service returns an empty list rather than
 * failing the whole pipeline.
 */
@Slf4j
@Service
@Profile("!test")
public class GmailFetchService implements EmailFetcherPort {

    private static final long TWENTY_FOUR_HOURS_MS = 24L * 60 * 60 * 1000;

    private final Gmail gmail;

    public GmailFetchService(java.util.Optional<Gmail> gmailOptional) {
        this.gmail = gmailOptional.orElse(null);
    }

    @Override
    public List<EmailMessage> fetchEmailsFromLast24Hours() {
        if (gmail == null) {
            log.warn("Gmail client is not initialised — skipping fetch. Check your credentials.json.");
            return List.of();
        }

        try {
            long sinceEpochSeconds = (System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS) / 1000;
            var response = gmail.users().messages().list("me")
                    .setQ("after:" + sinceEpochSeconds)
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

            log.info("Fetched {} email(s) from Gmail.", emails.size());
            return emails;

        } catch (Exception e) {
            log.error("Failed to fetch emails from Gmail", e);
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
                .findFirst()
                .orElse("");
    }

    private String getBody(Message msg) {
        if (msg.getPayload() == null) return "(body unavailable)";

        return findPart(msg.getPayload(), "text/plain")
                .or(() -> findPart(msg.getPayload(), "text/html").map(GmailFetchService::stripHtml))
                .orElse("(body unavailable)");
    }

    private Optional<String> findPart(MessagePart part, String mimeType) {
        if (mimeType.equalsIgnoreCase(part.getMimeType())
                && part.getBody() != null
                && part.getBody().getData() != null) {
            return Optional.of(decode(part.getBody().getData()));
        }
        if (part.getParts() == null) return Optional.empty();
        return part.getParts().stream()
                .map(p -> findPart(p, mimeType))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private static String decode(String data) {
        return new String(com.google.api.client.util.Base64.decodeBase64(data), StandardCharsets.UTF_8);
    }

    private static String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
