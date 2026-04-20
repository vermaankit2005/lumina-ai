package com.luminaai.service.ai;

import com.luminaai.domain.model.AnalysisResult;
import com.luminaai.domain.model.EmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callSpec;

    private LLMService llmService;

    @BeforeEach
    void setUp() throws Exception {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        llmService = new LLMService(chatClientBuilder);
    }

    @Test
    void analyze_withEmptyList_returnsEmptyResult() {
        AnalysisResult result = llmService.analyze(List.of());

        assertThat(result).isNotNull();
        assertThat(result.getSummary()).isEqualTo("No significant emails to report.");
        assertThat(result.getTasks()).isEmpty();
    }

    @Test
    void analyze_withNullList_returnsEmptyResult() {
        AnalysisResult result = llmService.analyze(null);

        assertThat(result).isNotNull();
        assertThat(result.getTasks()).isEmpty();
    }

    @Test
    void analyze_withEmail_callsChatClientAndParsesJson() {
        String jsonResponse = """
                {
                  "summary": "Test summary",
                  "important_thread_count": 1,
                  "tasks": [
                    {
                      "title": "Reply to test email",
                      "description": "Test description",
                      "priority": "MEDIUM",
                      "source_email_id": "msg-001",
                      "source_sender": "Sender <sender@example.com>",
                      "source_subject": "Test Subject",
                      "confidence": 0.9
                    }
                  ]
                }
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(jsonResponse);

        EmailMessage email = EmailMessage.builder()
                .id("msg-001")
                .subject("Test Subject")
                .from("Sender <sender@example.com>")
                .body("Please reply to this test email.")
                .build();

        AnalysisResult result = llmService.analyze(List.of(email));

        assertThat(result.getSummary()).isEqualTo("Test summary");
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().get(0).getTitle()).isEqualTo("Reply to test email");
        assertThat(result.getTasks().get(0).getPriority()).isEqualTo("MEDIUM");
        verify(chatClient).prompt();
    }

    @Test
    void analyze_whenLLMThrowsException_returnsFallback() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("Ollama not available"));

        EmailMessage email = EmailMessage.builder()
                .id("msg-002").subject("Subject").from("a@b.com").body("body").build();

        AnalysisResult result = llmService.analyze(List.of(email));

        assertThat(result).isNotNull();
        assertThat(result.getTasks()).isEmpty();
        assertThat(result.getProcessingNotes()).contains("LLM error");
    }
}
