package com.luminaai.service.gmail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GmailFetchServiceTest {

    @Test
    void canInitializeService() {
        GmailFetchService service = new GmailFetchService(java.util.Optional.empty());
        assertNotNull(service);
    }

    @Test
    void fetchLatestEmailSubjectReturnsNullWhenGmailNotConfigured() {
        GmailFetchService service = new GmailFetchService(java.util.Optional.empty());
        assertTrue(service.fetchEmailsFromLast24Hours().isEmpty());
    }
}
