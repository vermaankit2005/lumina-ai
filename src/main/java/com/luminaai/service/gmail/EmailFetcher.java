package com.luminaai.service.gmail;

import com.luminaai.domain.model.EmailMessage;

import java.util.List;

public interface EmailFetcher {
    List<EmailMessage> fetchEmailsFromLast24Hours();
}
