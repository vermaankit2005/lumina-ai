package com.luminaai.service.gmail;

import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GmailFetchServiceTest {

    @Test
    void canInitializeService() {
        GmailFetchService service = new GmailFetchService(java.util.Optional.empty());
        assertNotNull(service);
    }
}