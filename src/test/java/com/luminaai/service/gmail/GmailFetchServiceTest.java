package com.luminaai.service.gmail;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.luminaai.domain.model.EmailMessage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailFetchServiceTest {

    @Test
    void canInitializeService() {
        GmailFetchService service = new GmailFetchService(Optional.empty());
        assertNotNull(service);
    }

    @Test
    void fetchEmailsReturnsEmptyWhenGmailNotConfigured() {
        GmailFetchService service = new GmailFetchService(Optional.empty());
        assertTrue(service.fetchEmailsFromLast24Hours().isEmpty());
    }

    @Test
    void parsesPlainTextFromMultipartMessage() throws Exception {
        Message msg = multipartMessage("text/plain", "Hello world");
        EmailMessage parsed = invokeToEmailMessage(msg);
        assertThat(parsed.getBody()).isEqualTo("Hello world");
    }

    @Test
    void fallsBackToHtmlWhenPlainAbsent() throws Exception {
        Message msg = multipartMessage("text/html", "<p>Hi <b>there</b></p>");
        EmailMessage parsed = invokeToEmailMessage(msg);
        assertThat(parsed.getBody()).isEqualTo("Hi there");
    }

    @Test
    void parsesHeadersFromMultipartMessage() throws Exception {
        Message msg = multipartMessage("text/plain", "body");
        EmailMessage parsed = invokeToEmailMessage(msg);
        assertThat(parsed.getSubject()).isEqualTo("Test Subject");
        assertThat(parsed.getFrom()).isEqualTo("a@b.com");
    }

    private Message multipartMessage(String mimeType, String body) {
        MessagePartBody bodyPart = new MessagePartBody()
                .setData(Base64.encodeBase64URLSafeString(body.getBytes(StandardCharsets.UTF_8)));

        MessagePart inner = new MessagePart().setMimeType(mimeType).setBody(bodyPart);

        MessagePart payload = new MessagePart()
                .setMimeType("multipart/alternative")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("Subject").setValue("Test Subject"),
                        new MessagePartHeader().setName("From").setValue("a@b.com")))
                .setParts(List.of(inner));

        return new Message().setId("msg-1").setPayload(payload);
    }

    private EmailMessage invokeToEmailMessage(Message msg) throws Exception {
        GmailFetchService service = new GmailFetchService(Optional.empty());
        Method method = GmailFetchService.class.getDeclaredMethod("toEmailMessage", Message.class);
        method.setAccessible(true);
        return (EmailMessage) method.invoke(service, msg);
    }
}
