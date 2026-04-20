package com.luminaai.port;

import com.luminaai.domain.model.EmailMessage;

import java.util.List;

/**
 * Port that abstracts the source of incoming emails.
 * Implementations may fetch from Gmail, a local SMTP stub, or any other provider.
 */
public interface EmailFetcherPort {
    List<EmailMessage> fetchEmailsFromLast24Hours();
}
