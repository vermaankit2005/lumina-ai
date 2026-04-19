package com.luminaai.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmailMessage {
    private String id;
    private String subject;
    private String from;
    private String body;
}
