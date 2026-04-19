package com.luminaai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LLMAnalysisResult {

    private String summary;

    @JsonProperty("important_thread_count")
    private int importantThreadCount;

    private List<TaskItem> tasks;

    @JsonProperty("processing_notes")
    private String processingNotes;

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

        private float confidence;
    }
}
