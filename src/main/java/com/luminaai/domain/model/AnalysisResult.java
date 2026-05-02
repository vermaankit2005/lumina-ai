package com.luminaai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Structured result produced by the AI analysis of a batch of emails.
 * Contains a mood-line summary, inbox awareness highlights, and extracted action items.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {

    private String summary;

    @JsonProperty("important_thread_count")
    private int importantThreadCount;

    @JsonProperty("inbox_highlights")
    private List<InboxHighlight> inboxHighlights;

    private List<TaskItem> tasks;

    @JsonProperty("processing_notes")
    private String processingNotes;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InboxHighlight {
        @JsonProperty("thread_id")
        private String threadId;
        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TaskItem {
        private String title;
        private String description;
        private String priority;

        @JsonProperty("deadline_date")
        private String deadlineDate;

        @JsonProperty("deadline_raw_text")
        private String deadlineRawText;

        @JsonProperty("source_thread_id")
        private String sourceThreadId;

        @JsonProperty("source_email_id")
        private String sourceEmailId;

        @JsonProperty("source_sender")
        private String sourceSender;

        @JsonProperty("source_subject")
        private String sourceSubject;

        private String assignee;

        private float confidence;
    }
}
